package com.buwan.shizuku.client;

import com.buwan.shizuku.network.FieldActivatePacket;
import com.buwan.shizuku.network.PacketHandler;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent.Key;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 「星河领域」的按键输入：默认绑定到某个键，按下后发 FieldActivatePacket 给服务端。
 *
 * 只在客户端 tick 里读按键，且要求玩家手持宇宙刀，避免误触发。
 */
@OnlyIn(Dist.CLIENT)
public final class FieldActivateInput {
    private FieldActivateInput() {
    }

    @SubscribeEvent
    public static void onKey(Key event) {
        if (event.getAction() == 1) {
            if (event.getKey() == 88) {
                if ((event.getModifiers() & 1) != 0) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        ItemStack held = mc.player.getMainHandItem();
                        if (held.getItem() instanceof ItemSlashBlade) {
                            String key = held.getCapability(ItemSlashBlade.BLADESTATE).map(ISlashBladeState::getTranslationKey).orElse("");
                            if ("item.shizuku.mosmicblade".equals(key)) {
                                PacketHandler.INSTANCE.sendToServer(new FieldActivatePacket());
                            }
                        }
                    }
                }
            }
        }
    }
}
