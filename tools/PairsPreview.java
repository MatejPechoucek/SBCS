import com.sbcs.anim.PairsTimeline;
import com.sbcs.detect.superpairs.PairsReward;
import com.sbcs.render.PairsHunt;
import com.sbcs.render.PairsScene;
import com.sbcs.render.SeraphScene;
import com.sbcs.render.art.ChopArt;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders frames of "The Hunt" (Superpairs) offline, through the real PairsScene on ChopPreview's game-accurate canvas:
 * 640 x 360 GUI px rasterised at -Dscale (3), small textures nearest, a dimmed screenshot behind (-Dbg).
 *
 *   java -Dvanilla=<flat dir: obsidian, enchanted_book, pufferfish, sga_a..z> -Dskins=<dir of NEU id_skin.png>
 *        [-Dat=0,2000,3600] -cp out:tools PairsPreview <outdir> <seed> WORD...
 *
 * WORD is what /sbcs test superpairs takes (fish, serum, guardian, book, dye, missed, two), or "hunt" for a mock of the
 * Superpairs menu while it is played: the boxes landing on tiles, the embers and clicks round it, a pair found.
 * A head with no skin in -Dskins is drawn with -Dstandin (a skin file) recoloured to the reward's colour, and says so.
 */
public final class PairsPreview {

    public static void main(String[] args) throws Exception {
        String out = args[0];
        long seed = Long.parseLong(args[1]);
        int fps = Integer.getInteger("fps", 30);
        String van = System.getProperty("vanilla"), skins = System.getProperty("skins");
        Map<String, BufferedImage> tex = new HashMap<>();
        for (String[] v : PairsScene.VANILLA) tex.put(v[0], HellionPreview.read(van, v[1] + ".png"));
        BufferedImage white = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) white.setRGB(x, y, 0xFFFFFFFF);
        tex.put(SeraphScene.TEX_WHITE, white);
        tex.put(ChopArt.TEX_GLOW, ChopArt.glow());
        tex.put(ChopArt.TEX_RAYS, ChopArt.rays());
        tex.put(ChopArt.TEX_RING, ChopArt.ring());
        tex.put(ChopArt.TEX_SHEET, ChopArt.sheet());
        tex.put(ChopArt.TEX_PIXDROP, ChopArt.pixDrop());
        tex.put(ChopArt.TEX_PIXSPLAT, ChopArt.pixSplat());
        for (int k = 0; k < 26; k++) {
            BufferedImage g = HellionPreview.read(van, "sga_" + (char) ('a' + k) + ".png");
            if (g != null) tex.put(ChopArt.glyphTex(k), g);
        }
        BufferedImage bg = System.getProperty("bg") == null ? null : ImageIO.read(new File(System.getProperty("bg")));
        int W = ChopPreview.W, H = ChopPreview.H, S = ChopPreview.S;
        for (int i = 2; i < args.length; i++) {
            if (args[i].equals("hunt")) {
                hunt(new File(out, "hunt"), fps, bg);
                continue;
            }
            PairsTimeline tl = PairsTimeline.test(args[i], seed);
            String[] nth = new String[tl.count()];
            List<Object[]> arts = new ArrayList<>();
            for (int k = 0; k < tl.count(); k++) {
                PairsTimeline.Box b = tl.box(k);
                if (b.matched()) {
                    tl.price(k, b.reward().tier() == PairsReward.Tier.DYE ? 41_250_000L : b.reward().tier() == PairsReward.Tier.ULTRA ? 6_480_000L : 2_350_000L,
                            "lowest BIN on the auction house");
                    nth[k] = "your 1st";
                }
                arts.add(art(b.reward(), van, skins));
            }
            PairsScene.nthNotes = nth;
            File dir = new File(out, args[i] + "_" + seed);
            dir.mkdirs();
            long[] times;
            String only = System.getProperty("at");
            if (only != null) {
                String[] q = only.split(",");
                times = new long[q.length];
                for (int k = 0; k < q.length; k++) times[k] = Long.parseLong(q[k].trim());
            } else {
                int n = (int) (tl.total() * fps / 1000) + 1;
                times = new long[n];
                for (int k = 0; k < n; k++) times[k] = k * 1000L / fps;
            }
            int n = 0;
            for (long t : times) {
                BufferedImage img = new BufferedImage(W * S, H * S, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.scale(S, S);
                ChopPreview.background(g, bg);
                int k = tl.index(t);
                ChopPreview.Java2D canvas = new ChopPreview.Java2D(g, tex, tl.box(k).title(), arts.get(k));
                PairsScene.draw(canvas, canvas::fill, tl, t, W, H);
                g.dispose();
                ImageIO.write(img, "PNG", new File(dir, only != null ? String.format("t_%05d.png", t) : String.format("f_%04d.png", n)));
                n++;
            }
            System.out.println(args[i] + ": " + n + " frames, total " + tl.total() + " ms");
        }
    }

    /** How a reward is drawn: a head's skin, or a flat icon - greyed by the scene for a box that got away. */
    static Object[] art(PairsReward r, String van, String skins) throws Exception {
        String a = r.art();
        if (a.startsWith("head:")) {
            BufferedImage skin = HellionPreview.read(skins, a.substring(5) + "_skin.png");
            if (skin == null && System.getProperty("standin") != null) {
                skin = recolour(HellionPreview.argb(ImageIO.read(new File(System.getProperty("standin")))), r.colour() == 0xFF55FF ? PairsReward.NADESHIKO : r.colour());
                System.out.println("  " + r.title() + ": no skin here, drawn with a stand-in recoloured " + Integer.toHexString(r.colour()));
            }
            return skin == null ? null : new Object[]{"head", skin};
        }
        BufferedImage icon = HellionPreview.read(van, (a.equals("fish") ? "pufferfish" : "enchanted_book") + ".png");
        return icon == null ? null : new Object[]{"flat", icon};
    }

    /** A skin's brightness onto one colour, for a stand-in. */
    static BufferedImage recolour(BufferedImage in, int rgb) {
        BufferedImage o = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < in.getHeight(); y++) for (int x = 0; x < in.getWidth(); x++) {
            int p = in.getRGB(x, y);
            float l = ((p >> 16 & 255) * 0.3f + (p >> 8 & 255) * 0.59f + (p & 255) * 0.11f) / 255f;
            float k = 0.35f + 0.9f * l;
            int r = Math.min(255, Math.round((rgb >> 16 & 255) * k)), gg = Math.min(255, Math.round((rgb >> 8 & 255) * k)),
                    b = Math.min(255, Math.round((rgb & 255) * k));
            o.setRGB(x, y, (p & 0xFF000000) | r << 16 | gg << 8 | b);
        }
        return o;
    }

    // ------------------------------------------------------------------ a mock of the menu while it is played

    /**
     * A stand-in Superpairs menu (5 x 9 tiles, a vanilla chest's panel) with the real PairsHunt drawing on it: tiles turn
     * up as grey glass or plain rewards, box A lands at 1.0 s and B at 2.6 s, A's pair turns at 4.6 s and is matched,
     * and the clicks run down. Only the panel and the tiles are mock; the boxes, embers, pips and crack are the mod's.
     */
    static void hunt(File dir, int fps, BufferedImage bg) throws Exception {
        dir.mkdirs();
        int W = ChopPreview.W, H = ChopPreview.H, S = ChopPreview.S;
        int mw = 176, mh = 6 * 18 + 17 + 7, mx = (W - mw) / 2, my = (H - mh) / 2;
        long total = 7000;
        int frames = (int) (total * fps / 1000);
        // Tiles: slot -> {turnsUp, turnsDown, kind (0 glass, 1 book, 2 bottle, 10 box A, 11 box B)}
        long[][] flips = {
                {10, 300, 900, 1}, {24, 300, 900, 2},
                {12, 1000, -1, 10}, {31, 1000, 1800, 2},
                {40, 2000, 2500, 1}, {19, 2000, 2500, 2},
                {28, 2600, -1, 11}, {14, 2600, 3300, 1},
                {37, 3500, 4100, 2}, {21, 3500, 4100, 1},
                {43, 4600, -1, 10},
        };
        for (int f = 0; f < frames; f++) {
            long t = f * 1000L / fps;
            BufferedImage img = new BufferedImage(W * S, H * S, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.scale(S, S);
            ChopPreview.background(g, bg);
            g.setColor(new Color(0, 0, 0, 0x90));
            g.fillRect(0, 0, W, H);
            g.setColor(new Color(0xC6C6C6));
            g.fillRect(mx, my, mw, mh);
            g.setColor(new Color(0x404040));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 8));
            g.drawString("Superpairs (Metaphysical)", mx + 8, my + 12);
            PairsHunt.Paint p = paint(g);
            boolean bUp = t >= 2600, aUp = t >= 1000;
            for (int s = 0; s < 54; s++) {
                int sx = mx + 8 + (s % 9) * 18, sy = my + 18 + (s / 9) * 18;
                g.setColor(new Color(0x8B8B8B));
                g.fillRect(sx - 1, sy - 1, 18, 18);
                int kind = 0;
                for (long[] fl : flips) if (fl[0] == s && t >= fl[1] && (fl[2] < 0 || t < fl[2])) kind = (int) fl[3];
                if (kind >= 10) {
                    long seen = kind == 10 ? (s == 43 ? 4600 : 1000) : 2600;
                    PairsHunt.cover(p, sx, sy, kind == 10 ? 'A' : 'B', t - seen, t);
                } else if (kind == 1) {
                    g.setColor(new Color(0x7A3FA0)); g.fillRect(sx + 3, sy + 2, 10, 12);
                } else if (kind == 2) {
                    g.setColor(new Color(0x6FD06F)); g.fillRect(sx + 5, sy + 3, 6, 11);
                } else {
                    g.setColor(new Color(0x5A5A6A)); g.fillRect(sx, sy, 16, 16);
                }
            }
            int clicks = 12 - (int) Math.min(11, t / 500);
            List<PairsHunt.Match> ms = t >= 4700 ? List.of(new PairsHunt.Match(mx + 8 + 3 * 18, my + 18 + 18, mx + 8 + 7 * 18, my + 18 + 4 * 18, 4700))
                    : List.of();
            boolean hunting = t < 4700 || bUp;
            PairsHunt.overlay(p, W, H, mx, my, mw, mh, hunting && aUp, (int) (t / 500), clicks, 12, ms, t);
            g.dispose();
            ImageIO.write(img, "PNG", new File(dir, String.format("f_%04d.png", f)));
        }
        System.out.println("hunt: " + frames + " frames");
    }

    static PairsHunt.Paint paint(Graphics2D g) {
        return new PairsHunt.Paint() {
            public void fill(float x, float y, float w, float h, int argb) {
                g.setColor(new Color(argb, true));
                g.fillRect(Math.round(x), Math.round(y), Math.max(1, Math.round(w)), Math.max(1, Math.round(h)));
            }

            public void text(String text, float cx, float cy, float scale, int argb) {
                g.setColor(new Color(argb, true));
                g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 9));
                int tw = g.getFontMetrics().stringWidth(text);
                g.drawString(text, cx - tw / 2f, cy + 3.5f);
            }
        };
    }
}
