package com.buwan.shizuku.registry.other;

import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class LBEntiteRegristrys {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "shizuku");

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
