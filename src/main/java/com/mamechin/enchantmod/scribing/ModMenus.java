package com.mamechin.enchantmod.scribing;

import java.util.function.Supplier;

import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, EnchantMod.MODID);

    public static final Supplier<MenuType<ScribingTableMenu>> SCRIBING_TABLE =
            MENUS.register("scribing_table", () -> IMenuTypeExtension.create(ScribingTableMenu::new));

    private ModMenus() {}
}
