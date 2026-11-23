package com.buwan.shizuku.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * 把指定实体 / 整类实体的渲染器替换成 NullRenderer，让它们「虽然还在服务端列表里
 * 但客户端不再绘制」——夜冥杀批量清场时用，减少一次删几百个实体造成的卡顿。
 */
public class YmsaRenderKiller {
    private static final Set<EntityType<?>> KILLED_TYPES = ConcurrentHashMap.newKeySet();
    private static EntityRenderer<Entity> nullRenderer;

    public static void killEntity(Entity entity) {
        if (entity != null) {
            killType(entity.getType());
        }
    }

    public static void clear() {
        KILLED_TYPES.clear();
        nullRenderer = null;
    }

    public static void killType(EntityType<?> type) {
        // 只处理原版实体：模组实体常有自定义渲染器，别的 mod 会把自己的渲染器强转回去
        // （例如 SuperSteve 的 SSEvents$Client#renderLevelStage 会 cast 成 SuperSteveRenderer），
        // 我们把整类换成 NullRenderer 就会让它们 ClassCastException 崩客户端。
        if (type != null && !"minecraft".equals(
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())) {
            return;
        }
        if (type != null) {
            KILLED_TYPES.add(type);
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.level != null) {
                reassert(mc.getEntityRenderDispatcher());
            }
        }
    }

    public static void reassert(EntityRenderDispatcher dispatcher) {
        if (dispatcher != null && !KILLED_TYPES.isEmpty()) {
            boolean needRebuild = false;

            for (EntityType<?> type : KILLED_TYPES) {
                EntityRenderer<?> current = dispatcher.renderers.get(type);
                if (current == null || !(current instanceof YmsaRenderKiller.NullRenderer)) {
                    needRebuild = true;
                    break;
                }
            }

            if (needRebuild) {
                replaceForKilledTypes(dispatcher);
            }
        }
    }

    private static synchronized void replaceForKilledTypes(EntityRenderDispatcher dispatcher) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null) {
                return;
            }

            if (nullRenderer == null) {
                nullRenderer = new YmsaRenderKiller.NullRenderer<>(
                    new Context(
                        dispatcher,
                        mc.getItemRenderer(),
                        mc.getBlockRenderer(),
                        mc.gameRenderer.itemInHandRenderer,
                        mc.getResourceManager(),
                        mc.getEntityModels(),
                        mc.font
                    )
                );
            }

            Map<EntityType<?>, EntityRenderer<?>> renderers = dispatcher.renderers;
            Builder<EntityType<?>, EntityRenderer<?>> builder = ImmutableMap.builder();

            for (Entry<EntityType<?>, EntityRenderer<?>> entry : renderers.entrySet()) {
                builder.put(entry.getKey(), KILLED_TYPES.contains(entry.getKey()) ? nullRenderer : entry.getValue());
            }

            dispatcher.setRenderShadow(false);
            dispatcher.setRenderHitBoxes(false);
            dispatcher.renderers = builder.build();
        } catch (Exception var6) {
        }
    }

    public static class NullRenderer<T extends Entity> extends EntityRenderer<T> {
        public NullRenderer(Context context) {
            super(context);
            this.shadowRadius = 0.0F;
            this.shadowStrength = 0.0F;
        }

        @Override
        protected int getSkyLightLevel(T entity, BlockPos pos) {
            return 0;
        }

        @Override
        protected int getBlockLightLevel(T entity, BlockPos pos) {
            return 0;
        }

        @Override
        public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
            return false;
        }

        @Override
        public void render(T entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        }

        @Override
        protected boolean shouldShowName(T entity) {
            return false;
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return null;
        }

        @Override
        protected void renderNameTag(T entity, Component name, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        }
    }
}
