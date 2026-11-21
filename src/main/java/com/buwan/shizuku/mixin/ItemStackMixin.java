package com.buwan.shizuku.mixin;

import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 ItemStack.hurtAndBreak 的 HEAD 拦截：物品不可损坏或耐久已满时直接取消，
 * 避免异常情况下的 IllegalArgument。
 *
 * 注意：这是对原版类的全局注入（影响所有物品，不只刀），属于比较激进的写法。
 */
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
