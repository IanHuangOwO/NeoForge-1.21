package com.mamechin.enchantmod.cauldron;

import java.util.function.Supplier;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.block.ModBlocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnchantMod.MODID);

    public static final Supplier<BlockEntityType<CauldronBlockEntity>> SMALL_CAULDRON = BLOCK_ENTITIES.register("small_cauldron",
            () -> BlockEntityType.Builder.of((pos, state) -> new CauldronBlockEntity(ModBlockEntities.SMALL_CAULDRON.get(), CauldronSize.SMALL, pos, state),
                    ModBlocks.SMALL_CAULDRON.get()).build(null));

    public static final Supplier<BlockEntityType<CauldronBlockEntity>> LARGE_CAULDRON = BLOCK_ENTITIES.register("large_cauldron",
            () -> BlockEntityType.Builder.of((pos, state) -> new CauldronBlockEntity(ModBlockEntities.LARGE_CAULDRON.get(), CauldronSize.LARGE, pos, state),
                    ModBlocks.LARGE_CAULDRON.get()).build(null));

    private ModBlockEntities() {}
}
