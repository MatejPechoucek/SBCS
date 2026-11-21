# SBCS - SkyBlock Cutscenes

Fabric client mod for **Minecraft 26.2** on Hypixel SkyBlock. When a rare drop source resolves - a Scatha kill, a
Vanguard corpse, a Diana creature, a slayer boss, a lava or water sea creature, a dungeon chest, a Crystal Nucleus
bundle, a Superpairs tile - it hides the loot and plays a cutscene whose ending reveals it. The player keeps control
while it plays.

This file is the guide for working on the code. Per-feature reference lives in [`docs/design/`](docs/design/) (index
at the bottom). `README.md` is the player-facing description.

## Building

```bash
./gradlew build          # Windows: .\gradlew.bat build
```

Java 25 toolchain, Fabric Loom. The local build auto-bumps `mod_version` in `gradle.properties` after building
(keeping any `-alpha` / `-beta` suffix); the jar it just built carries the old number. CI never commits the bump.

`useLegacyMixinAp = false` in build.gradle is required: the project has no refmap, and without it mixin targets are
looked up under Mojang names at runtime and crash with "No refMap loaded".

## Verify code before handing it over

The mapped Minecraft jar Loom downloads can be compiled against directly, which catches every API mistake without a
Gradle build:

```bash
# Its class files are Java 25 (major 69). If your javac is older, rewrite the
# version bytes to 65 and it reads fine - compilation only needs the constant
# pool and member descriptors.
python3 - <<'PY'
import zipfile, struct
src = "<gradle-cache>/fabric-loom/minecraftMaven/.../minecraft-merged-deobf-26.2.jar"
zin, zout = zipfile.ZipFile(src), zipfile.ZipFile("/tmp/mc21.jar", "w", zipfile.ZIP_STORED)
for it in zin.infolist():
    d = zin.read(it.filename)
    if it.filename.endswith(".class") and d[:4] == b'\xca\xfe\xba\xbe':
        d = d[:6] + struct.pack(">H", 65) + d[8:]
    zout.writestr(it, d)
zout.close()
PY

javac -proc:none -d /tmp/out -cp /tmp/mc21.jar -sourcepath "src/main/java:/tmp/stubs" \
      $(find src/main/java -name '*.java' ! -path '*mixin*')
```

Fabric, brigadier, authlib, fastutil and gson are not in that jar. Tiny hand-written stubs for the handful of types
used (`ClientModInitializer`, `Event<T>`, `HudElementRegistry`, `LiteralArgumentBuilder`, `GameProfile`, `IntList`,
`JsonObject`...) are enough; they only need the members this mod calls.

**`javap -c` on the same jar settles any "does this method exist / what does this argument mean" question.** Do that
instead of guessing - for every mixin target especially.

## 26.2 is not 26.1

Reference mods (Scatha-Pro and others) often target 26.1, so their signatures cannot be copied verbatim:

| 26.1 | 26.2 |
| --- | --- |
| `ItemInHandRenderer.renderHandsWithItems` | `submitHandsWithItems` |
| `Minecraft.getMainRenderTarget()` | `gameRenderer.mainRenderTarget()` |
| `renderAllFeatures()` | `renderAllFeatures(SubmitNodeStorage)` |
| `renderBuffers().bufferSource().endBatch()` | `renderBuffers().endFrame()` |
| `Minecraft.setScreen` | `Gui.setScreen` (`Minecraft` keeps only `setScreenAndShow`) |

Also on 26.2: `Screen.init(w, h)` is public; `GuiRenderer.render()` takes no argument; coloured items are collections
(`Items.WOOL.pick(DyeColor.X)`, no `Items.YELLOW_WOOL`); `GuiRenderState` keeps overlapping elements in submission
order.

`sbcs.mixins.json` has `"defaultRequire": 1`, so a mixin whose target does not resolve crashes on load rather than
silently never running. Optional hooks say `require = 0` and report through `Safety.hook`.

## Layout

```
com.sbcs
  SbcsClient          entrypoint: events, commands, texture registration, opening and queueing cutscenes
  Safety, Dev, Maintenance   fail-safes, the developer whitelist, kill switches
  anim/               pure timelines per source (ChopTimeline, HellionTimeline, ...); SceneClock; ThawTimeline
  audio/              the synthesiser, Voices, per-source cue lists (HellionSound, SeraphSound, ...)
  config/             PatternConfig (config/sbcs.json), SbcsStats
  detect/             watchers per source + DecisionLog + DropRecorder; vanguard/, nucleus/, dungeon/, superpairs/
  gui/                cutscene screens (ChopScreen, HellionScreen, ThawScreen, ...), ProfitScreen, SbcsMenuScreen
  gui/menu/           MenuView (the whole menu, no Minecraft), LedgerView, StampArt, StampCard, Draw
  ledger/             the Ledger's pure logic; StampTable is generated from docs/ledger_stamps.xlsx
  market/             Bazaar, LivePrice (Coflnet lowest BIN), CorpseValue - async, cached, never waited on
  mixin/              HUD hiding, chat hold, sound muting, render-target copy, menu covers
  render/             scenes (ChopScene, HellionScene, SeraphScene, EruptionScene, ...), rasterisers, PixelLayer,
                      CutsceneHud, HeadSkins, TextureBank, ArtOverrides
  render/art/         Graphics2D painters for runtime-generated textures
  render/model/       Mesh, BoxMesh, PlayerHeadMesh, ModelProjection, the meshes
src/main/resources/assets/sbcs/   bundled NEU head skins (*_heads.json), baked tracks, a few PNGs
tools/                offline tests (*Test), previews (*Preview), soundtracks (*Soundtrack), gen_*.py, fixtures/
docs/design/          per-feature reference (below); docs/ideas/ proposals; docs/modrinth.md release notes
```

## Non-negotiable rules

1. **Honest tells.** A cutscene's tell (the shield colour, the heat, the flare, the beam) never lies. Tension comes from
   an outcome-identical build-up, not from fake signals.
2. **Nothing before the reveal may depend on the outcome.** Every cutscene opens before or without knowing its outcome
   and resolves later by rebuilding its timeline on the same seed - invisible only because nothing before the reveal
   reads the outcome. Each source has a `*Test` that hashes every draw call (and renders all audio) before the reveal
   for every outcome on several seeds and requires them identical. Run it after any change to a scene, timeline or
   cue list. Anything outcome-specific starts at or after the reveal.
3. **Read and draw only.** Never click, send a packet, or act on the player's behalf (opening chests, flipping tiles).
   Covers and overlays only change what is drawn.
4. **Keep `MenuView` free of Minecraft** (and of Fabric beyond config / stats, and of the market classes), so the menu
   renders offline.
5. **Rasterise big models instead of depth-sorting them.** There is no depth buffer in the GUI layer; a painter's sort
   gets long overlapping models wrong. Creature-sized models go through `ModelRaster` / `DragonRaster`.
6. **Count draw calls for every new ending** with a counting Canvas before handing it over; thousands a frame drop the
   game's frame rate. Keep gild colours flat across a face, draw grid effects through a `PixelLayer`, flat planes as
   one image.
7. **Add `DecisionLog` narration with every `detect/` change.** Detection cannot be summoned on demand; a refusal that
   says nothing is indistinguishable from a bug.
8. **Records first.** Write detection against real recorded strings (`DropRecorder`), not guesses. Mark anything
   from the wiki or other mods as unverified until records confirm it. Never infer a Diana outcome by elimination.
9. **Every cutscene screen** calls `GameplayView.draw` last and `release` in `close()`, sets `ChopScene.calm` and
   `Punch` each frame, and counts stats only for real runs (never `/sbcs test`).
10. **Pixel art only for effects; never a full-screen white or grey fill; the 3D never fades as a whole.**
11. **Generated art is edited at its source**: the Java painter in `render/art/` or the `tools/gen_*.py` script, never
    the PNG it writes.
12. **Adopt game textures at runtime, never bundle them.** For any SkyBlock head texture, check the NEU repo first.

## Tests

Tests are plain Java `main`s in `tools/`, printing `N passed, M failed`. Pure ones compile with
`-sourcepath src/main/java:tools` and slf4j on the classpath; those that touch Fabric or game classes compile against
`build/classes/java/main` plus the game's classpath (`./gradlew compileJava` first).

| Area | Run |
| --- | --- |
| Scatha | `PetTierTest`; detection via `/sbcs log` in game |
| Thaw (Vanguard) | `VgTest` (parser), `ThawTest` (timeline) |
| Diana | `ChopTest` |
| Hellion | `HellionTest`, `HellionDetectTest`, `HellionReplay` |
| Seraph | `SeraphTest`, `SeraphDetectTest` |
| Revenant / Tarantula | `SlayerDetectTest`, `FrameBudgetTest` |
| Lava fishing | `EruptionTest`, `LavaDetectTest` |
| Water mythics | `WaterTest`, `WaterDetectTest` |
| Dungeons | `DungeonChestTest`, `OssuaryTest` |
| Crystal Nucleus | `NucleusTest`, `VaultTest` |
| Superpairs | `PairsTest` |
| Ledger | `LedgerTest` |
| Every watcher vs. player chat | `ChatSpoofTest` |
| Fail-safes, locale | `SafetyTest`, `LocaleTest` |

Previews (`*Preview`) render frames offline and soundtracks (`*Soundtrack`) render cue lists to WAV - see
[rendering.md](docs/design/rendering.md#offline-previews).

## Releases

`.github/workflows/release.yml`: a commit to main that changes `mod_version`, a pushed tag `v<mod_version>`, or a manual
run builds the jar and attaches it to a GitHub Release `v<version>`, with notes from that version's `### Changelog`
block in `docs/modrinth.md` (else GitHub's generated notes). Alpha and beta are full releases (a pre-release never
shows in the sidebar). Modrinth publishing is planned; `docs/modrinth.md` holds the future listing's fields.

`docs/history/index.html` is every commit as an interactive page, written by `tools/gen_history.py` from
`docs/history/template.html` (edit the template) and regenerated by `.github/workflows/history.yml` on every push.

## Design reference

| File | Covers |
| --- | --- |
| [scatha.md](docs/design/scatha.md) | the cleaver cutscene, Scatha kill and drop detection, the worm's skins |
| [thaw.md](docs/design/thaw.md) | Vanguard corpses: parser, timeline, ice, the profit window |
| [diana.md](docs/design/diana.md) | the Chopping Block, its endings, Diana detection |
| [hellion.md](docs/design/hellion.md) | the Inferno Demonlord: blaze, pillars, dice, detection |
| [seraph.md](docs/design/seraph.md) | the Voidgloom Seraph: beacon, endings, the dragon, detection |
| [slayers.md](docs/design/slayers.md) | shared slayer rules, the Revenant and Tarantula cutscene |
| [eruption.md](docs/design/eruption.md) | lava fishing: the vent, floor, endings, detection |
| [water.md](docs/design/water.md) | Titanoboa and Wiki Tiki |
| [ossuary.md](docs/design/ossuary.md) | dungeon reward chests: the wheel, pools, menu covers |
| [vault.md](docs/design/vault.md) | the Crystal Nucleus bundle and its in-world cover |
| [superpairs.md](docs/design/superpairs.md) | hidden Superpairs tiles and the hunt |
| [fin-wisp.md](docs/design/fin-wisp.md) | the shark and ghost pop-ups |
| [ledger.md](docs/design/ledger.md) | the stamp book |
| [menu.md](docs/design/menu.md) | the menu, settings, config, stats, Mod Menu |
| [safety.md](docs/design/safety.md) | fail-safes, mixin policy, DecisionLog, drop records |
| [rendering.md](docs/design/rendering.md) | GUI drawing, 3D, rasterising, art, the cutscene host, chat hold, previews |
| [audio.md](docs/design/audio.md) | the synthesiser, cue lists, muting the game |
