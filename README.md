# WU-third_person_view 0.4.4

A third-person camera client mod for Wurm Unlimited.

## Features

- the camera is centered on Wurm's native eye/head position;
- normal mouse look rotates the character's view and the complete camera boom in both yaw and pitch;
- the mouse wheel changes only the distance along the eye/head view line;
- zooming out from first person enables third person at its minimum distance, while zooming in at that boundary returns to first person;
- looking up moves the camera below the head and makes it look upward through the head; looking down moves it above the head;
- native mouse look also turns the visible head model; while standing, the head turns first and the body follows at the configured yaw limit;
- holding `Left Shift` makes the mouse orbit the camera around the character instead of turning the character;
- the camera smoothly returns behind the character after Shift is released;
- flying freelook detaches the camera from the character without moving the character on the server;
- vegetation detail follows the camera progressively as third-person zoom grows;
  the decoration grid expands from 15×15 to 17×17 so maximum zoom provides eight
  full tiles in every direction;
- flying freelook moves terrain, vegetation, cell, object-LOD, occlusion, and
  floating render origins to the camera instead of leaving them on the character;
- flying freelook displays a non-interactive `FREE LOOK MODE` marker above the bottom Event window and removes it immediately when the mode ends;
- every camera mode stays at least `surfaceClearance` above the loaded surface or active cave floor;
- spherical obstacle detection uses Wurm Unlimited's advanced collision engine;
- a separate cave-volume check keeps zoom, orbit and flying freelook above the
  floor, below the ceiling and out of solid cave tiles;
- cave exits remain open: their tiles have no artificial ceiling, while their
  floors and neighbouring walls still constrain the camera;
- flying freelook checks its full movement path each frame, including
  accelerated movement, and can leave or re-enter through a loaded cave exit;
- when an obstacle leaves too little space, the camera temporarily uses the native first-person view without losing the selected zoom;
- the minimum third-person distance is 1.20 metres, keeping the camera behind the character's head and equipment;
- a compatible Keybinder automatically displays a `3rd Person View` catalog containing ready-to-use commands;
- a preset for older Keybinder versions is included at `mods/WU-third_person_view/keybinder/WU-third_person_view.keybinder`.

## Installation

Close Wurm Unlimited and extract the ZIP into the directory that contains
`WurmLauncher.exe`. The resulting files must include:

```text
WurmLauncher/mods/WU-third_person_view.properties
WurmLauncher/mods/WU-third_person_view/WU-third_person_view.jar
```

Start the game through the client mod launcher. A successful installation adds
the following lines near the beginning of `client.log`:

```text
Reading mods\WU-third_person_view.properties
Loading org.wuthirdpersonview.client.WUThirdPersonViewMod as WU-third_person_view
WU-third_person_view 0.4.4 hooks installed
WU-third_person_view armed; runtime initialization is deferred
WU-third_person_view ready
```

If `tp status` reports `Unknown command: tp`, the mod was not initialized. Check
the paths above, restart the entire client rather than merely reconnecting, and
make sure the `client.log` belongs to the Wurm installation you modified.

Do not run this mod together with another third-person camera mod. Both mods can
modify the same camera methods, making it unclear which implementation is active
and potentially causing hook conflicts.

## Keybinder integration

When both compatible mods are installed, Keybinder provides the step type
`Catalog: 3rd Person View`. The catalog is visible only while this camera mod is
installed. Each catalog item creates one complete console command and has no
Source, Tool, or Target fields.

With an older Keybinder release, open its import window and import the preset
from the camera mod's `keybinder` subdirectory. The preset assigns:

- `V` — toggle first-person/third-person view;
- `Mouse Wheel Up` — zoom in;
- `Mouse Wheel Down` — zoom out;
- `Home` — return the camera behind the character;
- `Left Shift` (hold) — orbit without turning the character;
- `F8` — toggle flying freelook.

Orbit remains attached to the character and is intended for inspecting the
character from every side. Flying freelook is a separate mode: mouse look aims
the camera, the normal forward/back/left/right movement bindings fly it, `Shift`
accelerates, and `Alt` enables precise slow movement. The character remains in
place. Leaving freelook restores the previously selected first- or third-person
mode. A brass-edged `FREE LOOK MODE` marker remains visible above the bottom
Event window only while the flying camera is active. It is rendered outside
the interactive component list, so it cannot intercept mouse input.

For held orbit, the mod uses a compatible runtime bridge to read the binding for
the `tp orbit-hold` action from Keybinder and match its press/release events.
This allows the action to be reassigned in Keybinder even though version 0.7.5
does not expose a separate external release callback. Without Keybinder, the mod
uses `orbitKey` from `WU-third_person_view.properties`, which defaults to
`LSHIFT`. The mod also detects wheel chords managed by Keybinder and avoids
handling the same wheel event twice.

## Commands

```text
tp toggle
tp on | off
tp zoom-in | zoom-out | zoom <0..1>
tp reset
tp orbit-on | orbit-off
tp orbit-hold
tp freelook-toggle
tp freelook-on | freelook-off
tp status
tp debug on | off
```

`tp orbit-hold` marks a held Keybinder action. `tp orbit-on` and `tp orbit-off`
are intended for automated checks and external bridges.

## Configuration and state

Camera settings are stored in `mods/WU-third_person_view.properties`. The
current zoom and view mode are saved in `mods/WU-third_person_view/state.properties`.
Visible head tracking can be tuned with `animateHead`, `maxHeadYawDegrees`,
`maxHeadPitchDegrees`, and `bodyFollowSeconds`.

Rendering detail can be camera-centred only for data already loaded by the
client. In flying freelook, nearby terrain, vegetation and loaded objects follow
the viewpoint, but the server still streams creatures, structures and other
world data around the character. Beyond that streamed region, a client-only mod
cannot create missing high-detail data.

If the advanced collision manager is unavailable, the mod remains usable, logs
one warning, and falls back to loaded terrain/cave geometry. Cave floor,
ceiling and solid-tile checks remain active. Surface object and building wall
collision is not guaranteed in that fallback mode.

## Building from source

Use a Java 8 JDK and the included Gradle wrapper. Copy the pinned dependencies
listed in `libs/README.md` into `libs/`; binary client dependencies are
intentionally excluded from the repository. The canonical updater reference at
`C:/projects/updater` is required; override its location with
`-PchamomiloUpdaterReference=/path/to/updater` or
`CHAMOMILO_UPDATER_REFERENCE`. Its complete module is clean-built, checked and
embedded automatically. The current module is 1.1.0 with Chamomilo UI 0.4.4;
the module version and SHA-256 are recorded in
`chamomilo-updater.lock.properties`.

The updater's native layout probe also needs JavaFX 8. Set
`WURM_JAVAFX_JAR`, `WURM_JFX` or `-PwurmJavaFxJar` to an existing `jfxrt.jar`.
The reference uses its own pinned client libraries by default; override them
with `WURM_CLIENT_LIBS` or `-PwurmClientLibDir`. On Windows, run:

```powershell
.\gradlew.bat clean test verifyClientContract verifyDistArchive
```

The distributable ZIP is written to `build/distributions/`.

## License

Copyright (C) 2026 chamomilo.

The original source code and documentation in this repository are licensed
under the GNU General Public License, version 3. See `LICENSE`. Wurm Unlimited,
its client files, and third-party dependencies are not part of this license or
this repository.

The complete behavior specification is in `THIRD_PERSON_VIEW_SPEC.md`.

## Chamomilo versions

Version 0.4.0 separates Holster Weapons into its own installable mod and makes
visual detail camera-aware. The shared Mods Registry remains available through
the native Mod updates menu. ZIP installation is manual. See
`docs/UPDATER_BUTTONS.md` for the reusable button API.

Version 0.4.1 makes the standalone mod classes explicitly visible to Javassist
under the real Wurm ModLoader classpath.

Version 0.4.2 replaces the local updater with the canonical compiled module
1.0.2. Builds verify its version, SHA-256 and exact embedded contents in both
the JAR and installable ZIP. Cave-volume checks now constrain the floor, ceiling
and walls in follow, orbit and flying freelook, retaining passable cave exits.

Version 0.4.3 updates the embedded updater to 1.0.5, with bundled registry
fonts, a narrower default window, larger mod headings and project links beside
the headings. Waypointer and Keybinder appear first in the registry.

Version 0.4.4 embeds updater 1.1.0 and Chamomilo UI 0.4.4. Its Force language
selector applies a shared user-language choice to installed mods that expose
a supported language API. Off stops applying a shared choice. Third Person
View's own camera commands and messages remain in English.
