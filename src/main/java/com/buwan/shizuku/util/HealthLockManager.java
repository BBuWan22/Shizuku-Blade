package com.buwan.shizuku.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.LivingEntity;

/**
 * util 包下的血量锁副本（与 core/HealthLockManager 同源），
 * 供不需要 Agent 的路径使用。
 *
 * 已知问题（上游行为）：两份实现并存，改一处容易忘记另一处。
 */
public class HealthLockManager {
    private static final Map<UUID, HealthLockManager.LockedHealthData> lockedEntities = new ConcurrentHashMap<>();

    public static void lockEntityHealth(LivingEntity entity, float originalHealth, float lockedHealth, List<HealthLockManager.FieldAccessInfo> healthFields) {
        lockedEntities.put(entity.getUUID(), new HealthLockManager.LockedHealthData(originalHealth, lockedHealth, healthFields));
    }

    public static void unlockEntityHealth(LivingEntity entity) {
        lockedEntities.remove(entity.getUUID());
    }

    public static boolean isEntityLocked(LivingEntity entity) {
        return lockedEntities.containsKey(entity.getUUID());
    }

    public static HealthLockManager.LockedHealthData getLockedData(LivingEntity entity) {
        return lockedEntities.get(entity.getUUID());
    }

    public static void clearAllLocks() {
        lockedEntities.clear();
    }

    public static Set<UUID> getLockedEntityUUIDs() {
        return new HashSet<>(lockedEntities.keySet());
    }

    public static int getLockedEntityCount() {
        return lockedEntities.size();
    }

    public static void forceSetLockedFields(LivingEntity entity, float value) {
        HealthLockManager.LockedHealthData lockData = getLockedData(entity);
        if (lockData != null) {
            for (HealthLockManager.FieldAccessInfo fieldInfo : lockData.healthFields) {
                try {
                    fieldInfo.setValue(value);
                } catch (Exception var6) {
                    System.err.println("[HealthLockManager] forceSetLockedFields \u5f02\u5e38: " + var6.getMessage());
                }
            }
        }
    }

    public static boolean checkAndFixFields(LivingEntity entity) {
        HealthLockManager.LockedHealthData lockData = getLockedData(entity);
        if (lockData == null) {
            return false;
        } else {
            boolean hasChanged = false;

            for (HealthLockManager.FieldAccessInfo fieldInfo : lockData.healthFields) {
                try {
                    float currentValue = fieldInfo.getValue();
                    if (currentValue > lockData.lockedHealth) {
                        fieldInfo.setValue(lockData.lockedHealth);
                        hasChanged = true;
                    }
                } catch (Exception var61) {
                    System.err.println("[HealthLockManager] checkAndFixFields \u5f02\u5e38: " + var61.getMessage());
                }
            }

            return hasChanged;
        }
    }

    public static class FieldAccessInfo {
        public final Field field;
        public final Object targetObject;
        public final String description;
        public final float originalValue;

        public FieldAccessInfo(Field field, Object targetObject, String description, float originalValue) {
            this.field = field;
            this.targetObject = targetObject;
            this.description = description;
            this.originalValue = originalValue;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj != null && this.getClass() == obj.getClass()) {
                HealthLockManager.FieldAccessInfo that = (HealthLockManager.FieldAccessInfo)obj;
                return Objects.equals(this.field, that.field) && Objects.equals(this.targetObject, that.targetObject);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.field, this.targetObject);
        }

        public void setValue(float value) throws Exception {
            this.field.setAccessible(true);
            if (this.field.getType() == float.class) {
                this.field.setFloat(this.targetObject, value);
            } else if (this.field.getType() == double.class) {
                this.field.setDouble(this.targetObject, value);
            } else if (this.field.getType() == int.class) {
                this.field.setInt(this.targetObject, (int)value);
            }
        }

        public float getValue() throws Exception {
            this.field.setAccessible(true);
            if (this.field.getType() == float.class) {
                return this.field.getFloat(this.targetObject);
            } else if (this.field.getType() == double.class) {
                return (float)this.field.getDouble(this.targetObject);
            } else {
                return this.field.getType() == int.class ? this.field.getInt(this.targetObject) : 0.0F;
            }
        }
    }

    public static class LockedHealthData {
        public final float originalHealth;
        public final float lockedHealth;
        public final long lockTime;
        public final List<HealthLockManager.FieldAccessInfo> healthFields;

        public LockedHealthData(float originalHealth, float lockedHealth, List<HealthLockManager.FieldAccessInfo> healthFields) {
            this.originalHealth = originalHealth;
            this.lockedHealth = lockedHealth;
            this.lockTime = System.currentTimeMillis();
            this.healthFields = new ArrayList<>(healthFields);
        }
    }
}
