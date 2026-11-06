package com.mamechin.enchantmod.fluid;

import com.mamechin.enchantmod.cauldron.ModBlockEntities;
import com.mamechin.enchantmod.item.ModItems;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStackSimple;

public final class ModCapabilities {
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.CALCITE_CAULDRON.get(), (cauldron, side) -> cauldron.fluidHandler);
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new InkBucketHandler(stack), ModItems.INK_BUCKET.get());
    }

    /** Holds exactly one bucket (1000 mB) of enchanting ink and turns back into an empty bucket when drained. */
    private static class InkBucketHandler extends FluidHandlerItemStackSimple.SwapEmpty {
        InkBucketHandler(ItemStack stack) {
            super(ModDataComponents.FLUID_CONTENT, stack, new ItemStack(Items.BUCKET), 1000);
        }

        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return fluid.is(ModFluids.ENCHANTING_INK.get());
        }
    }

    private ModCapabilities() {}
}
