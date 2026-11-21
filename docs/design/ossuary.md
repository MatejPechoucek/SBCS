# The Ossuary - dungeon reward chests

A prize wheel rolls for F1-F7 / M1-M7 reward chests. Unverified in game. In a run, the chest being opened rolls; at
Croesus, clicking a run rolls only its most profitable chest (least negative if all lose), with the menu covered so a
hover cannot give it away. A setting turns Croesus off. Default tiers Obsidian and Bedrock, the rest selectable.

**Read and draw only** - never a click, packet or "Open Reward Chest" on the player's behalf (ban risk).

Licences: SkyOcean's code and repo data are MIT (its `dungeon_chests.json` completes the pools, credited in the
JSON's note), its textures are not; BCCO is all rights reserved (ideas only); Skyblocker / SkyHanni strings are used as
facts.

## Records first

`detect/dungeon/DungeonRecorder` writes `dungeon_chest` (title, every slot with SNBT and lore, the sidebar),
`dungeon_sidebar` and `dungeon_line`. **The menu strings in `ChestReader` come from other mods until records confirm
them** - "Open Reward Chest" at slot 31, the cost lore, the Croesus page's "Contents" / "Cost" lore, the back arrow
"To Catacombs - Floor VII". Next step: records of 2-3 runs, one Master Mode, Croesus and a Kismet reroll.

## Pure logic

- `ChestReader`: a chest's menu (null until "Open Reward Chest" is there); a run's page from each head's lore.
- `PriceKeys`, `ChestValue` (best item ranks essence last; the key's price is added to the cost).
- `RewardChest.fingerprint` (price keys, so the boss room and Croesus agree), `RollMemory` (72 h).
- `DungeonPool`, `RollPicker` (which menu rolls, which chest; a run's page is remembered as `RUN|...` so it rolls once
  - a chest rolled in the boss room does not count, and the page then rolls its best other chest).

### The pools

`dungeon_pool.json` has every chest's own pool - Wood to Obsidian for all 14 floors, Bedrock for F5-F7 / M5-M7 - with
Hypixel's weights and the wiki's added cost per item (a borrowed pool showed Wither armour in a Wood chest). Source
per chest is in its `sources`:

- The wiki's Loot / Master_Mode_Loot pages, copied as data into `tools/fixtures/dungeon_loot/<floor>.csv` (F2, F7,
  M2-M7).
- Tables cut short by the fetch completed from SkyOcean, whose Obsidian and Bedrock weights are Hypixel's (every total
  checked within 0.1%).
- F3-F6's lower chests from their Master Mode page (F2 and F7 show both modes share them).
- F1/M1's lower chests derived from F2's (Bonzo's items in Scarf's places) - **unverified, replace when the pages
  load**.
- The dyes are not pool rolls (Hypixel rolls them after the pool); they sit under `extras` for their looks.

Regenerate with `tools/gen_dungeon_pool.py <SkyOcean dungeon_chests.json> [neu cache]`.

## The roll

- `anim/OssuaryTimeline`: an event log; pulls are solved so the wheel lands exactly on its segment whatever the
  strength. The chain is optional: the hand waits `AUTO_WAIT`, then keeps going at watch pace. Bands BUST / BLUE
  (>= max(1M, the menu's floor)) / PURPLE 5M / GOLD 25M / JACKPOT 150M or a floor's jackpot item; endings BUST / WIN /
  JACKPOT / DYE.
- **One big wheel** (`OssuaryTimeline.SEGMENTS` 16, `VEILS` {0, 6, 11}). The stop is always a veiled segment, chosen
  by the pull alone, so it stays honest; filler is fixed per segment.
- `render/OssuaryScene` (through ChopScene's box renderer). The face is one CPU image (`OssuaryScene.face`, 128 texels:
  gold rim, bulb sockets, studs, the tell lighting the stop's segment) drawn through the camera's affine map, its
  outline the silhouette stamped 8 times - two `RasterImage`s in `OssuaryRoll` (face and ink), since an upload is once
  a frame. Bulbs chase idle, race rainbow while spinning, flash the tell and land on the truth; a bust's go out one by
  one. The flapper kicks on every peg. Master Mode: red, barred with an iron bar held by seals; M7: five dragon gems on
  stalks. The hand hauls the rim down; drag the wheel to spin it.
- **Bigger drops, more excitement**: one shockwave ring per band, a coin fountain from gold up, a bigger showcase and a
  punch, the fanfare from gold. Each drop its own flourish (`fxOf` / `itemFx`).
- The machine steps back and darkens for the showcase; the 3D never fades - it shrinks away over `FADE_MS`.
- `audio/OssuarySound` is event-driven: the host asks for the cues due since the last frame.

## In game

- `DungeonRolls` (every tick, Hypixel only) reads the open `AbstractContainerScreen`'s slots into `SlotView`s and keeps
  the live `gui/OssuaryRoll`.
- `mixin/ContainerCoverMixin` cancels the menu's `extractRenderState` and swallows its mouse and keys while `covers()`
  (Escape ends the roll and closes as usual; Space / Backspace skip). `ChestBackgroundMixin` drops the chest panel.
  `GuiOssuaryMixin` keeps the frame's extractor as it is handed to `Hud.extractRenderState` and draws the roll before
  `Hud.extractDeferredSubtitles` - after the screen and toasts, the only unconditional call there. All `require = 0`
  with `Safety.hook(3/4)`; a cover nothing draws lifts after a second. `HudHiddenMixin` answers hidden while covered.
- A candidate menu is covered from its first frame for up to 1.5 s while its contents load; a run's page waits up to
  1.2 s for prices before picking.
- **Test chests** (`anim/OssuaryTests`, `gui/OssuaryTestScreen`; Bedrock from Floor V, else Obsidian) are the floor's
  real pool at market prices (`OssuaryRoll.market`: bazaar instant sell, the key's instant buy, else lowest BIN, else
  the wiki added cost or 60k). The outcome only picks which pool item is best; a band a floor cannot reach plays its
  nearest real item.
- Config `dungeon*`, stats key "dungeon". `/sbcs test dungeon <f1..m7> <bust|blue|purple|gold|jackpot|dye> [tier]`.

## Tests and tools

- `tools/DungeonChestTest`: menu reading, picking, every wiki row (weight and added cost) against the pool, every
  chest's total against the wiki's.
- `tools/OssuaryTest`: hashes every frame (and the face image) and sound before the reveal over floors x tiers x input
  scripts x chain/watch x outcomes.
- `tools/OssuaryPreview` (`FLOOR:TIER:KEY:WORTH[:chain]`, WORTH 0 a bust).
