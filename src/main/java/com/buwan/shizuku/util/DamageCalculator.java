package com.buwan.shizuku.util;

import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.util.AttackHelper;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class DamageCalculator {
    public static float calculateRefinedDamage(LivingEntity caster, Entity target, float comboRatio) {
        return calculateRefinedDamage(caster, comboRatio);
    }

    public static float calculateRefinedDamage(LivingEntity caster, Entity target) {
        return calculateRefinedDamage(caster, 1.0F);
    }

    public static float calculateRefinedDamage(LivingEntity caster, float comboRatio) {
        if (!(caster instanceof Player)) {
            return 6.0F * comboRatio;
        } else {
            double attack = caster.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double damage = comboRatio * attack + AttackHelper.getRankBonus(caster);
            damage *= AttackManager.getSlashBladeDamageScale(caster) * SlashBladeConfig.SLASHBLADE_DAMAGE_MULTIPLIER.get();
            return (float)damage;
        }
    }
}
