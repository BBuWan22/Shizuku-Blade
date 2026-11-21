package com.buwan.shizuku.network;

import com.buwan.shizuku.sa.GalaxyField;
import java.util.function.Supplier;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * 客户端 -> 服务端：请求开关「星河领域」。空包（无字段）。
 */
public class FieldActivatePacket {
    public void encode(FriendlyByteBuf buf) {
    }

    public static FieldActivatePacket decode(FriendlyByteBuf buf) {
        return new FieldActivatePacket();
    }

    public void handle(Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                String key = player.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).map(ISlashBladeState::getTranslationKey).orElse("");
                if ("item.shizuku.mosmicblade".equals(key)) {
                    GalaxyField.toggleField(player);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
