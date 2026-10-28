package com.sbcs.render;

import java.util.List;

/**
 * Superpairs while the game is played: the black box over a covered tile, and what the hunt draws round the menu.
 * Pure (no Minecraft) so the preview can draw a mock grid with it, as the game draws the real one.
 *
 * <b>The box is the same for every covered reward</b>: plain black with a faint shimmer (on the global clock, never
 * the reward's) and its letter. A box on its own only says "something from the rare row up"; the letter only says which
 * boxes are a pair. Round the menu, never over it: embers rise up the screen's edges while a box is unmatched, the
 * clicks left are pips down the menu's right side, and a found pair is joined by a crack of light that flashes and
 * fades.
 */
public final class PairsHunt {
    private PairsHunt() {}

    /** What the hunt draws with: GUI pixels. */
    public interface Paint {
        void fill(float x, float y, float w, float h, int argb);

        void text(String text, float cx, float cy, float scale, int argb);

        /** A line's width at scale 1, for left-aligned text; the default is the game font's usual 6 a character. */
        default float width(String text) {
            return text.length() * 6f;
        }
    }

    static final int GOLD = 0xE8C46A, BLACK = 0xFF0A080E, RIM = 0xFF2C2338, EMBER_A = 0xFFB070FF, EMBER_B = 0xFFE8C46A;

    /**
     * The cover over one tile, 16 x 16 at ({@code x}, {@code y}). {@code since} ms since this box first showed (it
     * drops in over the first 160), {@code now} the clock the shimmer runs on.
     */
    public static void cover(Paint p, float x, float y, char mark, long since, long now) {
        float k = since < 0 ? 1f : Math.min(1f, since / 160f);
        float grow = 1f + 0.5f * (1f - k) * (1f - k);
        float s = 16f * grow, ox = x + 8f - s / 2f, oy = y + 8f - s / 2f;
        p.fill(ox - 1, oy - 1, s + 2, s + 2, RIM);
        p.fill(ox, oy, s, s, BLACK);
        // The shimmer: a two-pixel diagonal band crossing every 2.4 s, the same on every box.
        float ph = (now % 2400) / 2400f;
        int band = Math.round(-16 + ph * 48);
        for (int row = 0; row < 16; row++) {
            int col = band - row / 2;
            if (col >= 0 && col < 15) p.fill(ox + col * grow, oy + row * grow, 2 * grow, grow, 0x2CB8A0FF);
        }
        float glow = 0.75f + 0.25f * (float) Math.sin(now / 260.0);
        int a = Math.round(255 * glow * Math.min(1f, k * 1.5f + 0.2f));
        p.text(String.valueOf(mark), x + 8f, y + 8.5f, 1f, a << 24 | GOLD);
    }

    /** A tile on screen: a box's slot, in GUI pixels (its top-left), and its letter. */
    public record Tile(float x, float y, char mark, boolean matched, long firstSeen) {}

    /** A pair found: its two tiles, and when. */
    public record Match(float x0, float y0, float x1, float y1, long at) {}

    /**
     * Round the menu: the menu's rect ({@code mx}, {@code my}, {@code mw}, {@code mh}) is kept clear. {@code hunting}
     * while a box waits for its pair; {@code turns} how far the hunt has gone (embers thicken with it).
     */
    public static void overlay(Paint p, int w, int h, float mx, float my, float mw, float mh, boolean hunting, int turns,
                               Integer clicksLeft, Integer clicksMax, List<Match> matches, long now) {
        int u = Math.max(2, Math.round(h / 120f));
        if (hunting) embers(p, w, h, mx, mw, u, turns, now);
        if (hunting && clicksLeft != null && clicksMax != null && clicksMax > 0) pips(p, mx + mw + 6, my + 4, mh - 8, clicksLeft, clicksMax, now);
        for (Match m : matches) crack(p, m, u, now);
    }

    /** Embers rising up both edges of the screen, outside the menu, in violet and gold. */
    static void embers(Paint p, int w, int h, float mx, float mw, int u, int turns, long now) {
        int n = 18 + Math.min(30, turns * 3);
        float left = Math.max(0f, mx - 8f), right = Math.min(w, mx + mw + 8f);
        for (int i = 0; i < n; i++) {
            float seedA = rnd(i, 1), seedB = rnd(i, 2), seedC = rnd(i, 3);
            boolean onLeft = (i & 1) == 0;
            float span = onLeft ? left : w - right;
            if (span < u * 2) continue;
            float period = 2600f + 2400f * seedB;
            float ph = ((now / period) + seedC) % 1f;
            float x = (onLeft ? 0f : right) + span * (onLeft ? seedA * seedA : 1f - seedA * seedA) + (float) Math.sin(ph * 7 + i) * u * 2;
            float y = h - ph * (h * (0.5f + 0.5f * seedB));
            float a = (1f - ph) * 0.8f * (0.5f + 0.5f * seedA);
            int col = i % 3 == 0 ? EMBER_B : EMBER_A;
            p.fill(Math.round(x / u) * u, Math.round(y / u) * u, u, u, Math.round(a * 255) << 24 | (col & 0xFFFFFF));
        }
    }

    /** The clicks left, a pip each down the menu's right edge; the last few pulse red. */
    static void pips(Paint p, float x, float y, float h, int left, int max, long now) {
        float step = Math.min(8f, h / Math.max(1, max));
        for (int i = 0; i < max; i++) {
            float py = y + i * step;
            boolean lit = i < left;
            int col = !lit ? 0x60302838 : left <= 3 ? (((now / 250) & 1) == 0 ? 0xFFFF5555 : 0xFFAA2222) : 0xFFE8C46A;
            p.fill(x, py, 4, Math.max(2f, step - 3f), col);
        }
    }

    /** A crack of light running between a found pair's two tiles, flashing, then fading over a second. */
    static void crack(Paint p, Match m, int u, long now) {
        long since = now - m.at();
        if (since < 0 || since > 1400) return;
        float run = Math.min(1f, since / 180f), fade = 1f - Math.max(0f, (since - 400) / 1000f);
        float x0 = m.x0() + 8, y0 = m.y0() + 8, x1 = m.x1() + 8, y1 = m.y1() + 8;
        float len = (float) Math.hypot(x1 - x0, y1 - y0);
        int steps = Math.max(1, Math.round(len / 2f));
        for (int s = 0; s <= steps * run; s++) {
            float f = s / (float) steps;
            float jitter = (rnd(s, (int) (m.at() & 0xFFFF)) - 0.5f) * 3f * (1f - f * (1 - f) * 0f);
            float x = x0 + (x1 - x0) * f - (y1 - y0) / Math.max(1f, len) * jitter;
            float y = y0 + (y1 - y0) * f + (x1 - x0) / Math.max(1f, len) * jitter;
            int col = s % 3 == 0 ? 0xFFFFFF : GOLD;
            p.fill(Math.round(x) - 1, Math.round(y) - 1, 2, 2, Math.round(255 * fade) << 24 | col);
        }
        // Both tiles flash.
        float flash = (float) Math.exp(-since / 220.0);
        for (float[] t : new float[][]{{m.x0(), m.y0()}, {m.x1(), m.y1()}}) {
            p.fill(t[0] - 2, t[1] - 2, 20, 20, Math.round(160 * flash) << 24 | 0xFFF2C8);
        }
    }

    /**
     * One rare reward in the panel: a box ({@code mark} its letter, {@code title} null - the panel never names what is
     * under a box) or a tile left uncovered under the price floor ({@code mark} 0, its title, colour and worth, -1 not
     * known). {@code revealed} once the board has been turned over, when an unfound one has got away.
     */
    public record Line(char mark, String title, int colour, long worth, boolean found, boolean revealed) {}

    static final int PANEL_BG = 0xE0100C16, PANEL_RIM = 0xFF5A4520, GREY = 0xFFA8A0B0, GREEN = 0xFF55FF55, RED = 0xFFFF5555;

    /**
     * The panel beside the menu: "RARE DROP!" and a line per rare reward on the board, with whether its pair has been
     * found. It sits to the right of the menu (past the clicks' pips), or to its left if the screen is too narrow, and
     * flashes for a moment when a new one turns up ({@code newestAt}).
     */
    public static void panel(Paint p, int w, int h, float mx, float my, float mw, float mh, List<Line> lines,
                             long newestAt, long now) {
        if (lines.isEmpty()) return;
        String head = "RARE DROP!", sub = lines.size() == 1 ? "on the board" : lines.size() + " on the board";
        float inner = Math.max(p.width(head), p.width(sub));
        for (Line l : lines) {
            inner = Math.max(inner, 12 + p.width(name(l)));
            inner = Math.max(inner, 12 + p.width(status(l)));
        }
        float pw = inner + 8, ph = 30 + lines.size() * 22;
        // Right of the menu, past the clicks' pips; else its left; on a screen too narrow for either, the right edge.
        float x = mx + mw + 14;
        if (x + pw > w - 4) x = mx - 14 - pw >= 4 ? mx - 14 - pw : Math.max(4, w - 4 - pw);
        float y = Math.max(4, Math.min(my, h - 4 - ph));
        long since = now - newestAt;
        float flash = since >= 0 && since < 1200 ? (float) Math.exp(-since / 300.0) : 0f;

        p.fill(x - 1, y - 1, pw + 2, ph + 2, PANEL_RIM);
        p.fill(x, y, pw, ph, PANEL_BG);
        p.fill(x, y, pw, 1, 0xFFE8C46A);
        if (flash > 0) p.fill(x, y, pw, ph, Math.round(90 * flash) << 24 | 0xFFF2C8);
        // The header blinks gold and white while it is new, then stays gold.
        int headCol = flash > 0.3f && ((since / 120) & 1) == 0 ? 0xFFFFFFFF : 0xFF000000 | GOLD;
        p.text(head, x + pw / 2f, y + 8, 1f, headCol);
        p.text(sub, x + pw / 2f, y + 18, 1f, GREY);

        float ly = y + 30;
        for (Line l : lines) {
            float lx = x + 4;
            if (l.mark() != 0) {
                p.fill(lx, ly, 10, 10, RIM);
                p.fill(lx + 1, ly + 1, 8, 8, BLACK);
                p.text(String.valueOf(l.mark()), lx + 5, ly + 5.5f, 1f, 0xFF000000 | GOLD);
            } else {
                p.fill(lx + 2, ly + 2, 6, 6, 0xFF000000 | l.colour());
            }
            left(p, name(l), lx + 12, ly + 5.5f, l.mark() != 0 ? 0xFFE0D8E8 : 0xFF000000 | l.colour());
            int col = l.found() ? GREEN : l.revealed() ? RED : (((now / 400) & 1) == 0 ? 0xFFE8C46A : 0xFFB8963A);
            left(p, status(l), lx + 12, ly + 15.5f, col);
            ly += 22;
        }
    }

    static String name(Line l) {
        return l.mark() != 0 ? "Hidden reward" : l.title();
    }

    /** "Find its pair!", "Found!" or "Got away", after the worth of a tile left uncovered. */
    static String status(Line l) {
        String s = l.found() ? "Found!" : l.revealed() ? "Got away" : "Find its pair!";
        return l.mark() == 0 && l.worth() > 0 ? coins(l.worth()) + " - " + s : s;
    }

    static void left(Paint p, String text, float x, float cy, int argb) {
        p.text(text, x + p.width(text) / 2f, cy, 1f, argb);
    }

    static String coins(long c) {
        if (c >= 1_000_000) return String.format(java.util.Locale.ROOT, "%.1fM", c / 1e6).replace(".0M", "M");
        if (c >= 1_000) return Math.round(c / 1000.0) + "k";
        return String.valueOf(c);
    }

    static float rnd(int x, int y) {
        int hsh = x * 374761393 + y * 668265263;
        hsh = (hsh ^ (hsh >>> 13)) * 1274126177;
        return ((hsh ^ (hsh >>> 16)) & 0xFFFF) / 65535f;
    }
}
