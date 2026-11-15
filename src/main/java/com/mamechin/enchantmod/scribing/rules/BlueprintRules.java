package com.mamechin.enchantmod.scribing.rules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The editing rules of the enchant template, with no game code in it so they can be checked on their own. Every method
 * returns the new blueprint, or null when the edit is not allowed. The server runs these on every edit a client sends.
 *
 * Rules: a core rune covers a 3x3 square of cells, a stabilizer 2x2 and the boost rune one cell, the cell stored is the centre (top left of the middle for 2x2); every
 * cell holds one thing (part of a rune or a line cell); two runes never touch, not even at a corner; lines move one cell
 * at a time, never diagonally, so two lines can never cross on a sheet; a line runs along the outside of the runes and
 * never over one; erasing a rune also erases its lines.
 */
public final class BlueprintRules {
    public static final int WIDTH = 15;
    public static final int HEIGHT = 11;
    public static final int CELLS = WIDTH * HEIGHT;

    public static boolean inGrid(int cell) {
        return cell >= 0 && cell < CELLS;
    }

    private static boolean adjacent(int a, int b) {
        int dx = Math.abs(a % WIDTH - b % WIDTH), dy = Math.abs(a / WIDTH - b / WIDTH);
        return dx + dy == 1;
    }

    /** The side length of the square a rune covers; 1 for an unknown rune. */
    public static int sizeOf(String runeId) {
        Rune rune = Rune.byId(runeId);
        return rune == null ? 1 : rune.size();
    }

    /** The cells a rune of this size covers around its anchor cell (the centre for 1 and 3, the top left of the middle for 2), or null when any of them is outside the grid. */
    public static Set<Integer> footprint(int centre, int size) {
        if (!inGrid(centre)) return null;
        int lo = -(size - 1) / 2, hi = size / 2, cx = centre % WIDTH, cy = centre / WIDTH; // even sizes anchor at the top left of the middle
        if (cx + lo < 0 || cx + hi >= WIDTH || cy + lo < 0 || cy + hi >= HEIGHT) return null;
        Set<Integer> cells = new HashSet<>();
        for (int y = cy + lo; y <= cy + hi; y++) for (int x = cx + lo; x <= cx + hi; x++) cells.add(y * WIDTH + x);
        return cells;
    }

    private static Set<Integer> footprint(Blueprint.RuneAt rune) {
        Set<Integer> cells = footprint(rune.cell(), sizeOf(rune.rune()));
        return cells == null ? Set.of(rune.cell()) : cells;
    }

    /** Every cell covered by a rune. */
    public static Set<Integer> runeCells(Blueprint bp) {
        Set<Integer> cells = new HashSet<>();
        for (Blueprint.RuneAt r : bp.runes()) cells.addAll(footprint(r));
        return cells;
    }

    /** Every cell covered by a rune or holding a line cell. */
    public static Set<Integer> occupied(Blueprint bp) {
        Set<Integer> cells = runeCells(bp);
        for (Blueprint.Line l : bp.lines()) cells.addAll(l.path());
        return cells;
    }

    /** True when a rune has this cell as its centre; lines are stored against centre cells. */
    public static boolean hasRune(Blueprint bp, int cell) {
        for (Blueprint.RuneAt r : bp.runes()) if (r.cell() == cell) return true;
        return false;
    }

    /** The rune covering this cell, or null. */
    public static Blueprint.RuneAt runeAt(Blueprint bp, int cell) {
        for (Blueprint.RuneAt r : bp.runes()) if (footprint(r).contains(cell)) return r;
        return null;
    }

    /** True when the cell is straight above, below, left or right of a cell of this rune. */
    public static boolean touches(Blueprint.RuneAt rune, int cell) {
        Set<Integer> cells = footprint(rune);
        for (int c : cells) if (adjacent(c, cell)) return true;
        return false;
    }

    public static Blueprint place(Blueprint bp, int cell, String runeId) {
        return place(bp, cell, runeId, TemplateLimits.FULL);
    }

    /** Places a rune; every cell it covers must be inside the template's drawing area. */
    public static Blueprint place(Blueprint bp, int cell, String runeId, TemplateLimits tier) {
        // shortcut: any rune is accepted; once runes are discovered, check the player's known runes here
        if (Rune.byId(runeId) == null) return null;
        Set<Integer> cells = footprint(cell, sizeOf(runeId));
        if (cells == null || !cells.stream().allMatch(tier::contains)) return null;
        Set<Integer> taken = occupied(bp), runes = runeCells(bp);
        for (int c : cells) {
            if (taken.contains(c)) return null;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int x = c % WIDTH + dx, y = c / WIDTH + dy;
                    if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT && runes.contains(y * WIDTH + x)) return null;
                }
            }
        }
        List<Blueprint.RuneAt> list = new ArrayList<>(bp.runes());
        list.add(new Blueprint.RuneAt(cell, runeId));
        return new Blueprint(list, bp.lines());
    }

    /** Erases the rune covering this cell with all its lines, or, for a line cell, just that line. */
    public static Blueprint erase(Blueprint bp, int cell) {
        Blueprint.RuneAt rune = runeAt(bp, cell);
        if (rune != null) {
            List<Blueprint.RuneAt> runes = new ArrayList<>(bp.runes());
            runes.remove(rune);
            List<Blueprint.Line> lines = new ArrayList<>(bp.lines());
            lines.removeIf(l -> l.a() == rune.cell() || l.b() == rune.cell());
            return new Blueprint(runes, lines);
        }
        List<Blueprint.Line> lines = new ArrayList<>(bp.lines());
        if (!lines.removeIf(l -> l.path().contains(cell))) return null;
        return new Blueprint(bp.runes(), lines);
    }

    /**
     * Adds a line from rune a to rune b (given by their centre cells) along path, the cells between them in order. The
     * path starts next to rune a, ends next to rune b and is never empty, since runes never touch. Parallel lines are
     * allowed.
     */
    public static Blueprint addLine(Blueprint bp, int a, int b, List<Integer> path) {
        return addLine(bp, a, b, path, TemplateLimits.FULL);
    }

    /** As above, with every path cell inside the template's drawing area. */
    public static Blueprint addLine(Blueprint bp, int a, int b, List<Integer> path, TemplateLimits tier) {
        if (a == b || !hasRune(bp, a) || !hasRune(bp, b) || path.isEmpty() || path.size() > CELLS || bp.lines().size() >= CELLS) return null;
        Blueprint.RuneAt runeA = runeAt(bp, a), runeB = runeAt(bp, b);
        Set<Integer> taken = occupied(bp);
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < path.size(); i++) {
            int cell = path.get(i);
            if (!inGrid(cell) || !tier.contains(cell) || taken.contains(cell) || !seen.add(cell)) return null;
            if (i == 0 ? !touches(runeA, cell) : !adjacent(path.get(i - 1), cell)) return null;
        }
        if (!touches(runeB, path.get(path.size() - 1))) return null;
        List<Blueprint.Line> lines = new ArrayList<>(bp.lines());
        lines.add(new Blueprint.Line(a, b, path));
        return new Blueprint(bp.runes(), lines);
    }

    /** Ink an edit costs: one durability for each cell it fills, so a core rune costs 9 and a line costs its length. Erasing is free. */
    public static int cost(Blueprint before, Blueprint after) {
        return Math.max(0, occupied(after).size() - occupied(before).size());
    }

    private BlueprintRules() {}
}
