# Publishing on Modrinth

Everything the Modrinth project page and the first version upload ask for, ready to paste.

## Project

| Field | Value |
| --- | --- |
| Name | SBCS |
| Slug | `sbcs` (or `sbcs-skyblock` if taken) |
| Summary | Cutscenes for Hypixel SkyBlock's rare drops. Your loot stays hidden until the reveal, so every kill is a little gamble. |
| Icon | `src/main/resources/assets/sbcs/icon.png` (256 x 256) |
| Categories | Utility, Decoration |
| Client side | Required |
| Server side | Unsupported |
| License | MIT |
| Source code | https://github.com/MatejPechoucek/SkyBlockCutScenes |
| Issue tracker | https://github.com/MatejPechoucek/SkyBlockCutScenes/issues |

**Description:** paste `README.md`, minus the centred icon block at the top (Modrinth shows the project icon
already, and the image's path only works on GitHub). The "Building from source" section can go too.

**Gallery** (optional, but it is what sells a cutscene mod): a screenshot or GIF per animation, taken with "Try it" in
the menu. The Thaw's Dye, Diana's Relic and Hellion's High Class dice are the showiest.

## Version 0.4.0-alpha (draft)

Draft for the 0.4.0 release, written 2026-10-26. `mod_version` is not changed here: the release build sets it. Other
0.4.0 workstreams add to this list before it ships - check `git log` since 0.3.11-alpha.

| Field | Value |
| --- | --- |
| File | `build/libs/sbcs-0.4.0-alpha.jar` (not the `-sources` jar) |
| Version number | `0.4.0-alpha` |
| Version name | SBCS 0.4.0 (alpha) |
| Release channel | Alpha |
| Loader | Fabric |
| Game version | 26.2 |
| Dependencies | Fabric API - required; Mod Menu - optional |

### Changelog

```markdown
The big one: four new animations, the Seraph back, and a book of stamps.

**New animations**
- **Exhumed** (Revenant Horror) and **The Brood** (Tarantula Broodfather): your good slayer drops each get their own
  ending - a Warden tears out of the ground for its heart, the Matcha Dye floods a stopped world, and more.
- **The Vault** (Crystal Nucleus): the loot bundle is cracked open like a safe. Divan's Alloy melts the door, the Jade
  Dye stops the world. The bundle on the floor is covered until you've seen the cutscene.
- **The Ossuary** (Dungeons F1-F7, M1-M7): a prize wheel rolls before you see a reward chest, in the boss room and
  at Croesus. Spin it yourself or let it spin. Read-only: it never clicks anything for you.

**The Seraph's Glyph is back**
- Out of maintenance, for tier III and IV, with every good drop its own ending (the Summoning Eye's altar, the
  Etherwarp Merger's teleports, the rune, the shard, the idol that turns the Seraph to stone).

**Slayers**
- Each slayer has a "Plays for" setting: Very Rare and up, Crazy Rare and up, or RNGesus drops only.
- Drops are matched to the boss whose quest you are on - a Paragon Shard off a Revenant no longer plays the blaze.
- Hellion: a new stage without the towers (bigger blaze), pillars that explode in turn, and an option to click the
  hits yourself.

**The SBCS Ledger**
- A leather-bound book of ink stamps, a chapter per animation, with secret stamps to find. Your existing stats are
  stamped the first time you open it.

**Everything else**
- Bigger, punchier effects across every cutscene; big drops hit harder the more they are worth.
- Fail-safes: an animation that errors three times switches itself off for the session, a cutscene stuck for 90 s
  is ended, and `/sbcs stop` gives you the screen back at once.
- "Try it" follows your bust setting, so you see what you will actually get.
```

## Version 0.3.3-alpha

| Field | Value |
| --- | --- |
| File | `build/libs/sbcs-0.3.3-alpha.jar` (not the `-sources` jar) |
| Version number | `0.3.3-alpha` |
| Version name | SBCS 0.3.3 (alpha) |
| Release channel | Alpha |
| Loader | Fabric |
| Game version | 26.2 |
| Dependencies | Fabric API - required; Mod Menu - optional |

`.\gradlew.bat build` builds `0.3.3-alpha` and then bumps `gradle.properties` to `0.3.4-alpha` for the next build,
so the jar to upload is the one the build just named, not the number now in the file.

### Changelog

```markdown
The first public release.

- Animated cutscenes for SkyBlock's rare drops: the Scatha, Vanguard corpses, Diana's Mythological Ritual,
  the Inferno Demonlord IV, lava fishing, plus quick pop-ups for the Fishing Festival's Great White Shark and the
  Dwarven Mines' Ghosts.
- The drop stays hidden (chat, HUD, loot sound, floor loot) until the reveal, and nothing before the reveal
  depends on what dropped.
- You keep control while it plays. Chat that arrives in the meantime is held and put back afterwards.
- Settings with `/sbcs` or Mod Menu: per-animation switches, speed, volume, size, how busts play, and a
  "Try it" button for every outcome.
- Stats: dry streaks and drops per source.
- Hold Backspace to skip.

The Voidgloom Seraph's animation is under maintenance and switched off in this release.
```
