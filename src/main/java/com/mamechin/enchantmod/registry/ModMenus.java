package com.mamechin.enchantmod.registry;

import com.mamechin.enchantmod.blocks.scribing.ScribingTableMenu;
import java.util.function.Supplier;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.blocks.press.PrintingPressMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, EnchantMod.MODID);

    public static final Supplier<MenuType<ScribingTableMenu>> SCRIBING_TABLE =
            MENUS.register("scribing_table", () -> IMenuTypeExtension.create(ScribingTableMenu::new));

    public static final Supplier<MenuType<PrintingPressMenu>> PRINTING_PRESS =
            MENUS.register("printing_press", () -> IMenuTypeExtension.create(PrintingPressMenu::new));

    public static final Supplier<MenuType<com.mamechin.enchantmod.multiblock.sphere.SphereMenu>> SPHERE =
            MENUS.register("sphere", () -> IMenuTypeExtension.create(com.mamechin.enchantmod.multiblock.sphere.SphereMenu::new));

    private ModMenus() {}
}
