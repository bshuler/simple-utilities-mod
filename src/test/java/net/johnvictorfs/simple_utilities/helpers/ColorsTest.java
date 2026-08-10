package net.johnvictorfs.simple_utilities.helpers;

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
}
