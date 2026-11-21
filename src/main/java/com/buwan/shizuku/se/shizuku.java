package com.buwan.shizuku.se;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent.HitEvent;
import mods.flammpfeil.slashblade.event.SlashBladeEvent.UpdateEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * SE「雫」：最基础的 SE，命中/更新时给持有者小额的伤害与状态调整。
 */
@EventBusSubscriber
public class shizuku extends SpecialEffect {
    public shizuku() {
        super(33);
    }

    @SubscribeEvent
    public static void onUpdate(UpdateEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(SERegistry.SHIZUKU.getId())) {
            if (event.getEntity() instanceof Player) {
                Player player = (Player)event.getEntity();
                if (SpecialEffect.isEffective(SERegistry.SHIZUKU.getId(), player.experienceLevel) && player.tickCount % 100 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 2, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1, false, false));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onHitEntity(HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(SERegistry.SHIZUKU.getId())) {
            if (event.getUser() instanceof Player) {
                Player player = (Player)event.getUser();
                if (SpecialEffect.isEffective(SERegistry.SHIZUKU.getId(), player.experienceLevel)) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 2, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1, false, false));
                }
            }
        }
    }
}
