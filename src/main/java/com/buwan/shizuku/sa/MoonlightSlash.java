package com.buwan.shizuku.sa;

import com.buwan.shizuku.util.DamageCalculator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SakuraEnd;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 「月光斩」：周期性的月光斩，用 activeSlashes 记录每个玩家的状态与间隔，
 * playerTick 里推进，clearPlayerState 在登出/死亡时清理。
 */
public class MoonlightSlash {
    private static final int MOONLIGHT_COLOR = 14743295;
    private static final int SLASH_INTERVAL = 8;
    private static final Map<UUID, MoonlightSlash.SlashData> activeSlashes = new ConcurrentHashMap<>();

    public static void doMoonlightSlash(LivingEntity entity) {
        if (!entity.level().isClientSide()) {
            if (entity instanceof Player player) {
                ServerLevel level = (ServerLevel)entity.level();
                float damage = DamageCalculator.calculateRefinedDamage(entity, 1.0F);
                int colorCode = entity.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).map(ISlashBladeState::getColorCode).orElse(14743295);
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.3F);
                MoonlightSlash.SlashData slashData = new MoonlightSlash.SlashData(damage, colorCode);
                activeSlashes.put(player.getUUID(), slashData);
                executeSlash(player, slashData, 0);
                slashData.slashIndex++;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (!event.side.isClient() && event.phase == Phase.END) {
            Player player = event.player;
            UUID uuid = player.getUUID();
            MoonlightSlash.SlashData slashData = activeSlashes.get(uuid);
            if (slashData != null) {
                slashData.tickCounter++;
                if (slashData.tickCounter % 8 == 0 && slashData.slashIndex < 5) {
                    executeSlash(player, slashData, slashData.slashIndex);
                    slashData.slashIndex++;
                }

                if (slashData.slashIndex >= 5) {
                    activeSlashes.remove(uuid);
                }
            }
        }
    }

    private static void executeSlash(Player player, MoonlightSlash.SlashData data, int slashIndex) {
        ServerLevel level = (ServerLevel)player.level();
        switch (slashIndex) {
            case 0:
                SakuraEnd.doSlash(player, 15.0F, data.colorCode, new Vec3(0.0, 0.1, 0.0), true, false, data.baseDamage * 0.5F, KnockBacks.cancel);
                break;
            case 1:
                SakuraEnd.doSlash(player, -15.0F, data.colorCode, new Vec3(0.0, 0.1, 0.0), true, false, data.baseDamage * 0.5F, KnockBacks.cancel);
                break;
            case 2:
                SakuraEnd.doSlash(player, 0.0F, data.colorCode, new Vec3(0.0, 0.2, 0.0), true, true, data.baseDamage * 0.7F, KnockBacks.cancel);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.2F);
                break;
            case 3:
                SakuraEnd.doSlash(player, 90.0F, data.colorCode, new Vec3(0.0, 0.2, 0.0), true, false, data.baseDamage * 0.6F, KnockBacks.cancel);
                break;
            case 4:
                SakuraEnd.doSlash(player, 45.0F, data.colorCode, new Vec3(0.0, 0.3, 0.0), false, true, data.baseDamage * 1.2F, KnockBacks.smash);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.5F, 0.8F);
        }
    }

    public static void clearPlayerState(UUID playerUUID) {
        activeSlashes.remove(playerUUID);
    }

    private static class SlashData {
        int tickCounter = 0;
        int slashIndex = 0;
        final float baseDamage;
        final int colorCode;

        SlashData(float damage, int color) {
            this.baseDamage = damage;
            this.colorCode = color;
        }
    }
}
