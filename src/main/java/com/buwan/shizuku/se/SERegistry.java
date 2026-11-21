package com.buwan.shizuku.se;

import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SERegistry {
    public static final DeferredRegister<SpecialEffect> REGISTRY_KEY2 = DeferredRegister.create(SpecialEffect.REGISTRY_KEY, "shizuku");
    public static final RegistryObject<SpecialEffect> SHIZUKU = REGISTRY_KEY2.register("shizuku", shizuku::new);
    public static final RegistryObject<SpecialEffect> LEISE = REGISTRY_KEY2.register("leise", Leise::new);
    public static final RegistryObject<SpecialEffect> JINGXIN = REGISTRY_KEY2.register("jingxin", Jingxin::new);
    public static final RegistryObject<SpecialEffect> WORLDS_FAVORITE = REGISTRY_KEY2.register("worlds_favorite", WorldsFavorite::new);

    public static void register(IEventBus bus) {
        REGISTRY_KEY2.register(bus);
    }
}
