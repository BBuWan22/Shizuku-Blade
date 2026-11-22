package com.buwan.shizuku.jei;

import com.buwan.shizuku.shizuku;
import com.buwan.shizuku.client.screen.ShizukuCraftScreen;
import com.buwan.shizuku.crafting.ITierCraftingRecipe;
import com.buwan.shizuku.jei.category.ShizukuCraftingTableCategory;
import com.buwan.shizuku.menu.ShizukuCraftMenu;
import com.buwan.shizuku.registry.ModBlocks;
import com.buwan.shizuku.registry.ModMenus;
import com.buwan.shizuku.registry.ModRecipeTypes;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

/**
 * JEI 插件入口：注册工作台配方分类、配方列表、催化剂（工作台方块）、
 * 配方转移（一键把材料摆进 9x9 界面）以及界面里的配方点击区域。
 */
@JeiPlugin
public class ShizukuJeiPlugin implements IModPlugin {
    public static final ResourceLocation UID = shizuku.prefix("jei_plugin");

    @NotNull
    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ShizukuCraftingTableCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world != null) {
            RecipeManager manager = world.getRecipeManager();
            List<ITierCraftingRecipe> recipes = manager.getAllRecipesFor((RecipeType<ITierCraftingRecipe>)ModRecipeTypes.CRAFTING_TABLE_RECIPE.get());
            registration.addRecipes(ShizukuCraftingTableCategory.RECIPE_TYPE, new ArrayList<>(recipes));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack((ItemLike)ModBlocks.SHIZUKU_CRAFTING_TABLE.get()), ShizukuCraftingTableCategory.RECIPE_TYPE);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
            ShizukuCraftMenu.class, (MenuType<ShizukuCraftMenu>)ModMenus.SHIZUKU_CRAFT_TABLE.get(), ShizukuCraftingTableCategory.RECIPE_TYPE, 1, 81, 82, 36
        );
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(ShizukuCraftScreen.class, 174, 90, 22, 12, ShizukuCraftingTableCategory.RECIPE_TYPE);
    }
}
