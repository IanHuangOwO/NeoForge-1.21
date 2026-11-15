package com.mamechin.enchantmod.blocks;

import com.mamechin.enchantmod.altar.AltarBlockEntity;
import com.mamechin.enchantmod.press.PrintingPressBlockEntity;
import com.mamechin.enchantmod.shelf.TemplateShelfBlockEntity;
import com.mamechin.enchantmod.cauldron.CauldronBlockEntity;
import com.mamechin.enchantmod.cauldron.CauldronSize;
import java.util.function.Supplier;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.scribing.ScribingTableBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnchantMod.MODID);

    public static final Supplier<BlockEntityType<CauldronBlockEntity>> SMALL_CAULDRON = BLOCK_ENTITIES.register("small_cauldron",
            () -> BlockEntityType.Builder.of((pos, state) -> new CauldronBlockEntity(ModBlockEntities.SMALL_CAULDRON.get(), CauldronSize.SMALL, pos, state),
                    ModBlocks.SMALL_CAULDRON.get()).build(null));

    /** Lives on the left half of the scribing table only. */
    public static final Supplier<BlockEntityType<ScribingTableBlockEntity>> SCRIBING_TABLE = BLOCK_ENTITIES.register("scribing_table",
            () -> BlockEntityType.Builder.of(ScribingTableBlockEntity::new, ModBlocks.SCRIBING_TABLE.get()).build(null));

    public static final Supplier<BlockEntityType<AltarBlockEntity>> ALTAR = BLOCK_ENTITIES.register("altar",
            () -> BlockEntityType.Builder.of(AltarBlockEntity::new, ModBlocks.ALTAR.get()).build(null));

    public static final Supplier<BlockEntityType<TemplateShelfBlockEntity>> TEMPLATE_SHELF = BLOCK_ENTITIES.register("template_shelf",
            () -> BlockEntityType.Builder.of(TemplateShelfBlockEntity::new, ModBlocks.TEMPLATE_SHELF.get()).build(null));

    public static final Supplier<BlockEntityType<PrintingPressBlockEntity>> PRINTING_PRESS = BLOCK_ENTITIES.register("printing_press",
            () -> BlockEntityType.Builder.of(PrintingPressBlockEntity::new, ModBlocks.PRINTING_PRESS.get()).build(null));

    private ModBlockEntities() {}
}
