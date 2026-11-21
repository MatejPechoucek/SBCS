# Fin and Wisp - the small announcements

Two pop-ups, not cutscenes: HUD, chat and game sounds are untouched, no dim. Drawn by `drawOwnHud` after the HUD, at
`h - LIFT` (82) - at `h - 64` they covered the action bar (vanilla draws it at h - 72 .. h - 63). Shapes are character
maps drawn as 2-GUI-pixel squares with a dark outline. Speed: `fishingSpeed`, `ghostSpeed` scale their own `elapsed()`.

## Fin - the Fishing Festival's Great White Shark

Announces a **Megalodon** pet or a **Great White Shark Tooth**. `detect/SharkDrops` reads the drop line (pure; any
"... DROP!" prefix; the pet's rarity is in the text, e.g. "PET DROP! EPIC Slug (+1300☘)"; colour codes stripped).
**No real line has been seen yet** - DropRecorder keeps every fishing line as `fishing_line`; check the first.

`render/FinPopup`:

- A pixel fin rises out of a two-row water strip (`RISE`), swims over from `FIN_FROM` and dives; a fishing hook on a
  line drops in (`HOOK_IN` .. `HOOK_UNDER`) and at `CHOP` it is yanked up (`YANKED`) over the drop (`Voices.finYank`).
  The cleaver was dropped from Fin - it is the Scatha's.
- The drop pops out: the Megalodon's NEU skin (`HeadSkins.FISHING`, `fishing_heads.json`, the same skin both
  rarities) on a 3D head three-quarters on (yaw 35 with a 9 degree sway, no spin - its eyes are on the sides), or the
  tooth as the game's ghast tear icon. Name in the rarity colour.
- Price: the pets show what George pays, fixed (`GEORGE_LEGENDARY` 2.5M, `GEORGE_EPIC` 500k), labelled "at George";
  the tooth the bazaar's instant sell.
- Sounds: `onFinRise`, `onFinReveal` (3-note chime, 4 for a legendary), on `fishingVolume`.
- **The bust** (`detect/SharkWatcher`, written with no real line or nametag captured, after SkyMaxx's
  `SeaCreatureDetector` rules, reusing DianaWatcher's HP reading):
  - Ours: a "Great White Shark" nametag first seen within 16 blocks, from 2 s before to 4 s after a catch line (any game
    line naming the shark that is not a drop, a charm or a loot share).
  - Dead: HP reads 0, or the nametag goes at half health or less, within 32 blocks, after 6+ ticks, no teleport.
  - Bust: no drop line from 1.5 s before the death to 1.5 s after.
  - `FinPopup.showBust()`: the fin ducks, the swing goes empty into the water, splash and bubbles (`onFinMiss`). It
    never interrupts a drop that is showing.
  - Nametags are recorded as `fishing_mob`. **Check the first records.**
- Config `fishingAnnounce`, menu "Fishing Festival - Fin", `/sbcs test shark megalodon|legendary|tooth|bust`. A
  legendary Megalodon awards a Ledger stamp.

## Wisp - the Dwarven Mines' Ghosts

Announces **Sorrow** or **Plasma** from a Ghost in the Mist. Works in game. `detect/GhostDrops` (pure): any
"... DROP!" line whose item is Sorrow or Plasma by name, alone (not Plasmaflux), no player chat - e.g.
`RARE DROP! Sorrow (+325 ✯ Magic Find)`. Ghosts have no nametag or health bar, so there is no kill and no bust.
DropRecorder keeps `ghost_line`.

`render/GhostPopup`: a mist strip (`MIST`); the Ghost - a charged creeper, front on, pale and flickering, the charge as
diagonal blue stripes crawling in a ring two squares out (`GHOST`, `dist`) - rises (`RISE`-`RISEN`), bursts into wisps
(`BURST` 1180, every square drifting up), and the drop pops (`POP`) with name and bazaar instant sell. The icon is
Hypixel's pack's when present, else the ghast tear for Sorrow and a pixel orb (`ORB`) for Plasma. Sounds
`Voices.ghostWail` / `ghostBurst`, a chime at 220 Hz, on `ghostVolume`. Config `ghostAnnounce`, `ghostPrice`;
`/sbcs test ghost sorrow|plasma`.

## Tools

`tools/PopupPreview` drives the real `FinPopup` / `GhostPopup` frame by frame through `tools/GuiPreview` and captures
the synth's cues into a WAV.
