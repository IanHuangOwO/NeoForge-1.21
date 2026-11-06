package com.mamechin.enchantmod.cauldron;

import java.util.function.Supplier;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.block.ModBlocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnchantMod.MODID);

    public static final Supplier<BlockEntityType<CalciteCauldronBlockEntity>> CALCITE_CAULDRON = BLOCK_ENTITIES.register("calcite_cauldron",
            () -> BlockEntityType.Builder.of(CalciteCauldronBlockEntity::new, ModBlocks.CALCITE_CAULDRON.get()).build(null));

    private ModBlockEntities() {}
}
