package net.johnvictorfs.simple_utilities.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.johnvictorfs.simple_utilities.helpers.Colors;
import org.junit.jupiter.api.Test;

class GameInfoMathTest {

    private static final double DELTA = 1e-9;

    // -- capitalize --

    @Test
    void capitalize_uppercasesFirstCharacterOnly() {
        assertEquals("Plains", GameInfoMath.capitalize("plains"));
        assertEquals("A", GameInfoMath.capitalize("a"));
    }

    @Test
    void capitalize_nullInNullOut() {
        assertNull(GameInfoMath.capitalize(null));
    }

    // -- zeroPadding --

    @Test
    void zeroPadding_padsSingleDigitWithLeadingZero() {
        assertEquals("00", GameInfoMath.zeroPadding(0));
        assertEquals("05", GameInfoMath.zeroPadding(5));
        assertEquals("09", GameInfoMath.zeroPadding(9));
    }

    @Test
    void zeroPadding_leavesDoubleDigitAndAboveUnchanged() {
        assertEquals("10", GameInfoMath.zeroPadding(10));
        assertEquals("59", GameInfoMath.zeroPadding(59));
        assertEquals("123", GameInfoMath.zeroPadding(123));
    }

    // -- secondsToString --

    @Test
    void secondsToString_formatsMinutesAndSecondsZeroPadded() {
        // zeroPadding is applied to BOTH the minutes and seconds component, so
        // sub-10 minutes are zero-padded too (e.g. "02:09", not "2:09").
        assertEquals("00:00", GameInfoMath.secondsToString(0));
        assertEquals("00:05", GameInfoMath.secondsToString(5));
        assertEquals("01:00", GameInfoMath.secondsToString(60));
        assertEquals("02:09", GameInfoMath.secondsToString(129));
        assertEquals("10:00", GameInfoMath.secondsToString(600));
    }

    // -- parseTime --

    @Test
    void parseTime_atZeroTicksIsSixAM() {
        assertEquals("6:00 AM", GameInfoMath.parseTime(0));
    }

    @Test
    void parseTime_middayIsNoonPM() {
        // Minecraft noon is at tick 6000: (6000/1000 + 6) % 24 = 12 -> displayed as 12 PM.
        assertEquals("12:00 PM", GameInfoMath.parseTime(6000));
    }

    @Test
    void parseTime_midnightIsTwelveAM() {
        // Minecraft midnight is at tick 18000: (18000/1000 + 6) % 24 = 0 -> displayed as 12 AM.
        assertEquals("12:00 AM", GameInfoMath.parseTime(18000));
    }

    @Test
    void parseTime_afternoonIsPM() {
        // tick 12000: (12000/1000 + 6) % 24 = 18 -> hours -= 12 -> 6 PM.
        assertEquals("6:00 PM", GameInfoMath.parseTime(12000));
    }

    @Test
    void parseTime_padsSingleDigitMinutes() {
        // minutes = (500 % 1000) * 60 / 1000 = 30 -> not single digit; use a tick that yields <10.
        // time=50: minutes = (50 % 1000)*60/1000 = 3 -> "03"
        assertEquals("6:03 AM", GameInfoMath.parseTime(50));
    }

    // -- getOffset --

    @Test
    void getOffset_positiveXOnly() {
        assertEquals("+X", GameInfoMath.getOffset(1, 0));
    }

    @Test
    void getOffset_negativeXOnly() {
        assertEquals("-X", GameInfoMath.getOffset(-1, 0));
    }

    @Test
    void getOffset_positiveZOnly() {
        assertEquals("+Z", GameInfoMath.getOffset(0, 1));
    }

    @Test
    void getOffset_negativeZOnly() {
        assertEquals("-Z", GameInfoMath.getOffset(0, -1));
    }

    @Test
    void getOffset_combinedDirections() {
        assertEquals("+X +Z", GameInfoMath.getOffset(1, 1));
        assertEquals("-X -Z", GameInfoMath.getOffset(-1, -1));
        assertEquals("+X -Z", GameInfoMath.getOffset(1, -1));
        assertEquals("-X +Z", GameInfoMath.getOffset(-1, 1));
    }

    @Test
    void getOffset_bothZeroIsEmptyString() {
        assertEquals("", GameInfoMath.getOffset(0, 0));
    }

    // -- formatCoordinates --

    @Test
    void formatCoordinates_joinsWithCommaSpace() {
        assertEquals("10, 64, -20", GameInfoMath.formatCoordinates(10, 64, -20));
    }

    // -- formatNetherFromOverworld / formatOverworldFromNether --

    @Test
    void formatNetherFromOverworld_scalesDownByEight() {
        assertEquals("X: 10, Z: -5", GameInfoMath.formatNetherFromOverworld(80.0, -40.0));
    }

    @Test
    void formatOverworldFromNether_scalesUpByEight() {
        assertEquals("X: 80, Z: -40", GameInfoMath.formatOverworldFromNether(10.0, -5.0));
    }

    // -- formatSpeed --

    @Test
    void formatSpeed_convertsBlocksPerTickToMetersPerSecond() {
        // 0.05 blocks/tick -> exactly 1.00 m/s (20 ticks/sec * 0.05 = 1 block/sec, and the
        // conversion factor here treats 0.05 blocks/tick as the 1 m/s baseline).
        assertEquals("1.00 m/s", GameInfoMath.formatSpeed(0.05F));
        assertEquals("0.00 m/s", GameInfoMath.formatSpeed(0F));
        assertEquals("4.00 m/s", GameInfoMath.formatSpeed(0.2F));
    }

    // -- durabilityColor --

    @Test
    void durabilityColor_fullDurabilityKeepsDefaultColor() {
        assertEquals(Colors.white, GameInfoMath.durabilityColor(100, 100, Colors.white));
    }

    @Test
    void durabilityColor_belowMaxIsLightGreen() {
        assertEquals(Colors.lightGreen, GameInfoMath.durabilityColor(99, 100, Colors.white));
    }

    @Test
    void durabilityColor_atTwoThirdsIsLightYellow() {
        // maxDamage/1.5 == 100/1.5 == 66.67; 66 <= 66.67 -> lightYellow (also < maxDamage).
        assertEquals(Colors.lightYellow, GameInfoMath.durabilityColor(66, 100, Colors.white));
    }

    @Test
    void durabilityColor_atTwoFifthsIsLightOrange() {
        // maxDamage/2.5 == 40; 40 <= 40 -> lightOrange (most severe threshold reached so far).
        assertEquals(Colors.lightOrange, GameInfoMath.durabilityColor(40, 100, Colors.white));
    }

    @Test
    void durabilityColor_atOneQuarterIsLightRed() {
        // maxDamage/4 == 25; 25 <= 25 -> lightRed (most severe, overrides all previous).
        assertEquals(Colors.lightRed, GameInfoMath.durabilityColor(25, 100, Colors.white));
    }

    @Test
    void durabilityColor_zeroDurabilityIsLightRed() {
        assertEquals(Colors.lightRed, GameInfoMath.durabilityColor(0, 100, Colors.white));
    }

    // -- formatItemCount --

    @Test
    void formatItemCount_formatsCountAndInventoryTotal() {
        assertEquals("3 (12)", GameInfoMath.formatItemCount(3, 12));
        assertEquals("0 (0)", GameInfoMath.formatItemCount(0, 0));
    }
}
