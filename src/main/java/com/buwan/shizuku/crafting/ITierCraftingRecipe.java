package com.buwan.shizuku.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.NotNull;

public interface ITierCraftingRecipe extends Recipe<Container> {
    int getTier();

    boolean hasRequiredTier();

    @NotNull
    @Override
    default NonNullList<ItemStack> getRemainingItems(@NotNull Container inv) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);

        for (int i = 0; i < remaining.size(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.hasCraftingRemainingItem()) {
                remaining.set(i, stack.getCraftingRemainingItem());
            }
        }

        return remaining;
    }
}
