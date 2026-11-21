package com.buwan.shizuku.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("removalReason")
    void ymsa$setRemovalReason(RemovalReason var1);

    @Accessor("isAddedToWorld")
    void ymsa$setAddedToWorld(boolean var1);
}
