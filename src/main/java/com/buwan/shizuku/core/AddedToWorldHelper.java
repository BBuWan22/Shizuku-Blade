package com.buwan.shizuku.core;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;

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
