package net.johnvictorfs.simple_utilities.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * {@link Colors} is a plain (non-final) constants holder with no explicit
 * constructor, so javac generates an implicit public no-arg constructor -
 * JaCoCo attributes that synthetic constructor's line to the class
 * declaration itself, and it shows up as uncovered unless something
 * actually instantiates the class. Nothing in production does (only the
 * {@code static final int} fields are ever read), so this test exists
 * purely to exercise that synthetic constructor for coverage purposes.
 */
class ColorsTest {

    @Test
    void constructor_isReachable() {
        assertNotNull(new Colors());
    }

    // opaqueIfNoAlpha reproduces the fixup Minecraft's own Font.adjustColor did
    // until 26.x removed it; these cases are that method's truth table. The
    // consequence of getting it wrong is not a wrong shade, it is an invisible
    // HUD on 26.x - see Colors#opaqueIfNoAlpha and PLAN.md "Tier 3".

    @Test
    void opaqueIfNoAlpha_promotesBareRgbToOpaque() {
        assertEquals(0xFF9E9E9E, Colors.opaqueIfNoAlpha(Colors.lightGray));
        assertEquals(0xFFE0E0E0, Colors.opaqueIfNoAlpha(Colors.white));
    }

    @Test
    void opaqueIfNoAlpha_leavesRealAlphaAlone() {
        // 0x80 is a genuine half-transparent alpha and must survive untouched,
        // otherwise a user asking for a faded HUD silently gets a solid one.
        assertEquals(0x80123456, Colors.opaqueIfNoAlpha(0x80123456));
        assertEquals(0xFF123456, Colors.opaqueIfNoAlpha(0xFF123456));
    }

    @Test
    void opaqueIfNoAlpha_usesVanillasOwnThreshold() {
        // Vanilla masked with 0xFC000000, not 0xFF000000: alpha values 1-3 read
        // as "no alpha given" and get promoted, 4 is the first that counts.
        assertEquals(0xFF000000, Colors.opaqueIfNoAlpha(0x03000000));
        assertEquals(0x04000000, Colors.opaqueIfNoAlpha(0x04000000));
    }

    @Test
    void opaqueIfNoAlpha_promotesBlack() {
        assertEquals(0xFF000000, Colors.opaqueIfNoAlpha(0x00000000));
    }
}
