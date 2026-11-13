package com.mamechin.enchantmod.scribing.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The research tablet's pages on their own, opened by using the tablet away from a scribing table. */
public class ResearchScreen extends Screen {
    private static final int HEIGHT = 170;
    private final ResearchPanel panel;

    public ResearchScreen() {
        super(Component.translatable("item.enchantmod.research_tablet"));
        panel = new ResearchPanel(Minecraft.getInstance().font);
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new ResearchScreen());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        panel.render(graphics, (width - ResearchPanel.WIDTH) / 2, (height - HEIGHT) / 2, HEIGHT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return panel.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return panel.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
