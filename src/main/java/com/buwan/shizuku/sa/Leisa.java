package com.buwan.shizuku.sa;

import com.buwan.shizuku.util.DamageCalculator;
import mods.flammpfeil.slashblade.slasharts.SakuraEnd;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class Leisa {
    private static final int PURPLE_COLOR = 10040319;
    private static final ResourceLocation[] LEISA_SOUNDS = new ResourceLocation[]{
        new ResourceLocation("shizuku", "leisa_sound1"), new ResourceLocation("shizuku", "leisa_sound2"), new ResourceLocation("shizuku", "leisa_sound3")
    };

    public static void doLeisaSlash(LivingEntity player) {
        if (!player.level().isClientSide()) {
            playLeisaSound(player);
            float damage = DamageCalculator.calculateRefinedDamage(player, 1.0F);
            SakuraEnd.doSlash(player, 0.0F, 10040319, new Vec3(0.0, 0.2, 0.0), true, true, damage * 0.8F, KnockBacks.cancel);
            SakuraEnd.doSlash(player, -45.0F, 10040319, new Vec3(0.0, 0.1, 0.0), true, true, damage, KnockBacks.cancel);
            SakuraEnd.doSlash(player, 45.0F, 10040319, new Vec3(0.0, 0.1, 0.0), true, true, damage, KnockBacks.cancel);
        }
    }

    private static void playLeisaSound(LivingEntity player) {
        RandomSource random = player.getRandom();
        ResourceLocation soundLoc = LEISA_SOUNDS[random.nextInt(LEISA_SOUNDS.length)];
        SoundEvent sound = (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(soundLoc);
        if (sound != null) {
            player.level()
                .playSound(
                    null,
                    BlockPos.containing(player.getX(), player.getY(), player.getZ()),
                    sound,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F + (random.nextFloat() - 0.5F) * 0.2F
                );
        }
    }
}
