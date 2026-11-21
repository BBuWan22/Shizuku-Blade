package com.buwan.shizuku.sa;

import com.buwan.shizuku.network.PacketHandler;
import com.buwan.shizuku.network.VoidPrisonPacket;
import com.buwan.shizuku.util.DamageCalculator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import mods.flammpfeil.slashblade.SlashBlade.RegistryEvents;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 「虚空囚牢」：把范围内敌人困在球体内并持续伤害，
 * 区域状态通过 VoidPrisonPacket 同步给客户端画线框。
 */
public class VoidPrison {
    private static final int PRISON_COLOR = 13382451;
    private static final double PRISON_RADIUS = 4.0;
    private static final int PRISON_DURATION = 80;
    private static final int WALL_COUNT = 12;
    private static final Map<UUID, VoidPrison.PrisonData> activePrisons = new ConcurrentHashMap<>();

    public static void doVoidPrison(LivingEntity entity) {
        if (!entity.level().isClientSide()) {
            if (entity instanceof Player player) {
                ServerLevel level = (ServerLevel)entity.level();
                Vec3 center = entity.position().add(entity.getLookAngle().scale(5.0)).add(0.0, 1.0, 0.0);
                List<LivingEntity> prisoners = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(center.x - 4.0, center.y - 2.0, center.z - 4.0, center.x + 4.0, center.y + 4.0, center.z + 4.0),
                    e -> e != entity && e.isAlive()
                );
                if (!prisoners.isEmpty()) {
                    List<EntitySlashEffect> walls = createPrisonWalls(player, center, 13382451, level);

                    for (LivingEntity prisoner : prisoners) {
                        prisoner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 10, false, false));
                        prisoner.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 5, false, false));
                    }

                    VoidPrison.PrisonData prisonData = new VoidPrison.PrisonData(center, prisoners, walls);
                    activePrisons.put(player.getUUID(), prisonData);
                    level.playSound(null, center.x, center.y, center.z, SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 2.0F, 0.6F);
                    if (player instanceof ServerPlayer serverPlayer) {
                        VoidPrisonPacket packet = new VoidPrisonPacket(player.getUUID(), center, 4.0, 100);
                        PacketHandler.sendToNearby(packet, serverPlayer, 64.0);
                    }

                    createPrisonFormationParticles(level, center);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (!event.side.isClient() && event.phase == Phase.END) {
            Player player = event.player;
            UUID uuid = player.getUUID();
            VoidPrison.PrisonData prisonData = activePrisons.get(uuid);
            if (prisonData != null) {
                prisonData.tickCounter++;
                ServerLevel level = (ServerLevel)player.level();
                if (!prisonData.exploded && prisonData.tickCounter % 10 == 0) {
                    createPrisonParticles(level, prisonData.center);
                }

                if (!prisonData.exploded && prisonData.tickCounter >= 80) {
                    explodePrison(player, prisonData, level);
                    prisonData.exploded = true;
                }

                if (prisonData.exploded && prisonData.tickCounter >= 100) {
                    activePrisons.remove(uuid);
                }
            }
        }
    }

    private static void explodePrison(Player player, VoidPrison.PrisonData data, ServerLevel level) {
        for (LivingEntity prisoner : data.prisoners) {
            if (prisoner.isAlive() && !prisoner.isRemoved()) {
                float damage = DamageCalculator.calculateRefinedDamage(player, prisoner, 3.0F);
                prisoner.hurt(level.damageSources().playerAttack(player), damage);
            }
        }

        for (int i = 0; i < 100; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double distance = Math.random() * 4.0 * 1.5;
            double x = data.center.x + Math.cos(angle) * distance;
            double z = data.center.z + Math.sin(angle) * distance;
            double y = data.center.y + Math.random() * 4.0;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 5, 0.2, 0.2, 0.2, 0.3);
        }

        for (int i = 0; i < 50; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double distance = Math.random() * 4.0;
            double x = data.center.x + Math.cos(angle) * distance;
            double z = data.center.z + Math.sin(angle) * distance;
            double y = data.center.y + Math.random() * 3.0;
            level.sendParticles(ParticleTypes.LAVA, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        for (EntitySlashEffect wall : data.walls) {
            if (wall.isAlive()) {
                wall.discard();
            }
        }
    }

    private static void createPrisonFormationParticles(ServerLevel level, Vec3 center) {
        for (int i = 0; i < 60; i++) {
            double angle = (Math.PI * 2) * i / 60.0;
            double x = center.x + Math.cos(angle) * 4.0;
            double z = center.z + Math.sin(angle) * 4.0;
            double y = center.y + Math.random() * 4.0;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0.0, 0.0, 0.0, 0.05);
        }
    }

    private static void createPrisonParticles(ServerLevel level, Vec3 center) {
        for (int i = 0; i < 12; i++) {
            double angle = (Math.PI * 2) * i / 12.0;
            double x = center.x + Math.cos(angle) * 4.0;
            double z = center.z + Math.sin(angle) * 4.0;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, center.y + 0.5, z, 1, 0.0, 0.5, 0.0, 0.02);
        }
    }

    private static List<EntitySlashEffect> createPrisonWalls(Player player, Vec3 center, int colorCode, ServerLevel level) {
        List<EntitySlashEffect> walls = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            double angle = (Math.PI * 2) * i / 12.0;
            EntitySlashEffect wall = new EntitySlashEffect(RegistryEvents.SlashEffect, level);
            Vec3 wallPos = center.add(Math.cos(angle) * 4.0, 0.0, Math.sin(angle) * 4.0);
            wall.setPos(wallPos.x, wallPos.y, wallPos.z);
            wall.setOwner(player);
            wall.setYRot((float)Math.toDegrees(angle));
            wall.setRotationRoll(90.0F);
            wall.setColor(colorCode);
            wall.setDamage(0.0);
            wall.setKnockBack(KnockBacks.cancel);
            wall.setMute(true);
            wall.setLifetime(80);
            player.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).ifPresent(rank -> wall.setRank(rank.getRankLevel(level.getGameTime())));
            level.addFreshEntity(wall);
            walls.add(wall);
        }

        return walls;
    }

    public static void clearPlayerState(UUID playerUUID) {
        activePrisons.remove(playerUUID);
    }

    private static class PrisonData {
        final Vec3 center;
        final List<LivingEntity> prisoners;
        final List<EntitySlashEffect> walls;
        int tickCounter;
        boolean exploded;

        PrisonData(Vec3 center, List<LivingEntity> prisoners, List<EntitySlashEffect> walls) {
            this.center = center;
            this.prisoners = prisoners;
            this.walls = walls;
            this.tickCounter = 0;
            this.exploded = false;
        }
    }
}
