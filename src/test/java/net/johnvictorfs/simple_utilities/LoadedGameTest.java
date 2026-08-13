package net.johnvictorfs.simple_utilities;

//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.johnvictorfs.simple_utilities.helpers.Colors;
import net.johnvictorfs.simple_utilities.hud.GameInfoMath;
import net.johnvictorfs.simple_utilities.util.TextCompat;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tier 1 "loaded game" tests: these run against a real, bootstrapped
 * Minecraft and a real Fabric loader rather than mocks, courtesy of
 * fabric-loader-junit (see the dependency comment in build.gradle.kts).
 *
 * <p>{@code GameInfoMathTest} already covers the HUD arithmetic headless, with
 * every input hand-written. What only a loaded game can check is whether the
 * <em>game data those numbers are supposed to describe</em> still matches: that
 * this cell's {@code fabric.mod.json} parses and its version ranges are
 * satisfiable here, that {@code TextCompat}'s 1.19 mapping split really
 * produces a usable {@code Component} on this cell, that every facing vanilla
 * can report still has a translation in the shipped lang file, that the speed
 * readout's hardcoded {@code 0.05F} still equals one game tick, that the clock
 * still wraps on the game's own day length, and that the durability warning
 * bands still land where a real tool's durability puts them.
 *
 * <p>Fabric cells only: NeoForge's equivalent bootstrap (junit-fml) is only
 * usable from ModDevGradle, not from Architectury Loom - see the junit-fml
 * exclusion comment in build.gradle.kts.
 */
public class LoadedGameTest {

    private static final String MOD_ID = "simple_utilities";

    /** Every non-dynamic translation key referenced from {@code src/main/java}. */
    private static final String[] STATIC_LANG_KEYS = {
            "key.category.simple_utilities.hud",
            "key.simple_utilities.toggle_hud",
            "key.simple_utilities.toggle_hud.chat_message.on",
            "key.simple_utilities.toggle_hud.chat_message.off",
            "text.hud.simple_utilities.biome",
            "text.hud.simple_utilities.sprinting",
    };

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // 26.x only, and a real behavioural change rather than test plumbing:
        // an item's data components used to be baked into the Item instance at
        // construction, so Bootstrap alone was enough to read one. In 26.x they
        // are produced by BuiltInRegistries.DATA_COMPONENT_INITIALIZERS from a
        // HolderLookup.Provider - i.e. from loaded registry data - and bound
        // onto each Holder.Reference afterwards. Until that runs, constructing
        // any ItemStack throws "Components not bound yet" from
        // Holder.Reference.components(). The server does this during
        // ReloadableServerResources' load; VanillaRegistries.createLookup() is
        // the equivalent built-in-only provider available without a server.
        // (Guarded at 26.1 because that is where this matrix's 26.x line
        // starts - there is no cell between 1.21.4 and 26.2 to narrow it
        // further, and 1.21.4 demonstrably does not need it.)
        //? if >=26.1 {
        /*net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
                .build(net.minecraft.data.registries.VanillaRegistries.createLookup())
                .forEach(net.minecraft.core.component.DataComponentInitializers.PendingComponents::apply);
        *///?}
    }

    @Test
    void gameDataIsActuallyLoaded() {
        // Guard on the harness itself: if the bootstrap above ever silently
        // no-ops, every other assertion in this class becomes vacuous.
        assertNotNull(Items.DIAMOND_PICKAXE, "Items.DIAMOND_PICKAXE should be a real loaded game object");
        // The item registry moved from Registry.ITEM to BuiltInRegistries.ITEM
        // in 1.19.3; both expose getKey/keySet identically.
        //? if >=1.19.3 {
        var itemRegistry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        //?} else
        /*var itemRegistry = net.minecraft.core.Registry.ITEM;*/
        assertEquals("minecraft:diamond_pickaxe", itemRegistry.getKey(Items.DIAMOND_PICKAXE).toString());
        assertTrue(itemRegistry.keySet().size() > 500,
                "the real item registry should hold the full vanilla item set");
    }

    @Test
    void modIsDiscoveredByARealFabricLoader() {
        // The processed fabric.mod.json (Stonecraft templating already
        // applied) is on the test classpath, so a real loader discovers this
        // mod exactly as the game would. Malformed or mis-templated metadata
        // fails here instead of at first launch.
        var self = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow(
                () -> new AssertionError("a real Fabric loader did not discover mod id '" + MOD_ID + "'"));
        assertEquals(MOD_ID, self.getMetadata().getId());
        assertFalse(self.getMetadata().getVersion().getFriendlyString().isBlank(),
                "mod version must survive resource templating");
    }

    @Test
    void declaredDependencyRangesAreSatisfiableInThisCell() {
        // The real drift hazard in a Stonecutter matrix: a cell can compile and
        // package flawlessly while declaring a minecraft range that excludes
        // the very version it was built for, producing a jar that ships and
        // then refuses to load. Here the loader resolves the ranges against the
        // actually-loaded versions, per cell.
        var loader = FabricLoader.getInstance();
        var self = loader.getModContainer(MOD_ID).orElseThrow();
        for (ModDependency dependency : self.getMetadata().getDependencies()) {
            if (dependency.getKind() != ModDependency.Kind.DEPENDS) {
                continue;
            }
            var provider = loader.getModContainer(dependency.getModId());
            assertTrue(provider.isPresent(),
                    "fabric.mod.json requires '" + dependency.getModId() + "' but nothing provides it");
            assertTrue(dependency.matches(provider.get().getMetadata().getVersion()),
                    "fabric.mod.json requires " + dependency + " but this cell loads "
                            + dependency.getModId() + " "
                            + provider.get().getMetadata().getVersion().getFriendlyString());
        }
    }

    @Test
    void textCompatBuildsRealComponentsOnThisCell() {
        // TextCompat is the mod's one text-API version split (pre-1.19
        // new TextComponent/new TranslatableComponent -> 1.19+
        // Component.literal/translatable) and is excluded from the JaCoCo
        // bundle precisely because it imports Minecraft. Under a bootstrapped
        // game it can be exercised for real, on whichever side of the split
        // this cell compiled.
        assertEquals("Biome: Plains", TextCompat.literal("Biome: Plains").getString(),
                "a literal component must round-trip its text verbatim");

        var translatable = TextCompat.translatable("text.hud.simple_utilities.biome");
        assertNotNull(translatable, "translatable() must build a real component on this cell");
        assertFalse(translatable.getString().isBlank(),
                "an unresolved translation still renders as its key, never as empty text");

        // The HUD calls translatableString(key, args) and concatenates the
        // result, so argument substitution has to survive the split too.
        assertFalse(TextCompat.translatableString("text.hud.simple_utilities.biome").isBlank());
    }

    @Test
    void everyRealHorizontalDirectionHasALangKeyAndAnOffset() throws IOException {
        // GameInfoHud builds its facing label as
        //   "text.direction.simple_utilities." + player.getDirection().getSerializedName()
        // and its axis hint from getStepX()/getStepZ(). Both are driven by
        // vanilla data, so this asserts against every facing the *game* can
        // actually report rather than against a hand-written list of four -
        // a renamed serialized name or a new horizontal facing shows up as a
        // missing translation here instead of as a raw key on screen.
        String enUs = readShippedEnUs();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            String langKey = "text.direction.simple_utilities." + facing.getSerializedName();
            assertTrue(enUs.contains('"' + langKey + '"'),
                    "shipped en_us.json has no entry for '" + langKey + "'");

            String axis = facing.getAxis().getName().toUpperCase(Locale.ROOT);
            String expected = (facing.getAxisDirection().getStep() > 0 ? "+" : "-") + axis;
            assertEquals(expected, GameInfoMath.getOffset(facing.getStepX(), facing.getStepZ()),
                    "offset string disagreed with vanilla's own axis metadata for " + facing);
        }
    }

    @Test
    void speedReadoutUsesTheGamesOwnTickRate() {
        // formatSpeed divides by a hardcoded 0.05F to turn blocks-per-tick into
        // metres-per-second. That constant is only correct while a tick is
        // 1/20s, which is the game's own SharedConstants.TICKS_PER_SECOND -
        // pin the two together so a tick-rate change can't silently skew the
        // readout.
        assertEquals(String.format("%.2f m/s", (float) SharedConstants.TICKS_PER_SECOND),
                GameInfoMath.formatSpeed(1f),
                "one block per tick should read as TICKS_PER_SECOND m/s");
        assertEquals(String.format("%.2f m/s", 0f), GameInfoMath.formatSpeed(0f));
    }

    @Test
    void clockWrapsExactlyAtTheGamesDayLength() {
        // parseTime's "+6 hours" offset and its 24-hour wrap encode vanilla's
        // day-time convention: tick 0 is sunrise (06:00), a quarter-day later
        // is noon, three quarters later is midnight. Anchor all three to
        // SharedConstants.TICKS_PER_GAME_DAY rather than to the literal 24000.
        int day = SharedConstants.TICKS_PER_GAME_DAY;
        assertEquals("6:00 AM", GameInfoMath.parseTime(0));
        assertEquals("12:00 PM", GameInfoMath.parseTime(day / 4));
        assertEquals("12:00 AM", GameInfoMath.parseTime(day * 3 / 4));
        assertEquals(GameInfoMath.parseTime(0), GameInfoMath.parseTime(day),
                "the clock must wrap exactly on the game's own day length");

        // And nothing in a whole day may produce a malformed readout.
        for (long tick = 0; tick < day; tick += 137) {
            assertTrue(GameInfoMath.parseTime(tick).matches("\\d{1,2}:\\d{2} [AP]M"),
                    "malformed clock string at tick " + tick + ": " + GameInfoMath.parseTime(tick));
        }
    }

    @Test
    void durabilityColorsFollowRealItemDurability() {
        // GameInfoHud computes currentDurability as
        //   equippedItem.getMaxDamage() - equippedItem.getDamageValue()
        // then hands both to durabilityColor. Drive that with a real damaged
        // ItemStack so the warning bands stay pinned to whatever the game
        // currently says a diamond pickaxe's durability is, rather than to a
        // number copied into a test.
        var pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        int maxDamage = pickaxe.getMaxDamage();
        assertTrue(maxDamage > 100, "a real diamond pickaxe should have substantial durability");

        int defaultColor = 0xFFFFFF;
        assertEquals(defaultColor, colorAt(pickaxe, 0, defaultColor),
                "an undamaged tool should use the configured default text color");
        assertEquals(Colors.lightGreen, colorAt(pickaxe, 1, defaultColor));
        assertEquals(Colors.lightYellow, colorAt(pickaxe, maxDamage - maxDamage / 2, defaultColor));
        assertEquals(Colors.lightOrange, colorAt(pickaxe, maxDamage - maxDamage / 3, defaultColor));
        assertEquals(Colors.lightRed, colorAt(pickaxe, maxDamage - maxDamage / 5, defaultColor));
        assertEquals(Colors.lightRed, colorAt(pickaxe, maxDamage, defaultColor),
                "a fully worn tool should be in the most severe band");
    }

    /** Damages {@code stack} and resolves the HUD color exactly as {@code GameInfoHud} does. */
    private static int colorAt(ItemStack stack, int damageValue, int defaultColor) {
        stack.setDamageValue(damageValue);
        int currentDurability = stack.getMaxDamage() - stack.getDamageValue();
        return GameInfoMath.durabilityColor(currentDurability, stack.getMaxDamage(), defaultColor);
    }

    /** Reads the mod's own shipped en_us.json off the test classpath. */
    private static String readShippedEnUs() throws IOException {
        try (InputStream in = LoadedGameTest.class.getResourceAsStream(
                "/assets/" + MOD_ID + "/lang/en_us.json")) {
            assertNotNull(in, "the mod's shipped en_us.json is missing from the packaged resources");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void everyStaticallyReferencedLangKeyIsTranslated() throws IOException {
        // The keybinding/category/chat-message/HUD-label keys the code names as
        // string literals. A key that exists in code but not in en_us.json
        // renders as the raw key in-game, which no compile step catches.
        String enUs = readShippedEnUs();
        for (String key : STATIC_LANG_KEYS) {
            assertTrue(enUs.contains('"' + key + '"'),
                    "shipped en_us.json has no entry for '" + key + "'");
        }
    }
}
//?}
