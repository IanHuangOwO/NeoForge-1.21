package com.mamechin.enchantmod.allay;

import java.util.List;
import java.util.UUID;

import com.mamechin.enchantmod.altar.AltarBlockEntity;
import com.mamechin.enchantmod.balance.Balance;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A glass capsule that holds one allay (author, 2026-11-23). Right-click an allay to catch it ({@link AllayBinding#onInteract}); use it
 * on a block to let the allay out there, free; shift-use it on an altar to let it out bound to that altar, where it carries dropped
 * books to the altar's shelves ({@link AllayBinding}). The allay is kept whole (its saved data) in {@code CUSTOM_DATA}.
 */
public class AllayCapsule extends Item {
    private static final String KEY = "allay";

    public AllayCapsule(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean full(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(KEY);
    }

    /** Puts the allay in the capsule and removes it from the world. */
    static void capture(ItemStack capsule, Allay allay) {
        CompoundTag tag = new CompoundTag();
        allay.saveWithoutId(tag);
        CustomData.update(DataComponents.CUSTOM_DATA, capsule, t -> t.put(KEY, tag));
        allay.discard();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!full(stack)) return InteractionResult.PASS;
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos clicked = context.getClickedPos();
        Player player = context.getPlayer();
        // shift-use reaches here even on the altar, because a sneaking player with an item skips the block's own use
        boolean bind = context.isSecondaryUseActive() && level.getBlockEntity(clicked) instanceof AltarBlockEntity;
        if (bind && !roomFor(level, clicked)) {
            if (player != null) player.displayClientMessage(Component.translatable("message.enchantmod.allay.full"), true);
            return InteractionResult.FAIL;
        }
        Allay allay = EntityType.ALLAY.create(level);
        if (allay == null) return InteractionResult.FAIL;
        allay.load(stack.get(DataComponents.CUSTOM_DATA).copyTag().getCompound(KEY));
        allay.setUUID(UUID.randomUUID()); // a copied capsule (creative) must not clash with an allay already out
        BlockPos at = bind ? clicked.above() : clicked.relative(context.getClickedFace());
        allay.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, allay.getYRot(), 0);
        if (bind) allay.setData(AllayBinding.ALTAR, clicked);
        else allay.removeData(AllayBinding.ALTAR);
        level.addFreshEntity(allay);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.remove(KEY));
        if (stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        if (bind && player != null) player.displayClientMessage(Component.translatable("message.enchantmod.allay.bound"), true);
        return InteractionResult.SUCCESS;
    }

    /**
     * Whether the altar can bind one more allay ({@code max_allays} in altar.json, -1 = no limit). Counts the bound allays loaded round it.
     * shortcut: an allay in an unloaded chunk is not counted; keep a list of bound allays in the altar if the limit is ever used.
     */
    private static boolean roomFor(Level level, BlockPos altar) {
        int max = Balance.current().altar().maxAllays();
        if (max < 0) return true;
        var bound = level.getEntitiesOfClass(Allay.class, new AABB(altar).inflate(64),
                a -> a.hasData(AllayBinding.ALTAR) && a.getData(AllayBinding.ALTAR).equals(altar));
        return bound.size() < max;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return full(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(full(stack) ? "item.enchantmod.allay_capsule.full" : "item.enchantmod.allay_capsule.empty"));
    }
}
