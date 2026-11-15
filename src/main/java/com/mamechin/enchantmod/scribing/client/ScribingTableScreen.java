package com.mamechin.enchantmod.scribing.client;

import com.mamechin.enchantmod.scribing.research.GuiArt;
import com.mamechin.enchantmod.scribing.research.ResearchPanel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mamechin.enchantmod.items.ModItems;
import com.mamechin.enchantmod.scribing.rules.Blueprint;
import com.mamechin.enchantmod.scribing.rules.BlueprintRules;
import com.mamechin.enchantmod.scribing.rules.LineRouting;
import com.mamechin.enchantmod.scribing.EditBlueprintPayload;
import com.mamechin.enchantmod.scribing.rules.Rune;
import com.mamechin.enchantmod.scribing.ScribingTableMenu;

import com.mamechin.enchantmod.scribing.LibraryPayload;
import com.mamechin.enchantmod.scribing.RenameTemplatePayload;
import com.mamechin.enchantmod.scribing.rules.SavedTemplate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    private static final int GRID_X = 40, GRID_Y = 20, CELL = 9;
    // shortcut: the palette is 4 columns of the 24 shipped runes; a data pack with many more runes overflows it, add scrolling or tabs then
    private static final int PALETTE_X = 186, PALETTE_Y = 18, PALETTE_CELL = 14, PALETTE_COLUMNS = 4;

    private static final int LINE = 0xFFB8764A, LINE_PREVIEW = 0x80B8764A, TEXT = 0xFFE2D4AC; // lines are copper

    private static final ResourceLocation BACKGROUND = GuiArt.gui("scribing_table"), CELL_TEXTURE = GuiArt.gui("rune_cell"),
            PALETTE_SLOT = GuiArt.gui("palette_slot"), PALETTE_SELECTED = GuiArt.gui("palette_selected");
    private static final ResourceLocation[] SLOT_GHOSTS = {GuiArt.gui("slot_inkwell"), GuiArt.gui("slot_quill"), GuiArt.gui("slot_template")};

    private ResearchPanel research;                // enchant pages of the research handbook, shown while the player carries one
    private Rune held;                               // rune dragged out of the palette
    private int lineStart = -1;                      // rune cell a line is being drawn from
    private final List<Integer> linePath = new ArrayList<>();

    public ScribingTableScreen(ScribingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        research = new ResearchPanel(Minecraft.getInstance().font, true);
        imageWidth = 256;
        imageHeight = 222;
        inventoryLabelY = 130;
    }

    private EditBox nameBox;
    private Button saveButton;
    private boolean syncingName;

    @Override
    protected void init() {
        super.init();
        nameBox = new EditBox(font, leftPos + GRID_X, topPos + 7, 100, 12, Component.translatable("gui.enchantmod.scribing_table.rename"));
        nameBox.setMaxLength(SavedTemplate.NAME_MAX);
        nameBox.setHint(Component.translatable("gui.enchantmod.scribing_table.rename").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setResponder(name -> {
            if (!syncingName) PacketDistributor.sendToServer(new RenameTemplatePayload(name));
        });
        addRenderableWidget(nameBox);
        saveButton = addRenderableWidget(Button.builder(Component.translatable("gui.enchantmod.scribing_table.save"),
                button -> PacketDistributor.sendToServer(new LibraryPayload(LibraryPayload.OP_SAVE, ""))).bounds(leftPos + GRID_X + 102, topPos + 6, 33, 14).build());
    }

    /** Keeps the name box showing the template's name and switches the box and the save button on or off. */
    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack template = menu.templateStack();
        nameBox.setEditable(!template.isEmpty());
        saveButton.active = !template.isEmpty() && !menu.blueprint().runes().isEmpty() && hasHandbook();
        Component custom = template.get(DataComponents.CUSTOM_NAME);
        String wanted = custom == null ? "" : custom.getString();
        if (!nameBox.isFocused() && !nameBox.getValue().equals(wanted)) {
            syncingName = true;
            nameBox.setValue(wanted);
            syncingName = false;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox.isFocused() && keyCode != 256) { // typing a name must not trigger the inventory key
            nameBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int cellAt(double mouseX, double mouseY) {
        int x = (int) Math.floor((mouseX - leftPos - GRID_X) / CELL), y = (int) Math.floor((mouseY - topPos - GRID_Y) / CELL);
        return x < 0 || y < 0 || x >= BlueprintRules.WIDTH || y >= BlueprintRules.HEIGHT ? -1 : y * BlueprintRules.WIDTH + x;
    }

    private int paletteAt(double mouseX, double mouseY) {
        int x = (int) Math.floor((mouseX - leftPos - PALETTE_X) / PALETTE_CELL), y = (int) Math.floor((mouseY - topPos - PALETTE_Y) / PALETTE_CELL);
        int index = y * PALETTE_COLUMNS + x;
        return x < 0 || x >= PALETTE_COLUMNS || y < 0 || index >= Rune.values().size() ? -1 : index;
    }

    /** Cell centre in half pixels, so a line can be half a pixel thick. */
    private int centreX2(int cell) {
        return 2 * (leftPos + GRID_X + (cell % BlueprintRules.WIDTH) * CELL) + CELL - 1; // a cell is drawn CELL - 1 pixels wide
    }

    private int centreY2(int cell) {
        return 2 * (topPos + GRID_Y + (cell / BlueprintRules.WIDTH) * CELL) + CELL - 1;
    }

    private void send(int op, int cell, int cell2, String rune, List<Integer> path) {
        PacketDistributor.sendToServer(new EditBlueprintPayload(op, cell, cell2, rune, path));
    }

    private void cancelLine() {
        lineStart = -1;
        linePath.clear();
    }

    /** Walks the line toward the cell under the cursor; a line whose start rune has vanished is dropped. */
    private void extendLine(int mouseX, int mouseY) {
        if (lineStart < 0) return;
        int target = cellAt(mouseX, mouseY);
        if (target >= 0 && !LineRouting.extend(menu.blueprint(), lineStart, linePath, target)) cancelLine();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        extendLine(mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (held != null) {
            int cell = cellAt(mouseX, mouseY);
            if (cell >= 0) {
                Set<Integer> cells = BlueprintRules.footprint(cell, held.size());
                Blueprint placed = BlueprintRules.place(menu.blueprint(), cell, held.id());
                boolean fine = placed != null && BlueprintRules.cost(menu.blueprint(), placed) <= menu.ink();
                int color = fine ? 0x6040C040 : 0x60C04040;
                for (int c : cells == null ? Set.of(cell) : cells) {
                    int x = leftPos + GRID_X + (c % BlueprintRules.WIDTH) * CELL, y = topPos + GRID_Y + (c / BlueprintRules.WIDTH) * CELL;
                    graphics.fill(x, y, x + CELL - 1, y + CELL - 1, color);
                }
            }
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);
            GuiArt.glyph(graphics, font, held, mouseX, mouseY, GuiArt.glyphScale(held));
            graphics.pose().popPose();
        }
        Rune hovered = runeUnder(mouseX, mouseY);
        if (hovered != null) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable(hovered.langKey()),
                    Component.translatable(hovered.langKey() + ".desc").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
        if (hasHandbook()) research.renderTooltip(graphics, mouseX, mouseY);
    }

    /** The rune in the palette or on the grid under the cursor, or null. */
    private Rune runeUnder(int mouseX, int mouseY) {
        int palette = paletteAt(mouseX, mouseY);
        if (palette >= 0) return Rune.values().get(palette);
        int cell = cellAt(mouseX, mouseY);
        if (cell < 0) return null;
        Blueprint.RuneAt placed = BlueprintRules.runeAt(menu.blueprint(), cell);
        return placed == null ? null : Rune.byId(placed.rune());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (hasHandbook()) research.render(graphics, leftPos - ResearchPanel.WIDTH - 2, topPos, imageHeight, mouseX, mouseY);
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        for (int i = 0; i < SLOT_GHOSTS.length; i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.hasItem()) graphics.blit(SLOT_GHOSTS[i], leftPos + slot.x, topPos + slot.y, 0, 0, 16, 16, 16, 16);
        }

        Blueprint blueprint = menu.blueprint();
        Set<Integer> runeCells = BlueprintRules.runeCells(blueprint);
        for (int cell = 0; cell < BlueprintRules.CELLS; cell++) {
            if (!runeCells.contains(cell)) blitCell(graphics, CELL_TEXTURE, cell);
        }
        for (Blueprint.RuneAt placed : blueprint.runes()) {
            Rune rune = Rune.byId(placed.rune());
            if (rune == null) continue;
            int[] b = LineRouting.bounds(placed);
            int full = rune.size() * CELL, side = full - 1; // the board texture ends in a 1 px gap
            int x0 = leftPos + GRID_X + b[0] * CELL, y0 = topPos + GRID_Y + b[1] * CELL;
            graphics.blit(GuiArt.board(rune.size()), x0, y0, 0, 0, full, full, full, full);
            GuiArt.glyph(graphics, font, rune, x0 + side / 2.0F, y0 + side / 2.0F, GuiArt.glyphScale(rune));
        }
        for (Blueprint.Line line : blueprint.lines()) drawLine(graphics, line.a(), line.path(), line.b(), LINE);
        if (lineStart >= 0) drawLine(graphics, lineStart, linePath, -1, LINE_PREVIEW);

        List<Rune> runes = Rune.values();
        for (int i = 0; i < runes.size(); i++) {
            int x = leftPos + PALETTE_X + (i % PALETTE_COLUMNS) * PALETTE_CELL, y = topPos + PALETTE_Y + (i / PALETTE_COLUMNS) * PALETTE_CELL;
            graphics.blit(PALETTE_SLOT, x, y, 0, 0, PALETTE_CELL, PALETTE_CELL, PALETTE_CELL, PALETTE_CELL);
            if (i == paletteAt(mouseX, mouseY)) graphics.blit(PALETTE_SELECTED, x, y, 0, 0, PALETTE_CELL, PALETTE_CELL, PALETTE_CELL, PALETTE_CELL);
            GuiArt.glyph(graphics, font, runes.get(i), x + PALETTE_CELL / 2.0F, y + PALETTE_CELL / 2.0F, 1);
        }
    }

    private void blitCell(GuiGraphics graphics, ResourceLocation texture, int cell) {
        graphics.blit(texture, leftPos + GRID_X + (cell % BlueprintRules.WIDTH) * CELL, topPos + GRID_Y + (cell / BlueprintRules.WIDTH) * CELL, 0, 0, CELL, CELL, CELL, CELL);
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

    /** Draws a line two and a half pixels thick from the edge of rune a along the path cells to the edge of rune b (or open ended when b is -1). */
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
            graphics.fill(Math.min(p[0], q[0]) - 2, Math.min(p[1], q[1]) - 2, Math.max(p[0], q[0]) + 3, Math.max(p[1], q[1]) + 3, color); // 5 half pixels thick, centred
        }
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, playerInventoryTitle, 47, inventoryLabelY, TEXT, false);
        if (!menu.canEdit()) {
            Component hint = Component.translatable("gui.enchantmod.scribing_table.need_tools");
            graphics.drawString(font, hint, imageWidth - 8 - font.width(hint), inventoryLabelY, 0xFFD98A6A, false);
        } else if (menu.ink() == 0) {
            Component dry = Component.translatable("gui.enchantmod.scribing_table.no_ink");
            graphics.drawString(font, dry, imageWidth - 8 - font.width(dry), inventoryLabelY, 0xFFD98A6A, false);
        }
    }

    private boolean hasHandbook() {
        return minecraft != null && minecraft.player != null && minecraft.player.getInventory().hasAnyMatching(stack -> stack.is(ModItems.RESEARCH_HANDBOOK.get()));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hasHandbook() && research.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hasHandbook() && research.mouseClicked(mouseX, mouseY, button)) return true;
        if (lineStart >= 0) {
            int cell = cellAt(mouseX, mouseY);
            Blueprint.RuneAt end = cell < 0 ? null : BlueprintRules.runeAt(menu.blueprint(), cell);
            if (button == 0 && end != null && end.cell() != lineStart && !linePath.isEmpty() && LineRouting.adjacent(linePath.get(linePath.size() - 1), cell)) {
                send(ScribingTableMenu.OP_LINE, lineStart, end.cell(), "", new ArrayList<>(linePath));
            }
            cancelLine();
            return true;
        }
        if (menu.canEdit()) {
            int palette = paletteAt(mouseX, mouseY);
            if (button == 0 && palette >= 0) {
                held = Rune.values().get(palette);
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
