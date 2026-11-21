package com.buwan.shizuku.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 把合成波渲染挂到 RenderLevelStageEvent 的时机上（世界渲染完之后、GUI 之前）。
 */
@OnlyIn(Dist.CLIENT)
public class SynthwaveShaderRenderer {
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() == Stage.AFTER_CUTOUT_BLOCKS) {
            SynthwaveShaderManager.render(event.getPoseStack(), event.getProjectionMatrix());
        }
    }
}
