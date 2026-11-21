# The Hunt - Superpairs

An ultra-rare Superpairs tile (VII books, the Nadeshiko Dye, Experiment the Fish, the Metaphysical Serum, a Legendary
Guardian) is drawn as a black box in the menu so the player never sees the item; the cutscene reveals it after the
game, and the hunt for its pair carries the tension. Unverified in game. Spec: `docs/ideas/06-superpairs.md`.

**Read and draw only** - nothing is clicked or sent; clicks, keys and every other tile are the game's.

## Pure logic

- `detect/superpairs/PairsReward`: a tile -> NORMAL / RARE / ULTRA / DYE, from name, item, SkyBlock id, a book's
  enchantments, pet type and tier, lore.
- `PairsGame`: one game. Boxes are lettered A, B... by first sighting - the letter tells which boxes pair and nothing
  else. Matched = both tiles face up at once, or listed on a rewards page. Turns, clicks left.
- `anim/PairsTimeline`: one box after another; per box `IN`, the colour steps from `WIND` (aqua -> violet -> pink,
  shrinking to `MIN_STEP` - without the floor the geometric steps never reached the reveal), `REVEAL` 3600; a box that
  got away is grey (`MISSED_REVEAL` 1500).
- `render/PairsScene`: an obsidian box through ChopScene's box renderer. RARE shatters; ULTRA unfolds and a 3D book
  opens and riffles with the game's sga glyphs; DYE stops the world grey for three heartbeats, then petals. The 3D
  shrinks, never fades.
- `render/PairsHunt`: the cover and the overlay round the menu - embers, clicks as pips, a crack of light between a
  found pair. `audio/PairsSound` + `PairsVoices`.

## The price floor and the panel

A rare tile is boxed only if worth `pairsBoxMin` or more (`PatternConfig.PAIRS_BOX_MIN`, default 400k; bazaar instant
sell, else lowest BIN, asked for when the game starts; unknown worth is boxed). **Decided once**, when the tile is
first seen (`PairsGame.Box.hidden`), so a late price never uncovers one; only hidden boxes are lettered, hunted and
revealed.

`PairsHunt.panel` (`pairsPanel`) sits right of the menu: "RARE DROP!", a line per rare tile - "Hidden reward" for a box
(never named), name and worth for an uncovered one - and Find its pair / Found! / Got away.

## Facts from a real game (`tools/fixtures/superpairs_game.jsonl`)

- With no clicks left Hypixel turns the whole board over. Without care that reads as a match;
  `PairsGame.revealed` (more than 4 slots changing at 0 clicks) stops matching after it.
- The rewards page lists what was won in the Superpairs item's lore after "Rewards:" ("+ Titanic Experience Bottle"),
  which settles every box both ways (`rewardsListed`, an `UNMATCH` event).
- Face-down tiles are cyan stained glass; the title is "Superpairs (Metaphysical)", the clicks "Remaining Clicks: N" on
  slot 4, the rewards page "Superpairs Rewards".

Still theory until more records (`detect/superpairs/PairsRecorder`, `superpairs_menu`, every flip with "Drop records"
on, each tile stamped with how it was read): other stake titles, how a matched pair stays shown. Another mod's
Superpairs solver that draws remembered items over face-down tiles is outside our cover.

## In game

- `detect/superpairs/PairsWatcher` (every tick, Hypixel only): the menu's slots into the game; the DecisionLog
  narrates every box, match and game end; a heartbeat quickening with the turns while a box waits. The game ends when
  no Superpairs menu has been open for 1.5 s, is counted (stats key "superpairs", one run per game, its first find the
  drop), and the reveal waits for no screen and no cutscene (10 min at most).
- `mixin/PairsCoverMixin` (`require = 0`, `Safety.hook(5)`): `extractSlot` HEAD draws the box instead of the item -
  **decided from the stack being drawn**, so the item never shows for a frame before the tick - and `extractTooltip`
  HEAD drops the tooltip over a box. It also hands over the menu's rect. The overlay is drawn from `GuiOssuaryMixin`
  (`Safety.hook(6)`).
- Chat is held from the first box until the reveal is over (`holdsChat`).
- `gui/PairsScreen`, `SbcsClient.openPairs`, menu card "Superpairs - The Hunt", config `pairs*`,
  `/sbcs test superpairs fish|serum|guardian|book|dye|missed|two`.

## Tests and tools

- `tools/PairsTest`: frame and sound honesty per box alone and as the second box, tile reading, bookkeeping, beats,
  the draw-call budget, the overlay staying off the menu.
- `tools/PairsPreview` (also `hunt`, a mock menu with the real cover and overlay; `-Dstandin` recolours a skin for
  heads that cannot be fetched), `tools/PairsSoundtrack`. Skins come from `superpairs_heads.json` (NEU).
