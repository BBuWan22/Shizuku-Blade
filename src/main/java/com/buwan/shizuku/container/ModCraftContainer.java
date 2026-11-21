package com.buwan.shizuku.container;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 把 Forge 的 ItemStackHandler 适配成原版 Container，供 ShizukuCraftMenu 使用。
 *
 * 因为工作台是 9x9（最多 81 格）且要支持 tier 过滤，这里只是薄薄一层转发。
 */
public class ModCraftContainer extends TransientCraftingContainer {
    private final AbstractContainerMenu menu;
    private final ItemStackHandler inventory;

    public ModCraftContainer(AbstractContainerMenu menu, ItemStackHandler inventory, int size) {
        super(menu, size, size);
        this.menu = menu;
        this.inventory = inventory;
    }

    @Override
    public int getContainerSize() {
        return this.inventory.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            if (!this.inventory.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @NotNull
    @Override
    public ItemStack getItem(int slot) {
        return this.inventory.getStackInSlot(slot);
    }

    @NotNull
    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = this.inventory.extractItem(slot, amount, false);
        this.menu.slotsChanged(this);
        return stack;
    }

    @NotNull
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = this.inventory.getStackInSlot(slot);
        this.inventory.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        this.inventory.setStackInSlot(slot, stack);
        this.menu.slotsChanged(this);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            this.inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
}
