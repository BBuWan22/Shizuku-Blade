package com.buwan.shizuku.registry;

import com.buwan.shizuku.crafting.ShapedTableCraftingRecipe;
import com.buwan.shizuku.crafting.ShapelessTableCraftingRecipe;
import java.util.function.Supplier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "shizuku");
    public static final RegistryObject<RecipeSerializer<?>> SHAPED_CRAFT_SERIALIZER = serializer("shaped_table", ShapedTableCraftingRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<?>> SHAPELESS_CRAFT_SERIALIZER = serializer(
        "shapeless_table", ShapelessTableCraftingRecipe.Serializer::new
    );

    public static RegistryObject<RecipeSerializer<?>> serializer(String name, Supplier<RecipeSerializer<?>> serializer) {
        return SERIALIZERS.register(name, serializer);
    }
}
