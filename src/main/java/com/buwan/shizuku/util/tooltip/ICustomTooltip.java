package com.buwan.shizuku.util.tooltip;

/**
 * 自定义 tooltip 的接口与枚举：实现它的物品可以拿到自己的渐变配置与立绘。
 */
public interface ICustomTooltip {
    default ICustomTooltip.TooltipType getTooltipType() {
        return ICustomTooltip.TooltipType.DEFAULT;
    }

    public static enum TooltipType {
        DEFAULT,
        LEGENDARY,
        MYSTIC,
        TORNADO,
        VOID,
        TIME,
        TELEPORT,
        SUMMER,
        COSMIC;
    }
}
