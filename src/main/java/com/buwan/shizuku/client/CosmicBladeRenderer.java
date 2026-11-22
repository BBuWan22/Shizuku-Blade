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
    private CosmicBladeRenderer() {
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
