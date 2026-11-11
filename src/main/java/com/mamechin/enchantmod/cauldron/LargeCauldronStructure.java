package com.mamechin.enchantmod.cauldron;

import java.util.List;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.block.ModBlocks;
import com.mamechin.enchantmod.cauldron.heat.CauldronHeat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The large cauldron: a smooth basalt body (two layers) on four smooth basalt legs around a small cauldron, open on top, heated from below. Forming turns the core into the
 * large cauldron (the controller, same position) and the 20 smooth basalt blocks into parts; removing any part reverts all of it.
 */
public final class LargeCauldronStructure {
    /** Items that form the structure when used on the core. */
    public static final TagKey<Item> FORMING_ITEMS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "quills"));

    /**
     * Layers bottom to top, rows north to south, columns west to east. B smooth basalt (leg or body), C the core cauldron,
     * A must be air (between the legs), H the heat source: any block, never converted, free to change afterwards.
     */
    private static final String[] LAYERS = {
            "BAB" + "AHA" + "BAB",
            "BBB" + "BCB" + "BBB",
            "BBB" + "BAB" + "BBB"};

    private static char at(int x, int y, int z) {
        return LAYERS[y].charAt(z * 3 + x);
    }

    /** The first position that breaks the pattern, or null when the structure around the core is complete. */
    public static BlockPos firstMismatch(Level level, BlockPos core) {
        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    char want = at(x, y, z);
                    if (want == 'C' || want == 'H') continue;
                    BlockPos pos = core.offset(x - 1, y - 1, z - 1);
                    BlockState state = level.getBlockState(pos);
                    if (want == 'B' ? !state.is(Blocks.SMOOTH_BASALT) : !state.isAir()) return pos;
                }
            }
        }
        return null;
    }

    /** Small cauldron to large cauldron. Everything stored moves over; a running brew is not allowed (checked by the caller). */
    public static void form(Level level, BlockPos core) {
        CauldronBlockEntity old = (CauldronBlockEntity) level.getBlockEntity(core);
        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    if (at(x, y, z) != 'B') continue;
                    level.setBlock(core.offset(x - 1, y - 1, z - 1), ModBlocks.LARGE_CAULDRON_PART.get().defaultBlockState()
                            .setValue(LargeCauldronPartBlock.DX, x).setValue(LargeCauldronPartBlock.DY, y).setValue(LargeCauldronPartBlock.DZ, z), 3);
                }
            }
        }
        level.setBlock(core, ModBlocks.LARGE_CAULDRON.get().defaultBlockState()
                .setValue(SmallCauldronBlock.HEATED, CauldronHeat.tier(level.getBlockState(core.below())) > 0), 3);
        moveContents(level, core, old);
    }

    /** Large cauldron back to a small one with smooth basalt around it again. Fluid above the small capacity is lost, items that do not fit drop. */
    public static void dismantle(Level level, BlockPos core) {
        CauldronBlockEntity old = (CauldronBlockEntity) level.getBlockEntity(core);
        level.setBlock(core, ModBlocks.SMALL_CAULDRON.get().defaultBlockState(), 3);
        moveContents(level, core, old);
        revert(level, core);
    }

    /** Every remaining part becomes plain smooth basalt. Parts react to this only while a large cauldron is still at the core. */
    public static void revert(Level level, BlockPos core) {
        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    BlockPos pos = core.offset(x - 1, y - 1, z - 1);
                    if (at(x, y, z) == 'B' && level.getBlockState(pos).getBlock() instanceof LargeCauldronPartBlock) {
                        level.setBlock(pos, Blocks.SMOOTH_BASALT.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void moveContents(Level level, BlockPos core, CauldronBlockEntity from) {
        if (from == null || !(level.getBlockEntity(core) instanceof CauldronBlockEntity to)) return;
        List<ItemStack> overflow = from.moveTo(to);
        for (ItemStack stack : overflow) {
            Containers.dropItemStack(level, core.getX() + 0.5, core.getY() + 1, core.getZ() + 0.5, stack);
        }
    }

    private LargeCauldronStructure() {}
}
