package com.buwan.shizuku.core;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.DataItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;

public class HealthModify {
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("78364952-8455-5678-9abc-785463259abc");

    public static boolean tagModify(Entity entity, float newValue) {
        if (entity instanceof LivingEntity le) {
            if (le.getHealth() < 0.0F) {
                return false;
            } else {
                try {
                    CompoundTag ct = new CompoundTag();
                    le.save(ct);
                    ct.putFloat("Health", newValue);
                    le.load(ct);
                    if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer) {
                        serverPlayer.connection
                            .send(
                                new ClientboundSetHealthPacket(
                                    newValue, serverPlayer.getFoodData().getFoodLevel(), serverPlayer.getFoodData().getSaturationLevel()
                                )
                            );
                    }

                    return true;
                } catch (Throwable var5) {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public static boolean attributeModifier(Entity entity, float newMaxValue) {
        if (entity instanceof LivingEntity le) {
            if (le.getHealth() < 0.0F) {
                return false;
            } else {
                AttributeInstance maxHealthAttribute = le.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealthAttribute == null) {
                    return false;
                } else {
                    maxHealthAttribute.removeModifier(HEALTH_MODIFIER_UUID);
                    double currentMax = maxHealthAttribute.getBaseValue();
                    double difference = newMaxValue - currentMax;
                    if (Math.abs(difference) > 0.001) {
                        AttributeModifier modifier = new AttributeModifier(HEALTH_MODIFIER_UUID, "health_modifier", difference, Operation.ADDITION);
                        maxHealthAttribute.addPermanentModifier(modifier);
                    }

                    return true;
                }
            }
        } else {
            return false;
        }
    }

    public static boolean traversalModify(Entity entity, Entity player, float newValue) {
        if (entity instanceof LivingEntity le) {
            if (le instanceof Player) {
                return false;
            }

            double maxHealth = le.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
            float health = le.getHealth();
            if (player instanceof Player p) {
                if (le.getHealth() > 0.0F) {
                    UltimateAttackOHelp.smartModifyMode(le, le.getHealth(), newValue, false, true);
                }

                if (le.getMaxHealth() > 0.0F) {
                    UltimateAttackOHelp.smartModifyMode(le, le.getMaxHealth(), newValue, false, true);
                }

                if (health > 0.0F) {
                    UltimateAttackOHelp.smartModifyMode(le, health, newValue, false, true);
                }

                if (maxHealth > 0.0) {
                    attributeModifier(entity, newValue);
                }

                try {
                    Field dataHealthIdField = LivingEntity.class.getDeclaredField("DATA_HEALTH_ID");
                    dataHealthIdField.setAccessible(true);
                    Object dataHealthId = dataHealthIdField.get(null);
                    le.getEntityData().set((EntityDataAccessor<Float>)dataHealthId, newValue);
                } catch (Exception var10) {
                }
            }
        }

        return true;
    }

    public static boolean entityDataModify(Entity target, float newValue) {
        if (target.isRemoved() && !target.isAddedToWorld()) {
            return false;
        } else if (!(target instanceof LivingEntity le)) {
            return false;
        } else if (le.getHealth() < 0.0F) {
            return false;
        } else {
            try {
                SynchedEntityData entityData = target.getEntityData();

                try {
                    Field itemsByIdField = SynchedEntityData.class.getDeclaredField("f_135345_");
                    itemsByIdField.setAccessible(true);
                    Map<Integer, DataItem<?>> itemsById = (Map<Integer, DataItem<?>>)itemsByIdField.get(entityData);
                    Field valueField = DataItem.class.getDeclaredField("f_135391_");
                    valueField.setAccessible(true);

                    for (DataItem<?> item : itemsById.values()) {
                        Object value = item.getValue();
                        if (value instanceof Float fgH && fgH == le.getHealth()) {
                            valueField.set(item, newValue);
                        }

                        if (value instanceof Double dgH && dgH == le.getHealth()) {
                            valueField.set(item, (double)newValue);
                        }

                        if (value instanceof Integer igH && igH.intValue() == le.getHealth()) {
                            valueField.set(item, (int)newValue);
                        }
                    }
                } catch (Exception var11) {
                }

                return true;
            } catch (Throwable var12) {
                return false;
            }
        }
    }

    public static boolean forceModify(Entity entity, float newValue, float newMaxValue) {
        if (entity instanceof LivingEntity le) {
            EntityHelper.forcedSetHealth(entity, newValue);
            EntityHelper.forcedSetMaxHealth(entity, newMaxValue);

            try {
                Field dataHealthIdField = LivingEntity.class.getDeclaredField("DATA_HEALTH_ID");
                dataHealthIdField.setAccessible(true);
                Object dataHealthId = dataHealthIdField.get(null);
                le.getEntityData().set((EntityDataAccessor<Float>)dataHealthId, newValue);
            } catch (Exception var6) {
            }
        }

        return true;
    }
}
