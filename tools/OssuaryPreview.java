import com.sbcs.anim.OssuaryTimeline;
import com.sbcs.detect.dungeon.*;
import com.sbcs.render.OssuaryScene;
import com.sbcs.render.SeraphScene;
import com.sbcs.render.art.ChopArt;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Renders frames of "The Ossuary" offline through the real OssuaryScene on ChopPreview's game-accurate canvas.
 *
 *   java -Dvanilla=<dir of the game's block and item textures, flat: deepslate_tiles.png, lantern.png...>
 *        -Dat=0,1500,4000 -cp out:tools OssuaryPreview <outdir> <seed> FLOOR:TIER:KEY:WORTH[:chain] ...
 *
 * e.g. F7:BEDROCK:NECRON_HANDLE:600000000 or M5:BEDROCK:DYE_LIVID:20000000:chain. Plays as watch mode (or the
 * chain left alone), so the hand does everything; frames run to the end. Items are drawn as their vanilla icon,
 * heads (no skins offline) as a tinted square. Dealers' heads draw as bone without -Dskins=<dir of KEY_skin.png>.
 */
public final class OssuaryPreview {

    public static void main(String[] args) throws Exception {
        String out = args[0];
        long seed = Long.parseLong(args[1]);
        int fps = Integer.getInteger("fps", 20);
        String van = System.getProperty("vanilla");
        Map<String, BufferedImage> tex = new HashMap<>();
        for (String[] v : OssuaryScene.VANILLA) {
            String file = v[1].substring(v[1].indexOf('/') + 1);
            BufferedImage img = HellionPreview.read(van, file + ".png");
            if (img != null) tex.put(v[0], img);
        }
        for (ChestTier t : ChestTier.values()) {
            BufferedImage img = HellionPreview.read(van, t.block + ".png");
            if (img != null) tex.put(OssuaryScene.chestTex(t), img);
        }
        BufferedImage white = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) white.setRGB(x, y, 0xFFFFFFFF);
        tex.put(SeraphScene.TEX_WHITE, white);
        tex.put(ChopArt.TEX_GLOW, ChopArt.glow());
        String skins = System.getProperty("skins");
        BufferedImage bg = System.getProperty("bg") == null ? null : ImageIO.read(new File(System.getProperty("bg")));
        DungeonPool pool = DungeonPool.parse(Files.readString(Path.of("src/main/resources/assets/sbcs/dungeon_pool.json")));
        int W = ChopPreview.W, H = ChopPreview.H, S = ChopPreview.S;

        for (int i = 2; i < args.length; i++) {
            String[] q = args[i].split(":");
            DungeonFloor f = DungeonFloor.fromLabel(q[0]);
            ChestTier tier = ChestTier.valueOf(q[1]);
            String key = q[2];
            long worth = Long.parseLong(q[3]);
            boolean chain = q.length > 4 && q[4].equals("chain");
            var entries = pool.pool(f, tier);
            List<Loot> looks = new ArrayList<>();
            int[] wts = new int[entries.size()];
            for (int k = 0; k < wts.length; k++) {
                looks.add(entries.get(k).look());
                wts[k] = entries.get(k).weight();
            }
            OssuaryTimeline tl = new OssuaryTimeline(f, tier, f.master(), chain, seed, looks, wts);
            Loot best = pool.look(key) != null ? pool.look(key) : Loot.named(key, key, 1);
            List<Loot> items = List.of(best, pool.look("RECOMBOBULATOR_3000"), pool.look("ENCHANTMENT_ULTIMATE_WISE_1"),
                    Loot.named("ESSENCE_WITHER", "Wither Essence", 24), pool.look("HOT_POTATO_BOOK"));
            Map<String, Long> p = new HashMap<>(Map.of("RECOMBOBULATOR_3000", 6_400_000L, "ENCHANTMENT_ULTIMATE_WISE_1", 250_000L,
                    "ESSENCE_WITHER", 2_500L, "HOT_POTATO_BOOK", 90_000L));
            p.put(key, worth);
            if (worth == 0) p.replaceAll((k2, v2) -> 1_000L); // a bust: nothing in it worth the floor
            ChestValue v = ChestValue.of(new RewardChest(f, tier, items, tier == ChestTier.BEDROCK ? 4_000_000 : 2_000_000, false, false, -1), p::get);
            tl.resolve(v, 1_000_000);
            for (long t = 0; t < 60_000 && tl.reveal() < 0; t += 16) tl.update(t);
            long total = tl.total();
            OssuaryScene.nthNote = "your 2nd";
            OssuaryScene.dryNote = "12 chests since a rare reward";
            File dir = new File(out, args[i].replace(':', '_') + "_" + seed);
            dir.mkdirs();
            long[] times;
            String only = System.getProperty("at");
            if (only != null) {
                String[] qs = only.split(",");
                times = new long[qs.length];
                for (int k = 0; k < qs.length; k++) {
                    String s = qs[k].trim();
                    // "r+500": ms after the reveal; "end-300": before the end.
                    times[k] = s.startsWith("r") ? tl.reveal() + Long.parseLong(s.substring(1)) : s.startsWith("end") ? total + Long.parseLong(s.substring(3)) : Long.parseLong(s);
                }
            } else {
                int n = (int) (total * fps / 1000) + 1;
                times = new long[n];
                for (int k = 0; k < n; k++) times[k] = k * 1000L / fps;
            }
            int n = 0;
            for (long t : times) {
                BufferedImage img = new BufferedImage(W * S, H * S, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.scale(S, S);
                ChopPreview.background(g, bg);
                Canvas canvas = new Canvas(g, tex, van, skins);
                OssuaryScene.draw(canvas, canvas::fill, tl, t, W, H);
                g.dispose();
                ImageIO.write(img, "PNG", new File(dir, only != null ? String.format("t_%05d.png", t) : String.format("f_%04d.png", n)));
                n++;
            }
            System.out.println(args[i] + ": " + n + " frames, reveal " + tl.reveal() + ", total " + total + " ms, " + tl.band() + " " + tl.ending());
        }
    }

    static final class Canvas extends ChopPreview.Java2D implements OssuaryScene.Canvas {
        final String van, skins;
        final Map<String, BufferedImage> icons = new HashMap<>();

        Canvas(Graphics2D g, Map<String, BufferedImage> tex, String van, String skins) {
            super(g, tex, "", null);
            this.van = van;
            this.skins = skins;
        }

        public void loot(Loot l, float cx, float cy, float size, float alpha) {
            if (l == null || alpha < 0.05f || size < 2) return;
            String item = l.item() == null ? "minecraft:paper" : l.item();
            String file = item.substring(item.indexOf(':') + 1);
            if (file.equals("player_head")) {
                int col = 0xFF000000 | (Objects.hashCode(l.key()) & 0x7F7F7F) + 0x303030;
                fill(cx - size * 0.38f, cy - size * 0.38f, size * 0.76f, size * 0.76f, (Math.round(alpha * 255) << 24) | (col & 0xFFFFFF));
                return;
            }
            BufferedImage img = icons.computeIfAbsent(file, k -> HellionPreview.read(van, k + ".png"));
            if (img == null) return;
            Composite before = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(img, Math.round(cx - size / 2f), Math.round(cy - size / 2f), Math.round(size), Math.round(size), null);
            g.setComposite(before);
        }

        public String headSkin(String key) {
            if (skins == null) return null;
            String name = "dealer_" + key;
            if (!tex.containsKey(name)) {
                BufferedImage img = HellionPreview.read(skins, key + "_skin.png");
                if (img == null) return null;
                tex.put(name, img);
            }
            return name;
        }
    }
}
