# Safety, logging and records

## Fail-safes - `com.sbcs.Safety`

- **Errors**: every animation draw (`drawOwnHud`), watcher tick and watcher chat call runs in `Safety.run(key, ...)`
  (keys are the stats' source keys plus "ledger"). A throw goes to `Safety.failed`: logged, `stopAll(false)`, one chat
  line; after `STRIKES` (3) `Safety.on(key)` is false for the session - its watcher stops and `mayOpen` refuses it.
- **`SbcsClient.stopAll(byPlayer)`**: interrupts the newer cutscene, closes the Scatha / Thaw screens (an unresolved
  Scatha goes to `skippedScope`, like a skip), stops the guillotine, Fin, Wisp and the stamp pop-up, silences the
  synth, `CutsceneHud.clear()`, nulls every `current*`, clears `waiting`, and holds the pre-kill hide off
  (`hideSuppressedUntil`: 15 s for `/sbcs stop`, 5 s otherwise). Each step in its own try.
- **`watchdog()`** every tick: the same thing up for `WATCHDOG_MS` 90 s (per screen) is stopped; the pre-kill hide on
  with nothing playing for `HIDE_STUCK_MS` 45 s is held off 60 s.
- **Hypixel only**: `Safety.onHypixel()` (`getCurrentServer().ip` contains hypixel.net; a development run always
  passes) gates every watcher tick and chat line. Tests and the menu are unaffected.
- **Bursts**: `Safety.allowOpen` - a fourth real open of one source in 10 s is refused. `SbcsStats.refused` drops the
  same (source, drop) win within 2 s (never for corpses) and anything past 20 counted events a second.
- **Slow frames**: `CutsceneHud.draw` is timed; averaging over 50 ms for a second sets `Safety.slow()` until the next
  cutscene, which `ChopScene.calm` and `GameplayView.corner()` read.
- **Optional hooks**: `DroppedItemHideMixin`, `PickupHideMixin`, `HudHiddenMixin` and the Ossuary / Superpairs mixins
  are `require = 0` and call `Safety.hook(i)` (one array read on the hot path); `/sbcs log` ends with which ran, what
  is off, and whether we are off Hypixel.
- **Stats backups**: `load()` copies `stats.json.YYYY-MM-DD.bak` once a day (newest `BACKUPS` 3 kept) and
  `stats.json.pre-ledger` once.
- **Chat spoofing**: only game lines are read, never player chat. `detect/ChatLines` recognises a player's line
  (channel prefixes "Party > ", "Guild > ", "Co-op > ", "From ", "To ", bracketed tags, emblems, the name). Our own
  "[SBCS] ..." lines are skipped. `tools/ChatSpoofTest` checks every watcher against spoofed lines.

## Mixins

`sbcs.mixins.json` has `"defaultRequire": 1`, so a mixin whose target name does not resolve **crashes on load** instead
of silently never running (a broken mixin once went unnoticed for a long time that way). Check every target with
`javap` before writing a mixin. Hooks that are allowed to be missing say so with `require = 0` and report through
`Safety.hook`.

`useLegacyMixinAp = false` in build.gradle is required: the project produces no refmap, so without it mixin targets are
looked up under Mojang names at runtime and crash with "No refMap loaded".

## Developer commands

`Dev.allowed()`: the UUIDs in `Dev.WHITELIST`, or a development run. `/sbcs test` and `/sbcs debug` carry
`.requires(src -> Dev.allowed())`, so for anyone else they do not exist. Everything a player needs is in the menu.

## Maintenance switches

`Maintenance` (pure) holds per-feature kill switches, e.g. `Maintenance.SERAPH`: while true, `SbcsClient.seraphOn()`
is false whatever the config says, every Seraph path goes through it, "Try it" only plays for `Dev.allowed()`, and the
menu draws a `TapedRow` ("UNDER MAINTENANCE") in place of its card. Turning one back on also needs the README's table
and fabric.mod.json's description updated.

## The decision log

Detection cannot be summoned on demand (a pet is ~1 in 2000, a kill means going and finding one), so every detector
narrates itself into `DecisionLog`: every kill accepted, every kill refused *and why*, every item matched or ignored,
every outcome inferred. Read it back with `/sbcs log`. `DecisionLog` logs through its own slf4j logger (and a `tap`), so
it works offline.

**When changing anything in `detect/`, add the narration at the same time.** A refusal that says nothing is
indistinguishable from a bug.

## Drop records

`detect/DropRecorder` appends real events to `config/sbcs-drops.jsonl`, one JSON object per line, append-only, written
on its own thread, never for tests. Off by default (`recordDrops`; menu "Drop records", `/sbcs records`). Kinds:

- `scatha_kill` (verdict, the last 15 s of chat, the last 25 decisions), `vanguard_corpse` (the haul, 10 s of chat).
- Capture-only lines, matched by broad keyword patterns **before** any detection - because detection must be written
  against real strings: `diana_line`, `fishing_line`, `slayer_line`, `ghost_line`, `dungeon_line`.
- Nametags: `diana_mob`, `slayer_mob` / `water_mob` / `water_model` (`detect/SlayerRecorder`), `fishing_mob`.
- Menus: `dungeon_chest`, `dungeon_sidebar`, `superpairs_menu`, `head_capture`.

Every incoming line passes through `DropRecorder.chatLine` in `allowChatLine` before anything hides it.

**Records first.** A new source's strings are captured and checked before a parser is trusted; strings taken from the
wiki or another mod are marked as unverified in that source's doc until records confirm them. Fixtures from real
records live in `tools/fixtures/`, with other players' names replaced by placeholders.
