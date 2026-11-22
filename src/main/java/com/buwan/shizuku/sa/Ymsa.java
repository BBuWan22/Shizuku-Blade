package com.buwan.shizuku.sa;

import com.buwan.shizuku.mixin.ChunkMapAccessor;
import com.buwan.shizuku.mixin.ClassInstanceMultiMapAccessor;
import com.buwan.shizuku.mixin.DamageSourcesAccessor;
import com.buwan.shizuku.mixin.EntityAccessor;
import com.buwan.shizuku.mixin.EntityLookupAccessor;
import com.buwan.shizuku.mixin.EntityManagerAccessor;
import com.buwan.shizuku.mixin.EntitySectionAccessor;
import com.buwan.shizuku.mixin.EntityTickListAccessor;
import com.buwan.shizuku.mixin.LivingEntityAccessor;
import com.buwan.shizuku.mixin.ServerLevelAccessor;
import com.buwan.shizuku.mixin.SynchedEntityDataAccessor;
import com.buwan.shizuku.network.PacketHandler;
import com.buwan.shizuku.network.JudgmentImpactPacket;
import com.buwan.shizuku.network.YmsaForceRemovePacket;
import com.buwan.shizuku.util.LootDropController;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData.DataItem;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

/**
 * 「夜冥杀」：本 mod 最复杂的一个 SA（900 行），包含蓄力判定、范围标记、
 * 批量处决、掉落处理、boss 血条清理以及给客户端发 YmsaForceRemovePacket。
 *
 * 流程大致是：蓄力 -> 标记目标 -> 强制移除（走 UltimateEntityRemover 的多级降级）
 * -> 补掉落 -> 清理状态。
 *
 * 注意：这里为了「真正杀死」目标，做了大量违反常规 API 的操作，
 * 遇到与其它 mod 的兼容问题时要先看本类与 core/ 下的工具。
 */
public class Ymsa {
    public static final int STRIKE_TICK = 8;
    public static final int RIFT_CLOSED_TICK = 38;
    public static final int VERDICT_TICK = 42;
    public static final int DOMAIN_RADIUS = 128;
    public static final int SHEATH_TICK = 54;
    public static final int END_TICK = 60;
    public static final int RECOVERY_FRAME = END_TICK * 3 / 2;
    public static final ResourceLocation MOTION = new ResourceLocation("shizuku", "combostate/judgment.vmd");
    private static final double KILL_RADIUS = 256.0;
    /** 解放过载时的处决半径：在原本 320 的基础上再放大，覆盖整片战场。 */
    private static final double OVERDRIVE_RADIUS = 512.0;
    private static final float KILL_DAMAGE = Float.MAX_VALUE;
    private static final boolean KILL_PLAYERS = true;
    private static final boolean KILL_TOTEM_BYPASS = true;
    private static final boolean SPARE_CASTER_PETS = true;
    private static final int RESPAWN_TRACK_TICKS = 1200;
    private static final int BOSS_SUPPRESS_TICKS = 1200;
    private static final float HIGH_HP_THRESHOLD = 1000.0F;
    private static final List<String> PROTECTION_TAGS = List.of("Immortal", "Protected", "Invincible", "Unkillable", "GodMode", "BossImmune", "Undying", "god");
    private static final Map<UUID, YmsaLogic.CleanupEntry> RESPAWN_TRACKER = new ConcurrentHashMap<>();
    private static final Map<String, Integer> TYPE_SUPPRESSION = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> PENDING_DROPS = new ConcurrentHashMap<>();
    private static final Set<UUID> ANNIHILATED = ConcurrentHashMap.newKeySet();
    private static final ResourceLocation CHAOS_WITHER = ResourceLocation.fromNamespaceAndPath("chaoswither", "chaos_wither");
    static final TagKey<EntityType<?>> BOSS_TYPES = TagKey.create(Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("forge", "bosses"));
    private record SpawnBlock(UUID caster, ServerLevel level, AABB area, long expires, Set<EntityType<?>> types,
                              List<Vec3> bossPositions) {
        boolean matches(LivingEntity entity) {
            if (entity.level() != level || level.getGameTime() >= expires || !inVerdictArea(area, entity)
                    || entity instanceof Player || entity instanceof TamableAnimal pet && caster.equals(pet.getOwnerUUID())) return false;
            if (types.contains(entity.getType())) return true;
            // Some encounters create a different boss type at the previous form's position on death.
            if (level.getGameTime() - (expires - BOSS_SUPPRESS_TICKS) > 20 || !isUnleashedBoss(entity)) return false;
            for (Vec3 position : bossPositions) {
                if (position.distanceToSqr(entity.position()) <= 64) return true;
            }
            return false;
        }
    }
    private static final List<SpawnBlock> SPAWN_BLOCKS = new ArrayList<>();
    private static final Map<UUID, ServerLevel> ANNIHILATION_PURGE = new ConcurrentHashMap<>();
    private static final Map<UUID, Entity> PENDING_WIPE = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(Ymsa.class);
    private static final List<String> CLASS_BOSS_FRAGMENTS = List.of(
        "wither",
        "dragon",
        "boss",
        "titan",
        "chaos",
        "demon",
        "god",
        "silence",
        "execution",
        "zilla",
        "superbee",
        "agent",
        "ancient",
        "legendary",
        "mythic",
        "void",
        "shadow",
        "dark",
        "crystal",
        "elemental",
        "guardian",
        "golem",
        "construct",
        "abomination",
        "horror",
        "nightmare"
    );
    private static final List<String> NAME_BOSS_FRAGMENTS = List.of(
        "\u51cb\u7075",
        "\u9f99",
        "\u6df1\u6e0a",
        "\u7ec8\u5f71",
        "\u6267\u884c",
        "\u8fdc\u53e4",
        "\u4f20\u8bf4",
        "\u795e\u8bdd",
        "\u865a\u7a7a",
        "\u6697\u5f71",
        "\u9ed1\u6697",
        "\u5143\u7d20",
        "\u5b88\u62a4\u8005",
        "\u5080\u5121",
        "\u6784\u9020\u4f53",
        "\u618e\u6076",
        "\u6050\u6016",
        "\u68a6\u9b47",
        "\u673a\u68b0",
        "\u738b",
        "\u5973\u738b",
        "\u6cf0\u5766",
        "\u5deb\u5996",
        "\u51e4\u51f0",
        "\u6076\u9b54",
        "\u5929\u4f7f",
        "\u9886\u4e3b",
        "\u4e3b\u8111",
        "\u7687\u5e1d",
        "\u9738\u4e3b",
        "\u4e3b\u5bb0",
        "\u59cb\u7956"
    );

    public static void beginJudgment(LivingEntity caster) {
        if (caster instanceof ServerPlayer player) {
            broadcastPhase(player, 1);
        }
    }

    private static void broadcastPhase(ServerPlayer player, int phaseTick) {
        PacketHandler.sendToNearby(JudgmentImpactPacket.from(player, phaseTick), player, DOMAIN_RADIUS * 2.5);
    }

    public static void openRift(LivingEntity caster) {
        if (caster instanceof ServerPlayer player) {
            if (UnleashedJudgment.isCasting(player)) UnleashedVerdicts.get(player.serverLevel().getServer()).lockTargets(player);
            protectMainHandBlade(player);
            broadcastPhase(player, STRIKE_TICK);
            player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.72F);
        }
    }

    public static InteractionResult doYmsaSlash(LivingEntity entityIn) {
        if (entityIn instanceof ServerPlayer player) {
            broadcastPhase(player, VERDICT_TICK);
            player.level().playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.4F, 0.65F);
            boolean overdrive = UnleashedJudgment.isCasting(player) && JudgmentExecution.supported();
            if (overdrive) JudgmentExecution.arm();

            try {
                performYmsaCast(player);
            } catch (Exception var3) {
                LOGGER.error("[SA] Ymsa \u65bd\u6cd5\u5f02\u5e38(\u5df2\u9694\u79bb, \u670d\u52a1\u5668\u7ee7\u7eed\u8fd0\u884c): {}", var3.toString());
            } finally {
                // 过载只覆盖本次处决窗口，结束后立刻关掉，避免污染常规战斗
                if (overdrive) JudgmentExecution.disarm();
            }

            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    public static void clearAllSAState() {
        UnleashedJudgment.clear();
        processing.clear();
        lastProcessed.clear();
        SPAWN_BLOCKS.clear();
        RESPAWN_TRACKER.clear();
        TYPE_SUPPRESSION.clear();
        PENDING_DROPS.clear();
        ANNIHILATED.clear();
        ANNIHILATION_PURGE.clear();
        PENDING_WIPE.clear();
    }

    public static boolean isTickSuppressed(LivingEntity entity) {
        return ANNIHILATED.contains(entity.getUUID());
    }

    public static void clearPlayerState(UUID playerUUID) {
        YmsaLogic.filterByKiller(RESPAWN_TRACKER, playerUUID);
    }

    private static void protectMainHandBlade(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (UnleashedJudgment.isBlade(mainHand) && mainHand.isDamageableItem()) {
            mainHand.setDamageValue(mainHand.getDamageValue());
            if (!mainHand.hasTag()) {
                mainHand.setTag(new CompoundTag());
            }

            CompoundTag tag = mainHand.getTag();
            tag.putBoolean("Unbreakable", true);
            tag.putInt("RepairCost", 0);
        }
    }

    private static void performYmsaCast(ServerPlayer player) {
        ServerLevel level = (ServerLevel)player.level();
        boolean unleashed = UnleashedJudgment.isCasting(player);
        double radius = unleashed ? (JudgmentExecution.armed() ? OVERDRIVE_RADIUS : 320.0) : KILL_RADIUS;
        AABB area = player.getBoundingBox().inflate(radius);
        int killed = 0;
        int forcedDrops = 0;
        int annihilated = 0;

        Set<LivingEntity> targets = new LinkedHashSet<>();
        JudgmentIndex stores = unleashed ? new JudgmentIndex(level) : null;
        Set<Entity> cores = new LinkedHashSet<>();
        if (unleashed) {
            for (Entity root : stores.verdictTargets(area, player.getUUID())) {
                if (root instanceof LivingEntity living) targets.add(living);
                else if (JudgmentEncounters.identifies(root)) cores.add(root);
            }
        } else {
            targets.addAll(level.getEntitiesOfClass(LivingEntity.class, area, e -> shouldKill(e, player)));
            for (PartEntity<?> part : level.getPartEntities()) {
                if (part.getBoundingBox().intersects(area) && part.getParent() instanceof LivingEntity parent && shouldKill(parent, player))
                    targets.add(parent);
            }
        }
        Set<EntityType<?>> bossTypes = new HashSet<>();
        if (unleashed) {
            List<Vec3> bossPositions = new ArrayList<>();
            var verdicts = UnleashedVerdicts.get(level.getServer());
            var locked = verdicts.land(player);
            if (!locked.entities().isEmpty()) {
                for (ServerLevel dimension : level.getServer().getAllLevels()) {
                    var elsewhere = dimension == level ? stores : new JudgmentIndex(dimension);
                    for (Entity candidate : elsewhere.entities) {
                        if (candidate instanceof Player || candidate instanceof net.minecraft.world.entity.OwnableEntity pet
                                && player.getUUID().equals(pet.getOwnerUUID()) || !locked.matches(candidate)) continue;
                        if (dimension == level && candidate instanceof LivingEntity living) targets.add(living);
                        else if (dimension == level) cores.add(candidate);
                        else {
                            if (!locked.entities().contains(candidate.getUUID())) verdicts.record(player.getUUID(), candidate, false);
                            removeUnleashedTarget(elsewhere, candidate, player);
                        }
                    }
                }
            }
            verdicts.includeSimultaneousVerdicts(player.getUUID(), level, area);
            for (LivingEntity target : targets) {
                boolean boss = isUnleashedBoss(target);
                verdicts.record(player.getUUID(), target, boss);
                if (boss) {
                    bossTypes.add(target.getType());
                    bossPositions.add(target.position());
                }
            }
            for (Entity core : cores) verdicts.record(player.getUUID(), core, true);
            JudgmentEncounters.collectControllers(level, area, player.getUUID(), verdicts);
            for (Entity candidate : stores.entities) {
                if (!(candidate instanceof LivingEntity) && !(candidate instanceof PartEntity<?>) && verdicts.matches(level, candidate))
                    cores.add(candidate);
            }
            for (Entity core : cores) if (removeUnleashedTarget(stores, core, null)) annihilated++;
            if (net.minecraftforge.fml.ModList.get().isLoaded("chaoswither")) {
                EntityType<?> chaos = ForgeRegistries.ENTITY_TYPES.getValue(CHAOS_WITHER);
                if (chaos != null) bossTypes.add(chaos);
            }
            if (!bossTypes.isEmpty()) SPAWN_BLOCKS.add(new SpawnBlock(player.getUUID(), level, area,
                level.getGameTime() + Math.min(BOSS_SUPPRESS_TICKS, JudgmentConfig.RETENTION.get()), bossTypes, bossPositions));
        }
        for (LivingEntity target : targets) {
            if (unleashed) {
                if (removeUnleashedTarget(stores, target, player)) {
                    killed++;
                    annihilated++;
                }
                continue;
            }
            if (SuperSteveCompat.isBoss(target)) {
                if (!target.isAlive()) continue;
                if (SuperSteveCompat.execute(target)) {
                    killed++;
                    continue;
                }
            }
            Ymsa.KillOutcome outcome = killTarget(target, player, level);
            if (outcome.killed) {
                killed++;
            }

            if (outcome.forcedDrops) {
                forcedDrops++;
            }

            if (outcome.annihilated) {
                annihilated++;
            }
        }

        int playersKilled = damageNearbyPlayers(player, area, unleashed);

        int projectilesCleared = clearHostileProjectiles(level, area);
        int objectsCleared = sweepBossObjects(player, level);
        LootDropController.conditionalCleanup(player, 256.0);
        LOGGER.info(
            "[SA] Ymsa \u91ca\u653e: \u51fb\u6740\u5b9e\u4f53 {} \u4e2a (\u5f3a\u5236\u6389\u843d {} \u4e2a, \u7edd\u5bf9\u62b9\u9664 {} \u4e2a), \u51fb\u6740\u73a9\u5bb6 {} \u540d, \u62b9\u9664\u5bf9\u8c61 {} \u4e2a, \u6e05\u9664\u6295\u5c04\u7269 {} \u4e2a",
            new Object[]{killed, forcedDrops, annihilated, playersKilled, objectsCleared, projectilesCleared}
        );
    }

    private static boolean shouldKill(LivingEntity e, ServerPlayer caster) {
        if (e instanceof Player) return false;
        boolean isOwnPet = e instanceof TamableAnimal ta && caster.getUUID().equals(ta.getOwnerUUID());
        return YmsaLogic.shouldKillPure(caster.getUUID().equals(e.getUUID()), isOwnPet, e.isRemoved() && e.getRemovalReason() != null, false);
    }

    private static EntityLookupAccessor entityIndex(ServerLevel level) {
        var manager = ((ServerLevelAccessor)level).ymsa$getEntityManager();
        return (EntityLookupAccessor)((EntityManagerAccessor)manager).ymsa$getVisibleEntityStorage();
    }

    static boolean isUnleashedBoss(LivingEntity target) {
        if (isChaosWither(target) || SuperSteveCompat.isBoss(target) || target.getType().is(BOSS_TYPES) || JudgmentEncounters.identifies(target)) return true;
        var health = target.getAttribute(Attributes.MAX_HEALTH);
        return health != null && health.getValue() >= 100.0;
    }

    private static boolean inVerdictArea(AABB area, Entity entity) {
        return area.intersects(entity.getBoundingBox()) || area.contains(entity.position());
    }

    private static final Set<UUID> processing = new HashSet<>();
    private static final Map<UUID, Integer> lastProcessed = new java.util.HashMap<>();

    private static boolean removeUnleashedTarget(JudgmentIndex stores, Entity target, @Nullable ServerPlayer caster) {
        if (target instanceof Player) return false;
        int tick = stores.level.getServer().getTickCount();
        UUID id = target.getUUID();
        if (lastProcessed.getOrDefault(id, -1) == tick || !processing.add(id)) return false;
        try {
            lastProcessed.put(id, tick);
            if (caster != null && target instanceof LivingEntity living) {
                try {
                    // 解放路径才是审判真正走的分支（上面的 killTarget 只处理非解放）。
                    // 这里先把无敌窗口清零、把血量内存值写成 0 —— 且不受下面的
                    // isUnleashedBoss 条件限制，Boss 同样被内存级打穿。
                    JudgmentExecution.breakInvulnerability(living);
                    if (JudgmentExecution.armed()) JudgmentExecution.fakeDeathState(living);
                    var adapter = JudgmentEncounters.adapter(living);
                    if (adapter != null) adapter.beforeRemoval(stores.level, living);
                    else if (living.getClass().getName().startsWith("net.minecraft.") && !isUnleashedBoss(living)) {
                        living.setLastHurtByPlayer(caster);
                        living.hurt(ymsaKillSource(stores.level, caster), KILL_DAMAGE);
                    }
                } catch (RuntimeException ex) {
                    LOGGER.warn("[Judgment] death callback failed for {}", id, ex);
                }
            }
            return detachUnleashedTarget(stores, target);
        } catch (RuntimeException ex) {
            LOGGER.warn("[Judgment] failed to verify removal of {}", id, ex);
            return false;
        } finally {
            processing.remove(id);
        }
    }

    private static boolean detachUnleashedTarget(JudgmentIndex stores, Entity target) {
        var level = stores.level;
        int id = target.getId();
        UUID uuid = target.getUUID();
        EntityAccessor state = (EntityAccessor)target;
        EntityInLevelCallback callback = state.ymsa$getLevelCallback();
        // Let the owning section manager update tracking, ticking and lookup together.
        // This boss's health and removal overrides can run unrelated combat logic.
        Set<Entity> pieces = new LinkedHashSet<>();
        pieces.add(target);
        pieces.addAll(stores.parts.getOrDefault(target, List.of()));
        if (target.isMultipartEntity() && target.getParts() != null)
            for (PartEntity<?> part : target.getParts()) if (part != null) pieces.add(part);
        state.ymsa$setRemovalReason(RemovalReason.KILLED);
        try {
            callback.onRemove(RemovalReason.KILLED);
        } catch (RuntimeException ex) {
            LOGGER.warn("[Judgment] failed to unregister target {}", uuid, ex);
        }
        state.ymsa$setAddedToWorld(false);
        level.getPartEntities().removeIf(part -> part.getParent() == target || part == target);
        boolean removed = true;
        for (Entity piece : pieces) removed &= stores.detach(piece);
        if (removed) {
            JudgmentEncounters.finishEntity(level, target);
            if (target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon) {
                var fight = dragon.getDragonFight();
                if (fight != null && uuid.equals(fight.getDragonUUID()) && !fight.saveData().dragonKilled())
                    fight.setDragonKilled(dragon);
            }
            var packet = new YmsaForceRemovePacket(pieces.stream().mapToInt(Entity::getId).toArray(),
                pieces.stream().map(Entity::getUUID).toArray(UUID[]::new), new boolean[0]);
            for (ServerPlayer viewer : level.players()) PacketHandler.sendToPlayer(packet, viewer);
        }
        LOGGER.info("[Judgment] target={} id={} removedFromIndex={}", uuid, id, removed);
        return removed;
    }

    static void finishChaosEncounter(ServerLevel level, Entity boss) {
        for (ServerLevel world : level.getServer().getAllLevels()) {
            for (Entity other : entityIndex(world).ymsa$getById().values()) {
                if (other.getType() == boss.getType()) return;
            }
        }
        // 0.9fix sets this encounter flag in its constructor and uses it to request new bosses.
        try {
            boss.getClass().getField("goneToWorld").setBoolean(null, false);
        } catch (ReflectiveOperationException ex) {
            LOGGER.debug("[Judgment] No 0.9fix encounter flag on {}", boss.getType(), ex);
        }
    }

    public static boolean blocksChaosSpawn(ServerLevel level, Vec3 position) {
        return UnleashedVerdicts.get(level.getServer()).blocksSpawn(level, CHAOS_WITHER, position)
            || SPAWN_BLOCKS.stream().anyMatch(block -> block.level == level && block.types.contains(ForgeRegistries.ENTITY_TYPES.getValue(CHAOS_WITHER))
            && level.getGameTime() < block.expires && block.area.contains(position));
    }

    static void clearUnleashedArea(UUID caster) {
        SPAWN_BLOCKS.removeIf(block -> block.caster.equals(caster));
    }

    private static int clearHostileProjectiles(ServerLevel level, AABB area) {
        int cleared = 0;

        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area)) {
            if (!(projectile.getOwner() instanceof Player)) {
                projectile.discard();
                cleared++;
            }
        }

        return cleared;
    }

    static int damageNearbyPlayers(ServerPlayer caster, AABB area, boolean unleashed) {
        if (!UnleashedJudgment.canDamagePlayers(caster)) return 0;
        ServerLevel level = caster.serverLevel();
        DamageSource source = ymsaKillSource(level, caster);
        if (source.getEntity() == null) source = level.damageSources().playerAttack(caster);
        int killed = 0;
        for (ServerPlayer target : level.getEntitiesOfClass(ServerPlayer.class, area, player -> player != caster && player.isAlive())) {
            if (!caster.canHarmPlayer(target) || (unleashed && UnleashedJudgment.isCasting(target))) continue;
            if (target.hurt(source, KILL_DAMAGE) && target.isDeadOrDying()) killed++;
        }
        return killed;
    }

    private static DamageSource ymsaKillSource(ServerLevel level, @Nullable ServerPlayer attacker) {
        try {
            ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("shizuku", "ymsa_kill"));
            return ((DamageSourcesAccessor)level.damageSources()).ymsa$source(key, attacker, attacker);
        } catch (Exception var3) {
            LOGGER.debug("[SA] \u81ea\u5b9a\u4e49\u4f24\u5bb3\u7c7b\u578b shizuku:ymsa_kill \u4e0d\u53ef\u7528, \u56de\u9000 genericKill: {}", var3.toString());
            return level.damageSources().genericKill();
        }
    }

    private static Ymsa.KillOutcome killTarget(LivingEntity target, ServerPlayer player, ServerLevel level) {
        Ymsa.KillOutcome outcome = new Ymsa.KillOutcome();
        if (target.isRemoved()) {
            return outcome;
        } else if (!target.isAlive()) {
            try {
                if (target.getRemovalReason() == null) {
                    target.setRemoved(RemovalReason.KILLED);
                }

                if (level.getEntity(target.getId()) == target) {
                    outcome.annihilated = annihilate(target, player, level);
                }

                outcome.killed = true;
            } catch (Exception var16) {
                LOGGER.warn(
                    "[SA] Ymsa \u6e05\u7406\u6b7b\u4ea1\u6b8b\u7559 {} \u5931\u8d25(\u5df2\u9694\u79bb): {}", target.getName().getString(), var16.toString()
                );

                try {
                    if (level.getEntity(target.getId()) == target) {
                        outcome.annihilated = annihilate(target, player, level);
                    }
                } catch (Exception var15) {
                }
            }

            return outcome;
        } else {
            DamageSource src = ymsaKillSource(level, player);
            UUID targetId = target.getUUID();
            PENDING_DROPS.put(targetId, Boolean.TRUE);

            try {
                stripProtections(target);
                // Unsafe 层：先把无敌/受击窗口清零，再把 DATA_HEALTH_ID 的 DataItem.value
                // 直接写成 0 —— 绕开 setHealth() 的 clamp(0, maxHealth)，
                // 血量锁 / 无限血 Boss 也吃这一刀。后面的 hurt/die/remove 作为兜底链路保留。
                JudgmentExecution.breakInvulnerability(target);
                if (JudgmentExecution.armed()) JudgmentExecution.fakeDeathState(target);
                target.setLastHurtByPlayer(player);
                target.hurt(src, Float.MAX_VALUE);
                if (!target.isDeadOrDying()) {
                    target.getCombatTracker().recordDamage(src, Float.MAX_VALUE);
                    target.setHealth(0.0F);
                    target.die(src);
                }

                if (!target.isDeadOrDying()) {
                    EntityDataAccessor<Float> healthId = LivingEntityAccessor.ymsa$getDataHealthId();
                    target.getEntityData().set(healthId, 1.0F);
                    if (!target.isRemoved() && !target.isDeadOrDying()) {
                        target.getEntityData().set(healthId, 0.0F);
                        target.getCombatTracker().recordDamage(src, Float.MAX_VALUE);
                        target.die(src);
                    }
                }

                if (PENDING_DROPS.remove(targetId) != null && !target.isRemoved()) {
                    ((LivingEntityAccessor)target).ymsa$dropAllDeathLoot(src);
                    outcome.forcedDrops = true;
                }

                if (!target.isRemoved()) {
                    target.remove(RemovalReason.KILLED);
                }

                if (target.getRemovalReason() == null) {
                    target.setRemoved(RemovalReason.KILLED);
                }

                outcome.killed = true;
                if (isBossLike(target)) {
                    broadcastClientErase(target, level, false);
                }

                if (level.getEntity(target.getId()) == target) {
                    outcome.annihilated = annihilate(target, player, level);
                }
            } catch (Exception var19) {
                LOGGER.warn(
                    "[SA] Ymsa \u51fb\u6740 {} \u5931\u8d25(\u5df2\u9694\u79bb, \u4e0d\u5f71\u54cd\u5176\u4ed6\u76ee\u6807): {}",
                    target.getName().getString(),
                    var19.toString()
                );

                try {
                    if (!target.isRemoved()) {
                        target.remove(RemovalReason.KILLED);
                    }
                } catch (Exception var18) {
                }

                try {
                    if (level.getEntity(target.getId()) == target) {
                        outcome.annihilated = annihilate(target, player, level);
                    }
                } catch (Exception var17) {
                }
            } finally {
                PENDING_DROPS.remove(targetId);
            }

            if (!target.isRemoved()) {
                suppressRespawn(target, player);
            }

            return outcome;
        }
    }

    private static void stripProtections(LivingEntity target) {
        target.removeAllEffects();
        target.setInvulnerable(false);
        CompoundTag data = target.getPersistentData();

        for (String tag : PROTECTION_TAGS) {
            data.remove(tag);
        }
    }

    private static void suppressRespawn(LivingEntity target, ServerPlayer player) {
        RESPAWN_TRACKER.put(target.getUUID(), new YmsaLogic.CleanupEntry(player.getUUID(), 1200));
        if (isChaosWither(target)) return;
        if (isBossLike(target)) {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
            if (key != null) {
                TYPE_SUPPRESSION.put(key.toString(), 1200);
            }
        }
    }

    private static boolean isBossLike(LivingEntity entity) {
        boolean byKeywords = YmsaLogic.matchesBossKeywordsPure(
            entity.getClass().getName().toLowerCase(), entity.getName().getString().toLowerCase(), CLASS_BOSS_FRAGMENTS, NAME_BOSS_FRAGMENTS
        );
        return byKeywords || entity.getMaxHealth() > 1000.0F || !isMinecraftNamespace(entity);
    }

    private static boolean isMinecraftNamespace(LivingEntity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && "minecraft".equals(key.getNamespace());
    }

    private static boolean annihilate(LivingEntity target, ServerPlayer player, ServerLevel level) {
        UUID targetId = target.getUUID();
        ANNIHILATED.add(targetId);

        try {
            DamageSource src = ymsaKillSource(level, player);
            wipeBossHealth(target);

            try {
                target.getEntityData().set(LivingEntityAccessor.ymsa$getDataHealthId(), 1.0F);
            } catch (Exception var7) {
            }

            for (int i = 0; i < 30 && target.isAlive() && !target.isRemoved(); i++) {
                clearHitCooldowns(target);
                target.hurt(src, Float.MAX_VALUE);
            }

            if (PENDING_DROPS.remove(targetId) != null && !target.isRemoved()) {
                ((LivingEntityAccessor)target).ymsa$dropAllDeathLoot(src);
            }

            target.kill();
            if (level.getEntity(target.getId()) == target) {
                physicallyRemove(target, level);
            }

            ANNIHILATION_PURGE.put(targetId, level);
            LOGGER.warn("[SA] Ymsa \u7edd\u5bf9\u62b9\u9664\u76ee\u6807(\u6301\u7eed\u51c0\u5316): {}", target.getName().getString());
            return true;
        } catch (Exception var8) {
            LOGGER.warn("[SA] Ymsa \u7edd\u5bf9\u62b9\u9664 {} \u5931\u8d25(\u5df2\u9694\u79bb): {}", target.getName().getString(), var8.toString());

            try {
                if (level.getEntity(target.getId()) == target) {
                    physicallyRemove(target, level);
                }
            } catch (Exception var6) {
            }

            return false;
        }
    }

    private static void wipeBossHealth(LivingEntity target) {
        try {
            float rawHealth = target.getEntityData().get(LivingEntityAccessor.ymsa$getDataHealthId());

            try {
                target.setHealth(0.0F);
            } catch (Exception var10) {
            }

            try {
                target.getEntityData().set(LivingEntityAccessor.ymsa$getDataHealthId(), 0.0F);
            } catch (Exception var9) {
            }

            try {
                Int2ObjectMap<DataItem<?>> items = ((SynchedEntityDataAccessor)target.getEntityData()).ymsa$getItemsById();
                ObjectIterator dead = items.values().iterator();

                while (dead.hasNext()) {
                    DataItem<?> itemEntry = (DataItem<?>)dead.next();
                    if (itemEntry.getValue() instanceof Float f && Math.abs(f - rawHealth) < 0.01F) {
                        ((DataItem<Float>)itemEntry).setValue(0.0F);
                    }
                }
            } catch (Exception var12) {
            }

            for (Class<?> clazz = target.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
                for (Field field : clazz.getDeclaredFields()) {
                    try {
                        if (field.getType() == float.class) {
                            field.setAccessible(true);
                            float fv = field.getFloat(target);
                            if (Math.abs(fv - rawHealth) < 0.01F) {
                                field.setFloat(target, 0.0F);
                            }
                        }
                    } catch (Exception var8) {
                    }
                }
            }

            for (Class<?> clazz = target.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
                try {
                    Field dead = clazz.getDeclaredField("isDead");
                    dead.setAccessible(true);
                    if (dead.getType() == boolean.class) {
                        dead.setBoolean(target, true);
                    }
                    break;
                } catch (NoSuchFieldException var11) {
                }
            }
        } catch (Exception var13) {
        }
    }

    private static void clearHitCooldowns(LivingEntity target) {
        target.invulnerableTime = 0;

        try {
            for (Class<?> clazz = target.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
                try {
                    Field field = clazz.getDeclaredField("iFrames");
                    field.setAccessible(true);
                    field.setInt(target, 0);
                    break;
                } catch (NoSuchFieldException var3) {
                }
            }
        } catch (Exception var4) {
        }
    }

    private static void physicallyRemove(LivingEntity target, ServerLevel level) {
        ((EntityAccessor)target).ymsa$setRemovalReason(RemovalReason.KILLED);
        ((EntityAccessor)target).ymsa$setAddedToWorld(false);
        target.setLevelCallback(EntityInLevelCallback.NULL);
        PersistentEntitySectionManager<Entity> manager = ((ServerLevelAccessor)level).ymsa$getEntityManager();
        ((EntityManagerAccessor)manager).ymsa$getKnownUuids().remove(target.getUUID());
        EntityLookup<Entity> lookup = ((EntityManagerAccessor)manager).ymsa$getVisibleEntityStorage();
        ((EntityLookupAccessor)lookup).ymsa$getByUuid().remove(target.getUUID());
        ((EntityLookupAccessor)lookup).ymsa$getById().remove(target.getId());
        long sectionPos = SectionPos.of(target.blockPosition()).asLong();
        EntitySectionStorage<Entity> storage = ((EntityManagerAccessor)manager).ymsa$getSectionStorage();
        EntitySection<Entity> section = storage.getSection(sectionPos);
        if (section != null) {
            try {
                section.remove(target);
            } catch (Exception var13) {
            }

            try {
                Map<Class<?>, List<?>> byClass = ((ClassInstanceMultiMapAccessor)((EntitySectionAccessor)section).ymsa$getStorage()).ymsa$getByClass();

                for (List<?> list : byClass.values()) {
                    list.remove(target);
                }
            } catch (Exception var14) {
            }
        }

        EntityTickList tickList = ((ServerLevelAccessor)level).ymsa$getEntityTickList();
        ((EntityTickListAccessor)tickList).ymsa$getActive().remove(target.getId());
        ((EntityTickListAccessor)tickList).ymsa$getPassive().remove(target.getId());
        ((ServerLevelAccessor)level).ymsa$getNavigatingMobs().remove(target);

        try {
            level.getChunkSource().removeEntity(target);
        } catch (Exception var12) {
        }

        try {
            ChunkMap chunkMap = level.getChunkSource().chunkMap;
            Int2ObjectMap<?> entityMap = ((ChunkMapAccessor)chunkMap).ymsa$getEntityMap();
            if (entityMap != null) {
                entityMap.remove(target.getId());
            }
        } catch (Exception var11) {
        }

        broadcastClientErase(target, level, true);
        if (target.isMultipartEntity()) {
            level.getPartEntities().removeIf(part -> part.getParent() == target);
        }

        target.onRemovedFromWorld();
        MinecraftForge.EVENT_BUS.post(new EntityLeaveLevelEvent(target, level));
        PENDING_WIPE.put(target.getUUID(), target);
    }

    private static void broadcastClientErase(LivingEntity target, ServerLevel level, boolean typeKill) {
        try {
            int[] entityIds;
            if (target.isMultipartEntity()) {
                PartEntity<?>[] parts = target.getParts();
                entityIds = new int[1 + parts.length];
                entityIds[0] = target.getId();

                for (int i = 0; i < parts.length; i++) {
                    entityIds[i + 1] = parts[i].getId();
                }
            } else {
                entityIds = new int[]{target.getId()};
            }

            boolean[] typeKills = new boolean[entityIds.length];
            Arrays.fill(typeKills, typeKill);

            try {
                List<DataValue<?>> values = target.getEntityData().getNonDefaultValues();
                if (values != null && !values.isEmpty()) {
                    level.getServer().getPlayerList().broadcastAll(new ClientboundSetEntityDataPacket(target.getId(), values), level.dimension());
                }
            } catch (Exception var8) {
            }

            level.getServer().getPlayerList().broadcastAll(new ClientboundRemoveEntitiesPacket(entityIds), level.dimension());

            try {
                UUID[] uuids = new UUID[entityIds.length];
                uuids[0] = target.getUUID();
                if (target.isMultipartEntity()) {
                    PartEntity<?>[] parts = target.getParts();

                    for (int i = 0; i < parts.length; i++) {
                        uuids[i + 1] = parts[i].getUUID();
                    }
                }

                PacketHandler.sendToAll(new YmsaForceRemovePacket(entityIds, uuids, typeKills));
            } catch (Exception var9) {
            }
        } catch (Exception var10) {
        }
    }

    private static void wipeEntityFields(Entity entity) {
        try {
            for (Class<?> clazz = entity.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
                for (Field field : clazz.getDeclaredFields()) {
                    try {
                        int mod = field.getModifiers();
                        if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod)) {
                            field.setAccessible(true);
                            Class<?> type = field.getType();
                            if (type == boolean.class) {
                                field.setBoolean(entity, false);
                            } else if (type == byte.class) {
                                field.setByte(entity, (byte)0);
                            } else if (type == short.class) {
                                field.setShort(entity, (short)0);
                            } else if (type == int.class) {
                                field.setInt(entity, 0);
                            } else if (type == long.class) {
                                field.setLong(entity, 0L);
                            } else if (type == float.class) {
                                field.setFloat(entity, 0.0F);
                            } else if (type == double.class) {
                                field.setDouble(entity, 0.0);
                            } else if (type == Boolean.class) {
                                field.set(entity, Boolean.FALSE);
                            } else if (type == Byte.class) {
                                field.set(entity, (byte)0);
                            } else if (type == Short.class) {
                                field.set(entity, (short)0);
                            } else if (type == Integer.class) {
                                field.set(entity, 0);
                            } else if (type == Long.class) {
                                field.set(entity, 0L);
                            } else if (type == Float.class) {
                                field.set(entity, 0.0F);
                            } else if (type == Double.class) {
                                field.set(entity, 0.0);
                            } else if (type == String.class) {
                                field.set(entity, "");
                            } else if (Collection.class.isAssignableFrom(type)) {
                                if (field.get(entity) instanceof Collection<?> collection) {
                                    try {
                                        collection.clear();
                                    } catch (Exception var11) {
                                    }
                                }
                            } else if (type == RemovalReason.class) {
                                field.set(entity, RemovalReason.DISCARDED);
                            }
                        }
                    } catch (Exception var12) {
                    }
                }
            }
        } catch (Exception var13) {
        }
    }

    private static void processPendingPhysicalRemovals(MinecraftServer server) {
        if (!ANNIHILATION_PURGE.isEmpty()) {
            for (Entry<UUID, ServerLevel> entry : ANNIHILATION_PURGE.entrySet()) {
                ServerLevel level = entry.getValue();
                if (level != null && level == server.getLevel(level.dimension()) && level.getEntity(entry.getKey()) instanceof LivingEntity living) {
                    try {
                        physicallyRemove(living, level);
                    } catch (Exception var7) {
                        LOGGER.warn("[SA] Ymsa \u6301\u7eed\u51c0\u5316 {} \u5931\u8d25(\u5df2\u9694\u79bb): {}", living.getName().getString(), var7.toString());
                    }
                }
            }
        }
    }

    private static int sweepBossObjects(ServerPlayer player, ServerLevel level) {
        int cleared = 0;

        try {
            Class<?> levelClass = Class.forName("flashfur.omnimobs.entities.metapotent_flashfur.MetapotentFlashfurLevel");
            Field listField = levelClass.getDeclaredField("metapotentFlashfurList");
            listField.setAccessible(true);
            if (!(listField.get(null) instanceof List<?> list && !list.isEmpty())) {
                return 0;
            }

            List<Object> victims = new ArrayList<>();

            for (Object obj : list) {
                if (obj != null) {
                    try {
                        Vec3 pos = (Vec3)obj.getClass().getField("pos").get(obj);
                        if (player.position().distanceTo(pos) <= 256.0) {
                            victims.add(obj);
                        }
                    } catch (Exception var14) {
                    }
                }
            }

            if (victims.isEmpty()) {
                return 0;
            }

            for (Object victim : victims) {
                try {
                    Object bossEvent = victim.getClass().getField("bossEvent").get(victim);
                    if (bossEvent != null) {
                        bossEvent.getClass().getMethod("setVisible", boolean.class).invoke(bossEvent, false);

                        try {
                            bossEvent.getClass().getMethod("removeAllPlayers").invoke(bossEvent);
                        } catch (Exception var12) {
                        }
                    }
                } catch (Exception var13) {
                }
            }

            if (removeObjectsFromProtectedList(list, victims)) {
                if (!UnleashedJudgment.isCasting(player)) deleteFlashfurSaveFiles(level.getServer());
                cleared = victims.size();
            }
        } catch (ClassNotFoundException var15) {
            return 0;
        } catch (Exception var16) {
            LOGGER.warn("[SA] Ymsa \u6e05\u626b\u70ab\u5f69\u732b\u5931\u8d25(\u5df2\u9694\u79bb): {}", var16.toString());
        }

        return cleared;
    }

    private static boolean removeObjectsFromProtectedList(List<?> list, List<Object> victims) {
        try {
            ArrayList<?> arrayList = (ArrayList<?>)list;
            Field dataField = ArrayList.class.getDeclaredField("elementData");
            Field sizeField = ArrayList.class.getDeclaredField("size");
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe)unsafeField.get(null);
            long dataOffset = unsafe.objectFieldOffset(dataField);
            long sizeOffset = unsafe.objectFieldOffset(sizeField);
            Object[] data = (Object[])unsafe.getObject(arrayList, dataOffset);
            int size = unsafe.getInt(arrayList, sizeOffset);
            Set<Object> victimSet = new HashSet<>(victims);
            List<Object> survivors = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                Object obj = data[i];
                if (obj != null && !victimSet.contains(obj)) {
                    survivors.add(obj);
                }
            }

            unsafe.putObject(arrayList, dataOffset, survivors.toArray());
            unsafe.putInt(arrayList, sizeOffset, survivors.size());
            return true;
        } catch (Exception var17) {
            LOGGER.warn("[SA] Ymsa Unsafe \u91cd\u5efa\u70ab\u5f69\u732b\u5217\u8868\u5931\u8d25: {}", var17.toString());
            return false;
        }
    }

    private static void deleteFlashfurSaveFiles(MinecraftServer server) {
        try {
            Path dir = server.getWorldPath(LevelResource.ROOT).resolve("metapotent_flashfurs");
            if (Files.isDirectory(dir)) {
                try (Stream<Path> stream = Files.list(dir)) {
                    stream.filter(p -> p.getFileName().toString().endsWith(".omnimob")).forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception var2x) {
                        }
                    });
                }
            }
        } catch (Exception var7) {
        }
    }

    public static void handleServerTickEnd(MinecraftServer server) {
        UnleashedJudgment.tick(server);
        if (server.overworld() != null && server.getTickCount() % 20 == 0) {
            var verdicts = UnleashedVerdicts.get(server);
            int slot = Math.floorMod(server.getTickCount() / 20, 5);
            if (verdicts.prune(server.overworld().getGameTime())) {
                for (ServerLevel level : server.getAllLevels()) {
                    if (Math.floorMod(level.dimension().location().hashCode(), 5) != slot) continue;
                    if (!verdicts.hasLoadedMatch(level)) continue;
                    var stores = new JudgmentIndex(level);
                    for (Entity entity : stores.entities) {
                        if (entity instanceof PartEntity<?> part && stores.entities.contains(part.getParent())) continue;
                        if (verdicts.matches(level, entity)) removeUnleashedTarget(stores, entity, null);
                    }
                }
            }
        }
        if (!SPAWN_BLOCKS.isEmpty()) {
            SPAWN_BLOCKS.removeIf(block -> block.level != server.getLevel(block.level.dimension())
                || block.level.getGameTime() >= block.expires);
        }
        lastProcessed.entrySet().removeIf(entry -> entry.getValue() != server.getTickCount());
        YmsaLogic.tickCleanups(RESPAWN_TRACKER);
        YmsaLogic.tickTypeSuppression(TYPE_SUPPRESSION);
        processPendingPhysicalRemovals(server);
        processPendingWipes();
    }

    private static void processPendingWipes() {
        if (!PENDING_WIPE.isEmpty()) {
            for (Entry<UUID, Entity> entry : PENDING_WIPE.entrySet()) {
                try {
                    wipeEntityFields(entry.getValue());
                } catch (Exception var3) {
                    LOGGER.warn("[SA] Ymsa \u5b57\u6bb5\u6e05\u7a7a\u5931\u8d25(\u5df2\u9694\u79bb): {}", var3.toString());
                }
            }

            PENDING_WIPE.clear();
        }
    }

    public static void handleDropsProcessed(LivingEntity entity) {
        if (entity != null) {
            PENDING_DROPS.remove(entity.getUUID());
        }
    }

    static boolean isChaosWither(Entity entity) {
        return CHAOS_WITHER.equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
    }

    public static boolean shouldCancelJoin(Entity entity) {
        if (blocksUnleashedJoin(entity)) return true;
        if (entity instanceof LivingEntity living) {
            if (entity.level() instanceof ServerLevel level && isChaosWither(entity)) {
                // The boss requests more copies at its fixed position while the first is still present.
                // Keep the first one targetable until the sword lands.
                if (level.players().stream().anyMatch(p -> UnleashedJudgment.holdsEnabledBlade(p)
                        && inVerdictArea(p.getBoundingBox().inflate(320), entity))) {
                    for (Entity existing : entityIndex(level).ymsa$getById().values()) {
                        if (existing.getType() == entity.getType() && existing.position().distanceToSqr(entity.position()) < 1.0)
                            return true;
                    }
                }
            }
            if (RESPAWN_TRACKER.containsKey(living.getUUID())) {
                return true;
            } else {
                ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
                return key != null && TYPE_SUPPRESSION.containsKey(key.toString());
            }
        } else {
            return false;
        }
    }

    public static boolean blocksUnleashedJoin(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level) || level.getServer().overworld() == null
                || !level.getServer().isSameThread()) return false;
        if (!UnleashedVerdicts.get(level.getServer()).matches(level, entity)
                && !(entity instanceof LivingEntity living && SPAWN_BLOCKS.stream().anyMatch(block -> block.matches(living)))) return false;
        if (isChaosWither(entity)) finishChaosEncounter(level, entity);
        return true;
    }

    @SubscribeEvent
    public static void onUnleashedEntityJoin(EntityJoinLevelEvent event) {
        if (blocksUnleashedJoin(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !UnleashedJudgment.isCasting(player))
            UnleashedJudgment.cancelCast(event.getEntity().getUUID());
        clearPlayerState(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        UnleashedJudgment.disconnect(event.getEntity().getUUID());
        clearPlayerState(event.getEntity().getUUID());
    }

    private static final class KillOutcome {
        boolean killed;
        boolean forcedDrops;
        boolean annihilated;
    }
}
