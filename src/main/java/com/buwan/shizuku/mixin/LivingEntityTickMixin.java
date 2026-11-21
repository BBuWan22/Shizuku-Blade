package com.buwan.shizuku.mixin;

import com.buwan.shizuku.sa.Ymsa;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityTickMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void ymsa$suppressAnnihilatedTick(CallbackInfo ci) {
        if (Ymsa.isTickSuppressed((LivingEntity)(Object)this)) {
            ci.cancel();
        }
    }
}
