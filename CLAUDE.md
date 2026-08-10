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
indicator) and a config file to toggle it, plus a toggle keybinding. The
feature runs identically on every cell in the version×loader matrix -
Fabric, Forge, and NeoForge alike - via the loader-agnostic
`HudHooks`/`PlatformCanvas`/`SimpleCanvas` abstraction (see "Multi-loader
scope decisions" below); there are no config-only stub cells.

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
template used by `critical-orientation` (primary reference). The
HUD-render feature itself follows the sibling **FlightHud** mod's proven
no-Mixin, loader-official-hook pattern (`HudHooks`/`PlatformCanvas`/
`SimpleCanvas`) - see "Multi-loader scope decisions" below.

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
- **Mappings are uniformly Mojang official across every cell.** No
  `yarn_mappings` property is set anywhere in this fork any more (removed
  from every `versions/dependencies/*.properties` file, and the
  `versions/1.21.4-fabric/gradle.properties` override that used to set it
  was deleted outright). This was a deliberate migration, not an
  oversight: 26.1+ stopped publishing Yarn mappings at all, and moving the
  *whole* mod to Mojmap (not just 26.2) is what let `GameInfoHud.java`
  become one loader-agnostic file instead of three divergent Yarn/Mojmap
  copies. See `build.gradle.kts`'s mappings comment and PLAN.md's "26.2"
  section for the full rationale.
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

Superseded by the Mojmap/no-Mixin rewrite: the four Yarn-mapped hotspots
originally found here (`InGameHud`'s split constructor, the
`GameOptions.debugEnabled`/`shouldShowDebugHud` version split, the
`RegistryKey` package rename, and the `DrawContext`-didn't-exist-pre-1.20
split) all applied to the old Yarn-mapped `GameInfoMixin`-based renderer,
which has been deleted along with the Mixin infrastructure. The current
`GameInfoHud.java` never touches `InGameHud`, `MatrixStack`, or a
Yarn-mapped `RegistryKey` at all - see "Multi-loader scope decisions"
below for the replacement architecture. Current, Mojmap-era hotspots
(confirmed via `javap` against real merged/deobfuscated jars at both the
1.18.2 and 26.2 extremes, not from memory):

- **Three real HUD-canvas API shapes, not one boundary**: `<1.20`
  (`PoseStack` + `GuiComponent.fill`/`Font.draw`/`drawShadow`), the
  "classic" `GuiGraphics` shape from 1.20.1 through 1.21.4
  (`drawString`/`renderItem`), and `>=26.1`'s `GuiGraphicsExtractor`
  (renamed `text(...)`/`item(...)`, `void`-returning). All three live
  behind `PlatformCanvas.java`, the one version-conditional file behind
  `SimpleCanvas`.
- **`Inventory`'s non-equipment-item accessor was renamed**:
  `Inventory.items` (public field) through the pre-26.1 range became
  `Inventory.getNonEquipmentItems()` (method) at 26.1+. `GameInfoHud.java`
  has no direct Yarn `PlayerInventory.count(Item)` equivalent to fall back
  on in Mojmap, so it sums manually over both the non-equipment slots and
  the currently-equipped items list instead.
- **`Level.dimension()`/`Level.OVERWORLD`/`Level.NETHER`** are uniform
  across the whole matrix and are what the nether-coordinate-conversion
  check uses (`ResourceKey<Level>` equality), rather than a string-based
  dimension-namespace comparison.
- **Yarn mappings stop being published after 26.1** (Fabric moved to
  Mojang-official-only from there); the whole mod migrated to Mojmap
  uniformly rather than keeping a Yarn/Mojmap split. See "Multi-loader
  scope decisions" and PLAN.md's "26.2" section for the full writeup.

### Multi-loader scope decisions

**Every cell in the matrix carries the real HUD-render + keybinding
feature.** There are no config-only stub cells any more. An earlier pass
had scoped Forge/NeoForge (and, when it was added, 26.2-fabric) as
config-only-on-client-setup stubs, reasoning that the original
Yarn-mapped, Mixin-based `GameInfoHud`/`GameInfoMixin`/`GameClientMixin`
would need a near-total re-derivation to run against Mojang-mapped
targets. The coordinator explicitly rejected that as an acceptable end
state ("a jar that loads but drops the HUD feature isn't 'supporting'
that loader"), so the re-derivation was done:

- The whole mod migrated to uniform Mojang-official mappings (no
  `yarn_mappings` anywhere, on any cell).
- `GameInfoHud.java` was rewritten from scratch against Mojmap types,
  drawing only through the `SimpleCanvas` interface - no per-loader
  divergence in its body at all.
- `HudHooks.java` (new) registers `GameInfoHud` against each loader's own
  **official** HUD-render hook per era instead of a Mixin: Fabric
  `HudRenderCallback` (pre-1.21.2) or `HudElementRegistry` (26.1+);
  NeoForge `RenderGuiEvent.Post`; Forge `OverlayRegistry`/`ForgeIngameGui`
  (`<1.19`) or `RegisterGuiOverlaysEvent`/`ForgeGui` (`>=1.19`).
- `PlatformCanvas.java` (new) is the one version-conditional file behind
  `SimpleCanvas`, covering the three real drawing-API shapes across the
  matrix (`<1.20` raw `PoseStack`, the classic `GuiGraphics` shape,
  `>=26.1`'s `GuiGraphicsExtractor`).
- The now-unused Mixin infrastructure was deleted outright:
  `GameInfoMixin.java`, `GameClientMixin.java`,
  `simple_utilities.mixins.json`, and the `"mixins"` key in
  `fabric.mod.json`. There is no longer a resource-safety caveat to
  record about a mixins.json/fabric.mod.json mismatch, because there is
  no Mixin anywhere in this mod any more.

See PLAN.md's "Multi-loader scope decisions" and "26.2" sections for the
full before/after writeup and the `unzip -l` class-count proof (every
cell now packages the identical 8 mod classes).

## Build System: Stonecutter + Stonecraft

- `gg.meza.stonecraft` (Loom wrapper) + `dev.kikugie.stonecutter`
  (conditional compilation), matching the house template.
- `settings.gradle.kts` declares the version×loader matrix via a small
  `mc(version, vararg loaders)` helper; see "Version matrix" below.
- No manual `mappings()` block in `build.gradle.kts` - Stonecraft falls
  back to Mojang official mappings for every cell, since no cell sets
  `yarn_mappings` any more.
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

- **1.18.2 / 1.19.4 / 1.20.1** (Fabric + Forge): full HUD+keybinding
  feature on both loaders. Forge's HUD registration API is itself
  version-split within this range at the 1.19 boundary (`OverlayRegistry`/
  `ForgeIngameGui` below it, `RegisterGuiOverlaysEvent`/`ForgeGui` at/above
  it) - see `HudHooks.java`.
- **1.21.4** (Fabric + NeoForge): last classic-numbered, Yarn-mapped-era
  version (now built with Mojmap like everything else); full feature on
  both loaders via `HudRenderCallback` (Fabric) and `RenderGuiEvent.Post`
  (NeoForge).
- **26.2** (Fabric + NeoForge): newest stable Minecraft. Yarn mappings are
  no longer published for it, which is exactly why the whole mod moved to
  Mojmap; full feature on both loaders (Fabric via `HudElementRegistry`,
  since `HudRenderCallback` doesn't exist here; NeoForge unchanged from
  1.21.4's `RenderGuiEvent.Post`). See PLAN.md "26.2" for the full writeup.

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
