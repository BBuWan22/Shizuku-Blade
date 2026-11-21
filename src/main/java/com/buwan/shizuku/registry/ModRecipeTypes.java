package com.buwan.shizuku.registry;

import com.buwan.shizuku.crafting.ITierCraftingRecipe;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 配方类型注册：雫工作台配方类型（ITierCraftingRecipe）。
 */
public class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, "shizuku");
    public static final RegistryObject<RecipeType<ITierCraftingRecipe>> CRAFTING_TABLE_RECIPE = recipe(
        "crafting_table_recipe", () -> RecipeType.simple(new ResourceLocation("shizuku", "crafting_table_recipe"))
    );

    public static <T extends Recipe<Container>> RegistryObject<RecipeType<T>> recipe(String name, Supplier<RecipeType<T>> type) {
        return RECIPES.register(name, type);
    }
}
