package com.buwan.shizuku.network;

import com.buwan.shizuku.client.SynthwaveShaderManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class SynthwaveShaderPacket {
    private final boolean activate;

    public SynthwaveShaderPacket(boolean activate) {
        this.activate = activate;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.activate);
    }

    public static SynthwaveShaderPacket decode(FriendlyByteBuf buf) {
        return new SynthwaveShaderPacket(buf.readBoolean());
    }

    public void handle(Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (this.activate) {
                SynthwaveShaderManager.startShader();
            } else {
                SynthwaveShaderManager.stopShader();
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
