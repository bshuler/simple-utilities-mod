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
| 1.18.2 | full feature | config-only stub | n/a (pre-NeoForge) |
| 1.19.4 | full feature | config-only stub | n/a |
| 1.20.1 | full feature | config-only stub | n/a |
| 1.21.4 | full feature | n/a (post-Forge/pre-NeoForge split handled by version, Forge ended at 1.20.4-era) | config-only stub |
| 26.2 | config-only stub (see "26.2" below) | n/a | config-only stub |

Loader coverage is mandatory per this program's coordinator: Fabric+Forge
for MC 1.20.4 and older, Fabric+NeoForge for MC 1.20.5+ (Forge itself ended
at the NeoForge split and has no 1.21.4/26.2 release to target). Quilt is
**not** a separate cell - it runs Fabric jars natively, so Fabric coverage
already covers it; this is documented rather than wired in as its own
Gradle subproject.

"Full feature" = HUD-render overlay + toggle keybinding, wired via a
Yarn-mapped Mixin into `InGameHud.render`. "Config-only stub" = the mod
loads its config file on client setup and does nothing else - matches the
pattern already used by every Forge/NeoForge cell in this fork since the
original Stonecutter migration.

## Multi-loader scope decisions (see CLAUDE.md "Multi-loader scope decisions")

Forge/NeoForge never got the HUD feature in the original Stonecutter
migration - it's Yarn-mapped end-to-end and porting it to Mojang mappings
means re-deriving nearly all of `GameInfoHud.java` (~380 lines touch
Yarn-mapped types throughout the body, not just the render entrypoint).
That's out of scope for this pass; Forge/NeoForge remain config-only stubs,
same as they always have been in this fork.

## 26.2

26.2 is the coordinator-mandated newest target for this pass. It was
**not** simply added to the matrix as another full-feature Fabric cell,
for a concrete reason found while investigating: Fabric stopped publishing
Yarn mappings after 1.21.11 (26.1+ is Mojang-official-only), and 26.1+ also
removed `GuiGraphics`/`DrawContext` from the client entirely, replacing them
with an internal `Gui.extractRenderState(...)` → `GuiRenderState` model that
isn't meant to be hooked directly (and isn't a stable target for a Mixin
the way `InGameHud.render` was).

This mod's HUD-render feature (`GameInfoHud.java`, `GameInfoMixin.java`,
`GameClientMixin.java`) and its text-compat helper (`TextCompat.java`) are
all Yarn-mapped and Mixin-based, so none of that code can compile against
26.2 as written, and a same-pass full rewrite wasn't attempted - see below
for why and what the actual follow-up plan is.

**Decision made for this pass**: scope 26.2-fabric the same way Forge/
NeoForge cells are already scoped - config-only stub, no HUD, no
keybinding. Concretely:

- `GameInfoHud.java`, `GameInfoMixin.java`, `GameClientMixin.java`,
  `TextCompat.java`: guard changed from `//? if fabric {` to
  `//? if fabric && <26.1 {` (excludes 26.2-fabric from compiling any of
  this Yarn-mapped code, same way it's already excluded from Forge/
  NeoForge).
- `SimpleUtilities.java`: fabric entrypoint split into a nested `>=26.1`
  branch (config-only, matching Forge/NeoForge) vs the pre-existing `else`
  branch (full keybinding+toggle feature, unchanged).
- `settings.gradle.kts`: added `mc("26.2", "fabric", "neoforge")` as the
  new top ("newest first") matrix entry; `vcsVersion` was deliberately
  **left** at `"1.21.4-fabric"` rather than bumped to 26.2 - this matches
  the precedent set by the sibling FlightHud mod, which also left its own
  `vcsVersion` at its pre-existing baseline (`1.20.1-fabric`) after adding
  26.2 as the new top entry. `vcsVersion` only affects a fresh checkout's
  default active cell, not the matrix itself.
- `versions/dependencies/26.2.properties` created, mirroring FlightHud's
  proven file exactly: no `yarn_mappings` key (none published, and not
  needed either way since the 26.2-fabric cell is a Mojang-mapped stub).

**Rejected alternative**: porting just the keybinding+toggle feature to
Mojang mappings (via `KeyMapping`/`KeyMapping.Category`/`KeyMappingHelper`,
the exact pattern FlightHud proved for its own 26.2 keybinding) while still
deferring the larger HUD-render port. This was seriously considered - it
would have been a smaller capability regression for Fabric users on 26.2 -
but was set aside in favor of matching the existing, already-proven
Forge/NeoForge stub pattern exactly, keeping this pass's diff smaller and
its risk lower. It's a reasonable next increment if a future pass wants
Fabric-26.2 parity with the toggle feature specifically, without yet
tackling the full HUD-render port.

**Documented follow-up plan for the full HUD-render port** (not done this
pass): the sibling FlightHud mod already solved exactly this problem for
its own HUD feature and reached all-green across 26.2. Its pattern, to
reuse here:

- Replace the `InGameHud.render` Mixin with `HudHooks`-style registration
  against each loader's own **official** HUD-render hook: Fabric
  `HudElementRegistry.addLast(...)` for `fabric && >=26.1` (vs.
  `HudRenderCallback.EVENT.register(...)` for older Fabric, as today);
  NeoForge `NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) -> ...)`
  (works unchanged across NeoForge 1.21.4 and 26.2 - NeoForge's own event
  already adapts `getGuiGraphics()` to return the right type per version).
  No Forge target needed here since this mod's Forge cells are already
  stubs by choice, but the same `OverlayRegistry`/`RegisterGuiOverlaysEvent`
  pattern FlightHud uses would apply if that ever changes.
- Introduce a small version/loader-agnostic canvas interface (FlightHud
  calls it `PlatformCanvas`/`FlightCanvas`) with one Stonecutter-branched
  adapter implementation, instead of scattering `DrawContext`/`MatrixStack`/
  `GuiGraphicsExtractor` calls directly through `GameInfoHud.java`.
- `TextCompat.java` would need a `Component`-returning branch for
  Mojang-mapped targets (`net.minecraft.network.chat.Component` instead of
  `net.minecraft.text.Text`), which means wrapping the method *signature*,
  not just the body, in a Stonecutter conditional - noted as one of the
  reasons this is a real follow-up task, not a small tweak.

This is the same reference already used by `critical-orientation`; both are
recommended reading before starting that follow-up work.

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
EasierVillagerTrading. This mod's Forge/NeoForge cells are also currently
config-only stubs with no HUD feature, so a Forgix-merged jar would today
just be "Fabric's full jar with Forge/NeoForge's stub bolted on" - low
value until the HUD-render port above happens. Revisit once (or if) that
port lands.

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
- [x] Forge/NeoForge stub scope decision documented and applied
      consistently (including to 26.2-fabric).
- [x] Quilt documented as covered by Fabric (no separate cell needed).
- [ ] Forgix - investigated, deliberately not wired in this pass (see
      "Forgix" above).

### Phase 3.5 - 26.2 addition (this pass, coordinator-mandated)
- [x] Live-checked newest stable MC (26.2) and its loader/dependency
      versions (Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, NeoForge
      26.2.0.58).
- [x] `mc("26.2", "fabric", "neoforge")` added to `settings.gradle.kts`.
- [x] `versions/dependencies/26.2.properties` created.
- [x] `GameInfoHud`/`GameInfoMixin`/`GameClientMixin`/`TextCompat` guarded
      to `fabric && <26.1`; `SimpleUtilities` fabric entrypoint split on
      `>=26.1`.
- [x] Both new cells (26.2-fabric, 26.2-neoforge) build green individually
      and via `chiseledBuild`; jar contents verified via `unzip -l`.
- [ ] Full HUD-render port to 26.2 (FlightHud `HudHooks`/`PlatformCanvas`
      pattern) - documented above, deferred to a follow-up pass.

### Phase 4 - Verification
- [x] All 10 cells (1.18.2/1.19.4/1.20.1 fabric+forge, 1.21.4/26.2
      fabric+neoforge) build green via `chiseledBuild`.
- [x] Jar contents verified via `unzip -l` for every cell, not just
      "BUILD SUCCESSFUL" text - Fabric cells below 26.1 carry 33 files/8
      classes (full HUD+mixin); Forge cells and NeoForge/26.2-fabric stub
      cells carry 25-26 files/3 classes (config-only), consistent across
      the whole matrix with no green-but-empty jars.

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
- **Open risk, not remediated this pass**: this machine's boot volume has
  been observed at ~98% used (roughly 11-13GB free) across this program's
  work on several sibling mods, and has previously caused Loom/toolchain
  cache corruption requiring manual cleanup. Not this mod-specific and out
  of scope to fix here, but flagged for whoever picks up the next pass.
