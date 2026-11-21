package com.buwan.shizuku.util.tooltip;

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
