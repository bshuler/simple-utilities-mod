# CLAUDE.md - Simple HUD Utilities Minecraft Mod

## Project Overview

Simple HUD Utilities (`net.johnvictorfs.simple_utilities`) is a fork of
[`johnvictorfs/simple-utilities-mod`](https://github.com/johnvictorfs/simple-utilities-mod),
this fork living at `bshuler/simple-utilities-mod`. Upstream marked the mod
End-of-Life and pointed users at an alternative, but it still builds cleanly
and this fork keeps it alive across modern Minecraft versions/loaders as part
of a broader "keep Bert's mods compiling on current Minecraft" effort.

The mod draws a small on-screen HUD overlay (coordinates/facing, FPS, biome,
game time, nether-coordinate conversion, held-item durability, sprint
indicator) and a config file to toggle it. It is Fabric-only in behavior;
Forge/NeoForge cells currently build but only load config (see "Multi-loader
scope decisions" below).

## Fork Provenance

- `origin` = `bshuler/simple-utilities-mod` (this fork, where all work lands).
- `upstream` = `johnvictorfs/simple-utilities-mod` (read-only reference; never
  force-pushed, never pushed to at all - upstream is EOL and not accepting
  changes back).
- Upstream was checked before porting; upstream's last real commits topped
  out around 1.20.6 and its own EOL notice. Its history was merged into `main`
  as a forward merge (not rebased, not force-pushed) before this fork's own
  Stonecutter migration commit (`da43a8b`).

## Architecture

Migrated from upstream's single-version Loom build to a
**Stonecutter + Stonecraft** multi-version matrix, mirroring the house
template used by `critical-orientation` (primary reference) and
`EasierVillagerTrading`/EVT (secondary reference, especially for the
Fabric-first/Forge-NeoForge-stub split pattern this mod also uses).

Key structural facts specific to this Stonecraft/Loom setup (worth stating
explicitly, since they are easy to get wrong by analogy to other Loom
projects):

- **`src/main/java` is the one real source tree.** There is no
  `src/client/java` sourceSet in this template - all MC-importing code
  (including client-only HUD/keybinding code) lives under `src/main/java`,
  gated with `//? if fabric { ... }`-style Stonecutter conditionals rather
  than sourceSet separation.
- **The Stonecutter "active" version compiles `src/main/java` directly.**
  Every other version in the matrix gets its own independently generated
  copy under `versions/<mc>-<loader>/build/generated/stonecutter/main/java/...`
  - so a change only "counts" as tested against the active cell until you
    switch active or run `chiseledBuild`.
- **Mappings default to Mojang official**, not Yarn, unless a cell's
  `gradle.properties` (or its `versions/dependencies/<mc>.properties`) sets
  `yarn_mappings`. This fork's Fabric-cell HUD code is written against
  Yarn/intermediary names (inherited from upstream), so `yarn_mappings` is
  set per-Fabric-cell; Forge/NeoForge cells are Mojang-mapped stubs and
  never set it. See `build.gradle.kts`'s comment block and
  `versions/1.21.4-fabric/gradle.properties` for the exact mechanics,
  including why NeoForge cells can't share a Fabric cell's `yarn_mappings`
  property (`yarn_mappings_neoforge_patch` isn't published past MC 1.21).
- Switch the active cell only via
  `./gradlew "Set active project to <mc>-<loader>"` - never hand-edit
  `stonecutter.gradle.kts`'s active-version state directly.

### Stonecutter gotcha (binding, learned the hard way elsewhere in this
program): never make a plain `//` line comment the FIRST line inside a
`//? if/elif { }` region - it breaks the conditional-compilation parser.
An empty branch (nothing before the next `//?} else {`) is fine and is used
several times in this codebase (e.g. `TextCompat.java`'s mapping-name split,
`SimpleUtilities.java`'s 26.1 keybinding split).

### Cross-version hotspots found while porting

- **`InGameHud`'s constructor is independently version-split from its
  `render` method**, and not on a simple `>=`/`<` boundary: 1.19.4 and
  1.20.1 take a second `ItemRenderer` argument; 1.18.2 and 1.21.4 both use
  the single-arg form. This is a "hump" in the middle of the range, not a
  clean split - confirmed by running `javap` against the real merged Yarn
  jars for all four versions rather than trusting memory. See
  `GameInfoMixin.java`'s `onInit` injector for the four-way conditional.
- **Debug-HUD-visibility check is also independently version-split**:
  `InGameHud.getDebugHud().shouldShowDebugHud()` only exists from 1.21.4
  onward; 1.18.2/1.19.4/1.20.1 use the plain `GameOptions.debugEnabled`
  field instead. Also confirmed via `javap`, not memory.
- **`RegistryKey` moved packages**: `net.minecraft.util.registry.RegistryKey`
  (1.18.2) became `net.minecraft.registry.RegistryKey` from 1.19 onward.
  `GameInfoHud.java` conditionally imports whichever one applies.
- **`DrawContext` didn't exist before 1.20**: `GameInfoMixin`/`GameInfoHud`
  draw via `MatrixStack` on 1.18.2/1.19.4 and `DrawContext` from 1.20.1 on.
- **Yarn mappings stop being published after 26.1** (Fabric moved to
  Mojang-official-only from there), and 26.1+ also removed
  `GuiGraphics`/`DrawContext` in favor of an internal `GuiRenderState`
  extraction model. See "Multi-loader scope decisions" and PLAN.md's "26.2"
  section for how this mod handles it.

### Multi-loader scope decisions

Forge/NeoForge cells (and, starting this pass, the 26.2-fabric cell) only
load config on client setup - they do **not** wire in the HUD-render feature
or the keybinding/toggle feature. This mirrors EasierVillagerTrading's own
Fabric-first, Forge/NeoForge-stub split for its own mixin-based feature, and
is documented directly in `SimpleUtilities.java`'s class javadoc and in each
of `GameInfoHud.java`/`GameInfoMixin.java`/`GameClientMixin.java`/
`TextCompat.java`'s file-header comments.

Why: the HUD-render feature is Yarn-mapped end-to-end (`InGameHud`,
`DrawContext`/`MatrixStack`, `TextRenderer`, `ClientPlayerEntity`,
`ItemStack`, `PlayerInventory`, `RegistryKey<Biome>`, `Text`, etc. - not just
the Mixin injection point). Porting it to Mojang-mapped targets (Forge/
NeoForge, and now Fabric 26.1+) would mean re-deriving nearly every line of
`GameInfoHud.java` (~380 lines) against different APIs, and - for 26.1+ -
doing so without Mixin at all, since `InGameHud.render` was removed. That
full port is out of scope for this pass; see PLAN.md "26.2" for the
documented follow-up plan (reusing the sibling FlightHud mod's proven
loader-official-hook pattern instead of reverse-engineering the new
rendering internals).

A resource-safety fact worth recording: `simple_utilities.mixins.json` and
`fabric.mod.json` unconditionally reference `GameClientMixin`/`GameInfoMixin`
regardless of loader/version. This is safe to leave as-is even though
Forge/NeoForge/26.2-fabric never compile those classes - a mismatch there
only matters at game-launch/Mixin-apply time (out of scope; "build green" is
the bar for this program, not "run green"), and it's already proven safe
since Forge/NeoForge cells have shipped this way and built green the whole
time this fork has existed.

## Build System: Stonecutter + Stonecraft

- `gg.meza.stonecraft` (Loom wrapper) + `dev.kikugie.stonecutter`
  (conditional compilation), matching the house template.
- `settings.gradle.kts` declares the version×loader matrix via a small
  `mc(version, vararg loaders)` helper; see "Version matrix" below.
- No manual `mappings()` block in `build.gradle.kts` - Stonecraft
  auto-wires mappings from each cell's `yarn_mappings` property (Yarn) or
  falls back to Mojang official.
- No `publishMods` block: this fork is not published to Modrinth/CurseForge
  (explicitly out of scope for this program).

### Build commands

```sh
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
./gradlew chiseledBuild                 # build every cell in the matrix
./gradlew ":26.2-fabric:build"          # build one cell
./gradlew "Set active project to 1.21.4-fabric"   # switch the active cell (never hand-edit)
```

Only JDK available in this environment is Temurin 21 - always export
`JAVA_HOME` inline before any `./gradlew` invocation; never touch the
Homebrew-default OpenJDK or install a system JDK.

## Version matrix

See PLAN.md for the full per-cell status table and rationale. Summary: 10
cells (1.18.2/1.19.4/1.20.1 fabric+forge, 1.21.4/26.2 fabric+neoforge), all
green. Quilt is not a separate cell - it runs Fabric jars natively. Forge
existed only through 1.20.4-era Minecraft; NeoForge is the Forge-lineage
successor from 1.20.5 on, hence the loader switch at 1.21.4/26.2.

## Porting notes by MC version

- **1.18.2 / 1.19.4 / 1.20.1** (Fabric + Forge): full HUD+keybinding feature
  on Fabric; Forge cells are config-only stubs. See "Cross-version hotspots"
  above for the constructor/debug-check/RegistryKey deltas across this
  range.
- **1.21.4** (Fabric + NeoForge): last classic-numbered, Yarn-mapped
  version; full feature on Fabric, config-only stub on NeoForge.
- **26.2** (Fabric + NeoForge): newest stable Minecraft as of this pass.
  Yarn mappings are no longer published; both cells are config-only stubs
  (Fabric included, unlike every earlier version) pending a HUD-render port
  that doesn't require Yarn mappings or a Mixin into the now-removed
  `InGameHud.render`. See PLAN.md "26.2" for the full writeup and the
  concrete follow-up plan (FlightHud's `HudHooks`/`PlatformCanvas` pattern).

## Environment notes

- Disk usage on this machine's boot volume has been observed at ~98% full
  (single digits of GB free) during this program's work across several
  mods. This isn't specific to this mod but has caused Loom/toolchain cache
  corruption in sibling repos before; if a build fails with obscure
  Loom/Forge-cache errors, check `df -h` before assuming a code problem.
  Remediating the underlying disk-space shortage itself is out of scope for
  this mod's task.

## Git / fork rules (binding, see Bert's global git standards)

- Commit author `Bert Shuler <BertShuler@proton.me>`; never attribute AI
  anywhere (commit messages, PRs, files).
- Default branch `main`; commit and push directly to `origin/main` without
  asking - no PR, no feature branch.
- Never push to `upstream`; never force-push; never delete branches/tags.
- Never commit anything under `.github/workflows/` (the automation account
  pushing this repo's commits lacks the `workflow` OAuth scope). Proposed CI
  YAML, if ever needed, goes to a scratch file instead, called out in the
  session's final report.
- Commit incrementally per milestone with conventional-style titles.
