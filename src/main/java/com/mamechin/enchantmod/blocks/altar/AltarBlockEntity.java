package com.mamechin.enchantmod.blocks.altar;

import com.mamechin.enchantmod.registry.ModTags;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mamechin.enchantmod.registry.ModBlockEntities;
import com.mamechin.enchantmod.items.books.Books;
import com.mamechin.enchantmod.multiblock.sphere.SphereCoreBlock;
import com.mamechin.enchantmod.multiblock.wheel.WheelCoreBlock;
import com.mamechin.enchantmod.multiblock.GuidedCoreBlockEntity;
import com.mamechin.enchantmod.balance.Balance;
import com.mamechin.enchantmod.registry.ModParticles;
import com.mamechin.enchantmod.items.cube.EnchantCube;
import com.mamechin.enchantmod.registry.ModDataComponents;
import com.mamechin.enchantmod.enchanting.CubeContents;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;


/**
 * The altar holds the item to enchant and an enchant cube. Shift and a quill scans: the altar links the nearest formed spinning wheel
 * (core in its tier 0 area) and glass sphere (core in its tier 1 area), takes its tier (2 with both, 1 with the wheel, else 0) and links the chiseled bookshelves in the tier's cylinder ({@link AltarPower}). A quill starts a 10
 * enchanting ({@link AltarSpin}): the cost is worked out, one book per linked shelf is picked, the shelves in a random order, until
 * it is paid (or the altar refuses), the flyer spins up for 3 seconds, every picked book drains at once (one second per enchant level,
 * each becoming a plain book when done; the client draws a trail of enchant glyphs from each shelf to the altar, through the wheel
 * at tier 1), the cube's enchants are fixed onto the item, and it slows down. With a formed spinning wheel in front of it the wheel shows the cube and spins too.
 */
public class AltarBlockEntity extends BlockEntity {
    public static final int ITEM = 0, CUBE = 1;

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return (slot == CUBE) == (stack.getItem() instanceof EnchantCube) && !stack.is(ModTags.QUILLS);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };

    /** Ticks left of the enchanting in progress, 0 when idle. Synced; the client counts down on its own between syncs. */
    private int remaining;
    private UUID starter;
    /** Client only: how far the flyer or wheel has turned, in degrees. */
    private float angle;
    /** The tier found by the last scan, -1 before the first. Synced. */
    private int tier = -1;
    /** The formed wheel and sphere cores linked by the last scan, or null. Synced. */
    private BlockPos wheel, sphere;
    /** The chiseled bookshelves linked by the last scan. */
    private List<BlockPos> shelves = List.of();
    /** Enchant power in the linked shelves, refreshed by the server once a second. Synced. */
    private int power;
    /** The books picked to pay for the enchanting in progress, drained in this order, and how many are done. */
    private final List<Drain> drains = new ArrayList<>();
    /** Length of the enchanting in progress, in ticks. Synced. */
    private int duration;

    /** A picked book, the slot it is in and its enchant levels (one second of draining each); {@code done} once it is a plain book. */
    private static final class Drain {
        final BlockPos shelf;
        final int slot, levels;
        boolean done;

        Drain(BlockPos shelf, int slot, int levels) {
            this.shelf = shelf;
            this.slot = slot;
            this.levels = levels;
        }
    }

    public AltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALTAR.get(), pos, state);
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public boolean busy() {
        return remaining > 0;
    }

    /** Ticks since the quill click (fractional with the partial tick), 0 when idle. */
    public float elapsed(float partialTick) {
        return busy() ? duration - remaining + partialTick : 0;
    }

    /** Spin speed from 0 to 1. */
    public float speed(float partialTick) {
        return busy() ? AltarSpin.speed(elapsed(partialTick), duration) : 0;
    }

    /** Degrees turned so far, smoothed with the partial tick. Client side. */
    public float angle(float partialTick) {
        return angle + speed(partialTick) * MAX_DEGREES_PER_TICK * partialTick;
    }

    private static final float MAX_DEGREES_PER_TICK = 24;

    public int tier() {
        return tier;
    }

    /** The wheel core linked by the last scan, or null. */
    public BlockPos wheel() {
        return wheel;
    }

    /** The sphere core linked by the last scan, or null. */
    public BlockPos sphere() {
        return sphere;
    }

    /** The chiseled bookshelves linked by the last scan. */
    public List<BlockPos> shelves() {
        return shelves;
    }

    public int power() {
        return power;
    }

    /** Finds the tier and links the chiseled bookshelves in its cylinder. Server side only. */
    public void scan(Player player) {
        if (busy()) {
            player.displayClientMessage(Component.translatable("message.enchantmod.altar.busy"), true);
            return;
        }
        wheel = nearestFormed(WheelCoreBlock.class, WheelCoreBlock.FORMED, 0);
        sphere = nearestFormed(SphereCoreBlock.class, SphereCoreBlock.FORMED, 1);
        tier = wheel == null ? 0 : sphere == null ? 1 : 2;
        for (BlockPos core : new BlockPos[] {wheel, sphere}) {
            if (core != null && level.getBlockEntity(core) instanceof GuidedCoreBlockEntity guided) guided.setAltar(worldPosition);
        }
        shelves = blockEntitiesIn(tier).stream().filter(be -> be instanceof ChiseledBookShelfBlockEntity).map(BlockEntity::getBlockPos).toList();
        power = currentPower();
        sync();
        player.displayClientMessage(Component.translatable("message.enchantmod.altar.scanned", tier, shelves.size(), power), true);
    }

    /** The nearest formed core of that block in the area of {@code tier} (author, 2026-11-22: placed freely round the altar), or null. */
    private BlockPos nearestFormed(Class<? extends Block> type, BooleanProperty formed, int tier) {
        BlockPos best = null;
        for (BlockEntity be : blockEntitiesIn(tier)) {
            BlockPos pos = be.getBlockPos();
            BlockState state = be.getBlockState();
            if (type.isInstance(state.getBlock()) && state.getValue(formed) && (best == null || pos.distSqr(worldPosition) < best.distSqr(worldPosition))) best = pos;
        }
        return best;
    }

    /**
     * The block entities in the area of {@code tier}, read from the block entity lists of the loaded chunks it touches (shelves and
     * cores all have one), so even the radius 32 area costs a few dozen chunk lookups instead of a hundred thousand cells. Chunks that
     * are not loaded are skipped, never loaded.
     */
    private List<BlockEntity> blockEntitiesIn(int tier) {
        Balance.Area area = Balance.current().altar().area(tier);
        int r = area.radius(), x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        List<BlockEntity> out = new ArrayList<>();
        for (int cx = (x - r) >> 4; cx <= (x + r) >> 4; cx++) {
            for (int cz = (z - r) >> 4; cz <= (z + r) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getBlockPos();
                    if (!be.isRemoved() && AltarPower.inArea(area, pos.getX() - x, pos.getY() - y, pos.getZ() - z)) out.add(be);
                }
            }
        }
        return out;
    }

    /** Total enchant levels stored in a book, 0 for anything else. */
    private static int bookLevels(ItemStack stack) {
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null) return 0;
        int sum = 0;
        for (var e : stored.entrySet()) sum += e.getIntValue();
        return sum;
    }

    /** Enchant power of a book: the sum over its stored enchants of level times the enchantment's weight ({@code altar.json}). */
    private static int bookPower(ItemStack stack) {
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null) return 0;
        Balance.Altar table = Balance.current().altar();
        int sum = 0;
        for (var e : stored.entrySet()) {
            String id = e.getKey().unwrapKey().map(k -> k.location().toString()).orElse("");
            sum += e.getIntValue() * table.weight(id);
        }
        return sum;
    }

    private int currentPower() {
        int sum = 0;
        for (BlockPos pos : shelves) {
            if (!(level.getBlockEntity(pos) instanceof ChiseledBookShelfBlockEntity shelf)) continue;
            for (int slot = 0; slot < shelf.getContainerSize(); slot++) sum += bookPower(shelf.getItem(slot));
        }
        return sum;
    }

    /** The cube's enchants the item can take now (it supports them and they do not clash with what it has), with their holders. */
    private List<Map.Entry<CubeContents.Entry, Holder<Enchantment>>> fitting(ItemStack target, CubeContents contents) {
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var have = EnchantmentHelper.getEnchantmentsForCrafting(target).keySet();
        List<Map.Entry<CubeContents.Entry, Holder<Enchantment>>> out = new ArrayList<>();
        for (CubeContents.Entry entry : contents.entries()) {
            Holder<Enchantment> holder = registry.get(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(entry.enchantment()))).orElse(null);
            if (holder != null && holder.value().canEnchant(target) && EnchantmentHelper.isEnchantmentCompatible(have, holder)) out.add(Map.entry(entry, holder));
        }
        return out;
    }

    /**
     * Starts the enchanting when the linked shelves hold enough power: the cost is the sum of the highest level each fitting enchant
     * can roll (times the cost per level), and random enchanted books are picked, up to {@code books_per_shelf} from each shelf, the
     * shelves in a random order (a shelf that is gone is skipped).
     * Server side only.
     */
    public void start(Player player) {
        if (busy()) {
            player.displayClientMessage(Component.translatable("message.enchantmod.altar.busy"), true);
            return;
        }
        ItemStack target = inventory.getStackInSlot(ITEM), cube = inventory.getStackInSlot(CUBE);
        if (target.isEmpty() || cube.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.enchantmod.altar.need_both"), true);
            return;
        }
        if (tier < 0) {
            player.displayClientMessage(Component.translatable("message.enchantmod.altar.not_scanned"), true);
            return;
        }
        var fits = fitting(target, cube.getOrDefault(ModDataComponents.CUBE_CONTENTS.get(), CubeContents.EMPTY));
        if (fits.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.enchantmod.altar.nothing"), true);
            return;
        }
        int cost = Balance.current().altar().costPerLevel() * fits.stream().mapToInt(e -> e.getKey().chances().stream().mapToInt(CubeContents.Chance::level).max().orElse(0)).sum();
        List<BlockPos> order = new ArrayList<>(shelves);
        Util.shuffle(order, level.random);
        List<Drain> books = new ArrayList<>();
        List<Integer> powers = new ArrayList<>();
        for (BlockPos pos : order) {
            if (!(level.getBlockEntity(pos) instanceof ChiseledBookShelfBlockEntity shelf)) continue;
            List<Integer> slots = new ArrayList<>();
            for (int slot = 0; slot < shelf.getContainerSize(); slot++) if (bookPower(shelf.getItem(slot)) > 0) slots.add(slot);
            Util.shuffle(slots, level.random);
            for (int slot : slots.subList(0, Math.min(slots.size(), Balance.current().altar().booksPerShelf()))) {
                books.add(new Drain(pos, slot, bookLevels(shelf.getItem(slot))));
                powers.add(bookPower(shelf.getItem(slot)));
            }
        }
        int needed = AltarPower.booksNeeded(powers.stream().mapToInt(Integer::intValue).toArray(), cost);
        if (needed < 0) {
            player.displayClientMessage(Component.translatable("message.enchantmod.altar.not_enough", powers.stream().mapToInt(Integer::intValue).sum(), cost), true);
            return;
        }
        drains.clear();
        drains.addAll(books.subList(0, needed));
        duration = AltarSpin.duration(drains.stream().mapToInt(d -> d.levels).max().orElse(0));
        remaining = duration;
        starter = player.getUUID();
        sync();
        player.displayClientMessage(Component.translatable("message.enchantmod.altar.started", cost, needed), true);
    }

    public void serverTick() {
        if (level.getGameTime() % 20 == 0 && !busy()) {
            int now = currentPower();
            if (now != power) {
                power = now;
                sync();
            }
        }
        if (!busy()) return;
        remaining--;
        int elapsed = duration - remaining;
        boolean changed = false;
        for (Drain d : drains) {
            if (d.done || AltarSpin.drainEnd(d.levels) > elapsed) continue;
            if (!drain(d)) {
                abort();
                return;
            }
            d.done = changed = true;
        }
        if (changed) sync(); // the client stops that book's trail
        if (elapsed == AltarSpin.enchantAt(duration)) {
            drains.clear();
            enchant(player());
        }
        if (remaining == 0) {
            power = currentPower();
            sync();
        } else setChanged();
    }

    private Player player() {
        return starter == null ? null : level.getPlayerByUUID(starter);
    }

    /** Turns one picked book into a plain book. False when the shelf or the enchanted book is gone. */
    private boolean drain(Drain d) {
        if (!(level.getBlockEntity(d.shelf) instanceof ChiseledBookShelfBlockEntity shelf) || bookPower(shelf.getItem(d.slot)) <= 0) return false;
        shelf.setItem(d.slot, new ItemStack(Books.plain(Math.max(0, Books.tier(shelf.getItem(d.slot)))))); // a plain book of the same tier
        level.playSound(null, d.shelf, SoundEvents.CHISELED_BOOKSHELF_PICKUP_ENCHANTED, SoundSource.BLOCKS, 0.6F, 1.0F);
        return true;
    }

    /** Client: every tick while a book drains, a random burst of enchant glyphs leaves its shelf for the altar, or at tier 1 for the wheel's hub, which sends one on to the altar. */
    private void spawnTrails() {
        int elapsed = duration - remaining;
        if (elapsed < AltarSpin.rampUp()) return;
        Vec3 altar = Vec3.atLowerCornerOf(worldPosition).add(0.5, 1.0, 0.5);
        BlockState core = wheel == null ? null : level.getBlockState(wheel);
        Vec3 hub = core != null && core.getBlock() instanceof WheelCoreBlock && core.getValue(WheelCoreBlock.FORMED) ? Vec3.atCenterOf(wheel) : null;
        boolean any = false;
        for (Drain d : drains) {
            if (d.done || elapsed >= AltarSpin.drainEnd(d.levels)) continue;
            any = true;
            wave(Vec3.atCenterOf(d.shelf), hub != null ? hub : altar);
        }
        if (any && hub != null) wave(hub, altar);
    }

    /**
     * A random burst of 0 to 3 enchant glyphs (1.5 a tick on average) that fly from anywhere in the block round {@code from} to near
     * {@code to}, each on its own wandering path ({@code drain_glyph}: it starts at pos + velocity and lands exactly on pos).
     */
    private void wave(Vec3 from, Vec3 to) {
        int count = level.random.nextInt(4);
        for (int i = 0; i < count; i++) {
            double ex = to.x + spread(0.4), ey = to.y + spread(0.4), ez = to.z + spread(0.4);
            level.addParticle(ModParticles.DRAIN_GLYPH.get(), ex, ey, ez,
                    from.x + spread(1.0) - ex, from.y + spread(1.0) - ey, from.z + spread(1.0) - ez);
        }
    }

    /** A random offset in [-width / 2, width / 2). */
    private double spread(double width) {
        return (level.random.nextDouble() - 0.5) * width;
    }

    /** A picked book went missing: no enchant, the cube is kept, and the spin slows down from here. */
    private void abort() {
        drains.clear();
        remaining = AltarSpin.rampDown() - 1; // past the roll, so it only slows down
        power = currentPower();
        sync();
        tell(player(), Component.translatable("message.enchantmod.altar.lost_book"));
    }

    public void clientTick() {
        if (!busy()) return;
        angle = (angle + speed(0) * MAX_DEGREES_PER_TICK) % 360;
        spawnTrails();
        remaining--;
    }

    private void sync() {
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(i));
        }
    }

    /**
     * Fixes the cube's enchants onto the item, each rolled from its chances. An enchant the item cannot take, or that
     * clashes with one already there, is skipped. The cube is used up unless nothing in it fits the item. Server side only;
     * {@code player} (who started it, null if gone) gets the result on the action bar.
     */
    private void enchant(Player player) {
        ItemStack target = inventory.getStackInSlot(ITEM), cube = inventory.getStackInSlot(CUBE);
        if (target.isEmpty() || cube.isEmpty()) return; // the slots are locked while busy, so only a broken world gets here
        var fits = fitting(target, cube.getOrDefault(ModDataComponents.CUBE_CONTENTS.get(), CubeContents.EMPTY));
        List<Component> done = new ArrayList<>();
        int[] failed = {0};
        EnchantmentHelper.updateEnchantments(target, enchants -> {
            for (var fit : fits) {
                Holder<Enchantment> holder = fit.getValue();
                if (!EnchantmentHelper.isEnchantmentCompatible(enchants.keySet(), holder)) continue; // clashes with one fixed just now
                int levelGot = fit.getKey().roll(level.random.nextDouble());
                if (levelGot <= 0) failed[0]++;
                else {
                    enchants.upgrade(holder, levelGot);
                    done.add(Enchantment.getFullname(holder, levelGot));
                }
            }
        });
        inventory.setStackInSlot(ITEM, target); // updateEnchantments changed the stack in place; this resyncs the client
        if (done.isEmpty() && failed[0] == 0) {
            tell(player, Component.translatable("message.enchantmod.altar.nothing"));
            return;
        }
        inventory.setStackInSlot(CUBE, ItemStack.EMPTY); // the cube is used up, whether or not the fixation worked
        level.playSound(null, worldPosition, done.isEmpty() ? SoundEvents.GRINDSTONE_USE : SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        Component list = ComponentUtils.formatList(done, Component.literal(", "));
        tell(player, done.isEmpty()
                ? Component.translatable("message.enchantmod.altar.failed")
                : Component.translatable("message.enchantmod.altar.done", list));
    }

    private static void tell(Player player, Component message) {
        if (player != null) player.displayClientMessage(message, true);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", inventory.serializeNBT(registries));
        tag.putInt("remaining", remaining);
        if (starter != null) tag.putUUID("starter", starter);
        tag.putInt("tier", tier);
        tag.putInt("power", power);
        if (wheel != null) tag.putLong("wheel", wheel.asLong());
        if (sphere != null) tag.putLong("sphere", sphere.asLong());
        tag.putLongArray("shelves", shelves.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putInt("duration", duration);
        ListTag list = new ListTag();
        for (Drain d : drains) {
            CompoundTag t = new CompoundTag();
            t.putLong("pos", d.shelf.asLong());
            t.putInt("slot", d.slot);
            t.putInt("levels", d.levels);
            t.putBoolean("done", d.done);
            list.add(t);
        }
        tag.put("drains", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("items"));
        remaining = tag.getInt("remaining");
        starter = tag.hasUUID("starter") ? tag.getUUID("starter") : null;
        tier = tag.contains("tier") ? tag.getInt("tier") : -1;
        power = tag.getInt("power");
        wheel = tag.contains("wheel") ? BlockPos.of(tag.getLong("wheel")) : null;
        sphere = tag.contains("sphere") ? BlockPos.of(tag.getLong("sphere")) : null;
        shelves = Arrays.stream(tag.getLongArray("shelves")).mapToObj(BlockPos::of).toList();
        duration = tag.getInt("duration");
        drains.clear();
        for (Tag t : tag.getList("drains", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Drain d = new Drain(BlockPos.of(c.getLong("pos")), c.getInt("slot"), c.getInt("levels"));
            d.done = c.getBoolean("done");
            drains.add(d);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
