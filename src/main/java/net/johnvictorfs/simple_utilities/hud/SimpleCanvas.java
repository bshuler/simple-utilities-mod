package net.johnvictorfs.simple_utilities.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;

/**
 * Loader/version-agnostic HUD drawing surface. {@link GameInfoHud} depends
 * only on this interface, never on {@code GuiGraphics}/{@code PoseStack}/
 * {@code GuiGraphicsExtractor} directly - {@link PlatformCanvas} is the one
 * Stonecutter-branched file that implements it, mirroring the sibling
 * FlightHud mod's {@code FlightCanvas}/{@code PlatformCanvas} split (see
 * {@code PlatformCanvas}'s class doc for the full javap-confirmed shape of
 * each era this abstracts over).
 *
 * <p>Narrower than FlightHud's {@code FlightCanvas}: {@code GameInfoHud}
 * never pushes/pops a transform, translates, rotates, or scales - it only
 * fills, draws text, measures text, and draws a single item icon - so there
 * is nothing to abstract beyond these four operations. It also only ever
 * needs to draw plain pre-formatted {@code String} lines (every line is
 * already run through {@code String.format}/{@code TextCompat.translatableString}
 * before it reaches here), never a rich {@code Component} - so, unlike
 * FlightHud's canvas, this one takes {@code String} directly. {@code Font}
 * has a {@code String}-accepting overload of {@code draw}/{@code drawShadow}/
 * {@code width} at every version in the matrix (confirmed via {@code javap}),
 * so this costs nothing.
 */
public interface SimpleCanvas {
    void fill(int x1, int y1, int x2, int y2, int color);

    /** Draws {@code text} at (x, y); returns the drawn width in pixels. */
    int drawText(Font font, String text, int x, int y, int color, boolean shadow);

    int textWidth(Font font, String text);

    /**
     * Draws a single item icon at (x, y). Unsupported (no-op) on the
     * pre-1.20 implementation, matching this mod's pre-existing scope
     * decision that equipment info is text-only there (no icon) - see
     * {@code GameInfoHud}'s equipment-drawing code for the version split
     * that only ever calls this on 1.20+.
     */
    void drawItem(ItemStack stack, int x, int y);
}
