# 06 - Superpairs (the Experimentation Table)

Superpairs is the Experimentation Table's main experiment: a memory game where every pair you match is a reward. The
grid is the roll, since what sits under each tile is random. The jackpots are ultra-rare books (the VIIs), the
Nadeshiko Dye, Experiment the Fish and the Metaphysical Serum. Matej picked this one out of the extra ideas
(2026-10-23).

## How it works (wiki, Superpairs page, 2026-10-23)

- The table has 3 charges, one back every 24 h, plus 3 paid resets (XP levels + bits). So it is a few games a day.
- Superpairs unlocks after the two add-ons (Chronomatron, Ultrasequencer), which only give XP and **bonus clicks**.
- You flip two tiles at a time. A matching pair awards that tile's reward. There is a **time limit** and a **click
  limit** (10-16 by stake; Metaphysical is 12 + 2x the add-on clicks).
- Power-ups on the grid: Lapis (+1 click), Feather (+3 clicks), Diamond (the next click finds a pair).

| Stake | Grid | Rare rewards in the pool |
| --- | --- | --- |
| Beginner | 7x2 | Guardian pet |
| High | 5x4 | Guardian pet, Titanic Exp Bottle |
| Grand | 5x4 | + Metaphysical Serum, books III-VI |
| Supreme | 7x4 | + **Nadeshiko Dye**, tiny chance of a **VII** |
| Transcendent | 7x4 | very rare VII |
| Metaphysical | 7x4 | + **Experiment the Fish**, rare VII |

- **Ultra-rare books and endcap upgrades:** Supreme and up. At Metaphysical it's a **1% base chance per experiment**
  that one is on the grid, ~0.027% (1 in 3,700) for each particular one. A Mythic Guardian pet adds 7% and Enderman
  Slayer VIII adds 15% (to that 1%).
- RNG meter: 1 RNG XP per 1,000 Enchanting XP. The Nadeshiko Dye needs 2.5M, the most of anything.
- Not on the page we read (truncated at 100k characters): per-tile weights, the menu title, chat messages.

## What makes it a good roll

It has the tension built in. When you flip a tile and it's an ultra-rare book, **you haven't won it yet**: you still
have to find its pair before the clicks or the clock run out. Every other animation has to manufacture that suspense.
Here the game provides it, so the animation only has to make the player feel it.

## Trigger

1. **An ultra-rare tile is revealed** (a VII book, an endcap upgrade, the Nadeshiko Dye, Experiment the Fish,
   optionally the Serum). This starts the "hunt".
2. **Its pair is matched** (won) or **the game ends without it** (lost).
3. Optional (Matej's call): a small end-of-game summary of the best reward, for an ordinary good game.

## Detection

**Theory: everything is in the menu, nothing has been recorded yet.**

- The game is a chest-style container (title from memory "Superpairs (Metaphysical)", one per stake). As the Ossuary
  reads dungeon chests, read the slots every tick. A hidden tile is a fixed placeholder item, and a revealed one shows
  the reward's stack with its name and lore (book name and level, dye, pet, serum).
- **Matched vs merely shown:** after a non-matching pair the tiles flip back. A matched pair stays revealed, or
  changes look, or is marked in lore. The records must settle which.
- **Clicks and time left:** the menu shows them (from memory, a clock/item in a corner slot). Read them to know how
  close the hunt is.
- **Never click for the player** (the Ossuary rule), and never read anything the game doesn't show: hidden tiles stay
  hidden to us too. Mods that reveal hidden tiles or solve the add-ons exist, but this mod only reacts to what the
  player can already see.
- Recording: a `superpairs_menu` record per change (title, slot -> item name + lore, clicks/time), written by a
  small `DropRecorder`-style recorder while the menu is open. Two or three games from Matej give the strings.

## Matej's rule: a rare tile is covered (2026-10-23)

"If it is an ultra-rare tile, please cover it with some kind of black box or something within the gui because you
don't want the user to see it." So the item itself is never shown until the cutscene.

- **The cover.** When a revealed slot holds a covered-tier item, we draw a black box over that slot inside the menu,
  in place of the item and its tooltip (no hover tooltip either, or the name leaks). It's the same idea as the
  Nucleus bundle cover (`render/BundleCover`), done in the GUI with a container-screen hook instead of in the world.
  It goes on the frame the tile flips, so the player never sees the item for even one frame. **Theory:** the hook
  point needs checking with javap (the container screen's slot render, or the item tooltip).
- **The box has to be the same for every covered item**, or its look gives the drop away. So it's one plain black
  tile with a faint shimmer. It shows the same whatever is under it.
- **Cover more than the jackpots**, or a black box on its own means "VII book". Cover every tile from the "rare" row
  up (Serum, Experiment the Fish, Legendary Guardian, the VIIs, endcaps, the dye). Then a box only says "something
  good", and the cutscene tells you what.
- **The game must stay playable.** Superpairs is matching, and two covered tiles might be different items (a Serum
  and a VII). If both are plain black, the player can't tell whether they match. So each covered tile also carries a
  small **mark** (a letter or rune, A, B, C... in the order covered items were first seen this game). Its pair gets
  the same mark when it flips. The mark tells you which boxes match and nothing about what is under them.
- **Hidden after the match too.** A matched pair stays covered. The reward is only shown by the cutscene after the game,
  and the claim screen (if it lists rewards) is covered the same way until then.
- **Off switch.** A menu setting "Cover rare Superpairs tiles" (default on), for players who would rather see.

## Cutscene concept: "The Hunt" (around the menu, never blocking play)

The player is busy in a menu with a time limit, so only the covers sit on the grid; everything else is around the
menu's edge, and the reveal waits until the game ends.

- **A box appears (the hunt starts):** the box lands on the tile with a soft thud and its mark glows. A low heartbeat
  starts (`Voices`, quiet). Faint embers rise from the edges of the screen outside the menu. The player still has to
  play.
- **While hunting:** each further flip that misses makes the heartbeat a touch faster, and the remaining clicks
  appear beside the menu as pips, going out one by one. That's the tension, and it's the true state of the game, so
  it isn't a lie.
- **Match:** a crack of light runs between the two boxes, both flash, the heartbeat stops on one chord. The boxes stay
  shut.
- **After the game** (it ends, or the menu closes), **the reveal plays**, one per matched box, best last. The box drifts
  to the middle of the screen and opens. A Serum, a Fish or a Guardian gets a short reveal. A VII or an endcap: the
  book rises, opens, its pages riffle, and the name stamps in letter by letter with odds ("1 in ~3,700 experiments")
  and the price counted up. The Nadeshiko Dye gets the grandest version: pink petals (its colour) fill the screen in
  pixel squares and the dye forms in the middle, as each animation's dye does. Because every box looks the same, the
  build-up as it opens is identical whatever is inside (the honesty rule, checked by a test as the others are).
- **Lost (clicks or time ran out with a box unmatched):** the game ends and the box opens anyway, to show what got
  away: a short, grey "missed" reveal (the item, "you didn't find its pair"). Matej's call whether a missed one should
  be shown at all, or stay secret.
- **Ordinary game, optional:** after a game, a small corner card with the best reward (e.g. a VI book) and the
  game's total worth. Off by default.

## Drop tiers

| Tier | What | Ending |
| --- | --- | --- |
| Jackpot | Nadeshiko Dye | the full screen, petals |
| Ultra-rare | a VII book, an endcap upgrade | book rises, stamp, price |
| Rare | Experiment the Fish, Metaphysical Serum, Legendary Guardian pet | short version of the above |
| Normal | VI and lower books, bottles | nothing, or the optional corner card |

The "rare" row needs prices checked. Experiment the Fish and the Serum may be cheap enough to drop to "normal".

## Open questions for Matej

1. A missed box (never matched): open it at the end to show what got away, or keep it secret forever?
2. Which rewards get covered: from the Serum / Fish / Guardian up (recommended, so a box doesn't mean "VII"), or the
   VIIs and the dye only?
3. The mark on each box (A, B, C...) so pairs can still be matched: OK, or another way?
4. Does Matej do Metaphysical daily? His games with records on are what detection gets written against.
