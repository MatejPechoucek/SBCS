import com.sbcs.anim.WaterTimeline;
import com.sbcs.anim.WaterTimeline.Creature;
import com.sbcs.anim.WaterTimeline.Drop;
import com.sbcs.render.WaterScene;
import com.sbcs.render.art.ChopArt;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders frames of "The Coil" (the Titanoboa) and "The Totem" (the Wiki Tiki) offline through the real WaterScene on
 * ChopPreview's game-accurate canvas.
 *
 *   java -Dvanilla=<assets/minecraft/textures> -Dskins=<folder of NAME_skin.png> -Dpack=<hypixel item textures>
 *        -Dat=0,700 -cp "out:src/main/resources" WaterPreview <outdir> <seed> CREATURE:DROP...
 *
 * The creatures' and the head drops' skins are read from {@code -Dskins} ("Titanoboa_skin.png", "Wiki Tiki_skin.png",
 * "SNAKE_EYES_skin.png"...). Where one is missing a painted stand-in is used, so a preview can be made without them -
 * the game always uses the real NEU skins.
 */
public final class WaterPreview {

    public static void main(String[] args) throws Exception {
        String out = args[0];
        long seed = Long.parseLong(args[1]);
        int fps = Integer.getInteger("fps", 30);
        String van = System.getProperty("vanilla"), skins = System.getProperty("skins"), pack = System.getProperty("pack");
        Map<String, BufferedImage> tex = new HashMap<>();
        tex.put(ChopArt.TEX_GLOW, ChopArt.glow());
        BufferedImage white = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) white.setRGB(x, y, 0xFFFFFFFF);
        WaterScene.RASTER.texture(WaterScene.SLOT_WHITE, white);
        WaterScene.RASTER.texture(WaterScene.SLOT_LILY, argb(ImageIO.read(new File(van, "block/lily_pad.png"))));
        for (int i = 0; i < 5; i++) {
            WaterScene.RASTER.texture(WaterScene.SLOT_TC + i, argb(ImageIO.read(new File(van, "block/" + WaterScene.RING_TEXTURES[i] + ".png"))));
        }
        WaterScene.RASTER.texture(WaterScene.SLOT_LOG, argb(ImageIO.read(new File(van, "block/stripped_jungle_log.png"))));
        BufferedImage water = argb(ImageIO.read(new File(van, "block/water_still.png")));
        WaterScene.waterFrames = water.getHeight() / 16;
        WaterScene.waterPixels = water.getRGB(0, 0, 16, water.getHeight(), null, 0, 16);
        BufferedImage bg = System.getProperty("bg") == null ? null : ImageIO.read(new File(System.getProperty("bg")));
        int W = ChopPreview.W, H = ChopPreview.H, S = ChopPreview.S;
        for (int i = 2; i < args.length; i++) {
            String[] cd = args[i].split(":");
            Creature cr = Creature.valueOf(cd[0]);
            Drop d = Drop.valueOf(cd[1]);
            WaterScene.RASTER.texture(WaterScene.SLOT_SKIN, skin(skins, cr.title, cr == Creature.TITANOBOA ? 0 : 1));
            Object[] art = null;
            String name = d.win() ? d.title : cr.guaranteed;
            if (d.head != null) {
                art = new Object[]{"head", skin(skins, d.head, d == Drop.TIKI_MASK ? 1 : d == Drop.SNAKE_EYES ? 2 : d == Drop.BUBBLE ? 3 : 4)};
            } else if (d.win()) {
                File f = pack != null && d.pack != null ? new File(pack, d.pack + ".png") : null;
                if (f == null || !f.exists()) f = new File(van, "item/" + d.icon + ".png");
                art = new Object[]{"flat", argb(ImageIO.read(f))};
            } else if (cr.guaranteedIcon != null) {
                art = new Object[]{"flat", argb(ImageIO.read(new File(van, "item/" + cr.guaranteedIcon + ".png")))};
            }
            WaterTimeline tl = new WaterTimeline(cr, d, seed);
            tl.price(prices(d, cr), d.win() ? "lowest BIN on the auction house" : "bazaar instant sell");
            WaterScene.nthNote = d.win() ? "your 1st" : null;
            File dir = new File(out, cr.name() + "_" + d.name() + "_" + seed);
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
                ChopPreview.Java2D canvas = new ChopPreview.Java2D(g, tex, name, art);
                WaterScene.draw(canvas, canvas::fill, canvas::fill, tl, t, W, H);
                g.dispose();
                ImageIO.write(img, "PNG", new File(dir, only != null ? String.format("t_%05d.png", t) : String.format("f_%04d.png", n)));
                n++;
            }
            System.out.println(cr + " " + d + ": " + n + " frames, " + tl.total() + " ms");
        }
    }

    /** Rough prices (guesses, 2026-10-26 - nothing fetched), for the counter. */
    static long prices(Drop d, Creature c) {
        return switch (d) {
            case BUST -> c == Creature.TITANOBOA ? 1_600 : 640;
            case SCRIPTURES -> 900_000;
            case SHED -> 4_000_000;
            case BUBBLE -> 3_000_000;
            case SNAKE_EYES -> 9_000_000;
            case TIKI_MASK -> 25_000_000;
            case AQUAMARINE -> 300_000_000;
        };
    }

    /** The skin from the folder, else a painted stand-in: 0 snake, 1 tiki, 2 gold eyes, 3 bubble, 4 aquamarine dye. */
    static BufferedImage skin(String dir, String name, int kind) throws Exception {
        if (dir != null) {
            File f = new File(dir, name + "_skin.png");
            if (f.exists()) return argb(ImageIO.read(f));
        }
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        java.util.Random r = new java.util.Random(kind * 31 + 7);
        int[] base = {0x4E6B2C, 0x8A5A2B, 0xD8A62A, 0x9FD8F0, 0x5FD8B8};
        int[] dark = {0x2C3D18, 0x5A3618, 0x8A5E10, 0x6AAED0, 0x2E9A80};
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 32; x++) {
                int c = base[kind];
                if (kind == 0 && ((x + y * 2) % 6 == 0 || (x * 2 - y) % 6 == 0)) c = dark[0];
                else if (kind == 1 && y % 4 == 3) c = dark[1];
                else if (r.nextInt(5) == 0) c = dark[kind];
                img.setRGB(x, y, 0xFF000000 | c);
            }
        }
        // The front face (8..16, 8..16).
        if (kind == 0 || kind == 2) {
            for (int[] e : new int[][]{{9, 10}, {14, 10}}) {
                img.setRGB(e[0], e[1], 0xFFFFD43A);
                img.setRGB(e[0] + 1, e[1], 0xFF120A02);
            }
            img.setRGB(11, 14, 0xFF1A1208);
            img.setRGB(12, 14, 0xFF1A1208);
        }
        if (kind == 1) {
            for (int x = 9; x <= 14; x++) img.setRGB(x, 9, 0xFF3A2008);
            for (int[] e : new int[][]{{9, 11}, {13, 11}}) {
                img.setRGB(e[0], e[1], 0xFFF0F0E0);
                img.setRGB(e[0] + 1, e[1], 0xFF101010);
            }
            for (int x = 10; x <= 13; x++) {
                img.setRGB(x, 13, 0xFFB8312A);
                img.setRGB(x, 14, (x & 1) == 0 ? 0xFFF0F0E0 : 0xFF3A2008);
            }
        }
        return img;
    }

    static BufferedImage argb(BufferedImage in) {
        BufferedImage o = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.drawImage(in, 0, 0, null);
        g.dispose();
        return o;
    }
}
