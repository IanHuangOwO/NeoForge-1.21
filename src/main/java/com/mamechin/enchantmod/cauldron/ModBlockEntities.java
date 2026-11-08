package com.mamechin.enchantmod.cauldron;

import java.util.function.Supplier;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.block.ModBlocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnchantMod.MODID);

    public static final Supplier<BlockEntityType<CauldronTier0BlockEntity>> CAULDRON_TIER0 = BLOCK_ENTITIES.register("cauldron_tier0",
            () -> BlockEntityType.Builder.of(CauldronTier0BlockEntity::new, ModBlocks.CAULDRON_TIER0.get()).build(null));

    private ModBlockEntities() {}
}
