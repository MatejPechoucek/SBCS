# 04 - Lava fishing: every creature, and what is left

Matej's idea: "other lava fishing mobs that are pretty rare", and (2026-10-23) "include stuff for lava fishing such as
jawbus and others". Eruption already plays for the five rare Crimson Isle creatures, so this file is the whole lava
picture: each creature's real drop table (wiki, 2026-10-23), what Eruption does with it today, and what could be added.

## Today, in one table

Eruption's endings (`EruptionTimeline.Drop`): Chain (PLAIN), Severed Pincer (PINCER), Flash I (FLASH), Brimstone Handle
(FORGE), Radioactive Vial (VIAL, "Meltdown"), Scuttler Shell (SHELL), Burnt Texts (END_TIMES), Carmine Dye (KUUDRA,
"Kuudra Rises"). Everything else is the bust, which shows the guaranteed drop under the cooled vent.

## The creatures

### Lord Jawbus - Mythic, 0.17% (Fishing 45, Adept Trophy Fisher)
Catch line: "You have angered a legendary creature... Lord Jawbus has arrived."

| Drop | Chance | Eruption today | Idea |
| --- | --- | --- | --- |
| Magma Lord Fragment | 100% | shown on the bust | - |
| Silver Magmafish x16-32 | 100% | - | - |
| Lord Jawbus Shard | 2% | bust | small win? (check price) |
| Bobbin' Scriptures | 1% | bust | small win - see below |
| Radioactive Vial | 0.5% | **Meltdown** | - |
| Carmine Dye | 1/50k | **Kuudra Rises** | - |
| *Loot share:* Magma Lord Fragment 20%, Radioactive Vial 0.1% | | plays if the win's line comes to us (0.2.82) | confirm from records |

Jawbus is the creature people hunt for weeks, so **every Jawbus kill is an event even without a drop**. Idea: a Jawbus
**intro** - the golem's real rig (`LavaRigs.golem`) rising out of the lava before the build-up starts, his roar, the
camera shaking - which Thunder and the Scuttler don't get. The same for every Jawbus, so it gives nothing away.
Plus a **"Jawbus #N"** line on every bust ("your 14th Jawbus, 0 Vials") - the stats already count kills per source.

### Thunder - Legendary, 0.86% (Fishing 36, Novice Trophy Fisher)
Catch line: "You hear a massive rumble as Thunder emerges."

| Drop | Chance | Today |
| --- | --- | --- |
| Thunder Fragment | 100% | bust |
| Silver Magmafish x8-16 | 100% | - |
| Flash I book | 1.5% (loot share 0.3%) | **FLASH** |
| Carmine Dye | 1/50k | Kuudra Rises |

Thunder is common enough (1 in 116 catches) that its bust should be the shortest of all. Check that the short-bust
setting feels right for Thunder in particular; maybe Thunder busts are short by default.

### Ragnarok - Mythic, 0.25% of Crimson Isle hotspot catches (Fishing 47)
Catch line: "The sky darkens and the air thickens. The end times are upon us: Ragnarok is here."

| Drop | Chance | Today |
| --- | --- | --- |
| Silver Magmafish x16-32 | 100% | - |
| Bobbin' Scriptures | 1% | bust |
| Chain of the End Times | 1% | **PLAIN** |
| Brimstone Handle | 0.5% | **FORGE** |
| Burnt Texts | 0.25% | **END_TIMES** |
| Carmine Dye | 1/50k | Kuudra Rises |

Fully covered. Possible addition: Ragnarok's own intro (the sky darkening - the floor fog turned red-black).

### Fiery Scuttler - Legendary, 1% of hotspot catches (Fishing 37)
Catch line: "A Fiery Scuttler inconspicuously waddles up to you, friends in tow."

| Drop | Chance | Today |
| --- | --- | --- |
| Scorched Crab Stick | 100% | shown on the bust |
| Severed Pincer | 0.2% | **PINCER** |
| Scuttler Shell | 0.1% | **SHELL** |
| Carmine Dye | 1/50k | Kuudra Rises |

Fully covered.

### Plhlegblast - Mythic, 0.1% of Plhlegblast Pool catches (Fishing 36)
Catch line: "WOAH! A Plhlegblast appeared."

| Drop | Chance | Today |
| --- | --- | --- |
| Ink sacs, lily pads | 100% | - |
| Enchanted Ink Sac | 0.5% | nothing |
| Squid Boots | 1% | nothing |
| Bobbin' Scriptures | 1% | nothing |
| Carmine Dye | 1/50k | **Kuudra Rises** |

Today Plhlegblast plays **only** for the dye (Matej's call, 0.2.74). Option: play every Plhlegblast kill like the
others, with a Squid Boots ending, since the creature itself is a 1-in-1000 pool catch. Price check the boots first.

### Taurus - Epic, 3.4% (Fishing 35)
Catch line: "Taurus and his steed emerge."

| Drop | Chance | Today |
| --- | --- | --- |
| Horn of Taurus, Silver Magmafish | 100% | - |
| Taurus Helmet | 2% | - |
| **Carmine Dye** | **1 in 2,500,000** | **missed** |

**The one real gap.** Eruption doesn't follow Taurus, so a Taurus Carmine Dye - the rarest roll in lava fishing - plays
nothing. Fix: let the dye line open Kuudra Rises from any lava creature, the way the Diana Dye plays from any
creature. Add "Taurus" to `SlayerRecorder.LAVA` first. **Recommended, small.**

### The rest (no rare drops)
Magma Slug, Moogma, Volcanic Snail, Lava Leech, Pyroclastic Worm, Lava Flame, Fire Eel, Fireproof Witch, Magma Pillar,
Fried Chicken (Crimson Isle), and the Crystal Hollows' Magma Fields creatures: nothing worth a reveal. The Carmine Dye
may be on some of their tables at the 1/2.5M rate - the any-creature dye rule above covers that too.

## Bobbin' Scriptures, for every mythic

1% from Jawbus, Ragnarok, Plhlegblast, Titanoboa, Wiki Tiki, Nessie... A cheap book. Today a bust. Option: a one-second
"small win" beat after the bust (the book flips open, a pixel fish jumps out). Matej's call.

## Open questions

1. The any-creature Carmine Dye - yes? (recommended)
2. A Jawbus intro and the "Jawbus #N" line on busts?
3. Plhlegblast: keep it dye-only, or play every kill with a Squid Boots ending?
4. Bobbin' Scriptures and the Jawbus Shard: small wins or busts?
