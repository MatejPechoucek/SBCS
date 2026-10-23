# 08 - Fishing treasure (Outstanding Catch and the Treasure Dye)

My idea. Every fishing catch can be **treasure** instead of a fish or a creature, and treasure rolls a tier. It is a
real roll that every fisher sees constantly - water, lava, winter, the Galatea islands - and it has its own dye.

## Trigger

A treasure catch of the GREAT or OUTSTANDING tier, and the Treasure Dye from any tier. GOOD catches never play.

## Rolls (wiki, Treasure page, 2026-10-23)

| Tier | Default share of treasure catches | Boosts (additive) |
| --- | --- | --- |
| GOOD CATCH | 89% | - |
| GREAT CATCH | 10% | Hermit Crab +10%, Blessing +12%, Blessed Bait +50%, Mantis Shrimp shard +10% |
| OUTSTANDING CATCH | 1% | the same, Blue Crab shard instead of Mantis Shrimp |

- **Treasure Dye:** overrides the roll at 0.0001% (Good), 0.001% (Great), **0.01% (Outstanding)** - so the Dye nearly
  always comes as an Outstanding catch.
- Each place has its own pool: Water, Lotus Atoll, Winter, Moonglade Marsh, Torrhus Canyon, Junk, Fairy Pond, Lava,
  Crimson Isle. Lava's (read): Outstanding is 500k-1M coins; Great a Titanic Experience Bottle or 250-500k coins. The
  water pools' Outstanding items need reading per page.

## Detection

**Theory, likely easy.** The tier is in the chat line: from memory `OUTSTANDING CATCH! You caught a <item>!` /
`GREAT CATCH! You found ...`. SkyHanni's fishing tracker parses these (strings as facts). Add `CATCH!` to
`DropRecorder.FISHING` now so the real lines are recorded. No kill, no pending: the line is the outcome.

## Cutscene concept: "The Chest on the Line" popup

Fin-sized (~2.5 s, no dim). A small chest on a hook swings up out of a water strip; its lock glows; it pops open with
the tier word stamped in the tier's colour (GREAT blue, OUTSTANDING gold) and the item rises out with its price.
**Treasure Dye:** the only full cutscene - the chest is too heavy to pull, the line strains, the water under it turns
the dye's colour in rings, the chest bursts and the dye floats up through a geyser (a cousin of Diana's Dye geyser).

## Open questions

- GREAT too, or only OUTSTANDING? GREAT is 1 in 10 treasure catches - probably too often unless it is a tiny toast.
- A price floor for Outstanding items (some are coins, some are items)?
