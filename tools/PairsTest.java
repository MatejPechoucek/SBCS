import com.sbcs.anim.PairsTimeline;
import com.sbcs.anim.PairsTimeline.Box;
import com.sbcs.anim.PairsTimeline.Ending;
import com.sbcs.audio.PairsSound;
import com.sbcs.audio.SynthEngine;
import com.sbcs.audio.Voice;
import com.sbcs.detect.dungeon.SlotView;
import com.sbcs.detect.superpairs.PairsGame;
import com.sbcs.detect.superpairs.PairsReward;
import com.sbcs.render.PairsHunt;
import com.sbcs.render.PairsScene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Offline checks for "The Hunt" (Superpairs). The one that matters: nothing drawn or heard before a box's reveal depends
 * on what is in it - every draw call of every frame is hashed, and every cue rendered, for every reward on several seeds,
 * on its own and as the second box of a game, and they must agree. A box that got away may differ (the player knows it
 * got away); two of those must still agree with each other.
 *
 * Then the reading of tiles (PairsReward), the game's bookkeeping (PairsGame: letters, matches, turns, the menu titles),
 * every ending's beats in order, and the draw-call budget.
 *
 *   javac -d out -cp <out of the mod>:<slf4j>:<gson> tools/PairsTest.java tools/HellionTest.java && java -cp ... PairsTest
 */
public final class PairsTest {
    static int passed, failed;

    static void check(String what, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + what);
        }
    }

    static final String[] WINS = {"fish", "serum", "guardian", "book", "dye"};

    static long frames(PairsTimeline tl, long from, long to) {
        HellionTest.Hash hash = new HellionTest.Hash();
        for (long t = from; t < to; t += 16) PairsScene.draw(hash, hash::fill, tl, t, 640, 360);
        return hash.h;
    }

    static float[] render(List<PairsSound.Cue> cues, long from, long to) {
        int rate = (int) SynthEngine.SAMPLE_RATE;
        float seconds = (to - from) / 1000f;
        float[] mix = new float[(int) (seconds * rate) * 2];
        for (PairsSound.Cue c : cues) {
            if (c.at < from || c.at >= to) continue;
            int at = (int) ((c.at - from) * rate / 1000L) * 2;
            if (c.cut) Arrays.fill(mix, at, mix.length, 0f);
            for (Voice v : c.voices) {
                float[] one = SynthEngine.renderOffline(v, seconds - (c.at - from) / 1000f);
                for (int i = 0; i < one.length && at + i < mix.length; i++) mix[at + i] += one[i];
            }
        }
        return mix;
    }

    static Box box(char mark, String word, boolean matched) {
        return new Box(mark, PairsTimeline.testReward(word), matched);
    }

    /** Counts what goes to the GUI (the pixel layer is one image in game, so its cells are counted apart). */
    static final class Count implements com.sbcs.render.ChopScene.Canvas {
        int calls, cells;

        @Override public void quad(String tex, float ox, float oy, float ux, float uy, float vx, float vy, int u0, int v0, int uw, int vh, int argb) { calls++; }
        @Override public void sprite(String tex, float cx, float cy, float w, float h2, float rot, int argb) { calls++; }
        @Override public void line(float x0, float y0, float x1, float y1, float width, int argb) { calls++; }
        @Override public void fill(float x, float y, float w, float h2, int argb) { calls++; }
        @Override public void item(float cx, float cy, float size, float alpha, float yaw) { calls += 60; }
        @Override public void name(float cx, float cy, float scale, int argb, int letters) { calls += 2; }
        @Override public void text(String text, float cx, float cy, float scale, int argb) { calls += 2; }
        @Override public String creatureSkin(com.sbcs.anim.ChopTimeline.Creature creature) { return null; }
        @Override public String dropSkin() { return null; }
    }

    public static void main(String[] a) {
        long[] seeds = {1, 7, 42, 99_991, 123_456_789};

        // ---- honesty: one box
        for (long seed : seeds) {
            long ref = frames(PairsTimeline.one(PairsTimeline.testReward("fish"), true, seed), 0, PairsTimeline.REVEAL);
            for (String w : WINS) {
                check("before the reveal, " + w + " draws the same as the fish (seed " + seed + ")",
                        frames(PairsTimeline.one(PairsTimeline.testReward(w), true, seed), 0, PairsTimeline.REVEAL) == ref);
            }
            long missed = frames(PairsTimeline.one(PairsTimeline.testReward("book"), false, seed), 0, PairsTimeline.MISSED_REVEAL);
            for (String w : WINS) {
                check("before its lid lifts, a missed " + w + " draws as a missed book (seed " + seed + ")",
                        frames(PairsTimeline.one(PairsTimeline.testReward(w), false, seed), 0, PairsTimeline.MISSED_REVEAL) == missed);
            }
        }
        // ---- honesty: the second box of a game, after a first box that is the same each time
        for (long seed : new long[]{3, 77}) {
            PairsTimeline r = new PairsTimeline(List.of(box('A', "serum", true), box('B', "fish", true)), seed);
            long s1 = r.start(1);
            long ref = frames(r, s1, s1 + PairsTimeline.REVEAL);
            for (String w : WINS) {
                PairsTimeline tl = new PairsTimeline(List.of(box('A', "serum", true), box('B', w, true)), seed);
                check("the second box's build-up does not depend on it: " + w + " (seed " + seed + ")",
                        tl.start(1) == s1 && frames(tl, s1, s1 + PairsTimeline.REVEAL) == ref);
                check("nor does anything of the first box's: " + w + " (seed " + seed + ")",
                        frames(tl, 0, s1) == frames(r, 0, s1));
            }
        }
        long fishAfter = frames(PairsTimeline.one(PairsTimeline.testReward("fish"), true, 7), PairsTimeline.REVEAL, PairsTimeline.REVEAL + 1500);
        for (String w : new String[]{"book", "dye"}) {
            check(w + " draws differently from the fish after the reveal",
                    frames(PairsTimeline.one(PairsTimeline.testReward(w), true, 7), PairsTimeline.REVEAL, PairsTimeline.REVEAL + 1500) != fishAfter);
        }
        // ---- honesty: sound
        for (long seed : new long[]{3, 77}) {
            float[] base = null;
            for (String w : WINS) {
                float[] pcm = render(PairsSound.schedule(PairsTimeline.one(PairsTimeline.testReward(w), true, seed)), 0, PairsTimeline.REVEAL);
                if (base == null) base = pcm;
                else check("before the reveal, " + w + " sounds the same as the fish (seed " + seed + ")", Arrays.equals(base, pcm));
            }
            PairsTimeline r = new PairsTimeline(List.of(box('A', "book", true), box('B', "fish", true)), seed);
            long s1 = r.start(1);
            float[] ref = render(PairsSound.schedule(r), s1, s1 + PairsTimeline.REVEAL);
            for (String w : WINS) {
                PairsTimeline tl = new PairsTimeline(List.of(box('A', "book", true), box('B', w, true)), seed);
                check("the second box sounds the same before its reveal: " + w + " (seed " + seed + ")",
                        Arrays.equals(ref, render(PairsSound.schedule(tl), s1, s1 + PairsTimeline.REVEAL)));
            }
        }

        // ---- the rewards: what each test word is
        check("the fish is rare", PairsTimeline.testReward("fish").tier() == PairsReward.Tier.RARE);
        check("the serum is rare", PairsTimeline.testReward("serum").tier() == PairsReward.Tier.RARE);
        check("a Legendary Guardian is rare", PairsTimeline.testReward("guardian").tier() == PairsReward.Tier.RARE);
        check("a VII book is ultra", PairsTimeline.testReward("book").tier() == PairsReward.Tier.ULTRA);
        check("the dye is the dye", PairsTimeline.testReward("dye").tier() == PairsReward.Tier.DYE);
        // ---- reading tiles
        check("a VI book is not covered", !PairsReward.of("Enchanted Book", "minecraft:enchanted_book", "ENCHANTED_BOOK",
                Map.of("sharpness", 6), null, null, List.of("Sharpness VI")).covered());
        check("a VII book is read from its enchantments", PairsReward.of("Enchanted Book", "minecraft:enchanted_book", "ENCHANTED_BOOK",
                Map.of("ultimate_wise", 7), null, null, List.of()).tier() == PairsReward.Tier.ULTRA);
        check("a VII book is read from its lore", PairsReward.of("Enchanted Book", "minecraft:enchanted_book", null,
                null, null, null, List.of("Growth VII")).tier() == PairsReward.Tier.ULTRA);
        check("a VII book is read from its name", PairsReward.of("Feather Falling VII", "minecraft:enchanted_book", null,
                null, null, null, List.of()).tier() == PairsReward.Tier.ULTRA);
        check("two VII books of one enchantment are one reward", PairsReward.of("Enchanted Book", "minecraft:enchanted_book", "ENCHANTED_BOOK",
                Map.of("sharpness", 7), null, null, List.of()).key().equals(PairsReward.of("Sharpness VII", "minecraft:enchanted_book", null,
                null, null, null, List.of()).key()));
        check("an Epic Guardian is not covered", !PairsReward.of("[Lvl 1] Guardian", "minecraft:player_head", "GUARDIAN;3", null,
                "GUARDIAN", "EPIC", List.of()).covered());
        check("XP bottles are not covered", !PairsReward.of("Titanic Experience Bottle", "minecraft:experience_bottle",
                "TITANIC_EXP_BOTTLE", null, null, null, List.of()).covered());
        check("a face-down tile is nothing", !PairsReward.of("?", "minecraft:gray_stained_glass_pane", null, null, null, null, List.of()).covered());
        check("the dye by name alone", PairsReward.of("Nadeshiko Dye", "minecraft:player_head", null, null, null, null, List.of()).tier()
                == PairsReward.Tier.DYE);
        check("every covered reward has a price id", PairsTimeline.testReward("book").priceId() != null
                && PairsTimeline.testReward("dye").priceId() != null && PairsTimeline.testReward("guardian").priceId() != null);

        // ---- the game
        check("the stake from the title", "Metaphysical".equals(PairsGame.gameStake("Superpairs (Metaphysical)")));
        check("other menus have no stake", PairsGame.gameStake("Experimentation Table") == null
                && PairsGame.gameStake("Chronomatron (Metaphysical)") == null);
        PairsGame g = new PairsGame("Metaphysical", 0);
        SlotView pane = new SlotView(10, "?", "minecraft:gray_stained_glass_pane", null, null, null, null, List.of(), 1, null);
        SlotView dyeA = new SlotView(12, "Nadeshiko Dye", "minecraft:player_head", "DYE_NADESHIKO", null, null, null, List.of(), 1, null);
        SlotView dyeB = new SlotView(30, "Nadeshiko Dye", "minecraft:player_head", "DYE_NADESHIKO", null, null, null, List.of(), 1, null);
        SlotView fish = new SlotView(14, "Experiment the Fish", "minecraft:pufferfish", "EXPERIMENT_THE_FISH", null, null, null, List.of(), 1, null);
        List<PairsGame.Event> ev = g.update(List.of(pane), rewards(pane), false, 0);
        check("a grid of face-down tiles makes no box", ev.isEmpty() && g.boxes().isEmpty());
        ev = g.update(List.of(pane, dyeA), rewards(pane, dyeA), false, 300);
        check("the dye's first tile gets box A", g.boxes().size() == 1 && g.boxes().get(0).mark == 'A'
                && ev.stream().anyMatch(e -> e.kind() == PairsGame.Event.Kind.BOX));
        check("the grid changing is a turn", ev.stream().anyMatch(e -> e.kind() == PairsGame.Event.Kind.TURN) && g.turns() == 1);
        check("a box waits for its pair", g.hunting());
        ev = g.update(List.of(pane, fish), rewards(pane, fish), false, 900);
        check("the fish gets box B", g.boxes().size() == 2 && g.boxes().get(1).mark == 'B');
        check("the dye turning back is not a match", !g.boxes().get(0).matched);
        ev = g.update(List.of(pane), rewards(pane), false, 1000);
        check("two changes 100 ms apart are one turn", g.turns() == 2);
        ev = g.update(List.of(dyeA, dyeB), rewards(dyeA, dyeB), false, 1600);
        check("both tiles up at once is a match", g.boxes().get(0).matched && g.boxes().get(0).slotA == 12 && g.boxes().get(0).slotB == 30
                && ev.stream().anyMatch(e -> e.kind() == PairsGame.Event.Kind.MATCH));
        check("a dye seen again keeps its letter", g.boxes().size() == 2 && g.boxes().get(0).mark == 'A');
        check("the hunt goes on for the fish", g.hunting() && g.turnsHunting() >= 1);
        ev = g.update(List.of(fish), rewards(fish), true, 2000);
        check("a rewards page listing the fish matches it", g.boxes().get(1).matched && !g.hunting());
        check("a lettered box is found again by its reward", g.existing("DYE_NADESHIKO") == g.boxes().get(0));
        PairsGame c = new PairsGame("Grand", 0);
        SlotView clicks = new SlotView(4, "Remaining Clicks", "minecraft:clock", null, null, null, null, List.of("Clicks Remaining: 7"), 1, null);
        c.update(List.of(clicks), rewards(clicks), false, 0);
        check("the clicks left are read", c.clicksLeft() != null && c.clicksLeft() == 7);

        // ---- Matej's real game (2026-10-28, Metaphysical): a Serum turned up, its pair never did
        List<List<SlotView>> real = new ArrayList<>();
        List<Boolean> realRewards = new ArrayList<>();
        try {
            for (String line : java.nio.file.Files.readAllLines(java.nio.file.Path.of("tools/fixtures/superpairs_game.jsonl"))) {
                com.google.gson.JsonObject o = com.google.gson.JsonParser.parseString(line).getAsJsonObject();
                List<SlotView> v = new ArrayList<>();
                for (com.google.gson.JsonElement e : o.getAsJsonArray("items")) {
                    com.google.gson.JsonObject it = e.getAsJsonObject();
                    List<String> lore = new ArrayList<>();
                    for (com.google.gson.JsonElement l : it.getAsJsonArray("lore")) lore.add(l.getAsString());
                    String id = null;
                    if (it.has("data")) {
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("id:\"([A-Z_;0-9]+)\"").matcher(it.get("data").getAsString());
                        if (m.find()) id = m.group(1);
                    }
                    v.add(new SlotView(it.get("slot").getAsInt(), it.get("name").getAsString(), it.get("item").getAsString(),
                            id, null, null, null, lore, 1, null));
                }
                real.add(v);
                realRewards.add(PairsGame.isRewards(o.get("title").getAsString()) && PairsGame.gameStake(o.get("title").getAsString()) == null);
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        for (boolean floored : new boolean[]{false, true}) {
            // Floored: the Serum (~47k) is under the 400k floor and left uncovered; otherwise everything rare is boxed.
            PairsGame r = new PairsGame("Metaphysical", 0).hiding(x -> !floored || !x.key().equals(PairsReward.SERUM_ID));
            String how = floored ? " (Serum under the floor)" : " (every rare tile boxed)";
            boolean matchedBeforeRewards = false, unmatchEvent = false;
            for (int i = 0; i < real.size(); i++) {
                List<SlotView> v = real.get(i);
                for (PairsGame.Event e : r.update(v, rewards(v.toArray(new SlotView[0])), realRewards.get(i), 500L * i)) {
                    if (e.kind() == PairsGame.Event.Kind.UNMATCH) unmatchEvent = true;
                }
                if (!realRewards.get(i)) for (PairsGame.Box b : r.boxes()) matchedBeforeRewards |= b.matched;
            }
            check("the real game has one rare tile, the Serum" + how, r.boxes().size() == 1
                    && r.boxes().get(0).reward.key().equals(PairsReward.SERUM_ID));
            PairsGame.Box serum = r.boxes().get(0);
            check("the board turned over at the end is noticed" + how, r.revealed());
            check("the end's full board is not a match" + how, !matchedBeforeRewards && !unmatchEvent);
            check("the rewards page does not list the Serum, so it got away" + how, !serum.matched);
            check(floored ? "a tile under the floor gets no box or letter" : "a boxed Serum is box A",
                    floored ? !serum.hidden && serum.mark == 0 : serum.hidden && serum.mark == 'A');
            check(floored ? "an unboxed tile is no hunt" : "a boxed tile left unfound is still a hunt", floored != r.hunting());
        }
        PairsGame listed = new PairsGame("Metaphysical", 0);
        SlotView fishTile = new SlotView(20, "Experiment the Fish", "minecraft:pufferfish", "EXPERIMENT_THE_FISH", null, null, null, List.of(), 1, null);
        listed.update(List.of(fishTile), rewards(fishTile), false, 0);
        SlotView page = new SlotView(13, "Superpairs", "minecraft:player_head", null, null, null, null,
                List.of("Experiment over", "", "Rewards:", "+429,000 Enchanting Exp (Pairs)", "+ Experiment the Fish", "", "Click to claim rewards!"), 1, null);
        listed.update(List.of(page), rewards(page), true, 900);
        check("a reward the rewards page lists is won", listed.boxes().get(0).matched);

        // ---- the beats
        check("the build-up takes 3.5-4 s", PairsTimeline.REVEAL >= 3500 && PairsTimeline.REVEAL <= 4000);
        check("the colour steps end before the reveal", PairsTimeline.HANG < PairsTimeline.REVEAL && PairsTimeline.STEPS.length >= 6);
        long prev = -1;
        boolean ordered = true;
        for (long s : PairsTimeline.STEPS) {
            ordered &= s > prev && s >= PairsTimeline.WIND;
            prev = s;
        }
        check("the colour steps are in order, after the wind-up", ordered);
        java.util.Set<Integer> tells = new java.util.HashSet<>();
        for (Ending e : Ending.values()) tells.add(e.tell);
        check("every ending lands on a colour of its own", tells.size() == Ending.values().length);
        for (String w : WINS) {
            Ending e = PairsTimeline.one(PairsTimeline.testReward(w), true, 1).box(0).ending();
            check(w + " shows the drop after the reveal", PairsTimeline.arrive(e) > PairsTimeline.REVEAL
                    && PairsTimeline.itemRise(e, PairsTimeline.REVEAL) == 0f);
            check(w + "'s box lands on its own colour", PairsTimeline.crackRgb(e, PairsTimeline.REVEAL + 1) == e.tell);
            PairsTimeline tl = PairsTimeline.one(PairsTimeline.testReward(w), true, 1);
            check(w + " fades out by its end", tl.master(tl.total() - 1) < 0.02f);
        }
        long rare = PairsTimeline.end(Ending.RARE), ultra = PairsTimeline.end(Ending.ULTRA), dye = PairsTimeline.end(Ending.DYE),
                missed = PairsTimeline.end(Ending.MISSED);
        check("the dye runs longest, then a VII book, then a rare (" + dye + ", " + ultra + ", " + rare + ")", dye > ultra && ultra > rare);
        check("a box that got away is short (" + missed + ")", missed <= 4500);
        check("the dye's clock stops between freeze and thaw", PairsTimeline.phys(Ending.DYE, PairsTimeline.FREEZE + 10)
                == PairsTimeline.phys(Ending.DYE, PairsTimeline.THAW - 10) && PairsTimeline.phys(Ending.DYE, PairsTimeline.REVEAL) == PairsTimeline.REVEAL);
        check("three heartbeats in the grey", PairsTimeline.HEARTS.length == 3 && PairsTimeline.HEARTS[0] > PairsTimeline.FREEZE
                && PairsTimeline.HEARTS[2] < PairsTimeline.THAW);
        check("the book rises, opens, riffles and flashes in order", PairsTimeline.BOOK_RISE < PairsTimeline.BOOK_OPEN
                && PairsTimeline.BOOK_OPEN < PairsTimeline.RIFFLE && PairsTimeline.RIFFLE < PairsTimeline.BOOK_FLASH
                && PairsTimeline.BOOK_FLASH < PairsTimeline.arrive(Ending.ULTRA));
        check("the banner is the book's and the dye's only", PairsTimeline.banner(Ending.ULTRA) && PairsTimeline.banner(Ending.DYE)
                && !PairsTimeline.banner(Ending.RARE) && !PairsTimeline.banner(Ending.MISSED));
        PairsTimeline two = PairsTimeline.test("two", 1);
        check("two boxes play one after the other", two.count() == 2 && two.start(1) == PairsTimeline.end(two.box(0).ending())
                && two.index(two.start(1) + 1) == 1);
        check("every cue is in time order", sorted(PairsSound.schedule(two)));
        check("Try it: the missed box is not a win", !PairsTimeline.testWins("missed") && PairsTimeline.testWins("dye"));

        // ---- the draw-call budget (Matej's frame rate: the Scatha once dropped it with thousands of states a frame)
        for (String w : new String[]{"fish", "book", "dye", "missed", "two"}) {
            PairsTimeline tl = PairsTimeline.test(w, 7);
            int worst = 0;
            long sum = 0, n = 0;
            for (long t = 0; t < tl.total(); t += 16) {
                Count cnt = new Count();
                PairsScene.draw(cnt, (x, y, ww, hh, argb) -> cnt.cells++, tl, t, 960, 540);
                worst = Math.max(worst, cnt.calls);
                sum += cnt.calls;
                n++;
            }
            System.out.println("  " + w + ": draw calls mean " + sum / Math.max(1, n) + ", worst " + worst);
            check(w + " stays under 3,000 GUI calls a frame (worst " + worst + ")", worst < 3000);
        }
        // ---- the hunt's own drawing: a cover and the overlay draw something and stay off the menu
        List<float[]> fills = new ArrayList<>();
        PairsHunt.Paint p = new PairsHunt.Paint() {
            public void fill(float x, float y, float w, float h, int argb) { fills.add(new float[]{x, y, w, h}); }
            public void text(String text, float cx, float cy, float scale, int argb) { }
        };
        PairsHunt.cover(p, 10, 10, 'A', 1000, 5000);
        check("a cover covers its whole tile", fills.stream().anyMatch(f -> f[0] <= 10 && f[1] <= 10 && f[0] + f[2] >= 26 && f[1] + f[3] >= 26));
        fills.clear();
        PairsHunt.overlay(p, 640, 360, 232, 90, 176, 180, true, 6, 5, 12, List.of(), 3000);
        check("the hunt's embers never land on the menu", !fills.isEmpty() && fills.stream().noneMatch(f ->
                f[0] + f[2] > 232 && f[0] < 408 && f[1] + f[3] > 90 && f[1] < 270));

        // The rare-drop panel: drawn beside the menu, never over it, and never names what is under a box.
        fills.clear();
        List<String> texts = new ArrayList<>();
        PairsHunt.Paint q = new PairsHunt.Paint() {
            public void fill(float x, float y, float w, float h, int argb) { fills.add(new float[]{x, y, w, h}); }
            public void text(String text, float cx, float cy, float scale, int argb) { texts.add(text); }
        };
        for (int sw : new int[]{640, 480}) {
            fills.clear();
            texts.clear();
            float mx = (sw - 176) / 2f;
            PairsHunt.panel(q, sw, 240, mx, 30, 176, 180, List.of(new PairsHunt.Line('A', null, 0xFF55FF, -1, false, false),
                    new PairsHunt.Line((char) 0, "Metaphysical Serum", 0xAA00AA, 47_047, false, true)), 0, 500);
            check("the panel draws at " + sw + " wide", !fills.isEmpty() && texts.contains("RARE DROP!"));
            check("the panel stays off the menu at " + sw + " wide", fills.stream().noneMatch(f ->
                    f[0] + f[2] > mx && f[0] < mx + 176 && f[1] + f[3] > 30 && f[1] < 210));
            check("the panel stays on screen at " + sw + " wide", fills.stream().allMatch(f -> f[0] >= 0 && f[0] + f[2] <= sw));
            check("the panel names the uncovered Serum and its price", texts.contains("Metaphysical Serum") && texts.contains("47k - Got away"));
            check("the panel never names a box's reward", texts.stream().noneMatch(t -> t.contains("Dye")));
        }

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    static boolean sorted(List<PairsSound.Cue> cues) {
        for (int i = 1; i < cues.size(); i++) if (cues.get(i).at < cues.get(i - 1).at) return false;
        return true;
    }

    static Map<Integer, PairsReward> rewards(SlotView... slots) {
        Map<Integer, PairsReward> m = new HashMap<>();
        for (SlotView s : slots) m.put(s.slot(), PairsReward.of(s.name(), s.item(), s.sbId(), s.enchants(), s.petType(), s.petTier(), s.lore()));
        return m;
    }
}
