plugins {
    id("gg.meza.stonecraft")
}

modSettings {
    clientOptions {
        fov = 90
        guiScale = 2
        narrator = false
        darkBackground = true
        musicVolume = 0.0
    }
}

// No publishMods block: this fork is not published to Modrinth/CurseForge
// (out of scope per project policy), and EVT's own build.gradle.kts - the
// reference this file was modeled on - has no such block either.

// No manual `dependencies { mappings(...) } }` block here: Stonecraft's own
// DependenciesKt.configureDependencies (decompiled and verified) ALREADY adds
// Yarn mappings automatically for any cell where mod.hasProp("yarn_mappings")
// is true, and falls back to Loom's officialMojangMappings() otherwise. This
// mod's ported source (inherited from upstream johnvictorfs/simple-utilities-mod)
// uses Yarn/Fabric-intermediary names throughout (net.minecraft.text.Text,
// InGameHud, MinecraftClient, DrawContext, etc.) in its Fabric-cell code, so
// yarn_mappings is set per-version in versions/dependencies/*.properties (or,
// for 1.21.4, in a per-cell versions/1.21.4-fabric/gradle.properties override -
// see that file for why). NeoForge cells never get yarn_mappings: Stonecraft's
// auto-wiring needs an extra "yarn_mappings_neoforge_patch"-driven
// dev.architectury:yarn-mappings-patch-neoforge layer for NeoForge, and that
// artifact is only published up to MC 1.21 - nothing exists for 1.21.4+. Legacy
// Forge cells (1.18.2/1.19.4/1.20.1) don't need a patch layer at all and are
// fine sharing yarn_mappings with their Fabric sibling's properties file.
