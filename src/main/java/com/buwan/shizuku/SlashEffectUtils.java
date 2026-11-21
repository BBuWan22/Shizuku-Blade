package com.buwan.shizuku;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class SlashEffectUtils {
    public static boolean hasSpecialEffect(ItemStack stack, String effect) {
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains("bladeState")) {
            CompoundTag forgeCaps = tag.getCompound("bladeState");
            if (forgeCaps.contains("SpecialEffects")) {
                ListTag specialEffects = forgeCaps.getList("SpecialEffects", 8);

                for (int i = 0; i < specialEffects.size(); i++) {
                    String currentEffect = specialEffects.getString(i);
                    if (effect.equals(currentEffect)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static int addslashbladesaDamage(LivingEntity player, int damageex) {
        ItemStack stack = player.getMainHandItem();
        CompoundTag tag = stack.getOrCreateTag();
        double add = damageex * 1;
        return (int)add;
    }
}
