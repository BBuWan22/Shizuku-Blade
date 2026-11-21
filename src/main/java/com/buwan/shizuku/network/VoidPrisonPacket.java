package com.buwan.shizuku.network;

import com.buwan.shizuku.client.VoidPrisonRenderer;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * 服务端 -> 客户端：同步虚空囚牢区域（圆心、半径、持续时间）的开启与关闭。
 */
public class VoidPrisonPacket {
    private final UUID playerId;
    private final double x;
    private final double y;
    private final double z;
    private final double radius;
    private final int duration;
    private final boolean remove;

    public VoidPrisonPacket(UUID playerId, Vec3 center, double radius, int duration) {
        this.playerId = playerId;
        this.x = center.x;
        this.y = center.y;
        this.z = center.z;
        this.radius = radius;
        this.duration = duration;
        this.remove = false;
    }

    public VoidPrisonPacket(UUID playerId) {
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

    public static VoidPrisonPacket decode(FriendlyByteBuf buf) {
        UUID playerId = buf.readUUID();
        boolean remove = buf.readBoolean();
        if (remove) {
            return new VoidPrisonPacket(playerId);
        } else {
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            double radius = buf.readDouble();
            int duration = buf.readInt();
            return new VoidPrisonPacket(playerId, new Vec3(x, y, z), radius, duration);
        }
    }

    public void handle(Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (this.remove) {
                VoidPrisonRenderer.removePrisonZone(this.playerId);
            } else {
                Vec3 center = new Vec3(this.x, this.y, this.z);
                VoidPrisonRenderer.addPrisonZone(this.playerId, center, this.radius, this.duration);
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
