package com.buwan.shizuku.server;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class BladeDurabilityFix {
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            ItemStack stack = attacker.getMainHandItem();
            if (stack.getItem() instanceof ItemSlashBlade) {
                fixBladeEnchantmentLevels(stack);
            }
        }
    }

    private static void fixBladeEnchantmentLevels(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            try {
                if (stack.hasTag() && stack.getOrCreateTag().contains("Enchantments")) {
                    ListTag enchantments = stack.getEnchantmentTags();
                    boolean hasNegativeLevel = false;

                    for (int i = 0; i < enchantments.size(); i++) {
                        CompoundTag enchantmentTag = enchantments.getCompound(i);
                        if (enchantmentTag.contains("lvl")) {
                            int level = enchantmentTag.getInt("lvl");
                            if (level < 0) {
                                hasNegativeLevel = true;
                                enchantmentTag.putInt("lvl", 0);
                                System.out
                                    .println(
                                        "[BladeDurabilityFix] \u4fee\u590d\u8d1f\u6570\u9644\u9b54\u7b49\u7ea7: "
                                            + enchantmentTag.getString("id")
                                            + " \u4ece "
                                            + level
                                            + " \u5230 0"
                                    );
                            }
                        }
                    }

                    if (hasNegativeLevel) {
                        System.out.println("[BladeDurabilityFix] \u5df2\u4fee\u590d\u62d4\u5200\u5251\u9644\u9b54: " + stack.getHoverName().getString());
                    }
                }
            } catch (Exception var6) {
                var6.printStackTrace();
            }
        }
    }
}
