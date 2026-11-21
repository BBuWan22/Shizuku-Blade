package com.buwan.shizuku.tile;

import com.buwan.shizuku.menu.ShizukuCraftMenu;
import com.buwan.shizuku.registry.ModTileEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 工作台的 BlockEntity：持有 81 格 ItemStackHandler，负责存读档
 * （load/saveAdditional）与创建菜单（createMenu）。
 */
public class ShizukuCraftTile extends BlockEntity implements MenuProvider {
    public static final int GRID = 9;
    public static final int SLOT_COUNT = 81;
    private final ItemStackHandler inventory = new ItemStackHandler(81);
    private LazyOptional<IItemHandler> capability = LazyOptional.of(() -> this.inventory);

    public ShizukuCraftTile(BlockPos pos, BlockState blockState) {
        super((BlockEntityType<?>)ModTileEntities.SHIZUKU_CRAFT_TILE.get(), pos, blockState);
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.shizuku.shizuku_crafting_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new ShizukuCraftMenu(containerId, playerInventory, this.getBlockPos());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.inventory.deserializeNBT(tag.getCompound("Inventory"));
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", this.inventory.serializeNBT());
    }

    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return !this.isRemoved() && cap == ForgeCapabilities.ITEM_HANDLER
            ? ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, this.capability)
            : super.getCapability(cap, side);
    }

    public void invalidateCaps() {
        super.invalidateCaps();
        this.capability.invalidate();
    }

    public void reviveCaps() {
        super.reviveCaps();
        this.capability = LazyOptional.of(() -> this.inventory);
    }
}
