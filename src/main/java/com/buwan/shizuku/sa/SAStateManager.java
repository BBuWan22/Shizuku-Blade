package com.buwan.shizuku.sa;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class SAStateManager {
    private static final Map<UUID, ResourceLocation> playerDimensions = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            Player player = event.getEntity();
            ResourceLocation dimension = player.level().dimension().location();
            playerDimensions.put(player.getUUID(), dimension);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            Player player = event.getEntity();
            UUID uuid = player.getUUID();
            playerDimensions.remove(uuid);
            clearAllSAStates(uuid);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (event.side != LogicalSide.CLIENT && event.phase == Phase.START) {
            Player player = event.player;
            UUID uuid = player.getUUID();
            ResourceLocation currentDimension = player.level().dimension().location();
            ResourceLocation recordedDimension = playerDimensions.get(uuid);
            if (recordedDimension != null && !currentDimension.equals(recordedDimension)) {
                clearAllSAStates(uuid);
                playerDimensions.put(uuid, currentDimension);
            } else if (recordedDimension == null) {
                playerDimensions.put(uuid, currentDimension);
            }
        }
    }

    private static void clearAllSAStates(UUID playerUUID) {
        TornadoSlash.clearPlayerState(playerUUID);
        MoonlightSlash.clearPlayerState(playerUUID);
        TsubameGaeshi.clearPlayerState(playerUUID);
        TeleportSlash.clearPlayerState(playerUUID);
        UnlimitedBladeWorks.clearPlayerState(playerUUID);
        TimeSlash.clearPlayerState(playerUUID);
        VoidPrison.clearPlayerState(playerUUID);
        Yingsa.clearPlayerState(playerUUID);
        Ymsa.clearPlayerState(playerUUID);
    }
}
