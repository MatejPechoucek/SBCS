# 05 - Farming

Matej's idea, with "not sure exactly how these would work". Farming has **three** kinds of RNG, and they need very
different treatment because of how often they happen. Farming is hours of the same motion, thousands of blocks a
minute, so anything common must be a Fin-sized popup that never takes the screen, and only the true jackpots get a
cutscene.

## 1. Pest drops (the best fit)

Pests spawn in Garden plots while farming and are killed with a Vacuum. Each kill is a roll. From the wiki / Elite's
guide (chances per kill; they vary by pest level and source - check before building):

| Pest | Notable drop | Chance |
| --- | --- | --- |
| Rat | **Rat pet (Legendary)** | 0.2% |
| Slug | **Slug pet** Epic / Legendary | 0.5% / 0.1% |
| Mite | **Atmospheric Filter** | 0.25% |
| Beetle | Pesterminator I | 3.5% |
| Locust | Sunder VI | 0.5% |
| Cricket | Chirping Stereo | 0.5% |
| Mosquito | Clipped Wings | 1-2% |
| Moth | Wriggling Larvae | 0.5% |
| Firefly / Dragonfly / Mantis | Fire in a Bottle / Vermin Vaporizer Chip / Mantid Claw | 1% |
| Field Mouse | Squeaky Mousemat | 0.5% |
| any pest | **Dung Dye** | 1 in 250,000 (1 in 50,000 from Field Mice) |

Vinyls (2-10%) and baits (10%) are noise.

- **Trigger:** a rare pest drop line. No kill animation - pests die every few seconds.
- **Detection:** **Theory.** Pest drops should be ordinary `RARE DROP!` / `PET DROP!` lines (`RARE DROP! Atmospheric
  Filter (+N ☘ Pest Fortune?)`). SkyHanni has a pest profit tracker that parses them - read its patterns for the
  exact strings (facts only), then confirm from our records: `slayer_line` already catches any `RARE DROP!`, so a
  farming session with records on is enough. "Ours" is free: pests' drops go to the player who killed them.
- **Cutscene: "The Vacuum" popup** (Fin/Wisp style, ~2.5 s, no dim, HUD untouched): a pixel vacuum nozzle swings in
  from the side, sucks the pest in with a little spiral of particles, rattles, and spits the drop out with name and
  price. Pets get the totem-style head pop (`PetHeadPopup`) and their rarity colour.
- **Dung Dye:** the one full cutscene - below.

## 2. Rare crops (the armour sets' bonus)

Wearing Cropie / Squash / Fermento / Helianthus armour, breaking crops can drop the next set's material (the wiki):

| Rare crop | From |
| --- | --- |
| Cropie | Wheat, Carrot, Potato |
| Squash | Pumpkin, Melon, Cocoa Beans |
| Fermento | Sugar Cane, Cactus, Mushroom, Nether Wart |
| Helianthus | Sunflower, Moonflower, Wild Rose (Fermento set bonus, up to 0.004% a crop) |

These drop **many times an hour** for a serious farmer, so a popup every time would be unbearable.

- **Trigger:** **Theory** - a "RARE CROP!" line exists from memory, unconfirmed on the current wiki. Records first.
- **Suggestion:** no animation for Cropie / Squash / Fermento. Helianthus at most a tiny corner toast (a sunflower
  blooming). Or skip rare crops altogether - Matej's call.

## 3. Wild Strawberry Dye (the jackpot)

**1 in 150,000,000 per mature crop broken** (wiki; affected by Overbloom), or 1 in 2,500 from the visitor Vincent.
Farmers break ~20 crops a second, so it is a real, if mythical, chance.

- **Trigger:** **Theory** - presumably a `WOW!` or `RARE DROP!` line naming it, as the other dyes. Matching the dye
  name in any game line (as LavaDrops does for the Carmine Dye) is safe because the name is unique.
- **Cutscene: "Overbloom"** - the only full farming cutscene. Time stops (the dyes' tradition), the dimmed field's
  crops grey texel by texel; then a strawberry vine erupts from the ground at the player's feet, grows across the whole
  screen in pixel tendrils (Brain Food's veins, but green), flowers open in a wave, and every flower turns strawberry
  red at once; the dye forms in the biggest flower. Dung Dye can share the beats with a funnier palette (it is a dye
  from a pest - a mud geyser instead of flowers).

## 4. Garden visitors

Every visitor's offer is a roll too: most rewards are copper and small items, but some visitors can give rare ones -
the wiki gives **Vincent a 1 in 2,500 chance to reward the Wild Strawberry Dye**, and from memory visitors can also
give Overgrown Grass, Jungle Keys, Dedication books and similar. **Theory** for everything but Vincent. Detection: the
visitor's reward appears in its offer menu and in chat on accepting (`OFFER ACCEPTED`, from memory). A rare reward
gets the Vacuum popup's sibling (the visitor's head nods and hands it over); the Dye from Vincent plays Overbloom.

## 5. Greenhouse / mutations (new, 2026)

The Garden now has mutation crops (Dustgrain, Witherbloom, Gloomgourd... - drop rates halved on 2026-03-18). Not
researched further; ask Matej whether any of them is rare enough to matter.

## Open questions for Matej

- Does he farm? If not, records will have to come from a friend, as for slayers.
- Pests: popup for every drop in the table, or only above a price floor (Rat/Slug pets, Atmospheric Filter)?
- Rare crops: skip, or a tiny Helianthus toast?
- Is a 1-in-150M dye worth a full cutscene? (Cheap to build next to the Dung Dye, and it is the dream drop.)
