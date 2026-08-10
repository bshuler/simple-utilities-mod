package net.johnvictorfs.simple_utilities;

import java.io.File;

import net.johnvictorfs.simple_utilities.config.SimpleUtilitiesConfig;
import net.johnvictorfs.simple_utilities.hud.HudHooks;
import net.johnvictorfs.simple_utilities.util.TextCompat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

//? if >=26.1 {
/*import net.minecraft.resources.Identifier;
*///?}

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//?} elif neoforge {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
*///?} elif forge && <1.19 {
/*import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
*///?} elif forge {
/*import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
*///?}

//? if fabric && >=26.1 {
/*import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
*///?} elif fabric {
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
//?}

/**
 * Mod entry point. Loads the vendored config, registers the "toggle HUD"
 * keybinding (K by default) plus its on/off chat message, and hands off
 * HUD-render-hook registration to {@link HudHooks} - on EVERY loader now
 * (Fabric, NeoForge, Forge), not just Fabric&lt;26.1 the way earlier passes
 * left it. Mirrors the sibling FlightHud mod's {@code FlightHud.java} entry
 * point almost line-for-line - same version×loader matrix (Fabric+Forge at
 * 1.18.2/1.19.4/1.20.1, Fabric+NeoForge at 1.21.4/26.2), same proven
 * keybinding-registration shape per era:
 * <ul>
 *   <li>Fabric: {@code ClientModInitializer}, keybinding via
 *       {@code KeyBindingHelper} (&lt;26.1) / {@code KeyMappingHelper}
 *       (&gt;=26.1, package moved), tick-polled via
 *       {@code ClientTickEvents.END_CLIENT_TICK}.</li>
 *   <li>NeoForge: constructor takes {@code IEventBus}, keybinding via
 *       {@code RegisterKeyMappingsEvent} on the mod bus, tick-polled via
 *       {@code ClientTickEvent.Post} on {@code NeoForge.EVENT_BUS}.</li>
 *   <li>Forge&lt;1.19: keybinding via the imperative
 *       {@code ClientRegistry.registerKeyBinding}, tick-polled via
 *       {@code TickEvent.ClientTickEvent} on {@code MinecraftForge.EVENT_BUS}.</li>
 *   <li>Forge&gt;=1.19: same tick-polling as Forge&lt;1.19, but keybinding via
 *       the event-based {@code RegisterKeyMappingsEvent} (mirrors NeoForge)
 *       and HUD registration via {@code HudHooks::registerOverlays} wired
 *       onto the mod event bus (HudHooks' imperative {@code register()} is a
 *       no-op for this era - see its class doc).</li>
 * </ul>
 *
 * <p>{@code KeyMapping}'s constructor takes a plain {@code String} category
 * through 1.21.4, and a {@code KeyMapping.Category} (built from a namespaced
 * {@code Identifier} via {@code KeyMapping.Category.register(...)}) from
 * 26.1 on - confirmed via {@code javap} against the real 26.2 jar, same
 * boundary this mod already uses elsewhere (see {@code TextCompat}/
 * {@code HudHooks}/{@code PlatformCanvas}). {@code Player.displayClientMessage
 * (Component, boolean)} was likewise split at 26.1 into
 * {@code sendSystemMessage(Component)}/{@code sendOverlayMessage(Component)}
 * - this mod only ever sent the "overlay" (action-bar) variant, so that's the
 * only 26.1+ replacement needed.
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

    private static KeyMapping toggleHudKey;
    private static File configDir;

    //? if fabric {
    @Override
    public void onInitializeClient() {
        configDir = FabricLoader.getInstance().getConfigDir().toFile();
        SimpleUtilitiesConfig.getInstance(configDir);

        setupKeyCode();
        ClientTickEvents.END_CLIENT_TICK.register(client -> onClientTick());

        HudHooks.register();
    }
    //?} elif neoforge {
    /*public SimpleUtilities(IEventBus modEventBus) {
        configDir = FMLPaths.CONFIGDIR.get().toFile();
        SimpleUtilitiesConfig.getInstance(configDir);

        modEventBus.addListener(this::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> onClientTick());

        HudHooks.register();
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        setupKeyCode();
        event.register(toggleHudKey);
    }
    *///?} elif forge && <1.19 {
    /*public SimpleUtilities() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        configDir = FMLPaths.CONFIGDIR.get().toFile();
        SimpleUtilitiesConfig.getInstance(configDir);

        setupKeyCode();
        ClientRegistry.registerKeyBinding(toggleHudKey);
        MinecraftForge.EVENT_BUS.addListener(this::onClientTickForge);

        HudHooks.register();
    }

    private void onClientTickForge(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            onClientTick();
        }
    }
    *///?} elif forge {
    /*public SimpleUtilities() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRegisterKeyMappings);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(HudHooks::registerOverlays);
        MinecraftForge.EVENT_BUS.addListener(this::onClientTickForge);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        configDir = FMLPaths.CONFIGDIR.get().toFile();
        SimpleUtilitiesConfig.getInstance(configDir);
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        setupKeyCode();
        event.register(toggleHudKey);
    }

    private void onClientTickForge(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            onClientTick();
        }
    }
    *///?}

    private static void setupKeyCode() {
        //? if >=26.1 {
        /*KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MODID, "hud"));
        toggleHudKey = new KeyMapping("key.simple_utilities.toggle_hud", GLFW.GLFW_KEY_K, category);
        *///?} else {
        toggleHudKey = new KeyMapping(
                "key.simple_utilities.toggle_hud",
                GLFW.GLFW_KEY_K,
                "key.category.simple_utilities.hud"
        );
        //?}

        //? if fabric && >=26.1 {
        /*KeyMappingHelper.registerKeyMapping(toggleHudKey);
        *///?} elif fabric {
        KeyBindingHelper.registerKeyBinding(toggleHudKey);
        //?}
    }

    private static void onClientTick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        while (toggleHudKey.consumeClick()) {
            SimpleUtilitiesConfig config = SimpleUtilitiesConfig.getInstance(configDir);

            String chatMessageKey = config.toggleSimpleUtilitiesHUD
                    ? "key.simple_utilities.toggle_hud.chat_message.off"
                    : "key.simple_utilities.toggle_hud.chat_message.on";

            //? if >=26.1 {
            /*client.player.sendOverlayMessage(TextCompat.translatable(chatMessageKey));
            *///?} else {
            client.player.displayClientMessage(TextCompat.translatable(chatMessageKey), true);
            //?}

            config.toggleSimpleUtilitiesHUD = !config.toggleSimpleUtilitiesHUD;
            config.save(configDir);
        }
    }
}
