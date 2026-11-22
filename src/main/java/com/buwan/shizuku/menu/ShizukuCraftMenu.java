package com.buwan.shizuku.menu;

import com.buwan.shizuku.container.ModCraftContainer;
import com.buwan.shizuku.container.slot.ModCraftResultSlot;
import com.buwan.shizuku.crafting.ITierCraftingRecipe;
import com.buwan.shizuku.registry.ModMenus;
import com.buwan.shizuku.registry.ModRecipeTypes;
import com.buwan.shizuku.tile.ShizukuCraftTile;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 工作台的容器菜单：9x9 输入 + 产物槽 + 玩家背包。
 *
 * - slotsChanged：输入变化时重新查找匹配配方（按 tier 过滤网格大小）；
 * - quickMoveStack：Shift 点击在背包与输入区之间搬运；
 * - stillValid：玩家离方块太远则关闭。
 */
public class ShizukuCraftMenu extends AbstractContainerMenu {
    public static final int GRID = 9;
    public static final int MAIN_X = 8;
    public static final int MAIN_Y = 18;
    public static final int PLAYER_INV_X = 39;
    public static final int PLAYER_INV_Y = 196;
    public static final int HOTBAR_Y = 254;
    public static final int OUT_X = 206;
    public static final int OUT_Y = 89;
    private final Level world;
    private final Container result;
    private final Player player;
    private final BlockPos blockPos;
    public final ModCraftContainer matrix;

    public ShizukuCraftMenu(int id, Inventory playerInventory, BlockPos pos) {
        super((MenuType<?>)ModMenus.SHIZUKU_CRAFT_TABLE.get(), id);
        this.player = playerInventory.player;
        this.world = playerInventory.player.level();
        this.blockPos = pos;
        ItemStackHandler handler;
        if (this.world.getBlockEntity(pos) instanceof ShizukuCraftTile tile) {
            handler = tile.getInventory();
        } else {
            handler = new ItemStackHandler(81);
        }

        this.result = new ResultContainer();
        this.matrix = new ModCraftContainer(this, handler, 81);
        this.addSlot(new ModCraftResultSlot(this.player, this, this.matrix, this.result, 81, 206, 89));

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(this.matrix, j + i * 9, 8 + j * 18, 18 + i * 18));
            }
        }

        for (int var8 = 0; var8 < 3; var8++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + var8 * 9 + 9, 39 + j * 18, 196 + var8 * 18));
            }
        }

        for (int j = 0; j < 9; j++) {
            this.addSlot(new Slot(playerInventory, j, 39 + j * 18, 254));
        }

        this.slotsChanged(this.matrix);
    }

    public static ShizukuCraftMenu open(int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
        return new ShizukuCraftMenu(windowId, playerInventory, buf.readBlockPos());
    }

    @Override
    public void slotsChanged(@NotNull Container matrix) {
        if (!this.world.isClientSide) {
            Optional<ITierCraftingRecipe> recipe = this.world
                .getRecipeManager()
                .getRecipeFor((RecipeType<ITierCraftingRecipe>)ModRecipeTypes.CRAFTING_TABLE_RECIPE.get(), matrix, this.world);
            if (recipe.isPresent() && !matrix.isEmpty()) {
                ItemStack resultStack = recipe.get().assemble(matrix, this.world.registryAccess());
                this.result.setItem(0, resultStack);
            } else {
                this.result.setItem(0, ItemStack.EMPTY);
            }

            super.slotsChanged(matrix);
        }
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int slotNumber) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotNumber);
        int inSlots = 82;
        int invSlots = 118;
        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (slotNumber == 0) {
                if (!this.moveItemStackTo(itemstack1, inSlots, invSlots, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            } else if (slotNumber >= inSlots && slotNumber < invSlots) {
                if (!this.moveItemStackTo(itemstack1, 1, inSlots, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, inSlots, invSlots, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return player.distanceToSqr(this.blockPos.getX() + 0.5, this.blockPos.getY() + 0.5, this.blockPos.getZ() + 0.5) <= 64.0;
    }
}
