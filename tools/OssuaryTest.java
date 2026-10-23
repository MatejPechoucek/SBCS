/*
 * Offline checks for "The Ossuary" (anim/OssuaryTimeline + render/OssuaryScene): the drum always stops on its niche
 * whatever the pull, the pull never changes the result, "Just watch" plays with no input, the seals, levers and the
 * Livid shell game run by themselves after their waits, the tell is the same every run, and - the honesty rule -
 * every draw call before the reveal is the same for every outcome, on every kind of floor, under several pulls.
 *
 *   javac -proc:none -d out/oss -cp "<the game's classpath>" -sourcepath src/main/java tools/OssuaryTest.java
 *   java -cp "out/oss:<the game's classpath>" OssuaryTest
 *
 * (The scene sits on ChopScene, which reaches the game's classes at load time, as EruptionTest does.)
 */
import com.sbcs.anim.OssuaryTimeline;
import com.sbcs.audio.OssuarySound;
import com.sbcs.audio.Voice;
import com.sbcs.anim.OssuaryTimeline.Band;
import com.sbcs.anim.OssuaryTimeline.Ending;
import com.sbcs.detect.dungeon.*;
import com.sbcs.render.OssuaryScene;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class OssuaryTest {
    static int pass = 0, fail = 0;

    static void check(String what, boolean ok, String detail) {
        System.out.printf("  %s %-64s %s%n", ok ? "PASS" : "FAIL", what, detail);
        if (ok) pass++; else fail++;
    }

    /** A canvas that only hashes what it is asked to draw. */
    static final class Hash implements OssuaryScene.Canvas {
        long h = 1125899906842597L;
        int calls;

        void mix(Object... xs) {
            calls++;
            for (Object x : xs) {
                long v = x instanceof Float f ? Math.round(f * 64) : x == null ? 7 : x.hashCode();
                h = 31 * h + v;
            }
        }

        public void quad(String tex, float ox, float oy, float ux, float uy, float vx, float vy, int u0, int v0, int uw, int vh, int argb) { mix("q", tex, ox, oy, ux, uy, vx, vy, u0, v0, uw, vh, argb); }
        public void sprite(String tex, float cx, float cy, float w, float h, float rot, int argb) { mix("s", tex, cx, cy, w, h, rot, argb); }
        public void line(float x0, float y0, float x1, float y1, float width, int argb) { mix("l", x0, y0, x1, y1, width, argb); }
        public void fill(float x, float y, float w, float h, int argb) { mix("f", x, y, w, h, argb); }
        public void item(float cx, float cy, float size, float alpha, float yaw) { mix("i"); }
        public void name(float cx, float cy, float scale, int argb, int letters) { mix("n"); }
        public void text(String text, float cx, float cy, float scale, int argb) { mix("t", text, cx, cy, scale, argb); }
        public String creatureSkin(com.sbcs.anim.ChopTimeline.Creature c) { return null; }
        public String dropSkin() { return null; }
        public void loot(Loot l, float cx, float cy, float size, float alpha) { mix("loot", l.key(), l.name(), cx, cy, size, alpha); }
        public String headSkin(String key) { return "head_" + key; }
        public void imageAffine(String key, int[] argb, int iw, int ih, float ox, float oy, float ux, float uy, float vx, float vy) {
            // The wheel's face, texel by texel.
            mix("img", key, iw, ih, ox, oy, ux, uy, vx, vy, java.util.Arrays.hashCode(java.util.Arrays.copyOf(argb, iw * ih)));
        }
    }

    static DungeonPool pool;

    static OssuaryTimeline make(DungeonFloor f, ChestTier tier, boolean chain, long seed) {
        var entries = pool.pool(f, tier);
        List<Loot> looks = new ArrayList<>();
        int[] w = new int[entries.size()];
        for (int i = 0; i < w.length; i++) {
            looks.add(entries.get(i).look());
            w[i] = entries.get(i).weight();
        }
        return new OssuaryTimeline(f, tier, f.master(), chain, seed, looks, w);
    }

    /** A chest whose best item is {@code best} (worth {@code worth}); the 2nd-4th are fixed, so the lords match. */
    static ChestValue chest(DungeonFloor f, ChestTier tier, String best, long worth) {
        List<Loot> items = List.of(Loot.named(best, best, 1), Loot.named("RECOMBOBULATOR_3000", "Recombobulator 3000", 1),
                Loot.named("ENCHANTMENT_ULTIMATE_BANK_1", "Bank I", 1), Loot.named("ESSENCE_WITHER", "Wither Essence", 20),
                Loot.named("HOT_POTATO_BOOK", "Hot Potato Book", 1));
        Map<String, Long> p = new HashMap<>(Map.of("RECOMBOBULATOR_3000", 600_000L, "ENCHANTMENT_ULTIMATE_BANK_1", 400_000L,
                "ESSENCE_WITHER", 2_000L, "HOT_POTATO_BOOK", 80_000L));
        p.put(best, worth);
        return ChestValue.of(new RewardChest(f, tier, items, 2_000_000, false, false, -1), p::get);
    }

    /** The outcomes every floor is tried with: a bust, blue, purple, gold, a jackpot, a dye. */
    static List<Object[]> outcomes(DungeonFloor f) {
        List<Object[]> o = new ArrayList<>();
        o.add(new Object[]{"FEL_SKULL", 700_000L});
        o.add(new Object[]{"WITHER_BLOOD", 2_000_000L});
        o.add(new Object[]{"SHADOW_ASSASSIN_HELMET", 9_000_000L});
        o.add(new Object[]{"GIANTS_SWORD", 40_000_000L});
        o.add(new Object[]{"NECRON_HANDLE", 600_000_000L});
        o.add(new Object[]{"DYE_NECRON", 900_000_000L});
        return o;
    }

    /** Plays a script of input: {time, kind, arg}, kinds 0 seal, 1 pull (arg = strength x100, drum in high bits), 2 livid. */
    static void play(OssuaryTimeline tl, long[][] script, long t) {
        for (long[] s : script) {
            if (s[0] > t) break;
        }
    }

    static long run(OssuaryTimeline tl, ChestValue v, long[][] script, long until, int step, List<Long> hashes) {
        tl.resolve(v, 1_000_000);
        int si = 0;
        long reveal = -1;
        for (long t = 0; t <= until; t += step) {
            while (si < script.length && script[si][0] <= t) {
                long[] s = script[si++];
                if (s[1] == 0) tl.clickSeal((int) s[2], s[0]);
                else if (s[1] == 1) tl.pull((int) (s[2] >> 16), (s[2] & 0xFFFF) / 100f, s[0]);
                else if (s[1] == 2) tl.pickLivid((int) s[2], s[0]);
            }
            tl.update(t);
            if (tl.reveal() >= 0 && t >= tl.reveal()) {
                reveal = tl.reveal();
                break;
            }
            // The sounds due since the last frame: what they are, never what they would say.
            List<OssuarySound.Cue> cues = new ArrayList<>();
            OssuarySound.between(tl, t - step, t, cues);
            long sh = 17;
            for (OssuarySound.Cue c : cues) {
                sh = sh * 31 + (c.cut() ? 1 : 2);
                for (Voice vo : c.voices()) sh = sh * 31 + vo.getClass().getName().hashCode();
                sh = sh * 31 + c.voices().length;
            }
            hashes.add(sh);
            Hash h = new Hash();
            OssuaryScene.dragY = 0f;
            OssuaryScene.draw(h, (x, y, w, hh, argb) -> h.mix("px", x, y, w, hh, argb), tl, t, 640, 360);
            hashes.add(h.h);
        }
        return reveal;
    }

    public static void main(String[] args) throws Exception {
        pool = DungeonPool.parse(Files.readString(Path.of("src/main/resources/assets/sbcs/dungeon_pool.json")));

        System.out.println("The stop: always the niche, whatever the pull");
        DungeonFloor f3 = new DungeonFloor(3, false);
        boolean allLand = true, inBounds = true;
        for (int s = 0; s <= 10; s++) {
            OssuaryTimeline tl = make(f3, ChestTier.OBSIDIAN, true, 42);
            tl.resolve(chest(f3, ChestTier.OBSIDIAN, "FEL_SKULL", 1L), 0);
            tl.update(800);
            tl.pull(0, s / 10f, 900);
            var d = tl.drum(0);
            float before = tl.pos(0, d.stopAt - 1);
            allLand &= Math.abs(tl.pos(0, d.stopAt) - d.target) < 1e-4 && before <= d.target + 1e-3;
            long len = d.stopAt - 900;
            inBounds &= len >= 1500 && len <= 6000;
            // Monotonic: it never runs backwards.
            float last = -1e9f;
            for (long t = 900; t <= d.stopAt; t += 7) {
                float p = tl.pos(0, t);
                allLand &= p >= last - 1e-4;
                last = p;
            }
        }
        check("every strength stops exactly on the niche, never backwards", allLand, "");
        check("every spin lasts 1.5 - 6 s", inBounds, "");
        OssuaryTimeline re = make(f3, ChestTier.OBSIDIAN, true, 42);
        re.resolve(chest(f3, ChestTier.OBSIDIAN, "FEL_SKULL", 1L), 0);
        re.update(800);
        re.pull(0, 0.3f, 900);
        re.pull(0, 1f, 1500);
        re.pull(0, 1f, 2100);
        long stop = re.drum(0).stopAt;
        check("re-pulls still land on a niche", Math.abs(re.pos(0, stop) - re.drum(0).target) < 1e-4, "pulls " + re.drum(0).pulls.size());
        re.pull(0, 1f, 2300);
        check("at most " + OssuaryTimeline.MAX_PULLS + " pulls", re.drum(0).pulls.size() == OssuaryTimeline.MAX_PULLS, "");

        System.out.println("Watch mode and the automatic hand");
        OssuaryTimeline w = make(f3, ChestTier.OBSIDIAN, false, 7);
        w.resolve(chest(f3, ChestTier.OBSIDIAN, "FEL_SKULL", 1L), 0);
        for (long t = 0; t < 20_000 && w.reveal() < 0; t += 16) w.update(t);
        check("plays to the reveal with no input", w.reveal() > 0, "reveal " + w.reveal());
        check("the hand pulls straight away", w.drum(0).pulls.get(0)[0] < OssuaryTimeline.ARRIVE + OssuaryTimeline.WATCH_WAIT + OssuaryTimeline.HAND_STROKE + 50, "");
        OssuaryTimeline ch = make(f3, ChestTier.OBSIDIAN, true, 7);
        ch.resolve(chest(f3, ChestTier.OBSIDIAN, "FEL_SKULL", 1L), 0);
        for (long t = 0; t < 20_000 && ch.reveal() < 0; t += 16) ch.update(t);
        check("chain mode: the hand pulls after " + OssuaryTimeline.AUTO_WAIT + " ms untouched", ch.drum(0).pulls.get(0)[0] >= OssuaryTimeline.ARRIVE + OssuaryTimeline.AUTO_WAIT, "");
        check("chain mode reaches the reveal untouched too", ch.reveal() > 0, "");

        System.out.println("Master Mode seals");
        DungeonFloor m7 = new DungeonFloor(7, true), m3 = new DungeonFloor(3, true);
        OssuaryTimeline sm = make(m3, ChestTier.BEDROCK, true, 9);
        check("M3 has three seals", sm.seals() == 3, "");
        sm.resolve(chest(m3, ChestTier.BEDROCK, "FEL_SKULL", 1L), 0);
        sm.clickSeal(1, 900);
        check("the seals go in order? a click on any cracks it", sm.sealAt(1) == 900, "");
        sm.clickSeal(0, 950);
        sm.clickSeal(2, 1000);
        sm.update(1000);
        check("unlocked after the last seal falls", sm.unlockAt() == 1000 + OssuaryTimeline.SEAL_FALL, sm.unlockAt() + "");
        OssuaryTimeline s7 = make(m7, ChestTier.BEDROCK, true, 9);
        check("M7: seven seals, the last takes three clicks", s7.seals() == 7 && s7.hitsFor(6) == 3 && s7.hitsFor(5) == 1, "");
        OssuaryTimeline sw = make(m7, ChestTier.BEDROCK, false, 9);
        sw.resolve(chest(m7, ChestTier.BEDROCK, "FEL_SKULL", 1L), 0);
        for (long t = 0; t < 30_000 && sw.reveal() < 0; t += 16) sw.update(t);
        check("watch mode cracks every seal and throws every lever", sw.reveal() > 0 && sw.unlockAt() > 0, "reveal " + sw.reveal());

        System.out.println("The wheel");
        OssuaryTimeline th = make(new DungeonFloor(7, false), ChestTier.BEDROCK, true, 3);
        th.resolve(chest(th.floor(), ChestTier.BEDROCK, "NECRON_HANDLE", 600_000_000L), 0);
        th.update(800);
        check("one wheel on every floor, F7 too", th.drumCount() == 1, "");
        check("three veiled segments", OssuaryTimeline.VEILS.length == 3, "");
        boolean fixed = true;
        for (long i = 0; i < OssuaryTimeline.SEGMENTS; i++) {
            if (th.fillerAt(0, i) != th.fillerAt(0, i + OssuaryTimeline.SEGMENTS * 5L)) fixed = false;
        }
        check("filler is the same every turn", fixed, "");
        th.pull(0, 0.5f, 900);
        long wheelStop = th.drum(0).stopAt;
        check("the stop is veiled", OssuaryTimeline.veiled(th.drum(0).target), "target " + th.drum(0).target);
        check("it stays veiled until the reveal", th.niche(0, th.drum(0).target, wheelStop) == -2, "");
        for (long t = 900; t < 30_000 && th.reveal() < 0; t += 16) th.update(t);
        check("and shows the best item after it", th.niche(0, th.drum(0).target, th.reveal()) == th.value().best(), "");

        System.out.println("The Livid shell game");
        DungeonFloor f5 = new DungeonFloor(5, false);
        OssuaryTimeline lv = make(f5, ChestTier.BEDROCK, false, 11);
        lv.resolve(chest(f5, ChestTier.BEDROCK, "SHADOW_FURY", 9_000_000L), 0);
        for (long t = 0; t < 20_000 && lv.reveal() < 0; t += 16) lv.update(t);
        check("watch mode picks a Livid by itself", lv.liviPick() >= 0 && lv.reveal() > lv.liviPickAt(), "");

        System.out.println("Bands and endings");
        DungeonFloor f6 = new DungeonFloor(6, false);
        Object[][] want = {{"FEL_SKULL", 700_000L, Band.BUST, Ending.BUST}, {"WITHER_BLOOD", 2_000_000L, Band.BLUE, Ending.WIN},
                {"SHADOW_ASSASSIN_HELMET", 9_000_000L, Band.PURPLE, Ending.WIN}, {"FEL_SKULL", 40_000_000L, Band.GOLD, Ending.WIN},
                {"GIANTS_SWORD", 30_000_000L, Band.JACKPOT, Ending.JACKPOT}, {"DYE_LIVID", 10_000_000L, Band.JACKPOT, Ending.DYE}};
        for (Object[] wv : want) {
            OssuaryTimeline b = make(f6, ChestTier.BEDROCK, false, 1);
            b.resolve(chest(f6, ChestTier.BEDROCK, (String) wv[0], (Long) wv[1]), 1_000_000);
            check(wv[0] + " at " + wv[1] + " -> " + wv[2], b.band() == wv[2] && b.ending() == wv[3], b.band() + " " + b.ending());
        }
        OssuaryTimeline floorBust = make(f6, ChestTier.BEDROCK, false, 1);
        floorBust.resolve(chest(f6, ChestTier.BEDROCK, "WITHER_BLOOD", 3_000_000L), 5_000_000);
        check("under the menu's floor is a bust", floorBust.band() == Band.BUST, "");
        OssuaryTimeline ap = make(m7, ChestTier.BEDROCK, false, 1);
        ap.resolve(chest(m7, ChestTier.BEDROCK, "DARK_CLAYMORE", 200_000_000L), 0);
        check("M7: the Dark Claymore lights the Apex", ap.dragon() == 4, ap.dragon() + "");

        System.out.println("The tell is the same every run");
        OssuaryTimeline a1 = make(f3, ChestTier.OBSIDIAN, false, 1), a2 = make(f3, ChestTier.OBSIDIAN, false, 99);
        a1.resolve(chest(f3, ChestTier.OBSIDIAN, "FEL_SKULL", 1L), 0);
        a2.resolve(chest(f3, ChestTier.OBSIDIAN, "ADAPTIVE_CHESTPLATE", 9_000_000L), 0);
        for (long t = 0; t < 20_000; t += 16) {
            a1.update(t);
            a2.update(t);
        }
        boolean same = true;
        for (long k = 0; k < OssuaryTimeline.TELL_MS; k += 10) same &= a1.tellRgb(a1.tellAt() + k) == a2.tellRgb(a2.tellAt() + k);
        check("the lantern's colours, step for step", same, a1.stepCount() + " steps");
        check("and it lands on the truth", a1.tellRgb(a1.reveal()) == Band.BUST.rgb && a2.tellRgb(a2.reveal()) == OssuaryTimeline.Dealer.PROFESSOR.rgb, "");

        System.out.println("Honesty: every draw call before the reveal, per outcome");
        long[][][] scripts = {
                {},
                {{1200, 1, 30}},
                {{900, 1, 100}, {1500, 1, 100}},
        };
        long[][] sealScript = {{800, 0, 0}, {820, 0, 1}, {840, 0, 2}, {860, 0, 3}, {880, 0, 4}, {900, 0, 5}, {920, 0, 6},
                {940, 0, 6}, {960, 0, 6}, {1400, 1, (2L << 16) | 60}, {1500, 1, 40}, {1550, 1, (1L << 16) | 90}};
        String[] floors = {"F1", "F3", "F5", "M5", "F6", "F7", "M7", "E"};
        for (String fl : floors) {
            DungeonFloor f = DungeonFloor.fromLabel(fl);
            for (ChestTier tier : new ChestTier[]{ChestTier.OBSIDIAN, ChestTier.BEDROCK}) {
                for (int si = 0; si < scripts.length + 1; si++) {
                    long[][] script = si < scripts.length ? scripts[si] : sealScript;
                    for (boolean chain : new boolean[]{true, false}) {
                        List<Long> ref = null;
                        long refReveal = 0;
                        boolean ok = true;
                        String why = "";
                        for (Object[] o : outcomes(f)) {
                            OssuaryTimeline tl = make(f, tier, chain, 1234 + si);
                            List<Long> hs = new ArrayList<>();
                            long rv = run(tl, chest(f, tier, (String) o[0], (Long) o[1]), script, 30_000, 33, hs);
                            if (rv < 0) {
                                ok = false;
                                why = "no reveal";
                                break;
                            }
                            if (ref == null) {
                                ref = hs;
                                refReveal = rv;
                            } else if (rv != refReveal || !hs.equals(ref)) {
                                ok = false;
                                int k = 0;
                                while (k < Math.min(hs.size(), ref.size()) && hs.get(k).equals(ref.get(k))) k++;
                                why = o[0] + " differs at frame " + k + " (reveal " + rv + " vs " + refReveal + ")";
                                break;
                            }
                        }
                        check(fl + " " + tier.title + (chain ? " chain" : " watch") + " script " + si, ok, why);
                    }
                }
            }
        }

        // ---- the sounds after the reveal: cut at it, and every ending has its own
        for (Object[] o : outcomes(DungeonFloor.fromLabel("F7"))) {
            OssuaryTimeline tl = make(DungeonFloor.fromLabel("F7"), ChestTier.BEDROCK, false, 9);
            tl.resolve(chest(DungeonFloor.fromLabel("F7"), ChestTier.BEDROCK, (String) o[0], (Long) o[1]), 1_000_000);
            for (long t = 0; t < 60_000 && tl.reveal() < 0; t += 16) tl.update(t);
            long r = tl.reveal();
            List<OssuarySound.Cue> at = new ArrayList<>(), after = new ArrayList<>();
            OssuarySound.between(tl, r - 1, r, at);
            OssuarySound.between(tl, r, tl.total(), after);
            check("sound: " + o[0] + " cuts at the reveal", !at.isEmpty() && at.get(0).cut(), at.size() + " cues");
            check("sound: " + o[0] + " has an ending", tl.ending() == Ending.BUST || after.size() >= 2, after.size() + " cues after");
            List<OssuarySound.Cue> whole = new ArrayList<>();
            OssuarySound.between(tl, -1, tl.total(), whole);
            check("sound: " + o[0] + " the whole roll sounds", whole.size() > 20, whole.size() + " cues");
        }

        // ---- Try it's chests: the floor's own pool, real-shaped prices, and the band they name where the floor has one
        for (String fl : new String[]{"F1", "F2", "F3", "F4", "F5", "F6", "F7", "M1", "M2", "M3", "M4", "M5", "M6", "M7"}) {
            DungeonFloor f = DungeonFloor.fromLabel(fl);
            ChestTier tier = com.sbcs.anim.OssuaryTests.defaultTier(f);
            var entries = pool.pool(f, tier);
            for (String out : com.sbcs.anim.OssuaryTests.OUTCOMES) {
                // Offline: no market, so every price is the wiki's added cost (or a cheap book's 60k).
                com.sbcs.anim.OssuaryTests.Test tt = com.sbcs.anim.OssuaryTests.make(f, tier, out, pool, k -> null);
                OssuaryTimeline tl = make(f, tier, false, 5);
                tl.resolve(ChestValue.of(tt.chest(), tt.prices()::get), 1_000_000);
                boolean fromPool = true;
                long added = 0;
                for (var l : tt.chest().items()) {
                    if (l.essence() || OssuaryTimeline.DYES.contains(l.key())) continue;
                    var e = entries.stream().filter(x -> x.look().key().equals(l.key())).findFirst();
                    if (e.isEmpty()) fromPool = false;
                    else added += e.get().cost();
                }
                check("try " + fl + " " + out + ": every item is from the floor's " + tier.title + " pool", fromPool, tt.chest().items().toString());
                check("try " + fl + " " + out + ": costs the chest's base plus its items' added costs",
                        tt.chest().cost() == com.sbcs.anim.OssuaryTests.baseCost(f, tier) + added, tt.chest().cost() + "");
                boolean madeUp = false;
                for (var en : tt.prices().entrySet()) {
                    var e = entries.stream().filter(x -> x.look().key().equals(en.getKey())).findFirst();
                    if (e.isPresent() && en.getValue() != (e.get().cost() > 0 ? e.get().cost() : 60_000)) madeUp = true;
                }
                check("try " + fl + " " + out + ": no made-up prices", !madeUp, tt.prices().toString());
                long lo = switch (out) { case "blue" -> 1_000_000L; case "purple" -> 5_000_000L; case "gold" -> 25_000_000L; default -> 0L; };
                long hi = switch (out) { case "blue" -> 5_000_000L; case "purple" -> 25_000_000L; default -> 150_000_000L; };
                var jack = OssuaryTimeline.JACKPOTS.get(Math.min(6, Math.max(1, f.number()) - 1));
                boolean reachable = switch (out) {
                    case "bust" -> true;
                    case "jackpot" -> entries.stream().anyMatch(e -> jack.contains(e.look().key()));
                    case "dye" -> f.master() && (f.number() == 5 || f.number() == 7) || entries.stream().anyMatch(e -> jack.contains(e.look().key()));
                    default -> entries.stream().anyMatch(e -> !jack.contains(e.look().key()) && e.cost() >= lo && e.cost() < hi);
                };
                Band wantBand = switch (out) {
                    case "bust" -> Band.BUST;
                    case "blue" -> Band.BLUE;
                    case "purple" -> Band.PURPLE;
                    case "gold" -> Band.GOLD;
                    default -> Band.JACKPOT;
                };
                boolean dyeOk = !out.equals("dye") || (f.master() && (f.number() == 5 || f.number() == 7)) == (tl.ending() == Ending.DYE);
                String got = tl.band() + " " + tl.ending() + " best " + (tl.value().bestItem() == null ? null : tl.value().bestItem().key());
                if (reachable) check("try " + fl + " " + out + " plays its band", tl.band() == wantBand && dyeOk, got);
                else check("try " + fl + " " + out + " (not on this floor) plays a real item, not a jackpot",
                        tl.value().bestItem() != null && tl.band() != Band.JACKPOT, got);
            }
        }
        // The 180k Recombobulator Matej saw came from the old made-up filler prices: it is priced as itself now.
        com.sbcs.anim.OssuaryTests.Test rec = com.sbcs.anim.OssuaryTests.make(DungeonFloor.fromLabel("F2"), ChestTier.OBSIDIAN,
                "purple", pool, k -> k.equals("RECOMBOBULATOR_3000") ? 6_100_000L : null);
        check("a recomb in a test is priced by the market", rec.chest().items().get(0).key().equals("RECOMBOBULATOR_3000")
                && !rec.prices().containsKey("RECOMBOBULATOR_3000") || rec.prices().get("RECOMBOBULATOR_3000") == 5_000_000L,
                rec.chest().items().get(0).key() + " " + rec.prices());

        System.out.printf("%n%d passed, %d failed%n", pass, fail);
        if (fail > 0) System.exit(1);
    }
}
