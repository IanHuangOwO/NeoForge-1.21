package com.mamechin.enchantmod.wheel;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the parts of the spinning wheel sit, as offsets from the core (pure, no Minecraft). The wheel is an upright ring of frame
 * blocks 5 across round the core, plus the four blocks touching the core ({@link #PATTERN}); its plane is at right angles to the core's facing, with a smooth basalt pillar
 * on each side from the ground up to the core's height ({@link #pillars}), 7 across in all. {@code facingX} is true when the core
 * faces east or west, so the wheel's plane runs along Z. (A 9-wide ring with 8 beams is kept for a later tier: see the design doc.)
 */
public final class WheelLayout {
    public static final int RADIUS = 2;
    /** The altar stands this many blocks out on the facing side, on the bottom row ({@code -RADIUS}); with a formed wheel there it is tier 2. */
    public static final int PEDESTAL_DISTANCE = 3;
    /** Front view, top row first; '#' is a frame block, 'C' the core. Symmetric through the core, which {@link #bodies} relies on. */
    static final String[] PATTERN = {
            ".###.",
            "#.#.#",
            "##C##",
            "#.#.#",
            ".###.",
    };

    private WheelLayout() {}

    /** The 10 pillar offsets {dx, dy, dz} from the core: one block out from each side of the ring, bottom row up to the core's row. */
    public static List<int[]> pillars(boolean facingX) {
        List<int[]> list = new ArrayList<>();
        for (int along : new int[] {-RADIUS - 1, RADIUS + 1}) {
            for (int dy = -RADIUS; dy <= 0; dy++) list.add(facingX ? new int[] {0, dy, along} : new int[] {along, dy, 0});
        }
        return list;
    }

    /** True when the offset (dx, dy, dz) from the core is one of the frame positions. */
    public static boolean isBody(int dx, int dy, int dz, boolean facingX) {
        int across = facingX ? dx : dz, along = facingX ? dz : dx;
        return across == 0 && Math.abs(along) <= RADIUS && Math.abs(dy) <= RADIUS && PATTERN[RADIUS - dy].charAt(along + RADIUS) == '#';
    }

    /** The frame offsets {dx, dy, dz} from the core. Symmetric, so their negations are the offsets from a frame to its core. */
    public static List<int[]> bodies(boolean facingX) {
        List<int[]> list = new ArrayList<>();
        for (int along = -RADIUS; along <= RADIUS; along++) {
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                if (PATTERN[RADIUS - dy].charAt(along + RADIUS) == '#') list.add(facingX ? new int[] {0, dy, along} : new int[] {along, dy, 0});
            }
        }
        return list;
    }
}
