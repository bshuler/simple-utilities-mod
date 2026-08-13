import gg.meza.stonecraft.mod

plugins {
    id("gg.meza.stonecraft")
    jacoco
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

dependencies {
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Tier 1 "loaded game" testing (Fabric cells only). fabric-loader-junit stands a real
    // Fabric loader up inside the JUnit worker, which is what makes it legal to call
    // SharedConstants.tryDetectVersion() + Bootstrap.bootStrap() and then assert against
    // genuinely loaded game data - see src/test/java/.../LoadedGameTest.java and PLAN.md.
    // NeoForge/Forge have no Loom-compatible equivalent (see the junit-fml note below).
    if (mod.isFabric) {
        testImplementation("net.fabricmc:fabric-loader-junit:0.19.3")
    }
}

// NeoForge's transitive net.neoforged.fancymodloader:junit-fml (<=9.0.18) auto-registers a
// LauncherSessionListener that looks up a run-config file "mainargs.txt" via a relative path
// that doesn't resolve under Gradle's test-worker working directory, failing `test` at
// JUnit-launcher startup with NoSuchFileException: mainargs.txt. Forcing the upstream-fixed
// junit-fml 10.0+ doesn't work either (NoClassDefFoundError:
// net/neoforged/fml/startup/StartupArgs - it needs a newer FML core than these NeoForge
// releases ship). This repo's tests are plain pure-logic JUnit tests that need no FML
// bootstrap at all, so junit-fml is excluded from the test runtime classpath. Same approach
// as the sibling critical-orientation / EasierVillagerTrading / FlightHud /
// simple-utilities-mod / ToroHealth repos.
//
// Caveat, recorded 2026-08-13 so nobody re-derives it: excluding junit-fml is the
// right call *for these repos*, not a universal one. junit-fml is precisely
// NeoForge's own loaded-test bootstrap - it is what stands FML up so a test can
// run against a real, loaded game. NeoForge's supported loaded-test path
// (`neoForge { unitTest { enable(); testedMod = ... } }`, the `testframework`
// artifact, `@ExtendWith(EphemeralTestServerProvider.class)`, `runGameTestServer`)
// is ModDevGradle-only, and this repo builds on Architectury Loom via Stonecraft,
// so that path is unavailable here regardless. Excluding junit-fml therefore costs
// nothing today. If a cell is ever migrated to ModDevGradle, this exclusion must
// be revisited before writing any loaded NeoForge test - it would silently disable
// the very bootstrap such a test depends on.
if (mod.isNeoforge) {
    // NeoForge's own POM pulls in fancymodloader's junit-fml, which registers a JUnit
    // Platform LauncherSessionListener that unconditionally expects a mainargs.txt
    // launch-args file (only ever produced for NeoForge's in-game gametest run
    // configs). This project's tests are plain, loader-agnostic pure-logic/config
    // tests - not in-game gametests - so pull it back off the test classpath. Mirrors
    // the fix already landed in the sibling FlightHud/critical-orientation mods; added
    // here proactively even though the active test project during Phase 2 is
    // 1.21.4-fabric (unaffected) - this repo does have 1.21.4-neoforge/26.2-neoforge
    // cells, and this keeps `test`/`check` from breaking on them later.
    configurations.named("testRuntimeClasspath") {
        exclude(group = "net.neoforged.fancymodloader", module = "junit-fml")
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

// JaCoCo scope: everything that touches a live Minecraft/loader singleton, GLFW,
// or a rendering primitive (Font/GuiGraphics/PoseStack/Component/translation) at
// class-load or call time is excluded - merely referencing such a class headless
// is unsafe/meaningless without a running game client. The genuinely pure,
// loader-agnostic logic (config persistence and HUD math/formatting) was extracted
// into (or already lived in) classes with zero Minecraft-class dependency and is
// fully unit-tested; see PLAN.md ("Phase 2: Test coverage") for the full per-class
// reasoning.
val jacocoExcludes = listOf(
    "net/johnvictorfs/simple_utilities/SimpleUtilities.class",
    "net/johnvictorfs/simple_utilities/SimpleUtilities$*.class",
    "net/johnvictorfs/simple_utilities/hud/GameInfoHud.class",
    "net/johnvictorfs/simple_utilities/hud/GameInfoHud$*.class",
    "net/johnvictorfs/simple_utilities/hud/HudHooks.class",
    "net/johnvictorfs/simple_utilities/hud/HudHooks$*.class",
    "net/johnvictorfs/simple_utilities/hud/PlatformCanvas.class",
    "net/johnvictorfs/simple_utilities/hud/PlatformCanvas$*.class",
    "net/johnvictorfs/simple_utilities/util/TextCompat.class",
    "net/johnvictorfs/simple_utilities/util/TextCompat$*.class",
)

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(classDirectories.files.map { fileTree(it) { exclude(jacocoExcludes) } })
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    classDirectories.setFrom(classDirectories.files.map { fileTree(it) { exclude(jacocoExcludes) } })
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "1.00".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// Forge's pack.mcmeta generation task writes into the main source set's resources
// output without declaring that as a tracked task output, which Gradle's task
// validation flags as an undeclared ("implicit") dependency on :compileTestJava
// (which consumes sourceSets.main.output as part of the test compile classpath).
// Declare the dependency explicitly so validation/chiseledBuild doesn't hit a
// task-graph ordering failure. tasks.matching(...) is a live/lazy filter, so this
// is a harmless no-op on loaders that don't have a generatePackMCMetaJson task
// (e.g. Fabric).
tasks.matching { it.name == "compileTestJava" }.configureEach {
    dependsOn(tasks.matching { it.name == "generatePackMCMetaJson" })
}
