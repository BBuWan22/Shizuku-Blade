package com.buwan.shizuku.mixin;

import com.buwan.shizuku.server.BladeDurabilityFix;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 修复无铭「有名」攻击或挖掘时的负数附魔等级。 */
@Mixin(ItemSlashBlade.class)
public class ItemSlashBladeMixin {
    @Inject(method = "hurtEnemy", at = @At("HEAD"), require = 0)
    private void shizuku$fixEnchantmentsOnHit(ItemStack stack, LivingEntity target, LivingEntity attacker, CallbackInfoReturnable<Boolean> cir) {
        BladeDurabilityFix.fixBladeEnchantmentLevels(stack);
    }

    @Inject(method = "mineBlock", at = @At("HEAD"), require = 0)
    private void shizuku$fixEnchantmentsOnMine(
        ItemStack stack, Level worldIn, BlockState state, BlockPos pos, LivingEntity entityLiving, CallbackInfoReturnable<Boolean> cir
    ) {
        BladeDurabilityFix.fixBladeEnchantmentLevels(stack);
    }
}
