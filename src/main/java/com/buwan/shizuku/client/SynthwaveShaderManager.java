package com.buwan.shizuku.client;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 合成波（synthwave）后处理的客户端状态机。
 *
 * - startShader()/stopShader()：由「蓄力」状态（YmsaChargeHandler）与 SA 结束（SAComboRegistry 的 releaseAction）驱动；
 * - render()：在渲染帧末尾用底层的后处理管线跑 synthwave.fsh；
 * - isShaderPackActive()：检测 iris/oculus 光影包，二者同时在跑时让路（否则画面会叠两次）。
 *
 * 淡入与时长上限（2026-11-22，对齐 1.20.1 Fabric 移植版的行为）：
 * - 每次 startShader() 都重置 animationStartTime（与 Fabric 版一致）⇒ 连点右键会重新从头铺开，而不是续着上一次；
 * - U_Fade 由 elapsed 现算（200 ms 延迟 + 250 ms 淡入），不依赖「上一帧」状态，
 *   所以即使触发方逐 tick 抖动也不会卡在某个中间值；
 * - 由「按住右键蓄力」驱动：按住期间一直亮着（扫描会持续铺开，这是设计如此），
 *   松开立即淡出；连点会重新开始一次，不会累积。
 */
@OnlyIn(Dist.CLIENT)
public class SynthwaveShaderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SynthwaveShaderManager.class);
    public static ShaderInstance synthwave;
    private static boolean isSynthwaveEnabled;
    private static long animationStartTime;
    private static final float SPEED_MULTIPLIER = 3.0F;
    /** 诊断开关：开启后每次 start/stop 都打 INFO 日志。 */
    private static final boolean DEBUG = Boolean.getBoolean("shizuku.synthwave.debug");
    /** 淡入淡出时长（毫秒）；不再延迟，避免「点了半天才出来」。 */
    private static final long FADE_MS = 150L;
    /** 期望状态：true = 正在蓄力，特效该亮着（按住右键时一直为 true）。 */
    private static boolean wantEnabled;
    /** 当前强度 0..1，写进 U_Fade。 */
    private static float fade;
    /** 上一帧时间戳，用于按真实时间推进 fade。 */
    private static long lastFadeMs;
    private static final Object SHADER_API = findShaderApi();
    private static final Method SHADER_IN_USE = findShaderInUseMethod();

    private static Object findShaderApi() {
        try {
            boolean iris = ModList.get().isLoaded("iris");
            boolean oculus = ModList.get().isLoaded("oculus");
            if (!iris && !oculus) {
                return null;
            } else {
                String apiClass = iris ? "net.irisshaders.iris.api.v0.IrisApi" : "net.irisshaders.oculus.api.OculusApi";
                return Class.forName(apiClass).getMethod("getInstance").invoke(null);
            }
        } catch (Exception var3) {
            return null;
        }
    }

    private static Method findShaderInUseMethod() {
        if (SHADER_API == null) {
            return null;
        } else {
            try {
                return SHADER_API.getClass().getMethod("isShaderPackInUse");
            } catch (Exception var1) {
                return null;
            }
        }
    }

    public static boolean isShaderPackActive() {
        if (SHADER_API != null && SHADER_IN_USE != null) {
            try {
                return Boolean.TRUE.equals(SHADER_IN_USE.invoke(SHADER_API));
            } catch (Exception var1) {
                return false;
            }
        } else {
            return false;
        }
    }

    public static void render(PoseStack poseStack, Matrix4f projectionMatrix) {
        updateFade();
        if (fade <= 0.002F) {
            return;
        }
        {
            if (!isShaderPackActive()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null && mc.player != null) {
                    if (synthwave == null) {
                        LOGGER.warn("Synthwave shader is null!");
                    } else {
                        LOGGER.debug("Rendering Synthwave shader...");
                        RenderSystem.backupProjectionMatrix();
                        RenderSystem.disableDepthTest();
                        RenderSystem.depthMask(false);
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
                        int depthTextureId = -1;
                        int currentFBO = GL30.glGetInteger(36006);
                        if (currentFBO != 0) {
                            int type = GL30.glGetFramebufferAttachmentParameteri(36160, 36096, 36048);
                            if (type == 5890) {
                                depthTextureId = GL30.glGetFramebufferAttachmentParameteri(36160, 36096, 36049);
                            } else if (type == 0) {
                                type = GL30.glGetFramebufferAttachmentParameteri(36160, 33306, 36048);
                                if (type == 5890) {
                                    depthTextureId = GL30.glGetFramebufferAttachmentParameteri(36160, 33306, 36049);
                                }
                            }
                        }

                        if (depthTextureId <= 0) {
                            depthTextureId = mc.getMainRenderTarget().getDepthTextureId();
                        }

                        RenderSystem.setShader(() -> synthwave);
                        synthwave.setSampler("MainDepthSampler", depthTextureId);
                        if (synthwave.getUniform("ScreenSize") != null) {
                            synthwave.getUniform("ScreenSize").set((float)mc.getWindow().getWidth(), (float)mc.getWindow().getHeight());
                        }

                        Matrix4f modelViewMatrix = poseStack.last().pose();
                        Matrix4f inverseProj = new Matrix4f(projectionMatrix);
                        Matrix4f inverseModelView = new Matrix4f(modelViewMatrix);
                        inverseProj.invert();
                        inverseModelView.invert();
                        if (synthwave.getUniform("U_InverseProjectionMatrix") != null) {
                            synthwave.getUniform("U_InverseProjectionMatrix").set(inverseProj);
                        }

                        if (synthwave.getUniform("U_InverseViewMatrix") != null) {
                            synthwave.getUniform("U_InverseViewMatrix").set(inverseModelView);
                        }

                        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
                        if (synthwave.getUniform("U_CameraPosition") != null) {
                            synthwave.getUniform("U_CameraPosition").set((float)cameraPos.x, (float)cameraPos.y, (float)cameraPos.z);
                        }

                        if (synthwave.getUniform("U_GameTime") != null) {
                            float timeSeconds = (float)(System.currentTimeMillis() - animationStartTime) / 1000.0F * 3.0F;
                            synthwave.getUniform("U_GameTime").set(timeSeconds);
                        }

                        // 淡入淡出强度：由 fsh 乘到 alphaMask 上（0=完全透明，1=原强度）
                        if (synthwave.getUniform("U_Fade") != null) {
                            synthwave.getUniform("U_Fade").set(fade);
                        }

                        Tesselator tesselator = Tesselator.getInstance();
                        BufferBuilder bufferbuilder = tesselator.getBuilder();
                        bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
                        bufferbuilder.vertex(-1.0, 1.0, 0.0).endVertex();
                        bufferbuilder.vertex(-1.0, -1.0, 0.0).endVertex();
                        bufferbuilder.vertex(1.0, -1.0, 0.0).endVertex();
                        bufferbuilder.vertex(1.0, 1.0, 0.0).endVertex();
                        BufferUploader.drawWithShader(bufferbuilder.end());
                        synthwave.clear();
                        RenderSystem.restoreProjectionMatrix();
                        RenderSystem.enableDepthTest();
                        RenderSystem.depthMask(true);
                    }
                }
            }
        }
    }

    /** 按真实时间推进淡入淡出（与帧率、tick 无关）。 */
    private static void updateFade() {
        long now = System.currentTimeMillis();
        float dt = lastFadeMs == 0L ? 0.016F : Math.min(0.2F, (now - lastFadeMs) / 1000.0F);
        lastFadeMs = now;
        float step = dt * 1000.0F / (float)FADE_MS;

        if (wantEnabled) {
            fade = Math.min(1.0F, fade + step);
            isSynthwaveEnabled = true;
        } else {
            fade = Math.max(0.0F, fade - step);
            if (fade <= 0.0F) {
                isSynthwaveEnabled = false;
                lastFadeMs = 0L;
            }
        }
    }

    public static void startShader() {
        if (wantEnabled) {
            return;                 // 已经在蓄力：保持动画时间连续（按住右键时效果持续铺开）
        }
        if (!isSynthwaveEnabled && fade <= 0.0F) {
            animationStartTime = System.currentTimeMillis();   // 新的一次蓄力：扫描从头开始
        }
        wantEnabled = true;
        if (DEBUG) LOGGER.info("[synthwave] start (charging)");
    }

    public static void stopShader() {
        if (!wantEnabled) {
            return;
        }
        wantEnabled = false;
        if (DEBUG) LOGGER.info("[synthwave] stop (released)");
    }

    public static boolean isShaderActive() {
        return isSynthwaveEnabled;
    }
}
