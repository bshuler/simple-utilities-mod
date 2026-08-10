# Simple HUD Utilities - Modernization Plan

## Goal

Get this fork of `johnvictorfs/simple-utilities-mod` building (not
necessarily running/tested at runtime) across a modern Minecraft
version×loader matrix, following the same Stonecutter/Stonecraft house
template used across this program's other mods (`critical-orientation`,
`FlightHud`, `EasierVillagerTrading`, `ToroHealth`).

"Build green" is the bar for this pass - compiled and packaged
successfully, jar contents verified with `unzip -l`, not launched in-game.

## Upstream check (done before any porting)

`upstream` (`johnvictorfs/simple-utilities-mod`) was reviewed before any
porting started. Upstream marked itself End-of-Life and pointed users at a
different mod; its last substantive commits topped out around Minecraft
1.20.6. Upstream's history was merged into this fork's `main` as an ordinary
forward merge (never rebased, never force-pushed) prior to the Stonecutter
migration commit (`da43a8b`). `upstream` remote is kept intact for future
reference; nothing is ever pushed to it.

## Newest stable Minecraft version (checked live, not from memory)

Checked against `https://meta.fabricmc.net/v2/versions/game` during this
pass: **26.2** is newest stable. Loader/dependency versions were also
checked live rather than trusted from training data:

- Fabric Loader: `0.19.3` (`https://meta.fabricmc.net/v2/versions/loader/26.2`)
- Fabric API: `0.156.0+26.2` (matches the version already cached locally
  from the sibling FlightHud mod's own 26.2 port, and confirmed identical in
  FlightHud's `versions/dependencies/26.2.properties`)
- NeoForge: `26.2.0.58` (last non-beta-suffixed entry in NeoForge's maven
  `maven-metadata.xml`; `.59` and later are beta-tagged)

## Target matrix

| MC version | Fabric | Forge | NeoForge |
|---|---|---|---|
| 1.18.2 | full feature | full feature | n/a (pre-NeoForge) |
| 1.19.4 | full feature | full feature | n/a |
| 1.20.1 | full feature | full feature | n/a |
| 1.21.4 | full feature | n/a (post-Forge/pre-NeoForge split handled by version, Forge ended at 1.20.4-era) | full feature |
| 26.2 | full feature (see "26.2" below) | n/a | full feature |

Loader coverage is mandatory per this program's coordinator: Fabric+Forge
for MC 1.20.4 and older, Fabric+NeoForge for MC 1.20.5+ (Forge itself ended
at the NeoForge split and has no 1.21.4/26.2 release to target). Quilt is
**not** a separate cell - it runs Fabric jars natively, so Fabric coverage
already covers it; this is documented rather than wired in as its own
Gradle subproject.

"Full feature" = HUD-render overlay + toggle keybinding. Every cell in the
matrix now carries it, wired through the loader-agnostic `HudHooks`/
`PlatformCanvas`/`SimpleCanvas` abstraction (no Mixin, no per-loader
divergence in `GameInfoHud.java` itself) - see "Multi-loader scope
decisions" and "26.2" below for how the previous config-only-stub state was
closed out.

## Multi-loader scope decisions (see CLAUDE.md "Multi-loader scope decisions")

**Superseded this pass.** Forge/NeoForge never got the HUD feature in the
original Stonecutter migration - it was Yarn-mapped end-to-end via a Mixin
into `InGameHud.render`, and porting *that* code to Mojang mappings would
have meant re-deriving nearly all of `GameInfoHud.java` (~380 lines touch
Yarn-mapped types throughout the body, not just the render entrypoint).
Rather than attempt that re-derivation, the mod adopted the sibling
FlightHud mod's proven no-Mixin pattern instead: `HudHooks` registers
against each loader's own official HUD-render hook (Fabric
`HudRenderCallback`/`HudElementRegistry` depending on era, NeoForge
`RenderGuiEvent.Post`, Forge `OverlayRegistry`/`RegisterGuiOverlaysEvent`
depending on era), each handing a `PlatformCanvas` (the one
version-conditional file) to a rewritten, loader-agnostic
`GameInfoHud.java` that draws through the `SimpleCanvas` interface. The
whole mod migrated to uniform Mojang-official mappings in the same pass
(dropping `yarn_mappings` everywhere), which is what made a single
loader-agnostic `GameInfoHud` possible in the first place. Forge/NeoForge
are no longer stubs; every cell now ships the real feature. See "26.2"
below for the concrete before/after and the class-count proof.

## 26.2

26.2 is the coordinator-mandated newest target. It was **not** simply
addable as another full-feature Fabric cell using the mod's original
Yarn-mapped, Mixin-based HUD code, for a concrete reason found while
investigating: Fabric stopped publishing Yarn mappings after 1.21.11
(26.1+ is Mojang-official-only), and 26.1+ also removed
`GuiGraphics`/`DrawContext` from the client entirely, replacing them with
an internal `Gui.extractRenderState(...)` → `GuiRenderState` model that
isn't meant to be hooked directly (and isn't a stable Mixin target the way
`InGameHud.render` was).

A first pass shipped 26.2-fabric (and Forge/NeoForge generally) as
config-only stubs, deferring the real port. **The coordinator rejected
that as an acceptable end state** ("a jar that loads but drops the HUD
feature isn't 'supporting' that loader") and required the full port on
every cell. That port is now done:

- The whole mod migrated to uniform Mojang-official mappings (dropping
  `yarn_mappings` from every `versions/dependencies/*.properties` file and
  the per-cell `versions/1.21.4-fabric/gradle.properties` override that
  set it). This is what made a single, loader-agnostic `GameInfoHud.java`
  possible instead of three divergent Yarn/Mojmap copies.
- `GameInfoHud.java` was rewritten from scratch against Mojang-mapped
  types (`Minecraft`, `Font`, `LocalPlayer`, `Level`, `ItemStack`,
  `Inventory`, `ServerData`, etc., all confirmed via `javap` against real
  merged jars at both the 1.18.2 and 26.2 extremes) and now draws
  exclusively through the `SimpleCanvas` interface - no direct
  `DrawContext`/`MatrixStack`/`GuiGraphicsExtractor` calls in the shared
  body at all.
- `PlatformCanvas.java` is the **one** version-conditional file behind
  `SimpleCanvas`, with exactly three real API shapes across the matrix
  (confirmed via `javap`, not assumption): `<1.20` (raw `PoseStack` +
  `GuiComponent.fill`/`Font.draw`/`drawShadow`), `>=26.1`
  (`GuiGraphicsExtractor`'s renamed `text(...)`/`item(...)`), and the
  "classic" `GuiGraphics` shape everywhere in between (1.20.1, 1.21.4).
- `HudHooks.java` registers `GameInfoHud` against each loader's own
  official HUD-render hook per era: Fabric `HudRenderCallback` (`<1.21.2`),
  a `GuiGraphics`-only `HudRenderCallback` variant (`<26.1`), Fabric
  `HudElementRegistry` (`>=26.1`); NeoForge `RenderGuiEvent.Post`; Forge
  `OverlayRegistry`/`ForgeIngameGui` (`<1.19`) or
  `RegisterGuiOverlaysEvent`/`ForgeGui` (`>=1.19`).
- `TextCompat.java` carries the `Component`-returning Mojmap branch
  alongside the split needed for `ResourceKey.location()` (pre-26.1) vs
  `.identifier()` (26.1+).
- The now-unused Mixin infrastructure (`GameInfoMixin.java`,
  `GameClientMixin.java`, `simple_utilities.mixins.json`, and the
  `"mixins"` key in `fabric.mod.json`) was deleted rather than left as
  dead weight alongside the new no-Mixin path.

**Result, verified via `chiseledBuild` + `unzip -l` (see "Phase 4" below
for the full per-cell table)**: every cell - including every Forge and
NeoForge cell, and 26.2-fabric - now packages the same 8 mod classes
(`SimpleUtilities`, `SimpleUtilitiesConfig`, `Colors`, `GameInfoHud`,
`HudHooks`, `PlatformCanvas`, `SimpleCanvas`, `TextCompat`), closing the
previous 8-classes-on-Fabric-only vs. 3-classes-everywhere-else gap
entirely. No stub cells remain anywhere in the matrix.

### Forgix - single merged all-loader jar (not wired in this pass)

Investigated as part of this program's "ALL-LOADER REQUIREMENT" - whether a
single merged jar (Fabric+Forge+NeoForge in one file, via the **Forgix**
Gradle plugin, `io.github.pacifistmc.forgix` / `PacifistMC/Forgix`) makes
sense for this mod. Forgix is alive (`2.0.0` stable tag). It repackages each
loader's already-compiled classes/resources into one jar with per-loader
manifests, applied at the true Gradle root (not per-Stonecutter-cell) with
one `forgix { }` block per Minecraft version, and needs explicit `inputJar`
overrides since Stonecutter's `<mc>-<loader>` cell naming (e.g.
`26.2-neoforge`) doesn't match Forgix's flat `:fabric`/`:forge` autodetection
convention. No existing public example wires Forgix through a Stonecutter/
Stonecraft project, so this would be first-of-its-kind wiring, not a
copy-paste.

**Decision**: not wired in this pass, mirroring the same decision made for
EasierVillagerTrading. Now that every Forge/NeoForge cell carries the real
HUD feature (see "26.2" above), a Forgix-merged jar is a more plausible
future increment than it was when Forge/NeoForge were stubs - but it's
still a first-of-its-kind wiring exercise for this house template and
remains out of scope for this pass. Revisit if a future pass wants one
merged all-loader jar instead of ten per-cell ones.

## Status checklist

### Phase 0 - Docs & repo hygiene
- [x] Fork provenance confirmed, `upstream` merged forward, remotes intact.
- [x] `CLAUDE.md` written (architecture, porting notes, hotspots).
- [x] `PLAN.md` written (this file).

### Phase 1 - Stonecutter scaffold
- [x] Migrated from upstream's single-version Loom build to
      Stonecutter + Stonecraft (commit `da43a8b`, prior pass).
- [x] Per-cell `gradle.properties` / `versions/dependencies/*.properties`
      wired for Yarn mappings where needed, Mojang-official fallback
      elsewhere.

### Phase 2 - Port latest-first, walk backwards
- [x] 1.21.4 fabric + neoforge - green.
- [x] 1.20.1 fabric + forge - green.
- [x] 1.19.4 fabric + forge - green.
- [x] 1.18.2 fabric + forge - green.
- [x] Cross-version hotspots found and fixed (InGameHud constructor "hump",
      debug-check version split, RegistryKey package rename) - see
      CLAUDE.md.

### Phase 3 - Loaders
- [x] Forge/NeoForge full HUD+keybinding port completed and verified on
      every Forge/NeoForge cell (superseded the original stub scope
      decision - see "Multi-loader scope decisions" and "26.2" above).
- [x] Quilt documented as covered by Fabric (no separate cell needed).
- [ ] Forgix - investigated, deliberately not wired in this pass (see
      "Forgix" above).

### Phase 3.5 - 26.2 addition (coordinator-mandated)
- [x] Live-checked newest stable MC (26.2) and its loader/dependency
      versions (Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, NeoForge
      26.2.0.58).
- [x] `mc("26.2", "fabric", "neoforge")` added to `settings.gradle.kts`.
- [x] `versions/dependencies/26.2.properties` created.
- [x] Full HUD-render port to 26.2 (and every other cell) via FlightHud's
      `HudHooks`/`PlatformCanvas`/`SimpleCanvas` pattern, migrating the
      whole mod to uniform Mojang-official mappings - done, not deferred.
      26.2-fabric is no longer a stub; it carries the identical 8-class
      feature set as every other cell.
- [x] All cells (26.2-fabric, 26.2-neoforge, and the other 8) build green
      individually and via `chiseledBuild`; jar contents verified via
      `unzip -l`.

### Phase 4 - Verification
- [x] All 10 cells (1.18.2/1.19.4/1.20.1 fabric+forge, 1.21.4/26.2
      fabric+neoforge) build green via `chiseledBuild`.
- [x] Jar contents verified via `unzip -l` for every cell, not just
      "BUILD SUCCESSFUL" text - **every** cell (Fabric, Forge, and
      NeoForge alike) now carries the same 8 mod classes
      (`SimpleUtilities`, `SimpleUtilitiesConfig`, `Colors`, `GameInfoHud`,
      `HudHooks`, `PlatformCanvas`, `SimpleCanvas`, `TextCompat`) - the
      previous 8-classes-Fabric-only vs. 3-classes-stub gap is closed on
      every cell, no green-but-stub jars remain anywhere in the matrix.

## Phase 2 (test coverage): JUnit 5 + JaCoCo

Added a JUnit 5 + JaCoCo test suite to the active Stonecutter cell only
(`1.21.4-fabric`, matching this repo's `vcsVersion` - tests are never run
across the full matrix; switch cells only via
`./gradlew "Set active project to <cell>"`, never by hand-editing
`stonecutter.gradle.kts`). Wiring mirrors the canonical pattern already
landed in `critical-orientation`/`FlightHud`: `jacoco` plugin,
`testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")`, a shared
`jacocoExcludes` file-pattern list applied to both `jacocoTestReport` and
`jacocoTestCoverageVerification`, a `LINE`/`COVEREDRATIO` minimum of
`1.00`, and `check` `dependsOn` the verification task.

### Result: 100% LINE coverage, enforced, `check` green

`./gradlew ":1.21.4-fabric:check"` is BUILD SUCCESSFUL with the
verification rule set to `1.00` (not a lowered bar). 38 tests, 0 failures,
across 3 test classes. `--info` confirms JaCoCo actually analyzed a
nonzero, correct class count (not a false-green empty-bundle run - the
prior FlightHud program's GOTCHA (t)): `Writing bundle '1.21.4-fabric'
with 3 classes`, matching the 3 classes below.

### Included (fully tested, 100% line coverage)

| Class | Why it's testable | Test file |
|---|---|---|
| `hud/GameInfoMath.java` (**new**) | Pure static math/formatting extracted from `GameInfoHud` - zero Minecraft imports, zero Stonecutter conditionals, identical on every cell. Mirrors FlightHud's `FlightComputerMath` extraction pattern. | `GameInfoMathTest.java` (27 tests) |
| `config/SimpleUtilitiesConfig.java` | Already Minecraft-import-free by design (see its own class javadoc) - plain `Properties`-backed persistence. | `SimpleUtilitiesConfigTest.java` (9 tests) |
| `helpers/Colors.java` | Plain `int` constants; the only coverable bytecode is the implicit default constructor (javac-synthesized, since the class isn't `final`/has no explicit ctor) - one trivial `new Colors()` test covers it. | `ColorsTest.java` (1 test) |

`hud/SimpleCanvas.java` (pure interface, only abstract methods) needs no
JaCoCo exclusion either - no coverable bytecode, same as FlightHud's
analogous `FlightCanvas`.

### Excluded (JaCoCo `jacocoExcludes`, documented per class)

| Class | Why excluded |
|---|---|
| `SimpleUtilities.java` | Entry point; every branch (Fabric `ClientModInitializer`, NeoForge/Forge `@Mod` constructors) touches live loader/KeyMapping/GLFW singletons at construction time - genuinely unsafe/meaningless to instantiate headless. |
| `hud/GameInfoHud.java` | Rendering/state class - draws via `SimpleCanvas`, reads live `LocalPlayer`/`ItemStack`/`Level` state. All its pure math/formatting was extracted into `GameInfoMath` (see above); what's left is Minecraft-state glue only. |
| `hud/HudHooks.java` | Registers against each loader's official HUD-render hook (`HudRenderCallback`/`HudElementRegistry`/`RenderGuiEvent.Post`/`RegisterGuiOverlaysEvent`) - loader-glue, nothing to unit-test without a running client. |
| `hud/PlatformCanvas.java` | The one version-conditional `SimpleCanvas` implementation; every branch calls a real `GuiGraphics`/`PoseStack`/`Font` drawing API - untestable headless. |
| `util/TextCompat.java` | See "Cross-cell test-safety trap" below - initially thought testable (an active-cell-only spike passed), but proven unsafe once checked against the full matrix, not just the active cell. |

### Cross-cell test-safety trap found while verifying (worth flagging for sibling mods)

An earlier pass wrote `TextCompatTest.java` to cover `TextCompat`'s
`Component`/`ResourceKey`-touching methods, reasoning that a passing spike
test against the *active* cell (`1.21.4-fabric`) proved it "runs headless."
That reasoning was **wrong**, caught only by the mandated end-of-work
`chiseledBuild` regression: `src/test/java` is Stonecutter-generated
per-cell exactly like `src/main/java` (evidenced by the generated paths
under `versions/<cell>/build/generated/stonecutter/test/java/...`), and
`build`/`check` are wired identically on every cell - so any test that
merely *compiles* against the active cell's jar can still break 8 of the
other 9 cells at either compile time or class-init time:

- `net.minecraft.core.registries.Registries` /
  `ResourceLocation.fromNamespaceAndPath(...)` don't exist pre-1.21 -
  **compile** failures on 1.18.2/1.19.4-fabric.
- Even after switching to the supposedly-stable, no-conditionals-needed
  `Level.OVERWORLD`/`Level.NETHER` constants, the test still failed at
  **runtime** (`ExceptionInInitializerError` /
  `NoClassDefFoundError`/`IllegalStateException`/`IllegalArgumentException`)
  on every cell *except* 1.21.4-fabric and 1.21.4-neoforge - `Level`'s
  static initializer evidently depends on registry/bootstrap state that
  differs release to release, and headless JUnit provides none of it.

Net lesson: passing on the active cell is **not sufficient evidence**
that a Minecraft-class-touching method is headless-safe across a
multi-version matrix - only a real `chiseledBuild` run (or equivalent
per-cell `test` run) can prove that, and until proven, treat *any* class
that imports a Minecraft package as excluded rather than "probably fine."
`TextCompat` was reverted to `jacocoExcludes`; the `Registries`-based spike
test and its `TextCompatTest.java` successor were both deleted rather than
left half-working. This is a stronger version of GOTCHA (t) worth carrying
into sibling mod briefs: verify "does it run headless" against the *whole
matrix*, not just the cell you happen to be iterating on.

No watcher/poller background thread exists anywhere in this mod (checked:
`grep -rn "Thread\|ScheduledExecutor\|Executors\|Runnable" src/main/java`
returned nothing) - the FlightHud-derived "watcher thread try-with-resources
coverage gap" gotcha does not apply here.

### Bug found while writing tests

`GameInfoMath.secondsToString(int)` (carried over unchanged from
`GameInfoHud`'s old private method) is dead code - `grep -rn
"secondsToString" src/main/java` shows it is never called anywhere in
production. Not deleted (out of scope for a behavior-preserving refactor,
and deletion wasn't requested), but flagged here and directly unit-tested
since it's still reachable, harmless pure logic.

### Forge/NeoForge test-infra gotchas (added proactively, not yet exercised)

Since Phase 2 test tooling only ran against the active `1.21.4-fabric`
cell, these two known upstream/Stonecraft gotchas (seen on sibling mods)
were wired defensively but not empirically triggered here:
- NeoForge's `junit-fml` module registers a `mainargs.txt`-expecting JUnit
  Platform listener that breaks plain (non-gametest) unit tests - excluded
  from `testRuntimeClasspath` via `if (mod.isNeoforge) { ... }` in
  `build.gradle.kts`.
- Forge's `generatePackMCMetaJson` task has an undeclared/implicit
  dependency relationship with `compileTestJava` - declared explicitly via
  `tasks.matching { it.name == "compileTestJava" }.configureEach {
  dependsOn(tasks.matching { it.name == "generatePackMCMetaJson" }) }`.

### Folia

Folia n/a - client mod.

## Notes log (append as work proceeds; keep dated)

- **2026-08-10**: Fixed four Yarn-API mismatches blocking the original
  8-cell matrix (`InGameHud` constructor "hump", debug-check version split,
  `RegistryKey` package rename, a duplicate `@Mod` annotation in
  `SimpleUtilities.java`). Confirmed via `javap` against real merged Yarn
  jars rather than assumption. Committed as `07ed9c5`, pushed to
  `origin/main`. (This commit had been parked for several sessions on an
  unattended 1Password commit-signing prompt - retried successfully this
  session per the standing "pause, don't poll" rule, with no bypass of
  signing at any point.)
- **2026-08-10**: Added Minecraft 26.2 (Fabric + NeoForge) to the matrix per
  coordinator instruction, following FlightHud's now-completed 26.2 pattern
  as the reference for *why* the scope decision below was made (though not
  yet for a full port - see "26.2" section above). Scoped 26.2-fabric as a
  config-only stub, matching Forge/NeoForge, rather than attempting a full
  HUD-render port in the same pass; documented the follow-up plan using
  FlightHud's `HudHooks`/`PlatformCanvas` pattern. Both new cells verified
  green via `chiseledBuild` + `unzip -l`; full 10-cell sweep re-run with no
  regressions to the 8 previously-green cells. Committed as `a095ee2`,
  pushed to `origin/main`.
- **2026-08-10**: Wrote `CLAUDE.md`/`PLAN.md` for this fork (this commit) -
  overdue since the original Stonecutter migration; documents the fork
  provenance, architecture, cross-version hotspots, the 26.2 scope decision
  and its rejected alternative, and the Forgix investigation/deferral.
- **2026-08-10**: Closed out the config-only-stub gap the coordinator
  rejected as an unacceptable end state. Migrated the whole mod to uniform
  Mojang-official mappings (removed `yarn_mappings` from every
  `versions/dependencies/*.properties` file and deleted the
  `versions/1.21.4-fabric/gradle.properties` override); rewrote
  `GameInfoHud.java` from scratch against Mojmap types, drawing only
  through `SimpleCanvas`; ported FlightHud's `HudHooks`/`PlatformCanvas`
  pattern so every loader (Fabric all eras, NeoForge, Forge both eras)
  registers the real HUD-render feature; deleted the now-unused
  `GameInfoMixin.java`/`GameClientMixin.java`/`simple_utilities.mixins.json`
  and the `"mixins"` key in `fabric.mod.json`. Hit and fixed a build
  failure isolated to the Stonecutter *active* cell (`1.21.4-fabric`,
  which compiles `src/main/java` directly with no generation step): the
  raw file had drifted out of sync with 1.21.4-fabric's actual conditions
  after hand-editing, leaving two branches of the same `//? if/elif` chain
  live simultaneously in a couple of spots (a Fabric branch and a
  Forge-only branch both uncommented) - fixed via the sanctioned
  `./gradlew "Set active project to 1.21.4-fabric"` re-toggle, not a hand
  edit of the comment markers. Verified via `chiseledBuild` (all 10 cells
  green) and `unzip -l` on every cell's jar: all 10 now carry the identical
  8 mod classes (`SimpleUtilities`, `SimpleUtilitiesConfig`, `Colors`,
  `GameInfoHud`, `HudHooks`, `PlatformCanvas`, `SimpleCanvas`,
  `TextCompat`) - the previous 8-vs-3 gap is fully closed, no stub cells
  remain.
- **2026-08-10**: Phase 2 (test coverage). Extracted `hud/GameInfoMath.java`
  (pure math/formatting) out of `GameInfoHud.java`; added JUnit 5 + JaCoCo
  wiring to `build.gradle.kts` (mirroring `critical-orientation`/
  `FlightHud`'s canonical pattern, `LINE`/`COVEREDRATIO` = `1.00`); wrote
  `GameInfoMathTest`, `SimpleUtilitiesConfigTest`, `ColorsTest` (38 tests
  total) against the active `1.21.4-fabric` cell. `./gradlew
  ":1.21.4-fabric:check"` green at 100% line coverage across the 3
  included classes; `GameInfoHud`/`HudHooks`/`PlatformCanvas`/
  `SimpleUtilities`/`TextCompat` excluded and documented (rendering/
  loader-glue/version-fragile, genuinely untestable headless) - see "Phase
  2 (test coverage)" above for the full table. Initially attempted a
  `TextCompatTest.java` on the theory that an active-cell-only spike
  proved it headless-safe; the mandated `chiseledBuild` regression caught
  that this was wrong (broke 8 of the other 9 cells at compile- or
  runtime) - reverted, documented as a cross-cell test-safety trap. Found
  (and left intact, documented) one piece of dead code: `secondsToString`
  is unused in production. Folia n/a (client mod). NeoForge junit-fml and
  Forge generatePackMCMetaJson/compileTestJava gotchas wired proactively
  though not exercised (active cell is Fabric). Full 10-cell
  `chiseledBuild` reconfirmed green after the revert.
- **Open risk, not remediated this pass**: this machine's boot volume has
  been observed at ~98% used (roughly 11-13GB free) across this program's
  work on several sibling mods, and has previously caused Loom/toolchain
  cache corruption requiring manual cleanup. Not this mod-specific and out
  of scope to fix here, but flagged for whoever picks up the next pass.
