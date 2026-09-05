# Technical specification: 3rd Person View client mod for Wurm Unlimited

Document version: 2.0
Updated: 5 September 2026

## 1. Purpose

The mod adds a Wurm Online-style third-person camera to Wurm Unlimited. The
camera is attached to the character's native eye/head view axis, and zoom
changes only its distance from that pivot.

The camera must:

- preserve Wurm's normal character-control semantics;
- keep the character's native eye/head pivot at the centre of the view;
- use the character's native horizontal and vertical look angles for the camera boom;
- change only boom distance when zooming;
- allow the player to orbit around the character while holding a configurable key;
- animate the visible character head from normal look input;
- provide a separate flying freelook mode that never moves the server-side character;
- avoid clipping through terrain, cave walls, constructed walls, floors, ceilings, roofs, and other opaque obstacles;
- keep server-side movement, combat, action range, and line-of-sight mechanics unchanged;
- behave predictably on foot, on mounts, in vehicles, inside structures, and in caves.

This is a client-only mod. It requires no server mod or protocol change.

## 2. Background

Wurm Online received a native third-person camera in 2020, including
`CHANGE_VIEW`, `ZOOM_IN`, and `ZOOM_OUT` actions. The default controls use `V`
for view switching and `+`/`-` for zoom. See the
[official patch notes](https://forum.wurmonline.com/index.php?/topic/173380-patch-notes-30jan20/)
and [Wurmpedia key bindings](https://www.wurmpedia.com/index.php/Key_bindings).
Wurm Unlimited does not include that native feature.

The head-centred boom, normal-look behavior, and visible head response are
matched to the representative gameplay in this
[Wurm Online video](https://www.youtube.com/watch?v=9ipXlUHmxkw).

[Thource's Third Person Mod](https://github.com/Thource/thources-third-person-mod)
demonstrates that the Wurm Unlimited client camera can be moved through
`WorldRender` hooks. It does not provide this specification's complete wheel,
independent orbit, collision, smoothing, persistence, and Keybinder behavior.
It must not be installed at the same time as this mod because both modify the
same camera pipeline.

Obstacle handling follows the standard spring-arm model: retain the distance
selected by the user, shorten only the camera's actual distance when blocked,
and restore it when space becomes available. Similar designs are documented by
[Unreal Spring Arm](https://dev.epicgames.com/documentation/en-us/unreal-engine/API/Runtime/Engine/USpringArmComponent),
[Unity Cinemachine Third Person Follow](https://docs.unity.cn/Packages/com.unity.cinemachine@2.10/manual/Cinemachine3rdPersonFollow.html),
and [Godot SpringArm3D](https://docs.godotengine.org/en/stable/classes/class_springarm3d.html).

## 3. Terms

- `anchor`: Wurm's native eye/head camera pivot.
- `desiredCamera`: the camera position requested by zoom and orbit before obstacle correction.
- `actualCamera`: the final camera position after collision correction and smoothing.
- `zoom` (`z`): normalized third-person zoom in the range `0.0..1.0`.
- `desiredDistance`: the distance selected by the player.
- `actualDistance`: the safe distance after collision correction.
- `viewYaw` and `viewPitch`: Wurm's native character look direction.
- `orbitYaw` and `orbitPitch`: temporary independent camera rotation.
- `camera boom`: the line that passes through the camera and the eye/head anchor, aligned with the current look direction.

## 4. Camera states

### 4.1. `FIRST_PERSON`

- Wurm's native first-person camera position and orientation are left intact.
- First person is a separate state and is not represented by `z = 0`.
- The previous third-person zoom is retained for normal toggle restoration.
- Collision may temporarily force native first-person rendering without changing the selected third-person zoom.

### 4.2. `THIRD_PERSON_FOLLOW`

- The camera remains on the boom behind the eye/head anchor.
- `z = 0` is a valid third-person position at the configured minimum distance.
- Normal horizontal and vertical look input changes the character's native view direction; the boom and camera follow that direction.
- Looking up moves the rear camera end below the head; looking down moves it above the head.
- First-person head bob is not applied to the third-person camera.

### 4.3. `ORBIT`

- Orbit is active while the configured hold action is pressed; the fallback key is `Left Shift`.
- Horizontal mouse input changes `orbitYaw`; vertical input changes `orbitPitch`.
- Orbit never changes the character, mount, or vehicle direction, locally or on the server.
- Horizontal orbit supports a complete 360-degree turn.
- Releasing the hold action smoothly returns the camera behind the character when `orbitReturns=true`.
- Pressing the hold action again during return continues from the current camera position without a jump.

### 4.4. `COLLISION_RETRACTED`

- This is an obstacle correction applied over follow or orbit, not a user-selected mode.
- An obstacle shortens only `actualDistance`; `zoom`, `desiredDistance`, and remembered zoom remain unchanged.
- The camera returns to `desiredDistance` after the obstacle is gone.

### 4.5. `FLYING_FREELOOK`

- The camera is detached from the character and retains its current position when the mode begins.
- Mouse look changes the flying camera yaw and pitch.
- Normal movement bindings fly the camera forward, back, left, and right without moving the character locally or on the server.
- `Shift` accelerates and `Alt` enables precise slow movement.
- Leaving flying freelook restores the selected first- or third-person camera state.
- Flying freelook is independent of held orbit; entering it clears the orbit hold state.
- A non-interactive `FREE LOOK MODE` HUD marker is visible above the bottom Event window for exactly as long as the renderer's flying free camera is active.
- The marker follows resolution changes, remains inside the screen bounds, and never captures clicks, wheel input, or hover targeting.

## 5. Controls and input arbitration

| Action | Default input | Result |
|---|---|---|
| Zoom in | mouse wheel up | Reduce `zoom`; switch to first person when already at `z = 0` |
| Zoom out | mouse wheel down | Increase `zoom`; switch from first person to third person at `z = 0` |
| Toggle view | Keybinder preset: `V` | Toggle first/third person and restore remembered third-person zoom |
| Turn character and camera | native Wurm look input | Change normal facing and view |
| Orbit without turning | hold `Left Shift` and use look input | Change camera orbit only |
| Reset orbit | Keybinder preset: `Home` or `tp reset` | Return the camera behind the character |
| Flying freelook | Keybinder preset: `F8` or `tp freelook-toggle` | Detach or reattach the camera without moving the character |

Input requirements:

- One logical wheel step changes `zoom` by `zoomStep`, default `0.08`.
- Camera wheel input is handled only over the 3D viewport when no UI component owns it.
- Wheel input over lists, inventory, chat, and other UI continues to scroll the UI.
- While Wurm's item placement mode is active, the wheel continues rotating the placed object and does not zoom the camera. See [Wurmpedia interface and controls](https://www.wurmpedia.com/index.php/Interface_and_controls).
- Keybinder and native input must not apply the same wheel event twice.
- Every camera action, including held orbit, can be reassigned through a compatible Keybinder.
- The mod must not silently replace an existing user binding.

## 6. Zoom and view direction

### 6.1. Range and boundaries

- `z = 0`: closest valid third-person position, still behind the character.
- `z = 1`: maximum configured distance on the same current view axis.
- Zooming out from native first person activates third person at exactly `z = 0`.
- Zooming in from `z > 0` first reaches `z = 0` and remains in third person.
- A further zoom-in command received while already at `z = 0` switches to native first person.
- Normal view toggling restores the remembered third-person zoom; the first-person zoom-out boundary intentionally starts at `z = 0` instead.

### 6.2. Distance curve

The implemented curve is:

```text
smoothstep(t) = 3*t*t - 2*t*t*t
D(z) = minDistance + (maxDistance - minDistance) * smoothstep(z)
```

The shipped defaults are `minDistance=1.20` and `maxDistance=12.0`. The hard
minimum of 1.20 metres keeps the closest third-person camera behind the
character's head, hair, helmet, and other equipment rather than inside them.

### 6.3. Native head/view direction

Zoom does not derive or modify a pitch angle. In follow mode, the boom uses
Wurm's native look values directly:

```text
cameraYaw = World.getPlayerRotX()
cameraPitch = clamp(World.getPlayerRotY(), -89, 89)
```

Consequently, normal look input behaves like movement of the character's head:

- looking left or right rotates the boom around the head;
- looking up tilts the forward view upward and puts the rear camera end below the head;
- looking down tilts the forward view downward and puts the rear camera end above the head;
- zooming at a fixed look direction changes only the length of the boom.

Field of view is never changed by camera zoom. The user's Wurm FOV setting is
preserved in every camera state.

## 7. Camera rig geometry

```text
desiredCamera = anchor - forward(cameraYaw, cameraPitch) * desiredDistance
cameraForward = normalize(anchor - actualCamera)
```

Requirements:

- The default anchor is `WorldRender.getPlayerH()`, Wurm's native eye/head camera height, plus `headAnchorVerticalOffset`.
- The anchor follows the character, not the terrain under the character.
- `headAnchorVerticalOffset` defaults to zero.
- There is no lateral shoulder offset: the head remains on the exact centre line of the camera.
- Camera roll is always zero relative to world up.
- Terrain, mount, and vehicle tilt must not roll the horizon.

## 8. Obstacles and clipping prevention

### 8.1. Main algorithm

Each camera update must:

1. Calculate the anchor, desired transform, and desired distance.
2. Sweep a spherical camera shape from the anchor towards the desired camera through Wurm Unlimited's advanced collision manager.
3. On the cave layer, independently trace crossed tile boundaries through `CaveDataBuffer.shouldHaveNorthWall` and `shouldHaveWestWall`.
4. Stop the camera centre before a cave boundary by at least `cameraRadius + collisionMargin`.
5. Use the nearest valid obstruction to calculate the safe distance.
6. Apply `min(desiredDistance, safeDistance)` without changing zoom or orbit angles.
7. Clamp the final camera centre to at least `surfaceClearance` above the loaded terrain surface or active cave floor.

The cave-boundary trace is mandatory for both zoom and orbit. It covers cave
walls that are visually solid but unavailable or unreliable in the advanced
collision scene.

The camera retracts along its own axis. Automatic sideways obstacle avoidance
is outside the MVP because it can cause unpredictable jumps in narrow spaces.

The final surface clamp applies in first person, third-person follow, Orbit,
collision-retracted first person, and Flying Freelook. It runs after the mode's
normal camera calculation, so a downward free flight or steep upward character
look cannot leave the rendered camera below the floor. On the cave layer it
uses `CaveDataBuffer.getInterpolatedFloor`; on the surface it uses
`NearTerrainDataBuffer.getInterpolatedHeight`. It never substitutes the outdoor
terrain height while the player is on the cave layer.

### 8.2. Blocking geometry

The camera is blocked by:

- ground and rock terrain;
- cave walls and cave ceilings on the player's active layer;
- constructed walls, closed doors, and gates;
- relevant floors, ceilings, roofs, bridges, and other opaque solid geometry.

The camera is not intended to be blocked by:

- the local character, their mount, or vehicle;
- other characters and creatures;
- grass, foliage, particles, water, fog, and decorative non-solid meshes;
- cave geometry belonging to a different world layer.

Layer filtering matters because Wurm Online previously fixed a case where cave
roofs disabled third person for players on the surface. See the
[2 June 2026 patch notes](https://forum.wurmonline.com/index.php?/topic/211668-patch-notes-02-jun-2026/).

### 8.3. Collision-forced first person

- If safe space is below `firstPersonEnterDistance`, the visual result switches to Wurm's native first-person camera.
- Third person is restored only after space exceeds `firstPersonExitDistance`.
- The separate thresholds provide hysteresis and prevent rapid flickering.
- The shipped thresholds are 1.00 and 1.15 metres.
- This temporary transition never changes the selected zoom.

### 8.4. Smoothing

- Retraction must be immediate enough that the camera cannot cross a blocking surface.
- Expansion after an obstacle is removed is smoothed with `collisionReturnSeconds`, default `0.28`.
- Teleportation, reconnection, layer changes, and large server position corrections reset stale smoothing state.
- All damping is time-based and frame-rate independent.

## 9. Character, combat, and interaction

- The mod does not modify character position, movement speed, collision capsule, action ranges, combat formulas, or server line of sight.
- Picking and action requests remain normal Wurm client requests; server validation stays relative to the character.
- Orbit input does not change facing, parry direction, mount heading, or vehicle heading.
- Flying freelook movement uses Wurm's temporary free-camera key set and does not send character movement.
- Crosshair and selection behavior must correspond to the final camera projection.

### 9.1. Visible head tracking

- In normal third-person follow mode, mouse look continues to update Wurm's native view yaw and pitch.
- The local player model's `Head` skeleton joint receives the same pitch, clamped to `maxHeadPitchDegrees`.
- While standing, head yaw follows the view until `maxHeadYawDegrees`; after that limit the rendered body follows so the head is not twisted farther.
- While moving, the rendered body recentres towards the native view over `bodyFollowSeconds`.
- The animation is injected after normal forward kinematics and before skinning, so walking, combat, equipment, and idle animations remain active.
- Held orbit and flying freelook do not turn the head because they do not change the character's native look direction.

## 10. Mounts and vehicles

- The anchor follows the rider/passenger position rather than the mount's feet or the vehicle origin.
- Follow yaw uses the effective controlled direction.
- Orbit never steers the character, mount, or vehicle.
- Vehicle roll and sudden pitch are not copied to the camera horizon.
- Embarking and disembarking transition to the new anchor safely and without retaining stale coordinates.

## 11. Configuration

The release ships with these defaults:

```properties
enabled=true
defaultZoom=0.32
zoomStep=0.08
minDistance=1.20
maxDistance=12.0
headAnchorVerticalOffset=0.0
animateHead=true
maxHeadYawDegrees=55.0
maxHeadPitchDegrees=60.0
bodyFollowSeconds=0.12
zoomSmoothingSeconds=0.10
collisionReturnSeconds=0.28
cameraRadius=0.24
collisionMargin=0.10
surfaceClearance=0.25
firstPersonEnterDistance=1.00
firstPersonExitDistance=1.15
orbitKey=LSHIFT
orbitSensitivity=1.0
orbitReturns=true
orbitReturnSeconds=0.45
maxOrbitPitchDegrees=35.0
inputProvider=auto
debug=false
```

Invalid values are clamped or replaced with safe defaults. Bad console-command
arguments are reported without allowing an exception to escape into the client.

## 12. Console commands

```text
tp toggle
tp on | off
tp zoom-in | zoom-out | zoom <0.0..1.0>
tp reset
tp orbit-on | orbit-off | orbit-hold
tp freelook-toggle | freelook-on | freelook-off
tp status
tp debug on | off
```

`tp orbit-hold` is a marker for a held Keybinder action. The camera mod registers
its ModLoader `ConsoleListener` during deferred runtime initialization on the
first rendered world frame. This avoids loading and freezing game classes while
legacy mods are still installing hooks during `init`.

## 13. Keybinder integration

Keybinder 0.7.5 or a later compatible release is the primary source of bindings
when it is installed and initialized successfully.

After both mods load, Keybinder displays `Catalog: 3rd Person View`. The camera
mod supplies this catalog through the optional reflection contract
`getKeybinderCommandCatalog()`. The catalog is absent when the provider class is
absent or incompatible.

Catalog requirements:

- every entry becomes a normal, complete `ConsoleCommandStep`;
- entries have no Source, Tool, or Target arguments;
- the catalog covers toggle, on/off, zoom steps and fixed zooms, reset, held orbit, orbit on/off, flying freelook, status, and debug actions;
- saved bindings retain normal Keybinder conflict and per-character rules;
- updating the catalog does not create or overwrite user bindings automatically;
- a malformed or incompatible provider hides only its catalog and does not prevent Keybinder from starting.

### 13.1. Held orbit contract

The compatible bridge reads the chord assigned to `tp orbit-hold` and matches it
against low-level key-press and key-release events:

- a press enters orbit exactly once;
- a release exits orbit exactly once and begins return-to-follow;
- key repeat does not create additional presses;
- loss of focus, menu transitions, disconnect, and character changes clear held input;
- a text field or modal UI that owns the key prevents orbit activation.

The integration is optional and reflection-based. The camera mod must continue
to start without Keybinder and use `orbitKey` as its fallback.

### 13.2. Input ownership

- Keybinder-owned chords are not also handled by the camera's native fallback.
- Context checks remain inside the camera mod, including viewport and item-placement checks.
- A bridge failure is logged once and falls back without disabling the camera.
- Removing or disabling Keybinder must leave console commands and fallback orbit operational.

## 14. ModLoader lifecycle and compatibility

The main class must implement all lifecycle marker interfaces used by Ago's
client mod loader:

```text
WurmClientMod
Configurable
PreInitable
Initable
ConsoleListener
```

Required phase behavior:

- `configure`: validate settings and create camera state;
- `preInit`: install bytecode hooks without loading game runtime classes;
- `init`: arm deferred runtime initialization without loading game classes;
- first rendered world frame: cache reflected members, initialize collision,
  register the `tp` console listener, and announce readiness.

A valid log includes all five of these milestones:

```text
Reading mods\WU-third_person_view.properties
Loading org.wuthirdpersonview.client.WUThirdPersonViewMod as WU-third_person_view
WU-third_person_view <version> hooks installed
WU-third_person_view armed; runtime initialization is deferred
WU-third_person_view ready
```

Seeing only `Reading` and `Loading` is not sufficient: it means the mod class was
found, but its operational lifecycle was not completed. `Unknown command: tp`
means the console listener was not registered.

Required client members are pinned and checked by automated contract tests.
Reflection lookup is cached and never repeated in the per-frame update loop. If
an essential hook cannot be installed, the failure must be visible in
`client.log`, and native first person must remain usable.

## 15. Performance and reliability

- Camera behavior must remain consistent at 30, 60, and 144 FPS.
- Per-frame code avoids repeated reflection lookup.
- The camera transform must never contain `NaN` or infinity after vertical views, teleportation, or zero-distance collision results.
- A failed collision backend falls back safely and logs a warning once.
- A recurring diagnostic must be rate-limited.

## 16. Acceptance criteria

### 16.1. Loading and standalone operation

1. With no other third-person camera mod installed, the startup log reports both `hooks installed` and `ready`.
2. `tp status` is accepted by the console and reports camera state.
3. `tp toggle`, `tp zoom-out`, and direct wheel input visibly control this mod's camera.
4. Removing Thource's or any other third-person mod does not affect this mod's operation.

### 16.2. Zoom and view transitions

1. Zooming out from first person activates third person at `z = 0`.
2. At `z = 0`, the camera remains at least 1.20 metres behind the anchor and never enters the character's head.
3. A further zoom-in from `z = 0` activates native first person.
4. Normal toggling restores the remembered third-person zoom.
5. At a fixed native yaw and pitch, zoom changes only boom length; the normalized camera-to-anchor direction remains unchanged.
6. Horizontal look rotates the camera around the head while keeping the head on the view centre line.
7. Looking up places the rear camera end below the head, and looking down places it above the head.
8. Zoom never modifies FOV.

### 16.3. Orbit

1. Without the hold action, normal look input turns both character and camera.
2. While held, the camera can orbit 360 degrees without changing character direction.
3. Releasing the action returns smoothly behind the character.
4. Re-pressing during return continues without a camera jump.
5. Orbit does not steer a mount or vehicle.

### 16.4. Collision

1. Zooming or orbiting near a constructed wall cannot put the camera centre or effective radius behind the wall.
2. Zooming or orbiting in a cave cannot cross a closed cave-tile boundary.
3. A narrow space may force native first person while preserving the selected zoom.
4. Moving back into open space restores the selected third-person distance smoothly.
5. Surface players are not retracted by cave geometry from another layer.
6. Collision hysteresis prevents frame-to-frame flicker.
7. No camera mode can leave the final camera centre below the active floor plus `surfaceClearance`.

### 16.5. Head tracking and flying freelook

1. Normal horizontal look turns the visible head while standing, with body follow at the configured yaw limit.
2. Normal vertical look visibly pitches the head in the same direction as the camera axis.
3. Walking recentres the rendered body and head without changing server movement.
4. Held orbit can inspect the character through 360 degrees without turning the head or body.
5. Flying freelook begins at the current camera, responds to mouse look and movement bindings, and leaves the character in place.
6. Leaving flying freelook restores the selected first- or third-person mode.
7. Entering flying freelook shows `FREE LOOK MODE` above the bottom Event window; leaving it removes the marker on the next rendered frame.
8. The marker stays correctly anchored after a resolution change and does not block mouse interaction beneath it.

### 16.6. Wurm-specific behavior

1. The wheel still rotates an object during item placement.
2. UI scrolling does not zoom the camera.
3. Server action range and line-of-sight validation remain unchanged.
4. Embark, disembark, teleport, surface/cave transition, death, respawn, and reconnect do not leave stale camera coordinates.

### 16.7. Keybinder

1. The `3rd Person View` catalog appears only when this camera mod is installed.
2. Every catalog item is a complete command without Source, Tool, or Target fields.
3. Reassigned bindings persist and begin working through normal Keybinder rules.
4. Held orbit works with a key other than `Left Shift` and ends on release.
5. One wheel event causes one zoom step.
6. Removing Keybinder leaves native fallback input and `tp` console commands functional.

## 17. Outside the MVP

- making walls, roofs, characters, or vegetation transparent instead of retracting;
- searching for an alternative sideways camera position around an obstacle;
- cinematic camera paths or photo mode;
- combat-target lock-on;
- speed- or zoom-dependent FOV;
- server-enforced third-person visibility restrictions for PvP;
- controller-specific input beyond controls already exposed as normal keybinds.

## 18. Product decision

This mod is a head-centred camera boom rather than a fixed world-space offset.
It keeps four concepts separate: native character look direction, user zoom,
independent held orbit, and temporary collision distance. In normal follow
mode the camera always lies on the character's eye/head view axis, while zoom
changes only distance and collision may temporarily shorten that distance.
