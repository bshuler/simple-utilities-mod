package net.johnvictorfs.simple_utilities.hud;

import net.johnvictorfs.simple_utilities.helpers.Colors;

/**
 * Pure, loader/version-agnostic HUD math and formatting extracted from
 * {@link GameInfoHud}: unit conversions (nether/overworld coordinate
 * scaling, blocks-per-tick to meters-per-second), direction-offset math,
 * durability-color thresholds, and plain string formatting. No Minecraft
 * class dependency at all (only primitives and {@link Colors}, itself a
 * plain int-constants class) and no Stonecutter conditionals, so this file
 * is identical on every cell in the matrix and is fully unit-testable
 * headless - mirrors the sibling FlightHud mod's {@code FlightComputerMath}
 * extraction pattern (see that class's javadoc for the same rationale).
 *
 * <p>{@link GameInfoHud}'s own draw/get-info methods stay as thin
 * Minecraft-touching wrappers that pull primitives (step deltas, durability
 * ints, coordinate doubles) out of live {@code Direction}/{@code ItemStack}/
 * {@code LocalPlayer} objects and hand them to the methods below - the
 * actual arithmetic/formatting lives here exactly once instead of inline.
 */
public final class GameInfoMath {

    private GameInfoMath() {
    }

    /** Capitalizes the first character of {@code str}; {@code null} in, {@code null} out. */
    public static String capitalize(String str) {
        if (str == null) return null;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /** Zero-pads a sub-10 number to two digits (e.g. {@code 5} -> {@code "05"}); leaves 10+ unchanged. */
    public static String zeroPadding(int number) {
        return (number >= 10) ? Integer.toString(number) : String.format("0%s", number);
    }

    /**
     * Formats a duration in seconds as {@code "M:SS"} (zero-padded seconds).
     * Not currently called from {@link GameInfoHud} - upstream dead code kept
     * intact (behavior-preserving refactor) and unit-tested directly here.
     */
    public static String secondsToString(int pTime) {
        final int min = pTime / 60;
        final int sec = pTime - (min * 60);
        return String.format("%s:%s", zeroPadding(min), zeroPadding(sec));
    }

    /** Converts a Minecraft day-time tick count ({@code 0..24000}) into a 12-hour {@code "H:MM AM/PM"} clock string. */
    public static String parseTime(long time) {
        long hours = (time / 1000 + 6) % 24;
        long minutes = (time % 1000) * 60 / 1000;
        String ampm = "AM";

        if (hours >= 12) {
            hours -= 12;
            ampm = "PM";
        }

        if (hours == 0) hours = 12;

        String mm = "0" + minutes;
        mm = mm.substring(mm.length() - 2);

        return hours + ":" + mm + " " + ampm;
    }

    /**
     * Formats a facing direction's horizontal step deltas as an
     * {@code "+X"}/{@code "-X"}/{@code "+Z"}/{@code "-Z"} offset string
     * (e.g. {@code "+X +Z"}), or {@code ""} when both deltas are zero.
     * Takes the raw step deltas rather than a {@code Direction} so this
     * stays Minecraft-free.
     */
    public static String getOffset(int stepX, int stepZ) {
        String offset = "";

        if (stepX > 0) {
            offset += "+X";
        } else if (stepX < 0) {
            offset += "-X";
        }

        if (stepZ > 0) {
            offset += " +Z";
        } else if (stepZ < 0) {
            offset += " -Z";
        }

        return offset.trim();
    }

    /** Formats player coordinates as {@code "X, Y, Z"}. */
    public static String formatCoordinates(int x, int y, int z) {
        return String.format("%d, %d, %d", x, y, z);
    }

    /** Converts overworld X/Z into the corresponding nether coordinates (1:8 scale) and formats them. */
    public static String formatNetherFromOverworld(double overworldX, double overworldZ) {
        return String.format("X: %.0f, Z: %.0f", overworldX / 8, overworldZ / 8);
    }

    /** Converts nether X/Z into the corresponding overworld coordinates (8:1 scale) and formats them. */
    public static String formatOverworldFromNether(double netherX, double netherZ) {
        return String.format("X: %.0f, Z: %.0f", netherX * 8, netherZ * 8);
    }

    /** Converts a blocks-per-tick speed into meters-per-second and formats it as {@code "N.NN m/s"}. */
    public static String formatSpeed(float blocksPerTick) {
        return String.format("%.2f m/s", blocksPerTick / 0.05F);
    }

    /**
     * Picks the durability-warning text color for the given current/max
     * damage values, falling back to {@code defaultColor} when durability
     * isn't below any threshold. Mirrors the cascading if-chain in
     * {@code GameInfoHud#drawEquipmentLine} exactly (each threshold can
     * override the previous, most-severe wins since they're checked in
     * order from least to most severe).
     */
    public static int durabilityColor(int currentDurability, int maxDamage, int defaultColor) {
        int color = defaultColor;
        if (currentDurability < maxDamage) {
            color = Colors.lightGreen;
        }
        if (currentDurability <= (maxDamage / 1.5)) {
            color = Colors.lightYellow;
        }
        if (currentDurability <= (maxDamage / 2.5)) {
            color = Colors.lightOrange;
        }
        if (currentDurability <= (maxDamage / 4)) {
            color = Colors.lightRed;
        }
        return color;
    }

    /** Formats an item's stack count alongside its total inventory count as {@code "count (inventoryCount)"}. */
    public static String formatItemCount(int count, int inventoryCount) {
        return count + " (" + inventoryCount + ")";
    }
}
