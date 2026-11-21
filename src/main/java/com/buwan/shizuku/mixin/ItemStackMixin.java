package com.buwan.shizuku.mixin;

import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "hurtAndBreak", at = @At("HEAD"), cancellable = true, require = 0)
    private void onHurtAndBreak(int amount, LivingEntity entity, Consumer<LivingEntity> onBroken, CallbackInfo ci) {
        ItemStack stack = (ItemStack)(Object)this;

        try {
            if (!stack.isDamageableItem()) {
                ci.cancel();
                return;
            }

            if (stack.getDamageValue() >= stack.getMaxDamage()) {
                ci.cancel();
                return;
            }
        } catch (IllegalArgumentException var7) {
            var7.printStackTrace();
            ci.cancel();
        }
    }
}
