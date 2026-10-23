# 09 - Crystal Hollows treasure chests

My idea. Mining in the Crystal Hollows sometimes spawns a **treasure chest** next to the player, opened by a
lockpicking minigame. Every chest is a roll on a loot table whose top is the **Prehistoric Egg** - a Scatha-hunter's
other jackpot, so it sits beside the Cleaver and the Vault naturally.

## Trigger

Opening a Crystal Hollows treasure chest. Plays only for the rare part of the table; the powder and rough-gem common
results do nothing (or a tiny toast - Matej's call).

## Rolls

Old fandom table (the Jungle chest; **out of date, check the current wiki**) - per chest:

| Result | Chance |
| --- | --- |
| Rough gems, powder | the bulk |
| Oil Barrel | 3.23% |
| Wishing Compass | 2.02% |
| Ascension Rope | 0.65% |
| Flawed gems (larger stacks) | 0.2-0.5% |
| 1200-4800 Mithril / Gemstone Powder | 0.32% each |
| **Prehistoric Egg** | **0.08%** (1 in ~1,250) |
| Fine Amethyst | 0.02% |

From memory, other zones' chests add **Pickonimbus 2000**, **Goblin Eggs** (Green/Red/Yellow/Blue - the Goblin Egg
pages exist on the wiki) and **Jungle Heart / Divan** parts - **Theory**, read the current wiki loot pages first.

## Detection

**Theory.** The chest's loot is announced in chat as it opens (from memory "CHEST LOCKPICKED" then a loot line per
item, SkyHanni has a powder/chest tracker that parses it). Add the chest lines to `DropRecorder.NUCLEUS` (already
Crystal Hollows-themed) and read the real strings from a mining session.

## Cutscene concept: "The Lockpick"

Short. The chest appears in the corner (vanilla chest model, the Ossuary's chest machinery may be reusable), a
lockpick turns in the lock through tumblers that click in a fixed sequence, the lid lifts and light spills out in the
best item's colour. **Prehistoric Egg:** the lid flies off, a cracked stone egg rises and wobbles three times, a crack
of light runs round it - and it stays whole (it's an egg you hatch later), with its price.

## Open questions

- Does Matej mine in the Hollows besides Scathas? If yes, this is a natural third Hollows animation.
- Show only Prehistoric Egg / Pickonimbus / Goblin eggs, or every chest?
