# Changelog

## 0.4.8 — 2026-10-10

- Embed the complete canonical Chamomilo updater 1.2.7 with Chamomilo UI
  0.4.10, retaining coordinator protocol 1 and exact entry/hash verification.
- Include the camera-based surface/cave visibility and separate loaded cave
  fixes from local builds 0.4.5–0.4.7 in this public release.
- Replace the installation guide with a concise README. Generate README.txt
  for the installable ZIP and reject Markdown in the ZIP and its nested JARs.
- Archive obsolete development specifications and the separate Holster project
  outside the camera working directory. Keep only camera source, build inputs,
  release documentation and current build outputs in this project.

## 0.4.7 — 2026-10-10

- Embed the complete canonical updater 1.2.6 and Chamomilo UI 0.4.9 with no Off choice. The updater
  is the only language selector for capable mods and applies a concrete shared
  language at startup and after every choice. Retain exact module/hash checks.

- Centre the 32x32 native cave geometry grid, boundary curtain and sky occlusion
  on the camera, allowing it to enter another loaded cave while the character
  remains in the original cave.
- Prepare the target cave grid before main/reflection portal visibility checks
  and select cave triangle sidedness from the camera's visual layer.
- Track exact cave tile-strip receipt coordinates through updates and reconnect.
  Use that coverage for collision and visual layer selection instead of treating
  the legitimate -10 metre floor value as missing data. All four floor corners
  are required; unreceived and overwritten slots remain blocking.

## 0.4.5

- Fix the missing surface when an orbit or flying camera leaves a cave while
  the character remains underground. Choose visual visibility and world
  lighting from the camera's loaded surface/cave geometry.
- Evaluate cave entrances at the camera tile for main, reflection and picking
  views. Preserve native portal visibility and restore the cave layer when
  the camera re-enters; player model lighting retains the actual player layer.
- Embed the complete current Chamomilo updater 1.2.4 with Chamomilo UI 0.4.8.

## 0.4.4

- Update the complete embedded Chamomilo updater to 1.1.0 with the pinned
  Chamomilo UI 0.4.4 SDK, retaining shared coordinator protocol 1.
- Add the updater's Force language selector for installed mods that support
  changing their user language. Off preserves each mod's own language choice.
- Coordinate typography across updater actions and language choices, including
  the detached dropdown popup, and use the current public mod catalogue.
- Remove obsolete local build caches and temporary artifacts, consolidate
  ignore rules and keep the separate Holster development project outside the
  camera mod's published source tree.

## 0.4.3

- Update the complete embedded Chamomilo updater from 1.0.2 to 1.0.5,
  retaining shared coordinator protocol 1 and the pinned Chamomilo UI 0.3.4.
- Use bundled fonts throughout the registry, a narrower default window,
  larger mod headings and project links beside each heading.
- List Waypointer first and Keybinder second, followed by the other mods
  alphabetically, regardless of installed version or release-check status.
- Refresh the updater lock and verify exact module entries and bytes in the
  mod JAR and installable ZIP.

## 0.4.2

- Prevent camera movement through cave floors, ceilings and solid tiles in
  follow, orbit and flying freelook. Check the complete flying-camera path
  before accepting each frame, including accelerated movement.
- Preserve roofless cave exits and allow the camera to leave and re-enter
  through loaded openings while keeping their floors and side walls solid.
- Hold the camera at unloaded cave boundaries instead of treating missing data
  as an opening.
- Replace the local updater implementation with the complete canonical
  Chamomilo updater 1.0.2 module, retaining shared coordinator protocol 1.
- Refresh the Mod updates window with the module's pinned Chamomilo UI 0.3.4,
  compact repeating artwork, bundled fonts and native scrolling.
- Synchronize the complete module before builds and record its version and
  SHA-256 in `chamomilo-updater.lock.properties`. Verify exact updater entries
  and bytes in the mod JAR and installable ZIP.

## 0.4.1

- Expose the Third Person View JAR to Javassist before compiling hooks under the
  production Wurm ModLoader classpath.
- Ship Holster Weapons 1.0.1 with the same classpath fix and the shared
  Chamomilo updater integration.

## 0.4.0

- Move all Holster rendering, animation, commands, state and Studio support to
  the standalone Holster Weapons mod.
- Move the vegetation/detail focus progressively toward the third-person camera,
  reaching the full eight-tile inclusive decoration area at maximum zoom.
- Centre terrain, cells, vegetation, occlusion and render-origin updates on the
  flying camera while the client has the required world data loaded.
- Document the server-streaming limit for detail far away from the character.

## 0.3.10

- Replace the previous updater with the current shared registry, native Mod updates menu and saved startup preference.
- Reuse the original button artwork with complete top/bottom borders and stable horizontal stretching.
- Verify native layout, input, HUD replacement and shared updater packaging before distribution.
