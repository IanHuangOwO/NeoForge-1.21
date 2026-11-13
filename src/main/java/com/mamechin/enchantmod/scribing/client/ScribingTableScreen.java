package com.mamechin.enchantmod.scribing.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mamechin.enchantmod.scribing.Blueprint;
import com.mamechin.enchantmod.scribing.BlueprintRules;
import com.mamechin.enchantmod.scribing.EditBlueprintPayload;
import com.mamechin.enchantmod.scribing.Rune;
import com.mamechin.enchantmod.scribing.ScribingTableMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The scribing table: a rune grid on the left of a palette. Drag a rune from the palette onto the grid (core runes cover
 * 3x3 cells, stabilizers 2x2, the boost rune 1; runes may not touch); click a placed rune to start a line that follows the
 * cursor along the outside of the runes, click another rune to connect it, right-click to cancel. Right-click a rune to
 * erase it with its lines, right-click a line to erase that line. Every edit goes to the
 * server, which checks it and stores the result on the template item; the screen only ever draws what the item holds.
 */
public class ScribingTableScreen extends AbstractContainerScreen<ScribingTableMenu> {
    private static final int GRID_X = 34, GRID_Y = 18, CELL = 9;
    private static final int PALETTE_X = 186, PALETTE_Y = 18, PALETTE_CELL = 14, PALETTE_COLUMNS = 4;

    private static final int PANEL = 0xFF2B1D12, PANEL_EDGE = 0xFF8A5A2B, SLOT = 0xFF120C07;
    private static final int GRID = 0xFF3A2918, GRID_RUNE = 0xFF5C3F20, GLYPH = 0xFFFFE8B0, BOOST_GLYPH = 0xFFFF6B5E, BOOST_BG = 0xFF5A2222, STABILIZER_GLYPH = 0xFF6BA8FF, STABILIZER_BG = 0xFF1F3560, LINE = GLYPH, LINE_PREVIEW = 0x80FFE8B0;

    private Rune held;                               // rune dragged out of the palette
    private int lineStart = -1;                      // rune cell a line is being drawn from
    private final List<Integer> linePath = new ArrayList<>();

    public ScribingTableScreen(ScribingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 222;
        inventoryLabelY = 130;
    }

    private int cellAt(double mouseX, double mouseY) {
        int x = (int) Math.floor((mouseX - leftPos - GRID_X) / CELL), y = (int) Math.floor((mouseY - topPos - GRID_Y) / CELL);
        return x < 0 || y < 0 || x >= BlueprintRules.WIDTH || y >= BlueprintRules.HEIGHT ? -1 : y * BlueprintRules.WIDTH + x;
    }

    private int paletteAt(double mouseX, double mouseY) {
        int x = (int) Math.floor((mouseX - leftPos - PALETTE_X) / PALETTE_CELL), y = (int) Math.floor((mouseY - topPos - PALETTE_Y) / PALETTE_CELL);
        int index = y * PALETTE_COLUMNS + x;
        return x < 0 || x >= PALETTE_COLUMNS || y < 0 || index >= Rune.values().length ? -1 : index;
    }

    private static int glyphColor(Rune rune) {
        return rune == Rune.URUZ ? BOOST_GLYPH : rune.isUtility() ? STABILIZER_GLYPH : GLYPH;
    }

    private static int runeBackground(Rune rune) {
        return rune == Rune.URUZ ? BOOST_BG : rune.isUtility() ? STABILIZER_BG : GRID_RUNE;
    }

    /** Cell centre in half pixels, so a line can be half a pixel thick. */
    private int centreX2(int cell) {
        return 2 * (leftPos + GRID_X + (cell % BlueprintRules.WIDTH) * CELL) + CELL - 1; // a cell is drawn CELL - 1 pixels wide
    }

    private int centreY2(int cell) {
        return 2 * (topPos + GRID_Y + (cell / BlueprintRules.WIDTH) * CELL) + CELL - 1;
    }

    /** First and last row or column of the cells a rune covers, as {minX, minY, maxX, maxY}. */
    private static int[] bounds(Blueprint.RuneAt rune) {
        int size = BlueprintRules.sizeOf(rune.rune()), lo = -(size - 1) / 2, hi = size / 2;
        int x = rune.cell() % BlueprintRules.WIDTH, y = rune.cell() / BlueprintRules.WIDTH;
        return new int[] {x + lo, y + lo, x + hi, y + hi};
    }

    private static boolean adjacent(int a, int b) {
        return Math.abs(a % BlueprintRules.WIDTH - b % BlueprintRules.WIDTH) + Math.abs(a / BlueprintRules.WIDTH - b / BlueprintRules.WIDTH) == 1;
    }

    private void send(int op, int cell, int cell2, String rune, List<Integer> path) {
        PacketDistributor.sendToServer(new EditBlueprintPayload(op, cell, cell2, rune, path));
    }

    private void cancelLine() {
        lineStart = -1;
        linePath.clear();
    }

    /** The cell just outside the rune, on the side facing the target, where a line leaves it; -1 when there is none. */
    private static int firstStep(Blueprint.RuneAt rune, int target) {
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

    /** Walks the line toward the cell under the cursor, one orthogonal step at a time; stops at anything occupied. */
    private void extendLine(int mouseX, int mouseY) {
        if (lineStart < 0) return;
        int target = cellAt(mouseX, mouseY);
        if (target < 0) return;
        Blueprint.RuneAt start = BlueprintRules.runeAt(menu.blueprint(), lineStart);
        if (start == null) {
            cancelLine();
            return;
        }
        Set<Integer> taken = new HashSet<>(BlueprintRules.occupied(menu.blueprint()));
        taken.addAll(linePath);
        for (int guard = 0; guard < BlueprintRules.CELLS; guard++) {
            int next;
            if (linePath.isEmpty()) {
                next = firstStep(start, target);
                if (next < 0) return;
            } else {
                int tail = linePath.get(linePath.size() - 1);
                if (tail == target) return;
                int dx = target % BlueprintRules.WIDTH - tail % BlueprintRules.WIDTH, dy = target / BlueprintRules.WIDTH - tail / BlueprintRules.WIDTH;
                next = Math.abs(dx) >= Math.abs(dy) ? tail + Integer.signum(dx) : tail + Integer.signum(dy) * BlueprintRules.WIDTH;
                int previous = linePath.size() >= 2 ? linePath.get(linePath.size() - 2) : -1;
                if (next == previous || (linePath.size() == 1 && BlueprintRules.runeAt(menu.blueprint(), next) == start)) {
                    taken.remove(linePath.remove(linePath.size() - 1)); // moving back over the line takes it back
                    continue;
                }
            }
            if (taken.contains(next)) return;
            linePath.add(next);
            taken.add(next);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        extendLine(mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (held != null) {
            int cell = cellAt(mouseX, mouseY);
            if (cell >= 0) {
                Set<Integer> cells = BlueprintRules.footprint(cell, held.size());
                int color = BlueprintRules.place(menu.blueprint(), cell, held.id()) != null ? 0x6040C040 : 0x60C04040;
                for (int c : cells == null ? Set.of(cell) : cells) {
                    int x = leftPos + GRID_X + (c % BlueprintRules.WIDTH) * CELL, y = topPos + GRID_Y + (c / BlueprintRules.WIDTH) * CELL;
                    graphics.fill(x, y, x + CELL - 1, y + CELL - 1, color);
                }
            }
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);
            drawGlyph(graphics, held, mouseX, mouseY, held.size());
            graphics.pose().popPose();
        }
        Rune hovered = runeUnder(mouseX, mouseY);
        if (hovered != null) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("rune.enchantmod." + hovered.id()),
                    Component.translatable("rune.enchantmod." + hovered.id() + ".desc").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    /** The rune in the palette or on the grid under the cursor, or null. */
    private Rune runeUnder(int mouseX, int mouseY) {
        int palette = paletteAt(mouseX, mouseY);
        if (palette >= 0) return Rune.values()[palette];
        int cell = cellAt(mouseX, mouseY);
        if (cell < 0) return null;
        Blueprint.RuneAt placed = BlueprintRules.runeAt(menu.blueprint(), cell);
        return placed == null ? null : Rune.byId(placed.rune());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_EDGE);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, PANEL);
        for (Slot slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, SLOT);
        }

        Blueprint blueprint = menu.blueprint();
        Set<Integer> runeCells = BlueprintRules.runeCells(blueprint);
        for (int cell = 0; cell < BlueprintRules.CELLS; cell++) {
            int x = leftPos + GRID_X + (cell % BlueprintRules.WIDTH) * CELL, y = topPos + GRID_Y + (cell / BlueprintRules.WIDTH) * CELL;
            graphics.fill(x, y, x + CELL - 1, y + CELL - 1, runeCells.contains(cell) ? GRID_RUNE : GRID);
        }
        for (Blueprint.RuneAt placed : blueprint.runes()) {
            Rune rune = Rune.byId(placed.rune());
            if (rune == null) continue;
            int[] b = bounds(placed);
            int x0 = leftPos + GRID_X + b[0] * CELL, y0 = topPos + GRID_Y + b[1] * CELL, side = rune.size() * CELL - 1;
            graphics.fill(x0, y0, x0 + side, y0 + side, runeBackground(rune));
            drawGlyph(graphics, rune, x0 + side / 2.0F, y0 + side / 2.0F, rune.size());
        }
        for (Blueprint.Line line : blueprint.lines()) drawLine(graphics, line.a(), line.path(), line.b(), LINE);
        if (lineStart >= 0) drawLine(graphics, lineStart, linePath, -1, LINE_PREVIEW);

        Rune[] runes = Rune.values();
        for (int i = 0; i < runes.length; i++) {
            int x = leftPos + PALETTE_X + (i % PALETTE_COLUMNS) * PALETTE_CELL, y = topPos + PALETTE_Y + (i / PALETTE_COLUMNS) * PALETTE_CELL;
            graphics.fill(x, y, x + PALETTE_CELL - 1, y + PALETTE_CELL - 1, GRID);
            drawGlyph(graphics, runes[i], x + PALETTE_CELL / 2.0F, y + PALETTE_CELL / 2.0F, 1);
        }
    }

    /** The point on the edge of a rune where a line cell next to it meets it, in half pixels. */
    private int[] edgePoint(int anchor, int cell) {
        Blueprint.RuneAt rune = BlueprintRules.runeAt(menu.blueprint(), anchor);
        int x = centreX2(cell), y = centreY2(cell);
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] step : steps) {
            int nx = cell % BlueprintRules.WIDTH + step[0], ny = cell / BlueprintRules.WIDTH + step[1];
            if (nx < 0 || ny < 0 || nx >= BlueprintRules.WIDTH || ny >= BlueprintRules.HEIGHT) continue;
            if (rune != null && BlueprintRules.runeAt(menu.blueprint(), ny * BlueprintRules.WIDTH + nx) == rune) return new int[] {x + step[0] * CELL, y + step[1] * CELL};
        }
        return new int[] {x, y};
    }

    /** Draws a line half a pixel thick from the edge of rune a along the path cells to the edge of rune b (or open ended when b is -1). */
    private void drawLine(GuiGraphics graphics, int a, List<Integer> path, int b, int color) {
        if (path.isEmpty()) return;
        List<int[]> points = new ArrayList<>();
        points.add(edgePoint(a, path.get(0)));
        for (int cell : path) points.add(new int[] {centreX2(cell), centreY2(cell)});
        if (b >= 0) points.add(edgePoint(b, path.get(path.size() - 1)));
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        for (int i = 0; i + 1 < points.size(); i++) {
            int[] p = points.get(i), q = points.get(i + 1);
            graphics.fill(Math.min(p[0], q[0]), Math.min(p[1], q[1]), Math.max(p[0], q[0]) + 1, Math.max(p[1], q[1]) + 1, color);
        }
        graphics.pose().popPose();
    }

    private void drawGlyph(GuiGraphics graphics, Rune rune, float centreX, float centreY, float scale) {
        String text = String.valueOf(rune.glyph());
        graphics.pose().pushPose();
        graphics.pose().translate(centreX, centreY, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-font.width(text) / 2.0F, -4.0F, 0); // fractional, so scaled glyphs stay centred
        graphics.drawString(font, text, 0, 0, glyphColor(rune), false);
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFE8C98A, false);
        graphics.drawString(font, playerInventoryTitle, 47, inventoryLabelY, 0xFFE8C98A, false);
        if (!menu.canEdit()) {
            graphics.drawString(font, Component.translatable("gui.enchantmod.scribing_table.need_tools"), GRID_X, GRID_Y + BlueprintRules.HEIGHT * CELL + 3, 0xFFB86B4B, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (lineStart >= 0) {
            int cell = cellAt(mouseX, mouseY);
            Blueprint.RuneAt end = cell < 0 ? null : BlueprintRules.runeAt(menu.blueprint(), cell);
            if (button == 0 && end != null && end.cell() != lineStart && !linePath.isEmpty() && adjacent(linePath.get(linePath.size() - 1), cell)) {
                send(ScribingTableMenu.OP_LINE, lineStart, end.cell(), "", new ArrayList<>(linePath));
            }
            cancelLine();
            return true;
        }
        if (menu.canEdit()) {
            int palette = paletteAt(mouseX, mouseY);
            if (button == 0 && palette >= 0) {
                held = Rune.values()[palette];
                return true;
            }
            int cell = cellAt(mouseX, mouseY);
            if (cell >= 0) {
                Blueprint.RuneAt rune = BlueprintRules.runeAt(menu.blueprint(), cell);
                if (button == 0 && rune != null) {
                    lineStart = rune.cell();
                } else if (button == 1) {
                    send(ScribingTableMenu.OP_ERASE, cell, 0, "", List.of());
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (held != null) {
            int cell = cellAt(mouseX, mouseY);
            if (cell >= 0) send(ScribingTableMenu.OP_PLACE, cell, 0, held.id(), List.of());
            held = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
