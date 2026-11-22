package com.buwan.shizuku.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.NotNull;

/**
 * 雫工作台配方的公共接口：tier n 对应 (2n+1)x(2n+1) 的合成网格，
 * 并提供按格位读取配料的视图（getIngredients 在 1.20.1 里会丢空槽信息）。
 */
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
