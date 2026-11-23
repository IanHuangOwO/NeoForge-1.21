package com.mamechin.enchantmod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * The client entry point. It only adds the config screen (Mods screen, the mod, Config); each feature registers its own client parts
 * (renderers, models, screens, overlays) in its {@code client/} folder, e.g. {@code blocks/altar/client/AltarClient}.
 */
@Mod(value = EnchantMod.MODID, dist = Dist.CLIENT)
public class EnchantModClient {
    public EnchantModClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
