# Rendering

How SBCS draws its cutscenes: the GUI layer, 3D on top of it, generated art, and the cutscene host that puts it
all over the live game.

## GUI drawing

Everything is drawn through `GuiGraphicsExtractor`.

```java
// 10-arg: no tint.
blit(pipeline, id, x, y, u, v, w, h, texW, texH)
// 11-arg: the same, plus an ARGB tint multiplied into the texture. The shorter
// overloads pass -1 (opaque white) for it - confirmed in 26.2's bytecode.
blit(pipeline, id, x, y, u, v, w, h, texW, texH, argb)
```

The tint carries **alpha**, so sprites can fade and be recoloured at draw time. Older code fakes brightness by
varying sprite size (torch glow, embers) or bakes fades into frames (spark sequence). Those still work; new code
should use the tint. A tint only ever multiplies: it can darken or recolour, never brighten (white overlays use
`TEX_WHITE` at partial alpha instead).

`enableScissor/disableScissor`, `centeredText` and `textWithWordWrap` also exist.

**The font draws alpha below 4 as fully opaque.** Any text that fades must stop drawing near zero, or it flashes
back on at the end of the fade.

**The GUI drops a quad wound the wrong way** (a Java2D preview draws it), so any code that emits arbitrary quads must
turn such a quad round, as `ChopScene.emit` and `EruptionScene.drawGpu` do.

## 3D in the GUI layer

`pose()` returns a 2D `Matrix3x2fStack`, but that does not rule out 3D. A 2D affine transform maps a rectangle onto
any parallelogram, and the orthographic projection of a flat quad is a parallelogram, so a 3D quad draws with the
ordinary blit:

```java
pose.pushMatrix();
pose.mul(new Matrix3x2f(ux/uw, uy/uw, vx/vh, vy/vh, ox, oy));
blit(pipeline, id, 0, 0, u0, v0, uw, vh, texW, texH, tint);
pose.popMatrix();
```

`render/model/` is that pipeline: `Mesh` (quads as corner + two edge vectors + normal), `ModelProjection`
(yaw/pitch, drop Z, cel-shade into 6 bands, keep depth), `BoxMesh`, `PlayerHeadMesh`. `render/MeshRenderer` queues
models, sorts every face from every model together (there is no depth buffer in the GUI layer, so one global
back-to-front sort is the only thing keeping a model from turning inside out), then draws.

Conventions that fail silently when wrong:

- **The camera sits at +Z.** Yaw 0 shows a model's +Z faces, so a model's front belongs on +Z, not on -Z as a
  vanilla entity model has it.
- **A mirrored face looks fine.** Get a u-edge direction backwards and the quad still renders, flipped.
  `PlayerHeadMesh`'s UVs were settled with a labelled skin (white pip top-left, black pip bottom-right).
- **Head sides:** the unwrap is `right | front | left | back` wrapped round the head, so the head's own right side
  (the slot left of the front) is on the viewer's left, -X, when the face looks at the camera. Symmetric skins hide a
  swap here; check any head change by rendering an asymmetric skin at several yaws and looking at the corners.
- **A face seen from behind** can only be drawn with its texture flipped (`ModelProjection`'s winding fix).

Outlines live in `MeshRenderer`, not in the art: the silhouette is stamped eight times in black behind the model,
offset in **screen** space. Scaling the model up instead would expand about the origin and give an uneven outline.

### `ChopScene`'s box renderer

Most newer scenes (Diana, Hellion, Seraph, Ossuary, Vault, Superpairs, slayers) draw through `render/ChopScene` on a
`Canvas` whose `quad` takes a texture name. `box()` draws any rotated textured box, front faces only, with an ink pass
for the outline; `boxAxes` takes explicit axes. One drawing routine serves the game screen and the offline preview, so
they cannot differ.

## Painter's sort vs rasterising

A painter's sort by face centre gets long, overlapping models wrong: far parts draw over near ones (the Ender Dragon
looked see-through). **Big models are rasterised on the CPU with a depth buffer and drawn as one image**:

- `render/DragonRaster`, `render/ModelRaster` - pure Java: exact row spans, cutout texels, emissive layers on the
  body's own surface, the outline as the covered area grown by the ink width. `ModelRaster` has 16 texture slots,
  a per-raster pixel `budget`, and `cutBelow` (refuse pixels below a surface, which is what an occluding lava/water
  plane would do).
- `render/RasterImage` uploads it in game (a `DynamicTexture` that only grows); the preview draws it with Java2D.
- Back faces are culled to save work (box faces by normal, rig polygons by the game's own polygon normal) except
  single-sheet parts such as wing membranes, and heads, which vanilla draws without culling.
- Typical scale: 2 image px per GUI px, a budget around 300k-600k px.

Small scenes of plain boxes are still sorted. Anything as big as a creature model: rasterise it.

## Draw-call budget

Every quad is a GUI render state. Thousands of them a frame drops the frame rate in game (a Seraph ending once hit
80,000 a frame). **Count the draw calls of any new ending** with a counting `Canvas` before handing it over;
`tools/FrameBudgetTest` and each scene's own test (`EruptionTest`, `WaterTest`, `PairsTest`...) enforce budgets.
Techniques that keep it down:

- **Gild runs, not texels.** `ChopSpecials.gildFace` works out every texel's colour first and draws runs of the same
  colour along a row and down rows as one quad. This only merges if neighbouring texels share a colour, so **keep gild
  colours flat across a face** (the texture shows through as relief via the alpha byte).
- **`render/PixelLayer`**: any effect made of many grid squares (frost, veins, sparks, rain) is blended into an ARGB
  buffer one texel per grid cell, uploaded once and blitted with the pose scaled by the cell size - nearest sampling
  keeps it crisp. One static layer per purpose. Do not `close()` the old texture on resize:
  `TextureManager.register` already does (`safeClose`), and twice is not safe.
- **Flat planes as one image.** A floor under an orthographic camera is a parallelogram: compose it on the CPU and
  draw it with one affine blit (`Canvas.imageAffine`; Eruption's lava, Water's surface, the Ossuary's wheel face).
- **Plain cubes to the GPU** (`Poly.gpu`), sorted by block, outline stamps first.

## Gild

`ChopScene.Gild` paints overlays texel by texel through the faces themselves: a colour per texel of every face, its
alpha byte how strongly the face's own texture shows through as relief. `RECOLOUR` (alpha 0xFE) maps the texel's
brightness onto the colour instead of multiplying (orange lava multiplied green goes olive). Screen washes with holes
do not work; use gild.

## Style rules

- **Pixel art only for effects.** Smooth shaded spheres and anti-aliased lines look cheap next to pixel-art heads.
  Liquids are pixel drops and squares; veins are pixels painted onto faces; glows are stepped (`ChopArt.glow` is 40
  texels in eight alpha steps, `ring` 40 texels).
- **Never a full-screen white or grey fill.** Over the dimmed game it reads as fog. A flash is a glow where it happens.
- **The 3D scene never fades out as a whole** - with no depth buffer, faded faces show through each other. It shrinks,
  darkens or sinks away instead.
- **Text plates**: every screen's text draws a dark plate (42% of the text's alpha) of the line's width.
- `render/Punch`: every scene's shake, glow and bigger particle bursts scale by `Punch.now` - `BASE` 1.5 until the
  reveal, then a win raises it by price (log scale 500k -> 200M, up to `MAX` 2.4) or band. Busts stay at base.
  "Calm effects" (`ChopScene.calm`) zero the shakes and take glows to 55%.

## Textures from the game

Textures are adopted from the running game (so resource packs apply), never bundled:

- `SbcsClient.readGameTexture` reads a PNG through the resource manager. **An animated block texture cannot be adopted
  by its Identifier** - that gives only the first frame - so fire, lava and magma are read as strips and normalised
  (`HellionArt.fireStrip`, `LavaArt.strip`).
- Hypixel pack icons (`hypixel_skyblock:textures/item/...`) are used only if the resource exists, with a vanilla
  fallback. An animated pack icon (e.g. Divan's Alloy) needs its `.mcmeta` read and one frame drawn at a time.
- SkyBlock heads come from NEU repo blobs bundled in `assets/sbcs/*_heads.json` (`render/HeadSkins`). **For any head
  texture, check the NEU repo first.** `HeadSkins.pixels` fetches the skin PNG from the blob's own URL on a daemon
  thread, caches it in `config/sbcs/skin_cache/`, and backs off two minutes after a failure.
- **On 26.2 coloured items are collections**: no `Items.GREEN_DYE` or `YELLOW_WOOL` - wool is
  `Items.WOOL.pick(DyeColor.X)`, and the copper family are `WeatheringCopperCollection`s.

## Generated art

**Players can replace any of it.** Every generated texture is written to `config/sbcs/textures/` on first use, and
`render/ArtOverrides` prefers an edited file. A SHA-256 manifest (`generated.properties`) tells them apart: a file
whose hash still matches is ours and is replaced when the art changes; one whose hash does not is the player's and is
never touched. To go back to the built-in art **delete the PNG**, not the manifest line (deleting the line marks the
file as the player's forever).

**Runtime (the live path).** `render/art/` draws with `Graphics2D` into a `BufferedImage` when the texture is first
asked for; `TextureBank` uploads it. `/sbcs reload` is the edit loop.

| class | produces |
| --- | --- |
| `CaveArt` | sparkle and vignette (still drawn); backdrop and gem outcrops still generated, unused |
| `PropArt` | the cleaver's skin; crystal, gem-glow and stone, unused |
| `ScathaArt` | placeholders until `ScathaSkins` has captured the real worm skin |
| `ChopArt`, `ThawArt`, `HellionArt`, `LavaArt`... | each scene's sheets |

**Baked PNGs (the older path).** Scripts in `tools/` write into `src/main/resources/`. **Edit the script and re-run
it** rather than touching the PNG, or the next regeneration wipes the change.

| script | produces | still drawn? |
| --- | --- | --- |
| `gen_sparks.py` | `sbcs/spark_0..5.png` | yes, the impact sparks |
| `gen_misc.py` | `spark.png` (+ dormant placeholder art) | yes |
| `gen_icon.py` | `icon.png` | yes - the mod icon |
| `gen_veil.py` | `prekill_veil.png` | no |
| `gen_cave.py` | `cave/cave_wide.png` | no - `CaveArt.backdrop()` replaced it |
| `gen_gems.py` | `cave/gem_*`, `rock_*`, `sparkle.png` | no - `CaveArt` replaced them |
| `gen_cleaver.py` | `sbcs/cleaver_00..13.png` | no - the cleaver is a mesh |

The superseded scripts are kept as the only source of that art; their outputs are removed from resources.

**The icon** (`gen_icon.py`): 32 x 32 pixel art written 8x up - a gold S on a band of film on a dark badge. The menu
draws it at its native 32 GUI pixels (`MenuView.ICON_SIZE`).

`guillotine_frame.png`, `blade.png`, `cave/cave_bg.png` and the `walk/frame_*` set have **no generator** and are still
drawn by the guillotine mode (`AnimationMode.GUILLOTINE`, selectable only from a config file). Do not delete them.
Older unused art is in git history at the `0.1.0-beta` tag.

Values duplicated between a script and Java must move together: `CaveArt.TORCHES` / `SPARKLES` (only if the cave ever
returns), and the cleaver edge anchor `CLEAVER_EDGE_FX/FY` with `CLEAVER_FRAMES` (14), which is still the swing's
timeline - each phase computes an index 0..13 that maps to a blade angle.

Two techniques the painters rely on:

- **Blur alpha only.** Transparent pixels carry RGB 0, so blurring RGBA drags black into every soft edge. Blur the
  mask and re-pair it with a flat colour.
- **A light map.** Multiply atmospheric effects by a blurred field bright only near the light sources, with a steep
  (`**2.4`) falloff. Rim light and haze applied everywhere read as a wash.

Greyscale + tint is why gem crystals are a separate sprite from rock: the tint multiplies the whole texture.

## The cutscene host

Cutscenes are written as `Screen`s but are **never opened**, so the player keeps the mouse and keyboard and can move
while one plays. `render/CutsceneHud.show(screen)` sizes the screen with `Screen.init(w, h)` and calls its
`extractRenderState` every frame; a cutscene removes itself with `CutsceneHud.hide(this)` in `close()`. The profit
window and the menu are real screens (they are interactive).

- **Where it draws.** `mixin/GuiFadeMixin` turns `HudFade` on from just after the frame's `GuiGraphicsExtractor` is
  made until the first read of `Gui.overlay`, and our cutscenes are drawn where that window closes, fade off, after
  `graphics.nextStratum()` (`SbcsClient.drawOwnHud`): after the HUD, before screens. A cutscene that adds no render
  states logs "A cutscene drew nothing" to the DecisionLog once.
- **The whole HUD is hidden, every mod's.** `render/HudFade` + `mixin/GuiRenderStateFadeMixin` scale the alpha of every
  state added while the fade is on (items and picture-in-picture states are dropped past half way; text below alpha 5
  is dropped). `mixin/HudFadeMixin` cancels `Hud.extractRenderState` once `hudAlpha()` reaches 0 (it eases 1 -> 0 over
  0.35 s). This catches mods that draw around the HUD pass, not only `HudElementRegistry` elements.
- **`mixin/HudHiddenMixin`** makes `Hud.isHidden()` answer true while hiding, so mods that skip overlays under F1 (e.g.
  SBO's loot tracker) skip them too. Vanilla callers: toasts, name tags, F3, block outline.
- **Keep vitals** (`keepVitals`): `mixin/HudInvoker` redraws vanilla's `extractHotbarAndDecorations` on a stratum after
  the cutscene.
- **Dropped items** are hidden during slayer cutscenes and their pre-kill hide (`DroppedItemHideMixin`,
  `PickupHideMixin`).
- **The gameplay window**: the game itself live in a corner of every cutscene (`GameplayView`, from SkyMaxx, MIT).
  `mixin/GameRendererMixin` copies the main render target's colour texture just before `GuiRenderer.render()`, only on
  frames after a cutscene asked. **Every cutscene screen must call `GameplayView.draw` last and `release` in
  `close()`.**
- Known gap: with F1 on, HUD elements (ours included) are not drawn.

### Chat during a cutscene is held, not dropped

While chat is covered (a cutscene, a pre-kill hide, the profit window - `SbcsClient.chatHeld()`), new lines are held by
`ChatHold` and put back, oldest first, on the first tick nothing covers chat.

- Held at `ChatComponent.addMessage` (`mixin/ChatComponentMixin`, HEAD, cancellable), **after** every other mod has
  filtered, and replayed through the same private method (`mixin/ChatComponentInvoker`) with source and tag untouched.
  Holding earlier (Fabric's `ALLOW_GAME`/`ALLOW_CHAT`) replays lines other mods hide, because Fabric ANDs listeners'
  votes and nobody at that hook knows the outcome.
- Replayed only once the profit window has closed too - a chat line shows for ten seconds, so replaying when the
  window opens hides them behind it.
- The game log's `[CHAT]` lines are written by `addMessage`: a line in the log reached the chat box.
- `GuiScreenMixin` keeps the chat box itself shut while a cutscene or pre-kill hide is up (its history would give a
  drop away). The pause menu is not blocked (the game opens it on focus loss).
- Our own "[SBCS] ..." lines are skipped in `allowChatLine`.

### Interrupts, skip and speed

- `gui/Cutscene` (Chop, Hellion, Seraph, Eruption, ... screens): `showsWin`, `skip`, `interrupt`. `SbcsClient.makeRoom`
  lets a known win interrupt a bust or a pending kill; anything else waits (`waitForTurn`: a bust is dropped after 5 s,
  a win always waits). Each watcher's `resolve` falls back to `killKnown` when its pending screen is gone. The Thaw and
  the Scatha are never interrupted.
- Skip: `key.sbcs.skip` (Backspace, held 400 ms). A win jumps to its showcase; jumped-over cues run inside
  `SbcsAudio.muted` so flags and counters are still set.
- `anim/SceneClock`: scene time is wall time times the per-animation speed (50-200%); `jump(target)` for skips and short
  busts; `holdAt`/`release` for interactive pauses. Cue lists and resolve deadlines are on scene time.

## Offline previews

Every scene renders offline so changes can be looked at without the game:

- Canvas scenes: `tools/ChopPreview`, `HellionPreview`, `SeraphPreview`, `EruptionPreview`, `WaterPreview`,
  `SlayerPreview`, `OssuaryPreview`, `VaultPreview`, `PairsPreview`. **The preview must render like the game**: laid out
  at 640 x 360 GUI pixels (`-Dw`/`-Dh`), rasterised at 3x (`-Dscale`), lines as hard-edged rectangles, small textures
  nearest-neighbour, a dark stand-in world behind (`-Dbg`). `-Dvanilla` points at the game's textures, `-Dskins` at
  `<ID>_skin.png` files.
- GUI-drawn cutscenes (Fin, Wisp, the Thaw): `tools/GuiPreview` is a stand-in GUI - `J2D` subclasses
  `GuiGraphicsExtractor` (allocated with Unsafe) and draws every call with Java2D. `render/Offline.hooks` is the mod's
  only hook (null in game). `tools/PopupPreview` and `tools/ThawGuiPreview` drive the real classes. Item stacks cannot be
  made offline; ThawGuiPreview allocates them with Unsafe.
- Running Minecraft's own classes offline needs the client jar plus joml, fastutil, guava, gson, datafixerupper, slf4j,
  log4j-api, logging, brigadier, commons-lang3, authlib and every netty jar, on JDK 25. Font needs a stub
  `com.ibm.icu.text.ArabicShapingException`. In Git Bash set `MSYS_NO_PATHCONV=1` or `;` classpaths are mangled.
