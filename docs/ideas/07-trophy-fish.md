# 07 - Diamond trophy fish

My idea. Trophy Fishing on the Crimson Isle has its own rarity ladder - Bronze, Silver, Gold, **Diamond** - for each of
~18 trophy fish. Diamond catches are genuinely rare and are what trophy fishers chase (the Diamond Trophy Hunter
armour), yet nothing animates them.

## Trigger

A Diamond trophy fish catch (optionally Gold too, and a first-ever catch of any species).

## Detection

**Theory.** From memory the catch line is `TROPHY FISH! You caught a Golden Fish DIAMOND.` - tier word at the end,
fish name before it. SkyHanni's trophy fish tracker parses this line - read its pattern (facts only) and confirm from
records. Add "TROPHY FISH" to `DropRecorder.FISHING`. There is no kill - the line is the whole event, so it is a
Fin/Wisp style popup and the outcome is known at once.

## Drop tiers

The tier word is the outcome. Some fish are much rarer than others (Golden Fish, Karate Fish, Vanille, Moldfin,
Soul Fish...) - their exact rates need research; the rarest species at Diamond deserve more.

## Cutscene concept: "Hooked" popup (~2.5 s)

A pixel lava surface along the bottom (Fin's water strip, orange); the fish leaps out in an arc, flips once, and
lands as its item with the tier word stamped in the tier's colour (diamond = aqua with a sparkle sweep). The trophy
fish are vanilla-style fish textures in Hypixel's pack - use the pack icon when present, else a tinted vanilla fish.

## Open questions

- Gold too, or only Diamond? A "first time you caught this fish" moment as well?
- Does Matej trophy fish? (He lava fishes, so likely by accident at least.)
