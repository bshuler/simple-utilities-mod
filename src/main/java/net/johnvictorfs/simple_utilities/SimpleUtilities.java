package net.johnvictorfs.simple_utilities;

import java.io.File;

import net.johnvictorfs.simple_utilities.config.SimpleUtilitiesConfig;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.johnvictorfs.simple_utilities.util.TextCompat;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
//?} elif neoforge {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
*///?} elif forge {
/*import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
*///?}

/**
 * Mod entry point. On Fabric this loads the vendored config, registers the
 * "toggle HUD" keybinding, and wires the on/off chat message. NeoForge and
 * Forge builds currently only load config on client setup - the HUD-overlay
 * mixin (Yarn-mapped internals) has not been ported to Mojang-mapped
 * targets yet, see PLAN.md. This mirrors the exact pattern
 * EasierVillagerTrading uses for its own Fabric-first, Forge/NeoForge-stub
 * split.
 */
//? if neoforge {
/*@Mod(SimpleUtilities.MODID)
*///?} elif forge {
/*@Mod(SimpleUtilities.MODID)
*///?}
public class SimpleUtilities
//? if fabric {
implements ClientModInitializer
//?}
{

    public static final String MODID = "simple_utilities";

    //? if fabric {
    @Override
    public void onInitializeClient() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        SimpleUtilitiesConfig.getInstance(configDir);

        registerKeybinding(configDir);
    }

    private void registerKeybinding(File configDir) {
        KeyBinding toggleHudKey = new KeyBinding(
                "key.simple_utilities.toggle_hud",
                GLFW.GLFW_KEY_K,
                "key.category.simple_utilities.hud"
        );

        KeyBinding toggleHudKeybinding = KeyBindingHelper.registerKeyBinding(toggleHudKey);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (toggleHudKeybinding.wasPressed()) {
                SimpleUtilitiesConfig config = SimpleUtilitiesConfig.getInstance(configDir);

                String chatMessageKey = config.toggleSimpleUtilitiesHUD
                        ? "key.simple_utilities.toggle_hud.chat_message.off"
                        : "key.simple_utilities.toggle_hud.chat_message.on";

                client.player.sendMessage(TextCompat.translatable(chatMessageKey), true);
                config.toggleSimpleUtilitiesHUD = !config.toggleSimpleUtilitiesHUD;
                config.save(configDir);
            }
        });
    }
    //?} elif neoforge {
    /*public SimpleUtilities(IEventBus modEventBus) {
        modEventBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        SimpleUtilitiesConfig.getInstance(FMLPaths.CONFIGDIR.get().toFile());
    }
    *///?} elif forge {
    /*public SimpleUtilities() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        SimpleUtilitiesConfig.getInstance(FMLPaths.CONFIGDIR.get().toFile());
    }
    *///?}
}
