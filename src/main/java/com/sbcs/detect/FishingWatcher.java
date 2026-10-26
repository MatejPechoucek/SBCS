package com.sbcs.detect;

import com.sbcs.anim.WaterTimeline.Creature;
import com.sbcs.anim.WaterTimeline.Drop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The water mythics (the Titanoboa, the Wiki Tiki - {@link WaterDrops#ROWS}): a kill of one that is ours opens the
 * water cutscene with the outcome pending, and its drop line fills it in. LavaWatcher's rules, which real lava records
 * proved (2026-10-05), on a creature table instead of a class per creature.
 *
 * <b>Unverified: no Titanoboa or Wiki Tiki nametag or line has been recorded yet</b> (they are now, as
 * {@code water_mob} and {@code fishing_line}). The rules:
 *
 * <ul>
 *   <li><b>Ours</b>: the creature's nametag first seen within {@link #CLAIM_REACH} blocks from {@link #CLAIM_BEFORE_MS}
 *       before to {@link #CLAIM_AFTER_MS} after its catch line. One catch line claims one nametag - if every one of the
 *       Titanoboa's segments carries its health, only the first is followed as ours. Anyone else's creature plays
 *       <b>only when a win of its own comes to us</b> by loot share (its death, the share line, its drop, within
 *       {@link #SHARE_MS}). Only our own arm the pre-kill hide.</li>
 *   <li><b>Followed through immunity</b>: the Wiki Tiki cannot be hurt while its Laser Totems stand; like Jawbus
 *       floating up, its nametag may drop its health then, and a followed entity stays followed whatever its name says.
 *       A Laser Totem's nametag is never the Tiki's.</li>
 *   <li><b>The kill</b>: its HP reads 0, or its nametag goes at {@link #GONE_FRACTION} of its health or less, close,
 *       with no teleport, or a charm (both are Elusive).</li>
 *   <li><b>The drop</b>: a win within {@link #DROP_WAIT_MS} resolves it; none, and it is a bust. A win up to
 *       {@link #EARLY_MS} before the death is the outcome already.</li>
 * </ul>
 *
 * Every decision is narrated into the DecisionLog.
 */
public final class FishingWatcher {

    public interface Listener {
        /** A creature of ours died: open with the outcome pending. */
        void kill(Creature creature, String why);

        /** The pending kill's outcome: a win, or {@link Drop#BUST}. */
        void resolve(Drop drop, String line);

        /** A kill whose outcome is already known (the drop line came first, or a shared win): open with it. */
        void killKnown(Creature creature, Drop drop, String line);
    }

    private final DecisionLog log;
    private final Listener listener;
    /** Whether loot-shared kills play, set by the client each tick. */
    public boolean lootShare = true;
    /** The player's own name, for the dye's WOW! line (null: any). */
    public String self;

    public FishingWatcher(DecisionLog log, Listener listener) {
        this.log = log;
        this.listener = listener;
    }

    private static final double SCAN_RADIUS = 48, CLAIM_REACH = 20, DEATH_REACH = 32, TELEPORT = 12, ARM_RADIUS = 24;
    private static final long CLAIM_BEFORE_MS = 2_000, CLAIM_AFTER_MS = 6_000, SHARE_MS = 2_000, DROP_WAIT_MS = 3_000,
            EARLY_MS = 1_500, ARM_MS = 2_500, MIN_SEEN_MS = 300, DYE_KILL_MS = 10_000;
    private static final double GONE_FRACTION = 0.05;

    private static final class Mob {
        int id;
        Creature creature;
        long firstSeen;
        boolean owned, dead, immune;
        double hp = -1, maxHp = -1, distSq;
    }

    private final Map<Integer, Mob> mobs = new HashMap<>();
    /** Catch lines not yet matched to a nametag: creature, when. */
    private final List<Object[]> catches = new ArrayList<>();
    private long lastShareAt = -1;
    private Object lastShareWhat;
    private Creature ownGone;
    private long ownGoneAt = -1;
    private Creature sharedCreature;
    private long sharedAt = -1;
    private Creature foreignDead;
    private long foreignDeadAt = -1;

    private long pendingSince = -1;
    private Creature pendingCreature;
    private Drop bestDrop;
    private String bestLine;
    private long bestAt = -1;
    /** The last creature that died for us, and when: whose a dye with no kill pending is. */
    private Creature lastDead;
    private long lastDeadAt = -1;
    /** The last creature we caught, and when: the dye's creature when no death was seen at all. */
    private Creature lastCaught;
    private long lastCaughtAt = -1;
    private Vec3 lastPos;

    // ------------------------------------------------------------------ chat

    public void onChatLine(String text) {
        onChatLine(text, System.currentTimeMillis());
    }

    public void onChatLine(String text, long now) {
        if (text == null) return;
        String s = DianaWatcher.plain(text).trim();
        if (ChatLines.fromPlayer(s)) return;

        Creature caught = WaterDrops.ofCatch(s);
        if (caught != null) {
            catches.add(new Object[]{caught, now});
            lastCaught = caught;
            lastCaughtAt = now;
            log.note("§7Water: you caught a " + caught.title + " - claiming its nametag (\"" + s + "\")");
            for (Mob m : mobs.values()) claimByCatch(m, now);
            return;
        }

        Creature charmed = WaterDrops.ofCharm(s);
        if (charmed != null) {
            Mob m = nearest(charmed);
            if (m != null && m.owned) {
                m.dead = true;
                died(charmed, "you charmed it", now);
            } else if (m != null) {
                m.dead = true;
                death(m, "you charmed it", now);
            } else {
                log.note("§8Water: you charmed a " + charmed.title + " whose nametag was never seen - nothing opens");
            }
            return;
        }

        Object share = WaterDrops.ofShare(s);
        if (share != null) {
            lastShareAt = now;
            lastShareWhat = share;
            if (ownGone != null && now - ownGoneAt <= SHARE_MS && (share == ownGone || share == WaterDrops.SHARE_UNNAMED)) {
                Creature c = ownGone;
                ownGone = null;
                died(c, "its nametag went and a loot share for its kill came", now);
            }
            if (!lootShare) {
                log.note("§8Water: loot share - loot-shared kills are off in the menu");
                return;
            }
            if (share instanceof Creature named) {
                sharedCreature = named;
                sharedAt = now;
                if (pendingSince < 0 && bestDrop != null && bestDrop.win() && bestDrop.from(named) && now - bestAt <= EARLY_MS) {
                    log.note("§aWater: loot share for a " + named.title + " that dropped " + bestDrop.title + " - opening (\"" + s + "\")");
                    listener.killKnown(named, bestDrop, bestLine);
                    bestDrop = null;
                }
            }
            return;
        }

        Drop d = WaterDrops.of(s, self);
        if (d == null) return;
        boolean better = bestDrop == null || bestDrop == Drop.BUST || (d != Drop.BUST && d.ordinal() > bestDrop.ordinal());
        if (better || now - bestAt > DROP_WAIT_MS) {
            bestDrop = d;
            bestLine = s;
            bestAt = now;
        }
        if (d == Drop.BUST) return;
        log.note("§7Water: drop line - " + d.title + " (\"" + s + "\")");
        if (pendingSince >= 0) {
            if (d.from(pendingCreature)) {
                pendingSince = -1;
                listener.resolve(d, s);
            } else {
                log.note("§8Water: " + d.title + " cannot come from a " + pendingCreature.title + " - not this kill's");
            }
            return;
        }
        if (lootShare && foreignDead != null && now - foreignDeadAt <= SHARE_MS && d.from(foreignDead)
                && lastShareAt >= 0 && Math.abs(lastShareAt - foreignDeadAt) <= SHARE_MS) {
            log.note("§aWater: " + d.title + " from someone's " + foreignDead.title + ", by loot share - opening");
            listener.killKnown(foreignDead, d, s);
            bestDrop = null;
            foreignDead = null;
            return;
        }
        if (lootShare && sharedCreature != null && now - sharedAt <= SHARE_MS && d.from(sharedCreature)) {
            log.note("§aWater: " + d.title + " right after a loot share for a " + sharedCreature.title + " - opening");
            listener.killKnown(sharedCreature, d, s);
            bestDrop = null;
            sharedCreature = null;
            return;
        }
        // The dye is a drop line of our own whatever the creature: it plays even with no kill seen (out of range).
        if (d == Drop.AQUAMARINE) {
            Creature from = lastDead != null && now - lastDeadAt <= DYE_KILL_MS ? lastDead
                    : lastCaught != null && now - lastCaughtAt <= 120_000 ? lastCaught : Creature.TITANOBOA;
            log.note("§aWater: the Aquamarine Dye - opening (from " + from.title + ")");
            listener.killKnown(from, d, s);
            bestDrop = null;
            return;
        }
        log.note("§8Water: " + d.title + " with no kill of ours - waiting for its creature's death (" + EARLY_MS + " ms)");
    }

    // ------------------------------------------------------------------ the world

    public void tick(LocalPlayer player, double hidePct) {
        long now = System.currentTimeMillis();
        if (player != null) self = player.getGameProfile().name();
        scan(player, now);
        arm(hidePct, now);
        afterTick(now);
    }

    public void afterTick(long now) {
        if (pendingSince >= 0 && now - pendingSince > DROP_WAIT_MS) {
            pendingSince = -1;
            log.note("§7Water: no rare drop in " + DROP_WAIT_MS / 1000 + " s after the kill - bust");
            listener.resolve(Drop.BUST, null);
        }
        catches.removeIf(u -> now - (long) u[1] > CLAIM_AFTER_MS);
    }

    /** One nametag this tick: its entity, its name, how far (squared). */
    public record Tag(int id, String name, double distSq) {
    }

    private void scan(LocalPlayer player, long now) {
        ClientLevel level = Minecraft.getInstance().level;
        if (player == null || level == null) {
            mobs.clear();
            lastPos = null;
            return;
        }
        Vec3 pos = player.position();
        boolean teleported = lastPos != null && pos.distanceToSqr(lastPos) > TELEPORT * TELEPORT;
        lastPos = pos;
        List<Tag> tags = new ArrayList<>();
        AABB box = AABB.ofSize(pos, SCAN_RADIUS * 2, SCAN_RADIUS * 2, SCAN_RADIUS * 2);
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, box)) {
            Component custom = stand.getCustomName();
            if (custom == null) continue;
            String name = custom.getString();
            if (WaterDrops.ofNametag(name) == null && !mobs.containsKey(stand.getId())) continue;
            tags.add(new Tag(stand.getId(), name, stand.distanceToSqr(player)));
        }
        observe(tags, teleported, now);
    }

    /** The pure step: every water mythic's nametag present this tick. */
    public void observe(List<Tag> tags, boolean teleported, long now) {
        if (teleported && !mobs.isEmpty()) log.note("§8Water: you teleported - nametags dropped");
        Set<Integer> seen = new HashSet<>();
        for (Tag tag : tags) {
            seen.add(tag.id());
            Mob m = mobs.get(tag.id());
            Creature shown = WaterDrops.ofNametag(tag.name());
            Double hp = shown == null ? null : DianaWatcher.hpOf(tag.name());
            if (m == null) {
                if (shown == null) continue;
                m = new Mob();
                m.id = tag.id();
                m.creature = shown;
                m.firstSeen = now;
                m.distSq = tag.distSq();
                if (hp != null) m.hp = hp;
                Double max = HellionWatcher.maxOf(tag.name());
                if (max != null) m.maxHp = max;
                mobs.put(m.id, m);
                claimByCatch(m, now);
            }
            m.distSq = tag.distSq();
            if (hp != null) m.hp = hp;
            boolean immune = shown == null;
            if (immune != m.immune) {
                m.immune = immune;
                if (m.owned) log.note(immune ? "§8Water: your " + m.creature.title + " shows no health (\"" + tag.name() + "\") - immune, not dead"
                        : "§8Water: your " + m.creature.title + " shows its health again at " + fmt(m.hp));
            }
            if (!m.dead && hp != null && hp <= 0) {
                m.dead = true;
                death(m, "its nametag reads 0", now);
            }
        }
        for (Iterator<Mob> it = mobs.values().iterator(); it.hasNext(); ) {
            Mob m = it.next();
            if (seen.contains(m.id)) continue;
            it.remove();
            if (m.dead || teleported) continue;
            if (now - m.firstSeen < MIN_SEEN_MS || m.distSq > DEATH_REACH * DEATH_REACH) continue;
            if (m.hp >= 0 && m.maxHp > 0 && m.hp <= m.maxHp * GONE_FRACTION) {
                m.dead = true;
                death(m, "its nametag went at " + fmt(m.hp) + " of " + fmt(m.maxHp), now);
            } else if (m.owned && bestDrop != null && bestDrop.win() && bestDrop.from(m.creature) && now - bestAt <= EARLY_MS) {
                m.dead = true;
                death(m, "its nametag went at " + fmt(m.hp) + " right after its " + bestDrop.title, now);
            } else if (m.owned && lastShareAt >= 0 && now - lastShareAt <= SHARE_MS
                    && (lastShareWhat == m.creature || lastShareWhat == WaterDrops.SHARE_UNNAMED)) {
                m.dead = true;
                death(m, "its nametag went at " + fmt(m.hp) + " with a loot share for its kill", now);
            } else if (m.owned) {
                ownGone = m.creature;
                ownGoneAt = now;
                log.note("§8Water: your " + m.creature.title + "'s nametag went at " + fmt(m.hp) + " - not a kill unless a loot share for it follows");
            }
        }
    }

    private void claimByCatch(Mob m, long now) {
        if (m.owned || m.dead) return;
        for (Iterator<Object[]> it = catches.iterator(); it.hasNext(); ) {
            Object[] c = it.next();
            long at = (long) c[1];
            if (c[0] != m.creature || m.firstSeen < at - CLAIM_BEFORE_MS || m.firstSeen > at + CLAIM_AFTER_MS) continue;
            if (m.distSq > CLAIM_REACH * CLAIM_REACH) continue;
            it.remove();
            m.owned = true;
            log.note("§7Water: your " + m.creature.title + " - first seen " + Math.round(Math.sqrt(m.distSq)) + " blocks away at its catch line");
            return;
        }
    }

    /** A nametag died. Ours opens the cutscene; anyone else's only plays if its own win comes to us. */
    private void death(Mob m, String how, long now) {
        if (m.owned) {
            died(m.creature, how, now);
            return;
        }
        foreignDead = m.creature;
        foreignDeadAt = now;
        if (lootShare && bestDrop != null && bestDrop.win() && bestDrop.from(m.creature)
                && now - bestAt <= EARLY_MS && lastShareAt >= 0 && now - lastShareAt <= SHARE_MS) {
            log.note("§aWater: someone's " + m.creature.title + " died and its " + bestDrop.title + " came to you by loot share - opening");
            lastDead = m.creature;
            lastDeadAt = now;
            listener.killKnown(m.creature, bestDrop, bestLine);
            bestDrop = null;
            return;
        }
        log.note("§8Water: a " + m.creature.title + " died that is not yours - " + how + "; it plays only if its drop comes to you");
    }

    private void died(Creature c, String how, long now) {
        if (pendingSince >= 0 && now - pendingSince < 1_000 && pendingCreature == c) return;
        lastDead = c;
        lastDeadAt = now;
        if (bestDrop != null && bestDrop.win() && bestDrop.from(c) && now - bestAt <= EARLY_MS) {
            log.note("§aWater: your " + c.title + " died - " + how + "; it dropped " + bestDrop.title);
            listener.killKnown(c, bestDrop, bestLine);
            bestDrop = null;
            return;
        }
        log.note("§aWater: your " + c.title + " died - " + how + "; opening, waiting for the drop");
        pendingSince = now;
        pendingCreature = c;
        bestDrop = null;
        bestLine = null;
        listener.kill(c, how);
    }

    private Mob nearest(Creature c) {
        Mob best = null;
        for (Mob m : mobs.values()) {
            if (m.creature != c || m.dead) continue;
            if (best == null || m.distSq < best.distSq) best = m;
        }
        return best;
    }

    // ------------------------------------------------------------------ the pre-kill hide

    private volatile long armedUntil;

    /** Whether a creature of ours is nearly dead: the HUD and chat fade. Cheap, any thread. */
    public boolean armed() {
        return System.currentTimeMillis() < armedUntil;
    }

    public boolean armedAt(long now) {
        return now < armedUntil;
    }

    /** Arms the hide for any creature of ours at or below {@code pct}% of its health. Public for the offline tests. */
    public void arm(double pct, long now) {
        if (pct <= 0) return;
        for (Mob m : mobs.values()) {
            if (m.dead || !m.owned) continue;
            if (m.distSq > ARM_RADIUS * ARM_RADIUS || now - m.firstSeen < MIN_SEEN_MS) continue;
            if (m.hp < 0 || m.maxHp <= 0 || m.hp > m.maxHp * pct / 100.0) continue;
            if (now >= armedUntil) log.note("§7Water: " + m.creature.title + " is at " + fmt(m.hp) + " - hiding the HUD");
            armedUntil = now + ARM_MS;
        }
    }

    private static String fmt(double hp) {
        return hp >= 1e6 ? Math.round(hp / 1e5) / 10.0 + "M" : hp >= 1e3 ? Math.round(hp / 100) / 10.0 + "k" : String.valueOf(Math.round(hp));
    }
}
