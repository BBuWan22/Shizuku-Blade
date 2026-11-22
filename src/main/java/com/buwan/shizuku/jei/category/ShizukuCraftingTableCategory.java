package com.buwan.shizuku.jei.category;

import com.buwan.shizuku.shizuku;
import com.buwan.shizuku.crafting.ITierCraftingRecipe;
import com.buwan.shizuku.crafting.ShapedTableCraftingRecipe;
import com.buwan.shizuku.crafting.ShapelessTableCraftingRecipe;
import com.buwan.shizuku.registry.ModBlocks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

/**
 * JEI 里「雫工作台」的分类界面：按配方 pattern 把 9x9 的输入槽摆出来，
 * 右侧放产物槽。
 *
 * 已知问题（上游行为，Fabric 移植版已修）：输入槽里的「命名刀」材料只能显示成
 * 无铭占位刀——配方配料里的 request（要求哪把刀）在 1.20.1 的 Ingredient 里
 * 拿不到，只能靠反射读 SlashBladeIngredient 的私有字段；读不到时退回原样配料。
 */
public class ShizukuCraftingTableCategory implements IRecipeCategory<ITierCraftingRecipe> {
    public static final RecipeType<ITierCraftingRecipe> RECIPE_TYPE = RecipeType.create("shizuku", "shizuku_craft", ITierCraftingRecipe.class);
    private static final ResourceLocation TEXTURE = shizuku.prefix("textures/gui/jei/shizuku_crafting_jei.png");
    private final IDrawable background;
    private final IDrawable icon;

    public ShizukuCraftingTableCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 189, 163);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack((ItemLike)ModBlocks.SHIZUKU_CRAFTING_TABLE.get()));
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
        @NotNull ITierCraftingRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY
    ) {
        this.background.draw(guiGraphics);
    }

    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, ITierCraftingRecipe recipe, @NotNull IFocusGroup focuses) {
        ClientLevel level = Minecraft.getInstance().level;

        assert level != null;

        NonNullList<Ingredient> inputs = recipe.getIngredients();
        ItemStack output = recipe.getResultItem(level.registryAccess());
        if (recipe instanceof ShapedTableCraftingRecipe shaped) {
            int stackIndex = 0;
            int heightOffset = Math.floorDiv(9 - shaped.getHeight(), 2);
            int widthOffset = Math.floorDiv(9 - shaped.getWidth(), 2);

            for (int i = heightOffset; i < shaped.getHeight() + heightOffset; i++) {
                for (int j = widthOffset; j < shaped.getWidth() + widthOffset; j++) {
                    builder.addSlot(RecipeIngredientRole.INPUT, j * 18 + 1, i * 18 + 1).addIngredients(inputs.get(stackIndex));
                    stackIndex++;
                }
            }

            builder.addSlot(RecipeIngredientRole.OUTPUT, 167, 74).addItemStack(output);
        } else if (recipe instanceof ShapelessTableCraftingRecipe) {
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    int index = j + i * 9;
                    if (index < inputs.size()) {
                        builder.addSlot(RecipeIngredientRole.INPUT, j * 18 + 1, i * 18 + 1).addIngredients(inputs.get(index));
                    }
                }
            }

            builder.setShapeless(163, 150);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 167, 74).addItemStack(output);
        }

        builder.moveRecipeTransferButton(170, 120);
    }
}
