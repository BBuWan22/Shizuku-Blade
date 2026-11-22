package com.buwan.shizuku.sa;

import com.buwan.shizuku.shizuku;
import com.buwan.shizuku.util.DamageCalculator;
import java.util.ArrayList;
import java.util.List;
import mods.flammpfeil.slashblade.SlashBlade.RegistryEvents;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.EntityJudgementCut;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.Drive;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 「雫草」：最基础的 SA，按速度参数生成数道刀光；重载版本可指定颜色与是否播声音。
 */
public class Shizukusa {
    private static final ResourceLocation[] LEISA_SOUNDS = new ResourceLocation[]{
        shizuku.LEISA_SOUND1.getId(), shizuku.LEISA_SOUND2.getId(), shizuku.LEISA_SOUND3.getId()
    };

    public static void doSlash(LivingEntity playerIn, float speed) {
        int colorCode = playerIn.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).map(ISlashBladeState::getColorCode).orElse(-13421569);
        doSlash(playerIn, speed, colorCode, true);
    }

    public static void doSlash(LivingEntity playerIn, float speed, int colorCode, boolean playSound) {
        if (!playerIn.level().isClientSide()) {
            if (playSound) {
                RandomSource random = playerIn.getRandom();
                ResourceLocation soundLoc = LEISA_SOUNDS[random.nextInt(LEISA_SOUNDS.length)];
                SoundEvent sound = (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(soundLoc);
                if (sound != null) {
                    playerIn.level()
                        .playSound(
                            null,
                            playerIn.getX(),
                            playerIn.getY(),
                            playerIn.getZ(),
                            sound,
                            SoundSource.PLAYERS,
                            1.0F,
                            1.0F + (random.nextFloat() - 0.5F) * 0.2F
                        );
                }
            }

            playerIn.getMainHandItem()
                .getCapability(ItemSlashBlade.BLADESTATE)
                .ifPresent(
                    state -> {
                        Level worldIn = playerIn.level();
                        RandomSource randomx = playerIn.getRandom();
                        Vec3 lookAngle = playerIn.getLookAngle();

                        for (int i = -1; i <= 1; i++) {
                            float angle = i * 120.0F;
                            Vec3 rotatedLook = rotateY(lookAngle, angle);
                            EntityDrive drive = Drive.doSlash(
                                playerIn,
                                angle * 0.5F,
                                40,
                                new Vec3(0.0, 0.5, 0.0).scale(0.8 + randomx.nextFloat() * 0.4),
                                true,
                                6.0,
                                speed * (0.9F + randomx.nextFloat() * 0.2F)
                            );
                            if (drive != null) {
                                drive.setSilent(true);
                                float refinedDamage = DamageCalculator.calculateRefinedDamage(playerIn, 1.0F);
                                drive.setDamage(refinedDamage);
                            }
                        }

                        doFocusedJudgementCut(playerIn, 18.0F, 160.0F);
                    }
                );
        }
    }

    private static Vec3 rotateY(Vec3 vec, float angleDegrees) {
        float angleRad = angleDegrees * (float) (Math.PI / 180.0);
        float cos = Mth.cos(angleRad);
        float sin = Mth.sin(angleRad);
        return new Vec3(vec.x * cos - vec.z * sin, vec.y, vec.x * sin + vec.z * cos);
    }

    private static void doFocusedJudgementCut(LivingEntity owner, float range, float angle) {
        Level level = owner.level();
        ItemStack stack = owner.getMainHandItem();
        Vec3 look = owner.getLookAngle().normalize();
        List<Entity> founds = new ArrayList<>();

        for (Entity entity : level.getEntities(owner, owner.getBoundingBox().inflate(range))) {
            if (entity instanceof LivingEntity) {
                Vec3 toTarget = entity.position().subtract(owner.position()).normalize();
                float targetAngle = (float)Math.toDegrees(Math.acos(look.dot(toTarget)));
                if (targetAngle <= angle / 2.0F) {
                    founds.add(entity);
                }
            }
        }

        for (Entity entityx : founds) {
            ((LivingEntity)entityx).addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 8));
            EntityJudgementCut judgementCut = new EntityJudgementCut(RegistryEvents.JudgementCut, level);
            judgementCut.setPos(entityx.getX(), entityx.getY(), entityx.getZ());
            judgementCut.setOwner(owner);
            judgementCut.setSilent(true);
            stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> judgementCut.setColor(state.getColorCode()));
            owner.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).ifPresent(rank -> judgementCut.setRank(rank.getRankLevel(level.getGameTime())));
            level.addFreshEntity(judgementCut);
        }
    }
}
