package com.buwan.shizuku.sa;

import com.buwan.shizuku.util.DamageCalculator;
import java.util.List;
import mods.flammpfeil.slashblade.SlashBlade.RegistryEvents;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.event.SlashBladeEvent.DoSlashEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;

/**
 * 「星河弧光」：先放一道放大的巨型刀光，再依次发射若干道气刃（fireQiSlash）。
 */
public class GalaxyArc {
    private static final int STAR_COLOR = 9662683;
    private static final float GIANT_SIZE = 3.0F;
    private static final float GIANT_SPEED = 1.0F;
    private static final int GIANT_LIFETIME = 20;
    private static final float GIANT_DAMAGE_RATIO = 3.0F;
    private static final float[] QI_ROLLS = new float[]{60.0F, -60.0F, 90.0F, 60.0F, -60.0F, 90.0F};
    private static final float QI_SIZE = 2.2F;
    private static final int QI_LIFETIME = 20;
    private static final float QI_DAMAGE_RATIO = 1.5F;
    private static final float QI_FORWARD = 0.8F;
    private static final double QI_RADIUS = 6.0;

    private GalaxyArc() {
    }

    public static void fireGiantDaoguang(LivingEntity entity) {
        if (!entity.level().isClientSide()) {
            if (entity instanceof Player player) {
                ServerLevel level = (ServerLevel)entity.level();
                int color = player.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).map(ISlashBladeState::getColorCode).orElse(9662683);
                Vec3 dir = player.getLookAngle();
                Vec3 pos = player.position().add(0.0, player.getEyeHeight() * 0.75 + 0.4, 0.0).add(player.getLookAngle().scale(0.3));
                EntityDrive drive = new EntityDrive(RegistryEvents.Drive, level);
                drive.setPos(pos.x, pos.y, pos.z);
                drive.setDamage(3.0);
                drive.setSpeed(1.0F);
                drive.shoot(dir.x, dir.y, dir.z, 1.0F, 0.0F);
                drive.setOwner(player);
                drive.setRotationRoll(0.0F);
                drive.setColor(color);
                drive.setIsCritical(true);
                drive.setKnockBack(KnockBacks.cancel);
                drive.setLifetime(20.0F);
                drive.setBaseSize(3.0F);
                player.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).ifPresent(rank -> drive.setRank(rank.getRankLevel(level.getGameTime())));
                level.addFreshEntity(drive);
            }
        }
    }

    public static void fireQiSlash(LivingEntity entity, int index) {
        if (!entity.level().isClientSide()) {
            if (entity instanceof Player player) {
                if (index >= 0 && index < QI_ROLLS.length) {
                    ServerLevel level = (ServerLevel)entity.level();
                    int color = player.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).map(ISlashBladeState::getColorCode).orElse(9662683);
                    float damage = DamageCalculator.calculateRefinedDamage(player, 1.0F) * 1.5F;
                    Vec3 pos = player.position().add(0.0, player.getEyeHeight() * 0.75 + 0.3, 0.0).add(player.getLookAngle().scale(1.100000011920929));
                    spawnSlashEffect(player, level, QI_ROLLS[index], pos, color, 0.0F, 2.2F, 20, index == 5, KnockBacks.smash);
                    applyQiDamage(player, level, pos, damage);
                }
            }
        }
    }

    private static void applyQiDamage(Player player, ServerLevel level, Vec3 center, float damage) {
        List<LivingEntity> targets = level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(center.x - 6.0, center.y - 3.0, center.z - 6.0, center.x + 6.0, center.y + 3.0, center.z + 6.0),
            e -> e != player && !(e instanceof Player) && e.isAlive() && !e.isRemoved() && e.distanceToSqr(center) <= 36.0
        );
        if (!targets.isEmpty()) {
            for (LivingEntity target : targets) {
                target.invulnerableTime = 0;
                target.hurt(player.damageSources().indirectMagic(null, player), damage);
            }
        }
    }

    private static void spawnSlashEffect(
        Player player, ServerLevel level, float roll, Vec3 pos, int color, float damage, float size, int lifetime, boolean crit, KnockBacks knockback
    ) {
        ItemStack blade = player.getMainHandItem();
        LazyOptional<ISlashBladeState> stateOpt = blade.getCapability(ItemSlashBlade.BLADESTATE);
        if (stateOpt.isPresent()) {
            DoSlashEvent event = new DoSlashEvent(
                blade, (ISlashBladeState)stateOpt.orElseThrow(NullPointerException::new), player, roll, crit, damage, knockback
            );
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                EntitySlashEffect se = new EntitySlashEffect(RegistryEvents.SlashEffect, level);
                se.setPos(pos.x, pos.y, pos.z);
                se.setOwner(event.getUser());
                se.setRotationRoll(event.getRoll());
                se.setYRot(player.getYRot());
                se.setXRot(0.0F);
                se.setColor(color);
                se.setMute(true);
                se.setIsCritical(event.isCritical());
                se.setDamage(event.getDamage());
                se.setKnockBack(event.getKnockback());
                se.setBaseSize(size);
                se.setLifetime(lifetime);
                player.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).ifPresent(rank -> se.setRank(rank.getRankLevel(level.getGameTime())));
                level.addFreshEntity(se);
            }
        }
    }
}
