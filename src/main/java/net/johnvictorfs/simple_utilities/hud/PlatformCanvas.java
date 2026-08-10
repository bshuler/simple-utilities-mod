package net.johnvictorfs.simple_utilities.hud;

//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
*///?} elif >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}

import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;

/**
 * The ONE version-conditional file behind the {@link SimpleCanvas}
 * abstraction, mirroring the sibling FlightHud mod's {@code PlatformCanvas}
 * exactly (same three eras, same boundaries - keyed on MC version, never on
 * loader, since every loader draws through whichever of these three shapes
 * its own version of the game exposes).
 *
 * <p>Three real API shapes exist across the matrix (all confirmed via
 * {@code javap} against the real per-node jars, not assumption):
 * <ul>
 *   <li>{@code <1.20} (1.18.2, 1.19.4): no {@code GuiGraphics} class exists
 *       yet. Drawing goes through a raw {@code PoseStack} plus the static
 *       helper {@code GuiComponent.fill(PoseStack, ...)}; text goes through
 *       {@code Font.draw}/{@code drawShadow(PoseStack, Component, float,
 *       float, int)}, both returning the drawn width directly.</li>
 *   <li>{@code >=26.1} (26.2): {@code GuiGraphics} was removed outright and
 *       replaced by {@code GuiGraphicsExtractor}, a render-state-extraction
 *       object with renamed methods - {@code text(...)} (not {@code
 *       drawString(...)}, and returning {@code void} rather than the drawn
 *       width - computed manually here via {@code font.width(text)}) and
 *       {@code item(...)} (not {@code renderItem(...)}).</li>
 *   <li>else (1.20.1, 1.21.4): the "classic" {@code GuiGraphics} shape,
 *       {@code drawString(...)} (returns drawn width) and {@code
 *       renderItem(...)}.</li>
 * </ul>
 */
public final class PlatformCanvas implements SimpleCanvas {

    //? if <1.20 {
    /*private final PoseStack pose;

    public PlatformCanvas(PoseStack pose) {
        this.pose = pose;
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        GuiComponent.fill(pose, x1, y1, x2, y2, color);
    }

    @Override
    public int drawText(Font font, String text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            return font.drawShadow(pose, text, (float) x, (float) y, color);
        }
        return font.draw(pose, text, (float) x, (float) y, color);
    }

    @Override
    public int textWidth(Font font, String text) {
        return font.width(text);
    }

    @Override
    public void drawItem(ItemStack stack, int x, int y) {
        // Unsupported pre-1.20 - see SimpleCanvas#drawItem's javadoc. Never
        // called from this branch (GameInfoHud only draws the equipment icon
        // on 1.20+), so this is intentionally a no-op rather than a thrown
        // exception.
    }
    *///?} elif >=26.1 {
    /*private final GuiGraphicsExtractor context;

    public PlatformCanvas(GuiGraphicsExtractor context) {
        this.context = context;
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y2, color);
    }

    @Override
    public int drawText(Font font, String text, int x, int y, int color, boolean shadow) {
        context.text(font, text, x, y, color, shadow);
        return x + font.width(text);
    }

    @Override
    public int textWidth(Font font, String text) {
        return font.width(text);
    }

    @Override
    public void drawItem(ItemStack stack, int x, int y) {
        context.item(stack, x, y);
    }
    *///?} else {
    private final GuiGraphics context;

    public PlatformCanvas(GuiGraphics context) {
        this.context = context;
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y2, color);
    }

    @Override
    public int drawText(Font font, String text, int x, int y, int color, boolean shadow) {
        return context.drawString(font, text, x, y, color, shadow);
    }

    @Override
    public int textWidth(Font font, String text) {
        return font.width(text);
    }

    @Override
    public void drawItem(ItemStack stack, int x, int y) {
        context.renderItem(stack, x, y);
    }
    //?}
}
