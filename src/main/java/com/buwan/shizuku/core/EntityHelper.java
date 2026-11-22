package com.buwan.shizuku.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体血量读写的统一门面：优先走 Agent 改写的 getter，其次反射读字段，
 * 最后退回原版 API；写血量时同时维护一个「强制血量」缓存。
 *
 * 字段名同时尝试 Mojang 名与 SRG 名（如 f_20961_/DATA_HEALTH_ID），
 * 以兼容不同运行时。
 */
public class EntityHelper {
    private static final Map<UUID, Float> FORCED_HEALTH_MAP = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> FORCED_MAX_HEALTH_MAP = new ConcurrentHashMap<>();
    public static volatile boolean HAS_FORCED_HEALTH = false;
    public static volatile boolean HAS_FORCED_MAX_HEALTH = false;
    private static Method getUUIDMethod = null;
    private static Method getUUIDMethodObf = null;
    private static boolean uuidMethodInitialized = false;
    private static Method getHealthMethod = null;
    private static Method getHealthMethodObf = null;
    private static Method getMaxHealthMethod = null;
    private static Method getMaxHealthMethodObf = null;
    private static boolean healthMethodsInitialized = false;
    private static Object dataHealthId = null;
    private static Field entityDataField = null;
    private static Method entityDataGetMethod = null;
    private static boolean entityDataInitialized = false;

    public static void forcedSetHealth(Object entity, float health) {
        if (entity != null) {
            UUID uuid = getEntityUUID(entity);
            if (uuid != null) {
                FORCED_HEALTH_MAP.put(uuid, health);
                HAS_FORCED_HEALTH = true;
            }
        }
    }

    public static void removeForcedHealth(Object entity) {
        if (entity != null) {
            UUID uuid = getEntityUUID(entity);
            if (uuid != null) {
                FORCED_HEALTH_MAP.remove(uuid);
                HAS_FORCED_HEALTH = !FORCED_HEALTH_MAP.isEmpty();
            }
        }
    }

    public static float getForcedHealth(Object entity) {
        if (!HAS_FORCED_HEALTH) {
            return -1.0F;
        } else if (entity == null) {
            return -1.0F;
        } else {
            UUID uuid = getEntityUUIDFast(entity);
            if (uuid == null) {
                return -1.0F;
            } else {
                Float forcedHealth = FORCED_HEALTH_MAP.get(uuid);
                return forcedHealth != null ? forcedHealth : -1.0F;
            }
        }
    }

    public static float getHealth(Object entity) {
        float forcedHealth = getForcedHealth(entity);
        return forcedHealth != -1.0F ? forcedHealth : getOriginalHealth(entity);
    }

    public static void forcedSetMaxHealth(Object entity, float maxHealth) {
        if (entity != null) {
            UUID uuid = getEntityUUID(entity);
            if (uuid != null) {
                FORCED_MAX_HEALTH_MAP.put(uuid, maxHealth);
                HAS_FORCED_MAX_HEALTH = true;
            }
        }
    }

    public static void removeForcedMaxHealth(Object entity) {
        if (entity != null) {
            UUID uuid = getEntityUUID(entity);
            if (uuid != null) {
                FORCED_MAX_HEALTH_MAP.remove(uuid);
                HAS_FORCED_MAX_HEALTH = !FORCED_MAX_HEALTH_MAP.isEmpty();
            }
        }
    }

    public static float getForcedMaxHealth(Object entity) {
        if (!HAS_FORCED_MAX_HEALTH) {
            return -1.0F;
        } else if (entity == null) {
            return -1.0F;
        } else {
            UUID uuid = getEntityUUIDFast(entity);
            if (uuid == null) {
                return -1.0F;
            } else {
                Float forcedMaxHealth = FORCED_MAX_HEALTH_MAP.get(uuid);
                return forcedMaxHealth != null ? forcedMaxHealth : -1.0F;
            }
        }
    }

    public static float getMaxHealth(Object entity) {
        float forcedMaxHealth = getForcedMaxHealth(entity);
        return forcedMaxHealth != -1.0F ? forcedMaxHealth : getOriginalMaxHealth(entity);
    }

    private static float getOriginalHealth(Object entity) {
        if (!healthMethodsInitialized) {
            initHealthMethods();
        }

        if (getHealthMethod != null) {
            try {
                Object entityData = getHealthMethod.invoke(entity);
                return (Float)entityData;
            } catch (Exception var11) {
            }
        }

        if (getHealthMethodObf != null) {
            try {
                Object entityData = getHealthMethodObf.invoke(entity);
                return (Float)entityData;
            } catch (Exception var10) {
            }
        }

        try {
            Method healthMethod = entity.getClass().getMethod("getHealth");
            Object healthValue = healthMethod.invoke(entity);
            return (Float)healthValue;
        } catch (Exception var9) {
            try {
                Method healthMethodObf = entity.getClass().getMethod("m_21223_");
                Object result = healthMethodObf.invoke(entity);
                return (Float)result;
            } catch (Exception var8) {
                if (!entityDataInitialized) {
                    initEntityData();
                }

                if (dataHealthId != null && entityDataField != null && entityDataGetMethod != null) {
                    try {
                        Object entityData = entityDataField.get(entity);
                        Object healthValuex = entityDataGetMethod.invoke(entityData, dataHealthId);
                        return (Float)healthValuex;
                    } catch (Exception var7) {
                    }
                }

                try {
                    return GetHealthMethod.getHealthFromFields(entity);
                } catch (Exception var6) {
                    return 20.0F;
                }
            }
        }
    }

    private static float getOriginalMaxHealth(Object entity) {
        if (!healthMethodsInitialized) {
            initHealthMethods();
        }

        if (getMaxHealthMethod != null) {
            try {
                Object result = getMaxHealthMethod.invoke(entity);
                return (Float)result;
            } catch (Exception var9) {
            }
        }

        if (getMaxHealthMethodObf != null) {
            try {
                Object result = getMaxHealthMethodObf.invoke(entity);
                return (Float)result;
            } catch (Exception var8) {
            }
        }

        try {
            Method maxHealthMethod = entity.getClass().getMethod("getMaxHealth");
            Object result = maxHealthMethod.invoke(entity);
            return (Float)result;
        } catch (Exception var7) {
            try {
                Method maxHealthMethodObf = entity.getClass().getMethod("m_21233_");
                Object resultx = maxHealthMethodObf.invoke(entity);
                return (Float)resultx;
            } catch (Exception var6) {
                try {
                    return GetHealthMethod.getMaxHealthFromAttributes(entity);
                } catch (Exception var5) {
                    return 20.0F;
                }
            }
        }
    }

    private static void initHealthMethods() {
        if (!healthMethodsInitialized) {
            try {
                Class<?> livingEntityClass = Class.forName("net.minecraft.world.entity.LivingEntity");

                try {
                    getHealthMethod = livingEntityClass.getMethod("getHealth");
                } catch (NoSuchMethodException var6) {
                    try {
                        getHealthMethodObf = livingEntityClass.getMethod("m_21223_");
                    } catch (NoSuchMethodException var5) {
                    }
                }

                try {
                    getMaxHealthMethod = livingEntityClass.getMethod("getMaxHealth");
                } catch (NoSuchMethodException var4) {
                    try {
                        getMaxHealthMethodObf = livingEntityClass.getMethod("m_21233_");
                    } catch (NoSuchMethodException var3) {
                    }
                }
            } catch (Exception var7) {
            }

            healthMethodsInitialized = true;
        }
    }

    private static void initEntityData() {
        if (!entityDataInitialized) {
            try {
                Class<?> livingEntityClass = Class.forName("net.minecraft.world.entity.LivingEntity");
                Field healthIdField = getFieldByNames(livingEntityClass, "f_20961_", "DATA_HEALTH_ID");
                if (healthIdField != null) {
                    healthIdField.setAccessible(true);
                    dataHealthId = healthIdField.get(null);
                }

                Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
                entityDataField = getFieldByNames(entityClass, "f_19804_", "entityData");
                if (entityDataField != null) {
                    entityDataField.setAccessible(true);
                }

                Class<?> synchedEntityDataClass = Class.forName("net.minecraft.network.syncher.SynchedEntityData");
                Method[] methods = synchedEntityDataClass.getDeclaredMethods();

                for (Method method : methods) {
                    if ((method.getName().equals("get") || method.getName().equals("m_135370_")) && method.getParameterCount() == 1) {
                        entityDataGetMethod = method;
                        entityDataGetMethod.setAccessible(true);
                        break;
                    }
                }
            } catch (Exception var9) {
            }

            entityDataInitialized = true;
        }
    }

    private static UUID getEntityUUID(Object entity) {
        return getEntityUUIDFast(entity);
    }

    private static UUID getEntityUUIDFast(Object entity) {
        if (!uuidMethodInitialized) {
            initUUIDMethods();
        }

        try {
            if (getUUIDMethod != null) {
                return (UUID)getUUIDMethod.invoke(entity);
            }

            if (getUUIDMethodObf != null) {
                return (UUID)getUUIDMethodObf.invoke(entity);
            }
        } catch (Exception var2) {
        }

        return null;
    }

    private static void initUUIDMethods() {
        if (!uuidMethodInitialized) {
            try {
                Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");

                try {
                    getUUIDMethod = entityClass.getMethod("getUUID");
                } catch (NoSuchMethodException var2) {
                    getUUIDMethodObf = entityClass.getMethod("m_20148_");
                }
            } catch (Exception var3) {
            }

            uuidMethodInitialized = true;
        }
    }

    private static Field getFieldByNames(Class<?> clazz, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException var7) {
            }
        }

        return null;
    }

    public static void clearAll() {
        FORCED_HEALTH_MAP.clear();
        HAS_FORCED_HEALTH = false;
        FORCED_MAX_HEALTH_MAP.clear();
        HAS_FORCED_MAX_HEALTH = false;
    }

    public static void clearEntity(Object entity) {
        if (entity != null) {
            UUID uuid = getEntityUUIDFast(entity);
            if (uuid != null) {
                FORCED_HEALTH_MAP.remove(uuid);
                FORCED_MAX_HEALTH_MAP.remove(uuid);
                HAS_FORCED_HEALTH = !FORCED_HEALTH_MAP.isEmpty();
                HAS_FORCED_MAX_HEALTH = !FORCED_MAX_HEALTH_MAP.isEmpty();
            }
        }
    }
}
