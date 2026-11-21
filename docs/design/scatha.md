# The Scatha (Crystal Hollows) - the cleaver cutscene

The original animation: a cleaver cuts a Scatha apart and reveals whether it dropped a pet or only gemstones.
`gui/ScopeMinigameScreen`; `PatternConfig.animationMode` is still named `SCOPE` for this mode (renaming it would break
existing configs). The cleaver belongs to the Scatha, not to the mod - other sources do not need one.

## Drop detection

There is **no chat message** for a Scatha kill or its drop.

### The kill - `ScathaWormTracker`

Scans nearby `ArmorStand` nametags for `❤` plus `" Scatha"`. The nametag also carries current HP, which drives the
pre-kill hide.

**A vanishing armour stand is not a kill.** Treating it as one fired the cutscene on chunk unloads, walking away,
teleports and relogs. `whyNotDeath()` (after SkyMaxx's `SeaCreatureDetector`) refuses a vanish that came with a player
teleport, a bulk entity unload, an unsettled world, too few tracked ticks, a stale nametag, too much distance, or no
damage ever observed. An entity that is still loaded but no longer matches is not a candidate - only one the client
has dropped. Every refusal is narrated with its reason.

The checks are one-sided on purpose: missing a real kill costs the player the reveal, so each only fires on strong
evidence. The no-damage rule is skipped inside `POINT_BLANK_RADIUS`, because the nametag is sampled at 20 Hz and a
one-shot kill never produces a damaged frame. Elusive mobs are *charmed* rather than killed; the armour stand vanishes
either way, so the tracker treats it the same.

### The drop - `HotbarDropWatcher`

Diffs inventory NBT (`CUSTOM_DATA` -> `"id"`, `"petInfo"`) against the previous tick, every tick.

**The diff must never be gated on the kill window.** A kill is noticed the tick *after* the armour stand goes, while
Hypixel hands over the loot in the same death handling, so the pet routinely lands *before* the kill is detected. If
diffing only ran inside the window, the pet would be absorbed into the baseline and a pet would be reported as a
gemstone. So every tick the inventory is diffed and anything new goes into `cachedDrops`, stamped; a cached drop stays
claimable for `KILL_WINDOW_MS`, and a kill claims whatever is in the cache. `SbcsClient` calls `dropWatcher.tick(...)`
on **every** tick without exception - only the `mayResolve` flag is conditional.

**Gemstones can only be inferred.** With a gemstone sack they never enter the inventory, so the only signal is the
window closing with nothing claimed. (Inference by elimination is allowed here only; Diana never does it.)

### Opening before the outcome

The cutscene **opens the instant the kill is detected**, outcome unknown, and `ScopeMinigameScreen.resolve()` fills it
in during the fade-in (otherwise every gemstone would wait the full window). `openScope` assigns `currentScope`
synchronously: a drop already in the cache resolves later in the *same* tick, and `trigger()` has to find the screen
waiting rather than open a second one.

A run skipped before it resolved is kept in `skippedScope` and its drop taken by `resolveSkipped` (counted and
summarised, not played). `countDrop` counts once, at the reveal or on a skip.

## The worm is player heads

A Crystal Hollows worm is not a custom entity. Each segment is an armour stand wearing a **player head**, and that
head's skin is the worm's texture - an ordinary Mojang skin owned by Hypixel, not in the game's assets or ours.

- The stepped silhouette along the worm's back is **not geometry**. It is the hat layer: a second cube inflated half a
  unit, transparent except where it pokes out. `PlayerHeadMesh.head()` gives base + hat, the whole segment.
- **Do not hand-paint the texture.** `render/ScathaSkins` captures the real one off a live worm: armour stands near a
  tracked Scatha with a head item carrying a `PROFILE` component, and the skin the game already resolved is kept. Head
  and body are told apart by frequency (seen many times per worm = body, once = head). Cached to
  `config/sbcs/skins.json`; a PNG in `config/sbcs/skins/` overrides it. `ScathaArt` is a placeholder for before the
  first capture and nothing more.

## The cutscene

- The cleaver is a real mesh (`CleaverMesh` with `PropArt.cleaver()`); `CLEAVER_FRAMES` (14) is still the swing's
  timeline, each phase computing an index 0..13 that maps to a blade angle. `anim/SwingCurve` is the swing: a Hermite
  spline, judder and spring.
- The cave backdrop was removed; the scene only dims the game.
- **The crystal veins** (`render/CrystalVeins`, pure) fill the empty sides: ~45 crystals in the Hollows' six gem
  colours, each a prism rasterised onto a grid (every cell whose centre is inside, then an outline of the cells round
  it). Each has an arrival 0..1; `ScopeMinigameScreen.drawVeins` grows them a batch per cut (`partsRemoved / chops`).
  At the reveal: SHATTER for a gemstone, FLARE (rarity colour) for a pet, SINK on a missed last swing. Drawn through a
  `PixelLayer` - as individual fills they cost thousands of render states.
- `FIFTH_SWING_PCT` 10: a bust can take four cuts and duck a fifth.
- **The pet**: held `PET_HOLD_MS` 2600 with a card (`petCard`): price from `LivePrice` with the filter id
  `PET_SCATHA?Rarity=LEGENDARY`, count and dry streak snapshotted at resolve; rays in the rarity colour, the name
  stamped 45 ms a letter, the price counted over 900 ms. `anim/PetHeadPopup` is vanilla's totem animation with a
  skinned head; `anim/PetDropFirework` the fireworks behind it.
- The older guillotine mode (`anim/SbcsHudRenderer`, `AnimationManager`) is still selectable from config.

## Tests

Detection cannot be summoned on demand (a pet is ~1 in 2000), so `DecisionLog` narration is how it is verified: read
it with `/sbcs log`. `tools/PetTierTest` covers the pet tiers; `tools/PetTierPreview` / `PetTierSoundtrack` render them.
