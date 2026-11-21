# The Thaw (Vanguard corpses)

A frozen Vanguard corpse is struck open with a pickaxe; the last strike's flare tells the best drop. Vanguard only -
Lapis, Umber and Tungsten corpses are parsed for reference and never animated. `gui/ThawScreen`,
`anim/ThawTimeline`.

## Detection - `detect/vanguard/`

Hypixel prints the whole haul in one chat message before anything is drawn, so none of the Scatha's claim-window
machinery applies. The header regex matches any `<TYPE> CORPSE LOOT!`; non-Vanguard messages are copied into
`DecisionLog` verbatim. Every parser bug so far was found by sampling real messages, none by reasoning - sample a new
source cheaply before trusting a parser for it.

```
▬▬▬ x64                      opens with a rule too - ignored, we start on the header
  VANGUARD CORPSE LOOT! (2)  a chat-dedup mod may append " (2)" to ANY line - stripped first
  +1 bonus drop!             preamble: everything above REWARDS is dropped
                             blank padding - skipped, never fatal
  REWARDS                    a heading: letters, none lower case - skipped
    Skeleton Key
     ☠ Flawless Onyx Gemstone x2   symbol prefix - VanguardLoot.key strips it
▬▬▬ x64                      closes it (as do a 20-line cap and a 40-tick timeout)
```

Skyblocker's `Corpse Profit` line never reaches our hook (it writes straight into the chat component), so it cannot
break the parse and cannot be hidden. The corpse loot message itself is hidden at `ALLOW_GAME`; a summary replaces it.

**The table** (`VanguardLoot`) is SkyOcean's weights cross-checked against the wiki (sums to 3516; 160/3516 is the
wiki's 4.55%), names and colours from the NEU item repo. `tier` (drop weight, drives the animation) and `colour`
(SkyBlock rarity, display only) are independent - a Flawless Gemstone is dark purple and the commonest drop. `Tier`
order is load-bearing: `CorpseHaul` sorts on `ordinal()`. `SPECIAL` is the Frostbitten Dye alone, pink. Glacite
Powder has weight 0 (every corpse gives some, it is never rolled) and sorts first.

## The timeline

`anim/ThawTimeline` is pure and computed once from the haul. Five endings - CRASH, REVEAL, EXPLODE from the best tier,
plus LOCKET (a legendary headline that is the Shattered Locket) and SPECIAL (the Dye) - and **one pace for every run**
until the last strike lands, at the same moment whatever the drop. Losses once played faster, which told the player
the outcome from the first strike. Loss ~10.4 s, legendary ~13.3 s, Locket ~18 s, Dye ~20 s.

**The flare never lies.** Every flare is its drop's true colour. The tension is the **last** strike, which cycles
blue -> violet -> gold three times from an opening blue and back to blue (`CYCLE`), steps shortening from 380 ms to
145 ms, then lands on the truth. The cycle is identical for every drop; ending on blue makes a legendary's landing the
biggest jump. No white anywhere. `stepStart` and `climbStep` must agree exactly. Under it: one riser
(`Voices.thawBuildUp`, cut at the landing), a tick per colour a semitone up, then the true colour's note.

**The flare's size is settled**: the gem glow at 1.35/0.8 screen heights with 18% in the ice. A larger, more saturated
version was rejected; `ThawArt.aura()` remains only for the special endings.

**The special endings** run a second clock, `ThawScreen.vt()`: from the last impact it runs at quarter speed for
`SLOW_MOTION`; the Locket's then resumes, the Dye's **freezes** until the paint bursts. Only physical things (chips,
pick, jolt, shake) use it. Both stamp the name letter by letter, then "1 in ~N corpses" and "Your Nth X"
(`SbcsStats.corpseItems`), under 7% letterbox bars. The Dye's grey is drawn over the tray and the pink crack after it,
so it is the only colour on screen. The Locket's price is Coflnet's lowest BIN (`SHATTERED_PENDANT`), requested when
the loot message is read, never waited for (NPC 5M fallback).

**The ice's fate** (`ThawScreen.Fate`, after the landing only): a lesser drop's ice gets, by seed, a small hole, drips
or a crack; the Locket's shatters hard at `LOCKET_BREAK` with a gold tint before it.

`vanguardQuick` jumps a CRASH corpse from the intro to `finaleStart - 1500`, events fired muted.

## Art and geometry

`render/art/ThawArt`, `render/model/ThawMeshes`, tuned with `tools/ThawPreview`.

- **The pick's striking prong is on +Y.** On -Y the tip still lands (the pivot is solved from it) but the head
  arrives back-first.
- **A player head's front faces the camera at yaw 0.** Rest items at yaw 32 for a three-quarter view.
- **The ice is a chunk cut by planes** (`ThawMeshes.PLANES`): six faces plus five chips. A facet is a pentagon or
  triangle but only parallelograms can be drawn, so each is its bounding parallelogram with the outline cut into the
  texture's alpha (`ThawArt.cut`), decided per edge (`IceFacet.across`): every edge is kept a pixel beyond its line so
  neighbours meet, except a far facet's edge on the outline, trimmed a pixel inside. **Far facets are painted
  mirrored** (`ThawArt.flip`), because a face seen from behind is drawn flipped; without it the facets did not join. A
  plane's `d` is a true distance along its direction (not divided by the normal's length again).
- **Ice texture density is 3 px/unit** (sheet 256). Cracks scale by `ThawArt.K` but stay one texel wide.
- **The Vanguard is a posed 3D figure painted as a shadow** (`ThawMeshes.vanguard`, `ThawArt.corpse`): player-shaped
  boxes on the 64x64 skin layout plus pauldrons, fully opaque (see-through reads as a ghost), upward faces lifted a
  little, the visor T glinting faintly. Six `POSES`, picked per corpse and maybe mirrored; the block is only 16 deep,
  so poses reach up and sideways. ThawTest fails if any pose leaves the ice.
- **Draw order is three passes**: far facets, the Vanguard, near facets (`ThawMeshes.iceFacing`). A global sort by
  face centres put pauldrons in front of the frost. For a convex shape with something inside, three passes are exact.
- **The screen freezing** (`render/ThawFrost`, pure): frost ferns walk in from each corner over rime, a ragged band
  along the top and sides (bottom middle clear for the tray). Growth 0.12 through the intro, then a step per strike
  (strikes land at the same moments whatever the haul). At the landing: CRASH melts, REVEAL cracks, the rest shatter
  inward. Drawn through a `PixelLayer`.

## Items

`render/VanguardItems`: 21 drops are player heads drawn as 3D heads with their SkyBlock skins (NEU blobs in
`assets/sbcs/vanguard_heads.json`); 5 are vanilla items through `g.item`. A skin lookup returns Steve until it
downloads; `path.startsWith("skins/")` tells a real skin apart, and an unready one draws as a tinted crystal.

**Glacite Powder** is a flat sprite (`ThawArt.glacitePowder`) in the style of the pre-1.14 light blue dye. Mojang's
own sprite is deliberately not bundled; a player can put one in `config/sbcs/textures/glacite_powder.png` and
`ArtOverrides` prefers it.

## Sound

The three flares sit low - G2, C3, E3, a C major triad inside the A minor pentatonic everything else rings on - and
differ by overtones, not pitch. Each strike's steel ring is two octaves above the flare before it, so the swell
resolves into the hit. Strikes are long: crunch, heavy thud, a ringing steel tone with an inharmonic partial at 2.76x,
then loose ice crumbling for a third of a second.

## The profit window

`gui/ProfitScreen`, `market/`. Opens when a Thaw ends, finished or skipped (`ThawScreen.announce` ->
`SbcsClient.onThawFinished`); the next queued corpse waits for it (`profitShowing`), and chat stays held until it
closes. It waits in `pendingProfit` until no screen is open (20 s at most). Every drop priced, most valuable first,
minus the Skeleton Key:

- `VanguardLoot.listing` - each drop's item ID and market: the Dye, Locket and Wisp are auction-only; the crystals and
  Glacite Powder have no market and count as 0; everything else, the key included, is on the bazaar.
- `market/Bazaar` - Hypixel's public `/v2/skyblock/bazaar`, no key, one request. Drops at `quick_status.sellPrice`
  (instant sell), the key at `buyPrice` (instant buy). `market/LivePrice` - Coflnet lowest BIN.
- `market/CorpseValue` - requested when the loot message is read, recomputed every frame so late prices fill in ("..."
  while fetching). Both fetchers are async, cached, never waited on. Gated by `livePrices` and `vanguardProfit`.

## Tests

`tools/VgTest` (parser, including real messages), `tools/ThawTest` (timeline: honesty before the landing, flare and
cycle, poses inside the ice), `tools/ThawPreview`, `tools/ThawGuiPreview`.
