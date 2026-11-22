package com.buwan.shizuku.core;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;

/**
 * Agent 侧的血量读写入口：把「血量/最大血量」写进一个静态缓存，
 * 供 AgentHealthTransformer 改写后的 getHealth()/getMaxHealth() 直接返回。
 *
 * 与反射方案的区别：这是真正的改字节码，所以对 final 字段、被覆写的 getter 也有效。
 */
public class AgentHealthMethodHelper {
    private static final Map<Object, AgentHealthMethodHelper.HealthData> HEALTH_MAP = Collections.synchronizedMap(new WeakHashMap<>());

    public static void forceSetHealth(Entity target, float health, float maxHealth) {
        if (target != null) {
            HEALTH_MAP.put(target, new AgentHealthMethodHelper.HealthData(health, maxHealth));
        }
    }

    public static float agentGetHealth(Entity target) {
        if (target == null) {
            return -1.0F;
        } else {
            AgentHealthMethodHelper.HealthData data = HEALTH_MAP.get(target);
            return data != null ? data.health : -1.0F;
        }
    }

    public static float agentGetMaxHealth(Entity target) {
        if (target == null) {
            return -1.0F;
        } else {
            AgentHealthMethodHelper.HealthData data = HEALTH_MAP.get(target);
            return data != null ? data.maxHealth : -1.0F;
        }
    }

    public static void forceSetHealth(Object target, float health, float maxHealth) {
        if (target != null) {
            HEALTH_MAP.put(target, new AgentHealthMethodHelper.HealthData(health, maxHealth));
        }
    }

    public static float agentGetHealth(Object target) {
        if (target == null) {
            return -1.0F;
        } else {
            AgentHealthMethodHelper.HealthData data = HEALTH_MAP.get(target);
            return data != null ? data.health : -1.0F;
        }
    }

    public static float agentGetMaxHealth(Object target) {
        if (target == null) {
            return -1.0F;
        } else {
            AgentHealthMethodHelper.HealthData data = HEALTH_MAP.get(target);
            return data != null ? data.maxHealth : -1.0F;
        }
    }

    public static boolean isModified(Object target) {
        return HEALTH_MAP.containsKey(target);
    }

    public static void clear(Object target) {
        if (target != null) {
            HEALTH_MAP.remove(target);
        }
    }

    public static void clearAll() {
        HEALTH_MAP.clear();
    }

    public static class HealthData {
        public float health;
        public float maxHealth;

        public HealthData(float health, float maxHealth) {
            this.health = health;
            this.maxHealth = maxHealth;
        }
    }
}
