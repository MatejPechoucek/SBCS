package com.sbcs.detect.nucleus;

import com.google.gson.JsonObject;
import com.sbcs.detect.DecisionLog;
import com.sbcs.detect.DropRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Finds the Crystal Loot Bundle in the world, where the black box goes.
 *
 * The wiki says the bundle appears "in front of the vault opposite the Topaz Crystal statue", and
 * Hypixel's own line, right under the loot message, says "Pick it up near the Nucleus Vault". No
 * reference mod draws or describes the bundle itself, so what kind of entity it is has to come from
 * records: from the loot message on, every entity near the vault is recorded once as
 * {@code nucleus_bundle} (type, name, position, size, equipment and skins), and the likeliest one is
 * followed as {@link #spot()}. Until one is found the spot is where it lands ({@link #DROP}, from
 * Matej's first run).
 *
 * Gone when the followed entity goes (picked up), the player leaves the Nucleus, or after
 * {@link #ARMED_MS}.
 */
public final class BundleSpotter {
    private BundleSpotter() {}

    /** Skyblocker's "Nucleus" waypoint (NucleusWaypoints, 2026-10-22): the middle of the Nucleus. */
    public static final Vec3 VAULT = new Vec3(513.5, 106, 524.5);
    /**
     * Where the bundle lands. Matej's first run (2026-10-22) recorded it: a dropped item, "Player Head", at
     * 515.2 108.5 533.3, falling to 513.8 106.0 534.1 - first seen 114 s after the message, when Matej
     * walked up to it (Matej remembered "around 513, 106, 551").
     */
    public static final Vec3 DROP = new Vec3(514, 106, 534);
    /** The Crystal Nucleus zone, from Skyblocker's WishingCompassSolver. */
    public static final AABB NUCLEUS = new AABB(462, 63, 461, 564, 181, 565);

    private static final long ARMED_MS = 5 * 60_000;
    /** Wide on purpose (Matej: "make a bigger box if necessary"); the closest candidate wins. */
    private static final double SEARCH = 20;

    private static long armedAt;
    private static Vec3 spot;
    /** The followed entity's box in the world, or null while only the vault spot is known. */
    private static AABB box;
    private static int followed = -1;
    private static String followedName = "";
    private static final Set<Integer> recorded = new HashSet<>();
    /** What stood near the vault before the message: decorations, not the bundle - unless named as one. */
    private static final Set<Integer> before = new HashSet<>();

    /** The loot message came: the bundle is about to appear. */
    public static void arm(LocalPlayer player) {
        armedAt = System.currentTimeMillis();
        spot = DROP;
        box = null;
        followed = -1;
        followedName = "";
        recorded.clear();
        before.clear();
        ClientLevel level = Minecraft.getInstance().level;
        if (player != null && level != null) {
            for (Entity e : level.getEntities(player, AABB.ofSize(DROP, SEARCH * 2, SEARCH * 2, SEARCH * 2))) {
                before.add(e.getId());
            }
        }
        if (player != null) {
            JsonObject o = new JsonObject();
            o.addProperty("event", "armed");
            o.addProperty("already_there", before.size());
            o.addProperty("player", pos(player.position()));
            DropRecorder.record("nucleus_bundle", o);
        }
    }

    /** The bundle was picked up, or anything else ended the wait. */
    public static void disarm(String why, DecisionLog log, LocalPlayer player) {
        if (armedAt == 0) return;
        if (player != null) {
            JsonObject o = new JsonObject();
            o.addProperty("event", why);
            o.addProperty("player", pos(player.position()));
            if (spot != null) o.addProperty("spot", pos(spot));
            if (followed >= 0) o.addProperty("followed", followedName);
            DropRecorder.record("nucleus_bundle", o);
        }
        if (log != null) log.note("§8Nucleus: the bundle's box is gone §8(" + why + ")");
        armedAt = 0;
        spot = null;
        box = null;
        followed = -1;
        recorded.clear();
    }

    /** Where the bundle is now, or null when nothing is waiting. */
    public static Vec3 spot() { return spot; }

    public static boolean armed() { return armedAt != 0; }

    /**
     * What the black box covers: the bundle's own bounding box once it is followed, null until
     * then. Not the vault spot: a box at a guessed place would cover nothing and mark a wrong one.
     */
    public static AABB cover() { return box; }

    public static void tick(LocalPlayer player, DecisionLog log) {
        if (armedAt == 0) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (player == null || level == null) {
            disarm("left the world", log, null);
            return;
        }
        if (System.currentTimeMillis() - armedAt > ARMED_MS) {
            disarm("not picked up in five minutes", log, player);
            return;
        }
        if (!NUCLEUS.contains(player.position())) {
            disarm("left the Nucleus", log, player);
            return;
        }
        AABB near = AABB.ofSize(DROP, SEARCH * 2, SEARCH * 2, SEARCH * 2);
        Entity best = null;
        int bestScore = 0;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : level.getEntities(player, near)) {
            if (e instanceof net.minecraft.world.entity.player.Player) continue;
            if (recorded.add(e.getId())) record(e);
            int score = score(e);
            if (score < 4 && before.contains(e.getId())) score = 0;
            double dist = e.position().distanceToSqr(DROP);
            if (score > bestScore || (score == bestScore && score > 0 && dist < bestDist)) {
                bestScore = score;
                bestDist = dist;
                best = e;
            }
        }
        if (best != null) {
            spot = best.position();
            box = coverOf(best);
            if (best.getId() != followed) {
                followed = best.getId();
                followedName = describe(best);
                if (log != null) log.note("§7Nucleus: following " + followedName + " as the bundle");
            }
        } else if (followed >= 0) {
            disarm("the bundle went - picked up", log, player);
        }
    }

    /**
     * The world box the black cover has to hide. An entity's own bounding box, except for an armour stand:
     * Hypixel's are invisible markers with no size at all, and what shows is the head they hold, drawn at
     * the hand - up to a block and a half above the feet and a third of a block to the side, either side
     * as the stand turns. So the box takes in everything an arm could reach.
     */
    private static AABB coverOf(Entity e) {
        if (e instanceof net.minecraft.world.entity.decoration.ArmorStand stand) {
            double s = stand.isSmall() ? 0.5 : 1.0;
            Vec3 p = stand.position();
            double r = 1.1 * s;
            return new AABB(p.x - r, p.y, p.z - r, p.x + r, p.y + 2.3 * s, p.z + r);
        }
        return e.getBoundingBox();
    }

    /** Whether any of a living entity's slots holds a player head - the bundle is one, held in a hand. */
    private static boolean holdsHead(net.minecraft.world.entity.LivingEntity le) {
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            net.minecraft.world.item.ItemStack it = le.getItemBySlot(slot);
            if (it != null && it.is(net.minecraft.world.item.Items.PLAYER_HEAD)) return true;
        }
        return false;
    }

    /**
     * How much an entity looks like the bundle. Named "bundle" or "loot" is near certain; a dropped
     * item or a head-wearing armour stand by the vault is a guess. 0 is not a candidate.
     */
    private static int score(Entity e) {
        String name = e.getCustomName() == null ? "" : e.getCustomName().getString().toLowerCase(Locale.ROOT);
        if (name.contains("bundle")) return 5;
        if (name.contains("loot")) return 4;
        if (e instanceof net.minecraft.world.entity.item.ItemEntity item) {
            String n = item.getItem().getHoverName().getString().toLowerCase(Locale.ROOT);
            return n.contains("bundle") || n.contains("loot") ? 5 : 2;
        }
        if (e instanceof net.minecraft.world.entity.decoration.ArmorStand stand && holdsHead(stand)) {
            return 2;
        }
        return 0;
    }

    private static String describe(Entity e) {
        String type = String.valueOf(net.minecraft.world.entity.EntityType.getKey(e.getType()));
        String name = e.getCustomName() == null ? "" : " \"" + e.getCustomName().getString() + "\"";
        return type + name + " at " + pos(e.position());
    }

    private static void record(Entity e) {
        JsonObject o = new JsonObject();
        o.addProperty("event", "entity");
        o.addProperty("type", String.valueOf(net.minecraft.world.entity.EntityType.getKey(e.getType())));
        o.addProperty("pos", pos(e.position()));
        o.addProperty("width", Math.round(e.getBbWidth() * 100) / 100.0);
        o.addProperty("height", Math.round(e.getBbHeight() * 100) / 100.0);
        o.addProperty("invisible", e.isInvisible());
        o.addProperty("seconds", (System.currentTimeMillis() - armedAt) / 1000.0);
        o.addProperty("already_there", before.contains(e.getId()));
        if (e.getCustomName() != null) o.addProperty("custom", e.getCustomName().getString());
        if (e instanceof net.minecraft.world.entity.item.ItemEntity item) {
            o.addProperty("item", item.getItem().getHoverName().getString());
        }
        if (e instanceof net.minecraft.world.entity.LivingEntity le) {
            for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                net.minecraft.world.item.ItemStack it = le.getItemBySlot(slot);
                if (it == null || it.isEmpty()) continue;
                String desc = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it.getItem()));
                String skin = com.sbcs.render.ScathaSkins.texturesProperty(it);
                o.addProperty(slot.getName(), skin == null ? desc : desc + " skin=" + skin);
            }
        }
        DropRecorder.record("nucleus_bundle", o);
    }

    private static String pos(Vec3 p) {
        return String.format(Locale.ROOT, "%.1f %.1f %.1f", p.x, p.y, p.z);
    }
}
