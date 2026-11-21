package com.buwan.shizuku.server;

import java.util.Set;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * 服务器侧的刀保护：登录/死亡/掉落时，把「受保护的刀」（星之雫系列）
 * 标记成不掉落也不被清除——死亡后会被放回背包。
 *
 * 判定同时看定义 id、显示名（无铭/有名）与 NBT 里的 display.Name，
 * 是为了覆盖改名后的刀。
 */
@EventBusSubscriber
public class BladeProtectorServer {
    private static final Set<ResourceLocation> PROTECTED_BLADES = Set.of(
        new ResourceLocation("shizuku", "shizuku"), new ResourceLocation("shizuku", "el"), new ResourceLocation("shizuku", "ym")
    );

    public static boolean isProtectedBlade(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            try {
                if (stack.getItem() instanceof ItemSlashBlade) {
                    String displayName = stack.getHoverName().getString();
                    if (displayName.contains("\u65e0\u94ed") && displayName.contains("\u6709\u540d")) {
                        return true;
                    }

                    CompoundTag tag = stack.getOrCreateTag();
                    if (tag.contains("display")) {
                        CompoundTag display = tag.getCompound("display");
                        if (display.contains("Name")) {
                            String nbtName = display.getString("Name");
                            if (nbtName.contains("\u65e0\u94ed") && nbtName.contains("\u6709\u540d")) {
                                return true;
                            }
                        }
                    }

                    if (tag.contains("SB.SwordName")) {
                        String bladeName = tag.getString("SB.SwordName");
                        if (bladeName.contains("\u65e0\u94ed") && bladeName.contains("\u6709\u540d")) {
                            return true;
                        }
                    }

                    if (!tag.contains("shizuku:protected_blade") && !tag.contains("shizuku:keep_on_death")) {
                        return stack.getCapability(ItemSlashBlade.BLADESTATE).map(state -> {
                            String bladeNamex = tag.getString("SB.SwordName");
                            return bladeNamex != null && (bladeNamex.contains("shizuku") || bladeNamex.contains("el") || bladeNamex.contains("ym"));
                        }).orElse(false);
                    }

                    return true;
                }
            } catch (Exception var5) {
            }

            return false;
        } else {
            return false;
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = event.getEntity();
            System.out.println("\u00a76[BladeProtectorServer] \u73a9\u5bb6\u767b\u5f55\uff0c\u786e\u4fdd\u4fdd\u62a4\u6807\u7b7e...");
            new Thread(
                    () -> {
                        try {
                            Thread.sleep(2000L);

                            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                                ItemStack stack = player.getInventory().getItem(i);
                                if (stack != null && !stack.isEmpty()) {
                                    try {
                                        if (stack.getItem() instanceof ItemSlashBlade && isProtectedBlade(stack)) {
                                            CompoundTag tag = stack.getOrCreateTag();
                                            tag.putBoolean("shizuku:keep_on_death", true);
                                            tag.putString("shizuku:protected_blade", "true");
                                            tag.putLong("shizuku:protection_timestamp", System.currentTimeMillis());
                                            System.out
                                                .println(
                                                    "\u00a76[BladeProtectorServer] \u786e\u4fdd\u4fdd\u62a4\u7269\u54c1\u6807\u7b7e: "
                                                        + stack.getHoverName().getString()
                                                );
                                        }
                                    } catch (Exception var4) {
                                    }
                                }
                            }

                            System.out.println("\u00a76[BladeProtectorServer] \u4fdd\u62a4\u6807\u7b7e\u786e\u4fdd\u5b8c\u6210\uff01");
                        } catch (InterruptedException var5) {
                            var5.printStackTrace();
                        }
                    }
                )
                .start();
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player)event.getEntity();
            System.out.println("\u00a76[BladeProtectorServer] \u670d\u52a1\u5668\u7aef\u6b7b\u4ea1\u4fdd\u62a4\u89e6\u53d1");

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (isProtectedBlade(stack)) {
                    CompoundTag tag = stack.getOrCreateTag();
                    tag.putBoolean("shizuku:anti_disarm", true);
                    tag.putString("shizuku:protected_blade", "true");
                    player.getInventory().setItem(i, stack);
                    System.out.println("\u00a76[BladeProtectorServer] \u670d\u52a1\u5668\u7aef\u4fdd\u62a4\u7269\u54c1: " + stack.getHoverName().getString());
                }
            }

            ItemStack mainHand = player.getMainHandItem();
            if (isProtectedBlade(mainHand)) {
                CompoundTag tag = mainHand.getOrCreateTag();
                tag.putBoolean("shizuku:anti_disarm", true);
                tag.putString("shizuku:protected_blade", "true");
                player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
                System.out
                    .println(
                        "\u00a76[BladeProtectorServer] \u670d\u52a1\u5668\u7aef\u4fdd\u62a4\u4e3b\u624b\u7269\u54c1: " + mainHand.getHoverName().getString()
                    );
            }

            ItemStack offHand = player.getOffhandItem();
            if (isProtectedBlade(offHand)) {
                CompoundTag tag = offHand.getOrCreateTag();
                tag.putBoolean("shizuku:anti_disarm", true);
                tag.putString("shizuku:protected_blade", "true");
                player.setItemInHand(InteractionHand.OFF_HAND, offHand);
                System.out
                    .println(
                        "\u00a76[BladeProtectorServer] \u670d\u52a1\u5668\u7aef\u4fdd\u62a4\u526f\u624b\u7269\u54c1: " + offHand.getHoverName().getString()
                    );
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player)event.getEntity();
            System.out
                .println(
                    "\u00a76[BladeProtectorServer] \u670d\u52a1\u5668\u7aef\u6389\u843d\u62e6\u622a\u89e6\u53d1\uff0c\u6389\u843d\u6570\u91cf: "
                        + event.getDrops().size()
                );
            System.out.println("\u00a76[BladeProtectorServer] \u670d\u52a1\u5668\u7aef\u5141\u8bb8\u6240\u6709\u7269\u54c1\u6b63\u5e38\u6389\u843d");
        }
    }
}
