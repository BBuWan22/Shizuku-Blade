package com.buwan.shizuku.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;

public class RemovalReasonHelper {
    private static final Map<Entity, RemovalReason> REMOVAL_REASON_OVERRIDE_MAP = new ConcurrentHashMap<>();

    public static boolean containsEntity(Object entity) {
        return !(entity instanceof Entity) ? false : REMOVAL_REASON_OVERRIDE_MAP.containsKey((Entity)entity);
    }

    public static RemovalReason getRemovalReason(Object entity) {
        if (entity instanceof Entity e) {
            return REMOVAL_REASON_OVERRIDE_MAP.containsKey(e) ? REMOVAL_REASON_OVERRIDE_MAP.get(e) : null;
        } else {
            return null;
        }
    }

    public static void setRemovalReason(Entity entity, RemovalReason removalReason) {
        if (entity != null) {
            REMOVAL_REASON_OVERRIDE_MAP.put(entity, removalReason);
        }
    }

    public static void clearAll() {
        REMOVAL_REASON_OVERRIDE_MAP.clear();
    }

    public static int size() {
        return REMOVAL_REASON_OVERRIDE_MAP.size();
    }
}
