package net.johnvictorfs.simple_utilities.gametest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.johnvictorfs.simple_utilities.config.SimpleUtilitiesConfig;
import net.johnvictorfs.simple_utilities.hud.HudHooks;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Tier 3: the mod running inside a <strong>real Minecraft client</strong> --
 * real window, real render thread, real game loop, real world -- asserted
 * against <strong>the pixels that actually reached the screen</strong>.
 *
 * <p>This exists because of one specific hole the other tiers cannot cover.
 * The unit suite proves the maths in {@code GameInfoMath}; Tier 1
 * ({@code LoadedGameTest}, via fabric-loader-junit) proves a real loader
 * discovers this mod and that its {@code fabric.mod.json} ranges resolve.
 * Neither renders a frame, so neither can tell you whether
 * {@code HudHooks.register()} picked the <em>right</em> hook for this
 * Minecraft version, nor whether the draw calls it makes produce anything
 * visible. A mod whose HUD silently never appears still compiles, still
 * packages, still loads, and still passes every other test in this repo.
 *
 * <p><strong>Both of those failures have actually happened here, and the
 * second one is why this test looks the way it does.</strong> The first
 * version of this test asserted only {@link HudHooks#hasRendered()} -- that
 * the hook fired. It went green on 26.2 while the HUD was <em>completely
 * invisible</em>: 26.x removed {@code Font.adjustColor}, which had been
 * silently promoting this mod's alpha-0 colours to opaque, so every line was
 * being submitted correctly and drawn with zero alpha (see
 * {@code Colors#opaqueIfNoAlpha} for the full root-cause writeup). "The hook
 * fired" is not "the user can see it", and only a screenshot can tell the
 * difference. So this test reads the screenshots back.
 *
 * <h2>How the pixel assertion works</h2>
 *
 * <p>Three screenshots are taken in-world, all from the same standing
 * position, {@value #SETTLE_TICKS} ticks apart:
 *
 * <ol>
 *   <li><b>off-a</b> and <b>off-b</b>, with the overlay toggled <em>off</em>;
 *   <li><b>on</b>, with it toggled back on.
 * </ol>
 *
 * <p>{@code diff(off-a, off-b)} is the <em>ambient noise floor</em>: how much
 * the frame changes on its own over one interval with the mod drawing nothing
 * -- drifting clouds, the sun, foliage and water animation, mobs, the vanilla
 * HUD's own jitter. {@code diff(off-b, on)} is that same noise <em>plus</em>
 * whatever the mod drew. Subtracting gives an estimate of the mod's own
 * pixels that needs no golden image, no fixed resolution, no fixed GUI scale
 * and no per-version reference art -- which matters because this one file is
 * shared by two Minecraft versions and runs on both a Mac laptop and a
 * software-rendered xvfb CI box, where nothing about the framebuffer matches.
 *
 * <p>Only the top-left quadrant is measured: that is where
 * {@code GameInfoHud} draws its game-info lines (x=4, y=4 downward). The
 * equipment/durability block is bottom-left and is deliberately outside the
 * measured region -- it sits next to the vanilla hotbar and hunger/health
 * bars, which animate constantly, and the game-info lines alone are enough to
 * prove the draw path reaches real pixels. The equipment block is still
 * exercised (its toggle is forced on), just not measured.
 *
 * <p>If the noise floor itself is implausibly large the test fails rather than
 * passing on a coin flip -- a noisy reading cannot be honestly interpreted
 * either way, and silently accepting it is exactly the kind of vacuous pass
 * this tier exists to eliminate.
 *
 * <p>What it asserts, in order:
 *
 * <ol>
 *   <li>the client reaches the title screen at all -- the mod's client
 *       entrypoint ran without killing startup;</li>
 *   <li>the loader running that client really has {@code simple_utilities}
 *       loaded (a guard on the harness: without it everything below could pass
 *       with the mod jar absent from the classpath);</li>
 *   <li>the HUD render hook <strong>actually fires in-world</strong>, read off
 *       {@link HudHooks#hasRendered()}, which can only become true inside
 *       {@code HudHooks.render(...)}. Kept as a separate assertion from the
 *       pixel check purely for diagnosis: it distinguishes "the hook is wrong
 *       for this version" from "the hook fires but nothing is visible";</li>
 *   <li>the overlay <strong>changes the framebuffer</strong> by at least
 *       {@value #MIN_HUD_PIXELS} pixels above the ambient noise floor -- the
 *       assertion the 26.2 alpha bug would have failed;</li>
 *   <li>the whole draw path survives sustained rendering. Nothing here
 *       swallows exceptions -- {@code HudHooks.render} calls
 *       {@code GameInfoHud.draw} with no try/catch, and the Fabric hook does
 *       not catch either -- so anything thrown while drawing propagates out of
 *       the render thread and takes the client down, failing this test by
 *       crash rather than by assertion.</li>
 * </ol>
 *
 * <p>One thing to know about {@code HudHooks.render}: it returns early while
 * the F3 debug overlay is showing. The gametest world opens with it off, and
 * nothing here presses F3 -- but if this test ever starts failing at assertion
 * 3 for no obvious reason, that guard is the first thing to check.
 *
 * <p>All screenshots are written to
 * {@code versions/<cell>/build/run/clientGameTest/screenshots/} on every run
 * as evidence, and their paths are quoted in the failure messages so a CI
 * failure can be diagnosed from the uploaded artifacts alone.
 *
 * <p><strong>No version branching, though Stonecutter would allow it.</strong>
 * This source set <em>is</em> preprocessed (Loom's {@code createSourceSet}
 * leaves {@code gametest} on Gradle's default convention and Stonecutter picks
 * it up from there), so {@code //? if} markers work here. There are none
 * because they are not needed: the file is shared by the 1.21.4 and 26.2 Fabric
 * cells (see {@code build.gradle.kts}, {@code clientGameTestSupported}), which
 * ship client-gametest API v4.1.1 and v6.0.0 respectively, and only the surface
 * those two have in common is used. In particular
 * {@code TestSingleplayerContext.getClientWorld()} (v4 only) and
 * {@code getConnection()} (v6 only) are both avoided; the full API diff is
 * recorded in the sibling FlightHud repo's PLAN.md "Tier 3". The screenshots
 * are decoded with {@link ImageIO} rather than Minecraft's own
 * {@code NativeImage} for the same reason: {@code NativeImage}'s pixel
 * accessors were renamed across this range, the JDK's were not.
 */
public class SimpleUtilitiesClientGameTest implements FabricClientGameTest {
    /** Ticks to let the world settle between screenshots. */
    private static final int SETTLE_TICKS = 20;

    /**
     * How many pixels the overlay must change, over and above the ambient
     * noise floor, before it counts as visible.
     *
     * <p>Deliberately a floor, not a target: the measured reading is ~3,500
     * px on both cells (four lines of shadowed text) against a noise floor of
     * 0, and the 26.2 alpha bug produced a reading of nothing at all. Anything
     * in between is not a legible HUD either way, so a low bar costs nothing
     * and keeps the test from turning into a brittle assertion about font
     * metrics, GUI scale or framebuffer size - all three of which differ
     * between a Mac laptop and the software-rendered CI box.
     */
    private static final int MIN_HUD_PIXELS = 300;

    /**
     * Per-channel difference at which two pixels count as different.
     *
     * <p>Set from measurement, not taste, and the measurement is the whole
     * reason this constant needs a comment. A first attempt used 8, and both
     * cells refused to return a verdict: 42% of the measured region changed
     * between two consecutive overlay-off frames. Rendering the difference as
     * an image showed why - it is a smooth, whole-frame radial gradient, the
     * sky and terrain lighting drifting a few units as the sun moves. Nothing
     * is "wrong" with it; it is simply not a per-pixel-independent noise
     * source, so a low threshold sums it into tens of thousands of pixels.
     *
     * <p>Its amplitude is bounded and small, though. Measured over the whole
     * frame on both cells: ~91k pixels differ by more than 8, ~18k by more
     * than 16, ~100 by more than 32, and <strong>zero</strong> by more than
     * 64. Shadowed HUD glyphs are the opposite shape - few pixels, enormous
     * amplitude - and land ~3,500 pixels above 64 in the measured region on
     * both cells. A threshold of 64 therefore separates the two cleanly with
     * an order of magnitude of headroom on each side.
     */
    private static final int CHANNEL_DELTA = 64;

    /**
     * Largest ambient noise floor, as a fraction of the measured region, that
     * still permits an honest verdict. Past this the subtraction is measuring
     * weather, not the mod.
     */
    private static final double MAX_NOISE_FRACTION = 0.10;

    private static final String MOD_ID = "simple_utilities";

    @Override
    public void runTest(ClientGameTestContext context) {
        context.waitForScreen(TitleScreen.class);
        context.takeScreenshot("simple-utilities-title-screen");

        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            throw new AssertionError("the client booted without the '" + MOD_ID
                    + "' mod loaded - everything below this point would have passed vacuously");
        }

        try (TestSingleplayerContext singleplayer =
                     context.worldBuilder().setUseConsistentSettings(true).create()) {
            // Overlay off: two frames one interval apart, to measure how much
            // this world changes on its own while the mod draws nothing.
            setOverlayEnabled(context, false);
            context.waitTicks(SETTLE_TICKS);
            Path offA = context.takeScreenshot("simple-utilities-hud-off-a");
            context.waitTicks(SETTLE_TICKS);
            Path offB = context.takeScreenshot("simple-utilities-hud-off-b");

            // Overlay on: same interval again, same viewpoint.
            setOverlayEnabled(context, true);
            context.waitTicks(SETTLE_TICKS);
            Path on = context.takeScreenshot("simple-utilities-in-world");

            if (!context.computeOnClient(client -> HudHooks.hasRendered())) {
                throw new AssertionError("the HUD render hook never fired: after "
                        + (3 * SETTLE_TICKS) + " in-world ticks HudHooks.hasRendered() is still "
                        + "false, which means HudHooks.render(...) was never called. The hook "
                        + "registered by HudHooks.register() is not one this Minecraft version "
                        + "fires.");
            }

            assertOverlayIsVisible(offA, offB, on);

            // Keep the singleplayer context referenced until here so it is
            // unambiguous that the assertions above ran with the world open.
            singleplayer.getWorldSave();
        }

        // The client gametest runner requires the test to end on the title
        // screen with no server running and no connection open.
        context.waitForScreen(TitleScreen.class);
    }

    /**
     * Forces the overlay toggles rather than trusting the config defaults: a
     * stale config file left in the run directory would otherwise quietly turn
     * every assertion below into a no-op.
     *
     * <p>{@code getInstance(null)} is safe here and does not risk a null config
     * dir - the mod's client entrypoint has already called
     * {@code getInstance(configDir)} during startup, and {@code getInstance}
     * ignores its argument once the singleton exists. The client has reached
     * the title screen by now, so that has certainly happened.
     *
     * <p>{@code toggleSimpleUtilitiesHUD} gates the whole of
     * {@code GameInfoHud.draw}, so switching it off blanks the mod's entire
     * overlay - which is what makes the off/on differential meaningful.
     */
    private static void setOverlayEnabled(ClientGameTestContext context, boolean enabled) {
        context.runOnClient(client -> {
            SimpleUtilitiesConfig config = SimpleUtilitiesConfig.getInstance(null);
            config.toggleSimpleUtilitiesHUD = enabled;
            config.toggleEquipmentStatus = true;
        });
    }

    /**
     * The pixel assertion described in this class's javadoc: the overlay must
     * move more pixels than the world moves on its own.
     */
    private static void assertOverlayIsVisible(Path offA, Path offB, Path on) {
        BufferedImage imageOffA = read(offA);
        BufferedImage imageOffB = read(offB);
        BufferedImage imageOn = read(on);

        int width = Math.min(imageOffA.getWidth(), Math.min(imageOffB.getWidth(), imageOn.getWidth())) / 2;
        int height = Math.min(imageOffA.getHeight(), Math.min(imageOffB.getHeight(), imageOn.getHeight())) / 2;

        int noise = differingPixels(imageOffA, imageOffB, width, height);
        int signal = differingPixels(imageOffB, imageOn, width, height);
        int attributable = signal - noise;

        String evidence = "measured the top-left " + width + "x" + height + " pixels; "
                + "ambient noise floor (overlay off, two frames " + SETTLE_TICKS + " ticks apart) = "
                + noise + " px; overlay-on difference = " + signal + " px; "
                + "attributable to the overlay = " + attributable + " px."
                + System.lineSeparator() + "  overlay off: " + offA
                + System.lineSeparator() + "  overlay off: " + offB
                + System.lineSeparator() + "  overlay on:  " + on;

        int maxNoise = (int) (width * (long) height * MAX_NOISE_FRACTION);
        if (noise > maxNoise) {
            throw new AssertionError("this world is too visually unstable to judge the overlay "
                    + "against: the frame changed by " + noise + " px with the overlay off, above "
                    + "the " + maxNoise + " px ceiling, so subtracting it cannot honestly tell you "
                    + "whether the overlay drew anything. This is a fault in the test's premise, "
                    + "not a verdict on the mod - check the screenshots for weather, a mob in "
                    + "frame, or a moving camera. " + evidence);
        }

        if (attributable < MIN_HUD_PIXELS) {
            throw new AssertionError("the HUD render hook fires but the overlay is not visible: "
                    + "switching it on changed fewer than " + MIN_HUD_PIXELS + " pixels above the "
                    + "noise floor. The mod is drawing into a frame that does not show it - the "
                    + "known cause is a colour with an empty alpha byte on 26.x, where "
                    + "Font.adjustColor no longer promotes it to opaque (see "
                    + "Colors#opaqueIfNoAlpha). " + evidence);
        }
    }

    /** Counts pixels differing by more than {@link #CHANNEL_DELTA} on any channel. */
    private static int differingPixels(BufferedImage a, BufferedImage b, int width, int height) {
        int count = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelA = a.getRGB(x, y);
                int pixelB = b.getRGB(x, y);

                if (pixelA == pixelB) {
                    continue;
                }

                if (Math.abs(((pixelA >> 16) & 0xFF) - ((pixelB >> 16) & 0xFF)) > CHANNEL_DELTA
                        || Math.abs(((pixelA >> 8) & 0xFF) - ((pixelB >> 8) & 0xFF)) > CHANNEL_DELTA
                        || Math.abs((pixelA & 0xFF) - (pixelB & 0xFF)) > CHANNEL_DELTA) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Reads a screenshot back off disk.
     *
     * <p>Safe to call the moment {@code takeScreenshot} returns: the API writes
     * the file synchronously (via {@code NativeImage.writeToFile}) before
     * handing back the path, so there is no partial-write window to poll for.
     *
     * <p>Headless is asserted before AWT is first touched. This runs inside a
     * live GLFW client, and on macOS initialising the AWT toolkit in a process
     * that owns the GLFW main thread can abort the JVM outright. PNG decoding
     * itself needs no toolkit, and this keeps it that way.
     */
    private static BufferedImage read(Path screenshot) {
        System.setProperty("java.awt.headless", "true");

        if (!Files.isRegularFile(screenshot)) {
            throw new AssertionError("the client gametest API returned a screenshot path that is "
                    + "not a file: " + screenshot);
        }

        try {
            BufferedImage image = ImageIO.read(screenshot.toFile());

            if (image == null) {
                throw new AssertionError("no image decoder accepted the screenshot at " + screenshot);
            }

            return image;
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read back the screenshot at " + screenshot, e);
        }
    }
}
