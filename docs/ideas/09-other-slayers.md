# 09 - The other slayers

My idea. Hellion (Inferno Demonlord) and the Seraph's Glyph (Voidgloom Seraph) prove the slayer pattern: open pending on
the boss's 0 HP, resolved by the bracketed drop line, a bust at QUEST COMPLETE. Four more slayers have big RNG drops.

## The bosses and their headline drops

From memory - **Theory**, fetch each wiki page and price before planning tiers:

| Slayer | Boss (top tier) | Headline drops |
| --- | --- | --- |
| Zombie | Revenant Horror / Atoned Horror (T5) | Scythe Blade, Warden Heart, Shard of the Shredded, Snake Rune |
| Spider | Tarantula Broodfather (T5) | Digested Mosquito, Tarantula Talisman, Fly Swatter |
| Wolf | Sven Packmaster (T4) | Overflux Capacitor, Red Claw Egg, Grizzly Bait |
| Vampire | Riftstalker Bloodfiend (T5, the Rift) | McGrubber's Burger, Unfanged Vampire Part |

## Detection

- **Proven pattern:** the drop line format (`VERY RARE DROP! (Item) (+N% ✯ Magic Find)`, `HellionDrops.item`), QUEST
  COMPLETE, QUEST FAILED - shared by every slayer.
- **Ownership:** the Seraph has a "Spawned by: <player>" nametag (**Proven** from records); the Demonlord does not
  (**Proven**). Each new boss needs its own records. `DropRecorder.SLAYER` already matches revenant, tarantula, sven,
  riftstalker and their lines - so a friend's session with records on is all that is needed.
- The Seraph's 500k floor (`SeraphDrops.floored`) applies.

## Cutscene concepts (one line each - each needs its own pitch)

- **Revenant, "The Grave":** the boss sinks into a grave, a headstone's inscription cycles, the earth bursts for a win
  (the Warden Heart beating out of the soil).
- **Tarantula, "The Web":** a web spun strand by strand across the screen, a cocoon in the middle swinging; it splits
  for the reveal.
- **Sven, "The Hunt":** wolf howls in a moonlit ring (the moon from Diana's scene), the Overflux Capacitor charges in
  arcs.
- **Riftstalker, "The Bite":** the Rift's purple-green palette, two fangs, a heartbeat.

## Open questions

- Which slayers does Matej or the friend actually do? Detection is only worth writing against real records.
- Tier restriction: top tier only, as Hellion and the Seraph?
