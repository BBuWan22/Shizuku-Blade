package com.buwan.shizuku.network;

import com.buwan.shizuku.client.TimeStopRenderer;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class TimeStopPacket {
    private final UUID playerId;
    private final double x;
    private final double y;
    private final double z;
    private final double radius;
    private final int duration;
    private final boolean remove;

    public TimeStopPacket(UUID playerId, Vec3 center, double radius, int duration) {
        this.playerId = playerId;
        this.x = center.x;
        this.y = center.y;
        this.z = center.z;
        this.radius = radius;
        this.duration = duration;
        this.remove = false;
    }

    public TimeStopPacket(UUID playerId) {
        this.playerId = playerId;
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
        this.radius = 0.0;
        this.duration = 0;
        this.remove = true;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.playerId);
        buf.writeBoolean(this.remove);
        if (!this.remove) {
            buf.writeDouble(this.x);
            buf.writeDouble(this.y);
            buf.writeDouble(this.z);
            buf.writeDouble(this.radius);
            buf.writeInt(this.duration);
        }
    }

    public static TimeStopPacket decode(FriendlyByteBuf buf) {
        UUID playerId = buf.readUUID();
        boolean remove = buf.readBoolean();
        if (remove) {
            return new TimeStopPacket(playerId);
        } else {
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            double radius = buf.readDouble();
            int duration = buf.readInt();
            return new TimeStopPacket(playerId, new Vec3(x, y, z), radius, duration);
        }
    }

    public void handle(Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (this.remove) {
                TimeStopRenderer.removeTimeStopZone(this.playerId);
            } else {
                Vec3 center = new Vec3(this.x, this.y, this.z);
                TimeStopRenderer.addTimeStopZone(this.playerId, center, this.radius, this.duration);
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
