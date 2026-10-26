# TODO

## Ledger: icons (2026-10-26)

Every stamp now draws its SkyBlock item where one can be drawn: a bundled NEU head (`head:set:name`) or, new, an icon
from Hypixel's resource pack (`pack:<path>:<fallback>` in `StampArt.ITEMS` - `textures/item/<path>.png` under
`hypixel_skyblock`, its first frame if it is a strip, and the vanilla fallback when the pack is not loaded; `GuiDraw.item`
reads each one's PNG header once). None of the old stand-ins was a head in NEU, so all of them went to the pack:
Worm Membrane, Skeleton Key, Subzero Inverter, Brimstone Handle, Great White Shark Tooth, Sorrow and Plasma.
LedgerTest checks every stamp has an icon, every head is bundled and every fallback is one GuiDraw knows.

Still vanilla, and that is accurate: Wilson's Engineering Plans is plain paper in game (no pack icon - the stamp keeps a
map so it does not look like a missing icon), Shimmering Wool is yellow wool, the Digested Mosquito is rotten flesh.

To check in game: the pack icons show with Hypixel's pack on, and the fallbacks without it.

Also to check in game: the head icons in the book and the pop-up show their skins (Steve for a moment on the very
first open is expected while the game downloads them).
