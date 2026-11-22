package com.buwan.shizuku.container.slot;

import com.buwan.shizuku.registry.ModRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;

/**
 * 工作台的产物槽：
 * - mayPlace 恒 false（只能取不能放）；
 * - onTake：取走产物时批量消耗输入格、给经验、并把剩余物品塞回玩家背包，
 *   避免材料被吞（这是手写配方最常见的坑）。
 */
public class ModCraftResultSlot extends Slot {
    private final AbstractContainerMenu container;
    private final CraftingContainer craftSlots;
    private final Player player;
    private int removeCount;

    public ModCraftResultSlot(
        Player player, AbstractContainerMenu menu, CraftingContainer craftSlots, Container container, int index, int xPosition, int yPosition
    ) {
        super(container, index, xPosition, yPosition);
        this.player = player;
        this.container = menu;
        this.craftSlots = craftSlots;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return false;
    }

    @NotNull
    @Override
    public ItemStack remove(int amount) {
        if (this.hasItem()) {
            this.removeCount = this.removeCount + Math.min(amount, this.getItem().getCount());
        }

        return super.remove(amount);
    }

    @Override
    protected void onQuickCraft(@NotNull ItemStack stack, int amount) {
        this.removeCount += amount;
        this.checkTakeAchievements(stack);
    }

    @Override
    protected void onSwapCraft(int numItemsCrafted) {
        this.removeCount += numItemsCrafted;
    }

    @Override
    public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
        this.checkTakeAchievements(stack);
        ForgeHooks.setCraftingPlayer(player);
        NonNullList<ItemStack> remaining = player.level()
            .getRecipeManager()
            .getRemainingItemsFor((RecipeType)ModRecipeTypes.CRAFTING_TABLE_RECIPE.get(), this.craftSlots, player.level());
        ForgeHooks.setCraftingPlayer(null);

        for (int i = 0; i < remaining.size(); i++) {
            ItemStack slotStack = this.craftSlots.getItem(i);
            ItemStack remainingStack = remaining.get(i);
            if (!slotStack.isEmpty()) {
                this.craftSlots.removeItem(i, 1);
                slotStack = this.craftSlots.getItem(i);
            }

            if (!remainingStack.isEmpty()) {
                if (slotStack.isEmpty()) {
                    this.craftSlots.setItem(i, remainingStack);
                } else if (ItemStack.isSameItem(slotStack, remainingStack) && ItemStack.isSameItemSameTags(slotStack, remainingStack)) {
                    remainingStack.grow(slotStack.getCount());
                    this.craftSlots.setItem(i, remainingStack);
                } else if (!this.player.getInventory().add(remainingStack)) {
                    this.player.drop(remainingStack, false);
                }
            }
        }

        this.container.slotsChanged(this.craftSlots);
    }

    @Override
    protected void checkTakeAchievements(@NotNull ItemStack stack) {
        if (this.removeCount > 0) {
            stack.onCraftedBy(this.player.level(), this.player, this.removeCount);
            ForgeEventFactory.firePlayerCraftingEvent(this.player, stack, this.craftSlots);
        }

        if (this.container instanceof RecipeHolder recipeholder) {
            recipeholder.awardUsedRecipes(this.player, this.craftSlots.getItems());
        }

        this.removeCount = 0;
    }
}
