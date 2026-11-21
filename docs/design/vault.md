# The Vault - the Crystal Nucleus loot bundle

Placing the fifth crystal at the Crystal Nucleus cracks open a safe. Unverified in game. A black box covers the bundle
where it lands so the floor does not give the haul away. `anim/VaultTimeline`, `render/VaultScene`,
`audio/VaultSound`, `gui/VaultScreen`.

## Detection - `detect/nucleus/`

Pure apart from `BundleSpotter`. The real message (`tools/fixtures/nucleus_hauls.txt`) is printed whole at the fifth
crystal: a rule, `CRYSTAL NUCLEUS LOOT BUNDLE`, `REWARDS`, item lines with counts folded (`Flawed Topaz Gemstone x86`),
SkyHanni's profit line *inside* it, a rule, `Pick it up near the Nucleus Vault!`.

- Every bundle carries one Fine gem of each of seven colours (Jasper too), so a Fine Jasper is a win only as a second
  one (`NucleusHaul.tierOf`).
- Weights sum to 307,212 (the wiki prints 311,212 - a typo); the Jade Dye is a separate 1 in 500,000.

## The cover

`BundleSpotter.BOX` is a fixed 10 x 3 x 10 box (x 508..518, y 104.5..107.5, z 546..556) round where the bundle lands
(a cluster of invisible stands at x 510.5..514.5, y 105.5, z 548.5..553.5). It is drawn as is, from the loot message
until the Vault is done - `announceNucleus`, which every end of a real bundle's wait goes through, disarms it. Leaving
the Nucleus and 5 minutes are fallbacks. With the animation off it is never shown. Following entities was tried and
picked the wrong one twice, so nothing is followed. `render/BundleCover` draws it on
`LevelRenderEvents.COLLECT_SUBMITS` (`RenderTypes.debugQuads()`, camera-relative). Config `nucleusCover`.

## Chat

The loot lines are hidden while `nucleusAnimation` is on; `chatHeld()` includes `nucleusWatcher.parsing()`, so
SkyHanni's profit line is held with everything else. `announceNucleus` puts one summary line back once the Vault
closes, or at once when nothing plays.

## The cutscene

A steel safe (the game's `iron_block`, `vault_bottom` door face, `vault_top` dial, `polished_blackstone` recess,
adopted at runtime), five sockets round the dial (`SOCKET_R` 6, brass rims), five brass bolts across the door's edge
(`BOLT_X`, `BOLT_HX`, `BOLT_TRAVEL`). The five crystals fly in from the front (`flightPos`) with a trail and click in
(`SOCKETS`), each pulling a bolt, drawn after everything on the door. `VaultScene.clearance()` keeps dial and bolts
off the gems. The dial spins through their colours (`STEPS`, ratio 0.82) while the door strains; at `REVEAL` 4800 it
lands on the tell:

- **grey - bust**: by seed (`BustStyle`) creaks a crack, swings open on nothing, or jams; dust; "No rare roll" and the
  dry streak.
- **sapphire - rare** (`OPEN`): unlatches with light round the edge (`seam`), swings or slams wide (`OpenStyle`) at
  `SWING`.
- **amethyst - Flawless gem** (`BLOWN`): three knocks from inside (`KNOCKS`, `Voices.vaultKnock`), blown off its hinges
  at `BLOW`.
- **gold - Divan's Alloy** (`ALLOY`, "the Meltdown"): each crystal pours a beam into the dial (`POURS`, every 420 ms),
  the door heats dull red -> orange -> white gold (a `TEX_WHITE` overlay at partial alpha - a tint only darkens),
  trembles white hot, slumps into a molten pool (`MELT`, `POOLED`), the alloy rises.
- **jade - the Dye** (`DYE`): the world stops and greys, four heartbeats, silence (`THAW`), jade floods out.

The Alloy and the Dye are the best drops and get the most climactic endings, so the Alloy outranks a Flawless gem in
the same haul (`Prize.of`). Every win holds `HOLD` 450 ms after the reveal before anything moves. The safe darkens
(`shade`) and never fades behind the showcase.

**Drop art**: the Dye and the Quick Claw are NEU heads (`HeadSkins.NUCLEUS`, `nucleus_heads.json`); gems and the alloy
are Hypixel pack icons when loaded, else tinted quartz / a gold ingot. **The alloy's icon is an animated strip** (16 x
64, four frames, `.mcmeta` frametime 6, interpolated, a frame list): `VaultScreen.strip` reads the PNG's size and the
`.mcmeta` and draws one frame at a time, the next fading in.

Never call anything "Butcher" (the mod's former name); the mod is SBCS.

## Tests and tools

- `tools/NucleusTest`: the parser and haul tiers.
- `tools/VaultTest`: frame and sound honesty for every prize on five seeds, the tells, clocks, the Meltdown's order,
  clearance.
- `tools/VaultPreview` (`-Dvanilla` a folder of block textures plus quartz and gold_ingot).
- Menu card "Crystal Nucleus - The Vault", stats key "nucleus", `/sbcs test nucleus <word>`.
