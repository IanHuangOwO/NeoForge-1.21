package com.mamechin.enchantmod.fluid;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * One placeable ink fluid: fluid type, source and flowing fluid, liquid block and bucket. A tier calls
 * {@link #register} once (see ModFluids); addons pass their own registers and namespace.
 */
public record InkFluid(String namespace, String name, int tint,
        Supplier<FluidType> type,
        DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
        DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing,
        DeferredBlock<LiquidBlock> block,
        DeferredItem<BucketItem> bucket) {

    /** Every ink fluid registered through {@link #register}, for client rendering setup. */
    public static final List<InkFluid> ALL = new ArrayList<>();

    /** @param tint ARGB colour applied to the vanilla water textures */
    public static InkFluid register(String namespace, String name, int tint,
            DeferredRegister<FluidType> types, DeferredRegister<Fluid> fluids,
            DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        var props = new BaseFlowingFluid.Properties[1]; // filled below, read lazily when the registries resolve
        var type = types.register(name, () -> new FluidType(FluidType.Properties.create()
                .descriptionId("block." + namespace + "." + name)));
        var source = fluids.register(name, () -> new BaseFlowingFluid.Source(props[0]));
        var flowing = fluids.register(name + "_flowing", () -> new BaseFlowingFluid.Flowing(props[0]));
        var block = blocks.registerBlock(name, p -> new LiquidBlock(source.get(), p), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));
        var bucket = items.registerItem(name + "_bucket", p -> new BucketItem(source.get(), p.craftRemainder(Items.BUCKET).stacksTo(1)));
        props[0] = new BaseFlowingFluid.Properties(type, source, flowing).block(block).bucket(bucket);

        InkFluid ink = new InkFluid(namespace, name, tint, type, source, flowing, block, bucket);
        ALL.add(ink);
        return ink;
    }
}
