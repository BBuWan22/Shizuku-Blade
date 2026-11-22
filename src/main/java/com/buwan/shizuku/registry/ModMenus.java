package com.buwan.shizuku.registry;

import com.buwan.shizuku.client.screen.ShizukuCraftScreen;
import com.buwan.shizuku.menu.ShizukuCraftMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 菜单注册表：工作台 MenuType，并注册客户端界面工厂（onClientSetup）。
 */
public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "shizuku");
    public static final RegistryObject<MenuType<ShizukuCraftMenu>> SHIZUKU_CRAFT_TABLE = MENUS.register(
        "shizuku_craft_table", () -> IForgeMenuType.create(ShizukuCraftMenu::open)
    );

    @OnlyIn(Dist.CLIENT)
    public static void onClientSetup() {
        MenuScreens.register((MenuType)SHIZUKU_CRAFT_TABLE.get(), ShizukuCraftScreen::new);
    }
}
