package net.johnvictorfs.simple_utilities.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Vendored, dependency-free config for Simple Utilities.
 *
 * <p>Upstream (johnvictorfs/simple-utilities-mod) uses Cloth Config + Mod
 * Menu for an in-game settings screen. Neither has a consistent version
 * matrix across all five target Minecraft versions (1.18.2 - 26.2) and three
 * loaders (Fabric/Forge/NeoForge) this fork targets - Mod Menu in particular
 * is Fabric/Quilt-only with no Forge/NeoForge equivalent, and Cloth Config's
 * Forge/NeoForge artifacts don't track the same version cadence as its
 * Fabric one. Replaced with a plain {@code simple_utilities.properties} file
 * in the game's config directory, read/written directly - no in-game
 * settings screen, no external mod dependency. This mirrors the house
 * pattern EasierVillagerTrading used to drop its own GBfabrictools
 * dependency for the same reason. No Minecraft import here on purpose: this
 * class lives in {@code src/main/java} so it compiles on every loader's
 * loader-agnostic source set.
 */
public class SimpleUtilitiesConfig {
    private static final String FILE_NAME = "simple_utilities.properties";

    private static SimpleUtilitiesConfig instance;

    public boolean toggleSimpleUtilitiesHUD = true;
    public boolean toggleCoordinatesStatus = true;
    public boolean toggleDirectionStatus = true;
    public boolean toggleEquipmentStatus = true;
    public boolean toggleFpsStatus = true;
    public boolean toggleSprintStatus = true;
    public boolean toggleBiomeStatus = true;
    public boolean toggleGameTimeStatus = true;
    public boolean toggleNetherCoordinateConversion = false;
    public boolean togglePlayerSpeedStatus = false;
    public boolean toggleLightLevelStatus = false;
    public boolean togglePlayerName = false;
    public boolean toggleServerName = false;
    public boolean toggleServerAddress = false;
    public int textColor = 0x00E0E0E0;

    public static synchronized SimpleUtilitiesConfig getInstance(File configDir) {
        if (instance == null) {
            instance = new SimpleUtilitiesConfig();
            instance.load(configDir);
        }
        return instance;
    }

    private static File configFile(File configDir) {
        return new File(configDir, FILE_NAME);
    }

    public void load(File configDir) {
        File file = configFile(configDir);
        if (!file.exists()) {
            save(configDir);
            return;
        }

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            return;
        }

        toggleSimpleUtilitiesHUD = readBoolean(props, "toggleSimpleUtilitiesHUD", toggleSimpleUtilitiesHUD);
        toggleCoordinatesStatus = readBoolean(props, "toggleCoordinatesStatus", toggleCoordinatesStatus);
        toggleDirectionStatus = readBoolean(props, "toggleDirectionStatus", toggleDirectionStatus);
        toggleEquipmentStatus = readBoolean(props, "toggleEquipmentStatus", toggleEquipmentStatus);
        toggleFpsStatus = readBoolean(props, "toggleFpsStatus", toggleFpsStatus);
        toggleSprintStatus = readBoolean(props, "toggleSprintStatus", toggleSprintStatus);
        toggleBiomeStatus = readBoolean(props, "toggleBiomeStatus", toggleBiomeStatus);
        toggleGameTimeStatus = readBoolean(props, "toggleGameTimeStatus", toggleGameTimeStatus);
        toggleNetherCoordinateConversion = readBoolean(props, "toggleNetherCoordinateConversion", toggleNetherCoordinateConversion);
        togglePlayerSpeedStatus = readBoolean(props, "togglePlayerSpeedStatus", togglePlayerSpeedStatus);
        toggleLightLevelStatus = readBoolean(props, "toggleLightLevelStatus", toggleLightLevelStatus);
        togglePlayerName = readBoolean(props, "togglePlayerName", togglePlayerName);
        toggleServerName = readBoolean(props, "toggleServerName", toggleServerName);
        toggleServerAddress = readBoolean(props, "toggleServerAddress", toggleServerAddress);
        try {
            textColor = Integer.decode(props.getProperty("textColor", "0x" + Integer.toHexString(textColor)));
        } catch (NumberFormatException ignored) {
            // keep default
        }
    }

    public void save(File configDir) {
        Properties props = new Properties();
        props.setProperty("toggleSimpleUtilitiesHUD", String.valueOf(toggleSimpleUtilitiesHUD));
        props.setProperty("toggleCoordinatesStatus", String.valueOf(toggleCoordinatesStatus));
        props.setProperty("toggleDirectionStatus", String.valueOf(toggleDirectionStatus));
        props.setProperty("toggleEquipmentStatus", String.valueOf(toggleEquipmentStatus));
        props.setProperty("toggleFpsStatus", String.valueOf(toggleFpsStatus));
        props.setProperty("toggleSprintStatus", String.valueOf(toggleSprintStatus));
        props.setProperty("toggleBiomeStatus", String.valueOf(toggleBiomeStatus));
        props.setProperty("toggleGameTimeStatus", String.valueOf(toggleGameTimeStatus));
        props.setProperty("toggleNetherCoordinateConversion", String.valueOf(toggleNetherCoordinateConversion));
        props.setProperty("togglePlayerSpeedStatus", String.valueOf(togglePlayerSpeedStatus));
        props.setProperty("toggleLightLevelStatus", String.valueOf(toggleLightLevelStatus));
        props.setProperty("togglePlayerName", String.valueOf(togglePlayerName));
        props.setProperty("toggleServerName", String.valueOf(toggleServerName));
        props.setProperty("toggleServerAddress", String.valueOf(toggleServerAddress));
        props.setProperty("textColor", "0x" + Integer.toHexString(textColor));

        File file = configFile(configDir);
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "Simple Utilities Mod config - edit by hand, no in-game GUI in this fork");
        } catch (IOException ignored) {
            // best-effort; nothing sensible to do if the config dir isn't writable
        }
    }

    private static boolean readBoolean(Properties props, String key, boolean def) {
        return Boolean.parseBoolean(props.getProperty(key, String.valueOf(def)));
    }
}
