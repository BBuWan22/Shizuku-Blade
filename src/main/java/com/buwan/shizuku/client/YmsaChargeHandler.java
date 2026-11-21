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
    private static boolean isChargingYmsa = false;

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
                        if (isUsing && isYmsa && usingTicks > 0) {
                            if (!isChargingYmsa) {
                                SynthwaveShaderManager.startShader();
                                isChargingYmsa = true;
                            }
                        } else if (isChargingYmsa) {
                            SynthwaveShaderManager.stopShader();
                            isChargingYmsa = false;
                        }
                    });
                }
            }
        }
    }
}
