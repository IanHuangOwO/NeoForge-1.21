package com.mamechin.enchantmod.cauldron;

import com.mamechin.enchantmod.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Forms the large cauldron by itself when a player completes the pattern: placing smooth basalt or a small cauldron,
 * or breaking a block (which may empty a cell that has to be air). The check runs a tick later so the world already
 * holds the change. Only players trigger it; reverting the structure never does.
 */
public final class LargeCauldronAutoForm {
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        BlockState placed = event.getPlacedBlock();
        if (placed.is(Blocks.SMOOTH_BASALT) || placed.is(ModBlocks.SMALL_CAULDRON.get())) schedule(event.getLevel(), event.getPos());
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        schedule(event.getLevel(), event.getPos());
    }

    private static void schedule(Object level, BlockPos changed) {
        if (!(level instanceof ServerLevel server)) return;
        BlockPos pos = changed.immutable();
        server.getServer().tell(new TickTask(server.getServer().getTickCount() + 1, () -> tryForm(server, pos)));
    }

    /** Forms the first small cauldron next to the changed block whose surroundings now match the pattern. */
    private static void tryForm(Level level, BlockPos changed) {
        for (BlockPos core : BlockPos.betweenClosed(changed.offset(-1, -1, -1), changed.offset(1, 1, 1))) {
            if (!level.getBlockState(core).is(ModBlocks.SMALL_CAULDRON.get())) continue;
            if (level.getBlockEntity(core) instanceof CauldronBlockEntity c && !c.isBrewing()
                    && LargeCauldronStructure.firstMismatch(level, core) == null) {
                LargeCauldronStructure.form(level, core.immutable());
                return;
            }
        }
    }

    private LargeCauldronAutoForm() {}
}
