# 03 - Titanoboa

Matej's idea. A Mythic water sea creature of the Backwater Bayou: a giant snake made of many segments, 45M HP.

## Trigger

Every kill of **our own** Titanoboa (caught by us, or one whose win comes to us through loot share), as Eruption does
for Jawbus. Opens on the kill with the outcome pending.

## Drops (wiki, 2026-10-23)

| Drop | Chance | Tier for the animation |
| --- | --- | --- |
| Enchanted Lily Pad x2-4 | 100% | the bust |
| Condensed Lily Pad | 1% | bust (cheap - check price) |
| Bobbin' Scriptures | 1% | small win (shared by every mythic - see 04) |
| **Titanoboa Shed** | 0.2% | the win (accessory) |
| **Snake Eyes** | 0.11% | the rarer win |
| **Aquamarine Dye** | 1 in 50,000 | the dye - the grandest ending |

Loot share works, with the usual 5x lower chances. The Titanoboa Shard comes from charming, which always procs on the
kill (it is Elusive) - not a reveal.

Catch rate: weight 25, 0.25% of catches (1 in 400) at Fishing 30+; Whale/Light/Dark bait x1.25.

## Detection - LavaWatcher's rules, a new creature list

- **Catch line (wiki):** "A massive Titanoboa surfaces. Its body stretches as far as the eye can see." Feesh may
  rewrite it (as for lava: "A Titanoboa has spawned!") - `LavaDrops.catchOf` already handles both shapes.
- **Ours:** nametag first seen within 20 blocks, 2 s before to 6 s after our catch line (**Proven** for lava).
- **Dead:** HP reads 0, or the nametag goes at <= 5% (**Proven** for lava).
- **Drop line:** `RARE DROP! Titanoboa Shed (+N ✯ Magic Find)` (**Theory** - the lava format without brackets).
- **The snake is many segments.** Each segment may be its own armour stand or entity; only one should carry the HP
  nametag. **Theory** - the first records must show how many nametags one Titanoboa has. If every segment shows HP,
  dedupe by "first one seen" as the Seraph's two-entity spawn was handled.
- Best built as one `FishingWatcher` that takes a creature table, so Wiki Tiki (02) and the other water mythics (06)
  are rows, not classes. LavaWatcher could later move onto it.

## Cutscene concept: "The Coil"

Over the dimmed game, from the side, like Eruption's camera. The snake is the tell.

- **Build-up (identical every run):** the bayou's water as a pixel floor (Eruption's CPU floor), the Titanoboa's body
  surfacing in loops (segments as boxes with its real texture - the NEU bestiary skin if it is a head, else rig). It
  coils round a lily pad at the centre, tighter each lap, its scales shifting colour through a fixed cycle (lily green,
  swamp brown, gold, aquamarine) like the Vault's dial.
- **Reveal at 4800:** the coil lands on a colour. Brown = bust: it slackens and sinks, the lily pad floats up with the
  Enchanted Lily Pads. Green = Shed: the snake slides out of its own skin, leaving a pale hollow coil that becomes the
  accessory. Gold = Snake Eyes: two eyes open in the dark centre of the coil, blink once, and are the drop.
- **Aquamarine Dye ("The Bayou Floods"):** the stopped-world trick (greyed, gild), then aquamarine water rises up the
  coil texel by texel and the snake rears into a vertical column of the dye's colour, the dye at its head.

## Open questions

- Is Condensed Lily Pad worth anything? (price check; probably bust.)
- Bobbin' Scriptures: small win or bust? It drops from every mythic at 1% - see 04.
- Should Titanoboa and Wiki Tiki share one menu card ("Water mythics") or have their own?
