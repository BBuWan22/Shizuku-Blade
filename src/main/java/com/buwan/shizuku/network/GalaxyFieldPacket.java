package com.buwan.shizuku.network;

import com.buwan.shizuku.client.GalaxyFieldRenderer;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * 服务端 -> 客户端：同步星河领域的开启/关闭（带圆心、半径、持续时间）。
 * 两个构造器分别对应「开启」与「仅带 UUID 的关闭」。
 */
public class GalaxyFieldPacket {
    private final UUID playerId;
    private final double x;
    private final double y;
    private final double z;
    private final float radius;
    private final int duration;
    private final boolean remove;

    public GalaxyFieldPacket(UUID playerId, Vec3 center, float radius, int duration) {
        this.playerId = playerId;
        this.x = center.x;
        this.y = center.y;
        this.z = center.z;
        this.radius = radius;
        this.duration = duration;
        this.remove = false;
    }

    public GalaxyFieldPacket(UUID playerId) {
        this.playerId = playerId;
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
        this.radius = 0.0F;
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
            buf.writeFloat(this.radius);
            buf.writeInt(this.duration);
        }
    }

    public static GalaxyFieldPacket decode(FriendlyByteBuf buf) {
        UUID playerId = buf.readUUID();
        boolean remove = buf.readBoolean();
        if (remove) {
            return new GalaxyFieldPacket(playerId);
        } else {
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float radius = buf.readFloat();
            int duration = buf.readInt();
            return new GalaxyFieldPacket(playerId, new Vec3(x, y, z), radius, duration);
        }
    }

    public void handle(Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (this.remove) {
                GalaxyFieldRenderer.removeZone(this.playerId);
            } else {
                GalaxyFieldRenderer.addZone(this.playerId, new Vec3(this.x, this.y, this.z), this.radius, this.duration);
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
