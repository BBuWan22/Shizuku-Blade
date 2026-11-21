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
