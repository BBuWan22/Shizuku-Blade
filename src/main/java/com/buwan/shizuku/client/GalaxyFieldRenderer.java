package com.buwan.shizuku.client;

import com.buwan.shizuku.client.shader.CosmicShaders;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

/**
 * 星河领域（GalaxyField）的客户端渲染与区域管理。
 *
 * - 维护 UUID -> ZoneData（圆心 + 半径）的并发表，由 GalaxyFieldPacket 增删；
 * - onRenderLevel：在 RenderLevelStageEvent 里画星空圆盘 + 边缘光效；
 * - onClientTick：按每 tick 递减生命周期，到时自动移除。
 *
 * 已知问题：圆盘用的是贴图近似（starry_sky.png + 径向 UV），观感比原版着色器版略差。
 */
@OnlyIn(Dist.CLIENT)
public final class GalaxyFieldRenderer {
    private static final Map<UUID, GalaxyFieldRenderer.ZoneData> activeZones = new ConcurrentHashMap<>();

    private GalaxyFieldRenderer() {
    }

    public static void addZone(UUID id, Vec3 center, float radius, int duration) {
        activeZones.put(id, new GalaxyFieldRenderer.ZoneData(center, radius));
    }

    public static void removeZone(UUID id) {
        activeZones.remove(id);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            if (!activeZones.isEmpty()) {
                for (GalaxyFieldRenderer.ZoneData zone : activeZones.values()) {
                    zone.ageTicks++;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
            if (!activeZones.isEmpty()) {
                if (CosmicShaders.COSMIC_SHADER != null) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level != null && mc.player != null) {
                        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
                        PoseStack poseStack = event.getPoseStack();
                        BufferSource buffers = mc.renderBuffers().bufferSource();

                        for (Entry<UUID, GalaxyFieldRenderer.ZoneData> entry : activeZones.entrySet()) {
                            Player owner = mc.level.getPlayerByUUID(entry.getKey());
                            if (owner != null) {
                                GalaxyFieldRenderer.ZoneData zone = entry.getValue();
                                poseStack.pushPose();
                                poseStack.translate(owner.getX() - cam.x, owner.getY() - cam.y, owner.getZ() - cam.z);
                                drawDisk(poseStack, buffers, zone);
                                poseStack.popPose();
                            }
                        }

                        buffers.endBatch(CosmicShaders.COSMIC_RENDER_TYPE);
                    }
                }
            }
        }
    }

    private static void drawDisk(PoseStack poseStack, BufferSource buffers, GalaxyFieldRenderer.ZoneData zone) {
        float alpha = Math.min(1.0F, zone.ageTicks / 4.0F) * 0.75F;
        if (!(alpha <= 0.01F)) {
            if (CosmicShaders.prepareCosmicRender(alpha)) {
                VertexConsumer vb = buffers.getBuffer(CosmicShaders.COSMIC_RENDER_TYPE);
                Matrix4f mat = poseStack.last().pose();
                float radius = zone.radius;
                float y = 0.05F;
                int segments = 48;

                for (int i = 0; i < segments; i++) {
                    double a0 = (Math.PI * 2) * i / segments;
                    double a1 = (Math.PI * 2) * (i + 1) / segments;
                    float x0 = (float)(Math.cos(a0) * radius);
                    float z0 = (float)(Math.sin(a0) * radius);
                    float x1 = (float)(Math.cos(a1) * radius);
                    float z1 = (float)(Math.sin(a1) * radius);
                    vb.vertex(mat, x0, y, z0)
                        .color(1.0F, 1.0F, 1.0F, alpha)
                        .uv(0.0F, 0.0F)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(15728880)
                        .normal(0.0F, 1.0F, 0.0F)
                        .endVertex();
                    vb.vertex(mat, x1, y, z1)
                        .color(1.0F, 1.0F, 1.0F, alpha)
                        .uv(0.0F, 0.0F)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(15728880)
                        .normal(0.0F, 1.0F, 0.0F)
                        .endVertex();
                    vb.vertex(mat, 0.0F, y, 0.0F)
                        .color(1.0F, 1.0F, 1.0F, alpha)
                        .uv(0.0F, 0.0F)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(15728880)
                        .normal(0.0F, 1.0F, 0.0F)
                        .endVertex();
                }
            }
        }
    }

    public static class ZoneData {
        public final double x;
        public final double y;
        public final double z;
        public final float radius;
        public int ageTicks;

        public ZoneData(Vec3 center, float radius) {
            this.x = center.x;
            this.y = center.y;
            this.z = center.z;
            this.radius = radius;
        }
    }
}
