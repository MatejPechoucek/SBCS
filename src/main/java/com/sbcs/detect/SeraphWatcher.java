package com.sbcs.detect;

import com.sbcs.anim.SeraphTimeline.Drop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The player's own tier IV Voidgloom Seraph: its kill opens the Seraph's Glyph, its drop line fills the outcome in.
 *
 * Written against a friend's real session (2026-10-05, 0.2.69: three of their own tier IV kills among a crowd of
 * other players' bosses). What it settled:
 *
 * <ul>
 *   <li><b>Ownership is the boss's own "Spawned by: &lt;player&gt;" nametag</b> - an armour stand two entity ids after
 *       the boss's, seen on the same tick. (The Demonlord never showed one; the Seraph always does.) There is no
 *       "SLAYER BOSS!" line for it.</li>
 *   <li><b>The nametag</b>: {@code ☠ Voidgloom Seraph IV 208.6M❤ ✯} with health, {@code ... 100 Hits} through the
 *       hitshield (no health to read - the last is kept), a {@code ✯} or {@code ᛤ} marker either side. It stays the
 *       same entity through the fight.</li>
 *   <li><b>The kill</b>: it reads {@code 0❤}; about 1.2 s later the drop line, a third of a second after that
 *       {@code SLAYER QUEST COMPLETE!}. So it opens on the 0 with the drop pending, the first win resolves it, QUEST
 *       COMPLETE with none (or 3 s) makes it a bust. A kill whose 0 was missed (the second one in the records went
 *       straight from 10.7M to gone) is taken from the nametag going at 5% or less, close, or from QUEST COMPLETE.</li>
 * </ul>
 *
 * Tiers III and IV (0.3.x, Matej's own records of 2026-10-25: he fights tier III, {@code [Lv534]  Voidgloom Seraph III
 * 0/50M❤} - current / max - with "Spawned by: qMighty" under it and the drop line ~0.3 s after the 0). A tier III can
 * drop the Summoning Eye and the Byzantium Dye; everything else the animation plays is tier IV's. The maximum comes
 * off the nametag when it shows one, else 50M / 210M by tier.
 *
 * <p>In those records not one of his seventeen bosses had a "Spawned by" tag the recorder caught, so the quest's own
 * line {@code SLAYER BOSS! The Voidgloom Seraph III spawned!} also claims one, as Hellion's does: the first nametag of
 * that tier seen within {@link #CLAIM_REACH} blocks, from 2 s before the line to 5 s after it (in the records it came
 * 50 ms after, 3 blocks away). Every decision is narrated.
 */
public final class SeraphWatcher {

    public interface Listener {
        /** Our boss died: open the Seraph's Glyph with the outcome pending. */
        void kill();

        /** The pending kill's outcome: a win, or {@link Drop#BUST}. */
        void resolve(Drop drop, String line);

        /** A kill the nametag missed, its outcome already known. */
        void killKnown(Drop drop, String line);

        /** Our boss has appeared (the prices of the floored drops are asked for then). */
        default void spawned() {
        }
    }

    private final DecisionLog log;
    private final Listener listener;
    /** The player's own name, as "Spawned by:" shows it. Set by the client (or the test). */
    public String self = "";

    public SeraphWatcher(DecisionLog log, Listener listener) {
        this.log = log;
        this.listener = listener;
    }

    /** The quest's boss (the client's, shared by every slayer watcher): another boss's drop lines are never the Seraph's. */
    public SlayerQuest quest = new SlayerQuest();

    static final String NAME = "Voidgloom Seraph ";
    private static final java.util.regex.Pattern TIER = java.util.regex.Pattern.compile("Voidgloom Seraph (III|IV)(?![IV])");
    private static final java.util.regex.Pattern MAX = java.util.regex.Pattern.compile("/\\s*([0-9]+(?:\\.[0-9]+)?)([kKmMbB]?)");

    /** 3 or 4 for a Seraph nametag the animation plays for, else 0 (tiers I and II drop nothing it plays). */
    static int tierOf(String name) {
        java.util.regex.Matcher m = TIER.matcher(name);
        return !m.find() ? 0 : m.group(1).equals("IV") ? 4 : 3;
    }

    /** The maximum health a nametag shows after its "/", or -1. */
    static double maxOf(String name) {
        java.util.regex.Matcher m = MAX.matcher(name);
        if (!m.find()) return -1;
        double v = Double.parseDouble(m.group(1));
        switch (m.group(2).toLowerCase(Locale.ROOT)) {
            case "k" -> v *= 1e3;
            case "m" -> v *= 1e6;
            case "b" -> v *= 1e9;
            default -> { }
        }
        return v;
    }

    /** The tier of the player's last Seraph (4 until one has been seen), for the odds the screen shows. */
    public volatile int tier = 4;
    private static final double SCAN_RADIUS = 48, TELEPORT = 12, GONE_REACH = 24, ARM_RADIUS = 24;
    private static final long DROP_WAIT_MS = 3_000, ARM_MS = 2_500, MIN_SEEN_MS = 300;

    private static final class Boss {
        int id;
        long firstSeen;
        boolean owned, dead;
        double hp = -1, distSq, max;
        int tier;
        String owner;
    }

    private final Map<Integer, Boss> bosses = new HashMap<>();
    private static final double CLAIM_REACH = 16;
    private static final long CLAIM_BEFORE_MS = 2_000, CLAIM_AFTER_MS = 5_000;
    private long spawnLineAt = -1;
    private int spawnLineTier;
    private long pendingSince = -1;
    private boolean killedThisQuest, oursThisQuest;
    private Drop bestDrop;
    private String bestLine;
    private long bestAt = -1;
    private Vec3 lastPos;

    // ------------------------------------------------------------------ chat

    public void onChatLine(String text) {
        onChatLine(text, System.currentTimeMillis());
    }

    public void onChatLine(String text, long now) {
        if (text == null) return;
        String s = DianaWatcher.plain(text).trim();
        if (ChatLines.fromPlayer(s)) return;
        quest.onChatLine(s, now); // idempotent: the client feeds it too, so a replay needs nothing else
        if (s.startsWith("SLAYER QUEST STARTED!")) {
            forget();
            return;
        }
        if (s.startsWith("SLAYER QUEST FAILED!") || s.startsWith("Your Slayer Quest has been cancelled")) {
            if (oursThisQuest) log.note("§8Seraph: the quest ended without a kill (" + s + ")");
            forget();
            return;
        }
        if (s.startsWith("SLAYER BOSS!") && s.contains("spawned")) {
            int t = tierOf(s);
            if (t != 0) {
                spawnLineAt = now;
                spawnLineTier = t;
                claimBySpawnLine(now);
            }
            return;
        }
        Drop d = SeraphDrops.of(s);
        if (d != null && quest.otherThan(SlayerQuest.Boss.VOIDGLOOM, now)) {
            log.note("§8Seraph: drop line during a " + quest.boss(now) + " quest - not the Seraph's (\"" + s + "\")");
            return;
        }
        if (d != null) {
            boolean better = bestDrop == null || bestDrop == Drop.BUST || (d != Drop.BUST && d.ordinal() > bestDrop.ordinal());
            if (better || now - bestAt > DROP_WAIT_MS) {
                bestDrop = d;
                bestLine = s;
                bestAt = now;
            }
            log.note("§7Seraph: drop line - " + (d == Drop.BUST ? "not one the animation plays" : d.title) + " (\"" + s + "\")");
            if (pendingSince >= 0 && d.win()) {
                pendingSince = -1;
                listener.resolve(d, s);
            }
            return;
        }
        if (s.startsWith("SLAYER QUEST COMPLETE!")) {
            if (pendingSince >= 0) {
                pendingSince = -1;
                log.note("§7Seraph: quest complete with no drop the animation plays - bust");
                listener.resolve(Drop.BUST, s);
            } else if (oursThisQuest && !killedThisQuest) {
                Drop known = bestDrop != null && now - bestAt <= DROP_WAIT_MS ? bestDrop : Drop.BUST;
                log.note("§aSeraph: quest complete - your Seraph died out of sight; " + (known.win() ? known.title : "bust"));
                listener.killKnown(known, bestLine);
            }
            forget();
        }
    }

    // ------------------------------------------------------------------ the world

    /** @param hidePct the pre-kill hide, as a percentage of the boss's maximum (0 off) */
    public void tick(LocalPlayer player, double hidePct) {
        long now = System.currentTimeMillis();
        if (player != null) self = player.getGameProfile().name();
        scan(player, now);
        arm(hidePct / 100.0, now);
        afterTick(now);
    }

    public void afterTick(long now) {
        if (pendingSince >= 0 && now - pendingSince > DROP_WAIT_MS) {
            pendingSince = -1;
            log.note("§7Seraph: no win in " + DROP_WAIT_MS / 1000 + " s after the kill - bust");
            listener.resolve(Drop.BUST, null);
        }
    }

    /** One nametag this tick: a tier III or IV Seraph's, or a "Spawned by:" one. */
    public record Tag(int id, String name, double distSq) {
    }

    private void scan(LocalPlayer player, long now) {
        ClientLevel level = Minecraft.getInstance().level;
        if (player == null || level == null) {
            bosses.clear();
            lastPos = null;
            return;
        }
        Vec3 pos = player.position();
        boolean teleported = lastPos != null && pos.distanceToSqr(lastPos) > TELEPORT * TELEPORT;
        lastPos = pos;
        java.util.List<Tag> tags = new java.util.ArrayList<>();
        AABB box = AABB.ofSize(pos, SCAN_RADIUS * 2, SCAN_RADIUS * 2, SCAN_RADIUS * 2);
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, box)) {
            Component custom = stand.getCustomName();
            if (custom == null) continue;
            String name = custom.getString();
            if (tierOf(name) == 0 && !name.startsWith("Spawned by:")) continue;
            tags.add(new Tag(stand.getId(), name, stand.distanceToSqr(player)));
        }
        observe(tags, teleported, now);
    }

    /** The pure step: every Seraph and "Spawned by" nametag present this tick. The test replays recorded ones. */
    public void observe(List<Tag> tags, boolean teleported, long now) {
        if (teleported && !bosses.isEmpty()) log.note("§8Seraph: you teleported - nametags dropped");
        Map<Integer, String> owners = new HashMap<>();
        for (Tag tag : tags) {
            if (tag.name().startsWith("Spawned by:")) owners.put(tag.id(), tag.name().substring("Spawned by:".length()).trim());
        }
        Set<Integer> seen = new HashSet<>();
        for (Tag tag : tags) {
            String name = tag.name();
            int tierHere = tierOf(name);
            if (tierHere == 0) continue;
            seen.add(tag.id());
            Boss b = bosses.get(tag.id());
            if (b == null) {
                b = new Boss();
                b.id = tag.id();
                b.firstSeen = now;
                b.tier = tierHere;
                b.max = tierHere == 4 ? 210e6 : 50e6;
                bosses.put(b.id, b);
            }
            double shownMax = maxOf(name);
            if (shownMax > 0) b.max = shownMax;
            b.distSq = tag.distSq();
            // Its owner's tag is right after it (two ids on; one either side is taken too, to be safe).
            if (b.owner == null) {
                for (int off : new int[]{2, 1, 3}) {
                    String o = owners.get(tag.id() + off);
                    if (o != null) {
                        b.owner = o;
                        break;
                    }
                }
                if (b.owner != null && !self.isEmpty() && b.owner.toLowerCase(Locale.ROOT).equals(self.toLowerCase(Locale.ROOT)) && !b.dead) {
                    b.owned = true;
                    oursThisQuest = true;
                    tier = b.tier;
                    log.note("§7Seraph: your Voidgloom Seraph " + (b.tier == 4 ? "IV" : "III") + " - spawned by you, " + Math.round(Math.sqrt(b.distSq)) + " blocks away");
                    listener.spawned();
                }
            }
            Double hp = DianaWatcher.hpOf(name);
            if (hp != null) b.hp = hp;
            if (!b.owned && spawnLineAt >= 0) claimBySpawnLine(now);
            if (b.owned && !b.dead && hp != null && hp <= 0) {
                b.dead = true;
                killed("its nametag reads 0", now);
            }
        }
        for (Iterator<Boss> it = bosses.values().iterator(); it.hasNext(); ) {
            Boss b = it.next();
            if (seen.contains(b.id)) continue;
            it.remove();
            if (!b.owned || b.dead || teleported) continue;
            if (now - b.firstSeen >= MIN_SEEN_MS && b.distSq <= GONE_REACH * GONE_REACH && b.hp >= 0 && b.hp <= b.max * 0.05) {
                b.dead = true;
                killed("its nametag went at " + fmt(b.hp), now);
            } else {
                log.note("§8Seraph: your Seraph's nametag went at " + (b.hp < 0 ? "?" : fmt(b.hp)) + " - not a kill");
            }
        }
    }

    /** The quest's "SLAYER BOSS!" line: the closest unclaimed nametag of its tier that appeared around it is ours. */
    private void claimBySpawnLine(long now) {
        if (spawnLineAt < 0) return;
        if (now - spawnLineAt > CLAIM_AFTER_MS) {
            log.note("§8Seraph: no tier " + (spawnLineTier == 4 ? "IV" : "III") + " nametag near you within " + CLAIM_AFTER_MS / 1000 + " s of the spawn line");
            spawnLineAt = -1;
            return;
        }
        Boss best = null;
        for (Boss b : bosses.values()) {
            if (b.owned || b.dead || b.tier != spawnLineTier || b.distSq > CLAIM_REACH * CLAIM_REACH) continue;
            if (b.firstSeen < spawnLineAt - CLAIM_BEFORE_MS || b.firstSeen > spawnLineAt + CLAIM_AFTER_MS) continue;
            if (b.owner != null && !b.owner.equalsIgnoreCase(self)) continue;
            if (best == null || b.distSq < best.distSq) best = b;
        }
        if (best == null) return;
        spawnLineAt = -1;
        best.owned = true;
        oursThisQuest = true;
        tier = best.tier;
        log.note("§7Seraph: your Voidgloom Seraph " + (best.tier == 4 ? "IV" : "III") + " - the spawn line's, " + Math.round(Math.sqrt(best.distSq)) + " blocks away");
        listener.spawned();
    }

    private void killed(String how, long now) {
        killedThisQuest = true;
        if (bestDrop != null && bestDrop.win() && now - bestAt <= 1_500) {
            log.note("§aSeraph: your Seraph died - " + how + "; it dropped " + bestDrop.title);
            listener.killKnown(bestDrop, bestLine);
            return;
        }
        log.note("§aSeraph: your Seraph died - " + how + "; opening, waiting for the drop");
        pendingSince = now;
        bestDrop = null;
        bestLine = null;
        listener.kill();
    }

    private void forget() {
        spawnLineAt = -1;
        oursThisQuest = false;
        killedThisQuest = false;
        for (Boss b : bosses.values()) b.owned = false;
    }

    // ------------------------------------------------------------------ the pre-kill hide

    private volatile long armedUntil;

    /** Whether your Seraph is nearly dead: the HUD and chat fade (Diana's rule). Cheap, any thread. */
    public boolean armed() {
        return System.currentTimeMillis() < armedUntil;
    }

    private void arm(double belowFrac, long now) {
        if (belowFrac <= 0) return;
        for (Boss b : bosses.values()) {
            if (!b.owned || b.dead || b.distSq > ARM_RADIUS * ARM_RADIUS) continue;
            if (b.hp < 0 || b.hp > b.max * belowFrac) continue;
            if (now >= armedUntil) log.note("§7Seraph: your Seraph is at " + fmt(b.hp) + " - hiding the HUD");
            armedUntil = now + ARM_MS;
        }
    }

    private static String fmt(double hp) {
        return hp >= 1e6 ? Math.round(hp / 1e5) / 10.0 + "M" : String.valueOf(Math.round(hp));
    }
}
