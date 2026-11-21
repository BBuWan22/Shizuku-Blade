package com.buwan.shizuku.mixin;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 宇宙刀的飞剑：命中后静默消失（不播原版命中效果）。
 *
 * 已知问题（上游 bug，未在本版本修复）：EntityAbstractSummonedSword 的 inBlockState 只在
 * onHitBlock 里赋值，而 tick() 里直接对它调 equals()（没有判空）。任何「还没撞过方块」就被
 * 存档/读档的刀光，下一次 tick 会 NPE 崩服；这里用 HEAD+cancel 会让该字段保持 null，
 * 反而更容易触发，建议改成 TAIL 并补一个非空哨兵值（我们移植到 26.1 时就是这么修的）。
 */
@Mixin(EntityAbstractSummonedSword.class)
public abstract class EntityAbstractSummonedSwordMixin {
    @Inject(method = "setHitEntity", at = @At("HEAD"), cancellable = true, remap = false)
    private void shizuku$disappearOnHit(Entity hitEntity, CallbackInfo ci) {
        if (isMosmicDrive((EntityAbstractSummonedSword)(Object)this)) {
            ci.cancel();
            ((Entity)(Object)this).remove(RemovalReason.DISCARDED);
        }
    }

    @Inject(method = "burst", at = @At("HEAD"), cancellable = true, remap = false)
    private void shizuku$silentRemove(CallbackInfo ci) {
        if (isMosmicDrive((EntityAbstractSummonedSword)(Object)this)) {
            ci.cancel();
            ((Entity)(Object)this).remove(RemovalReason.DISCARDED);
        }
    }

    private static boolean isMosmicDrive(EntityAbstractSummonedSword drive) {
        if (drive.getShooter() instanceof LivingEntity living) {
            ItemStack held = living.getMainHandItem();
            if (!(held.getItem() instanceof ItemSlashBlade)) {
                return false;
            } else {
                String key = held.getCapability(ItemSlashBlade.BLADESTATE).map(ISlashBladeState::getTranslationKey).orElse("");
                return "item.shizuku.mosmicblade".equals(key);
            }
        } else {
            return false;
        }
    }
}
