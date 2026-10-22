import com.sbcs.anim.VaultTimeline;
import com.sbcs.render.SeraphScene;
import com.sbcs.render.VaultScene;
import com.sbcs.render.art.ChopArt;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders frames of "The Vault" (the Crystal Nucleus) offline, through the real VaultScene on ChopPreview's
 * game-accurate canvas: 640 x 360 GUI px rasterised at -Dscale (3), small textures nearest, a dimmed screenshot
 * behind (-Dbg).
 *
 *   java -Dvanilla=<dir with vault_bottom.png, vault_top.png, iron_block.png, polished_blackstone.png, quartz.png,
 *        gold_ingot.png> -Dat=0,2000,4800 -cp out:tools VaultPreview <outdir> <seed> WORD...
 *
 * WORD is what /sbcs test nucleus takes: bust, claw, jasper, alloy, dye, or a Flawless gemstone's colour. The game's
 * textures are read from the designer's own game files for this preview only - the mod adopts them at runtime.
 */
public final class VaultPreview {

    public static void main(String[] args) throws Exception {
        String out = args[0];
        long seed = Long.parseLong(args[1]);
        int fps = Integer.getInteger("fps", 30);
        String van = System.getProperty("vanilla");
        Map<String, BufferedImage> tex = new HashMap<>();
        for (String[] v : VaultScene.VANILLA) tex.put(v[0], HellionPreview.argb(ImageIO.read(new File(van, v[1] + ".png"))));
        BufferedImage white = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) white.setRGB(x, y, 0xFFFFFFFF);
        tex.put(SeraphScene.TEX_WHITE, white);
        tex.put(ChopArt.TEX_GLOW, ChopArt.glow());
        BufferedImage bg = System.getProperty("bg") == null ? null : ImageIO.read(new File(System.getProperty("bg")));
        int W = ChopPreview.W, H = ChopPreview.H, S = ChopPreview.S;
        for (int i = 2; i < args.length; i++) {
            VaultTimeline.Prize prize = VaultTimeline.Prize.test(args[i]);
            VaultTimeline tl = new VaultTimeline(prize, seed);
            if (prize.win()) tl.price(12_345_678L, "lowest BIN on the auction house");
            VaultScene.nthNote = prize.win() ? "your 1st" : null;
            VaultScene.bustNote = prize.win() ? null : "14 bundles without a rare roll";
            Object[] art = null;
            if (prize.art() != null && !prize.art().startsWith("head:")) {
                String icon = prize.art().equals("alloy") ? "gold_ingot" : "quartz";
                BufferedImage img = HellionPreview.read(van, icon + ".png");
                if (img != null) art = new Object[]{"flat", img};
            }
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
                ChopPreview.Java2D canvas = new ChopPreview.Java2D(g, tex, prize.title(), art);
                VaultScene.draw(canvas, canvas::fill, tl, t, W, H);
                g.dispose();
                ImageIO.write(img, "PNG", new File(dir, only != null ? String.format("t_%05d.png", t) : String.format("f_%04d.png", n)));
                n++;
            }
            System.out.println(args[i] + ": " + n + " frames, total " + tl.total() + " ms");
        }
    }
}
