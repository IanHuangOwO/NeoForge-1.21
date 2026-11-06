package com.mamechin.enchantmod.block;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.item.inkwell.EnchantInkwellItem;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Cauldron whose exact contents live in a block entity; LEVEL (0-3) mirrors it for models. */
public class CalciteCauldronBlock extends AbstractCauldronBlock implements EntityBlock {
    public static final MapCodec<CalciteCauldronBlock> CODEC = simpleCodec(CalciteCauldronBlock::new);
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 3);
    public static final EnumProperty<CauldronContent> CONTENT = EnumProperty.create("content", CauldronContent.class);
    public static final int BOTTLE_ML = 250;
    public static final int INK_ML_PER_USE = 10;
    public static final TagKey<Block> HEAT_SOURCES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "cauldron_heat_sources"));

    // Required by the vanilla base class, but unused: useItemOn is overridden below.
    private static final CauldronInteraction.InteractionMap NO_INTERACTIONS = CauldronInteraction.newInteractionMap("calcite");

    public CalciteCauldronBlock(Properties properties) {
        super(properties, NO_INTERACTIONS);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0).setValue(CONTENT, CauldronContent.WATER));
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, CONTENT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CalciteCauldronBlockEntity(pos, state);
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.getValue(LEVEL) == 3;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        int lvl = state.getValue(LEVEL);
        return lvl == 0 ? 0.0 : (6.0 + lvl * 3.0) / 16.0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(LEVEL);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CalciteCauldronBlockEntity cauldron)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int ml = cauldron.getAmountMl();
        boolean water = cauldron.getContent() == CauldronContent.WATER;
        int newMl;
        ItemStack returned;
        SoundEvent sound;

        if (stack.is(Items.WATER_BUCKET) && ml == 0) {
            newMl = CalciteCauldronBlockEntity.CAPACITY_ML;
            returned = new ItemStack(Items.BUCKET);
            sound = SoundEvents.BUCKET_EMPTY;
        } else if (stack.is(Items.BUCKET) && water && ml == CalciteCauldronBlockEntity.CAPACITY_ML) {
            newMl = 0;
            returned = new ItemStack(Items.WATER_BUCKET);
            sound = SoundEvents.BUCKET_FILL;
        } else if (stack.is(Items.POTION) && isWater(stack) && water && ml + BOTTLE_ML <= CalciteCauldronBlockEntity.CAPACITY_ML) {
            newMl = ml + BOTTLE_ML;
            returned = new ItemStack(Items.GLASS_BOTTLE);
            sound = SoundEvents.BOTTLE_EMPTY;
        } else if (stack.is(Items.GLASS_BOTTLE) && cauldron.getContent() == CauldronContent.INK && ml >= INK_ML_PER_USE) {
            // empty bottle -> inkwell of the cauldron's tier, filled with as many uses as the ink allows
            Item inkwell = BuiltInRegistries.ITEM.get(cauldron.getInkwellId());
            returned = new ItemStack(inkwell);
            int uses = Math.min(returned.getMaxDamage(), ml / INK_ML_PER_USE);
            returned.setDamageValue(returned.getMaxDamage() - uses);
            newMl = ml - uses * INK_ML_PER_USE;
            sound = SoundEvents.BOTTLE_FILL;
        } else if (stack.getItem() instanceof EnchantInkwellItem && stack.isDamaged()
                && cauldron.getContent() == CauldronContent.INK && ml >= INK_ML_PER_USE
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(cauldron.getInkwellId())) {
            // matching inkwell -> refill durability from the cauldron
            int uses = Math.min(stack.getDamageValue(), ml / INK_ML_PER_USE);
            returned = stack.copy();
            returned.setDamageValue(stack.getDamageValue() - uses);
            newMl = ml - uses * INK_ML_PER_USE;
            sound = SoundEvents.BOTTLE_FILL;
        } else {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            // inkwells stack to 1, so replacing the held stack is exact
            player.setItemInHand(hand, stack.getMaxStackSize() == 1 ? returned : ItemUtils.createFilledResult(stack, player, returned));
            cauldron.setAmountMl(newMl);
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Empty hand: shift-click takes out the first stored stack; plain click starts the brew when there is
     * water, a full ingredient set and a heat source (ingredients are consumed when the brew finishes).
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CalciteCauldronBlockEntity cauldron)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            for (int i = 0; i < cauldron.inventory.getSlots(); i++) {
                ItemStack stored = cauldron.inventory.getStackInSlot(i);
                if (!stored.isEmpty()) {
                    if (!level.isClientSide) {
                        player.getInventory().placeItemBackInInventory(cauldron.inventory.extractItem(i, stored.getCount(), false));
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            return InteractionResult.PASS;
        }
        if (cauldron.getContent() != CauldronContent.WATER || cauldron.getAmountMl() == 0) {
            return InteractionResult.PASS;
        }
        InkBrewing.Recipe recipe = cauldron.matchedRecipe();
        if (recipe == null) {
            return InteractionResult.PASS;
        }
        if (!isHeated(level, pos)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.enchantmod.cauldron.no_heat"), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            cauldron.startBrewing(BuiltInRegistries.ITEM.getKey(recipe.inkwell().get()));
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static boolean isHeated(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(HEAT_SOURCES)
                && (!below.hasProperty(BlockStateProperties.LIT) || below.getValue(BlockStateProperties.LIT));
    }

    /** Thrown items fall in and are stored like a hopper would, if the slot accepts them. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // cheapest checks first: side, item type, then the cached "has room" flag
        if (level.isClientSide || !(entity instanceof ItemEntity item) || !InkBrewing.isIngredient(item.getItem())
                || !(level.getBlockEntity(pos) instanceof CalciteCauldronBlockEntity cauldron) || !cauldron.hasRoom()) {
            return;
        }
        ItemStack remainder = ItemHandlerHelper.insertItem(cauldron.inventory, item.getItem(), false);
        if (remainder.isEmpty()) {
            item.discard();
        } else {
            item.setItem(remainder);
        }
    }

    /** Breaking spills the water as a source block; ink has no fluid block yet, so it is lost. Items drop in onRemove. */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        boolean hasWater = level.getBlockEntity(pos) instanceof CalciteCauldronBlockEntity c
                && c.getContent() == CauldronContent.WATER && c.getAmountMl() > 0;
        BlockState replacement = hasWater ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        return level.setBlock(pos, replacement, level.isClientSide ? 11 : 3);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CalciteCauldronBlockEntity cauldron) {
            IItemHandler inv = cauldron.inventory;
            for (int i = 0; i < inv.getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.CALCITE_CAULDRON.get()) return null;
        return (l, p, s, be) -> ((CalciteCauldronBlockEntity) be).serverTick();
    }

    private static boolean isWater(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(Potions.WATER);
    }
}
