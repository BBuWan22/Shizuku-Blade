package com.buwan.shizuku.registry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "shizuku");
    public static final RegistryObject<Item> MEOW_STONE = ITEMS.register("meow_stone", () -> new Item(new Properties()));

    public static ItemStack createProtectedBlade(ItemStack originalStack) {
        if (originalStack != null && !originalStack.isEmpty()) {
            String displayName = originalStack.getHoverName().getString();
            if (displayName.contains("\u65e0\u94ed") && displayName.contains("\u6709\u540d")) {
                ItemStack protectedStack = originalStack.copy();
                CompoundTag tag = protectedStack.getOrCreateTag();
                tag.putBoolean("shizuku:anti_disarm", true);
                tag.putBoolean("shizuku:keep_on_death", true);
                if (!tag.contains("display")) {
                    tag.put("display", new CompoundTag());
                }

                CompoundTag display = tag.getCompound("display");
                display.putString("Name", "{\"text\":\"" + displayName + "\",\"italic\":false,\"color\":\"gold\"}");
                return protectedStack;
            } else {
                return originalStack;
            }
        } else {
            return originalStack;
        }
    }
}
