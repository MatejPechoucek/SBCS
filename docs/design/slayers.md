# Slayers - Exhumed (Revenant) and The Brood (Tarantula)

The Revenant Horror and Tarantula Broodfather have their own cutscene, for good drops only, each drop's ending about
its item (gore is fine). Hellion and the Seraph have their own files ([hellion.md](hellion.md),
[seraph.md](seraph.md)). Unverified in game.

## Shared slayer rules

- **Which boss a drop belongs to** is `detect/SlayerQuest` (`SbcsClient.slayerQuest`, shared by every slayer watcher):
  the "SLAYER BOSS! The X spawned!" line and the sidebar's quest. A drop line during another boss's quest is refused -
  the Paragon Shard drops from every slayer, so it needs the quest, otherwise a Revenant's Paragon Shard played Hellion.
- **The tier setting**: `zombieTier`, `spiderTier`, `blazeTier`, `seraphTier` (0 Very Rare+, 1 Crazy Rare+, 2 Pray to
  RNGesus only), checked by `SbcsClient.belowTier(source, word)` against the line's own tier word. A Revenant's Paragon
  Shard is plain `RARE DROP! (Paragon Shard)`, so the default does not play it.
- **The drop line opens it** (`detect/SlayerDropWatcher`): a bracketed slayer drop is only ever the player's own, so
  these open with the outcome known - no kill, no tell. "SLAYER QUEST COMPLETE!" with no good drop is a dry kill for
  the stats. The dyes' "WOW! <player> found a ..." line is checked against `self` (the player's name).
- Slayer cutscenes and their pre-kill hide hide dropped items (`hideDroppedItems()`), since slayer drops land on the
  floor.

## The cutscene

- `anim/SlayerTimeline` (pure: `Source`, `Tier`, `Ending`, 25 `Drop`s with odds from the wiki's weights).
- `render/SlayerScene`: the boss falls to three blows, then `SlayerEndings` (the Revenant's) or `BroodEndings` (the
  Tarantula's) play the drop's ending, then the showcase.
- `render/SlayerRigs`: the game's own Zombie with diamond armour, Spider, Cave Spider, Warden (its emerge / roar
  animations and heart layer), Bee, Silverfish - posed by their own code and rasterised with a depth buffer.
- Heads are NEU's, in `slayer_heads.json`.
- Revenant endings: Warden Heart (a Warden digs up, roars, its heart is torn out), Matcha Dye (the world stops grey,
  one drip, the matcha floods and paints the corpse), Beheaded Horror, Severed Hand (the arm crawls off), Festering
  Maggot, Snake Rune, Scythe Blade, Shredded Sinew, Paragon, the catalysts and Smite.
- `audio/SlayerSound`, `gui/SlayerScreen`.

## CPU cost

Draw calls are small (worst ~450 a frame); the cost is the raster - big bosses, overlapping faces, 20-28 ms a frame in
the dyes at a 960 x 540 GUI before tuning. `ModelRaster.budget` is per raster: SlayerScene sets 300k, and `begin`
never takes the image below 1 px per GUI pixel. Worst is now ~10-13 ms (~20 ms in the Digested Mosquito, two rasters).
A specialised plain-face span loop gained nothing - the cost is overdraw, not branches.

## Tests and tools

- `tools/SlayerDetectTest` replays real lines (`tools/fixtures/slayer_lines.txt`: 140 Revenant, 100 Tarantula, 17
  Seraph bosses).
- `tools/FrameBudgetTest` counts draw calls.
- `tools/SlayerPreview` (`-Dat=` or every frame at `-Dfps`), `tools/SlayerSoundtrack`.
