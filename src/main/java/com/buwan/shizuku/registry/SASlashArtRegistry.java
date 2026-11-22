package com.buwan.shizuku.registry;

import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 特殊技（SlashArts）注册：雫草、雷萨、影萨、传送斩、时停等，
 * 每个都绑定到对应的 SA 实现类。
 */
public class SASlashArtRegistry {
    public static final DeferredRegister<SlashArts> SLASH_ARTS = DeferredRegister.create(SlashArts.REGISTRY_KEY, "shizuku");
    public static final RegistryObject<SlashArts> Shizukusa = SLASH_ARTS.register("shizukusa", () -> new SlashArts(e -> SAComboRegistry.shizukusa.getId()));
    public static final RegistryObject<SlashArts> Leisa = SLASH_ARTS.register("leisa", () -> new SlashArts(e -> SAComboRegistry.LEISA.getId()));
    public static final RegistryObject<SlashArts> Yingsa = SLASH_ARTS.register("yingsa", () -> new SlashArts(e -> SAComboRegistry.YINGSA.getId()));
    public static final RegistryObject<SlashArts> TeleportSlash = SLASH_ARTS.register(
        "teleport_slash", () -> new SlashArts(e -> SAComboRegistry.TELEPORT_SLASH.getId())
    );
    public static final RegistryObject<SlashArts> TimeSlash = SLASH_ARTS.register("time_slash", () -> new SlashArts(e -> SAComboRegistry.TIME_SLASH.getId()));
    public static final RegistryObject<SlashArts> TornadoSlash = SLASH_ARTS.register(
        "tornado_slash", () -> new SlashArts(e -> SAComboRegistry.TORNADO_SLASH.getId())
    );
    public static final RegistryObject<SlashArts> UnlimitedBladeWorks = SLASH_ARTS.register(
        "unlimited_blade_works", () -> new SlashArts(e -> SAComboRegistry.UNLIMITED_BLADE_WORKS.getId())
    );
    public static final RegistryObject<SlashArts> MoonlightSlash = SLASH_ARTS.register(
        "moonlight_slash", () -> new SlashArts(e -> SAComboRegistry.MOONLIGHT_SLASH.getId())
    );
    public static final RegistryObject<SlashArts> TsubameGaeshi = SLASH_ARTS.register(
        "tsubame_gaeshi", () -> new SlashArts(e -> SAComboRegistry.TSUBAME_GAESHI.getId())
    );
    public static final RegistryObject<SlashArts> VoidPrison = SLASH_ARTS.register("void_prison", () -> new SlashArts(e -> SAComboRegistry.VOID_PRISON.getId()));
    public static final RegistryObject<SlashArts> CosmicCrossSlash = SLASH_ARTS.register(
        "cosmic_cross_slash", () -> new SlashArts(e -> SAComboRegistry.COSMIC_CROSS_SLASH.getId())
    );
    public static final RegistryObject<SlashArts> Ymsa = SLASH_ARTS.register("ymsa", () -> new SlashArts(e -> SAComboRegistry.YMSA.getId()));
    public static final RegistryObject<SlashArts> SummerSlash = SLASH_ARTS.register(
        "summer_slash", () -> new SlashArts(e -> SAComboRegistry.SUMMER_SLASH.getId())
    );
    public static final RegistryObject<SlashArts> GalaxyArc = SLASH_ARTS.register(
        "galaxy_arc", () -> new SlashArts(e -> SAComboRegistry.GALAXY_ARC.getId()).setProudSoulCost(0)
    );
}
