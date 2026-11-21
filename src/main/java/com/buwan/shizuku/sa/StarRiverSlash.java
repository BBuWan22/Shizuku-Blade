package com.buwan.shizuku.sa;

import com.buwan.shizuku.shizuku;
import java.util.EnumSet;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.SlashBlade.RegistryEvents;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.capability.inputstate.CapabilityInputState;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.InputCommand;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class StarRiverSlash {
    private static final int STAR_COLOR = 9662683;
    private static final float PHANTOM_SPEED = 1.0F;
    private static final float PHANTOM_SIZE = 1.0F;
    private static final int PHANTOM_LIFETIME = 16;
    private static final float PHANTOM_DAMAGE_RATIO = 0.7F;
    private static final float[] PHANTOM_ROLLS = new float[]{0.0F, 90.0F, 60.0F, -60.0F};

    private StarRiverSlash() {
    }

    public static void firePhantomSlash(LivingEntity entity) {
        if (!entity.level().isClientSide()) {
            if (entity instanceof Player player) {
                ServerLevel level = (ServerLevel)entity.level();
                int colorCode = player.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).map(ISlashBladeState::getColorCode).orElse(9662683);
                float roll = randomPhantomRoll(player.getRandom());
                AttackManager.playPiercingSoundAction(player);
                spawnDrive(player, level, roll, 0.4F, colorCode, 0.7F, 1.0F, 16, 1.0F, KnockBacks.cancel);
            }
        }
    }

    public static float randomPhantomRoll(RandomSource rand) {
        return PHANTOM_ROLLS[rand.nextInt(PHANTOM_ROLLS.length)];
    }

    public static ResourceLocation standbyCommand(LivingEntity user) {
        EnumSet<InputCommand> commands = user.getCapability(CapabilityInputState.INPUT_STATE)
            .map(state -> state.getCommands(user))
            .orElseGet(() -> EnumSet.noneOf(InputCommand.class));
        if (commands.contains(InputCommand.R_CLICK)) {
            return shizuku.prefix("cosmic_attack");
        } else {
            return commands.contains(InputCommand.L_CLICK) ? SlashBlade.prefix("combo_a1") : SlashBlade.prefix("none");
        }
    }

    private static void spawnDrive(
        Player player, ServerLevel level, float roll, float up, int colorCode, float damage, float size, int lifetime, float speed, KnockBacks knockback
    ) {
        Vec3 dir = player.getLookAngle();
        Vec3 pos = player.position().add(0.0, player.getEyeHeight() * 0.75 + up, 0.0).add(player.getLookAngle().scale(0.3));
        EntityDrive drive = new EntityDrive(RegistryEvents.Drive, level);
        drive.setPos(pos.x, pos.y, pos.z);
        drive.setDamage(damage);
        drive.setSpeed(speed);
        drive.shoot(dir.x, dir.y, dir.z, speed, 0.0F);
        drive.setOwner(player);
        drive.setRotationRoll(roll);
        drive.setColor(colorCode);
        drive.setIsCritical(false);
        drive.setKnockBack(knockback);
        drive.setLifetime(lifetime);
        drive.setBaseSize(size);
        player.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).ifPresent(rank -> drive.setRank(rank.getRankLevel(level.getGameTime())));
        level.addFreshEntity(drive);
    }
}
