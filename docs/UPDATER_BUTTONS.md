# Shared updater interface

Third Person View embeds the complete canonical Chamomilo updater module
1.1.0. The module owns the Mod updates window, buttons, artwork, fonts,
preferences and HUD lifecycle. The mod retains only its update metadata and
the three shared lifecycle calls.

The module pins Chamomilo UI 0.4.4 and includes its compact repeating textures,
four bundled Alegreya font files and OFL license. Its actions, Close control,
Force language selector and language options share one measured Alegreya Sans
SC size and baseline, including normal and hover weights. The closed selector
and its detached popup use coordinated typography. Native input, scrolling,
startup preferences and protocol-1 public APIs remain compatible.

The title, mod names, descriptions, version text, links, startup checkbox and
Main menu entry also use bundled fonts. The default registry window is 720 px
wide at the 1080p baseline. Mod headings use 20 px fonts, with project links
beside each heading and installed/release versions together on the bottom row.
Waypointer appears first, Keybinder second, and the remaining mods are sorted
alphabetically.

Force language defaults to Off. A selected language is saved in the user's
shared updater preferences and applied on the HUD thread to installed mods
with supported language APIs, including newly observed mods. Unsupported
languages retain a mod's local choice. Off stops future shared applications;
it does not undo choices already saved by participating mods.

Do not add private updater sources or resources to this project. Make updater
changes in the canonical reference at C:/projects/updater; its
`docs/INTEGRATION.md` describes the module and consumer release gate.

Every build clean-builds the reference and runs its native layout/input,
shared-loader and packaging checks. Preview images are written to the
reference's `build/updater-preview.png` and
`build/updater-buttons-preview.png`. Consumer checks compare exact module
entries and bytes in the mod JAR and installable ZIP. Actual in-game GPU
rendering remains a manual check.
