# The SBCS Ledger

A local achievement book: a "Ledger" tab beside Stats, an openable leather book of ink stamps, a chapter per
animation, old stats backfilled.

## The stamp list

**`docs/ledger_stamps.xlsx` is the source** (ID, chapter, name, what earns it, kind, four tier numbers, hidden, keep,
hint). `tools/gen_ledger.py` writes `ledger/StampTable.java` from it - edit the sheet and re-run, never the Java. IDs
are stable; what each one *counts* is code, `Ledger.value` (a switch on the ID; unknown IDs are events).

Saving the sheet with openpyxl drops Excel's extension-form dropdowns (the Chapter list): re-add it as a plain
`DataValidation` (`=Chapters!$A$2:$A$10`, B2:B140) after any scripted edit.

- 12 stamps a chapter (10 for Fin and Wisp). `Chapter.REVENANT` ("Exhumed") and `TARANTULA` ("The Brood") come after
  `SERAPH` so every older chapter keeps its ordinal - shapes and doodles are picked by it.
- **Secret stamps** carry a `hint` pencilled on their page in place of the rule; the pop-up says "SECRET STAMP!".
- A desc's "N" / "Nth" is filled with the stamp's number by `LedgerView.describe`.
- **An awarded event reads as its own number** (`need(grades)`): read as 1, events whose top tier is a large number
  were flagged but never stamped.

## `com.sbcs.ledger` is pure

No Minecraft, no files: `Chapter` (a colour for the tab, a darker `ink` for the page), `Grade` (Select, Choice, Prime,
A5 Wagyu and their ring colours), `Stamp`, `Facts` (what the stats say - `SbcsStats.facts()` fills it), `LedgerData`
(saved inside stats.json as `ledger`: entries {grade, at, value}, event `flags` {id -> kill}, `seen`, counters, the
last 60 drops), `Ledger` (value, gradeFor, refresh, award, run, drop, corpseWorth).

## Counting

- Every `SbcsStats.on*` ends in `Ledger.run`, `Ledger.drop` for a win (hot streak, night shift 3-5 AM local, the 777th
  kill of a source, back to back, the first pet's kill) and `stampCheck()` (refresh, then `SbcsStats.stamped` ->
  `StampPopup.queue`).
- `ledger()` backfills once (`backfilled`): cutscene counts from every source, `firstPetKill` as an upper bound, then a
  refresh with `pre` - undated, not announced. Resets never touch the Ledger.
- Events fed from outside: `onSkip` (never a test), `onCoins` (a real win's price from each screen's `summary()`),
  `onCorpseWorth` (the last real corpse once `CorpseValue` is complete, 60 s at most, live prices on), `award` for a
  Jawbus win and a legendary Megalodon; Diana's loot share is `DianaWatcher.lastShareAt` within 10 s of the win.
- **Timed events** (`Ledger.timed`, only when the clock is sane - now is past the last recent drop): dates (Friday
  13th, 29 Feb, Christmas, 31 October), hot streaks, Two-Timer (two sources within 5 min), the Ghosts' windows
  (`HAUNT_MS` 30 s, `GHOST_RUN_MS` 60 s), Back From the Dead (two Revenant drops in 5 min).
- **Daily rules**: `Ledger.kill` keeps per-key counts for the local day (`killsDay` / `dayKills`, `killsToday`), e.g.
  Natural Talent (a drop on the day's first kill), Beginner's Luck (a pet within the day's first 10 Scatha kills,
  `scathaDayKills`), Nobody Expects It (within the day's first 5 Inquisitors), Eight Legs (a drop on the day's 8th
  Tarantula).
- `Is That It?` (a rare pet after 300+ dry) takes the gap from `recent`, or the whole count for a first pet - never
  guessed.
- `/sbcs ledger recheck` = `SbcsStats.recheck()`.

## The book - `gui/menu/LedgerView`

- Laid out at 412 x 250 and scaled into the screen in quarter steps (2.0 at a 960 x 540 GUI; 10 px kept clear above
  and below), resting at `TILT` -0.02 rad with more while it lands, leaves, opens and turns; `toBook` undoes the
  transform for the mouse.
- The Ledger tab sets `MenuView.ledgerMode` (static, so the menu reopens in it); the menu fades out over 180 ms and the
  book drops in shut (`LedgerView.enter`, `backOut`) and swings open. Esc (`MenuView.escape`) and the ribbon
  (`shutByRibbon` -> `wantsLeave`) put it away. `tab` never becomes `TAB_LEDGER`.
- **The cover**: leather, gold border, the icon, "THE SBCS LEDGER", stamp counts, a strap. It keeps its face while it
  swings: `cover` draws `coverFace` whole, squeezed toward the spine with `Draw.scale(sx, 1)` and darkened.
- **Pages**: divider tabs down the right edge (a red dot for unseen stamps; they shrink to fit - `TAB_H` from the
  chapter count), ruled pages with a red margin, a red ribbon. Left page: the chapter's title page (stamps, grades,
  numbers, the closest stamp with a bar) or a stamp's own page (large, grade, description or hint, tiers, progress,
  "Stamped <date>" or pre-ledger). Right page: 4 x 3 stamps, "< n / m >" past twelve, "NEW" in red on fresh ink (dried
  when the chapter is left, the book shut or the stamp clicked).
- **Turning** (`Spot` = chapter + page; `next` / `prev` run through the whole book): folded corners (click, or drag -
  over at `TURN_COMMIT` 0.3), the wheel, the divider tabs. The statics stay at `turnFrom`; `at(spot, r)` draws a page
  as another spot; the sheet is that page squeezed toward the spine, darkening as it stands up.
- **Pencil**: a quip and a 2x doodle per chapter (`note`, `DOODLES` in Chapter order), a double squiggle under the
  title, a coffee ring on two chapters in three, tally marks.
- Stamp names are 0.75x, wrapped at 36.

## Stamps - `gui/menu/StampArt`

A grade ring, a paper ring, the ink disc, the chapter's item, pips, worn flecks fixed per id, a tilt per id.

- `StampArt.ITEMS` gives each stamp its own icon. A vanilla item must also be a case in `GuiDraw.stack`, or it draws as
  paper (and on 26.2 coloured items are collections - see [rendering.md](rendering.md)). `head:<set>:<name>` draws a
  SkyBlock head (`HeadSkins.named(set).blob(name)`; `head:scatha:pet` is `PetHeadPopup.SKIN_BASE64`) as a PLAYER_HEAD
  with a resolved profile (`GuiDraw.head`); `pack:<path>:<fallback>` a Hypixel pack icon. Icons still vanilla are
  listed in `docs/TODO.md`.
- `StampArt.shape`: one of six `Shape`s (circle, octagon, rounded square, shield, diamond, scalloped seal) by its place
  in the chapter, so neighbours never match. `inside(shape, x, y, r)` is the one definition; `fill` / `outline` draw it
  a row of runs at a time; empty slots pencil the outline dashed.
- The hazard tape (used for a taped chapter and the maintenance card).

## The pop-up

`render/StampPopup` (the queue; shown from `drawOwnHud` only when no cutscene, pop-up or pre-kill hide is up -
`tick(free)`) over `gui/menu/StampCard` (pure: a paper card, the stamp falling from 2.6x and turned, the thump at
170 ms with ink flecks and a jolt, "STAMPED!", the name, the grade; 2.95 s). `StampPopup.CAP` 3 in a row, then
`StampCard.paintMore` "+N more stamps". `Voices.stamp` (thump, edge click, paper, two bell notes, a third for a raised
grade) on `ledgerVolume`; `ledgerPopups` under General. `GuiDraw` is the menu's in-game `Draw`, shared.

## Developer commands

`/sbcs test ledger` (developers only): `popup <id> [grade]` (only the pop-up, shown even with pop-ups off; `popup
burst` queues six), `give <id> [grade]` / `take <id>`, `info <id>`, `fill` (every stamp at its top), `unseen` and
`wipe confirm` (empty, then the backfill again). Every stamp id is its own literal, so tab completes them.

## Safety

`LedgerData.fix()` caps grades at the stamp's, drops empty entries, zeroes negatives, caps `recent`, keeps unknown ids.
The clock-based awards only run when now is past the last recent drop.

## Tests

`tools/LedgerTest`: every chapter has 10+ stamps, every name a stamp looks for is spelled as the stats write it (reads
the drop enums' source), the clock and clean-load checks, events, backfill.
