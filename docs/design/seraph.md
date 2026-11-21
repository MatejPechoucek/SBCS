# The Seraph's Glyph - the Voidgloom Seraph (enderman slayer)

Tiers III and IV. The Seraph holds a beacon (the Yang Glyph) and throws it; the beam that lands is the tell, and **a
black beam is the bust**. End Crystals stand at the sides. Same architecture and rules as Hellion: honest tell, a 500k
floor. `anim/SeraphTimeline`, `render/SeraphScene`, `render/SeraphEndings`, `gui/SeraphScreen`,
`audio/SeraphSound`.

## Facts

- The fight: 210M HP (tier IV); the hitshield is 100 hits at spawn, 140M and 70M. Tier III is 50M.
- The drops and odds are in `SeraphTimeline.Drop`. Tier III shows its own odds (`SeraphScene.oddsFor`).
- Four small drops (Chalice, Espresso, Sinful Dice, Void Conqueror skin - NEU `PET_SKIN_ENDERMAN_SLAYER`) have endings
  but sit under the 500k floor and only play if their price rises.
- Heads are bundled in `slayer_heads.json` from the NEU repo.

## Vanilla, read with javap

- **EndermanModel**: head 8-cube at pivot (0,-13,0); the "hat" is the jaw, an 8-cube shrunk 0.5 at tex (0,16). Body
  8x12x4 at (0,-14,0), tex (32,16). Arms 2x30x2 at (±5,-12,0), legs at (±2,-5,0), tex (56,0), left ones mirrored.
  Texture 64x32. **Creepy**: the head lifts 5 px, the jaw stays.
- **Carrying**: both arms xRot -0.5, zRot ±0.05, and they stay there through the throw (vanilla endermen never lift
  their arms). The arms push 0.3 forward at the release. **CarriedBlockLayer**: translate (0, 0.6875, -0.75), rotX 20,
  rotY 45, translate (0.25, 0.1875, 0.25), scale (-0.5, -0.5, 0.5), rotY 90 - a half-size block in front at hand height.
- **Beacon**: `models/block/beacon.json` - glass shell, obsidian base, core.
- **EndCrystalModel**: outer glass, inner at 0.875, cube at 0.766, each turned 60 degrees about (1,0,1) and spinning
  age*3 degrees, on a 12x4x12 base. The texture is a 128x64 sheet for a 64x32 layout (`scale2`).
- All textures adopted at runtime (`SeraphScene.ADOPT`); none animated. `TEX_WHITE` is generated.

## The scene

- **Model to world**: world = (x, FEET + 24 - y, -z), a half turn about X. Rotations conjugate by diag(1,-1,-1): keep
  the X angle, negate Y and Z (`toWorld`).
- `rig()` builds the parts; `drawRig` sorts every part and the carried beacon together, ink first.
- **The camera** looks from the -X side of +Z (yaw 20), so a head facing it is turned `ry(-32)`.
- **Beams** are camera-facing ribbons tiled and scrolling (`beam()`; 16 px per 0.4 block). Crystal beams are two
  `TEX_WHITE` ribbons under the speckled texture (the vanilla beam texture is 80% transparent by design).
- **The tell** is a solid stub of beam plus a stained wash (the wash alone was lost under the beacon's cyan core). The
  black beam has a `BLACK_RIM` #9A5CF0 edge, the same for every drop before the reveal.
- Crystals 0.75x (base 9x3x9); `pillarX` from the screen's width with a 6% margin. `horizon`: six stepped bands of
  #3A2160 up from the ground.
- **The showcase is pinned to the centre**, every ending: from `arrive() - 900` the camera eases its focus onto
  `showTarget()`, putting banner, drop, name, odds and price in the middle. The sway turns about the focus.

## Endings (`SeraphTimeline.Ending`)

- **Exceedingly Rare Ender Artifact Upgrade**: the jackpot.
- **Judgement Core, "The Eye Opens"** (`EYE`, `E_*`): the beam lands purple and the stopped clock (`phys`) freezes
  everything but the core. The shell cracks and bursts; the Core's skin grows out of the beacon. Lids and pupil are
  texel overlays on its front face: open in three steps, `eyeLook` -1 / +1, the stare and pulse ring; `pullInto` spirals
  the Seraph and crystals into `pupil()`. Zoom 1.3x.
- **Byzantium Dye, "The Dragon's Tribute"** (`DRAGON`, `D_*`): the beam deepens to Byzantium, time stops and the world
  greys texel by texel (`worldGild`), the Ender Dragon flies a path, lands behind the beacon, lowers its head and
  breathes; the breath pools into paint (`pool()` on its own `GROUND` PixelLayer under everything) and the gild paints
  pillars and crystals. The beacon bursts, the dye rises, the dragon hops behind it with one wingbeat and rears.
- **Summoning Eye** `ALTAR`: eight end portal frames rise round the beacon, eyes fly in, the portal opens.
- **Etherwarp Merger** `WARP`: four aimed teleports.
- **Enchant Rune** `RUNE`: an enchanting table, sga glyphs stream in and form the rune.
- **Paragon Shard** `SHARD`: the beacon core cracks, crystal shards spiral in and fuse.
- **End Stone Idol** `IDOL`, "Set in Stone": the Seraph turns to end stone by gild, cracks and shatters.

Every dye gets its own, grandest ending - endings are never shared between a dye and another drop.

### The dragon

`render/DragonRig` is the game's own `EnderDragonModel`, built from its layer, with an `EnderDragonRenderState` filled
per frame (flap clock, the 64-tick flight history its neck and tail bend from, sitting), its own `setupAnim`,
EnderDragonRenderer's root transform, then `ModelPart.visit` for every posed polygon with its UVs. The preview runs
the same class with the client jar on its classpath.

- In the rig **positive `extraPitch` is nose up**; positive head `xRot` is down.
- Sitting puts the head at 0.9 blocks, hidden by the beacon; it stands (head at 2.2) and lowers the head 0.3 rad.
- Head-on it reads as a column and the wings go edge-on: it lands and rears at yaw 225.
- **It is rasterised** (`render/DragonRaster`) - a painter's sort of ~375 polygons drew far parts over near ones. Back
  faces culled except the wings (`q[21]`, single sheets). Textures through `readGameTexture`, so packs apply.
- The breath starts 12% past the jaws (the pixel layer is drawn over the scene).
- **The flight is Blender's** (`assets/sbcs/seraph_dragon.json`, `anim/SeraphDragon`): a Bezier path with auto handles,
  an empty on Follow Path, offset keyed 0 / .34 @22 / .86 @70 / 1 @92, sampled per tick, Blender (x, -z, y) = ours. The
  control points are in the JSON's note; re-bake there rather than editing frames. Heading from the path's direction,
  settling to `LAND_YAW`. The circle must stay within ~8 blocks of the beacon and ~4.5 high. `SeraphDragon.pos` scales
  x < 0 by `WEST_PULL` 0.6 in code (a re-bake should fold it into the control points). The rear: `REAR_BACK` 9,
  `REAR_LIFT` 4.6, `REAR_ZOOM` 0.24, `REAR_RISE` 6.

### Draw calls

A normal Seraph frame is ~1,300 calls. The Dragon once reached 80,000 (two quads per gilded texel, ~375 polygons x 9
quads); gild runs and the raster brought it to mean ~2,500. `tools/FrameBudgetTest` guards it.

## Sound

`SeraphSound` (`Voices.seraph*`): C# minor, the vwoop, a clink per hit pitched by colour (`SERAPH_GLASS`), cut at the
reveal. The Eye: freeze, crack, shatter, hum, lids, glances, heartbeat boom, implode. The Dragon: freeze, a growing
roar, a wingbeat on every downstroke of its real flap clock (`SeraphScene.flapAt`), landing, breath, glugs, burst,
rearing roar. The burst's shatter is 0.22 s after the splash (together they clipped). `onSkip` plays a stutter.

## Detection - `detect/SeraphWatcher`, `detect/SeraphDrops`

- **Ownership**: the boss's own "Spawned by: <player>" nametag, an armour stand two entity ids after the boss's, seen
  the same tick (the player's name is `player.getGameProfile().name()`), or the spawn line
  `SLAYER BOSS! The Voidgloom Seraph III spawned!` (16 blocks, -2 s .. +5 s).
- `tierOf` takes III and IV ("II" is inside "III"); the max HP comes from the nametag.
- **The nametag**: `☠ Voidgloom Seraph IV 208.6M❤ ✯` (a marker either side), `[Lv534]  Voidgloom Seraph III 0/50M❤`
  (current / max), `... 100 Hits` through the hitshield (no HP - the last is kept), `0❤` dead. One entity the whole
  fight.
- **The kill**: 0❤, the drop line ~1.2 s later (Hellion's bracketed format, `HellionDrops.item`), QUEST COMPLETE soon
  after. Opens pending on the 0, resolved by the first win, a bust at QUEST COMPLETE or after 3 s. The nametag going at
  5% or less, close, also counts. QUEST COMPLETE with no kill seen opens it known.
- **The 500k floor** for the four small endings (`SeraphDrops.floored`): prices are asked for when our boss appears;
  one worth less, or of unknown worth, plays as a bust (`SbcsClient.seraphFloor`, which also applies
  `belowTier("seraph", ...)`).
- `seraphHidePct` against 210M (the tier IV nametag may show no max).
- `detect/SlayerRecorder` records `slayer_mob`: armour stands within 40 blocks naming a slayer boss or "Spawned by",
  followed through name changes (4/s, with `dy`).
- `Maintenance.SERAPH` (pure) is a kill switch for the whole Seraph path (`SbcsClient.seraphOn()`); it is false.

## Tests and tools

- `tools/SeraphTest`: frame and audio honesty for every drop on five seeds, the cycle, the throw landing at the
  reveal, every ending's order, the eye's looks and lids, the dragon's track (landing spot and heading), paint and
  grey. Keeps DragonRig out (the scene skips the dragon when `SeraphScene.dragon` is null), so it needs only slf4j.
- `tools/SeraphDetectTest`: `-Drecords= -Dself=` replays a records file through the real watcher (compile against
  `build/classes/java/main`).
- `tools/SeraphPreview` (`-Dvanilla`, `-Dskins`), `tools/SeraphSoundtrack`.
