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
import net.minecraft.nbt.StringTag;
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
 * (core in its tier 0 area) and glass sphere (core in its tier 1 area), takes its tier (2 with both, 1 with the wheel, else 0) and links
 * the chiseled bookshelves in the tier's cylinder ({@link AltarPower}). A quill starts an enchanting ({@link AltarSpin}; author,
 * 2026-11-23): the linked shelves must hold enough power for every fitting enchant at its highest level; books are picked, up to
 * {@code books_per_shelf} a shelf; the altar and wheel spin up; then one enchant at a time is rolled and charged, its power flowing
 * from the books at the tier's rate (a book gives only what is needed, keeps the rest as {@code spent_power}, and turns plain once
 * empty) and fixed onto the item when it is in; finally the spin slows down. The client draws the flow ({@code client/AltarRibbons}).
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

    /** The phase of the enchanting ({@link AltarSpin}), ticks into it and its length. Synced; the client counts ticks on its own between syncs. */
    private int phase = AltarSpin.IDLE, phaseTicks, phaseLength;
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
    /** The books picked to pay for the enchanting in progress, in the order they are drawn from. Synced (the client draws ribbons from them). */
    private final List<Book> books = new ArrayList<>();
    /** The cube's enchants still to charge, in order, as enchantment ids. */
    private final List<String> queue = new ArrayList<>();
    /** The enchant charging now (id and rolled level), and how much each book gives to it. Synced. */
    private String current = "";
    private int currentLevel;
    private final List<Feed> feeds = new ArrayList<>();
    /** The feeds of the enchant fixed last: its ribbons' tails are still being drawn into the wheel early in the next phase (synced). */
    private final List<Feed> trail = new ArrayList<>();
    /** Client: ticks until the glyph burst of the enchant just fixed, once its power has visibly reached the item; -1 for none. */
    private int burstIn = -1;
    /** Enchants fixed so far (id and level), and rolls that gave nothing. */
    private final List<String> fixed = new ArrayList<>();
    private int failed;

    /** A picked book: its shelf and slot. */
    public record Book(BlockPos shelf, int slot) {}

    /** What book {@code book} (an index into {@link #books}) gives to the enchant charging now. */
    public record Feed(int book, int amount) {}

    public AltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALTAR.get(), pos, state);
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public boolean busy() {
        return phase != AltarSpin.IDLE;
    }

    public int phase() {
        return phase;
    }

    /** How far the charging enchant is, 0 to 1, smoothed with the partial tick; 0 outside the charge. */
    public float chargeProgress(float partialTick) {
        return phase == AltarSpin.CHARGE && phaseLength > 0 ? Math.min(1, (phaseTicks + partialTick) / phaseLength) : 0;
    }

    /** Length of the current phase in ticks. */
    public int phaseLength() {
        return phaseLength;
    }

    /** Ticks into the current phase, smoothed with the partial tick. */
    public float phaseTime(float partialTick) {
        return phaseTicks + partialTick;
    }

    /** Spin speed from 0 to 1. */
    public float speed(float partialTick) {
        return AltarSpin.speed(phase, phaseTicks + partialTick, phaseLength);
    }

    /** Where the books feeding the enchant charging now are: the front of each book's slot. Client side, for the ribbons. */
    public List<Vec3> feedSources() {
        return sources(feeds);
    }

    /** Where the books of the enchant fixed last are, for its ribbons' tails. Client side. */
    public List<Vec3> trailSources() {
        return sources(trail);
    }

    /** How many enchants are fixed so far: the charging enchant's number, so each enchant's ribbons take their own shape. */
    public int enchantIndex() {
        return fixed.size();
    }

    private List<Vec3> sources(List<Feed> list) {
        List<Vec3> out = new ArrayList<>();
        for (Feed f : list) {
            if (f.book() < 0 || f.book() >= books.size()) continue;
            Book b = books.get(f.book());
            out.add(ShelfSlot.front(b.shelf(), level.getBlockState(b.shelf()), b.slot()));
        }
        return out;
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

    /** A book's enchant power less what an altar has already drawn from it. */
    private static int remainingPower(ItemStack stack) {
        return Math.max(0, bookPower(stack) - stack.getOrDefault(ModDataComponents.SPENT_POWER.get(), 0));
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
            for (int slot = 0; slot < shelf.getContainerSize(); slot++) sum += remainingPower(shelf.getItem(slot));
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
     * Starts the enchanting when the linked shelves hold enough power for every fitting enchant at the highest level it can roll
     * (author, 2026-11-23: checked up front). Random enchanted books are picked, up to {@code books_per_shelf} from each shelf, the shelves
     * in a random order. Server side only.
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
        int cost = fits.stream().mapToInt(e -> Balance.current().altar().cost(e.getKey().enchantment(), e.getKey().chances().stream().mapToInt(CubeContents.Chance::level).max().orElse(0))).sum();
        List<BlockPos> order = new ArrayList<>(shelves);
        Util.shuffle(order, level.random);
        List<Book> picked = new ArrayList<>();
        int available = 0;
        for (BlockPos pos : order) {
            if (!(level.getBlockEntity(pos) instanceof ChiseledBookShelfBlockEntity shelf)) continue;
            List<Integer> slots = new ArrayList<>();
            for (int slot = 0; slot < shelf.getContainerSize(); slot++) if (remainingPower(shelf.getItem(slot)) > 0) slots.add(slot);
            Util.shuffle(slots, level.random);
            for (int slot : slots.subList(0, Math.min(slots.size(), Balance.current().altar().booksPerShelf()))) {
                picked.add(new Book(pos, slot));
                available += remainingPower(shelf.getItem(slot));
            }
        }
        if (available < cost) {
            player.displayClientMessage(Component.translatable("message.enchantmod.altar.not_enough", available, cost), true);
            return;
        }
        books.clear();
        books.addAll(picked);
        queue.clear();
        fits.forEach(e -> queue.add(e.getKey().enchantment()));
        feeds.clear();
        trail.clear();
        fixed.clear();
        failed = 0;
        current = "";
        starter = player.getUUID();
        enter(AltarSpin.WARMUP, Balance.current().altar().rampUp());
        player.displayClientMessage(Component.translatable("message.enchantmod.altar.started", cost, picked.size()), true);
    }

    private void enter(int newPhase, int length) {
        phase = newPhase;
        phaseTicks = 0;
        phaseLength = length;
        sync();
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
        phaseTicks++;
        if (phaseTicks < phaseLength) {
            setChanged();
            return;
        }
        switch (phase) {
            case AltarSpin.WARMUP -> nextEnchant();
            case AltarSpin.CHARGE -> {
                if (fixCurrent()) nextEnchant();
            }
            default -> {
                phase = AltarSpin.IDLE;
                feeds.clear();
                power = currentPower();
                sync();
            }
        }
    }

    private Player player() {
        return starter == null ? null : level.getPlayerByUUID(starter);
    }

    /**
     * Rolls the next enchant in the queue and starts charging it: its rolled level times its rarity weight times the cost per level, drawn from the picked
     * books in order, each giving only what is still needed ({@link AltarPower#allot}). An enchant that rolls nothing, or that clashes
     * with one fixed just now, is skipped. When the queue is empty the enchanting ends.
     */
    private void nextEnchant() {
        ItemStack target = inventory.getStackInSlot(ITEM), cube = inventory.getStackInSlot(CUBE);
        CubeContents contents = cube.getOrDefault(ModDataComponents.CUBE_CONTENTS.get(), CubeContents.EMPTY);
        while (!queue.isEmpty()) {
            String id = queue.remove(0);
            Holder<Enchantment> holder = enchantment(id);
            CubeContents.Entry entry = contents.entries().stream().filter(e -> e.enchantment().equals(id)).findFirst().orElse(null);
            if (holder == null || entry == null || target.isEmpty() || !EnchantmentHelper.isEnchantmentCompatible(EnchantmentHelper.getEnchantmentsForCrafting(target).keySet(), holder)) continue;
            int rolled = entry.roll(level.random.nextDouble());
            if (rolled <= 0) {
                failed++;
                continue;
            }
            int cost = Balance.current().altar().cost(id, rolled);
            int[] remaining = new int[books.size()];
            for (int i = 0; i < books.size(); i++) remaining[i] = remainingPower(bookAt(books.get(i)));
            int[] taken = AltarPower.allot(remaining, cost);
            if (taken == null) {
                stop(Component.translatable("message.enchantmod.altar.lost_book"));
                return;
            }
            feeds.clear();
            for (int i = 0; i < taken.length; i++) if (taken[i] > 0) feeds.add(new Feed(i, taken[i]));
            current = id;
            currentLevel = rolled;
            enter(AltarSpin.CHARGE, AltarSpin.chargeTicks(feeds.size(), Balance.current().altar().drainTicksPerBook()));
            return;
        }
        stop(null);
    }

    /** The charged enchant's power is in: the books give their share and it is fixed onto the item. False when a book went missing. */
    private boolean fixCurrent() {
        trail.clear();
        for (Feed f : feeds) {
            if (remainingPower(bookAt(books.get(f.book()))) < f.amount()) {
                stop(Component.translatable("message.enchantmod.altar.lost_book"));
                return false;
            }
        }
        for (Feed f : feeds) spend(books.get(f.book()), f.amount());
        trail.addAll(feeds);
        feeds.clear();
        Holder<Enchantment> holder = enchantment(current);
        ItemStack target = inventory.getStackInSlot(ITEM);
        if (holder != null && !target.isEmpty()) {
            EnchantmentHelper.updateEnchantments(target, enchants -> enchants.upgrade(holder, currentLevel));
            inventory.setStackInSlot(ITEM, target); // updateEnchantments changed the stack in place; this resyncs the client
            fixed.add(current + "|" + currentLevel);
            level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.blockEvent(worldPosition, getBlockState().getBlock(), FIXED_EVENT, 0);
        }
        return true;
    }

    /** Draws {@code amount} power from a book: it remembers what was spent, and turns into a plain book of its tier once empty. */
    private void spend(Book b, int amount) {
        if (!(level.getBlockEntity(b.shelf()) instanceof ChiseledBookShelfBlockEntity shelf)) return;
        ItemStack book = shelf.getItem(b.slot());
        int spent = book.getOrDefault(ModDataComponents.SPENT_POWER.get(), 0) + amount;
        if (spent >= bookPower(book)) {
            shelf.setItem(b.slot(), new ItemStack(Books.plain(Math.max(0, Books.tier(book)))));
            level.playSound(null, b.shelf(), SoundEvents.CHISELED_BOOKSHELF_PICKUP_ENCHANTED, SoundSource.BLOCKS, 0.6F, 1.0F);
        } else {
            book.set(ModDataComponents.SPENT_POWER.get(), spent);
            shelf.setChanged();
        }
    }

    private ItemStack bookAt(Book b) {
        return level.getBlockEntity(b.shelf()) instanceof ChiseledBookShelfBlockEntity shelf ? shelf.getItem(b.slot()) : ItemStack.EMPTY;
    }

    private Holder<Enchantment> enchantment(String id) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(id))).orElse(null);
    }

    /**
     * Ends the enchanting and slows down. The enchants fixed so far stay; the cube is used up unless nothing was rolled at all
     * (author, 2026-11-23: also when a book went missing part way, then {@code reason} is said instead of the result).
     */
    private void stop(Component reason) {
        queue.clear();
        feeds.clear();
        Player player = player();
        List<Component> names = new ArrayList<>();
        for (String f : fixed) {
            String[] parts = f.split("\\|");
            Holder<Enchantment> holder = enchantment(parts[0]);
            if (holder != null) names.add(Enchantment.getFullname(holder, Integer.parseInt(parts[1])));
        }
        if (fixed.isEmpty() && failed == 0 && reason == null) tell(player, Component.translatable("message.enchantmod.altar.nothing"));
        else {
            inventory.setStackInSlot(CUBE, ItemStack.EMPTY);
            if (fixed.isEmpty()) level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            tell(player, reason != null ? reason : names.isEmpty() ? Component.translatable("message.enchantmod.altar.failed")
                    : Component.translatable("message.enchantmod.altar.done", ComponentUtils.formatList(names, Component.literal(", "))));
        }
        enter(AltarSpin.COOLDOWN, Balance.current().altar().rampDown());
    }

    /** Ticks after an enchant is fixed until its power visibly reaches the item: the ribbon's tail into the wheel, then the threads. */
    public static final int TRAIL_TICKS = 40, THREAD_TICKS = 12;

    /** Block event: an enchant was just fixed; the client bursts enchant glyphs into the item once the power has arrived. */
    private static final int FIXED_EVENT = 1;

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id != FIXED_EVENT) return super.triggerEvent(id, param);
        if (level != null && level.isClientSide) burstIn = TRAIL_TICKS + (wheel != null ? THREAD_TICKS : 0);
        return true;
    }

    private void burst() {
        Vec3 item = Vec3.atLowerCornerOf(worldPosition).add(0.5, 1.625, 0.5);
        for (int i = 0; i < 24; i++) {
            double ex = item.x + spread(0.3), ey = item.y + spread(0.3), ez = item.z + spread(0.3);
            level.addParticle(ModParticles.DRAIN_GLYPH.get(), ex, ey, ez, spread(2.4), spread(2.4), spread(2.4));
        }
    }

    /** A random offset in [-width / 2, width / 2). */
    private double spread(double width) {
        return (level.random.nextDouble() - 0.5) * width;
    }

    public void clientTick() {
        // the cool-down squeezes the last drain into its length, so the last burst must not come after it
        if (phase == AltarSpin.COOLDOWN && burstIn > 0) burstIn = Math.min(burstIn, Math.max(0, phaseLength - phaseTicks - 1));
        if (burstIn >= 0 && burstIn-- == 0) burst();
        if (!busy()) return;
        angle = (angle + speed(0) * MAX_DEGREES_PER_TICK) % 360;
        if (phaseTicks < phaseLength) phaseTicks++;
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

    private static void tell(Player player, Component message) {
        if (player != null) player.displayClientMessage(message, true);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", inventory.serializeNBT(registries));
        tag.putInt("phase", phase);
        tag.putInt("phase_ticks", phaseTicks);
        tag.putInt("phase_length", phaseLength);
        if (starter != null) tag.putUUID("starter", starter);
        tag.putInt("tier", tier);
        tag.putInt("power", power);
        if (wheel != null) tag.putLong("wheel", wheel.asLong());
        if (sphere != null) tag.putLong("sphere", sphere.asLong());
        tag.putLongArray("shelves", shelves.stream().mapToLong(BlockPos::asLong).toArray());
        ListTag picked = new ListTag();
        for (Book b : books) {
            CompoundTag t = new CompoundTag();
            t.putLong("pos", b.shelf().asLong());
            t.putInt("slot", b.slot());
            picked.add(t);
        }
        tag.put("books", picked);
        ListTag feedList = new ListTag();
        for (Feed f : feeds) {
            CompoundTag t = new CompoundTag();
            t.putInt("book", f.book());
            t.putInt("amount", f.amount());
            feedList.add(t);
        }
        tag.put("feeds", feedList);
        ListTag trailList = new ListTag();
        for (Feed f : trail) {
            CompoundTag t = new CompoundTag();
            t.putInt("book", f.book());
            t.putInt("amount", f.amount());
            trailList.add(t);
        }
        tag.put("trail", trailList);
        ListTag queued = new ListTag();
        queue.forEach(id -> queued.add(StringTag.valueOf(id)));
        tag.put("queue", queued);
        ListTag done = new ListTag();
        fixed.forEach(f -> done.add(StringTag.valueOf(f)));
        tag.put("fixed", done);
        tag.putString("current", current);
        tag.putInt("current_level", currentLevel);
        tag.putInt("failed", failed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("items"));
        phase = tag.getInt("phase");
        phaseTicks = tag.getInt("phase_ticks");
        phaseLength = tag.getInt("phase_length");
        starter = tag.hasUUID("starter") ? tag.getUUID("starter") : null;
        tier = tag.contains("tier") ? tag.getInt("tier") : -1;
        power = tag.getInt("power");
        wheel = tag.contains("wheel") ? BlockPos.of(tag.getLong("wheel")) : null;
        sphere = tag.contains("sphere") ? BlockPos.of(tag.getLong("sphere")) : null;
        shelves = Arrays.stream(tag.getLongArray("shelves")).mapToObj(BlockPos::of).toList();
        books.clear();
        for (Tag t : tag.getList("books", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            books.add(new Book(BlockPos.of(c.getLong("pos")), c.getInt("slot")));
        }
        feeds.clear();
        for (Tag t : tag.getList("feeds", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            feeds.add(new Feed(c.getInt("book"), c.getInt("amount")));
        }
        trail.clear();
        for (Tag t : tag.getList("trail", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            trail.add(new Feed(c.getInt("book"), c.getInt("amount")));
        }
        queue.clear();
        for (Tag t : tag.getList("queue", Tag.TAG_STRING)) queue.add(t.getAsString());
        fixed.clear();
        for (Tag t : tag.getList("fixed", Tag.TAG_STRING)) fixed.add(t.getAsString());
        current = tag.getString("current");
        currentLevel = tag.getInt("current_level");
        failed = tag.getInt("failed");
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
