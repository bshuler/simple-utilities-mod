package net.johnvictorfs.simple_utilities.helpers;

public class Colors {
    // Every constant below is a bare RGB value with a zero alpha byte, as is
    // config.textColor's default and anything a user is likely to type into
    // their config. See opaqueIfNoAlpha for why that is not the bug it looks
    // like, and why it nonetheless has to be corrected before drawing on 26.x.
    public static final int lightGray = 0x9e9e9e;
    public static final int lightRed = 0xdb4f4f;
    public static final int lightYellow = 0xe0ed53;
    public static final int lightOrange = 0xedb753;
    public static final int lightGreen = 0x3ade65;
    public static final int white = 0x00E0E0E0;

    /**
     * Promotes a colour with an effectively-empty alpha byte to fully opaque,
     * leaving any colour that really does carry alpha untouched.
     *
     * <p>This restores, in mod code, a fixup Minecraft itself used to perform
     * and removed in 26.x. Through 1.21.4 {@code Font} had a private
     * {@code adjustColor(int)} that did exactly this before drawing:
     *
     * <pre>if ((color &amp; 0xFC000000) == 0) color = ARGB.opaque(color);</pre>
     *
     * <p>Confirmed by {@code javap -c} against both real jars: 1.21.4's
     * {@code Font} contains that method and tests against the constant
     * {@code -67108864} (0xFC000000); 26.2's {@code Font} has no
     * {@code adjustColor} at all. So an alpha-0 colour rendered as opaque on
     * every version up to 1.21.4 and renders as fully transparent on 26.x.
     *
     * <p>That is not hypothetical: it is the live bug the Tier 3 client
     * gametest caught. Every colour this mod draws with has a zero alpha byte,
     * so on 26.2 the entire HUD was being drawn perfectly and was completely
     * invisible - the render hook fired, the text was submitted, and nothing
     * appeared on screen. The whole build matrix, the unit suite and the Tier 1
     * loaded tests were all green throughout.
     *
     * <p>Fixing it here rather than by adding {@code 0xFF} to the constants
     * above is deliberate: {@code config.textColor} is user-supplied and lives
     * in a JSON file already written to disk, so existing configs (and any
     * plain RGB value a user picks in future) would still be invisible. The
     * 0xFC mask, rather than a plain {@code != 0} alpha check, is vanilla's own
     * rule kept verbatim, so behaviour on 26.x now matches what every earlier
     * version has always done.
     *
     * <p>Applied in {@code PlatformCanvas.drawText} on every cell, not just the
     * 26.1+ branch - the older cells were already getting this from vanilla, so
     * applying it uniformly changes nothing there and removes the version
     * dependency. It is deliberately <em>not</em> applied to
     * {@code PlatformCanvas.fill}: vanilla never did the fixup for fills, so
     * adding it there would change behaviour rather than preserve it.
     */
    public static int opaqueIfNoAlpha(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }
}
