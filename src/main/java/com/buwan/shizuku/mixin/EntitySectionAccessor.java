package com.buwan.shizuku.mixin;

import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.level.entity.EntitySection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntitySection.class)
public interface EntitySectionAccessor {
    @Accessor("storage")
    ClassInstanceMultiMap<?> ymsa$getStorage();
}
