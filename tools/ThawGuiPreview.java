import com.sbcs.detect.vanguard.CorpseHaul;
import com.sbcs.detect.vanguard.VanguardLoot;
import com.sbcs.gui.ThawScreen;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the whole Thaw offline - the real ThawScreen, frame by frame, with its sounds - on {@link GuiPreview}'s
 * stand-in GUI. (ThawPreview draws only the ice, through the meshes.)
 *
 *   java -Dvanilla=... -Dpack=... -Dskincache=... [-Dbg=<screenshot>]
 *        -cp out:src/main/resources:tools ThawGuiPreview <outdir> "Drop name" ["Drop name" ...]
 *
 * The drops are VanguardLoot names, the corpse's whole haul. Frames are f_0000.png... at 30 fps and a sound.wav.
 * Items the game draws as vanilla item stacks (g.item) are left out offline; heads and sprites are drawn.
 */
public final class ThawGuiPreview {
    public static void main(String[] a) throws Exception {
        GuiPreview.init();
        GuiPreview.captureAudio();
        File dir = new File(a[0]);
        dir.mkdirs();
        List<CorpseHaul.Drop> drops = new ArrayList<>();
        for (int i = 1; i < a.length; i++) drops.add(new CorpseHaul.Drop(VanguardLoot.find(a[i]), 1));
        int W = Integer.getInteger("w", 640), H = Integer.getInteger("h", 360), S = Integer.getInteger("scale", 2);
        BufferedImage bg = System.getProperty("bg") == null ? null : ImageIO.read(new File(System.getProperty("bg")));
        stacks();
        ThawScreen screen = new ThawScreen(new CorpseHaul(drops), 40);
        // Screen.init(w, h) also sets up focus and narration, which want a live game: the size is all the Thaw uses.
        GuiPreview.put(screen, net.minecraft.client.gui.screens.Screen.class, "width", W);
        GuiPreview.put(screen, net.minecraft.client.gui.screens.Screen.class, "height", H);
        com.sbcs.anim.ThawTimeline tl = (com.sbcs.anim.ThawTimeline) field(screen, "tl");
        long total = tl.total();
        int n = 0;
        for (long t = 0; t < total; t += 1000 / 30, n++) {
            long now = System.currentTimeMillis();
            GuiPreview.put(screen, ThawScreen.class, "openedAt", now - t);
            GuiPreview.put(screen, ThawScreen.class, "pipOpened", now - t);
            BufferedImage img = GuiPreview.frame(W, H, S, bg);
            Graphics2D g = img.createGraphics();
            screen.extractRenderState(GuiPreview.J2D.of(g, W, H, S), -1, -1, 0f);
            g.dispose();
            GuiPreview.drainAudio(t);
            ImageIO.write(img, "PNG", new File(dir, String.format("f_%04d.png", n)));
        }
        GuiPreview.writeWav(new File(dir, "sound.wav"), total / 1000f + 1.5f);
        System.out.println("thaw: " + n + " frames, " + tl.finale());
        System.exit(0);
    }

    /**
     * VanguardItems' five vanilla items, made without their data components: offline the registries are bootstrapped
     * but never bound to data, so {@code new ItemStack} throws. The stand-in GUI only asks a stack for its item.
     */
    @SuppressWarnings("unchecked")
    static void stacks() throws Exception {
        java.util.Map<String, net.minecraft.world.item.ItemStack> map =
                (java.util.Map<String, net.minecraft.world.item.ItemStack>) GuiPreview.getStatic(com.sbcs.render.VanguardItems.class, "VANILLA");
        Object[][] items = {{"Skeleton Key", net.minecraft.world.item.Items.TRIPWIRE_HOOK}, {"Umber Key", net.minecraft.world.item.Items.DEAD_BUSH},
                {"Tungsten Key", net.minecraft.world.item.Items.LEVER}, {"Ice Cold I", net.minecraft.world.item.Items.ENCHANTED_BOOK},
                {"Blue Goblin Egg", net.minecraft.world.item.Items.GLOW_SQUID_SPAWN_EGG}};
        for (Object[] it : items) {
            net.minecraft.world.item.ItemStack st = (net.minecraft.world.item.ItemStack) GuiPreview.U.allocateInstance(net.minecraft.world.item.ItemStack.class);
            GuiPreview.put(st, net.minecraft.world.item.ItemStack.class, "item", ((net.minecraft.world.item.Item) it[1]).builtInRegistryHolder());
            GuiPreview.put(st, net.minecraft.world.item.ItemStack.class, "count", 1);
            map.put((String) it[0], st);
        }
    }

    static Object field(Object o, String name) throws Exception {
        java.lang.reflect.Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }
}
