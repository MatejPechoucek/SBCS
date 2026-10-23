# 04 - More lava fishing

Matej's idea: "other lava fishing mobs that are pretty rare". First the honest finding: **Eruption already covers
every rare drop of every rare Crimson Isle lava creature** - Thunder, Fiery Scuttler, Lord Jawbus, Ragnarok and
Plhlegblast (for the Carmine Dye). What is left is small, so this is a list of **extensions to Eruption**, not a new
cutscene.

## The lava creatures (wiki, Crimson Isle list)

| Creature | Tier | Base chance | In Eruption? |
| --- | --- | --- | --- |
| Magma Slug, Moogma, Volcanic Snail | Uncommon | 25-35% | no - nothing rare |
| Lava Leech, Pyroclastic Worm, Lava Flame, Fire Eel, Fireproof Witch | Rare | 6-15% | no |
| Taurus | Epic | 3.4% | no |
| Magma Pillar | Epic | 5% | no |
| Thunder | Legendary | 0.86% | yes |
| Fiery Scuttler | Legendary | 1% (hotspot) | yes |
| Lord Jawbus | Mythic | 0.17% | yes |
| Ragnarok | Mythic | 0.25% (hotspot) | yes |
| Plhlegblast | Mythic | 0.1% of Plhlegblast Pool catches | dye only |

## Candidates

1. **Taurus's Carmine Dye (1 in 2.5M).** The Carmine Dye drops from Taurus too. Eruption's dye detection already reads
   any line naming the Carmine Dye, but only for a creature Eruption followed. Make the dye play from **any** lava
   creature (Kuudra Rises with a generic creature at the start, or the creature skipped). Cheap and the rarest drop in
   lava fishing. **Recommended.**
2. **Plhlegblast's Squid Boots (1%).** Plhlegblast is a 1-in-1000 pool catch, so its kill is special by itself;
   today it plays only for the dye. Add a small PINCER-sized ending for Squid Boots if the boots are worth the 500k
   floor (price check), else leave it.
3. **Loot-shared Radioactive Vial.** Jawbus's loot share pool has the Vial at 0.1%. Eruption already plays someone
   else's creature when its own win comes to us (0.2.82) - check a loot-shared Vial actually opens it (**Theory**:
   only the line format is unknown).
4. **Bobbin' Scriptures (1% from every mythic, lava and water).** A cheap win. Today it is a bust. Option: a 1-second
   "small win" beat after the bust (book flips open, a pixel fish jumps out). Ask Matej - it would make 1 in 100
   kills not a bust.
5. **Taurus Helmet (2%) / Horn of Taurus.** Cheap; leave as nothing.
6. **Trophy fish** - the actually rare lava-fishing RNG nobody animates. See [07](07-trophy-fish.md).
7. **Galatea lava** (Stridersurfer, Moonglade Marsh) - only a Rare creature. Nothing to animate.

## Detection

**Proven** for everything above except the Taurus nametag and line (not in `SlayerRecorder.LAVA`) and the loot-share
Vial line. Adding "Taurus" to the recorder's list is the first step.

## Open questions

- Which of 1-4 does Matej want? Recommendation: 1 now (tiny), 3 verified from records, 4 if busts feel too common.
