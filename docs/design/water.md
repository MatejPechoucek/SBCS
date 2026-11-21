# The Coil and the Totem - water mythics (Titanoboa, Wiki Tiki)

Unverified in game. **Detection is from the wiki and theory: no real line or nametag has been seen.** DropRecorder keeps
their lines as `fishing_line` and SlayerRecorder their nametags as `water_mob` (plus `water_model`, the armour stands
near one); check the first records and fix `WaterDrops` against them. Background: `docs/ideas/02-wiki-tiki.md`,
`docs/ideas/03-titanoboa.md`.

## Detection

- `detect/WaterDrops` (pure): a creature table (`ROWS`: name + its catch line's words), drop lines in the lava format
  and the slayer's bracketed one, the dye by any drop line or a WOW! naming the player. The Laser Totems are never a
  creature.
- `detect/FishingWatcher` is LavaWatcher's rules over that table: ours only from a catch line (or Feesh's rewrite),
  someone else's only when its own win reaches us by loot share; HP 0 or a close vanish at <= 5% is the death; a win
  within 3 s resolves it, else a bust. Every decision narrated as "Water:".

## The cutscene

- `anim/WaterTimeline` (pure): `Creature` (with its guaranteed drop, shown under a bust), `Drop` (level 0-4 is the
  tell), `Ending`. The tell is a five-colour scale per creature (bust first, aquamarine last) running down the snake's
  body / lighting the totem's faces and lasers; `REVEAL` 4800.
- Endings: BOOK (Scriptures), SHED (slides out of its skin), EYES (Snake Eyes: the coil goes dark, two eyes open),
  BUBBLE (the lasers meet in the top face's mouth), MASK (the top face peels off and turns), FLOOD - the Aquamarine Dye
  for both, "The Bayou Floods": time stops (`phys`), the world greys, three heartbeats, aquamarine climbs the creature,
  the snake rears into a column / the totem drowns under a geyser.
- `render/WaterScene`: Eruption's architecture. The water is one CPU image (`floor`, a texel per unit, the tell's
  colour reflected, foam where the body crosses the surface) and one affine blit; the creatures are boxes rasterised by
  `ModelRaster` with `cutBelow` 0 (`heights` keeps each pixel's world y for the flood line).
- **The creature is built before the floor.** Built after, the foam read the last frame's crossings and a run's first
  frame showed the previous run's. Its back-layer squares are held (`HELD`) and drawn after the floor.
- `audio/WaterSound` (existing voices only), `gui/WaterScreen` (EruptionScreen's shape; heads from `HeadSkins.WATER`,
  `water_heads.json`, NEU bestiary + items).
- Config `water*`, menu card "Water Mythics", stats key "water", `/sbcs test water <drop> [creature]`, Try it rows
  `CREATURE:DROP`.

## Tests and tools

- `tools/WaterTest`: frame and sound honesty per creature on four seeds, levels, the dye's clock, beats, no clipping,
  draw calls (worst ~67 a frame).
- `tools/WaterDetectTest`.
- `tools/WaterPreview` (`-Dskins` for the real skins; without them it paints stand-ins), `tools/WaterSoundtrack`.
