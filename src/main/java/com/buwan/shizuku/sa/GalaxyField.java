package com.buwan.shizuku.sa;

import com.buwan.shizuku.network.GalaxyFieldPacket;
import com.buwan.shizuku.network.PacketHandler;
import com.buwan.shizuku.util.DamageCalculator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 「星河领域」的 SA 本体：服务端维护领域状态并每 tick 结算（对范围内敌人造成周期伤害），
 * 同时通过 GalaxyFieldPacket 同步给客户端渲染。
 */
public class GalaxyField {
    public static final float RADIUS = 8.0F;
    private static final int DAMAGE_TICKS = 4;
    private static final float DAMAGE_RATIO = 0.5F;
    private static final Map<UUID, GalaxyField.FieldData> activeFields = new ConcurrentHashMap<>();

    private GalaxyField() {
    }

    public static void toggleField(LivingEntity entity) {
        if (!entity.level().isClientSide()) {
            if (entity instanceof Player player) {
                ServerLevel level = (ServerLevel)entity.level();
                GalaxyField.FieldData old = activeFields.remove(player.getUUID());
                if (old != null) {
                    PacketHandler.sendToNearby(new GalaxyFieldPacket(player.getUUID()), (ServerPlayer)player, 64.0);
                } else {
                    activeFields.put(player.getUUID(), new GalaxyField.FieldData(level, player));
                    PacketHandler.sendToNearby(new GalaxyFieldPacket(player.getUUID(), player.position(), 8.0F, -1), (ServerPlayer)player, 64.0);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 0.7F);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase == Phase.END) {
            if (!activeFields.isEmpty()) {
                activeFields.entrySet().removeIf(entry -> {
                    GalaxyField.FieldData data = entry.getValue();
                    if (data.owner != null && data.owner.isAlive() && data.owner.level() == data.level && isHoldingMosmicBlade(data.owner)) {
                        data.damageTicks++;
                        if (data.damageTicks % 4 == 0) {
                            applyDamage(data);
                        }

                        return false;
                    } else {
                        PacketHandler.sendToNearby(new GalaxyFieldPacket(entry.getKey()), (ServerPlayer)data.owner, 64.0);
                        return true;
                    }
                });
            }
        }
    }

    private static boolean isHoldingMosmicBlade(Player player) {
        ItemStack held = player.getMainHandItem();
        return !(held.getItem() instanceof ItemSlashBlade)
            ? false
            : held.getCapability(ItemSlashBlade.BLADESTATE).map(s -> "item.shizuku.mosmicblade".equals(s.getTranslationKey())).orElse(false);
    }

    private static void applyDamage(GalaxyField.FieldData data) {
        ServerLevel level = data.level;
        double cx = data.owner.getX();
        double cy = data.owner.getY();
        double cz = data.owner.getZ();
        List<LivingEntity> targets = level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(cx - 8.0, cy - 4.0, cz - 8.0, cx + 8.0, cy + 8.0, cz + 8.0),
            target -> !(target instanceof Player)
                && target.isAlive()
                && !target.isRemoved()
                && target.distanceToSqr(cx, target.getY(), cz) <= 64.0
                && !(target instanceof OwnableEntity owned && data.owner.getUUID().equals(owned.getOwnerUUID()))
        );
        if (!targets.isEmpty()) {
            float damage = DamageCalculator.calculateRefinedDamage(data.owner, 1.0F) * 0.5F;

            for (LivingEntity target : targets) {
                target.invulnerableTime = 0;
                target.hurt(target.damageSources().magic(), damage);
                target.setLastHurtByMob(data.owner);
            }
        }
    }

    public static void clearAll() {
        activeFields.clear();
    }

    private static class FieldData {
        final ServerLevel level;
        final Player owner;
        int damageTicks;

        FieldData(ServerLevel level, Player owner) {
            this.level = level;
            this.owner = owner;
        }
    }
}
