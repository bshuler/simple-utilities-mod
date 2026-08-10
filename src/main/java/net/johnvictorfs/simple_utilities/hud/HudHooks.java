package net.johnvictorfs.simple_utilities.hud;

//? if fabric && <1.20 {
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
*///?} elif fabric && <1.21.2 {
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.GuiGraphics;
*///?} elif fabric && >=26.1 {
/*import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
*///?} elif fabric {
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
//?}
//? if neoforge {
/*import net.minecraft.client.DeltaTracker;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
*///?} elif forge && <1.19 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.OverlayRegistry;
*///?} elif forge && <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
*///?} elif forge {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
*///?}

import net.minecraft.client.Minecraft;

/**
 * Loader/version-conditional HUD render-hook registration. Replaces the old
 * {@code mixin/GameInfoMixin.java} (retired): MC 26.1+'s {@code Gui} class
 * has no {@code render(...)} method to inject into at all any more - it now
 * exposes an extraction-based API instead, a genuine architecture change,
 * not a rename - so hooking every loader's own official HUD-render event/
 * registry directly (following the sibling FlightHud mod's proven pattern)
 * is both more portable AND unblocks 26.x without needing Mixin at all.
 *
 * <p>Also, unlike the old Mixin-based version of this feature, this class
 * (and the {@link GameInfoHud} it drives) now builds against uniform
 * Mojang-official mappings on every loader - Forge and NeoForge get the
 * real HUD feature too, not a config-only stub.
 *
 * <p>The two NeoForge target versions (1.21.4 and 26.2) share the exact same
 * source below: {@code RenderGuiEvent.Post#getGuiGraphics()} returns {@code
 * GuiGraphics} on 1.21.4 and {@code GuiGraphicsExtractor} on 26.2 (confirmed
 * via javap against both real jars), and {@link PlatformCanvas} has a
 * matching constructor overload compiled per node, so the identical call
 * site {@code new PlatformCanvas(event.getGuiGraphics())} resolves
 * correctly either way with zero further branching needed.
 */
public final class HudHooks {
    private static GameInfoHud hud;

    private HudHooks() {
    }

    private static void render(SimpleCanvas canvas) {
        // Debug-HUD-visible guard, moved in from the old GameInfoMixin's
        // onDraw injector (which gated the call to hudInfo.draw(...) on this
        // same condition). Version-split exactly like that mixin was:
        // Minecraft.getDebugOverlay() only exists from 1.21.4 onward
        // (confirmed via javap); 1.18.2/1.19.4/1.20.1 use the plain
        // Options.renderDebug field instead.
        //? if >=1.21 {
        if (Minecraft.getInstance().getDebugOverlay().showDebugScreen()) {
            return;
        }
        //?} else {
        /*if (Minecraft.getInstance().options.renderDebug) {
            return;
        }
        *///?}
        if (hud == null) {
            hud = new GameInfoHud(Minecraft.getInstance());
        }
        hud.draw(canvas);
    }

    /** Called from the Fabric/NeoForge/Forge&lt;1.19 imperative registration sites. */
    public static void register() {
        //? if fabric && <1.20 {
        /*HudRenderCallback.EVENT.register((poseStack, tickDelta) -> render(new PlatformCanvas(poseStack)));
        *///?} elif fabric && <1.21.2 {
        /*HudRenderCallback.EVENT.register((GuiGraphics context, float tickDelta) ->
            render(new PlatformCanvas(context)));
        *///?} elif fabric && >=26.1 {
        /*HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("simple_utilities", "hud"),
            (GuiGraphicsExtractor context, DeltaTracker deltaTracker) -> render(new PlatformCanvas(context)));
        *///?} elif fabric {
        HudRenderCallback.EVENT.register((GuiGraphics context, DeltaTracker deltaTracker) ->
            render(new PlatformCanvas(context)));
        //?} elif neoforge {
        /*NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) ->
            render(new PlatformCanvas(event.getGuiGraphics())));
        *///?} elif forge && <1.19 {
        /*OverlayRegistry.registerOverlayTop("simple_utilities",
            (ForgeIngameGui gui, PoseStack poseStack, float tickDelta, int width, int height) ->
                render(new PlatformCanvas(poseStack)));
        *///?}
        // forge>=1.19 is intentionally absent here: RegisterGuiOverlaysEvent must be
        // registered on the mod event bus at construction time, not called
        // imperatively - see registerOverlays(...) below and its wiring in
        // SimpleUtilities.java's constructor.
    }

    // forge<1.19 (1.18.2) intentionally has NO registerOverlays method: it
    // doesn't use the event-based RegisterGuiOverlaysEvent registration at
    // all - it registers imperatively via OverlayRegistry in register()
    // above instead (RegisterGuiOverlaysEvent/ForgeGui don't even resolve
    // against the 1.18.2 Forge jar). The empty first branch below is
    // deliberate (see FlightHud's HudHooks.java for the identical pattern).
    //? if forge && <1.19 {
    //?} elif forge && <1.20 {
    /*public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("simple_utilities",
            (ForgeGui gui, PoseStack poseStack, float tickDelta, int width, int height) ->
                render(new PlatformCanvas(poseStack)));
    }
    *///?} elif forge {
    /*public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("simple_utilities",
            (ForgeGui gui, GuiGraphics context, float tickDelta, int width, int height) ->
                render(new PlatformCanvas(context)));
    }
    *///?}
}
