package com.buwan.shizuku.util;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * 掉落清理策略：按玩家是否潜行、范围内掉落物数量决定「是否清掉地上掉落物」，
 * 避免夜冥杀把服务器刷爆。
 */
public class LootDropController {
    private static final double DEFAULT_CLEANUP_RANGE = 50.0;

    public static boolean isPlayerSneaking(Player player) {
        return player.isShiftKeyDown();
    }

    public static void cleanupDrops(Player player, double range) {
        if (!player.level().isClientSide()) {
            ServerLevel level = (ServerLevel)player.level();
            BlockPos playerPos = player.blockPosition();
            AABB bounds = new AABB(
                playerPos.getX() - range,
                playerPos.getY() - range,
                playerPos.getZ() - range,
                playerPos.getX() + range,
                playerPos.getY() + range,
                playerPos.getZ() + range
            );

            for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, bounds)) {
                drop.discard();
            }

            for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, bounds)) {
                orb.discard();
            }
        }
    }

    public static void cleanupDrops(Player player) {
        cleanupDrops(player, 50.0);
    }

    public static boolean conditionalCleanup(Player player, double range) {
        if (isPlayerSneaking(player)) {
            cleanupDrops(player, range);
            return true;
        } else {
            return false;
        }
    }

    public static boolean conditionalCleanup(Player player) {
        return conditionalCleanup(player, 50.0);
    }

    public static int getDropCount(Player player, double range) {
        if (player.level().isClientSide()) {
            return 0;
        } else {
            ServerLevel level = (ServerLevel)player.level();
            BlockPos playerPos = player.blockPosition();
            AABB bounds = new AABB(
                playerPos.getX() - range,
                playerPos.getY() - range,
                playerPos.getZ() - range,
                playerPos.getX() + range,
                playerPos.getY() + range,
                playerPos.getZ() + range
            );
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, bounds);
            List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, bounds);
            return drops.size() + orbs.size();
        }
    }
}
