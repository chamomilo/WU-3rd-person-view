# Camera settings

Open **Mod updates** from Wurm's menu, then click **Settings** on the Third
Person View card. All 24 camera parameters apply during play. Each label or
dropdown has hover help. Valid text edits save after a short pause, or immediately
when you press Enter; invalid edits remain marked for correction.

| Group | Settings |
| --- | --- |
| Camera | Third person enabled, default zoom, zoom step, minimum/maximum distance, head anchor offset |
| Head animation | Animate head, maximum yaw/pitch, body follow time |
| Smoothing and collision | Zoom smoothing, collision return time, camera radius, collision margin, surface clearance, enter/exit first-person distance |
| Orbit | Orbit key, sensitivity, return behavior, return time, maximum pitch |
| Input | Zoom input provider: auto, native or keybinder |
| Advanced | Camera diagnostics |

Changing tuning preserves the current zoom, view mode and orbit direction.
**Third person enabled** explicitly switches the view. **Default zoom** sets
the current zoom while retaining first/third-person mode.

Minimum distance is 1.20–3 m; maximum distance is 1.30–50 m and must exceed
minimum distance by at least 0.1 m. The exit-first-person threshold must exceed
the enter threshold by at least 0.05 m. Zoom values use the fraction 0.01–1.
The orbit key accepts LWJGL key names such as `LSHIFT`, `RSHIFT` or `NONE`.

Only edited settings are saved as overrides at
`%USERPROFILE%/.chamomilo/mod-settings/wu-third-person-view.properties`.
These override the initial values in `mods/WU-third_person_view.properties`.
To remove an override, close the client and remove that key from the override
file. Current view mode and zoom are also remembered in
`mods/WU-third_person_view/state.properties`.

The shared language selector translates window controls. Camera setting names
and help remain in English. Changing the language preserves unfinished input,
selection and scroll position.

Other participating mods may expose startup-only settings. Saving one shows a
restart warning: **Continue configuring** keeps the client running, while
**Exit game** saves valid pending edits and opens Wurm's normal quit confirmation.
