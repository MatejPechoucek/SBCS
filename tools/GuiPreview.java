import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.sbcs.audio.SbcsAudio;
import com.sbcs.audio.SynthEngine;
import com.sbcs.audio.Voice;
import com.sbcs.render.HeadSkins;
import com.sbcs.render.Offline;
import com.sbcs.render.TextureBank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Draws the mod's real GUI-drawn screens offline: a Java2D stand-in for the game's {@link GuiGraphicsExtractor}
 * (fills, blits with their tint, text in the game's own ascii font, the pose matrix, scissors), a stand-in
 * {@link Minecraft} that has a font and a resource manager reading textures from folders, and {@link Offline#hooks}
 * catching what would go to the GPU. It lets the screens that draw straight to the GUI - the Thaw, the Scatha's
 * cleaver, Fin, Wisp - be rendered frame by frame without the game, the way ChopPreview does for the Canvas scenes.
 *
 * Folders, as system properties: -Dvanilla (a resource pack's assets/minecraft, i.e. the folder holding textures/),
 * -Dpack (Hypixel's pack's assets/hypixel_skyblock), -Dskincache (textures.minecraft.net PNGs named by hash).
 * The mod's own assets are read from src/main/resources. Run from the repository root.
 *
 * Sounds: {@link #captureAudio()} turns the mod's synth into a recorder - every voice a screen plays is kept with
 * the frame time it was asked for ({@link #drainAudio}) - and {@link #writeWav} mixes them.
 */
public final class GuiPreview {
    private GuiPreview() {}

    static sun.misc.Unsafe U;
    static final Map<Identifier, BufferedImage> TEXTURES = new HashMap<>();
    static String vanilla, pack, skinCache;
    static BufferedImage ascii;
    static final int[] ADV = new int[256];

    /** Sets everything up. Call once, before any of the mod's classes draw. */
    public static void init() throws Exception {
        Field uf = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        uf.setAccessible(true);
        U = (sun.misc.Unsafe) uf.get(null);
        vanilla = System.getProperty("vanilla");
        pack = System.getProperty("pack");
        skinCache = System.getProperty("skincache");
        ascii = ImageIO.read(new File(vanilla, "textures/font/ascii.png"));
        for (int c = 0; c < 256; c++) {
            int gx = (c % 16) * 8, gy = (c / 16) * 8, right = -1;
            for (int xx = 0; xx < 8; xx++)
                for (int yy = 0; yy < 8; yy++)
                    if (((ascii.getRGB(gx + xx, gy + yy) >>> 24) & 0xFF) > 0) right = Math.max(right, xx);
            ADV[c] = c == ' ' ? 4 : right + 2;
        }
        Offline.hooks = new Offline.Hooks() {
            @Override
            public void upload(Identifier id, BufferedImage image) {
                TEXTURES.put(id, image);
            }

            @Override
            public void layer(GuiGraphicsExtractor g, int[] argb, int w, int h, int unit) {
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                img.setRGB(0, 0, w, h, argb, 0, w);
                ((J2D) g).image(img, 0, 0, w * unit, h * unit);
            }

            @Override
            public String headTexture(HeadSkins set, String name) {
                String blob = set.blob(name);
                BufferedImage img = blob == null ? null : skin(blob);
                if (img == null) return null;
                String bank = "offline_head_" + Integer.toHexString(blob.hashCode());
                TextureBank.register(bank, () -> img);
                return bank;
            }
        };
        Minecraft mc = (Minecraft) U.allocateInstance(Minecraft.class);
        Field inst = Minecraft.class.getDeclaredField("instance");
        U.putObject(U.staticFieldBase(inst), U.staticFieldOffset(inst), mc);
        put(mc, Minecraft.class, "font", U.allocateInstance(FakeFont.class));
        Font f = mc.font;
        put(f, Font.class, "lineHeight", 9);
        put(mc, Minecraft.class, "resourceManager", U.allocateInstance(FakeResources.class));
        // The stats and config live in Fabric's config folder: a scratch one, empty, so every count is a first.
        java.nio.file.Path cfg = java.nio.file.Files.createTempDirectory("sbcs-preview");
        Object loader = net.fabricmc.loader.impl.FabricLoaderImpl.INSTANCE;
        put(loader, loader.getClass(), "configDir", cfg);
        put(loader, loader.getClass(), "gameDir", cfg);
        // Item stacks (the Thaw's vanilla drops) need the registries.
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        com.sbcs.SbcsClient.registerTextures();
        // No game behind the cutscene, so no gameplay window in its corner.
        putStatic(com.sbcs.render.GameplayView.class, "broken", true);
    }

    static void put(Object o, Class<?> c, String name, Object v) throws Exception {
        Field f = c.getDeclaredField(name);
        long off = U.objectFieldOffset(f);
        if (v instanceof Integer i) U.putInt(o, off, i);
        else if (v instanceof Long l) U.putLong(o, off, l);
        else if (v instanceof Boolean b) U.putBoolean(o, off, b);
        else if (v instanceof Float x) U.putFloat(o, off, x);
        else U.putObject(o, off, v);
    }

    /** A static field of any class, final or not. */
    public static void putStatic(Class<?> c, String name, Object v) throws Exception {
        Field f = c.getDeclaredField(name);
        Object base = U.staticFieldBase(f);
        long off = U.staticFieldOffset(f);
        if (v instanceof Integer i) U.putInt(base, off, i);
        else if (v instanceof Long l) U.putLong(base, off, l);
        else if (v instanceof Boolean b) U.putBoolean(base, off, b);
        else if (v instanceof Float x) U.putFloat(base, off, x);
        else U.putObject(base, off, v);
    }

    public static Object getStatic(Class<?> c, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

    /** A head's skin from its textures blob, out of the skin cache. */
    static BufferedImage skin(String blob) {
        try {
            String json = new String(Base64.getDecoder().decode(blob.trim()), StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("texture/([0-9a-f]+)").matcher(json);
            if (!m.find() || skinCache == null) return null;
            File f = new File(skinCache, m.group(1) + ".png");
            return f.exists() ? argb(ImageIO.read(f)) : null;
        } catch (Exception e) {
            return null;
        }
    }

    static File file(Identifier id) {
        String ns = id.getNamespace(), p = id.getPath();
        if (ns.equals("minecraft")) return vanilla == null ? null : new File(vanilla, p);
        if (ns.equals("hypixel_skyblock")) return pack == null ? null : new File(pack, p);
        return new File("src/main/resources/assets/" + ns + "/" + p);
    }

    static final Set<Identifier> MISSING = new HashSet<>();

    static BufferedImage texture(Identifier id) {
        if (id == null) return null;
        BufferedImage img = TEXTURES.get(id);
        if (img != null) return img;
        if (MISSING.contains(id)) return null;
        try {
            File f = file(id);
            if (f != null && f.exists()) {
                img = argb(ImageIO.read(f));
                TEXTURES.put(id, img);
                return img;
            }
        } catch (Exception ignored) {
        }
        MISSING.add(id);
        System.out.println("[GuiPreview] no texture for " + id);
        return null;
    }

    static BufferedImage argb(BufferedImage in) {
        BufferedImage o = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.drawImage(in, 0, 0, null);
        g.dispose();
        return o;
    }

    // ================================================================== the stand-ins

    /** Textures by Identifier, read from folders. */
    public static class FakeResources extends ReloadableResourceManager {
        FakeResources() {
            super(null);
        }

        @Override
        public Optional<Resource> getResource(Identifier id) {
            File f = file(id);
            if (f == null || !f.exists()) return Optional.empty();
            return Optional.of(new Resource(null, () -> new FileInputStream(f)));
        }

        @Override
        public List<Resource> getResourceStack(Identifier id) {
            return getResource(id).map(List::of).orElse(List.of());
        }
    }

    /** The game's ascii font's advances. */
    public static class FakeFont extends Font {
        FakeFont() {
            super(null);
        }

        @Override
        public int width(String s) {
            return GuiPreview.width(s, Style.EMPTY);
        }

        @Override
        public int width(FormattedText t) {
            int[] w = {0};
            t.visit((style, s) -> {
                w[0] += GuiPreview.width(s, style);
                return Optional.empty();
            }, Style.EMPTY);
            return w[0];
        }

        @Override
        public int width(FormattedCharSequence seq) {
            int[] w = {0};
            seq.accept((i, style, cp) -> {
                w[0] += adv(cp) + (style.isBold() ? 1 : 0);
                return true;
            });
            return w[0];
        }
    }

    static int adv(int cp) {
        return cp < 256 ? ADV[cp] : 6;
    }

    static int width(String s, Style style) {
        int w = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            w += adv(cp) + (style.isBold() ? 1 : 0);
            i += Character.charCount(cp);
        }
        return w;
    }

    /** The GUI, drawn with Java2D onto an image {@code scale} times the GUI's size. */
    public static class J2D extends GuiGraphicsExtractor {
        Graphics2D g;
        int w, h;
        float scale;
        Matrix3x2fStack stack;
        Deque<Shape> clips;
        Map<Long, BufferedImage> tinted;

        J2D() {
            super(null, null, 0, 0);
        }

        public static J2D of(Graphics2D g, int w, int h, float scale) throws Exception {
            J2D j = (J2D) U.allocateInstance(J2D.class);
            j.g = g;
            j.w = w;
            j.h = h;
            j.scale = scale;
            j.stack = new Matrix3x2fStack(32);
            j.clips = new ArrayDeque<>();
            j.tinted = new HashMap<>();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            return j;
        }

        AffineTransform at() {
            Matrix3x2f m = stack;
            AffineTransform a = AffineTransform.getScaleInstance(scale, scale);
            a.concatenate(new AffineTransform(m.m00(), m.m01(), m.m10(), m.m11(), m.m20(), m.m21()));
            return a;
        }

        @Override
        public int guiWidth() {
            return w;
        }

        @Override
        public int guiHeight() {
            return h;
        }

        @Override
        public Matrix3x2fStack pose() {
            return stack;
        }

        @Override
        public void nextStratum() {}

        @Override
        public void enableScissor(int x0, int y0, int x1, int y1) {
            Shape s = at().createTransformedShape(new Rectangle2D.Float(x0, y0, x1 - x0, y1 - y0));
            if (!clips.isEmpty()) {
                java.awt.geom.Area a = new java.awt.geom.Area(clips.peek());
                a.intersect(new java.awt.geom.Area(s));
                s = a;
            }
            clips.push(s);
            g.setTransform(new AffineTransform());
            g.setClip(s);
        }

        @Override
        public void disableScissor() {
            if (!clips.isEmpty()) clips.pop();
            g.setTransform(new AffineTransform());
            g.setClip(clips.isEmpty() ? null : clips.peek());
        }

        @Override
        public void fill(int x0, int y0, int x1, int y1, int argb) {
            if ((argb >>> 24) == 0) return;
            g.setTransform(at());
            g.setColor(new Color(argb, true));
            g.fillRect(Math.min(x0, x1), Math.min(y0, y1), Math.abs(x1 - x0), Math.abs(y1 - y0));
        }

        @Override
        public void fill(RenderPipeline p, int x0, int y0, int x1, int y1, int argb) {
            fill(x0, y0, x1, y1, argb);
        }

        @Override
        public void fillGradient(int x0, int y0, int x1, int y1, int top, int bottom) {
            g.setTransform(at());
            g.setPaint(new GradientPaint(0, y0, new Color(top, true), 0, y1, new Color(bottom, true)));
            g.fillRect(Math.min(x0, x1), Math.min(y0, y1), Math.abs(x1 - x0), Math.abs(y1 - y0));
            g.setPaint(null);
        }

        @Override
        public void horizontalLine(int x0, int x1, int y, int argb) {
            fill(Math.min(x0, x1), y, Math.max(x0, x1) + 1, y + 1, argb);
        }

        @Override
        public void verticalLine(int x, int y0, int y1, int argb) {
            fill(x, Math.min(y0, y1) + 1, x + 1, Math.max(y0, y1), argb);
        }

        @Override
        public void outline(int x, int y, int w, int h, int argb) {
            fill(x, y, x + w, y + 1, argb);
            fill(x, y + h - 1, x + w, y + h, argb);
            fill(x, y + 1, x + 1, y + h - 1, argb);
            fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
        }

        @Override
        public void blit(RenderPipeline p, Identifier id, int x, int y, float u, float v, int w, int h, int tw, int th, int argb) {
            draw(id, x, y, w, h, u, v, w, h, tw, th, argb);
        }

        @Override
        public void blit(RenderPipeline p, Identifier id, int x, int y, float u, float v, int w, int h, int tw, int th) {
            draw(id, x, y, w, h, u, v, w, h, tw, th, -1);
        }

        @Override
        public void blit(RenderPipeline p, Identifier id, int x, int y, float u, float v, int w, int h, int rw, int rh, int tw, int th) {
            draw(id, x, y, w, h, u, v, rw, rh, tw, th, -1);
        }

        @Override
        public void blit(RenderPipeline p, Identifier id, int x, int y, float u, float v, int w, int h, int rw, int rh, int tw, int th, int argb) {
            draw(id, x, y, w, h, u, v, rw, rh, tw, th, argb);
        }

        @Override
        public void blit(Identifier id, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1) {
            BufferedImage img = texture(id);
            if (img == null) return;
            draw(id, x0, y0, x1 - x0, y1 - y0, u0 * img.getWidth(), v0 * img.getHeight(),
                    (int) ((u1 - u0) * img.getWidth()), (int) ((v1 - v0) * img.getHeight()), img.getWidth(), img.getHeight(), -1);
        }

        @Override
        public void blitSprite(RenderPipeline p, Identifier id, int x, int y, int w, int h) {
            draw(Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/gui/sprites/" + id.getPath() + ".png"), x, y, w, h, 0, 0, 1, 1, 1, 1, -1);
        }

        @Override
        public void blitSprite(RenderPipeline p, Identifier id, int x, int y, int w, int h, int argb) {
            draw(Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/gui/sprites/" + id.getPath() + ".png"), x, y, w, h, 0, 0, 1, 1, 1, 1, argb);
        }

        /** Draws a whole image over a GUI rect, under the pose. */
        void image(BufferedImage img, float x, float y, float w, float h) {
            g.setTransform(at());
            AffineTransform t = new AffineTransform();
            t.translate(x, y);
            t.scale(w / img.getWidth(), h / img.getHeight());
            g.drawImage(img, t, null);
        }

        void draw(Identifier id, float x, float y, float w, float h, float u, float v, float rw, float rh, float tw, float th, int argb) {
            if ((argb >>> 24) == 0 || w == 0 || h == 0) return;
            BufferedImage img = texture(id);
            if (img == null) return;
            float sx = img.getWidth() / tw, sy = img.getHeight() / th;
            int px = Math.round(u * sx), py = Math.round(v * sy);
            int pw = Math.max(1, Math.round(rw * sx)), ph = Math.max(1, Math.round(rh * sy));
            px = Math.max(0, Math.min(img.getWidth() - 1, px));
            py = Math.max(0, Math.min(img.getHeight() - 1, py));
            pw = Math.min(pw, img.getWidth() - px);
            ph = Math.min(ph, img.getHeight() - py);
            BufferedImage sub = tint(img, px, py, pw, ph, argb);
            g.setTransform(at());
            AffineTransform t = new AffineTransform();
            t.translate(x, y);
            t.scale(w / pw, h / ph);
            g.drawImage(sub, t, null);
        }

        BufferedImage tint(BufferedImage img, int px, int py, int pw, int ph, int argb) {
            long key = ((long) System.identityHashCode(img) * 31 + px) * 1_000_003L + py * 7919L + pw * 104729L + ph * 1299709L + ((long) argb << 20);
            BufferedImage hit = tinted.get(key);
            if (hit != null) return hit;
            BufferedImage out = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB);
            int[] row = new int[pw * ph];
            img.getRGB(px, py, pw, ph, row, 0, pw);
            if (argb != -1) {
                int ta = argb >>> 24, tr = argb >> 16 & 0xFF, tg = argb >> 8 & 0xFF, tb = argb & 0xFF;
                for (int i = 0; i < row.length; i++) {
                    int c = row[i];
                    int a = (c >>> 24) * ta / 255, r = (c >> 16 & 0xFF) * tr / 255, gg = (c >> 8 & 0xFF) * tg / 255, b = (c & 0xFF) * tb / 255;
                    row[i] = a << 24 | r << 16 | gg << 8 | b;
                }
            }
            out.setRGB(0, 0, pw, ph, row, 0, pw);
            if (tinted.size() > 6000) tinted.clear();
            tinted.put(key, out);
            return out;
        }

        // ------------------------------------------------------------ text

        @Override
        public void text(Font f, String s, int x, int y, int argb) {
            text(f, s, x, y, argb, true);
        }

        @Override
        public void text(Font f, String s, int x, int y, int argb, boolean shadow) {
            List<int[]> cps = new ArrayList<>();
            List<Style> styles = new ArrayList<>();
            s.codePoints().forEach(cp -> {
                cps.add(new int[]{cp});
                styles.add(Style.EMPTY);
            });
            run(cps, styles, x, y, argb, shadow);
        }

        @Override
        public void text(Font f, Component c, int x, int y, int argb) {
            text(f, c.getVisualOrderText(), x, y, argb, true);
        }

        @Override
        public void text(Font f, Component c, int x, int y, int argb, boolean shadow) {
            text(f, c.getVisualOrderText(), x, y, argb, shadow);
        }

        @Override
        public void text(Font f, FormattedCharSequence seq, int x, int y, int argb) {
            text(f, seq, x, y, argb, true);
        }

        @Override
        public void text(Font f, FormattedCharSequence seq, int x, int y, int argb, boolean shadow) {
            List<int[]> cps = new ArrayList<>();
            List<Style> styles = new ArrayList<>();
            seq.accept((i, style, cp) -> {
                cps.add(new int[]{cp});
                styles.add(style);
                return true;
            });
            run(cps, styles, x, y, argb, shadow);
        }

        @Override
        public void centeredText(Font f, String s, int x, int y, int argb) {
            text(f, s, x - f.width(s) / 2, y, argb, true);
        }

        @Override
        public void centeredText(Font f, Component c, int x, int y, int argb) {
            text(f, c, x - f.width(c) / 2, y, argb, true);
        }

        void run(List<int[]> cps, List<Style> styles, int x, int y, int argb, boolean shadow) {
            if ((argb >>> 24) == 0) return;
            if (shadow) glyphs(cps, styles, x + 1, y + 1, argb, true);
            glyphs(cps, styles, x, y, argb, false);
        }

        void glyphs(List<int[]> cps, List<Style> styles, int x, int y, int argb, boolean shadow) {
            float cx = x;
            for (int i = 0; i < cps.size(); i++) {
                int cp = cps.get(i)[0];
                Style st = styles.get(i);
                int rgb = st.getColor() != null ? st.getColor().getValue() : argb & 0xFFFFFF;
                if (shadow) rgb = (rgb >> 2) & 0x3F3F3F;
                int col = (argb & 0xFF000000) | rgb;
                if (cp < 256 && cp != ' ') {
                    Identifier asciiId = Identifier.withDefaultNamespace("textures/font/ascii.png");
                    TEXTURES.putIfAbsent(asciiId, argb(ascii));
                    draw(asciiId, cx, y, 8, 8, (cp % 16) * 8, (cp / 16) * 8, 8, 8, 128, 128, col);
                    if (st.isBold()) draw(asciiId, cx + 1, y, 8, 8, (cp % 16) * 8, (cp / 16) * 8, 8, 8, 128, 128, col);
                } else if (cp >= 256) {
                    // Outside the ascii page: a small diamond in its place, so the layout still shows.
                    fill(Math.round(cx + 1), y + 2, Math.round(cx + 4), y + 5, col);
                }
                cx += adv(cp) + (st.isBold() ? 1 : 0);
            }
        }

        // ------------------------------------------------------------ items

        static final Set<String> ITEMS_SEEN = new HashSet<>();

        /** An item as its flat icon (textures/item/<id>.png, else the block's texture) - the game's 3D block items are not. */
        @Override
        public void item(net.minecraft.world.item.ItemStack stack, int x, int y) {
            Identifier key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            Identifier tex = Identifier.fromNamespaceAndPath(key.getNamespace(), "textures/item/" + key.getPath() + ".png");
            if (texture(tex) == null) tex = Identifier.fromNamespaceAndPath(key.getNamespace(), "textures/block/" + key.getPath() + ".png");
            if (texture(tex) == null) {
                if (ITEMS_SEEN.add(key.toString())) System.out.println("[GuiPreview] no icon for " + key);
                return;
            }
            draw(tex, x, y, 16, 16, 0, 0, 16, 16, 16, 16, -1);
        }

        @Override
        public void item(net.minecraft.world.item.ItemStack stack, int x, int y, int seed) {
            item(stack, x, y);
        }
    }

    // ================================================================== sound

    static SynthEngine engine;
    static ConcurrentLinkedQueue<Voice> pending;
    static AtomicInteger silences;
    static int lastSilence;
    public record Cue(long at, boolean cut, Voice[] voices) {}
    public static final List<Cue> CUES = new ArrayList<>();

    /** Makes SbcsAudio's engine queue voices without a device, so {@link #drainAudio} can collect them. */
    public static void captureAudio() throws Exception {
        Field eng = SbcsAudio.class.getDeclaredField("ENGINE");
        eng.setAccessible(true);
        engine = (SynthEngine) eng.get(null);
        Field running = SynthEngine.class.getDeclaredField("running");
        running.setAccessible(true);
        running.set(engine, true);
        Field pf = SynthEngine.class.getDeclaredField("pending");
        pf.setAccessible(true);
        @SuppressWarnings("unchecked") ConcurrentLinkedQueue<Voice> q = (ConcurrentLinkedQueue<Voice>) pf.get(engine);
        pending = q;
        Field sf = SynthEngine.class.getDeclaredField("silenceRequests");
        sf.setAccessible(true);
        silences = (AtomicInteger) sf.get(engine);
        lastSilence = silences.get();
    }

    /** What was played since the last call, stamped {@code t} ms. */
    public static void drainAudio(long t) {
        boolean cut = silences.get() != lastSilence;
        lastSilence = silences.get();
        List<Voice> vs = new ArrayList<>();
        Voice v;
        while ((v = pending.poll()) != null) vs.add(v);
        if (cut || !vs.isEmpty()) CUES.add(new Cue(t, cut, vs.toArray(new Voice[0])));
    }

    public static void writeWav(File f, float seconds) throws Exception {
        int rate = (int) SynthEngine.SAMPLE_RATE;
        float[] mix = new float[(int) (seconds * rate) * 2];
        for (Cue c : CUES) {
            int at = (int) (c.at * rate / 1000L) * 2;
            if (at >= mix.length) continue;
            if (c.cut) Arrays.fill(mix, at, mix.length, 0f);
            float left = seconds - c.at / 1000f;
            for (Voice v : c.voices) {
                float[] one = SynthEngine.renderOffline(v, left);
                for (int i = 0; i < one.length && at + i < mix.length; i++) mix[at + i] += one[i];
            }
        }
        float peak = 0f;
        for (float v : mix) peak = Math.max(peak, Math.abs(v));
        float gain = peak > 0.98f ? 0.98f / peak : 1f;
        byte[] pcm = new byte[mix.length * 2];
        for (int i = 0; i < mix.length; i++) {
            int s = Math.round(Math.max(-1f, Math.min(1f, mix[i] * gain)) * 32767f);
            pcm[i * 2] = (byte) s;
            pcm[i * 2 + 1] = (byte) (s >> 8);
        }
        javax.sound.sampled.AudioFormat fmt = new javax.sound.sampled.AudioFormat(rate, 16, 2, true, false);
        javax.sound.sampled.AudioSystem.write(new javax.sound.sampled.AudioInputStream(new ByteArrayInputStream(pcm), fmt, mix.length / 2),
                javax.sound.sampled.AudioFileFormat.Type.WAVE, f);
        CUES.clear();
        System.out.printf("%s: %.1f s, peak %.3f%n", f.getName(), seconds, peak);
    }

    /** A frame: the background (a screenshot dimmed as ChopPreview does, or its stand-in sky), ready to draw on. */
    public static BufferedImage frame(int w, int h, int scale, BufferedImage bg) {
        BufferedImage img = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.scale(scale, scale);
        ChopPreview.background(g, bg);
        g.dispose();
        return img;
    }
}
