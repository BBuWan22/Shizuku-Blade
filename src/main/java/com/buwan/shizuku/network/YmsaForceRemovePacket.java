package com.buwan.shizuku.network;

import com.buwan.shizuku.client.YmsaClientErase;
import com.buwan.shizuku.client.YmsaRenderKiller;
import com.buwan.shizuku.mixin.ClassInstanceMultiMapAccessor;
import com.buwan.shizuku.mixin.ClientLevelAccessor;
import com.buwan.shizuku.mixin.EntityAccessor;
import com.buwan.shizuku.mixin.EntityLookupAccessor;
import com.buwan.shizuku.mixin.EntitySectionAccessor;
import com.buwan.shizuku.mixin.EntityTickListAccessor;
import com.buwan.shizuku.mixin.SynchedEntityDataAccessor;
import com.buwan.shizuku.mixin.TransientEntitySectionManagerAccessor;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData.DataItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * 服务端 -> 客户端：夜冥杀的批量去除指令。
 *
 * 同时带三种信息（实体 id、UUID、按类型整类杀），客户端收到后把对应实体加进
 * 「抹除」集合并跳过渲染；实体 id 与 UUID 都带是为了兼容「客户端还没收到生成包」的情况。
 */
@EventBusSubscriber(Dist.CLIENT)
public class YmsaForceRemovePacket {
    private final int[] entityIds;
    private final UUID[] entityUuids;
    private final boolean[] typeKills;

    public YmsaForceRemovePacket(int[] entityIds, UUID[] entityUuids, boolean[] typeKills) {
        this.entityIds = entityIds;
        this.entityUuids = entityUuids;
        this.typeKills = typeKills;
    }

    public static void encode(YmsaForceRemovePacket msg, FriendlyByteBuf buf) {
        buf.writeVarIntArray(msg.entityIds);
        buf.writeVarInt(msg.entityUuids.length);

        for (UUID uuid : msg.entityUuids) {
            buf.writeUUID(uuid);
        }

        buf.writeVarInt(msg.typeKills.length);

        for (boolean flag : msg.typeKills) {
            buf.writeBoolean(flag);
        }
    }

    public static YmsaForceRemovePacket decode(FriendlyByteBuf buf) {
        int[] ids = buf.readVarIntArray();
        int count = buf.readVarInt();
        UUID[] uuids = new UUID[count];

        for (int i = 0; i < count; i++) {
            uuids[i] = buf.readUUID();
        }

        int killCount = buf.readVarInt();
        boolean[] typeKills = new boolean[killCount];

        for (int i = 0; i < killCount; i++) {
            typeKills[i] = buf.readBoolean();
        }

        return new YmsaForceRemovePacket(ids, uuids, typeKills);
    }

    public static void handle(YmsaForceRemovePacket msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            for (UUID uuid : msg.entityUuids) {
                YmsaClientErase.markErased(uuid);
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.level != null) {
                ClientLevel level = mc.level;

                for (int i = 0; i < msg.entityIds.length; i++) {
                    int id = msg.entityIds[i];
                    Entity entity = level.getEntity(id);
                    if (entity != null) {
                        try {
                            forceRemoveClientEntity(entity, level);
                            purgeBossTrackers(entity);
                            if (msg.typeKills != null && i < msg.typeKills.length && msg.typeKills[i]) {
                                YmsaRenderKiller.killEntity(entity);
                            }
                        } catch (Exception var7) {
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void purgeBossTrackers(Entity entity) {
        try {
            Class<?> handleClass = Class.forName("com.wzz.witherzilla.event.EventHandle");
            Field listField = handleClass.getDeclaredField("livingEntities");
            listField.setAccessible(true);
            if (listField.get(null) instanceof Collection<?> list) {
                list.remove(entity);
            }
        } catch (Exception var6) {
        }

        try {
            Class<?> handleClass = Class.forName("net.wzz.chaoswither.event.EventHandle");
            Field listField = handleClass.getDeclaredField("livingEntities");
            listField.setAccessible(true);
            if (listField.get(null) instanceof Collection<?> list) {
                list.remove(entity);
            }
        } catch (Exception var5) {
        }
    }

    private static void forceRemoveClientEntity(Entity entity, ClientLevel level) {
        try {
            ObjectIterator manager = ((SynchedEntityDataAccessor)entity.getEntityData()).ymsa$getItemsById().values().iterator();

            while (manager.hasNext()) {
                DataItem<?> item = (DataItem<?>)manager.next();
                Object value = item.getValue();
                if (value instanceof Float) {
                    ((DataItem<Float>)item).setValue(0.0F);
                }
            }
        } catch (Exception var15) {
        }

        ((EntityAccessor)entity).ymsa$setRemovalReason(RemovalReason.KILLED);
        entity.setLevelCallback(EntityInLevelCallback.NULL);
        TransientEntitySectionManager<Entity> manager = ((ClientLevelAccessor)level).ymsa$getEntityStorage();
        EntityLookup<Entity> lookup = ((TransientEntitySectionManagerAccessor)manager).ymsa$getEntityStorage();
        ((EntityLookupAccessor)lookup).ymsa$getByUuid().remove(entity.getUUID());
        ((EntityLookupAccessor)lookup).ymsa$getById().remove(entity.getId());
        long sectionPos = SectionPos.of(entity.blockPosition()).asLong();
        EntitySectionStorage<Entity> storage = ((TransientEntitySectionManagerAccessor)manager).ymsa$getSectionStorage();
        EntitySection<Entity> section = storage.getSection(sectionPos);
        if (section != null) {
            try {
                section.remove(entity);
            } catch (Exception var13) {
            }

            try {
                Map<Class<?>, List<?>> byClass = ((ClassInstanceMultiMapAccessor)((EntitySectionAccessor)section).ymsa$getStorage()).ymsa$getByClass();

                for (List<?> list : byClass.values()) {
                    list.remove(entity);
                }
            } catch (Exception var14) {
            }
        }

        EntityTickList tickList = ((ClientLevelAccessor)level).ymsa$getTickingEntities();
        ((EntityTickListAccessor)tickList).ymsa$getActive().remove(entity.getId());
        ((EntityTickListAccessor)tickList).ymsa$getPassive().remove(entity.getId());

        try {
            level.getPartEntities().removeIf(part -> part.getId() == entity.getId());
        } catch (Exception var12) {
        }

        try {
            ((ClientLevelAccessor)level).ymsa$getPartEntities().remove(entity.getId());
        } catch (Exception var11) {
        }
    }
}
