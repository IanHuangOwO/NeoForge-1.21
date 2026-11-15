package com.mamechin.enchantmod.scribing.rules;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The geometry of drawing a line out of a rune across the grid: where it leaves the rune and how it follows the cursor. Pure, no Minecraft. */
public final class LineRouting {
    private LineRouting() {}

    /** First and last row or column of the cells a rune covers, as {minX, minY, maxX, maxY}. */
    public static int[] bounds(Blueprint.RuneAt rune) {
        int size = BlueprintRules.sizeOf(rune.rune()), lo = -(size - 1) / 2, hi = size / 2;
        int x = rune.cell() % BlueprintRules.WIDTH, y = rune.cell() / BlueprintRules.WIDTH;
        return new int[] {x + lo, y + lo, x + hi, y + hi};
    }

    public static boolean adjacent(int a, int b) {
        return Math.abs(a % BlueprintRules.WIDTH - b % BlueprintRules.WIDTH) + Math.abs(a / BlueprintRules.WIDTH - b / BlueprintRules.WIDTH) == 1;
    }

    /** The cell just outside the rune, on the side facing the target, where a line leaves it; -1 when there is none. */
    static int firstStep(Blueprint.RuneAt rune, int target) {
        int[] b = bounds(rune);
        int tx = target % BlueprintRules.WIDTH, ty = target / BlueprintRules.WIDTH;
        int ox = tx < b[0] ? tx - b[0] : Math.max(tx - b[2], 0), oy = ty < b[1] ? ty - b[1] : Math.max(ty - b[3], 0);
        if (ox == 0 && oy == 0) return -1;
        int x, y;
        if (Math.abs(ox) >= Math.abs(oy)) {
            x = ox < 0 ? b[0] - 1 : b[2] + 1;
            y = Math.max(b[1], Math.min(b[3], ty));
        } else {
            y = oy < 0 ? b[1] - 1 : b[3] + 1;
            x = Math.max(b[0], Math.min(b[2], tx));
        }
        return x < 0 || y < 0 || x >= BlueprintRules.WIDTH || y >= BlueprintRules.HEIGHT ? -1 : y * BlueprintRules.WIDTH + x;
    }

    /**
     * Walks a line that starts at the rune at lineStart toward a target cell, one orthogonal step at a time, adding the cells to
     * path (or taking them back when the target is behind the tail); stops at anything occupied. False when there is no rune at lineStart.
     */
    public static boolean extend(Blueprint blueprint, int lineStart, List<Integer> linePath, int target) {
        return extend(blueprint, lineStart, linePath, target, TemplateLimits.FULL);
    }

    /** As above; cells outside the template's drawing area count as taken, so the line stops at its edge. */
    public static boolean extend(Blueprint blueprint, int lineStart, List<Integer> linePath, int target, TemplateLimits tier) {
        Blueprint.RuneAt start = BlueprintRules.runeAt(blueprint, lineStart);
        if (start == null) return false;
        Set<Integer> taken = new HashSet<>(BlueprintRules.occupied(blueprint));
        taken.addAll(linePath);
        for (int cell = 0; cell < BlueprintRules.CELLS; cell++) if (!tier.contains(cell)) taken.add(cell);
        for (int guard = 0; guard < BlueprintRules.CELLS; guard++) {
            int next;
            if (linePath.isEmpty()) {
                next = firstStep(start, target);
                if (next < 0) return true;
            } else {
                int tail = linePath.get(linePath.size() - 1);
                if (tail == target) return true;
                int dx = target % BlueprintRules.WIDTH - tail % BlueprintRules.WIDTH, dy = target / BlueprintRules.WIDTH - tail / BlueprintRules.WIDTH;
                next = Math.abs(dx) >= Math.abs(dy) ? tail + Integer.signum(dx) : tail + Integer.signum(dy) * BlueprintRules.WIDTH;
                int previous = linePath.size() >= 2 ? linePath.get(linePath.size() - 2) : -1;
                if (next == previous || (linePath.size() == 1 && BlueprintRules.runeAt(blueprint, next) == start)) {
                    taken.remove(linePath.remove(linePath.size() - 1)); // moving back over the line takes it back
                    continue;
                }
            }
            if (taken.contains(next)) return true;
            linePath.add(next);
            taken.add(next);
        }
        return true;
    }
}
