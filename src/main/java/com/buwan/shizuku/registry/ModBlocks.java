package com.buwan.shizuku.registry;

import com.buwan.shizuku.block.ShizukuCraftTableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "shizuku");
    public static final RegistryObject<Block> SHIZUKU_CRAFTING_TABLE = BLOCKS.register(
        "shizuku_crafting_table",
        () -> new ShizukuCraftTableBlock(Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).sound(SoundType.STONE).strength(2.5F).lightLevel(state -> 15))
    );
}
