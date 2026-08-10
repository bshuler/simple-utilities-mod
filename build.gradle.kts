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

// No manual `dependencies { mappings(...) } }` block here, and no
// yarn_mappings property anywhere in versions/dependencies/*.properties any
// more: the whole mod (SimpleUtilities.java, GameInfoHud.java, HudHooks.java,
// PlatformCanvas.java, TextCompat.java) was migrated off Yarn to uniform
// Mojang-official mappings across every cell - Fabric, Forge, and NeoForge
// alike - following the sibling FlightHud mod's proven no-Mixin,
// loader-official-hook pattern (HudHooks/PlatformCanvas canvas abstraction).
// This was required to give 26.2-fabric the real feature at all (Yarn
// mappings stopped being published there) and, as a side effect, let every
// other cell drop the old Yarn/Fabric-intermediary names too - Stonecraft
// simply falls back to Loom's officialMojangMappings() everywhere now that
// mod.hasProp("yarn_mappings") is false on every cell.
