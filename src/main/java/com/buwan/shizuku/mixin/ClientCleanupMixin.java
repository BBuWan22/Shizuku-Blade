package com.buwan.shizuku.mixin;

import com.buwan.shizuku.client.YmsaClientErase;
import com.buwan.shizuku.client.YmsaRenderKiller;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(Minecraft.class)
public class ClientCleanupMixin {
    @Inject(method = "clearLevel()V", at = @At("HEAD"))
    private void ymsa$clearLevel(CallbackInfo ci) {
        YmsaClientErase.clear();
        YmsaRenderKiller.clear();
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void ymsa$stop(CallbackInfo ci) {
        YmsaClientErase.clear();
        YmsaRenderKiller.clear();
    }
}
