# Diana - The Chopping Block

Diana's Mythological Ritual. A creature's head drops onto a chopping block, three chops, and the split reveals the
drop. Played over the **dimmed** game, not a black screen. `anim/ChopTimeline`, `render/ChopScene`,
`gui/ChopScreen`, `render/art/ChopArt`. `docs/diana.md` is the original spec (for an earlier design, "The Thread");
where it conflicts with this file, this file is current.

## What plays

- **Every kill** of a Sphinx, Minos Inquisitor, Manticore or King Minos opens it, outcome pending (nearly always a
  bust).
- A **Minos Champion only when the Relic drops** - not on its kills.
- The **Mythological Dye from any creature**.
- These never start it, whatever they drop: Minos Hunter, Siamese Lynxes, Stranded Nymph, Cretan Bull, Harpy, Gaia
  Construct, Minotaur. Only rarer creatures and drops play.
- Drop-triggered cases open on the drop with the outcome known; they still play the same sway.
- A loot-shared drop plays like any other.
- **Never infer an outcome by elimination** (unlike the Scatha's gemstones). Unresolved at `RESOLVE_BY` (SPLIT - 250)
  is a bust.

## The timeline

`anim/ChopTimeline` is pure, per verdict, **creature** and seed:

- The head drops onto the block (`LAND` 560), three chops (`STRIKES` 1250/1850/2400, each deeper - `bite`
  {1.8, 2.8, 3.8, 8.4}), the last wind-up trembles while the cracks cycle colours (`CYCLE` 2700: a fixed sequence
  moonstone, wisteria, thulian, viridian, carmine, moonstone, wisteria, thulian), the split at `SPLIT` 3300 - the
  first moment anything may differ.
- Bust ~4.4 s: halves fall open and crumble. Normal win: light pours up, the drop rises (`ARRIVE`), the block and
  halves sink (`blockFall`). UNKNOWN 5.2 s, neutral.
- **The creature is not the verdict.** `Creature.of(verdict, seed)` gives the drop's own creature (Relic - Champion,
  Brain Food - Sphinx, Chimera - Inquisitor, Stinger / Manti-core - Manticore, Crown / Wool - King Minos); a bust or the
  Dye picks a kill creature by seed. In game the head is the kill's, which the player already knows.
- **The tell is honest**: no lying on a share of losses. Tension comes from the fixed, outcome-identical sway and the
  verdict blackout.
- **Colours** (one per outcome, deliberately uncommon shades): sway ember #7A3410 -> amber #F2A12A -> Diana red
  #E0301E -> black at the verdict, identical on every outcome. Then: bust cold ash #3C4250, Minos Relic moonstone
  #BFC8DD, Brain Food wisteria #9F7BEA, Chimera thulian pink #DE6FA1, Stinger / Manti-core viridian #3A8C6E, Crown /
  Wool carmine #C8003C, Mythological Dye its in-game olive #6F6F0C. No gold.

## The scene

- **Heads are real skins** from the NEU repo's `constants/bestiary.json` (keyed by display name, field `texture`),
  bundled in `assets/sbcs/creature_heads.json` and loaded by `HeadSkins.CREATURES`.
- The head is `HEAD` 4 half-extent on the block top (y 0), base + hat. After the split two half-boxes pivot on their
  outer bottom edges; the cut face is `ChopArt.FLESH` (meat #861C1E, fibres #B0302E / #5C1012, rim #E2BBA8) or
  `BRAIN_CUT` for Brain Food.
- **The cleaver** is `CleaverMesh` with `PropArt.cleaver()`, scale 0.42. **Its blade lies in the plane of the cut**
  (x = `strikeX`), edge front to back along the slot, handle behind, swung about its pommel (`cleaverVec`); `cleaver(t)`
  gives {x, edge y, angle, alpha}, angle 0 level in the head, -1.35 raised. A face-on blade sent chips flying at the
  viewer. While level and in, each face is clipped to the part above the head's top (`clipAbove`). `smear` draws the
  blade 28 and 56 ms back over the last 60 ms of each swing. Camera yaw 44 -> 36 through the chops.
- Chips burst off both faces of the blade (along X), drawn in two passes. Splinters are three grid cells turning.
- The board is thin (`BLOCK_H` 4). No letterbox bars on Diana's endings.
- The 3D scene is never faded in: `blockPop` 0.85 -> 1 over 120 ms with `ChopFill.dust`. A 2 px downward kick per blow
  in `shake`.
- **Filling the screen** (`render/ChopFill`, seeded, on the scene clock so honesty holds): `debris` (splinters and
  sparks toward the edges, `DEBRIS` {6, 12, 18, 40}), `rays` (a win's light: eight wedges from the drop, stepped
  every ~1.5 cells, ending 6 / 4 cells wide, coming in at the showcase `arrive() - 150`, starting where
  `ChopScene.showItem` last drew the item), `motes` (squares rising, drawn before the drop and its words), `ash` (a
  bust's). No rays or motes for the Relic.
- **Lights** (`ChopScene.light`, set per frame by `ChopSpecials.lights`) tint faces facing them toward their colour.

## Showcase

- Drops are drawn as their real items (`DianaItems`: NEU skins for heads, vanilla icons for the book and helmet, a 3D
  yellow wool block), outlined, with the price counting up (bazaar instant sell, else lowest BIN, else NPC -
  `ChopScreen.fixPrice`), the name letter by letter, text capped at 1.35x.
- Every showcase goes through `ChopSpecials.fit`: the drop at most 0.26 of the screen high and lifted so the words
  clear the bottom. `ChopTimeline.showScale` sizes by worth (the Dye 1.3).
- **Banner** (`ChopTimeline.banner`): "RARE DROP!" in #FFAA00, the Dye's "WOW!" in #FF55FF - Hypixel's own words.
- **Score** (`Voices.score`): an arpeggio in each ending's own key and mode, to the reveal.
- One merged line under it: "odds · your Nth" (`ChopTimeline.merged`, filled by `ChopScreen.settle`). A bust shows
  `ChopScreen.bustLine`.
- **Cheap drops are short** (`ChopTimeline.cheap()`: Brain Food, Crown, Stinger); expensive ones (Wool, Manti-core,
  Relic, Chimera, Dye) get the long endings.

## The endings

Each is its own class under `anim/` (timing) drawn by `render/ChopSpecials` with ChopScene's machinery.

- **The Relic** (Minos Champion): the split head holds together and throbs three times (`RELIC_BEATS`), comes apart
  into 4x4x4 chunks of its own skin, outside first (`chunkLaunch`, `chunk`; each chunk the skin colour where it sat),
  which spiral up a vortex to `MOON` and fuse at `RELIC_CONVERGE`; the block falls away and the camera climbs. The moon
  head with orbiting stars spins up flickering moon / Relic faces (`relicFaces`) and stops three-quarters on
  (`RELIC_REST_TURN`).
- **Brain Food** (~9.1 s): pixel veins (`veins()`, `{x0,y0,z0,x1,y1,z1, depth, parent}`, 60+ per seed over the head's
  top, front and across the block, painted as pixels on the faces - `ChopScene.pixelVeins`, half-texel grid), one
  flood, sparks running home along the parents, the head bursts, Brain Food rises in the pi ring. Head-local points
  turn with their half (`ChopScene.veinWorld`).
- **Chimera I, "The Glint"** (`GlintEnding`): the game's enchantment glint over every face. The glint texture is
  additive colour; the GUI only blends, so `ChopArt.glint` turns its brightness into alpha - read from the game's
  resources at runtime, never bundled - and `ChopScene.glintHook` scrolls it. The glyphs are the game's own
  `particle/sga_*`. A violet vignette (25% at 1.22x). The vanilla enchanted book icon, named in #FF55FF.
- **Stinger** (`StingEnding`): the barb rises out of the split, hangs with a pixel bead, stabs the board (`IMPACT`):
  splash, a crack and a stain in the wood (`stingDecal`); turns into the drop.
- **Crown** (`MidasEnding`): gold spreads by gild (60 ms/texel), a shine, the halves burst into coins and the 3D golden
  helmet pops out; the vanilla icon at the reveal.
- **Manti-core** (`HeartEnding`): 26 cells fly in and fuse (`cell`); each wing is three jointed panels (`bend`,
  `ChopSpecials.wingPanels`, texture split at `JOINTS`); the cocoon is the wings swung across in front (phi 2.25 - curled
  further they go edge-on); cracks of light (`wingCracks`); a lion's roar at the burst.
- **Wool** (`FleeceEnding`): the ram constellation (`STARS`, `LINES`, a line at a time, square to the camera) falls into
  a turning spindle; light shafts on the wool. A pluck per line.
- **Mythological Dye, "The Whole Hunt"** (`HuntEnding`): the split goes wrong (halves shut on an olive cut, board
  cracks - `huntCracks`, cleaver wrenched out), `HUSH` (half a second of nothing), `GEYSER` (a column of pixel squares,
  rain staining board and halves - `stained`, `dripped`), the parade (`BESTIARY`: all twelve creatures' heads via
  `Canvas.bestiarySkin`, tumbling into a ring, `slot` a permutation, King Minos last and biggest nearest the camera),
  each painted olive bottom-up by gild at full relief, `SPIN`, the moon rises, `ARROW` threads every head in `hitOrder`,
  `SUCK`, `SHOCK` (the board blown away), the dye at `ITEM_Y`. Heads behind the block draw in `beforeBlock`. Its
  soundtrack is a cue list, `audio/HuntSound` (G dorian).

Rules that came out of the endings:

- The overlay goes texel by texel through the faces (gild), never as a screen wash.
- Liquids and effects are pixel squares; smooth spheres read as baubles.
- Never a full-screen white or grey fill.

## Sounds

`Voices.chop*`: the head's thud, a swish before each blow, each chop a wet crunch + wood thock + ringing blade a whole
tone up and **louder each time** (-9.6, -9.0, -7.4 dB), a riser and cycle ticks over the wind-up, `silence()` and the
split (-2.2 dB). Bust: an empty breath and a crumble. Win: swell, bells and fanfare on the Diana win volume.

## Detection - `detect/DianaWatcher`

**No chat line marks a kill.** The kill is read from the creature's nametag armour stand (SBO's method): HP is the
number before "/" or the heart; dead the tick it reads 0. Fallback: the nametag going at <= 5% HP within 32 blocks with
no teleport. Nametags look like `[Lv1250]  Empyrean Sphinx 65M/65M❤` and `... 0/65M❤` at death.

A creature is ours if:

- **we dug it**: the dig line ("Oi! You dug out a Minos Inquisitor!") claims the nametag of that creature first seen
  within 20 blocks, from 3 s before the line to 15 s after;
- **it armed the pre-kill hide** (`Mob.armedAt`, within `ARM_MS`) - a party member's creature killed nearby may give no
  loot share line;
- **it was charmed**: "CHARM! You charmed the X and received" is read like a loot share naming X;
- **loot share**: "LOOT SHARE You received ... for assisting" within 2 s of a death either side. The newer wording
  names the creature ("2 King Minos Shards"), the older does not. A death is kept 2 s in `unclaimedDeaths`; a RARE
  DROP line in that gap opens the chop itself before the line reaches chat.

Drops (`dropOf`, pure): "RARE DROP!" plus a drop name (Chimera by "chimera"), or "WOW! ... found a Mythological Dye"
naming the player. Only game lines, never player chat. Most lines arrive stripped, but some keep `§` codes in the text,
so `plain()` strips them first.

A kill opens `ChopScreen` with a **pending** verdict (null); the drop line calls `resolve(v)`, which rebuilds the
timeline with the same seed and creature - invisible, since nothing before the split depends on the verdict. A drop
with no kill pending opens a known-outcome chop. The RARE DROP line can arrive before the nametag reads 0; then the
drop path opens first and the kill is skipped as already playing.

**The pre-kill hide** (SkyMaxx's rule): a creature that is ours, **or any while we have had loot share in the last
5 min**, seen >= 6 ticks, within 16 blocks, HP at or below `dianaHidePct` with a max above it, arms a rolling 2.5 s
hide. `DianaWatcher.arm` decides once a tick; `armed()` reads a volatile time (the sound mixin and every HUD element
ask). It feeds `shouldHidePreKill`: HUD, chat shut and held, game muted, hand hidden.

### What SBO matches (reference)

`§` codes as SBO matches them:

- Dug creature: `^(.*?) §eYou dug (.*?)§2(.*?)§e!(.*?)$` - group 3 the creature. Treasure:
  `^§6§lRARE DROP! §eYou dug out a (.*?)§e!$`, coins `^§6§lWow! §eYou dug out §6(.*?) coins§e!$`, burrows
  `^§eYou (.*?) Griffin [Bb]urrow(.*?)$`.
- A drop: `^§6§lRARE DROP! (.*?)$`, told apart by `contains` on the name; Magic Find rides along as
  `(+N ✯ Magic Find)`.
- The Dye: `^§d§lWOW! (.*?) §6found a §2Mythological Dye.*$` - group 1 is a player name; check it is ours.
- Loot share: `^§e§lLOOT SHARE §fYou received loot for assisting (.*?)$`.
- Kill: nametags containing the mythological glyph (`§2` + U+E07E), death when the parsed HP
  (`([0-9]+(?:\.[0-9]+)?[MK]?)§f/`) reaches 0, once per entity id.
- Level prefixes: Empyrean, Exalted, Runic, Venerable, Stalwart, Blessed. King Minos has a hit-shield phase
  (`.*?(\d+)\s+Hits.*`).

## Settings and stats

Menu Diana section: creature switches (checked in `SbcsClient.openChop` for real runs only - tests always play), each
drop's own ending (off: `ChopTimeline.options(simple, ...)` gives the plain ending; ChopTest checks pre-split frames are
unchanged), banner, price, size (`ChopTimeline.scale`, `dianaSize` default 130), music, win volume, speed.
`SbcsStats.onDiana(creature, drop)` counts a real run before its screen is made, so the screen reads "Your Nth X".
Diana kills with no screen (`unopenedDiana`) are claimed by their drop line, or counted as busts after
`DIANA_DROP_WAIT_MS`.

## Tests and tools

- `tools/ChopTest`: honesty per creature (hashes every draw call before the split for all verdicts, per creature, five
  seeds), the cycle's steps, a bust under 4.5 s, every ending fades out, the cleaver never jumps and is level on each
  blow, Relic chunks still until the peel and home at the moon, veins joined / ordered / dense, the Dye's beats.
- `tools/ChopPreview`: stills (`-Dat=`) or every frame; `-Dskins=`, `-Dvanilla=`, `-Dbg=`.
- `tools/HuntSoundtrack` renders the Dye's soundtrack.
