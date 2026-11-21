package com.buwan.shizuku.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityLookup.class)
public interface EntityLookupAccessor {
    @Accessor("byId")
    Int2ObjectMap<Entity> ymsa$getById();

    @Accessor("byUuid")
    Map<UUID, Entity> ymsa$getByUuid();
}
