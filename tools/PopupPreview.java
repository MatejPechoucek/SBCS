import com.sbcs.detect.GhostDrops;
import com.sbcs.detect.SharkDrops;
import com.sbcs.render.FinPopup;
import com.sbcs.render.GhostPopup;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Renders Fin (the Fishing Festival's pop-up) and Wisp (the Ghosts') offline, frame by frame, with their sounds,
 * through the real FinPopup / GhostPopup drawing on {@link GuiPreview}'s stand-in GUI.
 *
 *   java -Dvanilla=<assets/minecraft> -Dpack=<assets/hypixel_skyblock> -Dskincache=<skin PNGs by hash>
 *        [-Dbg=<screenshot>] [-Dcrop=true] -cp out:src/main/resources:tools PopupPreview <outdir> fin|ghost WHAT
 *
 * WHAT is a SharkDrops / GhostDrops name, or "bust" for Fin. Frames are f_0000.png... at 30 fps and a sound.wav.
 * -Dcrop keeps the bottom middle of the screen, where the pop-up is, a quarter of the screen's area.
 */
public final class PopupPreview {
    public static void main(String[] a) throws Exception {
        GuiPreview.init();
        GuiPreview.captureAudio();
        File dir = new File(a[0]);
        dir.mkdirs();
        boolean fin = a[1].equals("fin");
        String what = a[2];
        int W = Integer.getInteger("w", 640), H = Integer.getInteger("h", 360), S = Integer.getInteger("scale", 3);
        boolean crop = Boolean.getBoolean("crop");
        BufferedImage bg = System.getProperty("bg") == null ? null : ImageIO.read(new File(System.getProperty("bg")));
        long end;
        if (fin) {
            if (what.equals("bust")) FinPopup.showBust();
            else FinPopup.show(SharkDrops.Drop.valueOf(what));
            end = what.equals("bust") ? 1650 : 3000;
        } else {
            GhostPopup.show(GhostDrops.Drop.valueOf(what));
            end = 3100;
        }
        Class<?> c = fin ? FinPopup.class : GhostPopup.class;
        GuiPreview.putStatic(c, "speed", 1f);
        long lead = 400, tail = 500;
        int n = 0;
        for (long t = -lead; t <= end + tail; t += 1000 / 30, n++) {
            BufferedImage img = GuiPreview.frame(W, H, S, bg);
            if (t >= 0) {
                GuiPreview.putStatic(c, "startedAt", System.currentTimeMillis() - t);
                Graphics2D g = img.createGraphics();
                GuiPreview.J2D gui = GuiPreview.J2D.of(g, W, H, S);
                if (fin) FinPopup.draw(gui);
                else GhostPopup.draw(gui);
                g.dispose();
                GuiPreview.drainAudio(t + lead);
            }
            if (crop) img = img.getSubimage(img.getWidth() / 4, img.getHeight() / 2, img.getWidth() / 2, img.getHeight() / 2);
            ImageIO.write(img, "PNG", new File(dir, String.format("f_%04d.png", n)));
        }
        GuiPreview.writeWav(new File(dir, "sound.wav"), (end + lead + tail) / 1000f + 1f);
        System.out.println(a[1] + " " + what + ": " + n + " frames");
        System.exit(0);
    }
}
