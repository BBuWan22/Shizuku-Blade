package com.buwan.shizuku.core;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;

/**
 * 记录「实体是否已被加入世界」的旁路表（UUID -> boolean）。
 *
 * 原版的 addedToWorld 标记是私有的，某些清怪流程需要在实体真正加入世界之前就
 * 把它按「已加入」处理，否则后续 remove 调用会被原版忽略。
 */
public class AddedToWorldHelper {
    private static final Map<Entity, Boolean> ADDED_TO_WORLD_MAP = Collections.synchronizedMap(new WeakHashMap<>(512));

    public static void setAddedToWorldValue(Entity target, boolean value) {
        if (target != null) {
            ADDED_TO_WORLD_MAP.put(target, value);
        }
    }

    public static boolean containsEntity(Entity target) {
        return target != null && ADDED_TO_WORLD_MAP.containsKey(target);
    }

    public static boolean isAddedToWorld(Entity target) {
        if (target == null) {
            return false;
        } else {
            Boolean value = ADDED_TO_WORLD_MAP.get(target);
            return value != null ? value : false;
        }
    }

    public static void clearAll() {
        ADDED_TO_WORLD_MAP.clear();
    }

    public static int size() {
        return ADDED_TO_WORLD_MAP.size();
    }
}
