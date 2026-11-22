package com.buwan.shizuku.client;

import com.buwan.shizuku.registry.SASlashArtRegistry;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * 「夜冥杀」蓄力阶段的客户端处理：读取前置的蓄力状态，更新本地玩家身上的
 * 蓄力特效与提示，蓄满后由服务端触发真正的 SA。
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
public class YmsaChargeHandler {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("shizuku/charge");
    private static boolean isChargingYmsa = false;
    private static long lastLogMs = 0L;
    private static String lastLogged = "";
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                ItemStack mainHand = mc.player.getMainHandItem();
                if (!(mainHand.getItem() instanceof ItemSlashBlade)) {
                    if (isChargingYmsa) {
                        SynthwaveShaderManager.stopShader();
                        isChargingYmsa = false;
                    }
                } else {
                    boolean isUsing = mc.player.isUsingItem();
                    int usingTicks = mc.player.getTicksUsingItem();
                    mainHand.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
                        boolean isYmsa = state.getSlashArts().equals(SASlashArtRegistry.Ymsa.get());
                        logState(isUsing, usingTicks, isYmsa);
                        // 必须按住超过阈值才算「蓄力中」：轻点不会出特效
                        if (isUsing && isYmsa && usingTicks > 0) {
                            if (!isChargingYmsa) {
                                SynthwaveShaderManager.startShader();
                                isChargingYmsa = true;
                            }
                        } else if (isChargingYmsa && !isUsing) {
                            // 松手立刻收（不再做「连续 N tick 才关」的迟滞：那正是「连点等效长按」的成因）
                            SynthwaveShaderManager.stopShader();
                            isChargingYmsa = false;
                        }
                    });
                }
            }
        }
    }

    /** 诊断：状态变化立刻打，稳定时每 1 秒打一次（排查「按住时到底是什么在抖」）。 */
    private static void logState(boolean using, int ticks, boolean ymsa) {
        long now = System.currentTimeMillis();
        String sig = using + "/" + ymsa + "/" + isChargingYmsa;
        if (sig.equals(lastLogged) && now - lastLogMs < 1000L) {
            return;
        }
        lastLogged = sig;
        lastLogMs = now;
        LOGGER.info("[shizuku-state] using={} ticks={} ymsa={} charging={}",
                using, ticks, ymsa, isChargingYmsa);
    }
}
