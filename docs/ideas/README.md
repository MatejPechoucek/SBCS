# Ideas: future RNG cutscenes

Plans for animations SBCS does not have yet, one file per idea. Nothing here is built. Started 2026-10-23 from
Matej's list (Scatha drops, Wiki Tiki, Titanoboa, other rare lava creatures, farming) plus a few more.

Every plan has the same sections: what triggers it, how detection would work, the drop tiers, a cutscene concept,
and open questions. Detection claims carry one of three labels, the same way the Ossuary plan did:

- **Proven** - already works in SBCS, or seen in our own `sbcs-drops.jsonl` records.
- **Wiki** - from hypixelskyblock.minecraft.wiki (the fandom wiki is paywalled) or another mod's source, never seen
  in our records. Hypixel's wiki strings have been wrong before (the Nucleus weights).
- **Theory** - a guess. Has to be checked against records before anything is written against it.

The rules that apply to all of them come from CLAUDE.md and are not repeated per file: an honest tell (nothing before
the reveal depends on the outcome, checked by a `*Test` that hashes every draw call), the dye gets the grandest
ending, busts short, expensive drops long, cheap drops short, pixel-art effects only, no full-screen white, count
draw calls before handing over, offline previewer, records first.

## The list

| # | Idea | Source | Trigger rarity | Detection | Size | Suggested order |
| --- | --- | --- | --- | --- | --- | --- |
| 01 | [Scatha pet tiers](01-scatha-pet-tiers.md) | Crystal Hollows | pet 1 in ~4,200 kills | **Proven** (existing) | medium - extends the Cleaver | 1 |
| 02 | [Wiki Tiki](02-wiki-tiki.md) | Hotspot fishing (water) | creature 1 in 400 hotspot catches | Wiki, LavaWatcher pattern | large | 4 |
| 03 | [Titanoboa](03-titanoboa.md) | Backwater Bayou fishing | creature 1 in 400 catches | Wiki, LavaWatcher pattern | large | 3 |
| 04 | [More lava fishing](04-lava-fishing-more.md) | Crimson Isle | various | **Proven** (Eruption) | small - extends Eruption | 2 |
| 05 | [Farming](05-farming.md) | The Garden | pests and rare crops | Wiki + Theory | small popups + one big dye | 5 |
| 06 | [Other water mythics](06-water-mythics.md) | Galatea, Spooky, Winter | creature 1 in 400 | Wiki | medium, one shared cutscene | 6 |
| 07 | [Trophy fish (Diamond)](07-trophy-fish.md) | Crimson Isle | Diamond trophy fish | Theory | small popup | 7 |
| 08 | [Zealots and the Eye](08-zealots.md) | The End | Special Zealot | Wiki + Theory | small | 8 |
| 09 | [Other slayers](09-other-slayers.md) | Rev, Tara, Sven, Vampire | rare boss drops | Hellion/Seraph pattern | large, per boss | 9 |
| 10 | [Kuudra chests](10-kuudra-chests.md) | Crimson Isle | paid chest | Ossuary pattern | large | 10 |
| 11 | [Any dye, anywhere](11-any-dye.md) | everywhere | RNGesus dyes | Theory | small - a fallback | anytime |

The order is a suggestion: cheapest and best-proven first. 01 and 04 reuse detection that already works, 03 and 02
share one new watcher (both are water-fishing mythics with the Aquamarine Dye), and 06 is that same watcher with more
creatures in it.

## What to record first

Several plans need real chat lines and nametags before detection can be written. `DropRecorder` already keeps every
`RARE DROP!` line (the `SLAYER` pattern catches it, recorded as `slayer_line`) and lava/shark lines as `fishing_line`.
Not yet recorded, and needed:

- Water mythic catch lines and nametags (Titanoboa, Wiki Tiki, Nessie...) - `SlayerRecorder.LAVA` would need their
  names, and `FISHING` their catch lines. One line each, no behaviour change; worth doing on its own soon so records
  pile up while nothing is built.
- Pest kills and their drop lines, and the rare crop line (05).
- Trophy fish lines (07).

## Sources

hypixelskyblock.minecraft.wiki pages read on 2026-10-23: Scatha, Titanoboa, Wiki Tiki, Lord Jawbus, Ragnarok,
Plhlegblast, Taurus, Nessie, Zealot, Mosquito, Sea Creatures, Sea Creatures/List/Crimson Isle, Helianthus Armor,
Fermento Armor, Wild Strawberry Dye, Changelog/2026/March 18. Pest drop table: Elite SkyBlock's pest farming guide
(eliteskyblock.com/guides/tWvd7c). Everything else is marked Theory.
