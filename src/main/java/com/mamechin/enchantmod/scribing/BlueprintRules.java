package com.mamechin.enchantmod.scribing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The editing rules of the enchant template, with no game code in it so they can be checked on their own. Every method
 * returns the new blueprint, or null when the edit is not allowed. The server runs these on every edit a client sends.
 *
 * Rules: one thing per cell (a rune or a line cell); lines move one cell at a time, never diagonally, so two lines can
 * never cross on a sheet; erasing a rune also erases its lines.
 */
public final class BlueprintRules {
    public static final int WIDTH = 12;
    public static final int HEIGHT = 8;
    public static final int CELLS = WIDTH * HEIGHT;

    public static boolean inGrid(int cell) {
        return cell >= 0 && cell < CELLS;
    }

    private static boolean adjacent(int a, int b) {
        int dx = Math.abs(a % WIDTH - b % WIDTH), dy = Math.abs(a / WIDTH - b / WIDTH);
        return dx + dy == 1;
    }

    /** Every cell holding a rune or a line cell. */
    public static Set<Integer> occupied(Blueprint bp) {
        Set<Integer> cells = new HashSet<>();
        for (Blueprint.RuneAt r : bp.runes()) cells.add(r.cell());
        for (Blueprint.Line l : bp.lines()) cells.addAll(l.path());
        return cells;
    }

    public static boolean hasRune(Blueprint bp, int cell) {
        for (Blueprint.RuneAt r : bp.runes()) if (r.cell() == cell) return true;
        return false;
    }

    public static Blueprint place(Blueprint bp, int cell, String runeId) {
        // shortcut: any rune is accepted; once runes are discovered, check the player's known runes here
        if (!inGrid(cell) || Rune.byId(runeId) == null || occupied(bp).contains(cell)) return null;
        List<Blueprint.RuneAt> runes = new ArrayList<>(bp.runes());
        runes.add(new Blueprint.RuneAt(cell, runeId));
        return new Blueprint(runes, bp.lines());
    }

    /** Erases the rune in this cell with all its lines, or, for a line cell, just that line. */
    public static Blueprint erase(Blueprint bp, int cell) {
        if (hasRune(bp, cell)) {
            List<Blueprint.RuneAt> runes = new ArrayList<>(bp.runes());
            runes.removeIf(r -> r.cell() == cell);
            List<Blueprint.Line> lines = new ArrayList<>(bp.lines());
            lines.removeIf(l -> l.a() == cell || l.b() == cell);
            return new Blueprint(runes, lines);
        }
        List<Blueprint.Line> lines = new ArrayList<>(bp.lines());
        if (!lines.removeIf(l -> l.path().contains(cell))) return null;
        return new Blueprint(bp.runes(), lines);
    }

    /** Adds a line from rune a to rune b along path (the cells in between, in order). Parallel lines are allowed. */
    public static Blueprint addLine(Blueprint bp, int a, int b, List<Integer> path) {
        if (a == b || !hasRune(bp, a) || !hasRune(bp, b) || path.size() > CELLS || bp.lines().size() >= CELLS) return null;
        if (path.isEmpty()) { // a direct line takes no cell, so only one per pair; more would be free redundancy
            for (Blueprint.Line l : bp.lines()) {
                if (l.path().isEmpty() && ((l.a() == a && l.b() == b) || (l.a() == b && l.b() == a))) return null;
            }
        }
        Set<Integer> taken = occupied(bp);
        Set<Integer> seen = new HashSet<>();
        int previous = a;
        for (int cell : path) {
            if (!inGrid(cell) || taken.contains(cell) || !seen.add(cell) || !adjacent(previous, cell)) return null;
            previous = cell;
        }
        if (!adjacent(previous, b)) return null;
        List<Blueprint.Line> lines = new ArrayList<>(bp.lines());
        lines.add(new Blueprint.Line(a, b, path));
        return new Blueprint(bp.runes(), lines);
    }

    private BlueprintRules() {}
}
