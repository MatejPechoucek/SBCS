# 01 - Scatha pet tiers

Matej's idea: "scatha drops". The Cleaver already plays on every Scatha kill and reveals a pet, so the new part is
**making the three pet rarities feel different** - today a Rare and a Legendary pet get the same reveal in a different
colour (`ScopeMinigameScreen.rarityRgb`, `petCard`, `petRays`).

## Trigger

Unchanged: the kill (`ScathaWormTracker`), resolved by `HotbarDropWatcher`. Nothing new to detect.

## Drops (wiki, 2026-10-23)

| Drop | Chance per kill | Bestiary-maxed | Note |
| --- | --- | --- | --- |
| Rare Scatha pet | 0.144% | 0.24% | ~1 in 700 |
| Epic Scatha pet | 0.072% | 0.12% | ~1 in 1,400 |
| Legendary Scatha pet | 0.024% | 0.04% | ~1 in 4,200 - the jackpot |
| Dwarven O's Block Bran | 10% | | already a card line |
| Fine gems, 4x Worm Membrane | always | | the bust |

Two wiki notes worth knowing, both dated October 2026 and unconfirmed:

- **4x Worm Membrane is now guaranteed** (added 2026-10-06). It lands in the inventory on every kill. HotbarDropWatcher
  only matches pets and Dwarven O's, so it should be ignored - but check one `/sbcs log` after a kill to be sure it is
  narrated as ignored rather than miscounted.
- **Several players can drop a pet from one Scatha again** (2026-10-05). Doesn't change our detection, which is the
  player's own inventory.

## Detection

**Proven.** Already reads rarity from `petInfo` in the item's custom data (`HotbarDropWatcher.RARITY_ORDER`).

## Cutscene concept: three endings after the same last swing

The Cleaver's honesty stays: the swing, the veins and the cuts are identical for every outcome. Only after the reveal
do the tiers split, by **how long and how loud** the reveal is:

- **Rare (blue, ~1 in 700):** today's reveal, shortened. Head pops, card, done. It is the commonest pet, so it should
  not hold the screen long.
- **Epic (purple):** the veins FLARE purple and *keep growing* past the screen edge for a beat; the head comes out of
  the split spinning once; the card stamps letter by letter (Diana's `letters`).
- **Legendary (gold, ~1 in 4,200):** its own ending, the grandest the Scatha has. Proposal - **"The Hollows Answer"**:
  after the last cut the crystals don't flare, they *crack*, one per Hollows colour round the screen (the CrystalVeins
  already have the six colours), each crack pouring its colour into the split; the worm's body segments (real captured
  skin, `ScathaSkins`) rise out of frame in a coil and close round the head like a ring; gold floods the ring and the
  pet head comes out of it with "PET DROP!" banner, rays, a score (`Voices.score` in its own key), and the count
  "Your Nth legendary". Score + bullet-time hold like Hellion's High Class.

## Open questions for Matej

- Is the Rare pet worth a shorter reveal, or should all three keep the current length and only add on top?
- Legendary ending: the worm coil, or something else (e.g. the Hollows' Heart of the Mountain, a gemstone storm)?
- Should Dwarven O's get its own tiny moment, or stay a line on the card?
