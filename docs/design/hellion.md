# Hellion - the Inferno Demonlord (blaze slayer)

Tier IV only. The blaze's attunement shield cycles; where it lands is the tell, and **grey (Ashen) is the bust**.
Wins are drops worth 500k or more. `anim/HellionTimeline`, `render/HellionScene`, `gui/HellionScreen`,
`audio/HellionSound`.

## The timeline

`HellionTimeline` is pure. `Drop` (bust, Duplex I, Scorched Power Crystal, Subzero, Wilson's, Paragon Shard, Archfiend
Dice, High Class Archfiend Dice, Flame Dye) carries its landing attunement, ending, price ID, art (a
`HeadSkins.SLAYER` skin or a vanilla icon) and odds.

- `APPEAR` 350; the shield's `STEPS` (four rounds of Ashen, Spirit, Auric, Crystal from 700 ms, 420 ms a step
  shrinking 9% each); `HANG` at the last step's end (rods snap in, colours flicker); `REVEAL` 4800. Nothing before
  the reveal depends on the drop. A bust ends at ~2 s after; `arrive()` per ending is when the drop is shown.
- The rods never close in; only `spin` grows (1 + 9x^2, `SPIN_TOP` 5), integrated in `HellionScene.ticks`.

## The scene

- **The blaze is vanilla's `BlazeModel`** (read with javap): an 8-cube head and twelve 2x2x8 rods in rings of radius
  9 / 7 / 5 at model y -2 / 2 / 11, vanilla per-ring rates (middle ring reversed), the cosine bob, frozen at the
  reveal. Parts are depth-sorted, outline first. Texture `textures/entity/blaze/blaze.png`, adopted per frame.
- **Sigils**: 5x7 Elder Futhark runes (thurisaz, algiz, othala, dagaz) per attunement, the current one lit with a
  glow, the rest at half.
- **The pillars are the fight's own**: four terracotta blocks (brown, red, orange, yellow bottom up) with fire on top
  and a "6s 8 Hits" nametag. The seconds count to 0 at the reveal (the last second in tenths, red); hits come off with
  the clangs. They sit along the camera's resting sideways (yaw 26); `pillarX` = min(33, 18.5 * w / h).
  `pillarPull` 0.92 / the camera's zoom while it leans in.
- **Fire** is vanilla's `template_fire_floor` model: four planes, 0.8 px either side of the middle, turned 22.5 degrees
  with rescale, 22.4 px tall (`FIRE_CORNERS`). The fire strip is read through `readGameTexture` (an animated texture
  adopted by name gives one frame), normalised to 32 frames of 16 px (`HellionArt.fireStrip`), greyed (`greyFire`) and
  tinted with the shield's colour (`fireTint`, Ashen lifted to #8E8E8E). A warm core - the real fire's bottom five rows
  - over it. The Dye uses real orange fire.
- **Stages** (`blazeStage`): 0 classic pillars; 1 no pillars (`noPillars`, the blaze 1.25x, the tag over its head);
  2 pillars explode in turn (`countdown`: each tag counts to its own `PILLAR_BLOW` 2300 / 3600 and is blown apart -
  `blast`, then the tag moves over the blaze). The same on every run.
- **Click to hit** (`blazeClick`): `HellionScreen.hits()` holds the `SceneClock` just short of each hit until the attack
  key goes down (up to 300 ms early counts) or 1.5 s passes; "Left click to hit!" shows. Only the pace changes.
- Bust: rods drop and tumble, the head greys and crumbles to ash; it snuffs left then right (`snuffPuff`).
- Win: shards of the landing colour, rods blown out, the head implodes, the drop rises with name / odds / price.
- Banner: `HellionScene.bannerWord` from `HellionDrops.freshTier()` (the last drop line's tier word, 10 s), coloured by
  `tierColour`.

## The jackpots

- **Dice** (`anim/HellionDice`): a Blender rigid-body throw (2 m cube, floor at mod y -8, 60 fps, settles at frame 124)
  baked to `assets/sbcs/hellion_dice.json` - per frame x y z + the 3x3 rotation, row-major, columns = the die's axes.
  One throw, two tracks: the rotation is turned so each die's **6** ends up (Archfiend: back face -Z; High Class: right
  side -X - pips read off the NEU skins). Thrown at `DICE_LAUNCH` (reveal + 220), drawn with `ChopScene.boxAxes`,
  shadow, sparks and a thud per `bounces()`, then the camera leans over it. Clicks come from `HellionDice.tumbles` (the
  up face changing near the floor). The preview's classpath must include `src/main/resources` for the track.
- **High Class**: bullet time - `diceMs(t)` plays the track at `SLOW_RATE` 0.35 between `SLOW_FROM` 1150 and `SLOW_TO`
  1560 track ms (between the hard landing and the tip onto the 6); `diceReal` is the inverse, and every dice sound and
  spark goes through it. The pips (`PIPS`, six 2x2 on the -X face) light one per `PIP_STEP` 130 after `flatAt()`,
  tinted gold. At `jackpot()`: shake, shockwave, burst, rays, a coin fountain, `erupt()` pushes the pillars up. Banner
  "PRAY TO RNGESUS DROP!" in #AA00AA. Score in D phrygian dominant.
- **Flame Dye**: time stops at `FREEZE` (reveal + 260) - `phys(t)` is the clock rods, shards, fire and embers run on,
  held until `IGNITE` (reveal + 2400), with `HEARTS` at freeze + 400 / 1120 / 1840 pulling rings of cold squares in.
  The frozen world goes cold blue-dark (a tint cannot make yellow grey). Then two rings, the rods turn fire-coloured,
  a flame-wall `curtain()` along the bottom, `vortex` arms into the dye, fire `rays`, the name colour cycling.
  `arrive` = IGNITE + 1400. A big glow over the dimmed game reads as a wash - keep flashes small.

## Sound

`HellionSound.schedule(tl)` (`Voices.hellion*`): E phrygian, turning to E major for a win. Rod whooshes follow the
blaze's own sped-up clock (a pass every 250 ms of it). Clang pitch capped at an octave; the clang only adds 10% height.
`hellionSlow` (tape down, hum, heartbeats) for bullet time; `hellionPip` climbs E major; `hellionJackpot`;
`hellionSupernova` (boom + A major choir) at ignition; `hellionPillarBlast` and `hellionCountTick` for stage 2.

## Detection - `detect/HellionWatcher`, `detect/HellionDrops`

Written against real tier IV sessions (131 kills replayed).

- **Ownership** is the quest's chat line `SLAYER BOSS! The Inferno Demonlord IV spawned!`: the first T4 nametag within
  16 blocks (24 for a full-health boss) from 2 s before to 5 s after is ours. No "Spawned by" line exists for this boss.
  Until a spawn line has been seen, a running quest claims the first T4 nametag within 12 blocks.
- **Bosses that come with the quest** (auto-slayer): no spawn line; the nametag at full health appears in the same tick
  as "SLAYER QUEST STARTED!". Claimed within `INSTANT_MS` 1.5 s, at full health, within `INSTANT_REACH` 20; QUEST
  STARTED re-runs `claim` over bosses already present.
- **The nametag**: `[Lv825]  Inferno Demonlord IV 150M/150M❤`. It goes and comes back twice (the demon phases at ~2/3
  and ~1/3) as a new entity at or a little below the HP it left at: one first seen within 45 s of ours going, within
  `PHASE_REACH` 14 blocks, between that HP + 2% and - 15% of max, is ours again. An entity id that was ours this quest
  (`ownedIds`) is ours whatever the distance. A followed entity stays followed whatever its name says (immune phases
  hide the health); health is only read off a name that shows it.
- **The trailing ✯ is not ownership** - it is a Shuriken's Magic Find mark any player can put on any mob.
- **The kill**: ours reads `0/150M❤`; ~1.2 s later the drop line, then `SLAYER QUEST COMPLETE!`. Hellion opens on the
  0 with the drop pending (`HellionScreen(null)`; `resolve(d)` rebuilds on the same seed). The first win among the drop
  lines resolves it; QUEST COMPLETE with none (or 3 s) makes it a bust. A missed 0 opens at QUEST COMPLETE with the
  outcome known. QUEST COMPLETE with a win of ours and nothing claimed plays the win (`killKnown`): a bracketed slayer
  drop line is only ever the player's own. `SLAYER QUEST FAILED!` and a cancelled quest forget it.
- **Drop lines**: `VERY RARE DROP! (Scorched Power Crystal) (+236% ✯ Magic Find)` - the item in brackets after the
  tier (RARE / VERY RARE / CRAZY RARE / PRAY TO RNGESUS), a count ("5x ") or symbol ("◆ ") in front. Mini-boss drops
  and Diana's "RARE DROP!" have no brackets and are not read. Any slayer drop not in `Drop` is a bust here.
- Which boss a drop belongs to is `detect/SlayerQuest` (see [slayers.md](slayers.md)).
- `detect/HeadCapture`: any open chest menu showing a wanted head gives up its textures property as a `head_capture`
  record (how the Paragon Shard's head was captured; it is now in `slayer_heads.json`).

Settings: `blazeAnimation`, `blazeHidePct` (pre-kill hide, `HellionWatcher.armed()`), `blazeBanner`, `blazePrice`,
`blazeVolume`, `blazeTier`, `blazeStage`, `blazeClick`, `blazeSpeed`.

## Tests and tools

- `tools/HellionTest`: hashes every draw call before the reveal for every drop on five seeds, identical audio, the dice
  tracks' sixes.
- `tools/HellionDetectTest`: real lines; `-Drecords=` runs a records file through.
- `tools/HellionReplay`: plays a records file's slayer lines and nametags through the real watcher at their times
  (compile against `build/classes/java/main` plus the game classpath).
- `tools/HellionPreview`, `tools/HellionSoundtrack`.
