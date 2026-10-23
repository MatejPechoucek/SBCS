import com.sbcs.anim.ChopTimeline;
import com.sbcs.anim.HellionTimeline;
import com.sbcs.anim.SeraphTimeline;
import com.sbcs.anim.VaultTimeline;
import com.sbcs.render.ChopScene;
import com.sbcs.render.HellionScene;
import com.sbcs.render.SeraphScene;
import com.sbcs.render.VaultScene;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * How much each cutscene asks of the GUI, frame by frame, for every ending: the GUI calls a frame (every quad,
 * sprite, line, fill, item and text is one render state in game) and the CPU time the scene code takes. The
 * Seraph's Dragon once reached 80,000 calls a frame and "killed frames just like the scatha did" (CLAUDE.md, 0.2.67);
 * since then each new ending was counted by hand once. This counts them all, every run, and fails on any frame over
 * {@link #CALL_BUDGET}. Pixel-layer squares are counted apart: they go into one image, not one call each.
 * (Eruption has its own count in EruptionTest.)
 *
 *   javac -d out -cp <fabric+mc classpath> -sourcepath "src/main/java:tools" tools/FrameBudgetTest.java
 */
public final class FrameBudgetTest {
    static int passed, failed;

    static void check(String what, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + what);
        }
    }

    /** The most GUI calls any frame may make. The worst measured before this test (the Seraph, 0.2.67) was ~5,100. */
    static final int CALL_BUDGET = 6_000;
    static final int STEP_MS = 16;

    /** Counts calls; draws nothing. */
    static final class Counter implements ChopScene.Canvas {
        int calls, layer;

        public void quad(String tex, float ox, float oy, float ux, float uy, float vx, float vy, int u0, int v0, int uw, int vh, int argb) { calls++; }
        public void sprite(String tex, float cx, float cy, float w, float h, float rot, int argb) { calls++; }
        public void line(float x0, float y0, float x1, float y1, float width, int argb) { calls++; }
        public void fill(float x, float y, float w, float h, int argb) { calls++; }
        public void item(float cx, float cy, float size, float alpha, float yaw) { calls++; }
        public void name(float cx, float cy, float scale, int argb, int letters) { calls++; }
        public void text(String text, float cx, float cy, float scale, int argb) { calls++; }
        public String creatureSkin(ChopTimeline.Creature creature) { return "skin"; }
        public String dropSkin() { return "drop"; }
        public String bestiarySkin(String key) { return "bestiary"; }
        public void image(String key, int[] argb, int iw, int ih, float x, float y, float w, float h) { calls++; }
        public void imageAffine(String key, int[] argb, int iw, int ih, float ox, float oy, float ux, float uy, float vx, float vy) { calls++; }
        void layer(float x, float y, float w, float h, int argb) { layer++; }
    }

    interface Frame {
        void draw(Counter c, long t);
    }

    record Result(String name, int frames, double meanCalls, int worstCalls, long worstAt, double meanMs, double worstMs, int worstLayer) {}

    static final List<Result> results = new ArrayList<>();

    static void measure(String name, long total, Frame f) {
        // Once untimed so the JIT has seen the code, then for real.
        for (long t = 0; t <= total; t += STEP_MS * 4) f.draw(new Counter(), t);
        int frames = 0, worst = 0, worstLayer = 0;
        long worstAt = 0, sumCalls = 0;
        double sumMs = 0, worstMs = 0;
        for (long t = 0; t <= total; t += STEP_MS) {
            Counter c = new Counter();
            long t0 = System.nanoTime();
            f.draw(c, t);
            double ms = (System.nanoTime() - t0) / 1e6;
            frames++;
            sumCalls += c.calls;
            sumMs += ms;
            worstMs = Math.max(worstMs, ms);
            worstLayer = Math.max(worstLayer, c.layer);
            if (c.calls > worst) {
                worst = c.calls;
                worstAt = t;
            }
        }
        Result r = new Result(name, frames, sumCalls / (double) frames, worst, worstAt, sumMs / frames, worstMs, worstLayer);
        results.add(r);
        check(name + ": worst frame " + worst + " GUI calls at " + worstAt + " ms (budget " + CALL_BUDGET + ")", worst <= CALL_BUDGET);
    }

    public static void main(String[] a) {
        for (ChopTimeline.Verdict v : ChopTimeline.Verdict.values()) {
            ChopTimeline.Creature cr = ChopTimeline.Creature.of(v, 1);
            ChopTimeline tl = new ChopTimeline(v, cr, 1);
            measure("Diana " + v, tl.total(), (c, t) -> ChopScene.draw(c, tl, t, 640, 360));
        }
        for (HellionTimeline.Drop d : HellionTimeline.Drop.values()) {
            HellionTimeline tl = new HellionTimeline(d, 1);
            measure("Hellion " + d, tl.total(), (c, t) -> HellionScene.draw(c, c::layer, tl, t, 640, 360));
        }
        for (SeraphTimeline.Drop d : SeraphTimeline.Drop.values()) {
            SeraphTimeline tl = new SeraphTimeline(d, 1);
            // The Dragon's paint pool goes on the screen's GROUND layer in game; without one the scene falls back to a
            // fill per cell (the preview's way), 36,000 of them.
            measure("Seraph " + d, tl.total(), (c, t) -> {
                SeraphScene.under = c::layer;
                SeraphScene.draw(c, c::layer, tl, t, 640, 360);
                SeraphScene.under = null;
            });
        }
        List<String> words = new ArrayList<>(VaultTimeline.Prize.testWords());
        for (String w : words) {
            VaultTimeline tl = new VaultTimeline(VaultTimeline.Prize.test(w), 1);
            measure("Vault " + w, tl.total(), (c, t) -> VaultScene.draw(c, c::layer, tl, t, 640, 360));
        }

        System.out.println(String.format(Locale.ROOT, "%-34s %6s %9s %7s %8s %8s %8s %7s",
                "ending", "frames", "calls/fr", "worst", "at ms", "ms/fr", "worst ms", "pixels"));
        for (Result r : results) {
            System.out.println(String.format(Locale.ROOT, "%-34s %6d %9.0f %7d %8d %8.3f %8.2f %7d",
                    r.name(), r.frames(), r.meanCalls(), r.worstCalls(), r.worstAt(), r.meanMs(), r.worstMs(), r.worstLayer()));
        }
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
