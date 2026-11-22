package com.buwan.shizuku.sa;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public final class JudgmentConfig {
    public static final ForgeConfigSpec SPEC;
    static final ForgeConfigSpec.IntValue RETENTION, PHASE_WINDOW, RADIUS;
    static final ForgeConfigSpec.ConfigValue<List<? extends String>> TYPES, NAMESPACES, KEYWORDS, ENCOUNTER_KEYS;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("unleashedJudgment");
        RETENTION = builder.comment("Server ticks to retain a target or encounter identity.")
            .defineInRange("observationTicks", 24000, 1, Integer.MAX_VALUE);
        PHASE_WINDOW = builder.comment("Ticks to associate a nearby boss without an encounter identity.")
            .defineInRange("phaseTicks", 200, 0, Integer.MAX_VALUE);
        RADIUS = builder.defineInRange("phaseRadius", 24, 1, 320);
        TYPES = builder.defineListAllowEmpty("bossTypes", List.of(), v -> v instanceof String s && !s.isBlank());
        NAMESPACES = builder.defineListAllowEmpty("bossNamespaces", List.of(), v -> v instanceof String s && !s.isBlank());
        KEYWORDS = builder.defineListAllowEmpty("bossNameKeywords", List.of("boss"), v -> v instanceof String s && !s.isBlank());
        ENCOUNTER_KEYS = builder.comment("Additional UUID fields on entity ForgeData or summoner item NBT identifying a shared boss encounter.")
            .defineListAllowEmpty("encounterUuidKeys", List.of(), v -> v instanceof String s && !s.isBlank());
        builder.pop();
        SPEC = builder.build();
    }

    private JudgmentConfig() {}
}
