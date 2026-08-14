# Simple Utilities Mod

A maintained fork of
[johnvictorfs/simple-utilities-mod](https://github.com/johnvictorfs/simple-utilities-mod).
Upstream marked the mod end-of-life around Minecraft 1.20; this fork keeps
it alive across modern Minecraft — **1.18.2 through 26.2, on Fabric, Forge,
and NeoForge, from a single codebase**.

## Supported versions and loaders

Built with [Stonecutter](https://stonecutter.kikugie.dev/) +
[Stonecraft](https://github.com/meza/stonecraft); all ten cells build and
pass their test suites:

| Minecraft | Fabric | NeoForge | Forge |
|---|---|---|---|
| 26.2 | ✅ | ✅ | — (NeoForge only, post-split) |
| 1.21.4 | ✅ | ✅ | — |
| 1.20.1 | ✅ | — | ✅ |
| 1.19.4 | ✅ | — | ✅ |
| 1.18.2 | ✅ | — | ✅ |

Quilt runs the Fabric jars natively.

Beyond the version matrix, the fork's main structural changes (detailed in
`PLAN.md`/`CLAUDE.md`): the mod was rewritten Mixin-free against each
loader's official HUD hook, migrated wholesale to Mojang mappings (Yarn
stopped being published after 26.1), and the Forge/NeoForge and 26.x ports
carry the **full** HUD + keybinding feature — no stub cells.

## Features

A Minecraft Mod that enhances the Game's HUD with some simple utilities like the following:

- **HUD Features:**
    - Simplified coordinates that are available at all times on the screen (Example: `39, 64, 200` as `X, Y, Z`)
    - Cardinal directions and which Coordinates are increasing/decreasing (Example: `(East X+)` when looking East, where the `X` coordinate is increasing)
    - Current armour and hand items and their durabilities, available at all times on the screen
        - Different colors based on how low the durability is
    - Current Game time in AM/PM format
    - Current sprinting status
    - Current framerate
    - Current Biome the player is on
    - Nether/overworld coordinates conversion
    - Toggle key (editable in Controls screen, default is `k`)
    - Settings page and config file with Mod Menu
        - **Requires [Mod Menu](https://modrinth.com/mod/modmenu) to open settings screen in-game**
        - Config file: `.minecraft/config/simple_utilities.toml` (does not require Mod Menu to edit)

---

## Images

|           In-game HUD                 | Settings page (Requires [Mod Menu](https://modrinth.com/mod/modmenu)) |
| --------------------------------------| ----------------------------------------- |
| ![In-game HUD](images/ingame_hud.png) | ![Settings HUD](images/settings_hud.png)  |

---

## Installation

- Install [Fabric](https://fabricmc.net/use/).
- Download the latest version of Simple Utilities (in the `.jar` file format) from [GitHub](https://github.com/johnvictorfs/simple-utilities-mod/releases/latest) or from [Modrinth](https://modrinth.com/mod/simple-hud-utilities).
- Move the downloaded file into the `.minecraft/mods` folder.

---

<details>
<summary>
Building from source
</summary>

- Clone the project with `git clone https://github.com/bshuler/simple-utilities-mod.git`.
- Change to the project's directory with `cd simple-utilities-mod`.
- `unset JAVA_HOME` (Gradle's toolchain resolver provisions the right JDK per version).
- Run `./gradlew chiseledBuild` to build every version/loader, or
  `./gradlew :1.21.4-fabric:build` for one.
- Built jars land in `versions/<mc>-<loader>/build/libs/`.
    - Use the one without `sources` in the filename.
</details>

---

## Testing

Three tiers, all run by CI (`.github/workflows/build.yml`):

1. **Unit tests + coverage** — JUnit 5 over the pure logic (coordinate/time
   math, config, colors), 100% line coverage enforced by JaCoCo, run in
   **every one of the ten version×loader builds**.
2. **Loaded-game tests** (`fabric-loader-junit`) — a real bootstrapped
   Minecraft validates the mod's metadata, dependency ranges, and
   version-split code against genuinely loaded game data, on every Fabric
   cell.
3. **Client gametests** (`fabric-client-gametest-api-v1`, 1.21.4 + 26.2) —
   a **real Minecraft client** toggles the overlay and asserts the HUD
   changed actual screen pixels above the measured noise floor. This tier
   caught a real shipped bug: 26.x removed the vanilla behavior that had
   been silently promoting the mod's alpha-0 colors to opaque, leaving the
   HUD completely invisible while every other check stayed green. CI runs
   it headless under xvfb and uploads the screenshots.

Details and negative controls are in `PLAN.md`.

---

<details>
<summary>
Planned features
</summary>

- Add configuration to move any specific HUD elements around the screen (drag-and-drop if possible)
- Add option for E counter (entities) and C counter (chunk sections) (possibly M/C counters as well (monsters/creatures)) 
- Add current status effects duration to HUD
- Add Sun/Moon icons to the current game time, so it's easier to tell if the time of day is Day or Night
</details>

---

## FAQ

- **Does this Mod work on versions below 1.15?**
    - No. It *might* work on 1.14 with some changes, but cannot work below 1.14, since this mod is made with Fabric, which only supports Minecraft 1.14 and above.

- **Will this Mod get me banned from *(some multiplayer server)*?**
    - It depends on the server you are playing on. This mod runs entirely on the client-side (your computer) and does not support nor require server installation. The mod (mostly) shows things that are accessible through in-game features, such as the F3 Menu. Some features are not directly accessible in the vanilla game, such as very specific game time. Whether these are allowed depends on the server. Do not create issues about this here. Instead, check the rules of the server you intend to use this mod on.

- **Will you add '*(some feature not present in the [Planned Features](#planned-features) section)*'**?
    - Maybe. If it is related to the functionality of this mod, create [an issue](https://github.com/johnvictorfs/simple-utilities-mod/issues/new) about it. I can only add requested features during my spare time, so do not expect a timely response.
