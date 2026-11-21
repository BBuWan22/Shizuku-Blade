package com.buwan.shizuku.network;

import com.buwan.shizuku.client.TimeStopShaderManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class TimeStopShaderPacket {
    private final boolean activate;
    private final float strength;

    public TimeStopShaderPacket(boolean activate, float strength) {
        this.activate = activate;
        this.strength = strength;
    }

    public TimeStopShaderPacket(boolean activate) {
        this(activate, activate ? 1.0F : 0.0F);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.activate);
        buf.writeFloat(this.strength);
    }

    public static TimeStopShaderPacket decode(FriendlyByteBuf buf) {
        return new TimeStopShaderPacket(buf.readBoolean(), buf.readFloat());
    }

    public void handle(Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TimeStopShaderManager.syncFromServer(this.activate, this.strength)));
        ctx.get().setPacketHandled(true);
    }
}
