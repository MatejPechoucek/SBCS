package com.sbcs.detect.nucleus;

import com.google.gson.JsonObject;
import com.sbcs.detect.DecisionLog;
import com.sbcs.detect.DropRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Where the black box over the Crystal Loot Bundle goes: one fixed box by the Nucleus Vault, up from the
 * loot message (the fifth crystal) until the Vault animation is done ({@code SbcsClient.announceNucleus}), with
 * leaving the Nucleus and {@link #ARMED_MS} as fallbacks.
 *
 * Matej (2026-10-29): "the box that I gave is static. It should never try to find any armor stands, it
 * should just always be that box. it doesnt matter if its too big". Until then this followed whichever
 * entity by the vault looked most like the bundle, and twice it followed the wrong one - a dropped head by
 * the Topaz statue (2026-10-22) and a head stand at 524 109 528 (2026-10-28) - while the loot itself, a
 * cluster of invisible stands at x 510.5..514.5, y 105.5, z 548.5..553.5, went uncovered.
 */
public final class BundleSpotter {
    private BundleSpotter() {}

    /** Where the loot is, from Matej: "around 513 106 551". */
    public static final Vec3 DROP = new Vec3(513, 106, 551);
    /**
     * The cover, fixed: 10 x 3 x 10 blocks centred on {@link #DROP} (Matej, 2026-10-29: "make it a 10x10x3 box
     * centered at those coordinates"). The loot stands and what they hold reached x 509.4..515.6,
     * y 105.3..107.8, z 547.4..554.6 on 2026-10-28.
     */
    public static final AABB BOX = AABB.ofSize(DROP, 10, 3, 10);
    /** The Crystal Nucleus zone, from Skyblocker's WishingCompassSolver. */
    public static final AABB NUCLEUS = new AABB(462, 63, 461, 564, 181, 565);

    private static final long ARMED_MS = 5 * 60_000;

    private static long armedAt;

    /** The loot message came: the bundle is about to appear. */
    public static void arm(LocalPlayer player) {
        armedAt = System.currentTimeMillis();
        if (player != null) {
            JsonObject o = new JsonObject();
            o.addProperty("event", "armed");
            o.addProperty("player", pos(player.position()));
            DropRecorder.record("nucleus_bundle", o);
        }
    }

    /** The wait is over. */
    public static void disarm(String why, DecisionLog log, LocalPlayer player) {
        if (armedAt == 0) return;
        if (player != null) {
            JsonObject o = new JsonObject();
            o.addProperty("event", why);
            o.addProperty("player", pos(player.position()));
            DropRecorder.record("nucleus_bundle", o);
        }
        if (log != null) log.note("§8Nucleus: the bundle's box is gone §8(" + why + ")");
        armedAt = 0;
    }

    /** Where the bundle is, or null when nothing is waiting. */
    public static Vec3 spot() { return armedAt != 0 ? DROP : null; }

    public static boolean armed() { return armedAt != 0; }

    /** What the black box covers: {@link #BOX} while armed, null otherwise. */
    public static AABB cover() { return armedAt != 0 ? BOX : null; }

    public static void tick(LocalPlayer player, DecisionLog log) {
        if (armedAt == 0) return;
        if (player == null || Minecraft.getInstance().level == null) {
            disarm("left the world", log, null);
            return;
        }
        if (System.currentTimeMillis() - armedAt > ARMED_MS) {
            disarm("five minutes up", log, player);
            return;
        }
        if (!NUCLEUS.contains(player.position())) {
            disarm("left the Nucleus", log, player);
        }
    }

    private static String pos(Vec3 p) {
        return String.format(Locale.ROOT, "%.1f %.1f %.1f", p.x, p.y, p.z);
    }
}
