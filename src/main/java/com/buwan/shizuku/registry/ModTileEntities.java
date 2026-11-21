package com.buwan.shizuku.registry;

import com.buwan.shizuku.tile.ShizukuCraftTile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块实体注册：工作台 BlockEntity，并绑定到工作台方块。
 */
public class ModTileEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "shizuku");
    public static final RegistryObject<BlockEntityType<ShizukuCraftTile>> SHIZUKU_CRAFT_TILE = BLOCK_ENTITIES.register(
        "shizuku_craft_tile", () -> Builder.of(ShizukuCraftTile::new, (Block)ModBlocks.SHIZUKU_CRAFTING_TABLE.get()).build(null)
    );
}
