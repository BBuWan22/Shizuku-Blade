package com.buwan.shizuku.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;

/**
 * 记录「实体是否已被标记移除」的旁路表（UUID -> boolean），
 * 用于在实体已被移除但还没被 GC 的窗口期做判断。
 */
public class IsRemovedHelper {
    private static final Map<Entity, Boolean> REMOVED_OVERRIDE_MAP = new ConcurrentHashMap<>();

    public static boolean containsEntity(Object entity) {
        return !(entity instanceof Entity) ? false : REMOVED_OVERRIDE_MAP.containsKey((Entity)entity);
    }

    public static boolean isRemoved(Object entity) {
        if (entity instanceof Entity e && REMOVED_OVERRIDE_MAP.containsKey(e)) {
            Boolean result = REMOVED_OVERRIDE_MAP.get(e);
            return result != null ? result : false;
        } else {
            return false;
        }
    }

    public static void setIsRemoved(Entity entity, boolean isRemoved) {
        if (entity != null) {
            REMOVED_OVERRIDE_MAP.put(entity, isRemoved);
        }
    }

    public static void clearAll() {
        REMOVED_OVERRIDE_MAP.clear();
    }

    public static int size() {
        return REMOVED_OVERRIDE_MAP.size();
    }
}
