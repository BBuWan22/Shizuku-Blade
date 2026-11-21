package com.buwan.shizuku.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("dropAllDeathLoot")
    void ymsa$dropAllDeathLoot(DamageSource var1);

    @Accessor("DATA_HEALTH_ID")
    static EntityDataAccessor<Float> ymsa$getDataHealthId() {
        throw new AssertionError();
    }
}
