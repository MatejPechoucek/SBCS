import com.sbcs.anim.PetTierTimeline;
import com.sbcs.anim.PetTierTimeline.Tier;
import com.sbcs.render.CrystalVeins;
import com.sbcs.render.PetTierScene;
import com.sbcs.render.art.ChopArt;
import com.sbcs.render.art.ScathaArt;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders frames of the Scatha pet's reveal offline, through the real PetTierScene on ChopPreview's game-accurate
 * canvas, over what the cleaver cutscene has drawn by then: the game (-Dbg, or a stand-in), the cutscene's dim, the
 * crystal veins fully grown and flaring with the reveal's colour, the reveal's own dim.
 *
 *   java -Dskins=<dir with head.png, body.png> -Dbg=<screenshot> -Dat=0,2400 -cp out PetTierPreview <outdir> <seed> TIER...
 *
 * Without -Dskins the worm wears ScathaArt's placeholders - in game it wears the skin captured off a live Scatha.
 * Time 0 is the moment the last cut frees the head.
 */
public final class PetTierPreview {

    public static void main(String[] args) throws Exception {
        String out = args[0];
        long seed = Long.parseLong(args[1]);
        int fps = Integer.getInteger("fps", 30);
        String skins = System.getProperty("skins");
        Map<String, BufferedImage> tex = new HashMap<>();
        tex.put(ChopArt.TEX_GLOW, ChopArt.glow());
        tex.put(ChopArt.TEX_RAYS, ChopArt.rays());
        BufferedImage head = skins == null ? null : HellionPreview.read(skins, "head.png");
        BufferedImage body = skins == null ? null : HellionPreview.read(skins, "body.png");
        tex.put("drop", head != null ? head : ScathaArt.placeholderHead());
        tex.put("bestiary_" + PetTierScene.BODY_SKIN, body != null ? body : ScathaArt.placeholderBody());
        BufferedImage bg = System.getProperty("bg") == null ? null : ImageIO.read(new File(System.getProperty("bg")));
        int W = ChopPreview.W, H = ChopPreview.H, S = ChopPreview.S;
        CrystalVeins veins = new CrystalVeins(seed);
        for (int i = 2; i < args.length; i++) {
            Tier tier = Tier.valueOf(args[i]);
            PetTierTimeline tl = new PetTierTimeline(tier, seed);
            tl.price(tier == Tier.LEGENDARY ? 41_500_000L : tier == Tier.EPIC ? 9_800_000L : 3_200_000L,
                    "lowest BIN, " + tier.title.toLowerCase());
            tl.note(tier == Tier.LEGENDARY ? "Your 1st Legendary Scatha, after 3,914 kills" : "Your 2nd Scatha pet, after 812 kills");
            File dir = new File(out, tier.name() + "_" + seed);
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
                ChopPreview.Java2D canvas = new ChopPreview.Java2D(g, tex, tier.title + " Scatha", null);
                // What ScopeMinigameScreen has drawn by now: its dim, the veins flaring, the reveal's dim.
                canvas.fill(0, 0, W, H, 222 << 24 | 0x0A0C12);
                float flare = Math.min(1f, t / 1600f);
                veins.draw(canvas::fill, W, H, 1f, CrystalVeins.End.FLARE, flare, tl.colour(t), W / 2f, H * 0.46f, 1f);
                float dim = 1f - (float) Math.pow(1f - Math.min(1f, t / 700f), 3);
                canvas.fill(0, 0, W, H, Math.round(170 * dim) << 24);
                PetTierScene.draw(canvas, canvas::fill, null, tl, t, W, H, W * 0.62f, H * 0.6f, H * 0.2f);
                g.dispose();
                ImageIO.write(img, "PNG", new File(dir, only != null ? String.format("t_%05d.png", t) : String.format("f_%04d.png", n)));
                n++;
            }
            System.out.println(tier + ": " + n + " frames, " + tl.total() + " ms");
        }
    }
}
