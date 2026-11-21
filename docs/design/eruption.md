# Eruption - lava fishing (the Crimson Isle's rare sea creatures)

The creature sinks, a crust closes, a vent of magma rises and breathes heat; the heat it lands on is the tell, and
**black is the bust**. Every rare creature's kill plays, loot-shared kills too. Guaranteed drops (Jawbus's Magma Lord
Fragment, the Scuttler's Scorched Crab Stick) are not the reveal: a bust shows them under the cooled vent with their
price. Plhlegblast and Taurus play only for a win of their own. The Carmine Dye gets the grandest ending.
`anim/EruptionTimeline`, `render/EruptionScene`, `gui/EruptionScreen`, `audio/EruptionSound`.

## The timeline

`EruptionTimeline` is pure. `SINK`, `CRUST`, `VENT`, the heat steps black-red-orange-yellow-white (`STEPS`,
`stepLevel`, the same every run), a flicker in the hang, landing on the drop's `Heat` at `REVEAL` 4800. `Drop` holds
heat, ending, creature (null = any: the dye), NEU id, art and odds; heat never falls as value rises.

Endings: PLAIN (Chain), PINCER, FLASH, FORGE (Brimstone), VIAL, SHELL, END_TIMES (Burnt Texts), KUUDRA (Carmine Dye),
BOBBIN, INK, CHARGE. Kuudra stops `phys` from `K_FREEZE` to `K_RESUME`.

**`backOut` returns exactly 0 before it starts.** A float crumb there made every `> 0` test true and drew the Vial in
Jawbus's chest from the first frame.

## Drawing, chosen by cost

- **The lava floor is one CPU image** (`floor`, a texel per world unit, only the visible part) drawn with one affine
  blit. As gilded tiles it was up to 13,000 quads. `floorGild` holds the crust, heat ring, green (Vial), terracotta and
  cracks (End Times), carmine (Kuudra) with the frozen grey ahead of it; fixed shapes are one precomputed `mask`.
  `RECOLOUR` gild maps lava brightness onto the colour. The floor covers the whole view (to +-700/900 units) and fades
  to near-black with distance (`fog`, from 70 units back), so nothing of the game shows over it.
- **Plain cubes go to the GPU** (`Poly.gpu`, `drawGpu`): the vent's magma blocks, chunks, Kuudra's cube (the vanilla
  magma cube rig, inner cube and slice faces dropped), tentacle segments, basalt pillars. Sorted by block, outline
  first. **Quads wound the other way are turned round** - the game's GUI drops them.
- **Everything else is rasterised** (`ModelRaster`): the creatures (vanilla rigs posed by vanilla code, `LavaRigs`),
  the drop (a head, or its icon extruded a pixel deep), the crab, Jawbus. The raster image is drawn over the GPU
  cubes, so keep those behind. Faces turned away are culled (box faces by normal, rig polygons by the game's normal,
  written by `LavaRigs` to q[22..24]); heads are not. **Under the lava**: polygons wholly below y 0 in the lake are
  dropped, upright ones cut at the surface (`clipToSurface`), and `ModelRaster.cutBelow` refuses pixels below it.
- Effects are squares on two PixelLayers: back (embers, bubbles, fountain, bolts, rings, rays) before the 3D layer,
  front (steam, sparks, arcs, book fire, carmine rain) after.
- Measured at a 960x540 GUI, raster x2: mean 2-3 ms CPU, Kuudra's worst ~9 ms; GUI calls mean ~700, worst ~1,900.

## The stage

- Camera yaw 22 with a sway, pitch -24, a push in through the build-up. From the drop's rise the focus eases onto
  `dropRest` with the words in the middle. Kuudra pulls back to 0.68 and stays. `GOLEM_X` / `KUUDRA_X` / `TENTACLES`
  place things by screen x (screen x ~ 0.93 x + 0.37 z at yaw 22).
- **The vent** is a cone: half-sunk blackstone boulders (`SLOT_STONE`), a ring of blackstone and magma, a magma
  shoulder, a crater of 7 units (the game's lava in the heat's colour). Cracks are the heat's colour on a darkened
  block (mixing toward white read as peach). Magma blocks brighten above heat 2.4. Erupted, rock blocks stay as stumps.
- **The crust spreads outward** from the vent (`crustFront` 0 -> 380 over `CRUST_FROM` 1450 .. `CRUST_TO` 2150) with a
  glowing rim; a bust closes it back in (`closing`). The heat ring is `RECOLOUR` gild (#FFF0B0 with an #FF8A20 rim).
- **The sides**: `pillars()` (basalt column clusters, `SLOT_BASALT`, placed by screen x) and `smoke()` (plumes and
  ash, thicker to the reveal, identical every run, cleared by a win). `sideX` keeps 6% of the screen clear;
  `sideLimits` is set at the top of `draw` (set inside `models` it was a frame late and broke honesty on frame one).
- `creatureScale` per creature (Jawbus 1.5, Thunder 1.15, Plhlegblast 1.2, Ragnarok 1.3, Scuttler 1.4); the nametag
  sits over `creatureTop`. `ripples` where it sank. `worthScale` sizes the showcase (Kuudra's dye 1.25).
- A bust's `SLUMP` (R+520): the crater block drops in, `slumpSteam`.
- **Textures** are the game's, through the resource manager: lava and magma as normalised strips (`LavaArt.strip`),
  the golem, elder guardian, squid, skeleton horse and saddle, magma cube. Ragnarok's and the Scuttler's skins and
  every head drop are NEU heads (`assets/sbcs/lava_heads.json`).

## Endings

- **The Vial, "Meltdown"**: `V_PULSES` (six, faster), `critical()` (green screen edges, shake), `V_FREE` the
  detonation (Jawbus gone, debris, `mushroom()` cloud, radiation ring of four rings of squares), the vial blown into
  the cloud and down (`V_DESCEND`).
- **Flash**: a 4-cell bolt, the cone white for 50 ms at `F_BOLT`, `arcs` round the book (`outline`).
- **End Times** (Burnt Texts): `crackSegments` - straight segments with kinks and two branches per ray; terracotta at
  30 + 75*terra.
- **Kuudra** (Carmine Dye): the world freezes grey, carmine floods (capped at 105 units), Kuudra's cube and tentacles
  rise, the dye rests at 56.
- **Bobbin' Scriptures** (`BOBBIN`, from Jawbus, Ragnarok and Plhlegblast - `Drop.also`): a pixel Silver Magmafish
  leaps over it twice.
- **Squid Boots** (`INK`, Plhlegblast): ink squirts and an ink stain over the lava.
- **Taurus Helmet** (`CHARGE`): Taurus lunges at the camera twice, grit rings and nostril steam.
- **Jawbus's entrance** (`EruptionTimeline.jawbusIntro`, `lavaJawbusIntro`): he heaves up with his arms up and roars
  before sinking - the same for every Jawbus. `LavaRigs.golem`: arms past horizontal, then spread. A dry Jawbus shows
  "your 14th Lord Jawbus · 0 Vials" (`EruptionScene.bustNote`).
- **Taurus** is drawn as the game builds it, roughly: `LavaRigs.pigRider` - vanilla PigModel + saddle layer, a
  zombified piglin in the passenger pose with its head **left out by path** (`ModelPart.visit` walks invisible parts,
  so `visible = false` does nothing) and the Taurus Helmet skin (`SLOT_HELMET` 11) in its place.

## Sound

`EruptionSound` (`Voices.eruption*`): D phrygian, D major for a win. Rumble, sink, crust creaking, the vent, a bubble
per breath pitched by its heat, build, flicker; cut at the reveal. Then the boom (`eruptionBoom` takes an amplitude -
with the End Times bolt it clipped), quench, per-ending cues, arrive and glow.

## Detection - `detect/LavaWatcher`, `detect/LavaDrops`

- Drops: `RARE DROP! Enchanted Book (Flash I) (+157 ✯ Magic Find)` - no brackets round the item. The Carmine Dye's line
  is unknown: any drop or WOW! line naming it.
- **Ours only from a catch line**: the nametag first seen within 20 blocks, 2 s before to 6 s after its catch line -
  Hypixel's own or **Feesh's rewrite** ("A Thunder has spawned!"; Feesh hides the original, which is why our chat
  listeners run in an early phase). Taking creatures that armed the hide, or that a loot share or charm named, opened
  busts for party members' creatures, so nothing else makes a creature ours.
- **Someone else's creature plays only when its own win comes to us**: its death, a loot share line within 2 s, and a
  rare drop that creature can give (either order). A charm on ours is our kill; on anyone else's, their death.
- **Kills go to the last hitter.** Loot share names the player who killed it, even for our own catch.
- Dead: HP reads 0, or the nametag goes at 5% or less, close, no teleport. Also ours going short of 5% with a loot
  share for its kind within 2 s either side (a party member landed the last hit), or right after a win of its own.
  Pending until a win within 3 s, else a bust; a win up to 1.5 s before the death is the outcome already.
- **Jawbus floats up immune**: the same entity, but its nametag shows no health for ~4 s. A followed entity stays
  followed whatever its name says, and health is only read off a name that shows it.
- `Creature.onKill`: Taurus and Plhlegblast **never open on a kill** and never arm the hide (`LavaWatcher.dropOnly`: a
  win from one of ours that died within `DROP_ONLY_MS` 10 s, or a death within `EARLY_MS` after the line). The Taurus
  Helmet needs no death. Taurus's catch line is "Taurus and his steed emerge." (`ofCatch` reads "emerge").
- The pre-kill hide: ours only, at `lavaHidePct`.

Not done: the Lord Jawbus Shard (no NEU item or known line), the Enchanted Ink Sac (cheap).

## Tests and tools

- `tools/EruptionTest` (needs the client classpath): frame honesty per creature on four seeds (the raster and floor
  images hashed pixel by pixel), sound honesty, heat order, Kuudra's clock, every ending's beats, the draw-call budget.
- `tools/LavaDetectTest`: real lines and a whole session replayed through the watcher.
- `tools/EruptionPreview`, `tools/EruptionSoundtrack`.
- Menu "Lava Fishing - Eruption" (`lava*` config); `/sbcs test lava <drop> [creature]`.
