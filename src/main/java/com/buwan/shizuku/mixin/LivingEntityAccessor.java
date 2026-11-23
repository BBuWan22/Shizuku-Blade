package com.buwan.shizuku.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @Invoker：调用 LivingEntity.dropAllDeathLoot，并在需要时读取 DATA_HEALTH_ID。
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("dropAllDeathLoot")
    void ymsa$dropAllDeathLoot(DamageSource var1);

    /** 死亡标记：die() 成功会把 true 置上，用于区分「真的死了」与「血量 0 但被模组吞掉」。 */
    @Accessor("dead")
    boolean ymsa$isDead();

    @Accessor("dead")
    void ymsa$setDead(boolean value);

    /** 击杀分数（awardKillScore 用）。 */
    @Accessor("deathScore")
    int ymsa$getDeathScore();

    @Accessor("DATA_HEALTH_ID")
    static EntityDataAccessor<Float> ymsa$getDataHealthId() {
        throw new AssertionError();
    }
}
