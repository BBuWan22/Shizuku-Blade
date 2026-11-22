package com.buwan.shizuku.client.tooltip;

import com.buwan.shizuku.client.JudgmentControls;
import com.buwan.shizuku.util.tooltip.ICustomTooltip;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.client.gui.Font;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag.Default;
import net.minecraftforge.client.event.RenderTooltipEvent.Pre;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Tooltip 事件入口：订阅 RenderTooltipEvent 的 Pre，判断悬停物品是不是本 mod 的刀，
 * 是则取消原版绘制、改调 ShizukuTooltipRenderer。
 *
 * 已知问题（Fabric 版已修，Forge 侧无需处理）：Fabric 的 JEI 最终调用的是「只有文本行」
 * 的 renderTooltip 重载，ItemStack 已丢失，只能注入 JEI 内部类才能拿到；Forge 的补丁
 * 给该重载补了 Optional<ItemStack>，所以这边直接可用。
 */
public class TooltipEventHandler {
    @SubscribeEvent
    public static void onTooltipRender(Pre event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof ICustomTooltip customTooltipItem) {
            renderCustomTooltip(event, stack, customTooltipItem.getTooltipType());
        } else {
            if (isShizukuNamedBlade(stack)) {
                ICustomTooltip.TooltipType tooltipType = getTooltipTypeForBlade(stack);
                renderCustomTooltip(event, stack, tooltipType);
            }
        }
    }

    private static void renderCustomTooltip(Pre event, ItemStack stack, ICustomTooltip.TooltipType tooltipType) {
        event.setCanceled(true);
        List<Component> textLines = generateTooltipText(stack, event.getFont(), tooltipType);
        ShizukuTooltipRenderer.renderCustomTooltip(event.getGraphics(), textLines, event.getX(), event.getY(), event.getFont(), tooltipType);
    }

    private static boolean isShizukuNamedBlade(ItemStack stack) {
        if (!stack.getItem().toString().contains("slashblade")) {
            return false;
        } else {
            String stateKey = stack.getCapability(ItemSlashBlade.BLADESTATE).map(ISlashBladeState::getTranslationKey).orElse("");
            if ("item.shizuku.ymblade".equals(stateKey) || "shizuku:ymblade".equals(getBladeName(stack))) return true;
            if (!"item.shizuku.mosmicblade".equals(stateKey) && !"shizuku:mosmicblade".equals(getBladeName(stack))) {
                String displayName = stack.getHoverName().getString();
                if (displayName.contains("\u65e0\u94ed") && displayName.contains("\u6709\u540d")) {
                    return true;
                } else {
                    CompoundTag tag = stack.getTag();
                    if (tag == null) {
                        return false;
                    } else {
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

                        return false;
                    }
                }
            } else {
                return true;
            }
        }
    }

    static Component judgmentKeyHint() {
        return Component.translatable("tooltip.shizuku.judgment.open", JudgmentControls.OPEN.getTranslatedKeyMessage());
    }

    private static boolean isJudgmentBlade(ItemStack stack) {
        return stack.getItem() instanceof ItemSlashBlade && getTooltipTypeForBlade(stack) == ICustomTooltip.TooltipType.DEFAULT
            && isShizukuNamedBlade(stack);
    }

    private static ICustomTooltip.TooltipType getTooltipTypeForBlade(ItemStack stack) {
        String stateKey = stack.getCapability(ItemSlashBlade.BLADESTATE).map(ISlashBladeState::getTranslationKey).orElse("");
        if ("item.shizuku.mosmicblade".equals(stateKey)) {
            return ICustomTooltip.TooltipType.COSMIC;
        } else {
            String bladeName = getBladeName(stack);
            if (bladeName == null) {
                return ICustomTooltip.TooltipType.DEFAULT;
            } else {
                return switch (bladeName) {
                    case "shizuku:tornado_blade" -> ICustomTooltip.TooltipType.TORNADO;
                    case "shizuku:void_blade" -> ICustomTooltip.TooltipType.VOID;
                    case "shizuku:time_blade" -> ICustomTooltip.TooltipType.TIME;
                    case "shizuku:teleport_blade" -> ICustomTooltip.TooltipType.TELEPORT;
                    case "shizuku:unlimited_blade" -> ICustomTooltip.TooltipType.LEGENDARY;
                    case "shizuku:moonlight_blade" -> ICustomTooltip.TooltipType.MYSTIC;
                    case "shizuku:ganjo_natsuki_blade" -> ICustomTooltip.TooltipType.SUMMER;
                    case "shizuku:mosmicblade" -> ICustomTooltip.TooltipType.COSMIC;
                    default -> ICustomTooltip.TooltipType.DEFAULT;
                };
            }
        }
    }

    private static int getAttackDamage(ItemStack stack) {
        double dmg = 1.0;
        Multimap<Attribute, AttributeModifier> attrs = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);

        for (AttributeModifier mod : attrs.get(Attributes.ATTACK_DAMAGE)) {
            if (mod.getOperation() == Operation.ADDITION) {
                dmg += mod.getAmount();
            }
        }

        return (int)dmg;
    }

    private static String getBladeName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return null;
        } else {
            CompoundTag shareTag = tag.getCompound("ShareTag");
            if (!shareTag.isEmpty()) {
                String translationKey = shareTag.getString("translationKey");
                if (translationKey.startsWith("item.shizuku.")) {
                    return translationKey.substring("item.".length()).replaceFirst("\\.", ":");
                }
            }

            CompoundTag forgeCaps = tag.getCompound("ForgeCaps");
            if (forgeCaps.contains("slashblade:bladestate")) {
                CompoundTag bladeState = forgeCaps.getCompound("slashblade:bladestate");
                String translationKey = bladeState.getString("translationKey");
                if (translationKey.startsWith("item.shizuku.")) {
                    return translationKey.substring("item.".length()).replaceFirst("\\.", ":");
                }
            }

            return null;
        }
    }

    private static List<Component> generateTooltipText(ItemStack stack, Font font, ICustomTooltip.TooltipType tooltipType) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(stack.getHoverName());
        List<Component> itemTooltip = new ArrayList<>();
        stack.getItem().appendHoverText(stack, null, itemTooltip, Default.NORMAL);
        tooltip.addAll(itemTooltip);
        tooltip.add(Component.empty());
        if (tooltipType == ICustomTooltip.TooltipType.COSMIC) {
            tooltip.add(Component.literal("Shift + X \u5f00\u542f\u661f\u6cb3\u9886\u57df"));
            tooltip.add(Component.literal("\u957f\u6309\u53f3\u952e\u91ca\u653e\u661f\u6cb3\u65a9"));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("\u661f\u6cb3\u843d\u4e5d\u5929\uff0c\u5203\u5149\u6620\u4e07\u53e4"));
            tooltip.add(Component.literal("\u4e00\u5251\u65a9\u661f\u8fb0\uff0c\u4e07\u8c61\u7686\u4fef\u9996"));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("\u653b\u51fb\u4f24\u5bb3: \u8d8a\u6253\u8d8a\u9ad8"));
        } else {
            tooltip.add(Component.literal("\u950b\u5229:\u4e0d\u53ef\u4f30\u91cf"));
            tooltip.add(Component.literal("\u529b\u91cf:\u4e0d\u53ef\u4f30\u91cf"));
            tooltip.add(Component.literal("\u53ef\u7231:\u4e0d\u53ef\u4f30\u91cf"));
            tooltip.add(Component.literal("\u9b45\u529b:\u4e0d\u53ef\u4f30\u91cf"));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("shift + sa\u91ca\u653e\u540e\u53ef\u51fb\u6740\u602a\u7269\u5e76\u6e05\u9664\u6389\u843d\u7269"));
            if (isJudgmentBlade(stack)) {
                tooltip.add(judgmentKeyHint());
            }
            tooltip.add(Component.literal("shift + alt \u53ef\u5f00\u542f\u5750\u6807\u4f4d\u79fb"));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("\u76f8\u89c1\u65f6\u96be\u522b\u4ea6\u96be\uff0c\u4e1c\u98ce\u65e0\u529b\u767e\u82b1\u6b8b"));
            tooltip.add(Component.literal("\u6625\u8695\u5230\u6b7b\u4e1d\u65b9\u5c3d\uff0c\u8721\u70ac\u6210\u7070\u6cea\u59cb\u5e72"));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("  \u732b\u7fbd\u96eb \u653b\u51fb\u4f24\u5bb3"));
        }

        tooltip.add(Component.empty());
        return tooltip;
    }
}
