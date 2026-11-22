package com.buwan.shizuku.client.tooltip;

import com.buwan.shizuku.util.tooltip.ColorHelper;
import com.buwan.shizuku.util.tooltip.GradientConfig;
import com.buwan.shizuku.util.tooltip.ICustomTooltip;
import com.buwan.shizuku.util.tooltip.RainbowTextUtil;
import com.buwan.shizuku.util.tooltip.RoundedRectRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RenderTooltipEvent.Pre;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4i;

/**
 * 自定义 tooltip 的绘制实现：圆角面板 + 渐变边框 + 立绘 + 彩虹渐变文字。
 *
 * 两个入口（容器内与 JEI 内各一套重载），都从 TooltipEventHandler 转发进来；
 * 面板尺寸按文本行数动态计算，颜色取自 GradientConfig / RainbowTextUtil。
 */
public class ShizukuTooltipRenderer {
    private static final ResourceLocation SHIZUKU_BACKGROUND = ResourceLocation.fromNamespaceAndPath("shizuku", "textures/gui/shizuku_blade_bg.png");
    private static final int SEPARATOR_COLOR = -2130739136;

    @SubscribeEvent
    public static void onTooltipRender(Pre event) {
        ItemStack stack = event.getItemStack();
        if (isShizukuNamedBlade(stack)) {
            event.setCanceled(true);
            List<Component> textLines = generateTooltipText(stack, event.getFont());
            renderCustomTooltip(event.getGraphics(), textLines, event.getX(), event.getY(), event.getFont());
        }
    }

    private static boolean isShizukuNamedBlade(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            String displayName = stack.getHoverName().getString();
            if (displayName.contains("\u65e0\u94ed") && displayName.contains("\u6709\u540d")) {
                return true;
            } else {
                if (stack.hasTag()) {
                    CompoundTag tag = stack.getTag();
                    if (tag.contains("display")) {
                        CompoundTag displayTag = tag.getCompound("display");
                        if (displayTag.contains("Name")) {
                            String name = displayTag.getString("Name");
                            if (name.contains("\u65e0\u94ed") && name.contains("\u6709\u540d")) {
                                return true;
                            }
                        }
                    }

                    if (tag.contains("SB.SwordName")) {
                        String swordName = tag.getString("SB.SwordName");
                        if (swordName.contains("\u65e0\u94ed") && swordName.contains("\u6709\u540d")) {
                            return true;
                        }
                    }
                }

                return false;
            }
        }
    }

    private static List<Component> generateTooltipText(ItemStack stack, Font font) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(stack.getHoverName());
        List<Component> itemTooltip = new ArrayList<>();
        stack.getItem().appendHoverText(stack, (Level)null, itemTooltip, TooltipFlag.NORMAL);
        tooltip.addAll(itemTooltip);
        tooltip.add(Component.empty());
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("\u950b\u5229:\u4e0d\u53ef\u4f30\u91cf"));
        tooltip.add(Component.literal("\u529b\u91cf:\u4e0d\u53ef\u4f30\u91cf"));
        tooltip.add(Component.literal("\u53ef\u7231:\u4e0d\u53ef\u4f30\u91cf"));
        tooltip.add(Component.literal("\u9b45\u529b:\u4e0d\u53ef\u4f30\u91cf"));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("shift + sa\u91ca\u653e\u540e\u53ef\u51fb\u6740\u602a\u7269\u5e76\u6e05\u9664\u6389\u843d\u7269"));
        tooltip.add(TooltipEventHandler.judgmentKeyHint());
        tooltip.add(Component.literal("shift + alt \u53ef\u5f00\u542f\u5750\u6807\u4f4d\u79fb"));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("\u76f8\u89c1\u65f6\u96be\u522b\u4ea6\u96be\uff0c\u4e1c\u98ce\u65e0\u529b\u767e\u82b1\u6b8b"));
        tooltip.add(Component.literal("\u6625\u8695\u5230\u6b7b\u4e1d\u65b9\u5c3d\uff0c\u8721\u70ac\u6210\u7070\u6cea\u59cb\u5e72"));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("  \u732b\u7fbd\u96eb \u653b\u51fb\u4f24\u5bb3"));
        tooltip.add(Component.empty());
        tooltip.add(Component.empty());
        return tooltip;
    }

    public static void renderCustomTooltip(GuiGraphics graphics, List<Component> components, int x, int y, Font font) {
        renderCustomTooltip(graphics, components, x, y, font, ICustomTooltip.TooltipType.DEFAULT);
    }

    public static void renderCustomTooltip(GuiGraphics graphics, List<Component> components, int x, int y, Font font, ICustomTooltip.TooltipType tooltipType) {
        if (!components.isEmpty()) {
            boolean cosmic = tooltipType == ICustomTooltip.TooltipType.COSMIC;
            List<Integer> palette = ColorHelper.ShizukuBlade;
            List<Integer> lastLineColors = ColorHelper.ShizukuBlade_B;
            PoseStack poseStack = graphics.pose();
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, 400.0F);
            List<ShizukuTooltipRenderer.TooltipLine> lines = parseTooltipLines(components, font, palette, lastLineColors);
            int maxWidth = 0;

            for (ShizukuTooltipRenderer.TooltipLine line : lines) {
                maxWidth = Math.max(maxWidth, line.width + 40);
            }

            int tooltipHeight = 16;

            for (ShizukuTooltipRenderer.TooltipLine tooltipLine : lines) {
                if (tooltipLine.isSeparator) {
                    tooltipHeight += 6;
                } else {
                    tooltipHeight += 10;
                }
            }

            int screenWidth = graphics.guiWidth();
            int screenHeight = graphics.guiHeight();
            float scale = Math.min(1F, Math.min((screenWidth - 8F) / maxWidth, (screenHeight - 8F) / tooltipHeight));
            x = Math.max(4, Math.min(x, screenWidth - (int)Math.ceil(maxWidth * scale) - 4));
            y = Math.max(4, Math.min(y, screenHeight - (int)Math.ceil(tooltipHeight * scale) - 4));
            poseStack.translate(x, y, 0);
            poseStack.scale(scale, scale, 1);
            x = 0;
            y = 0;

            if (!cosmic) {
                renderShizukuBackground(graphics, x - 80, y - 140, 300, 320);
                renderTooltipBackground(graphics, x, y, maxWidth, tooltipHeight, palette);
            } else {
                renderTooltipBorderOnly(graphics, x, y, maxWidth, tooltipHeight, palette);
            }

            int currentY = y + 8;

            for (ShizukuTooltipRenderer.TooltipLine tooltipLinex : lines) {
                if (tooltipLinex.isSeparator) {
                    renderSeparatorLine(graphics, x + 8, currentY, maxWidth - 16);
                    currentY += 6;
                } else {
                    int textX = tooltipLinex.isTitle ? x + (maxWidth - tooltipLinex.width) / 2 : x + 12;
                    renderRainbowText(graphics, tooltipLinex.component, textX, currentY, font);
                    currentY += 10;
                }
            }

            poseStack.popPose();
        }
    }

    private static void renderRingHalf(
        GuiGraphics graphics,
        float centerX,
        float centerY,
        float outerRadius,
        float innerRadius,
        float tiltAngleX,
        float rotationZ,
        int segments,
        VertexConsumer consumer,
        boolean isFrontHalf,
        List<Integer> colors
    ) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        Minecraft mc = Minecraft.getInstance();
        long time = System.currentTimeMillis();
        poseStack.translate(centerX, centerY, 0.0F);
        poseStack.mulPose(new Quaternionf().rotationZ((float)Math.toRadians(rotationZ)));
        poseStack.translate(-centerX, -centerY, 0.0F);
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        float tiltScaleX = 1.0F - tiltAngleX * 0.9F;
        float nx = 0.0F;
        float ny = 0.0F;
        float nz = 1.0F;

        for (int i = 0; i < segments; i++) {
            float angle1 = (float)((Math.PI * 2) * i / segments);
            float angle2 = (float)((Math.PI * 2) * (i + 1) / segments);
            float x1Outer = centerX + (float)Math.cos(angle1) * outerRadius;
            float y1Outer = centerY + (float)Math.sin(angle1) * outerRadius * tiltScaleX;
            float x2Outer = centerX + (float)Math.cos(angle2) * outerRadius;
            float y2Outer = centerY + (float)Math.sin(angle2) * outerRadius * tiltScaleX;
            float x1Inner = centerX + (float)Math.cos(angle1) * innerRadius;
            float y1Inner = centerY + (float)Math.sin(angle1) * innerRadius * tiltScaleX;
            float x2Inner = centerX + (float)Math.cos(angle2) * innerRadius;
            float y2Inner = centerY + (float)Math.sin(angle2) * innerRadius * tiltScaleX;
            float avgY1 = (y1Outer + y1Inner) / 2.0F;
            float avgY2 = (y2Outer + y2Inner) / 2.0F;
            float z1 = (avgY1 - centerY) / (outerRadius * tiltScaleX) * 2.5F;
            float z2 = (avgY2 - centerY) / (outerRadius * tiltScaleX) * 2.5F;
            float avgZ = (z1 + z2) / 2.0F;
            if ((!isFrontHalf || !(avgZ <= 0.0F)) && (isFrontHalf || !(avgZ > 0.0F))) {
                int colorIndex = i * colors.size() / segments;
                int color = colors.get(colorIndex % colors.size());
                int r = color >> 16 & 0xFF;
                int g = color >> 8 & 0xFF;
                int b = color & 0xFF;
                consumer.vertex(pose, x1Outer, y1Outer, z1).color(r, g, b, 255).normal(nx, ny, nz).endVertex();
                consumer.vertex(pose, x1Inner, y1Inner, z1).color(r, g, b, 255).normal(nx, ny, nz).endVertex();
                consumer.vertex(pose, x2Inner, y2Inner, z2).color(r, g, b, 255).normal(nx, ny, nz).endVertex();
                consumer.vertex(pose, x1Outer, y1Outer, z1).color(r, g, b, 255).normal(nx, ny, nz).endVertex();
                consumer.vertex(pose, x2Inner, y2Inner, z2).color(r, g, b, 255).normal(nx, ny, nz).endVertex();
                consumer.vertex(pose, x2Outer, y2Outer, z2).color(r, g, b, 255).normal(nx, ny, nz).endVertex();
            }
        }

        graphics.bufferSource().endBatch();
        poseStack.popPose();
    }

    private static void renderShizukuBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        RenderSystem.setShaderTexture(0, SHIZUKU_BACKGROUND);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        long time = System.currentTimeMillis();
        float baseAngle = (float)(time % 6000L) / 6000.0F * 360.0F;
        float angle2 = baseAngle + 120.0F;
        float angle3 = baseAngle + 240.0F;
        float orbitRadius = 20.0F;
        float offset1X = (float)Math.cos(Math.toRadians(baseAngle)) * orbitRadius;
        float offset1Y = (float)Math.sin(Math.toRadians(baseAngle)) * orbitRadius;
        float offset2X = (float)Math.cos(Math.toRadians(angle2)) * orbitRadius;
        float offset2Y = (float)Math.sin(Math.toRadians(angle2)) * orbitRadius;
        float offset3X = (float)Math.cos(Math.toRadians(angle3)) * orbitRadius;
        float offset3Y = (float)Math.sin(Math.toRadians(angle3)) * orbitRadius;
        PoseStack poseStack = graphics.pose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.3F);
        poseStack.pushPose();
        poseStack.translate(offset1X, offset1Y, 0.0F);
        graphics.blit(SHIZUKU_BACKGROUND, x, y, 0.0F, 0.0F, width, height, width, height);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(offset2X, offset2Y, 0.0F);
        graphics.blit(SHIZUKU_BACKGROUND, x, y, 0.0F, 0.0F, width, height, width, height);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(offset3X, offset3Y, 0.0F);
        graphics.blit(SHIZUKU_BACKGROUND, x, y, 0.0F, 0.0F, width, height, width, height);
        poseStack.popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(SHIZUKU_BACKGROUND, x, y, 0.0F, 0.0F, width, height, width, height);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void renderTooltipBackground(GuiGraphics graphics, int x, int y, int width, int height, List<Integer> borderColors) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        float cornerRadius = 12.0F;
        int segments = 16;
        GradientConfig borderGradient = new GradientConfig(borderColors, 2.0F, GradientConfig.GradientType.BORDER_CIRCULAR, true);
        RoundedRectRenderer.renderRoundedRect(
            graphics, x, y, width, height, cornerRadius, cornerRadius, cornerRadius, cornerRadius, new Vector4i(135, 206, 250, 220), segments
        );
        RoundedRectRenderer.renderRoundedRectBorderGradient(
            graphics,
            x,
            y,
            width,
            height,
            cornerRadius,
            cornerRadius,
            cornerRadius,
            cornerRadius,
            1.0F,
            borderGradient,
            new Vector4i(255, 255, 255, 255),
            segments
        );
        poseStack.popPose();
    }

    private static void renderTooltipBorderOnly(GuiGraphics graphics, int x, int y, int width, int height, List<Integer> borderColors) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        float cornerRadius = 12.0F;
        int segments = 16;
        GradientConfig borderGradient = new GradientConfig(borderColors, 2.0F, GradientConfig.GradientType.BORDER_CIRCULAR, true);
        RoundedRectRenderer.renderRoundedRectBorderGradient(
            graphics,
            x,
            y,
            width,
            height,
            cornerRadius,
            cornerRadius,
            cornerRadius,
            cornerRadius,
            1.0F,
            borderGradient,
            new Vector4i(255, 255, 255, 255),
            segments
        );
        poseStack.popPose();
    }

    private static void renderSeparatorLine(GuiGraphics graphics, int x, int y, int width) {
        PoseStack poseStack = graphics.pose();
        Matrix4f matrix = poseStack.last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int centerX = x + width / 2;
        addGradientVertex(buffer, matrix, x, y, 16744512);
        addGradientVertex(buffer, matrix, centerX, y, -2130739136);
        addGradientVertex(buffer, matrix, centerX, y + 2, -2130739136);
        addGradientVertex(buffer, matrix, x, y + 2, 16744512);
        addGradientVertex(buffer, matrix, centerX, y, -2130739136);
        addGradientVertex(buffer, matrix, x + width, y, 16744512);
        addGradientVertex(buffer, matrix, x + width, y + 2, 16744512);
        addGradientVertex(buffer, matrix, centerX, y + 2, -2130739136);
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }

    private static void renderRainbowText(GuiGraphics graphics, Component component, int x, int y, Font font) {
        graphics.drawString(font, component, x, y, -1642241, false);
    }

    private static void addGradientVertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, int color) {
        int a = color >> 24 & 0xFF;
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        buffer.vertex(matrix, x, y, 0.0F).color(r, g, b, a).endVertex();
    }

    private static List<ShizukuTooltipRenderer.TooltipLine> parseTooltipLines(
        List<Component> components, Font font, List<Integer> palette, List<Integer> lastLineColors
    ) {
        List<ShizukuTooltipRenderer.TooltipLine> lines = new ArrayList<>();
        RainbowTextUtil.RainbowConfig nameConfig = new RainbowTextUtil.RainbowConfig()
            .setSpeed(10.0F)
            .setAnimationType(RainbowTextUtil.AnimationType.FLOWING)
            .setColorSpan(0.1F)
            .setColors(palette);
        RainbowTextUtil.RainbowConfig descConfig = new RainbowTextUtil.RainbowConfig()
            .setSpeed(17.0F)
            .setAnimationType(RainbowTextUtil.AnimationType.FLOWING)
            .setColorSpan(0.03F)
            .setColors(palette);
        RainbowTextUtil.RainbowConfig lastLineConfig = new RainbowTextUtil.RainbowConfig()
            .setSpeed(17.0F)
            .setAnimationType(RainbowTextUtil.AnimationType.FLOWING)
            .setColorSpan(0.03F)
            .setColors(lastLineColors);
        boolean isFirstLine = true;
        boolean separatorAdded = false;
        int totalLines = components.size();

        for (int i = 0; i < totalLines; i++) {
            Component component = components.get(i);
            String text = component.getString().replaceAll("\u00a7.", "");
            if (isFirstLine) {
                Component rainbow = RainbowTextUtil.createRainbowText(text, nameConfig);
                lines.add(new ShizukuTooltipRenderer.TooltipLine(rainbow, text, font.width(text), true, false));
                isFirstLine = false;
            } else {
                if (!separatorAdded) {
                    lines.add(new ShizukuTooltipRenderer.TooltipLine(Component.empty(), "", 0, false, true));
                    separatorAdded = true;
                }

                boolean isLastLine = i == totalLines - 1;
                String[] subLines = text.split("\\n");

                for (int j = 0; j < subLines.length; j++) {
                    String subLine = subLines[j].replaceAll("\u00a7.", "");
                    if (subLine.isEmpty()) {
                        lines.add(new ShizukuTooltipRenderer.TooltipLine(Component.empty(), "", 0, false, false));
                    } else if (!subLine.trim().isEmpty()) {
                        boolean isLastSubLine = isLastLine && j == subLines.length - 1;
                        RainbowTextUtil.RainbowConfig config = isLastSubLine ? lastLineConfig : descConfig;
                        Component rainbow = RainbowTextUtil.createRainbowText(subLine, config);
                        lines.add(new ShizukuTooltipRenderer.TooltipLine(rainbow, subLine, font.width(subLine), false, false));
                    }
                }
            }
        }

        return lines;
    }

    private static class TooltipLine {
        Component component;
        String text;
        int width;
        boolean isTitle;
        boolean isSeparator;

        TooltipLine(Component component, String text, int width, boolean isTitle, boolean isSeparator) {
            this.component = component;
            this.text = text;
            this.width = width;
            this.isTitle = isTitle;
            this.isSeparator = isSeparator;
        }
    }
}
