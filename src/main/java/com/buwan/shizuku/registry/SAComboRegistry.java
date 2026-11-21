package com.buwan.shizuku.registry;

import com.buwan.shizuku.SlashEffectUtils;
import com.buwan.shizuku.shizuku;
import com.buwan.shizuku.client.SynthwaveShaderManager;
import com.buwan.shizuku.sa.CosmicCrossSlash;
import com.buwan.shizuku.sa.GalaxyArc;
import com.buwan.shizuku.sa.Leisa;
import com.buwan.shizuku.sa.MoonlightSlash;
import com.buwan.shizuku.sa.Shizukusa;
import com.buwan.shizuku.sa.StarRiverSlash;
import com.buwan.shizuku.sa.SummerSlash;
import com.buwan.shizuku.sa.TeleportSlash;
import com.buwan.shizuku.sa.TimeSlash;
import com.buwan.shizuku.sa.TornadoSlash;
import com.buwan.shizuku.sa.TsubameGaeshi;
import com.buwan.shizuku.sa.UnlimitedBladeWorks;
import com.buwan.shizuku.sa.VoidPrison;
import com.buwan.shizuku.sa.Yingsa;
import com.buwan.shizuku.sa.Ymsa;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.event.client.UserPoseOverrider;
import mods.flammpfeil.slashblade.init.DefaultResources;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.registry.combo.ComboState.Builder;
import mods.flammpfeil.slashblade.registry.combo.ComboState.TimeLineTickAction;
import mods.flammpfeil.slashblade.registry.combo.ComboState.TimeoutNext;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SAComboRegistry {
    public static final DeferredRegister<ComboState> COMBO_STATES = DeferredRegister.create(ComboState.REGISTRY_KEY, "shizuku");
    public static final RegistryObject<ComboState> ALL_REUSE = COMBO_STATES.register(
        "all_reuse",
        Builder.newInstance()
            .startAndEnd(459, 488)
            .priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(entity -> SlashBlade.prefix("none"))
            .nextOfTimeout(entity -> SlashBlade.prefix("none"))
            .addTickAction(TimeLineTickAction.getBuilder().put(0, AttackManager::playQuickSheathSoundAction).build())
            .releaseAction(ComboState::releaseActionQuickCharge)::build
    );
    public static final RegistryObject<ComboState> shizukusa = COMBO_STATES.register(
        "shizukusa",
        Builder.newInstance()
            .startAndEnd(400, 459)
            .priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
            .nextOfTimeout(entity -> shizuku.prefix("all_reuse"))
            .addTickAction(
                TimeLineTickAction.getBuilder()
                    .put(2, entityIn -> AttackManager.doSlash(entityIn, -30.0F, Vec3.ZERO, false, false, SlashEffectUtils.addslashbladesaDamage(entityIn, 1)))
                    .put(3, entityIn -> Shizukusa.doSlash(entityIn, 2.5F, -13421569, false))
                    .build()
            )
            .addHitEffect(StunManager::setStun)::build
    );
    public static final RegistryObject<ComboState> LEISA = COMBO_STATES.register(
        "leisa",
        Builder.newInstance()
            .startAndEnd(400, 459)
            .priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
            .nextOfTimeout(entity -> shizuku.prefix("all_reuse"))
            .addTickAction(TimeLineTickAction.getBuilder().put(3, entityIn -> Leisa.doLeisaSlash(entityIn)).build())::build
    );
    public static final RegistryObject<ComboState> YINGSA = COMBO_STATES.register(
        "yingsa",
        Builder.newInstance()
            .startAndEnd(400, 430)
            .priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
            .nextOfTimeout(entity -> shizuku.prefix("all_reuse"))
            .addTickAction(TimeLineTickAction.getBuilder().put(3, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    Yingsa.doYingSa(entityIn);
                }
            }).build())
            .releaseAction(ComboState::releaseActionQuickCharge)::build
    );
    public static final RegistryObject<ComboState> TELEPORT_SLASH = COMBO_STATES.register(
        "teleport_slash",
        Builder.newInstance()
            .startAndEnd(400, 430)
            .priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
            .nextOfTimeout(entity -> shizuku.prefix("all_reuse"))
            .addTickAction(TimeLineTickAction.getBuilder().put(3, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    TeleportSlash.doTeleportSlash(entityIn);
                }
            }).build())
            .releaseAction(ComboState::releaseActionQuickCharge)::build
    );
    public static final RegistryObject<ComboState> TIME_SLASH = COMBO_STATES.register(
        "time_slash",
        Builder.newInstance()
            .startAndEnd(400, 430)
            .priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
            .nextOfTimeout(entity -> shizuku.prefix("all_reuse"))
            .addTickAction(TimeLineTickAction.getBuilder().put(3, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    TimeSlash.doTimeSlash(entityIn);
                }
            }).build())
            .releaseAction(ComboState::releaseActionQuickCharge)::build
    );
    public static final RegistryObject<ComboState> TORNADO_SLASH = COMBO_STATES.register(
        "tornado_slash",
        Builder.newInstance()
            .startAndEnd(400, 430)
            .priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
            .nextOfTimeout(entity -> shizuku.prefix("all_reuse"))
            .addTickAction(TimeLineTickAction.getBuilder().put(3, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    TornadoSlash.doTornadoSlash(entityIn);
                }
            }).build())
            .releaseAction(ComboState::releaseActionQuickCharge)::build
    );
    public static final RegistryObject<ComboState> UNLIMITED_BLADE_WORKS = COMBO_STATES.register(
        "unlimited_blade_works",
        Builder.newInstance()
            .startAndEnd(400, 430)
            .priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
            .nextOfTimeout(entity -> shizuku.prefix("all_reuse"))
            .addTickAction(TimeLineTickAction.getBuilder().put(3, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    UnlimitedBladeWorks.doUnlimitedBladeWorks(entityIn);
                }
            }).build())
            .releaseAction(ComboState::releaseActionQuickCharge)::build
    );
    public static final RegistryObject<ComboState> MOONLIGHT_SLASH = COMBO_STATES.register(
        "moonlight_slash",
        Builder.newInstance()
            .startAndEnd(400, 430)
            .priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
            .nextOfTimeout(entity -> shizuku.prefix("all_reuse"))
            .addTickAction(TimeLineTickAction.getBuilder().put(3, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    MoonlightSlash.doMoonlightSlash(entityIn);
                }
            }).build())
            .releaseAction(ComboState::releaseActionQuickCharge)::build
    );
    public static final RegistryObject<ComboState> TSUBAME_GAESHI = COMBO_STATES.register(
        "tsubame_gaeshi",
        Builder.newInstance()
            .startAndEnd(400, 430)
            .priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
            .nextOfTimeout(entity -> shizuku.prefix("all_reuse"))
            .addTickAction(TimeLineTickAction.getBuilder().put(3, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    TsubameGaeshi.doTsubameGaeshi(entityIn);
                }
            }).build())
            .releaseAction(ComboState::releaseActionQuickCharge)::build
    );
    public static final RegistryObject<ComboState> VOID_PRISON = COMBO_STATES.register(
        "void_prison",
        Builder.newInstance()
            .startAndEnd(400, 430)
            .priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
            .nextOfTimeout(entity -> shizuku.prefix("all_reuse"))
            .addTickAction(TimeLineTickAction.getBuilder().put(3, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    VoidPrison.doVoidPrison(entityIn);
                }
            }).build())
            .releaseAction(ComboState::releaseActionQuickCharge)::build
    );
    public static final RegistryObject<ComboState> COSMIC_CROSS_SLASH = COMBO_STATES.register(
        "cosmic_cross_slash",
        Builder.newInstance()
            .startAndEnd(400, 430)
            .priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
            .nextOfTimeout(entity -> shizuku.prefix("all_reuse"))
            .addTickAction(TimeLineTickAction.getBuilder().put(3, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    CosmicCrossSlash.doCosmicCrossSlash(entityIn);
                }
            }).build())
            .releaseAction(ComboState::releaseActionQuickCharge)::build
    );
    public static final RegistryObject<ComboState> YMSA = COMBO_STATES.register(
        "ymsa",
        Builder.newInstance()
            .startAndEnd(400, 430)
            .priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
            .nextOfTimeout(entity -> shizuku.prefix("all_reuse"))
            .addTickAction(TimeLineTickAction.getBuilder().put(3, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    Ymsa.doYmsaSlash(entityIn);
                }
            }).build())
            .releaseAction((user, elapsed) -> {
                if (user.level().isClientSide && user instanceof Player) {
                    SynthwaveShaderManager.stopShader();
                }

                return ComboState.releaseActionQuickCharge(user, elapsed);
            })::build
    );
    public static final RegistryObject<ComboState> SUMMER_SLASH = COMBO_STATES.register(
        "summer_slash",
        Builder.newInstance()
            .startAndEnd(400, 430)
            .priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
            .nextOfTimeout(entity -> shizuku.prefix("all_reuse"))
            .addTickAction(TimeLineTickAction.getBuilder().put(3, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    SummerSlash.doSummerSlash(entityIn);
                }
            }).build())
            .releaseAction(ComboState::releaseActionQuickCharge)::build
    );
    public static final RegistryObject<ComboState> COSMIC_STANDBY = COMBO_STATES.register(
        "cosmic_standby",
        Builder.newInstance()
            .startAndEnd(0, 1)
            .loop()
            .timeout(1000)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(StarRiverSlash::standbyCommand)
            .nextOfTimeout(entity -> SlashBlade.prefix("none"))
            .addTickAction(UserPoseOverrider::resetRot)::build
    );
    public static final RegistryObject<ComboState> GALAXY_ARC = COMBO_STATES.register(
        "galaxy_arc",
        Builder.newInstance()
            .startAndEnd(400, 430)
            .priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
            .nextOfTimeout(entity -> shizuku.prefix("all_reuse"))
            .addTickAction(TimeLineTickAction.getBuilder().put(2, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    GalaxyArc.fireGiantDaoguang(entityIn);
                }
            }).put(4, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    GalaxyArc.fireQiSlash(entityIn, 0);
                }
            }).put(7, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    GalaxyArc.fireQiSlash(entityIn, 1);
                }
            }).put(10, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    GalaxyArc.fireQiSlash(entityIn, 2);
                }
            }).put(13, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    GalaxyArc.fireQiSlash(entityIn, 3);
                }
            }).put(16, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    GalaxyArc.fireQiSlash(entityIn, 4);
                }
            }).put(19, entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    GalaxyArc.fireQiSlash(entityIn, 5);
                }
            }).build())
            .releaseAction(ComboState::releaseActionQuickCharge)::build
    );
    public static final RegistryObject<ComboState> COSMIC_ATTACK = COMBO_STATES.register(
        "cosmic_attack",
        Builder.newInstance()
            .startAndEnd(1, 10)
            .priority(100)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(TimeoutNext.buildFromFrame(5, entity -> SlashBlade.prefix("none")))
            .nextOfTimeout(entity -> shizuku.prefix("cosmic_attack_end"))
            .clickAction(entityIn -> {
                if (entityIn instanceof LivingEntity) {
                    StarRiverSlash.firePhantomSlash(entityIn);
                }
            })
            .addTickAction(UserPoseOverrider::resetRot)
            .addHitEffect(StunManager::setStun)::build
    );
    public static final RegistryObject<ComboState> COSMIC_ATTACK_END = COMBO_STATES.register(
        "cosmic_attack_end",
        Builder.newInstance()
            .startAndEnd(10, 21)
            .priority(100)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(entity -> SlashBlade.prefix("none"))
            .nextOfTimeout(entity -> shizuku.prefix("cosmic_attack_end2"))
            .addTickAction(TimeLineTickAction.getBuilder().put(0, AttackManager::playQuickSheathSoundAction).build())
            .releaseAction(ComboState::releaseActionQuickCharge)::build
    );
    public static final RegistryObject<ComboState> COSMIC_ATTACK_END2 = COMBO_STATES.register(
        "cosmic_attack_end2",
        Builder.newInstance()
            .startAndEnd(21, 41)
            .priority(100)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(entity -> SlashBlade.prefix("none"))
            .nextOfTimeout(entity -> SlashBlade.prefix("none"))::build
    );
}
