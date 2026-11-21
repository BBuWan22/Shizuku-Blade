package com.buwan.shizuku.block;

import com.buwan.shizuku.tile.ShizukuCraftTile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 雫工作台方块：右键打开 9x9 合成界面。
 *
 * - use()：服务端打开菜单（ShizukuCraftMenu），客户端不做事；
 * - onRemove()：被破坏时把 BlockEntity 里的物品全部掉出来（避免吞材料）；
 * - newBlockEntity()：创建 ShizukuCraftTile。
 */
public class ShizukuCraftTableBlock extends Block implements EntityBlock {
    public ShizukuCraftTableBlock(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    public InteractionResult use(
        @NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit
    ) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof ShizukuCraftTile table) {
            NetworkHooks.openScreen(serverPlayer, table, pos);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof ShizukuCraftTile table) {
            ItemStackHandler handler = table.getInventory();

            for (int i = 0; i < handler.getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i));
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ShizukuCraftTile(pos, state);
    }
}
