package net.johnvictorfs.simple_utilities.util;

//? if fabric && <26.1 {
import net.minecraft.text.Text;
//? if >=1.19 {
//?} else {
/*import net.minecraft.text.TranslatableText;
*///?}
//?}

/**
 * Bridges the Yarn-mapped translatable-text API across the fork's Fabric
 * version range. {@code Text.translatable(key, args...)} is the modern
 * factory method (introduced ~1.19); on 1.18.2 the equivalent is the
 * {@code TranslatableText} constructor directly. Keeping this in one place
 * means {@code GameInfoHud}/{@code SimpleUtilities} don't need their own
 * per-version branches for every translation call.
 *
 * <p>Fabric-only (and only below 26.1): Forge/NeoForge cells, and Fabric
 * 26.1+ (which stopped publishing Yarn mappings), don't wire in the
 * HUD-rendering feature at all (see PLAN.md), so this class is guarded to
 * compile to an empty (package-only) file on those targets, matching the
 * house pattern used by EasierVillagerTrading's mixin classes.
 */
//? if fabric && <26.1 {
public final class TextCompat {
    private TextCompat() {
    }

    public static Text translatable(String key, Object... args) {
        //? if >=1.19 {
        return Text.translatable(key, args);
        //?} else {
        /*return new TranslatableText(key, args);
        *///?}
    }

    public static String translatableString(String key, Object... args) {
        return translatable(key, args).getString();
    }
}
//?}
