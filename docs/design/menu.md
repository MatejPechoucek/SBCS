# The menu and settings

## Architecture

`gui/menu/MenuView` is the whole menu - rows, layout, drawing, mouse - with **no Minecraft in it**. It draws through
`gui/menu/Draw` (fill, text, width, item, modIcon, clip, push / translate / scale / rotate / pop) and asks a `Host`
for the config, "Try it", reset, Diana's drop names and the version. `gui/SbcsMenuScreen` is a thin `Screen` that
implements `Draw` over `GuiGraphicsExtractor` and forwards the mouse.

The split exists so the real menu can be rendered offline: a Java2D `Draw` with the game font's character advances,
plus stubs for `FabricLoader`, Gson and `SbcsClient.LOGGER`. **Keep `MenuView` free of Minecraft, Fabric (beyond
config / stats) and the market classes**, or that stops working.

The menu rebuilds its row list from config every frame, so what is drawn and what is clickable cannot drift apart.
Sliders carry a stable id so a drag survives the rebuild. Groups are built by `buildRows` calling each group's builder
in turn - chained builders let a closed group's early return hide the next.

## The look

Embers over a vignette; the title with the mod icon (`TEX_ICON`, 32 px at its own size); a gradient panel with a gold
trim and a glint along the top, as tall as its content (`shownH` eases; `PANEL_W` 440, `PANEL_H_MAX` 400, `PAD` 12,
`GAP` 4); tabs with 7x7 pixel icons and a sliding gold underline (a tab too narrow for its name draws its icon alone);
each animation a card in its own colour with a vanilla item icon, a subtitle, and **its on/off switch on the card**
(`HeaderRow.onSwitch`, `HeaderRow` takes an indent for sub-groups). Pill switches eased per id (`approach`), sliders,
`< value >` choices (right-click goes back), `ButtonRow` for "Play" and the red two-click resets, `HeroRow` for the
dry streak. Smooth scrolling, faded list edges, a footer with hints and the version. The tab and the open cards are
static, so the menu reopens where it was left.

Tabs: Animations, Sound (every volume, `snd_*` slider ids), Stats, Ledger (see [ledger.md](ledger.md)), General (live
prices and the price floor under Prices, Ledger pop-ups...). The menu is on the vanilla font; `render/SbcsFont` and
`assets/sbcs/font/` are kept for a future replacement (`ui.json` plus a `.ttf`).

## "Try it"

Every group ends in "Try it" (`tryRows` + `SbcsClient.playTest`, including "scatha" and "vanguard"). Menu text must not
point players at `/sbcs test`. Try it follows the bust setting: `playTest` sets `SbcsClient.tryingIt`, which the screen
keeps as `followsBusts`, so `shortBust` applies; with busts off, a bust "Try it" plays nothing and says so
(`testWins`). Without a world (the title screen) `Host.canTry()` is false and the rows say to join one.

## Settings that apply across sources

- **Busts** per source (`dianaBusts`, `blazeBusts`, `seraphBusts`, `lavaBusts`; `SbcsClient.bustStyle`): 0 full, 1
  short - each screen's `shortBust()` (called once the outcome is settled) jumps silently to the reveal - 2 none -
  `bustsOff` stops the kill opening; a win still plays through each listener's resolve fallback. The honest cost, said
  in the menu: a cutscene that keeps going is a win.
- **Minimum worth** (`minWorth`, an index into `PatternConfig.MIN_WORTH`): `underFloor(priceId)` turns a win under it
  into a bust; an unknown price always plays. `floorPrices()` asks for every animated drop's price every 10 min.
- **Pre-kill hides in percent**: `dianaHidePct`, `blazeHidePct`, `seraphHidePct` (against 210M), `lavaHidePct`.
  `PatternConfig.migrateHides` converts the old million fields once (`hidesInPercent`).
- **Speed** per animation (`dianaSpeed`, `blazeSpeed`, `seraphSpeed`, `lavaSpeed`, `fishingSpeed`, `ghostSpeed`,
  50-200, read when the screen is made); the Thaw (`vanguardSpeed`) and the Scatha (`scopeDurationMs`) use their own
  timelines. See `anim/SceneClock` in [rendering.md](rendering.md).
- **Tier** per slayer (`zombieTier`, `spiderTier`, `blazeTier`, `seraphTier`) - see [slayers.md](slayers.md).
- `chatSummary`: each screen's `summary()` on its natural end or a skip (`SbcsClient.summary`), and the Scatha's close.
- `calmEffects`: `ChopScene.calm` zeroes every shake and takes glows to 55%.
- `keepVitals` (default on), `gameplayCorner` (0 off / 1-4 corners), `recordDrops` (default **false**).
- `SbcsClient.resetSettings` resets settings, never stats.

## Config

`config/PatternConfig`, JSON at `config/sbcs.json`; `/sbcs reload` re-reads it. `PatternConfig.sanitize()` clamps
every value to the menu's range on load (keep it in step with new sliders); `fresh()` is the defaults with one-off
migrations marked done; `writeAtomically` is used by config and stats; an unreadable file is moved to `.broken`.

## Stats

`SbcsStats.Source` per key ("blaze", "seraph", "lava", "shark", "zombie", "spider", "water", "dungeon", "nucleus",
"superpairs"...): kills, drops by title, dry streak. Each screen's `count()` calls `onSource` once the outcome is
known (never for tests). Scenes show `nthNote` ("your 3rd") after the odds. Stats count at detection when no screen
will (`countUnplayed`: busts off, switched off, a waited bust dropped) - screens count their own runs, never both.

**Test runs are not counted**: `/sbcs test` plays the real cutscenes but touches no stats. Scatha tests go through
`triggerTest` and a `ScopeMinigameScreen` built with `test = true` (it counts in its constructor, so the flag is a
constructor argument); Vanguard tests go through `injectTest`, which sets `testingCorpse`.

## Mod Menu

`gui/ModMenuIntegration` (the "modmenu" entrypoint, `suggests` in fabric.mod.json) returns `SbcsMenuScreen::new` with
Mod Menu's screen as `parent`; `onClose` goes back to it. Mod Menu's maven is not used:
`src/main/java/com/terraformersmc/modmenu/api/` holds compile-time copies of `ModMenuApi` and `ConfigScreenFactory`,
and `build.gradle` excludes `com/terraformersmc/**` from the jar and the sources jar, so the real ones are always Mod
Menu's own.
