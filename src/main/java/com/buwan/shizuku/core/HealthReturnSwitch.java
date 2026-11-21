package com.buwan.shizuku.core;

public class HealthReturnSwitch {
    private static volatile boolean enabled = false;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean status) {
        enabled = status;
    }

    public static Integer getIntValue() {
        return enabled ? 0 : null;
    }

    public static Float getFloatValue() {
        return enabled ? 0.0F : null;
    }

    public static Double getDoubleValue() {
        return enabled ? 0.0 : null;
    }
}
