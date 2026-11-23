package com.mamechin.enchantmod.allay;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.altar.AltarBlockEntity;
import com.mamechin.enchantmod.altar.AltarPower;
import com.mamechin.enchantmod.balance.Balance;
import com.mamechin.enchantmod.books.Books;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Allays bound to an altar (author, 2026-11-23): one with {@link #ALTAR} set picks up dropped books (plain or enchanted, every tier) in
 * the altar's scan area, one at a time, and puts each in a free slot of a shelf the altar linked; with nothing to do it stays near the
 * altar. It is steered by setting its brain's walk target, so its own flying and wandering still run. The binding is saved with the
 * allay, so it survives a capsule trip; it is dropped when the altar is gone.
 */
public final class AllayBinding {
    private AllayBinding() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EnchantMod.MODID);
    /** The altar an allay is bound to. */
    public static final Supplier<AttachmentType<BlockPos>> ALTAR = ATTACHMENTS.register("bound_altar",
            () -> AttachmentType.builder(() -> BlockPos.ZERO).serialize(BlockPos.CODEC).build());
    /** The shelf picked for the book in hand, kept for the whole trip. Not saved: a reloaded allay picks again. */
    private static final Supplier<AttachmentType<BlockPos>> SHELF = ATTACHMENTS.register("target_shelf",
            () -> AttachmentType.builder(() -> BlockPos.ZERO).build());

    private static final float SPEED = 2;
    private static final double REACH_ITEM = 2 * 2, REACH_SHELF = 3 * 3, STAY = 8 * 8;

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(AllayBinding::onInteract);
        NeoForge.EVENT_BUS.addListener((EntityTickEvent.Post e) -> {
            if (e.getEntity() instanceof Allay allay && !allay.level().isClientSide && allay.hasData(ALTAR) && (allay.tickCount + allay.getId()) % 5 == 0) tick(allay);
        });
    }

    /** A capsule on an allay catches it. Runs before the allay's own use, which would otherwise take the capsule from the hand. */
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof AllayCapsule) || !(event.getTarget() instanceof Allay allay)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (!event.getLevel().isClientSide && !AllayCapsule.full(held) && allay.isAlive()) AllayCapsule.capture(held, allay);
    }

    private static void tick(Allay allay) {
        Level level = allay.level();
        BlockPos altarPos = allay.getData(ALTAR);
        if (!level.isLoaded(altarPos)) return;
        if (!(level.getBlockEntity(altarPos) instanceof AltarBlockEntity altar)) {
            allay.removeData(ALTAR);
            return;
        }
        Brain<?> brain = allay.getBrain();
        brain.eraseMemory(MemoryModuleType.LIKED_PLAYER); // it serves the altar, not a player
        // the allay's own pickups (items like the one in its hand) land in its inventory: carry those next
        if (allay.getMainHandItem().isEmpty() && Books.tier(allay.getInventory().getItem(0)) >= 0) {
            allay.setItemInHand(InteractionHand.MAIN_HAND, allay.getInventory().removeItem(0, 1));
        }
        ItemStack hand = allay.getMainHandItem();
        if (Books.tier(hand) >= 0) {
            ChiseledBookShelfBlockEntity shelf = shelfFor(allay, altar, hand);
            if (shelf == null) stay(allay, brain, altarPos);
            else if (allay.distanceToSqr(Vec3.atCenterOf(shelf.getBlockPos())) > REACH_SHELF) walk(brain, shelf.getBlockPos());
            else {
                for (int slot = 0; slot < shelf.getContainerSize(); slot++) {
                    if (shelf.canPlaceItem(slot, hand)) {
                        shelf.setItem(slot, hand.copyWithCount(1));
                        allay.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        allay.removeData(SHELF);
                        break;
                    }
                }
            }
            return;
        }
        if (!hand.isEmpty()) return; // holding something a player gave it
        ItemEntity book = nearestBook(level, altar, allay.position());
        if (book == null) stay(allay, brain, altarPos);
        else if (allay.distanceToSqr(book) > REACH_ITEM) walk(brain, book.blockPosition());
        else {
            allay.take(book, 1);
            ItemStack rest = book.getItem().copy();
            allay.setItemInHand(InteractionHand.MAIN_HAND, rest.split(1));
            if (rest.isEmpty()) book.discard();
            else book.setItem(rest);
        }
    }

    private static void walk(Brain<?> brain, BlockPos pos) {
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, SPEED, 1));
    }

    private static void stay(Allay allay, Brain<?> brain, BlockPos altar) {
        if (allay.distanceToSqr(Vec3.atCenterOf(altar)) > STAY) walk(brain, altar.above());
    }

    /** The shelf picked for this trip while it still has room, else a random linked shelf with a free slot for {@code book} (author), or null. */
    private static ChiseledBookShelfBlockEntity shelfFor(Allay allay, AltarBlockEntity altar, ItemStack book) {
        Level level = allay.level();
        if (allay.hasData(SHELF)) {
            ChiseledBookShelfBlockEntity kept = freeShelf(level, allay.getData(SHELF), book);
            if (kept != null) return kept;
            allay.removeData(SHELF);
        }
        List<ChiseledBookShelfBlockEntity> free = altar.shelves().stream().map(pos -> freeShelf(level, pos, book)).filter(Objects::nonNull).toList();
        if (free.isEmpty()) return null;
        ChiseledBookShelfBlockEntity pick = free.get(allay.getRandom().nextInt(free.size()));
        allay.setData(SHELF, pick.getBlockPos());
        return pick;
    }

    /** The shelf at {@code pos} when it is loaded and has a free slot for {@code book}, else null. */
    private static ChiseledBookShelfBlockEntity freeShelf(Level level, BlockPos pos, ItemStack book) {
        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof ChiseledBookShelfBlockEntity shelf)) return null;
        for (int slot = 0; slot < shelf.getContainerSize(); slot++) if (shelf.canPlaceItem(slot, book)) return shelf;
        return null;
    }

    /** The nearest dropped book in the altar's area that can be picked up now, or null. */
    private static ItemEntity nearestBook(Level level, AltarBlockEntity altar, Vec3 from) {
        Balance.Area area = Balance.current().altar().area(altar.tier());
        BlockPos o = altar.getBlockPos();
        AABB box = new AABB(o.getX() - area.radius(), o.getY() + area.minY(), o.getZ() - area.radius(),
                o.getX() + area.radius() + 1, o.getY() + area.maxY() + 1, o.getZ() + area.radius() + 1);
        return level.getEntitiesOfClass(ItemEntity.class, box, e -> !e.hasPickUpDelay() && Books.tier(e.getItem()) >= 0 && inArea(area, o, e.blockPosition()))
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(from))).orElse(null);
    }

    private static boolean inArea(Balance.Area area, BlockPos altar, BlockPos pos) {
        return AltarPower.inArea(area, pos.getX() - altar.getX(), pos.getY() - altar.getY(), pos.getZ() - altar.getZ());
    }
}
