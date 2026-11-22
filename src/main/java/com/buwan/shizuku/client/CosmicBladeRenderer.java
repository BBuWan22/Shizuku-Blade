package com.buwan.shizuku.client;

import com.buwan.shizuku.client.shader.CosmicShaders;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.client.RenderOverrideEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 宇宙刀（星河刀）的刀身星光：接管前置的 RenderOverrideEvent，把刀身那一层
 * 换成自定义 cosmic 着色器绘制。
 *
 * 注册在 Forge 的 RenderOverrideEvent 上；不是宇宙刀时直接 return，交回前置原样渲染。
 */
@OnlyIn(Dist.CLIENT)
public final class CosmicBladeRenderer {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("shizuku/icon");
    /** 只打一次，用来确认 ItemRendererGuiMixin 的注入真的生效了（排查图标问题用）。 */
    private static boolean hookLogged;

    private CosmicBladeRenderer() {
    }

    /** GUI 物品渲染的嵌套深度（>0 表示当前正在画物品图标）。 */
    private static int guiItemDepth;

    /**
     * 「正在以 GUI 上下文渲染物品」（快捷栏/背包格/JEI 图标；地面掉落物/展示框/手持都不走
     * GUI 上下文）。驱动 {@link CosmicShaders#inventoryRender} —— 即 1.20.1 原版 Forge
     * {@code ScreenEvent.Render.Pre/Post} 的等价物，而且更精确：只覆盖真正的物品图标，
     * 且**覆盖快捷栏**（快捷栏是 HUD 画的，不属于任何 Screen，服 ScreenEvent 会漏）。
     */
    public static void enterGuiItemRender(boolean gui) {
        if (gui) {
            guiItemDepth++;
            CosmicShaders.inventoryRender = true;
            if (!hookLogged) {
                hookLogged = true;
                LOGGER.info("[shizuku] GUI item context hook active (icon star layer will use the fixed view)");
            }
        }
    }

    public static void exitGuiItemRender(boolean gui) {
        if (gui && guiItemDepth > 0) {
            guiItemDepth--;
            if (guiItemDepth == 0) {
                CosmicShaders.inventoryRender = false;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderOverride(RenderOverrideEvent event) {
        ItemStack stack = event.getStack();
        if (stack != null && stack.getItem() instanceof ItemSlashBlade) {
            String translationKey = stack.getCapability(ItemSlashBlade.BLADESTATE).map(ISlashBladeState::getTranslationKey).orElse("");
            if ("item.shizuku.mosmicblade".equals(translationKey)) {
                String target = event.getTarget();
                if (target != null && target.endsWith("_luminous")) {
                    String baseTarget = target.substring(0, target.length() - "_luminous".length());
                    CosmicShaders.renderCosmic(event.getModel(), baseTarget, event.getPoseStack(), event.getBuffer(), 1.0F);
                }
            }
        }
    }
}
