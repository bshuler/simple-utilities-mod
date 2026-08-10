package net.johnvictorfs.simple_utilities.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link SimpleUtilitiesConfig} is a plain Java class (no Minecraft import)
 * so it's fully unit-testable headless. It does carry one piece of static
 * state - the {@code getInstance} singleton cache - which each test resets
 * via reflection in {@link #resetSingleton()} for isolation. This is test
 * plumbing for correctness, not a "reflection hack to inflate coverage":
 * every reset still exercises {@code getInstance}'s real null-check/load
 * path on the next call.
 */
class SimpleUtilitiesConfigTest {

    @BeforeEach
    void resetSingleton() throws Exception {
        Field instanceField = SimpleUtilitiesConfig.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void getInstance_createsAndCachesSameInstance(@TempDir File configDir) {
        SimpleUtilitiesConfig first = SimpleUtilitiesConfig.getInstance(configDir);
        SimpleUtilitiesConfig second = SimpleUtilitiesConfig.getInstance(configDir);
        assertSame(first, second);
    }

    @Test
    void getInstance_onFreshDirWritesDefaultConfigFile(@TempDir File configDir) {
        SimpleUtilitiesConfig.getInstance(configDir);
        File written = new File(configDir, "simple_utilities.properties");
        assertTrue(written.exists(), "expected getInstance() to save a default config file on first load");
    }

    @Test
    void load_onMissingFileKeepsDefaultsAndSaves(@TempDir File configDir) {
        SimpleUtilitiesConfig config = new SimpleUtilitiesConfig();
        config.load(configDir);

        assertTrue(config.toggleSimpleUtilitiesHUD);
        assertTrue(config.toggleCoordinatesStatus);
        assertFalse(config.toggleNetherCoordinateConversion);
        assertEquals(0x00E0E0E0, config.textColor);
        assertTrue(new File(configDir, "simple_utilities.properties").exists());
    }

    @Test
    void load_readsOverriddenBooleanAndColorValues(@TempDir File configDir) throws IOException {
        Properties props = new Properties();
        props.setProperty("toggleSimpleUtilitiesHUD", "false");
        props.setProperty("toggleNetherCoordinateConversion", "true");
        props.setProperty("textColor", "0xFF00FF");
        writeProps(configDir, props);

        SimpleUtilitiesConfig config = new SimpleUtilitiesConfig();
        config.load(configDir);

        assertFalse(config.toggleSimpleUtilitiesHUD);
        assertTrue(config.toggleNetherCoordinateConversion);
        assertEquals(0xFF00FF, config.textColor);
        // Untouched keys keep their built-in defaults.
        assertTrue(config.toggleCoordinatesStatus);
    }

    @Test
    void load_withInvalidTextColorFallsBackToDefault(@TempDir File configDir) throws IOException {
        Properties props = new Properties();
        props.setProperty("textColor", "not-a-number");
        writeProps(configDir, props);

        SimpleUtilitiesConfig config = new SimpleUtilitiesConfig();
        config.load(configDir);

        assertEquals(0x00E0E0E0, config.textColor);
    }

    @Test
    void load_withUnreadableMalformedPropertiesFileReturnsWithoutThrowing(@TempDir File configDir) throws IOException {
        // A directory where the file should be makes the FileInputStream open fail with
        // an IOException, exercising SimpleUtilitiesConfig#load's catch-and-return path.
        File asDirectory = new File(configDir, "simple_utilities.properties");
        assertTrue(asDirectory.mkdirs());

        SimpleUtilitiesConfig config = new SimpleUtilitiesConfig();
        config.load(configDir);

        // Falls back to compiled-in defaults; no exception propagates.
        assertTrue(config.toggleSimpleUtilitiesHUD);
        assertEquals(0x00E0E0E0, config.textColor);
    }

    @Test
    void saveAndLoad_roundTripsMutatedValues(@TempDir File configDir) {
        SimpleUtilitiesConfig original = new SimpleUtilitiesConfig();
        original.toggleSimpleUtilitiesHUD = false;
        original.toggleServerName = true;
        original.textColor = 0x123456;
        original.save(configDir);

        SimpleUtilitiesConfig reloaded = new SimpleUtilitiesConfig();
        reloaded.load(configDir);

        assertFalse(reloaded.toggleSimpleUtilitiesHUD);
        assertTrue(reloaded.toggleServerName);
        assertEquals(0x123456, reloaded.textColor);
    }

    @Test
    void save_whenTargetPathIsUnwritableIgnoresIOException(@TempDir File configDir) throws IOException {
        // Pre-create the config file's path AS A DIRECTORY, so
        // `new FileOutputStream(file)` fails with an IOException ("Is a
        // directory") - exercises save()'s catch-and-ignore (best-effort) path.
        File asDirectory = new File(configDir, "simple_utilities.properties");
        assertTrue(asDirectory.mkdirs());

        SimpleUtilitiesConfig config = new SimpleUtilitiesConfig();
        // Must not throw.
        config.save(configDir);
    }

    @Test
    void save_createsParentDirectoriesWhenMissing(@TempDir File configDir) {
        File nested = new File(configDir, "nested/subdir");
        SimpleUtilitiesConfig config = new SimpleUtilitiesConfig();
        config.save(nested);

        assertTrue(new File(nested, "simple_utilities.properties").exists());
    }

    private static void writeProps(File configDir, Properties props) throws IOException {
        File file = new File(configDir, "simple_utilities.properties");
        try (FileWriter writer = new FileWriter(file)) {
            props.store(writer, "test fixture");
        }
    }
}
