package com.buwan.shizuku.se;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent.UpdateEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class Leise extends SpecialEffect {
    public Leise() {
        super(32);
    }

    @SubscribeEvent
    public static void onUpdate(UpdateEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        if (state.hasSpecialEffect(SERegistry.LEISE.getId())) {
            if (event.getEntity() instanceof Player) {
                Player player = (Player)event.getEntity();
                if (SpecialEffect.isEffective(SERegistry.LEISE.getId(), player.experienceLevel) && player.tickCount % 100 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 120, 1, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 2, false, false));
                }
            }
        }
    }
}
