package com.buwan.shizuku.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Helper {
    public static List<Entity> getAllEntities_Method1(Level level) {
        List<Entity> entities = new ArrayList<>();
        if (level instanceof ServerLevel serverLevel) {
            Iterable<Entity> allEntities = serverLevel.getAllEntities();
            allEntities.forEach(entities::add);
        }

        return entities;
    }

    public static List<Entity> getAllEntities_Method2(Level level) {
        List<Entity> entities = new ArrayList<>();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getAllEntities().forEach(entities::add);
        }

        return entities;
    }

    public static List<Entity> getAllEntities_Method3(Level level) {
        List<Entity> entities = new ArrayList<>();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getAllEntities().forEach(entities::add);
        }

        return entities;
    }

    public static List<Entity> getEntitiesInRange_Method1(Level level, Vec3 center, double radius) {
        AABB searchArea = new AABB(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);
        return level.getEntitiesOfClass(Entity.class, searchArea, entity -> {
            double distance = entity.position().distanceTo(center);
            return distance <= radius;
        });
    }

    public static <T extends Entity> List<T> getEntitiesInRange_Method2(Level level, Class<T> entityClass, Vec3 center, double radius) {
        AABB searchArea = new AABB(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);
        return level.getEntitiesOfClass(entityClass, searchArea, entity -> {
            double distance = entity.position().distanceTo(center);
            return distance <= radius;
        });
    }

    public static List<LivingEntity> getLivingEntitiesAroundPlayer(Player player, double radius) {
        return getEntitiesInRange_Method2(player.level(), LivingEntity.class, player.position(), radius);
    }

    public static List<Mob> getHostileMobsInRange(Level level, Vec3 center, double radius) {
        return getEntitiesInRange_Method2(level, Mob.class, center, radius)
            .stream()
            .filter(mob -> mob.getTarget() != null || mob instanceof Enemy)
            .collect(Collectors.toList());
    }

    public static List<Player> getPlayersInRange(Level level, Vec3 center, double radius) {
        return getEntitiesInRange_Method2(level, Player.class, center, radius);
    }

    public static List<Entity> getEntitiesAroundEntity(Entity centerEntity, double radius) {
        return getEntitiesInRange_Method1(centerEntity.level(), centerEntity.position(), radius)
            .stream()
            .filter(entity -> !entity.equals(centerEntity))
            .collect(Collectors.toList());
    }

    public static List<Entity> getEntitiesInBox(Level level, BlockPos corner1, BlockPos corner2) {
        AABB box = new AABB(corner1, corner2);
        return level.getEntitiesOfClass(Entity.class, box, entity -> true);
    }

    public static List<Entity> getEntitiesInRangeWithPredicate(Level level, Vec3 center, double radius, Predicate<Entity> predicate) {
        return getEntitiesInRange_Method1(level, center, radius).stream().filter(predicate).collect(Collectors.toList());
    }

    public static List<Entity> getAllEntities(Level level) {
        return getAllEntities_Method2(level);
    }
}
