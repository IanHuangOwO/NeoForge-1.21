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
 * The scribing table: a rune grid on the left of a palette. Drag a rune from the palette onto the grid; click a placed
 * rune to start a line that follows the cursor one cell at a time, click another rune to connect it, right-click to
 * cancel. Right-click a rune to erase it with its lines, right-click a line to erase that line. Every edit goes to the
 * server, which checks it and stores the result on the template item; the screen only ever draws what the item holds.
 */
public class ScribingTableScreen extends AbstractContainerScreen<ScribingTableMenu> {
    private static final int GRID_X = 34, GRID_Y = 18, CELL = 12;
    private static final int PALETTE_X = 186, PALETTE_Y = 18, PALETTE_CELL = 14, PALETTE_COLUMNS = 4;

    private static final int PANEL = 0xFF2B1D12, PANEL_EDGE = 0xFF8A5A2B, SLOT = 0xFF120C07;
    private static final int GRID = 0xFF3A2918, GRID_RUNE = 0xFF5C3F20, GLYPH = 0xFFFFE8B0, LINE = GLYPH, LINE_PREVIEW = 0x80FFE8B0;

    private Rune held;                               // rune dragged out of the palette
    private int lineStart = -1;                      // rune cell a line is being drawn from
    private final List<Integer> linePath = new ArrayList<>();

    public ScribingTableScreen(ScribingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 222;
        inventoryLabelY = 128;
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

    private int cellCentreX(int cell) {
        return leftPos + GRID_X + (cell % BlueprintRules.WIDTH) * CELL + CELL / 2;
    }

    private int cellCentreY(int cell) {
        return topPos + GRID_Y + (cell / BlueprintRules.WIDTH) * CELL + CELL / 2;
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

    /** Walks the line toward the cell under the cursor, one orthogonal step at a time; stops at anything occupied. */
    private void extendLine(int mouseX, int mouseY) {
        if (lineStart < 0) return;
        int target = cellAt(mouseX, mouseY);
        if (target < 0) return;
        Set<Integer> taken = new HashSet<>(BlueprintRules.occupied(menu.blueprint()));
        taken.addAll(linePath);
        for (int guard = 0; guard < BlueprintRules.CELLS; guard++) {
            int tail = linePath.isEmpty() ? lineStart : linePath.get(linePath.size() - 1);
            if (tail == target) return;
            int dx = target % BlueprintRules.WIDTH - tail % BlueprintRules.WIDTH, dy = target / BlueprintRules.WIDTH - tail / BlueprintRules.WIDTH;
            int next = Math.abs(dx) >= Math.abs(dy) ? tail + Integer.signum(dx) : tail + Integer.signum(dy) * BlueprintRules.WIDTH;
            int previous = linePath.size() >= 2 ? linePath.get(linePath.size() - 2) : lineStart;
            if (!linePath.isEmpty() && next == previous) {
                linePath.remove(linePath.size() - 1); // moving back over the line takes it back
            } else if (taken.contains(next) || next == lineStart) {
                return;
            } else {
                linePath.add(next);
                taken.add(next);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        extendLine(mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (held != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);
            graphics.drawString(font, String.valueOf(held.glyph()), mouseX - 3, mouseY - 4, GLYPH, true);
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
        for (Blueprint.RuneAt placed : menu.blueprint().runes()) {
            if (placed.cell() == cell) return Rune.byId(placed.rune());
        }
        return null;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_EDGE);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, PANEL);
        for (Slot slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, SLOT);
        }

        Blueprint blueprint = menu.blueprint();
        Set<Integer> runeCells = new HashSet<>();
        blueprint.runes().forEach(r -> runeCells.add(r.cell()));
        for (int cell = 0; cell < BlueprintRules.CELLS; cell++) {
            int x = leftPos + GRID_X + (cell % BlueprintRules.WIDTH) * CELL, y = topPos + GRID_Y + (cell / BlueprintRules.WIDTH) * CELL;
            graphics.fill(x, y, x + CELL - 1, y + CELL - 1, runeCells.contains(cell) ? GRID_RUNE : GRID);
        }

        for (Blueprint.Line line : blueprint.lines()) {
            List<Integer> cells = new ArrayList<>();
            cells.add(line.a());
            cells.addAll(line.path());
            cells.add(line.b());
            drawLine(graphics, cells, LINE);
        }
        if (lineStart >= 0) {
            List<Integer> cells = new ArrayList<>();
            cells.add(lineStart);
            cells.addAll(linePath);
            drawLine(graphics, cells, LINE_PREVIEW);
        }
        for (Blueprint.RuneAt placed : blueprint.runes()) {
            Rune rune = Rune.byId(placed.rune());
            if (rune != null) drawGlyph(graphics, rune, cellCentreX(placed.cell()), cellCentreY(placed.cell()));
        }

        Rune[] runes = Rune.values();
        for (int i = 0; i < runes.length; i++) {
            int x = leftPos + PALETTE_X + (i % PALETTE_COLUMNS) * PALETTE_CELL, y = topPos + PALETTE_Y + (i / PALETTE_COLUMNS) * PALETTE_CELL;
            graphics.fill(x, y, x + PALETTE_CELL - 1, y + PALETTE_CELL - 1, GRID);
            drawGlyph(graphics, runes[i], x + PALETTE_CELL / 2, y + PALETTE_CELL / 2);
        }
    }

    private void drawLine(GuiGraphics graphics, List<Integer> cells, int color) {
        for (int i = 0; i + 1 < cells.size(); i++) {
            int x1 = cellCentreX(cells.get(i)), y1 = cellCentreY(cells.get(i)), x2 = cellCentreX(cells.get(i + 1)), y2 = cellCentreY(cells.get(i + 1));
            graphics.fill(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, color);
        }
    }

    private void drawGlyph(GuiGraphics graphics, Rune rune, int centreX, int centreY) {
        String text = String.valueOf(rune.glyph());
        graphics.drawString(font, text, centreX - font.width(text) / 2, centreY - 4, GLYPH, false);
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
            int tail = linePath.isEmpty() ? lineStart : linePath.get(linePath.size() - 1);
            if (button == 0 && cell >= 0 && cell != lineStart && BlueprintRules.hasRune(menu.blueprint(), cell) && adjacent(tail, cell)) {
                send(ScribingTableMenu.OP_LINE, lineStart, cell, "", new ArrayList<>(linePath));
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
                if (button == 0 && BlueprintRules.hasRune(menu.blueprint(), cell)) {
                    lineStart = cell;
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
