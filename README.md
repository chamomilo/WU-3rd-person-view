# Third Person View 0.4.8

A third-person, orbit and flying camera mod for the Wurm Unlimited client.

[Download the latest installable ZIP](https://github.com/chamomilo/WU-3rd-person-view/releases/latest).
Requires the Wurm Unlimited client mod launcher and a compatible client.

## Features

- Follow the character's native eye position with smooth zoom from 1.20 to
  12 metres. Zooming in at the nearest distance returns to first person.
- Hold orbit to look around the character without turning it. Releasing orbit
  smoothly returns the camera behind the character.
- Fly the camera independently while the character stays in place. A
  click-through FREE LOOK MODE marker shows when flying is active.
- Keep loaded terrain, vegetation, objects and cave rendering centred on the
  camera. Surface/cave visibility and world lighting follow the camera even
  when the character remains underground.
- Stop at cave floors, ceilings and solid walls. Loaded cave exits remain
  passable, and the camera can enter a separate loaded cave.
- Use the complete shared Chamomilo updater 1.2.7 with Chamomilo UI 0.4.10.
  Its Mod updates window checks releases and links to their GitHub pages.
  Installation of downloaded ZIPs is manual.

## Installation

1. Close the entire Wurm Unlimited client.
2. Extract the release ZIP into the client directory containing WurmLauncher.exe.
3. Check that these files exist:

```text
mods/WU-third_person_view.properties
mods/WU-third_person_view/WU-third_person_view.jar
mods/WU-third_person_view/README.txt
mods/WU-third_person_view/LICENSE
mods/WU-third_person_view/keybinder/WU-third_person_view.keybinder
```

4. Start the client through its mod launcher and enter `tp status` in the console.

If the command is unknown, check the extraction paths and client.log, then
restart the entire client. A successful initialization logs
`WU-third_person_view 0.4.8 hooks installed` followed by
`WU-third_person_view ready`.

Use one third-person camera mod at a time to avoid conflicting camera hooks.
When replacing an earlier release, keep your preferred camera settings and
saved state; the release properties contain the current defaults.

## Controls and Keybinder

A compatible Keybinder displays `Catalog: 3rd Person View` with ready-to-use
camera actions. Assign keys in Keybinder. For older versions, import the
bundled WU-third_person_view.keybinder preset. That preset assigns:

- V: toggle first-person/third-person view.
- Mouse Wheel Up / Down: zoom in / out.
- Home: reset the camera behind the character.
- Left Shift, held: orbit without turning the character.
- F8: toggle flying freelook.

These are preset bindings. Without Keybinder, the mod handles the mouse wheel
and uses the configured orbitKey, which defaults to LSHIFT. Other actions can
be invoked through console commands.

In flying freelook, mouse look aims the camera and the normal forward, back,
left and right movement bindings fly it. Shift accelerates; Alt slows movement
for precision. The character remains in place. Leaving freelook restores the
previous first-person or third-person mode.

## Console commands

```text
tp toggle
tp on
tp off
tp zoom-in
tp zoom-out
tp zoom <0..1>
tp reset
tp orbit-hold
tp orbit-on
tp orbit-off
tp freelook-toggle
tp freelook-on
tp freelook-off
tp status
tp debug on
tp debug off
```

Use tp orbit-hold for a held Keybinder action. The explicit orbit-on and
orbit-off commands are also available for external bindings.

## Settings

Edit mods/WU-third_person_view.properties while the client is closed. It
contains zoom, smoothing, collision, head animation and orbit settings.
Current view mode and zoom are saved in
mods/WU-third_person_view/state.properties.

The shared updater's language selector applies immediately to all installed
mods that expose a supported language interface. The updater supports English,
Brazilian Portuguese, German and Russian. The shared language is always active;
its initial choice uses a supported OS language or English. Camera commands and
camera messages remain in English.

## Loaded-world limits

The server streams world data around the character. Moving the camera does
not request a new region. Another cave can be entered only when its exact tile
coordinates and floor corners have been received. Missing or overwritten cave
data stays blocking; an unconnected cave is allowed when its data is loaded.

The camera checks its entire flying movement path, including accelerated
movement. Cave floors, ceilings and side walls remain solid around roofless
exit tiles. If an obstacle leaves too little room for the third-person camera,
the view temporarily falls back to first person without losing the selected zoom.

If Wurm's advanced collision manager is unavailable, the mod logs one warning
and retains loaded terrain and cave-volume checks. Surface object and building
wall collision is not guaranteed in that fallback mode.

## Building from source

Use a Java 8 JDK and the included Gradle 5.3.1 wrapper. Supply these compatible
local dependencies in libs; they are excluded from Git and release ZIPs:

```text
client-patched.jar
common.jar
javassist.jar
modlauncher.jar
junit-4.13.2.jar
hamcrest-core-1.3.jar
```

Use client libraries from a legally installed Wurm Unlimited client and a
compatible mod-loader development environment. The canonical updater source
is required at C:/projects/updater. Override its location with
CHAMOMILO_UPDATER_REFERENCE or -PchamomiloUpdaterReference=/path/to/updater.
The reference owns its pinned UI SDK and all updater implementation.

Set WURM_JFX, WURM_JAVAFX_JAR or -PwurmJavaFxJar to an existing JavaFX 8
jfxrt.jar for the updater's native UI checks. Its client libraries default to
the reference's libs directory; override with WURM_CLIENT_LIBS or
-PwurmClientLibDir. On Windows, run:

```powershell
.\gradlew.bat clean build dist verifyClientContract
```

The build clean-compiles and checks the complete reference updater, records
its version and SHA-256 in chamomilo-updater.lock.properties, and verifies
exact embedded module contents. Client contract tests compile the hooks against
the supplied client classes. README.txt is generated from this README for the
installable ZIP. Packaging rejects Markdown files in both the ZIP and nested
JARs. Outputs are in build/distributions; the build never installs the mod.

Automated checks do not replace a live game test of GPU rendering and camera
transitions on the user's client.

## License

Copyright (C) 2026 chamomilo. Original project source and documentation are
licensed under GNU GPL version 3; see LICENSE. Wurm Unlimited client files and
third-party dependencies are excluded from that license. Embedded fonts retain
their bundled OFL license.
