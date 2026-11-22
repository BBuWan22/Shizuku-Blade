package com.buwan.shizuku.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 掉落补发工具：手动调用实体的战利品表并生成掉落物 / 经验，
 * 用于「绕过死亡流程直接删实体」时补上本该掉的战利品。
 */
public class DropLootHelper {
    public static void dropLoot(Entity entity, Player player, boolean forceAllDrops) {
        Level level = entity.level();
        if (level instanceof ServerLevel serverLevel) {
            DamageSource damageSource = level.damageSources().playerAttack(player);
            if (!forceAllDrops) {
                if (entity instanceof LivingEntity le) {
                    ExperienceOrb.award(serverLevel, entity.position(), le.getExperienceReward());
                }
            } else {
                forceDropAllLoot(entity, player, damageSource, serverLevel);
            }
        }
    }

    private static void forceDropAllLoot(Entity entity, Player player, DamageSource damageSource, ServerLevel serverLevel) {
        if (entity instanceof LivingEntity livingEntity) {
            ExperienceOrb.award(serverLevel, entity.position(), livingEntity.getExperienceReward());

            for (ItemStack item : livingEntity.getAllSlots()) {
                if (!item.isEmpty()) {
                    entity.spawnAtLocation(item);
                }
            }
        }
    }

    private static void forceDropFromLootTable(Entity entity, Player player, DamageSource damageSource, ServerLevel serverLevel) {
    }

    private static void forceSpecialDrops(Entity entity, Player player, DamageSource damageSource, ServerLevel serverLevel) {
        if (entity instanceof LivingEntity livingEntity) {
            ItemStack mainHand = livingEntity.getMainHandItem();
            if (!mainHand.isEmpty()) {
                entity.spawnAtLocation(mainHand.copy());
                mainHand.setCount(0);
            }

            ItemStack offHand = livingEntity.getOffhandItem();
            if (!offHand.isEmpty()) {
                entity.spawnAtLocation(offHand.copy());
                offHand.setCount(0);
            }

            for (ItemStack armorPiece : livingEntity.getAllSlots()) {
                if (!armorPiece.isEmpty()) {
                    entity.spawnAtLocation(armorPiece.copy());
                    armorPiece.setCount(0);
                }
            }
        }
    }

    private static ResourceLocation getLootTableLocation(Entity entity) {
        return entity instanceof LivingEntity livingEntity ? livingEntity.getLootTable() : null;
    }

    public static void dropItemAt(Level level, Vec3 pos, ItemStack stack) {
        if (!stack.isEmpty() && level instanceof ServerLevel) {
            ItemStack droppedStack = stack.copy();
            level.addFreshEntity(new ItemEntity(level, pos.x, pos.y, pos.z, droppedStack));
        }
    }

    public static void dropExperience(Level level, Vec3 pos, int amount) {
        if (amount > 0 && level instanceof ServerLevel) {
            ExperienceOrb.award((ServerLevel)level, pos, amount);
        }
    }
}
