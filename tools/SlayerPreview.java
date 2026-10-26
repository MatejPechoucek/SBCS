import com.sbcs.anim.SlayerTimeline;
import com.sbcs.anim.SlayerTimeline.Drop;
import com.sbcs.render.SlayerScene;
import com.sbcs.render.art.ChopArt;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders frames of "Exhumed" and "The Brood" offline through the real SlayerScene on ChopPreview's game-accurate
 * canvas, the game's own models posed by its own code (SlayerRigs: the client jar on the classpath, as for Eruption).
 *
 *   java -Dvanilla=<assets/minecraft/textures> -Dskins=<NEU skins, ID_skin.png> -Dpack=<hypixel item textures>
 *        -Dat=0,700 -cp "<mc classpath>:out" SlayerPreview <outdir> <seed> DROP...
 */
public final class SlayerPreview {

    public static void main(String[] args) throws Exception {
        String out = args[0];
        long seed = Long.parseLong(args[1]);
        int fps = Integer.getInteger("fps", 30);
        String van = System.getProperty("vanilla"), skins = System.getProperty("skins"), pack = System.getProperty("pack");
        Map<String, BufferedImage> tex = new HashMap<>();
        BufferedImage white = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) white.setRGB(x, y, 0xFFFFFFFF);
        tex.put(SlayerScene.TEX_WHITE, white);
        tex.put(ChopArt.TEX_GLOW, ChopArt.glow());
        SlayerScene.rigs = new com.sbcs.render.SlayerRigs();
        SlayerScene.RASTER.texture(SlayerScene.SLOT_WHITE, white);
        SlayerScene.INSIDE.texture(SlayerScene.SLOT_WHITE, white);
        String[][] slots = {
                {"" + SlayerScene.SLOT_ARMOUR, "entity/equipment/humanoid/diamond.png"},
                {"" + SlayerScene.SLOT_WARDEN, "entity/warden/warden.png"},
                {"" + SlayerScene.SLOT_HEART, "entity/warden/warden_heart.png"},
                {"" + SlayerScene.SLOT_BEE, "entity/bee/bee.png"},
                {"" + SlayerScene.SLOT_EYES, "entity/spider/spider_eyes.png"},
                {"" + SlayerScene.SLOT_SILVERFISH, "entity/silverfish/silverfish.png"},
                {"" + SlayerScene.SLOT_COBWEB, "block/cobweb.png"},
                {"" + SlayerScene.SLOT_BRICKS, "block/bricks.png"},
                {"" + SlayerScene.SLOT_CAVE_SPIDER, "entity/spider/cave_spider.png"},
        };
        for (String[] s : slots) {
            BufferedImage img = argb(ImageIO.read(new File(van, s[1])));
            SlayerScene.RASTER.texture(Integer.parseInt(s[0]), img);
            SlayerScene.INSIDE.texture(Integer.parseInt(s[0]), img);
        }
        BufferedImage bg = System.getProperty("bg") == null ? null : ImageIO.read(new File(System.getProperty("bg")));
        int W = ChopPreview.W, H = ChopPreview.H, S = ChopPreview.S;
        for (int i = 2; i < args.length; i++) {
            Drop d = Drop.valueOf(args[i]);
            boolean rev = d.source == SlayerTimeline.Source.REVENANT;
            BufferedImage boss = argb(ImageIO.read(new File(van, rev ? "entity/zombie/zombie.png" : "entity/spider/spider.png")));
            SlayerScene.RASTER.texture(SlayerScene.SLOT_BOSS, boss);
            SlayerScene.INSIDE.texture(SlayerScene.SLOT_BOSS, boss);
            tex.put(SlayerScene.TEX_FLOOR_A, argb(ImageIO.read(new File(van, rev ? "block/coarse_dirt.png" : "block/cobblestone.png"))));
            tex.put(SlayerScene.TEX_FLOOR_B, argb(ImageIO.read(new File(van, rev ? "block/podzol_top.png" : "block/mossy_cobblestone.png"))));
            Object[] art;
            if (d.isHead()) {
                art = new Object[]{"head", argb(ImageIO.read(new File(skins, skinFile(d) + "_skin.png")))};
            } else {
                File f = pack != null && d.pack != null ? new File(pack, d.pack + ".png") : null;
                if (f == null || !f.exists()) f = new File(van, "item/" + d.icon + ".png");
                art = new Object[]{"flat", argb(ImageIO.read(f))};
            }
            SlayerTimeline tl = new SlayerTimeline(d, seed);
            tl.price(prices(d), "bazaar instant sell");
            SlayerScene.nthNote = "your 1st";
            File dir = new File(out, d.name() + "_" + seed);
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
                ChopPreview.Java2D canvas = new ChopPreview.Java2D(g, tex, d.title, art);
                SlayerScene.draw(canvas, canvas::fill, canvas::fill, tl, t, W, H);
                g.dispose();
                ImageIO.write(img, "PNG", new File(dir, only != null ? String.format("t_%05d.png", t) : String.format("f_%04d.png", n)));
                n++;
            }
            System.out.println(d + ": " + n + " frames, " + tl.total() + " ms");
        }
    }

    /** The NEU id the skin is saved under. */
    static String skinFile(Drop d) {
        return switch (d) {
            case SNAKE_RUNE -> "SNAKE_RUNE;1";
            case DARKNESS_WITHIN_RUNE -> "DARKNESS_WITHIN_RUNE;1";
            case PARAGON_REVENANT, PARAGON_TARANTULA -> "SHARD_PARAGON";
            default -> d.priceId;
        };
    }

    /** Rough prices (2026-10-25), for the counter. */
    static long prices(Drop d) {
        return switch (d) {
            case WARDEN_HEART -> 27_400_000L;
            case MATCHA_DYE -> 120_000_000L;
            case BRICK_RED_DYE -> 95_000_000L;
            case PRIMORDIAL_EYE -> 38_000_000L;
            case DIGESTED_MOSQUITO -> 4_100_000L;
            case BEHEADED_HORROR -> 1_250_000L;
            default -> 900_000L;
        };
    }

    static BufferedImage argb(BufferedImage in) {
        BufferedImage o = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.drawImage(in, 0, 0, null);
        g.dispose();
        return o;
    }
}
