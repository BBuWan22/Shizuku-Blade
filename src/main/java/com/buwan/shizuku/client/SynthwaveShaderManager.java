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
 * - startShader()/stopShader()：由 SynthwaveShaderPacket 驱动；
 * - render()：在渲染帧末尾用底层的后处理管线跑 synthwave.fsh；
 * - isShaderPackActive()：检测 iris/oculus 光影包，二者同时在跑时让路（否则画面会叠两次）。
 *
 * 已知问题（上游，未在本版本修复）：没有淡入，第一次开启是瞬跳到满强度；
 * 玩家快速连点右键会让特效反复开关，表现为「在脚下持续闪烁」。
 */
@OnlyIn(Dist.CLIENT)
public class SynthwaveShaderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SynthwaveShaderManager.class);
    public static ShaderInstance synthwave;
    private static boolean isSynthwaveEnabled;
    private static long animationStartTime;
    private static final float SPEED_MULTIPLIER = 3.0F;
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
        if (isSynthwaveEnabled) {
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

    public static void startShader() {
        isSynthwaveEnabled = true;
        animationStartTime = System.currentTimeMillis();
        LOGGER.info("Synthwave shader STARTED");
    }

    public static void stopShader() {
        isSynthwaveEnabled = false;
        LOGGER.info("Synthwave shader STOPPED");
    }

    public static boolean isShaderActive() {
        return isSynthwaveEnabled;
    }
}
