package com.buwan.shizuku.se;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent.HitEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class Jingxin extends SpecialEffect {
    public Jingxin() {
        super(34);
    }

    @SubscribeEvent
    public static void onHitEntity(HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(SERegistry.JINGXIN.getId())) {
            if (event.getUser() instanceof Player) {
                Player player = (Player)event.getUser();
                if (SpecialEffect.isEffective(SERegistry.JINGXIN.getId(), player.experienceLevel)) {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, false, false));
                }
            }
        }
    }
}
