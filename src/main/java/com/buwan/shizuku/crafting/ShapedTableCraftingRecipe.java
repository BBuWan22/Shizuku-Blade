package com.buwan.shizuku.crafting;

import com.buwan.shizuku.registry.ModRecipeSerializers;
import com.buwan.shizuku.registry.ModRecipeTypes;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 工作台的有序配方，支持在网格内平移与镜像匹配。
 *
 * - matches()：按 size 推导网格边长，逐 (x,y) 偏移尝试 checkMatch（含镜像）；
 * - getResultItem()：产物可以是「命名刀」——按 blade 字段从 SlashBlade 定义注册表取完整刀，
 *   取不到才退回普通产物栈；
 * - checkMatch()：空槽要求网格对应位置也为空（这是 9x9 大配方能正确匹配的关键）。
 *
 * 已知问题（上游行为）：按定义 id 查刀，命名刀定义文件名必须与 name 字段一致，
 * 不一致时 JEI/产物会退化成无铭占位刀。
 */
public class ShapedTableCraftingRecipe implements ITierCraftingRecipe {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShapedTableCraftingRecipe.class);
    private final ResourceLocation recipeId;
    private final NonNullList<Ingredient> inputs;
    private final ItemStack output;
    private final int width;
    private final int height;
    private final int tier;
    private final ResourceLocation outputBlade;

    public ShapedTableCraftingRecipe(ResourceLocation recipeId, int width, int height, NonNullList<Ingredient> inputs, ItemStack output) {
        this(recipeId, width, height, inputs, output, 0);
    }

    public ShapedTableCraftingRecipe(ResourceLocation recipeId, int width, int height, NonNullList<Ingredient> inputs, ItemStack output, int tier) {
        this(recipeId, width, height, inputs, output, tier, null);
    }

    public ShapedTableCraftingRecipe(
        ResourceLocation recipeId, int width, int height, NonNullList<Ingredient> inputs, ItemStack output, int tier, ResourceLocation outputBlade
    ) {
        this.recipeId = recipeId;
        this.inputs = inputs;
        this.output = output;
        this.width = width;
        this.height = height;
        this.tier = tier;
        this.outputBlade = outputBlade;
    }

    private static Map<Character, Ingredient> keyFromJson(JsonObject keyJson) {
        Map<Character, Ingredient> map = Maps.newHashMap();

        for (Entry<String, JsonElement> entry : keyJson.entrySet()) {
            if (entry.getKey().length() != 1) {
                throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
            }

            if (" ".equals(entry.getKey())) {
                throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
            }

            map.put(entry.getKey().charAt(0), Ingredient.fromJson(entry.getValue()));
        }

        return map;
    }

    private static NonNullList<Ingredient> dissolvePattern(String[] pattern, Map<Character, Ingredient> key, int width, int height) {
        NonNullList<Ingredient> inputs = NonNullList.withSize(width * height, Ingredient.EMPTY);
        Set<Character> set = Sets.newHashSet(key.keySet());
        set.remove(' ');

        for (int i = 0; i < pattern.length; i++) {
            for (int j = 0; j < pattern[i].length(); j++) {
                char c0 = pattern[i].charAt(j);
                if (c0 != ' ') {
                    Ingredient ingredient = key.get(c0);
                    if (ingredient == null) {
                        throw new JsonSyntaxException("Pattern references symbol '" + c0 + "' but it's not defined in the key");
                    }

                    set.remove(c0);
                    inputs.set(j + width * i, ingredient);
                }
            }
        }

        if (!set.isEmpty()) {
            throw new JsonSyntaxException("Key defines symbols that aren't used in pattern: " + set);
        } else {
            return inputs;
        }
    }

    private static String[] patternFromJson(JsonArray jsonArr) {
        String[] astring = new String[jsonArr.size()];

        for (int i = 0; i < astring.length; i++) {
            String s = GsonHelper.convertToString(jsonArr.get(i), "pattern[" + i + "]");
            if (i > 0 && astring[0].length() != s.length()) {
                throw new JsonSyntaxException("Invalid pattern: each row must be the same width");
            }

            astring[i] = s;
        }

        return astring;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    @NotNull
    @Override
    public ItemStack getResultItem(@NotNull RegistryAccess access) {
        if (this.outputBlade != null) {
            try {
                Registry<SlashBladeDefinition> registry = access.registryOrThrow(SlashBladeDefinition.REGISTRY_KEY);
                SlashBladeDefinition holder = registry.get(this.outputBlade);
                if (holder != null) {
                    return holder.getBlade();
                }

                LOGGER.error("[ShizukuCraft] Blade definition '{}' not found in registry {}", this.outputBlade, SlashBladeDefinition.REGISTRY_KEY.location());
            } catch (Exception var4) {
                LOGGER.error("[ShizukuCraft] Failed to create output blade '{}'", this.outputBlade, var4);
            }

            return this.output.copy();
        } else {
            return this.output;
        }
    }

    @NotNull
    @Override
    public ItemStack assemble(@NotNull Container inv, @NotNull RegistryAccess access) {
        return this.getResultItem(access);
    }

    @Override
    public boolean matches(@NotNull Container inv, @NotNull Level level) {
        if (this.tier != 0 && this.tier != this.getTierFromGridSize(inv.getContainerSize())) {
            return false;
        } else {
            int size = (int)Math.sqrt(inv.getContainerSize());

            for (int i = 0; i <= size - this.width; i++) {
                for (int j = 0; j <= size - this.height; j++) {
                    if (this.checkMatch(inv, i, j, true)) {
                        return true;
                    }

                    if (this.checkMatch(inv, i, j, false)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    @NotNull
    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.inputs;
    }

    @NotNull
    @Override
    public ResourceLocation getId() {
        return this.recipeId;
    }

    @NotNull
    @Override
    public RecipeSerializer<?> getSerializer() {
        return (RecipeSerializer<?>)ModRecipeSerializers.SHAPED_CRAFT_SERIALIZER.get();
    }

    @NotNull
    @Override
    public RecipeType<?> getType() {
        return (RecipeType<?>)ModRecipeTypes.CRAFTING_TABLE_RECIPE.get();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.width && height >= this.height;
    }

    @Override
    public int getTier() {
        if (this.tier > 0) {
            return this.tier;
        } else {
            return this.width < 4 && this.height < 4 ? 1 : (this.width < 6 && this.height < 6 ? 2 : (this.width < 8 && this.height < 8 ? 3 : 4));
        }
    }

    @Override
    public boolean hasRequiredTier() {
        return this.tier > 0;
    }

    private int getTierFromGridSize(int slots) {
        return slots < 10 ? 1 : (slots < 26 ? 2 : (slots < 50 ? 3 : 4));
    }

    private boolean checkMatch(Container inventory, int x, int y, boolean mirror) {
        int size = (int)Math.sqrt(inventory.getContainerSize());

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int k = i - x;
                int l = j - y;
                Ingredient ingredient = Ingredient.EMPTY;
                if (k >= 0 && l >= 0 && k < this.width && l < this.height) {
                    if (mirror) {
                        ingredient = this.inputs.get(this.width - k - 1 + l * this.width);
                    } else {
                        ingredient = this.inputs.get(k + l * this.width);
                    }
                }

                if (!ingredient.test(inventory.getItem(i + j * size))) {
                    return false;
                }
            }
        }

        return true;
    }

    public static class Serializer implements RecipeSerializer<ShapedTableCraftingRecipe> {
        @NotNull
        public ShapedTableCraftingRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            Map<Character, Ingredient> map = ShapedTableCraftingRecipe.keyFromJson(GsonHelper.getAsJsonObject(json, "key"));
            String[] pattern = ShapedTableCraftingRecipe.patternFromJson(GsonHelper.getAsJsonArray(json, "pattern"));
            int width = pattern[0].length();
            int height = pattern.length;
            NonNullList<Ingredient> inputs = ShapedTableCraftingRecipe.dissolvePattern(pattern, map, width, height);
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int tier = GsonHelper.getAsInt(json, "tier", 0);
            int size = tier * 2 + 1;
            if (tier == 0 || width <= size && height <= size) {
                ResourceLocation outputBlade = json.has("blade") ? new ResourceLocation(GsonHelper.getAsString(json, "blade")) : null;
                return new ShapedTableCraftingRecipe(recipeId, width, height, inputs, output, tier, outputBlade);
            } else {
                throw new JsonSyntaxException("The pattern size is larger than the specified tier can support");
            }
        }

        public ShapedTableCraftingRecipe fromNetwork(@NotNull ResourceLocation recipeId, FriendlyByteBuf buffer) {
            int width = buffer.readVarInt();
            int height = buffer.readVarInt();
            NonNullList<Ingredient> inputs = NonNullList.withSize(width * height, Ingredient.EMPTY);
            inputs.replaceAll(ignored -> Ingredient.fromNetwork(buffer));
            ItemStack output = buffer.readItem();
            int tier = buffer.readVarInt();
            ResourceLocation outputBlade = null;
            if (buffer.readBoolean()) {
                outputBlade = buffer.readResourceLocation();
            }

            return new ShapedTableCraftingRecipe(recipeId, width, height, inputs, output, tier, outputBlade);
        }

        public void toNetwork(FriendlyByteBuf buffer, ShapedTableCraftingRecipe recipe) {
            buffer.writeVarInt(recipe.width);
            buffer.writeVarInt(recipe.height);

            for (Ingredient ingredient : recipe.inputs) {
                ingredient.toNetwork(buffer);
            }

            buffer.writeItem(recipe.output);
            buffer.writeVarInt(recipe.tier);
            buffer.writeBoolean(recipe.outputBlade != null);
            if (recipe.outputBlade != null) {
                buffer.writeResourceLocation(recipe.outputBlade);
            }
        }
    }
}
