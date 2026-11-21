package com.buwan.shizuku.registry.other;

import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 实体类型注册（刀光/飞剑等由前置提供的类型在这里登记引用），
 * 名字里的 Regristrys 是原作者手误，保持原样以免破坏外部引用。
 */
public class LBEntiteRegristrys {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "shizuku");

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
