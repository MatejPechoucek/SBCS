# 11 - Any dye, anywhere

My idea. Every animation gives its dye the grandest ending, but dyes drop from far more places than SBCS animates
(Pearlescent from Zealots, Dung from pests, Aquamarine from water mythics, Wild Strawberry from crops, Tentacle from
Kuudra, and more). A dye is the rarest thing a player will ever see drop, and missing one because its source has no
cutscene is the worst miss there is.

## Trigger

Any game line announcing a dye drop for **this player**, from a source no other SBCS animation handles.

## Detection

**Theory.** Hypixel announces dyes as `WOW! <player> found a <Name> Dye!` (Diana's Mythological Dye line is
**Proven**: `§d§lWOW! (.*?) §6found a §2Mythological Dye`). Match "WOW!" + "Dye", check the player name is ours (as
`DianaWatcher` does), and step aside when another watcher claims the same line (Diana, Eruption, the Vault).

## Cutscene concept: "Colour"

A generic but spectacular dye ending: time stops, the dimmed game greys texel by texel, a single drop of the dye's
colour falls into the middle of the screen and the colour floods outward in pixel rings, then the dye rises in its own
colour with "WOW!", its name and price. The colour comes from the dye's own hex (NEU repo has them) - so one scene
serves every dye.

## Open questions

- Should this also play for dyes that already have a source-specific cutscene but whose kill was missed (as a safety
  net)?
