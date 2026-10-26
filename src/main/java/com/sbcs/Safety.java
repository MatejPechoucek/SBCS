package com.sbcs;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The fail-safes: nothing an animation does may take the game down or leave it broken.
 *
 * <ul>
 *   <li><b>Errors</b> ({@link #failed}): an animation, pop-up or detector that throws is stopped there, the screen
 *   handed back (HUD, chat, sound), the error logged and said once in chat. Three in a session and that animation is
 *   off until the game restarts ({@link #on}).</li>
 *   <li><b>Hypixel only</b> ({@link #onHypixel}): detection runs only on hypixel.net - a lookalike nametag or chat line
 *   on another server or in singleplayer opens nothing. "Try it" and the menu work anywhere.</li>
 *   <li><b>Spam</b> ({@link #allowOpen}): a source opening more than {@link #BURST} real cutscenes in
 *   {@link #BURST_MS} is a detection bug; the extras are refused and logged.</li>
 *   <li><b>Slow frames</b> ({@link #frame}): a cutscene averaging more than {@link #SLOW_NS} a frame over a second
 *   turns to calm effects and drops the gameplay window until it ends.</li>
 *   <li><b>Hooks</b> ({@link #hook}): the optional mixins say when they first run, so {@code /sbcs log} can tell
 *   which of them never did.</li>
 * </ul>
 * The watchdog and {@code /sbcs stop} are in SbcsClient ({@code watchdog}, {@code stopAll}).
 */
public final class Safety {
    private Safety() {}

    /** Errors before an animation is switched off for the session. */
    public static final int STRIKES = 3;

    private static final Map<String, Integer> errors = new HashMap<>();
    private static final Set<String> off = new HashSet<>();

    /** Whether this animation (by its stats key: scatha, vanguard, diana, blaze, seraph, lava, shark, ghost, ledger)
     *  may run - not switched off this session after repeated errors. */
    public static boolean on(String key) {
        return !off.contains(key);
    }

    /** Something of {@code key} threw: log it, stop it, hand the screen back, and say so once. */
    public static void failed(String key, Throwable t) {
        int n = errors.merge(key, 1, Integer::sum);
        SbcsClient.LOGGER.error("[SBCS] {} hit an error ({} of {} this session)", key, n, STRIKES, t);
        boolean dead = n >= STRIKES && off.add(key);
        SbcsClient.decisions.note("§cSafety: " + name(key) + " hit an error: " + t
                + (dead ? " - switched off until restart" : ""));
        try {
            SbcsClient.stopAll(false);
        } catch (Throwable again) {
            SbcsClient.LOGGER.error("[SBCS] could not stop cleanly after the error", again);
        }
        if (n == 1 || dead) {
            say("§c" + name(key) + " hit an error and stopped." + (dead
                    ? " It is switched off until you restart the game."
                    : " Play goes on - /sbcs log has the details."));
        }
    }

    /** Runs {@code r}; if it throws, {@link #failed} - the caller carries on. */
    public static void run(String key, Runnable r) {
        if (!on(key)) return;
        try {
            r.run();
        } catch (Throwable t) {
            failed(key, t);
        }
    }

    /** One line in chat from the mod, whatever the chat-summary setting says. */
    public static void say(String line) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal("§6[SBCS] §f" + line));
    }

    static String name(String key) {
        return switch (key) {
            case "scatha" -> "The Scatha's cutscene";
            case "vanguard" -> "The Thaw";
            case "diana" -> "The Chopping Block";
            case "blaze" -> "Hellion";
            case "seraph" -> "The Seraph's Glyph";
            case "zombie" -> "Exhumed";
            case "spider" -> "The Brood";
            case "slayer" -> "The slayer quest tracker";
            case "lava" -> "Eruption";
            case "shark" -> "Fin";
            case "ghost" -> "Wisp";
            case "nucleus" -> "The Vault";
            case "superpairs" -> "The Hunt";
            case "ledger" -> "The Ledger's pop-up";
            case "dungeon" -> "The Ossuary";
            default -> "SBCS (" + key + ")";
        };
    }

    // ------------------------------------------------------------------ Hypixel only

    private static long checkedAt;
    private static boolean hypixel;

    /** Whether the game is connected to Hypixel - asked every tick, worked out once a second. A development run is
     *  always "on Hypixel", so detection can be tried in singleplayer there. */
    public static boolean onHypixel() {
        long now = System.currentTimeMillis();
        if (now - checkedAt < 1000) return hypixel;
        checkedAt = now;
        boolean dev = net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
        var server = Minecraft.getInstance().getCurrentServer();
        hypixel = dev || (server != null && isHypixelAddress(server.ip));
        return hypixel;
    }

    /**
     * Whether a server address is Hypixel's: hypixel.net or a name under it ("mc.hypixel.net"), with or without a
     * port. Only the host counts - "hypixel.net.example.com" or "nothypixel.net" merely contain the words.
     */
    public static boolean isHypixelAddress(String address) {
        if (address == null) return false;
        String host = address.strip().toLowerCase(Locale.ROOT);
        int colon = host.lastIndexOf(':');
        if (colon >= 0 && host.indexOf(':') == colon) host = host.substring(0, colon);
        if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        return host.equals("hypixel.net") || host.endsWith(".hypixel.net");
    }

    // ------------------------------------------------------------------ spam

    public static final int BURST = 3;
    public static final long BURST_MS = 10_000;
    private static final Map<String, ArrayDeque<Long>> opens = new HashMap<>();

    /** A real cutscene of {@code key} wants to open: false if it is the (BURST+1)th inside BURST_MS. */
    public static boolean allowOpen(String key) {
        long now = System.currentTimeMillis();
        ArrayDeque<Long> q = opens.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (!q.isEmpty() && now - q.peekFirst() > BURST_MS) q.pollFirst();
        if (q.size() >= BURST) {
            SbcsClient.decisions.note("§cSafety: " + name(key) + " tried to open " + (q.size() + 1)
                    + " times in " + BURST_MS / 1000 + " s - refused (a detection bug?)");
            return false;
        }
        q.addLast(now);
        return true;
    }

    // ------------------------------------------------------------------ slow frames

    /** A cutscene frame over this, on average, is slow. */
    public static final long SLOW_NS = 50_000_000L;
    private static long frameSum, frameCount, frameSince;
    private static boolean slow;
    private static Object frameOwner;

    /** One cutscene frame took {@code nanos}; {@code owner} is the cutscene (a new one starts afresh). */
    public static void frame(Object owner, long nanos) {
        long now = System.currentTimeMillis();
        if (owner != frameOwner) {
            frameOwner = owner;
            slow = false;
            frameSum = frameCount = 0;
            frameSince = now;
        }
        frameSum += nanos;
        frameCount++;
        if (now - frameSince >= 1000) {
            if (!slow && frameCount >= 5 && frameSum / frameCount > SLOW_NS) {
                slow = true;
                SbcsClient.decisions.note("§eSafety: frames averaged " + frameSum / frameCount / 1_000_000
                        + " ms - calm effects, no gameplay window, until this cutscene ends");
            }
            frameSum = frameCount = 0;
            frameSince = now;
        }
    }

    /** The cutscene playing now is running slowly: calm effects, no gameplay window. */
    public static boolean slow() {
        return slow;
    }

    // ------------------------------------------------------------------ hooks

    /** The optional hooks (mixins with require = 0): their feature is lost, not the game, if one cannot apply. */
    public static final String[] OPTIONAL_HOOKS = {"dropped items", "item pickups", "HUD hidden for other mods",
            "dungeon chest cover", "dungeon roll drawing", "superpairs tile cover", "superpairs hunt drawing"};
    private static final boolean[] hooked = new boolean[OPTIONAL_HOOKS.length];

    /** An optional mixin's handler ran, so it is in. On hot paths (every entity, every frame): one array read. */
    public static void hook(int i) {
        if (hooked[i]) return;
        hooked[i] = true;
        SbcsClient.decisions.note("§8Safety: hook \"" + OPTIONAL_HOOKS[i] + "\" is active");
    }

    /** Which optional hooks have been seen running. One that never ran may only not have been needed yet (no item
     *  pickup since the game started) - or it did not apply to this version of the game, and only its feature is gone. */
    public static String hookReport() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < OPTIONAL_HOOKS.length; i++) {
            if (b.length() > 0) b.append(", ");
            b.append(OPTIONAL_HOOKS[i]).append(hooked[i] ? " §aactive§7" : " §enot seen yet§7");
        }
        return b.toString();
    }

    /** Animations switched off this session, for /sbcs log. */
    public static String offReport() {
        return off.isEmpty() ? "none" : String.join(", ", off);
    }
}
