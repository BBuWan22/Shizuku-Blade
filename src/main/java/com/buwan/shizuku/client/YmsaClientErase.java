package com.buwan.shizuku.client;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;

/**
 * 在客户端把「已经被夜冥杀抹除」的实体记进一个 UUID 集合，渲染前直接跳过，
 * 避免实体已死但还在画一帧的问题（与 YmsaRenderKiller 配合）。
 */
public class YmsaClientErase {
    private static final Set<UUID> ERASED = ConcurrentHashMap.newKeySet();

    public static void markErased(UUID uuid) {
        if (uuid != null) {
            ERASED.add(uuid);
        }
    }

    public static boolean isErased(Entity entity) {
        return entity != null && ERASED.contains(entity.getUUID());
    }

    public static void clear() {
        ERASED.clear();
    }
}
