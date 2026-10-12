Third Person View 0.4.9
Third-person, orbit and flying cameras for the Wurm Unlimited client.

Download and illustrated guide:
https://github.com/chamomilo/WU-3rd-person-view
Latest release:
https://github.com/chamomilo/WU-3rd-person-view/releases/latest

INSTALLATION

Requires the Wurm Unlimited client and a compatible client mod launcher.

1. Close the entire Wurm Unlimited client.
2. Extract the ZIP into the client directory containing WurmLauncher.exe.
   Merge the archive's mods folder with the existing mods folder.
3. Start the client through its mod launcher and enter the world.
4. Enter tp status in the console to check the camera.

The archive contains exactly these four files:
  mods/WU-third_person_view.properties
  mods/WU-third_person_view/WU-third_person_view.jar
  mods/WU-third_person_view/README.txt
  mods/WU-third_person_view/keybinder/WU-third_person_view.keybinder

When updating, back up your customized properties file and reapply your camera
settings after extraction. Saved state.properties is not included in the ZIP.
Use one third-person camera mod at a time.

CAMERA MODES

Third person follows the animated eye position. Smooth zoom ranges from
1.20 to 12 metres. Zooming closer at the nearest distance returns to first person.
Orbit looks around the character without turning it; releasing orbit smoothly
returns the camera behind the character.
Flying freelook moves the camera while the character stays in place.
A FREE LOOK MODE marker appears while flying is active.

CONTROLS

Keybinder is optional:
https://github.com/chamomilo/wurm-keybinder

Compatible versions expose Catalog: 3rd Person View for assigning keys.
Import the bundled WU-third_person_view.keybinder preset for these bindings:
  V                       Toggle first-person / third-person view
  Mouse Wheel Up / Down   Zoom in / out
  Home                    Reset camera behind the character
  Left Shift, held        Orbit without turning the character
  F8                      Toggle flying freelook

Without Keybinder, the mod handles the mouse wheel and its configured orbit
key (LSHIFT by default). Use console commands for the other actions.

In flying freelook, mouse look aims the camera. Normal forward, back, left
and right movement bindings fly it. Shift accelerates; Alt slows movement.
Leaving freelook restores the previous first-person or third-person mode.

CONSOLE COMMANDS

  tp toggle              Toggle third-person view
  tp on / tp off         Enable / disable third person
  tp zoom-in             Zoom in
  tp zoom-out            Zoom out
  tp zoom <0..1>         Set zoom from nearest (0) to farthest (1)
  tp reset               Reset orbit behind the character
  tp orbit-hold          Held orbit action for Keybinder
  tp orbit-on            Start orbit explicitly
  tp orbit-off           Stop orbit explicitly
  tp freelook-toggle     Toggle flying freelook
  tp freelook-on         Start flying freelook
  tp freelook-off        Stop flying freelook
  tp status              Show camera state
  tp debug on            Enable diagnostic logging
  tp debug off           Disable diagnostic logging

SETTINGS AND UPDATES

Edit mods/WU-third_person_view.properties while the client is closed.
Settings cover zoom, smoothing, collision, head animation and orbit.
View mode and zoom are saved in mods/WU-third_person_view/state.properties.

This release embeds Chamomilo updater 1.4.1 and Chamomilo UI 0.4.11.
Participating mods share one registry. The newest compatible embedded updater
is selected at startup. The registry checks releases and opens their GitHub
pages. Install downloaded release ZIPs manually.

The updater's shared language selector supports English, Brazilian Portuguese,
German and Russian. It applies immediately to capable installed mods. The
initial choice follows a supported OS language, with English fallback.
Camera commands and camera messages remain in English.

TROUBLESHOOTING AND LIMITS

If tp is unknown, enter the world, check extraction paths and client.log, then
restart the entire client. Successful initialization logs
WU-third_person_view 0.4.9 hooks installed and WU-third_person_view ready.

Wurm streams world data around the character. The camera does not request new
regions. Terrain, vegetation, objects, lighting and cave visibility follow the
camera only within loaded data. A separate cave can be entered only when its
exact tile data and floor corners have been received. Missing data blocks movement.

Cave floors, ceilings and solid walls constrain movement, including accelerated
flying. Loaded cave exits remain passable. Tight spaces temporarily return the
camera to first person without losing your selected zoom.

If the advanced collision manager is unavailable, the mod logs a warning and
retains terrain and cave checks. Surface object and building wall collision
is not guaranteed in that fallback mode.

LICENSE

Copyright (C) 2026 chamomilo.
Original project source and documentation are licensed under GNU GPL version 3.
The complete license is included inside WU-third_person_view.jar at
META-INF/LICENSE.txt. Bundled fonts retain their OFL license.
Wurm client files and third-party build dependencies are excluded.
