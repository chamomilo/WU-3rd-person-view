# Technical specification: Third Person View for Wurm Unlimited

Document version: 3.1
Updated: 10 October 2026

## 1. Scope

Third Person View is a client-only camera mod. It supplies first/third-person
switching, head-centred zoom, held orbit, flying freelook, collision handling,
visible head tracking, persistent camera state, and camera-aware rendering
detail. It does not change server movement, action range, combat, or line of
sight.

Equipment stowing is intentionally outside this mod. It is provided by the
independent Holster Weapons distribution.

## 2. Camera states

### Native first person

- Wurm's native position and orientation remain authoritative.
- The last third-person zoom is retained.
- Collision may temporarily return a third-person camera to this state without
  changing the selected zoom.

### Third-person follow

- The pivot is Wurm's animated eye/head camera point.
- Native look yaw and pitch define the complete camera boom direction.
- Zoom changes boom length only and never changes field of view.
- The range uses `minDistance` through `maxDistance` with smoothstep mapping.

### Held orbit

- The configured hold action rotates the camera without changing character,
  mount, or vehicle direction.
- Releasing the action returns smoothly behind the character when enabled.
- Horizontal orbit covers 360 degrees; vertical orbit is clamped.

### Flying freelook

- The camera detaches from the character and begins at the current viewpoint.
- Native free-camera movement flies the viewpoint without moving the character.
- Leaving the mode restores the selected first- or third-person camera.
- A non-interactive `FREE LOOK MODE` marker remains visible while active.

## 3. Zoom model

For normalized zoom `z`:

```text
smoothstep(z) = 3*z*z - 2*z*z*z
distance(z) = minDistance + (maxDistance - minDistance) * smoothstep(z)
```

`z=0` is the minimum valid third-person distance. A further zoom-in switches to
native first person. Zooming out from first person re-enters third person at
`z=0`. The shipped range is 1.20 to 12.0 metres.

## 4. Collision and floor clearance

The camera sweeps a sphere from the head anchor toward the desired viewpoint.
The nearest opaque collision shortens only the actual boom distance. Loaded
cave air is checked separately against interpolated floors, ceilings and solid
tiles because those boundaries are not consistently represented by the general
collision scene. The camera footprint includes its radius and collision margin.
Expansion after an obstruction is smoothed;
retraction is immediate.

Every camera mode is clamped to at least `surfaceClearance` above the loaded
surface or active cave floor. If the available boom is shorter than
`firstPersonEnterDistance`, the camera temporarily uses native first person and
returns only after `firstPersonExitDistance` is reached.

Native flying-camera movement is captured before each frame and constrained
along its complete path after movement, so large or accelerated steps cannot
skip cave walls, floors or ceilings. Geometry is sampled at most 0.10 metres
apart and the first obstruction is refined. Unloaded cave space is blocked.

The allowed space is the union of loaded cave air and air above the loaded
surface. `TILE_CAVE_EXIT` has no ceiling, matching Wurm's rendered geometry;
its floor and neighbouring solid tiles remain blocking. This permits passage
through real exits in both directions, including when a flying camera leaves
the cave while the character remains underground. A closed cave ceiling never
becomes an exit merely because a movement endpoint is above ground.

## 5. Visible character response

Normal look input pitches the local character's `Head` joint and permits limited
head yaw before the rendered body follows. Moving recentres the body over
`bodyFollowSeconds`. Held orbit and flying freelook do not rotate the character.
The adjustment runs after native forward kinematics and before skinning, so the
base locomotion animation remains active.

## 6. Camera-aware rendering detail

Wurm's decoration renderer normally owns a fixed 15×15 tile grid: the focus tile
plus seven offsets on each side. Vanilla centres this grid on the character. A
camera moved three tiles behind the character therefore has only four detailed
tiles left on its rear side. The mod expands this grid to 17×17, providing the
requested eight full tiles in every direction.

Third Person View replaces only visual position queries in these subsystems:

- terrain decoration chunks and individual sprite distances;
- terrain LOD tessellation;
- the 16×16 cell grid used by loaded objects and vegetation;
- cell distance and occlusion tests;
- `WorldRender` environmental ticks and floating render origin.

In normal third person, the visual focus moves from the player toward the camera
using the same smoothstep zoom progression. At maximum zoom the camera owns the
focus and receives eight full decoration tiles on every side. In native first
person the player remains the focus.

In flying freelook, the camera immediately becomes the full visual focus. The
terrain, vegetation, object-cell grid, occlusion point, and render origin follow
it. The original player getters are not changed globally, so gameplay and
network logic remain player-centred.

### Data-availability boundary

This change can improve only data present in the client. The server continues
streaming creatures, structures, items, and high-resolution near-terrain data
around the character. Free flight inside that loaded region can use camera-based
detail. Beyond it, the client may have only distant terrain or no object data;
a client-only mod cannot reconstruct or request missing authoritative data.

## 7. Input and commands

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

Mouse-wheel zoom operates only over the 3D viewport and does not steal item
placement or UI scrolling. Keybinder-managed wheel chords take precedence over
the native fallback. The command catalog supplies complete commands without
tool, source, or target arguments.

## 8. Configuration

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

Camera mode and zoom are saved atomically in
`mods/WU-third_person_view/state.properties`.

## 9. Lifecycle and compatibility

The main class implements `WurmClientMod`, `Configurable`, `PreInitable`,
`Initable`, and `ConsoleListener`. `preInit` installs bytecode hooks without
performing runtime reflection. Runtime fields and the console listener are
initialized on the first rendered world frame, after other mods have completed
their hook phase.

The pinned client contract verifies every hooked method. A required hook failure
is fatal during initialization and is written to `client.log`. Collision backend
failure is non-fatal and falls back to terrain checks.

Do not combine this mod with another third-person camera mod that patches the
same `WorldRender` pipeline.

## 10. Acceptance criteria

1. First/third-person transitions retain the selected zoom and never alter FOV.
2. Follow and orbit stay centred on the animated eye/head pivot.
3. Collision never places the camera through a loaded blocking surface or cave
   tile wall; narrow space can temporarily force first person.
4. UI wheel input and item placement are not consumed by camera zoom.
5. Held orbit and flying freelook do not move or steer the server character.
6. Visible head tracking preserves native locomotion.
7. At maximum third-person zoom, detailed decorations surround the viewpoint
   for eight full tiles in every direction.
8. In flying freelook, terrain and loaded visual detail follow the viewpoint,
   while missing server-streamed data remains an explicit limitation.
9. The installable distribution contains only camera functionality; Holster
   classes, hooks, commands, configuration, and state are absent.
