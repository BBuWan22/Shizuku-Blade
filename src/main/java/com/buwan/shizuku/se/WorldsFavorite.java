package com.buwan.shizuku.se;

import com.buwan.shizuku.core.AgentHealthMethodHelper;
import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent.Key;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent.Added;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class WorldsFavorite extends SpecialEffect {
    private static final Map<UUID, Boolean> coordinateShiftMode = new HashMap<>();
    private static final double SHIFT_SPEED = 0.5;

    public WorldsFavorite() {
        super(0);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity != null) {
            if (hasWorldsFavorite(entity)) {
                event.setCanceled(true);
                event.setAmount(0.0F);
                AgentHealthMethodHelper.forceSetHealth((Entity)entity, entity.getMaxHealth(), entity.getMaxHealth());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity != null) {
            if (hasWorldsFavorite(entity)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPotionEffectAdded(Added event) {
        LivingEntity entity = event.getEntity();
        if (entity != null) {
            if (hasWorldsFavorite(entity)) {
                MobEffectInstance effectInstance = event.getEffectInstance();
                if (effectInstance != null) {
                    MobEffect effect = effectInstance.getEffect();
                    if (!effect.isBeneficial()) {
                        entity.removeEffect(effect);
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity != null) {
            if (hasWorldsFavorite(entity)) {
                event.setCanceled(true);
                event.setAmount(0.0F);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTick(PlayerTickEvent event) {
        if (event.phase == Phase.END) {
            Player player = event.player;
        }
    }

    private static void applyAntiApollyonProtection(Player player) {
        try {
            AgentHealthMethodHelper.forceSetHealth((Entity)player, player.getMaxHealth(), player.getMaxHealth());
            if (player.getHealth() < player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }

            player.getPersistentData().putBoolean("CanHeal", true);
            player.getPersistentData().putBoolean("CanRegenerate", true);
            AttributeInstance movementAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementAttr != null) {
                movementAttr.getModifiers().forEach(modifier -> {
                    if (modifier.getAmount() < 0.0) {
                        movementAttr.removeModifier(modifier.getId());
                    }
                });
            }

            AttributeInstance knockbackAttr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (knockbackAttr != null) {
                knockbackAttr.getModifiers().forEach(modifier -> {
                    if (modifier.getAmount() > 0.0) {
                        knockbackAttr.removeModifier(modifier.getId());
                    }
                });
            }

            try {
                Field invulnerableTimeField = LivingEntity.class.getDeclaredField("f_19864_");
                invulnerableTimeField.setAccessible(true);
                invulnerableTimeField.setInt(player, 0);
            } catch (Exception var5) {
            }

            player.getActiveEffects().forEach(effectInstance -> {
                MobEffect effect = effectInstance.getEffect();
                if (!effect.isBeneficial()) {
                    player.removeEffect(effect);
                }
            });
            player.noPhysics = false;
            player.setNoGravity(false);

            try {
                Field canUpdateField = Entity.class.getDeclaredField("canUpdate");
                canUpdateField.setAccessible(true);
                canUpdateField.setBoolean(player, true);
            } catch (Exception var4) {
            }
        } catch (Exception var6) {
        }
    }

    public static boolean hasWorldsFavoriteStatic(LivingEntity entity) {
        ItemStack mainHand = entity.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getCapability(ItemSlashBlade.BLADESTATE).isPresent()) {
            boolean[] hasEffect = new boolean[]{false};
            mainHand.getCapability(ItemSlashBlade.BLADESTATE)
                .ifPresent(
                    state -> {
                        if (state.hasSpecialEffect(SERegistry.WORLDS_FAVORITE.getId())) {
                            hasEffect[0] = SpecialEffect.isEffective(
                                SERegistry.WORLDS_FAVORITE.getId(), entity instanceof Player ? ((Player)entity).experienceLevel : 0
                            );
                        }
                    }
                );
            if (hasEffect[0]) {
                return true;
            }
        }

        ItemStack offHand = entity.getOffhandItem();
        if (!offHand.isEmpty() && offHand.getCapability(ItemSlashBlade.BLADESTATE).isPresent()) {
            boolean[] hasEffect = new boolean[]{false};
            offHand.getCapability(ItemSlashBlade.BLADESTATE)
                .ifPresent(
                    state -> {
                        if (state.hasSpecialEffect(SERegistry.WORLDS_FAVORITE.getId())) {
                            hasEffect[0] = SpecialEffect.isEffective(
                                SERegistry.WORLDS_FAVORITE.getId(), entity instanceof Player ? ((Player)entity).experienceLevel : 0
                            );
                        }
                    }
                );
            if (hasEffect[0]) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasWorldsFavorite(LivingEntity entity) {
        return hasWorldsFavoriteStatic(entity);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKeyInput(Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null && mc.player != null) {
            if (hasWorldsFavorite(mc.player)) {
                int keyCode = event.getKey();
                if ((keyCode == 342 || keyCode == 346) && event.getAction() == 1) {
                    long window = mc.getWindow().getWindow();
                    boolean shiftDown = InputConstants.isKeyDown(window, 340) || InputConstants.isKeyDown(window, 344);
                    if (shiftDown) {
                        UUID playerId = mc.player.getUUID();
                        boolean currentMode = coordinateShiftMode.getOrDefault(playerId, false);
                        coordinateShiftMode.put(playerId, !currentMode);
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCoordinateShiftTick(PlayerTickEvent event) {
        if (event.phase == Phase.END) {
            Player player = event.player;
            UUID playerId = player.getUUID();
            boolean modeEnabled = coordinateShiftMode.getOrDefault(playerId, false);
            if (modeEnabled && !hasWorldsFavorite(player)) {
                coordinateShiftMode.put(playerId, false);
            } else if (modeEnabled) {
                if (player.level().isClientSide) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != player) {
                        return;
                    }

                    double dx = 0.0;
                    double dy = 0.0;
                    double dz = 0.0;
                    long window = mc.getWindow().getWindow();
                    if (InputConstants.isKeyDown(window, 87)) {
                        dz += 0.5;
                    }

                    if (InputConstants.isKeyDown(window, 83)) {
                        dz -= 0.5;
                    }

                    if (InputConstants.isKeyDown(window, 65)) {
                        dx += 0.5;
                    }

                    if (InputConstants.isKeyDown(window, 68)) {
                        dx -= 0.5;
                    }

                    if (InputConstants.isKeyDown(window, 32)) {
                        dy += 0.5;
                    }

                    if (InputConstants.isKeyDown(window, 340)) {
                        dy -= 0.5;
                    }

                    if (dx != 0.0 || dy != 0.0 || dz != 0.0) {
                        float rotation = player.getYRot();
                        double cos = Math.cos(Math.toRadians(rotation));
                        double sin = Math.sin(Math.toRadians(rotation));
                        double rotatedDx = dx * cos + dz * sin;
                        double rotatedDz = -dx * sin + dz * cos;
                        Vec3 newPos = player.position().add(rotatedDx, dy, rotatedDz);
                        String tpCommand = String.format("tp @s %f %f %f", newPos.x, newPos.y, newPos.z);
                        if (mc.player != null && mc.player.connection != null) {
                            mc.player.connection.sendUnsignedCommand(tpCommand);
                        }

                        player.setPos(newPos.x, newPos.y, newPos.z);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        coordinateShiftMode.remove(playerId);
    }
}
