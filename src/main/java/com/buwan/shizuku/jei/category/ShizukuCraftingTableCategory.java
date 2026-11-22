package com.buwan.shizuku.jei.category;

import com.buwan.shizuku.crafting.ITierCraftingRecipe;
import com.buwan.shizuku.crafting.ShapedTableCraftingRecipe;
import com.buwan.shizuku.crafting.ShapelessTableCraftingRecipe;
import com.buwan.shizuku.registry.ModBlocks;
import com.buwan.shizuku.shizuku;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mods.flammpfeil.slashblade.recipe.RequestDefinition;
import mods.flammpfeil.slashblade.recipe.SlashBladeIngredient;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

/**
 * 雫工作台 JEI 分类：9x9 输入网格 + 输出槽。
 *
 * <p>两个关键点（都是踩过坑的）：</p>
 * <ul>
 *   <li><b>配方类型 id 必须和 {@code ModRecipeTypes} 一致</b>（{@code shizuku:crafting_table_recipe}）。
 *       以前这里写成 {@code shizuku:shizuku_craft}，JEI 会报
 *       {@code Unknown recipe category: shizuku:crafting_table_recipe/...}，分类接不上。</li>
 *   <li><b>拔刀剑配料的 request 取不到</b>：{@code SlashBladeIngredient} 的 request 是私有字段、
 *       {@code Ingredient} 不暴露它，JEI 只能把材料画成「无铭」占位刀。
 *       这里按字段类型反射读出来，再去 {@code SlashBladeDefinition} 注册表取完整定义刀展示。</li>
 * </ul>
 */
public class ShizukuCraftingTableCategory implements IRecipeCategory<ITierCraftingRecipe> {

    /** 必须与 {@code ModRecipeTypes.CRAFTING_TABLE_RECIPE} 的 id 一致。 */
    public static final RecipeType<ITierCraftingRecipe> RECIPE_TYPE =
            RecipeType.create("shizuku", "crafting_table_recipe", ITierCraftingRecipe.class);

    private static final ResourceLocation TEXTURE = shizuku.prefix("textures/gui/jei/shizuku_crafting_jei.png");
    private static final int OUTPUT_X = 167;
    private static final int OUTPUT_Y = 74;

    private final IDrawable background;
    private final IDrawable icon;

    public ShizukuCraftingTableCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 189, 163);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack((ItemLike) ModBlocks.SHIZUKU_CRAFTING_TABLE.get()));
    }

    @NotNull
    @Override
    public RecipeType<ITierCraftingRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.translatable("jei.category.shizuku.shizuku_crafting_table");
    }

    @NotNull
    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        return 189;
    }

    @Override
    public int getHeight() {
        return 163;
    }

    public void draw(
            @NotNull ITierCraftingRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView,
            @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY
    ) {
        this.background.draw(guiGraphics);
    }

    @Override
    public void setRecipe(
            @NotNull IRecipeLayoutBuilder builder, ITierCraftingRecipe recipe, @NotNull IFocusGroup focuses
    ) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        RegistryAccess access = level.registryAccess();
        NonNullList<Ingredient> inputs = recipe.getIngredients();

        if (recipe instanceof ShapedTableCraftingRecipe shaped) {
            int width = shaped.getWidth();
            int height = shaped.getHeight();
            int offset = Math.floorDiv(9 - width, 2);
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    Ingredient ingredient = inputs.get(j + i * width);
                    if (!ingredient.isEmpty()) {
                        addIngredientSlot(builder, ingredient,
                                18 * (j + offset) + 1, 18 * (i + offset) + 1, access);
                    }
                }
            }
        } else if (recipe instanceof ShapelessTableCraftingRecipe) {
            int index = 0;
            for (Ingredient ingredient : inputs) {
                if (!ingredient.isEmpty()) {
                    addIngredientSlot(builder, ingredient, 18 * (index % 9) + 1, 18 * (index / 9) + 1, access);
                    index++;
                }
            }
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addItemStack(resolveOutput(recipe, access));
    }

    /** {@code SlashBladeIngredient#request}（私有字段、无 getter）。 */
    private static final java.lang.reflect.Field REQUEST_FIELD = findRequestField();

    private static java.lang.reflect.Field findRequestField() {
        // 按「字段类型」找而不是按名字找：上游改名也不受影响
        Class<?> type = SlashBladeIngredient.class;
        while (type != null && type != Object.class) {
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                if (RequestDefinition.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        return field;
                    } catch (Throwable ignored) {
                        return null;
                    }
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static RequestDefinition requestOf(Ingredient ingredient) {
        if (!(ingredient instanceof SlashBladeIngredient) || REQUEST_FIELD == null) {
            return null;
        }
        try {
            Object value = REQUEST_FIELD.get(ingredient);
            return value instanceof RequestDefinition def ? def : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void addIngredientSlot(IRecipeLayoutBuilder builder, Ingredient ingredient,
                                          int x, int y, RegistryAccess access) {
        var slot = builder.addSlot(RecipeIngredientRole.INPUT, x, y);
        RequestDefinition request = requestOf(ingredient);
        ItemStack display = resolveBladeFor(request, access);
        // 解析不出来就原样交给 JEI —— 不能"随便挑一把"，否则所有输入槽会显示成同一把刀
        if (display.isEmpty()) {
            slot.addIngredients(ingredient);
            return;
        }
        slot.addItemStack(display);
    }

    /**
     * 解析配方产物：带 {@code blade} 字段的配方按 id 取完整命名刀。
     *
     * <p>只调 {@code getResultItem()} 在 JEI 里会拿到未加载状态的占位栈（显示为「无铭」、伤害数值溢出）。</p>
     */
    private static ItemStack resolveOutput(ITierCraftingRecipe recipe, RegistryAccess access) {
        if (recipe instanceof ShapedTableCraftingRecipe shaped && shaped.getOutputBlade() != null) {
            SlashBladeDefinition def = access.registryOrThrow(SlashBladeDefinition.REGISTRY_KEY)
                    .get(shaped.getOutputBlade());
            if (def != null) {
                return def.getBlade();
            }
        }
        return recipe.getResultItem(access);
    }

    /**
     * 按配方要求取真实的命名刀（含完整默认属性）用于展示。
     *
     * <p>直接按定义注册表键（= 定义 JSON 文件名）查。约定：命名刀的定义文件名必须与 {@code name} 字段一致；
     * 不一致时按 id 查不到，图标会退化成默认模型。</p>
     */
    private static ItemStack resolveBladeFor(RequestDefinition request, RegistryAccess access) {
        ResourceLocation name = requestName(request);
        if (name == null) {
            return ItemStack.EMPTY;
        }
        SlashBladeDefinition def = access.registryOrThrow(SlashBladeDefinition.REGISTRY_KEY).get(name);
        return def == null ? ItemStack.EMPTY : def.getBlade();
    }

    /**
     * 取 request 里要求的刀 id。
     *
     * <p>这里刻意用反射而不是直接调方法：不同版本的 SlashBlade 对 {@code RequestDefinition} 的写法不同
     * （新版是 record，只有 {@code name()}；旧版是普通类，只有 {@code getName()}）。
     * 直接按其中一个编译，换版本就会 NoSuchMethodError。</p>
     */
    private static ResourceLocation requestName(RequestDefinition request) {
        if (request == null) {
            return null;
        }
        for (String method : new String[]{"name", "getName"}) {
            try {
                Object value = RequestDefinition.class.getMethod(method).invoke(request);
                if (value instanceof ResourceLocation loc) {
                    return loc;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }
}
