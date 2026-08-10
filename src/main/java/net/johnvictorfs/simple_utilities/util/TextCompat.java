package net.johnvictorfs.simple_utilities.util;

import net.minecraft.resources.ResourceKey;

//? if >=1.19 {
import net.minecraft.network.chat.Component;
//?} else {
/*import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
*///?}

/**
 * Bridges the Mojmap translatable/literal-text API across the whole
 * version×loader matrix. {@code Component.translatable(key, args...)} /
 * {@code Component.literal(text)} are the modern static factories on the
 * {@code Component} interface itself (confirmed via {@code javap} present
 * from 1.19.4 onward, identical signature through 26.2); on 1.18.2 there are
 * no such factories on {@code Component} at all - the equivalents are the
 * {@code TranslatableComponent}/{@code TextComponent} constructors (both
 * {@code extends BaseComponent}). Keeping this in one place means
 * {@code GameInfoHud}/{@code SimpleUtilities} don't need their own
 * per-version branches for every translation/literal call.
 *
 * <p>Loader-agnostic and used by every cell in the matrix (Fabric, Forge,
 * NeoForge) now that the whole mod builds against uniform Mojang-official
 * mappings - unlike the old Yarn-mapped version of this class, this one is
 * not gated to Fabric at all.
 */
public final class TextCompat {
    private TextCompat() {
    }

    //? if >=1.19 {
    public static Component translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static Component literal(String text) {
        return Component.literal(text);
    }
    //?} else {
    /*public static BaseComponent translatable(String key, Object... args) {
        return new TranslatableComponent(key, args);
    }

    public static BaseComponent literal(String text) {
        return new TextComponent(text);
    }
    *///?}

    public static String translatableString(String key, Object... args) {
        return translatable(key, args).getString();
    }

    /**
     * {@code net.minecraft.resources.ResourceLocation} was renamed to
     * {@code net.minecraft.resources.Identifier} at 26.1+ (matching Fabric's
     * own {@code Identifier} naming - confirmed via {@code javap}), and
     * {@code ResourceKey#location()} was renamed to {@code identifier()} in
     * lockstep. {@code ResourceKey} itself (this method's parameter type)
     * keeps the same name/package at every version, so these two methods are
     * the only version-split needed to read a key's namespace/path - used by
     * {@code GameInfoHud} for the dimension id (nether-coordinate
     * conversion) and the biome registry key (biome name line).
     */
    //? if >=26.1 {
    /*public static String keyNamespace(ResourceKey<?> key) {
        return key.identifier().getNamespace();
    }

    public static String keyPath(ResourceKey<?> key) {
        return key.identifier().getPath();
    }
    *///?} else {
    public static String keyNamespace(ResourceKey<?> key) {
        return key.location().getNamespace();
    }

    public static String keyPath(ResourceKey<?> key) {
        return key.location().getPath();
    }
    //?}
}
