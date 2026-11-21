package com.buwan.shizuku.mixin;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * 刀的两个耐久相关注入：hurtEnemy / mineBlock 里在扣耐久之前做判断。
 *
 * 已知问题（上游行为）：1.20.1 版本还会顺手把 NBT 里的负数附魔等级修成 0；
 * 这条逻辑只对 NBT 附魔有效，1.21+ 改成数据组件后就不适用了。
 */
@Mixin(ItemSlashBlade.class)
public class ItemSlashBladeMixin {
    @Inject(
        method = "hurtEnemy",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            shift = Shift.BEFORE
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        require = 0
    )
    private void onHurtEnemyBeforeHurtBreak(ItemStack stack, LivingEntity target, LivingEntity attacker, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!stack.isDamageableItem() || stack.getDamageValue() >= stack.getMaxDamage()) {
                System.out.println("[Shizuku] \u8df3\u8fc7 hurtAndBreak\uff1a\u7269\u54c1\u4e0d\u53ef\u635f\u574f\u6216\u8010\u4e45\u5ea6\u5df2\u6ee1");
                return;
            }

            if (stack.hasTag()) {
                ListTag enchantments = stack.getEnchantmentTags();

                for (int i = 0; i < enchantments.size(); i++) {
                    CompoundTag tag = enchantments.getCompound(i);
                    if (tag.contains("lvl")) {
                        int level = tag.getInt("lvl");
                        if (level < 0) {
                            System.out
                                .println(
                                    "[Shizuku] \u8b66\u544a\uff1a\u53d1\u73b0\u8d1f\u6570\u9644\u9b54\u7b49\u7ea7 "
                                        + level
                                        + "\uff0c\u81ea\u52a8\u4fee\u590d\u4e3a 0"
                                );
                            tag.putInt("lvl", 0);
                        }
                    }
                }
            }
        } catch (Exception var9) {
        }
    }

    @Inject(
        method = "mineBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            shift = Shift.BEFORE
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        require = 0
    )
    private void onMineBlockBeforeHurtBreak(
        ItemStack stack, Level worldIn, BlockState state, BlockPos pos, LivingEntity entityLiving, CallbackInfoReturnable<Boolean> cir
    ) {
        try {
            if (!stack.isDamageableItem() || stack.getDamageValue() >= stack.getMaxDamage()) {
                System.out.println("[Shizuku] \u8df3\u8fc7 hurtAndBreak\uff1a\u7269\u54c1\u4e0d\u53ef\u635f\u574f\u6216\u8010\u4e45\u5ea6\u5df2\u6ee1");
                return;
            }

            if (stack.hasTag()) {
                ListTag enchantments = stack.getEnchantmentTags();

                for (int i = 0; i < enchantments.size(); i++) {
                    CompoundTag tag = enchantments.getCompound(i);
                    if (tag.contains("lvl")) {
                        int level = tag.getInt("lvl");
                        if (level < 0) {
                            System.out
                                .println(
                                    "[Shizuku] \u8b66\u544a\uff1a\u53d1\u73b0\u8d1f\u6570\u9644\u9b54\u7b49\u7ea7 "
                                        + level
                                        + "\uff0c\u81ea\u52a8\u4fee\u590d\u4e3a 0"
                                );
                            tag.putInt("lvl", 0);
                        }
                    }
                }
            }
        } catch (Exception var11) {
        }
    }
}
