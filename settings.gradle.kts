pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("gg.meza.stonecraft") version "1.12.+"
    id("dev.kikugie.stonecutter") version "0.9.+"
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    shared {
        fun mc(version: String, vararg loaders: String) {
            for (loader in loaders) {
                version("$version-$loader", version)
            }
        }

        // Target versions and loaders - see PLAN.md for status/rationale per
        // cell. Loader coverage is mandatory: Fabric + NeoForge for 1.20.5+,
        // Fabric + Forge for 1.20.4 and older (Forge ended at the NeoForge
        // split). Quilt is not a separate cell - it runs Fabric jars natively.

        // Newest stable Minecraft per https://meta.fabricmc.net/v2/versions/game
        // (checked live - do not trust training-data memory) is 26.2. 26.1+
        // rewrote the client GUI rendering pipeline (GuiGraphics/DrawContext
        // replaced by a GuiRenderState-based state-extraction model) and
        // stopped publishing Yarn mappings entirely (Mojang-official names
        // only from here on). The sibling FlightHud mod proved the fix for
        // this exact situation: don't reverse-engineer GuiRenderState, hook
        // each loader's own official HUD-render event/registry instead
        // (Fabric HudElementRegistry/HudRenderCallback, NeoForge
        // RenderGuiEvent.Post). This mod's own HUD-render code (GameInfoHud/
        // GameInfoMixin) has NOT been ported to that pattern yet - it's
        // Yarn-mapped and Mixin-based, so it's scoped out of 26.2-fabric for
        // now the same way it's already scoped out of Forge/NeoForge (config
        // load only). See PLAN.md "26.2" section for the follow-up port plan.

        // 26.2 - Fabric + NeoForge (newest stable; HUD render deferred, see above)
        mc("26.2", "fabric", "neoforge")
        // 1.21.4 - Fabric + NeoForge (last classic-numbered Yarn-mapped version)
        mc("1.21.4", "fabric", "neoforge")
        // 1.20.1 - Fabric + Forge (last major Forge version before NeoForge split)
        mc("1.20.1", "fabric", "forge")
        // 1.19.4 - Fabric + Forge
        mc("1.19.4", "fabric", "forge")
        // 1.18.2 - Fabric + Forge
        mc("1.18.2", "fabric", "forge")

        vcsVersion = "1.21.4-fabric"
    }
    create(rootProject)
}

rootProject.name = "simple-utilities-mod"
