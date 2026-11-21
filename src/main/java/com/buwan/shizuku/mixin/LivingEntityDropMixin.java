package com.buwan.shizuku.mixin;

import com.buwan.shizuku.sa.Ymsa;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityDropMixin {
    @Inject(method = "dropAllDeathLoot", at = @At("RETURN"))
    private void ymsa$dropsProcessed(DamageSource source, CallbackInfo ci) {
        Ymsa.handleDropsProcessed((LivingEntity)(Object)this);
    }
}
