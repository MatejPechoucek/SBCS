package com.sbcs.detect;

import com.sbcs.anim.SlayerTimeline;
import com.sbcs.anim.SlayerTimeline.Drop;
import com.sbcs.anim.SlayerTimeline.Source;

/**
 * The Revenant's and the Tarantula's good drops, read off their drop lines (pure; tools/SlayerDetectTest).
 *
 * Unlike Hellion and the Seraph these do not open on the kill: a slayer drop line in brackets is only ever the
 * player's own ("CRAZY RARE DROP! (Severed Hand) (+236% ✯ Magic Find)", HellionDrops' format), so the line itself
 * is the trigger and the cutscene starts with its outcome known. Which slayer it belongs to is {@link SlayerQuest}'s
 * call: a drop both bosses share (the Paragon Shard) needs the quest, and a drop of the wrong boss for the quest
 * running is refused. A drop no other boss gives plays even when the quest was not seen.
 */
public final class SlayerDropWatcher {

    public interface Listener {
        /** A drop to play: {@code tier} is the line's own word ("CRAZY RARE DROP!"), null for a WOW! dye line. */
        void drop(Drop drop, String tier, String line);

        /** A Revenant or Tarantula quest of the player's completed with no good drop: a kill for the dry streak. */
        void dryKill(Source source);
    }

    /** When each source last had a drop played, so its quest completing does not count a second kill. */
    private long zombieDropAt = -1, spiderDropAt = -1;

    private final DecisionLog log;
    private final Listener listener;
    public SlayerQuest quest = new SlayerQuest();
    /** The player's name, for the dyes' "WOW! <player> found a ..." line; null skips the check (offline tests). */
    public String self;

    public SlayerDropWatcher(DecisionLog log, Listener listener) {
        this.log = log;
        this.listener = listener;
    }

    /** A game chat line with its colour codes stripped (player chat never reaches here). */
    public void onChatLine(String plain, long now) {
        if (plain == null) return;
        String s = plain.trim();
        if (ChatLines.fromPlayer(s)) return;
        SlayerQuest.Boss running = quest.boss(now);
        quest.onChatLine(s, now);
        if (s.startsWith("SLAYER QUEST COMPLETE!")) {
            Source src = running == SlayerQuest.Boss.REVENANT ? Source.REVENANT : running == SlayerQuest.Boss.TARANTULA ? Source.TARANTULA : null;
            long last = src == Source.REVENANT ? zombieDropAt : spiderDropAt;
            if (src != null && (last < 0 || now - last > 8_000)) {
                log.note("§8" + src.boss + ": quest complete with no good drop");
                listener.dryKill(src);
            }
            return;
        }
        String item, tier;
        if (s.startsWith("WOW!") && (s.contains("Matcha Dye") || s.contains("Brick Red Dye"))) {
            if (self != null && !s.contains(self)) return;
            item = s.contains("Matcha Dye") ? "Matcha Dye" : "Brick Red Dye";
            tier = null;
        } else {
            item = HellionDrops.item(s);
            if (item == null) return;
            tier = tierOf(s);
        }
        Drop d = pick(item, running != null ? running : quest.boss(now));
        if (d == null) return;
        if (d.source == Source.REVENANT) zombieDropAt = now;
        else spiderDropAt = now;
        log.note("§a" + d.source.boss + ": " + d.title + (tier == null ? "" : " (" + tier + ")") + " - playing");
        listener.drop(d, tier, s);
    }

    /** The drop an item is for the quest running; narrated when refused. */
    Drop pick(String item, SlayerQuest.Boss boss) {
        Drop zombie = Drop.of(Source.REVENANT, item), spider = Drop.of(Source.TARANTULA, item);
        if (zombie == null && spider == null) return null;
        if (boss == SlayerQuest.Boss.REVENANT) {
            if (zombie == null) log.note("§8Slayer: " + item + " is not a Revenant drop - ignored during a Revenant quest");
            return zombie;
        }
        if (boss == SlayerQuest.Boss.TARANTULA) {
            if (spider == null) log.note("§8Slayer: " + item + " is not a Tarantula drop - ignored during a Tarantula quest");
            return spider;
        }
        if (boss != null) {
            log.note("§8Slayer: " + item + " dropped during a " + boss.name().toLowerCase(java.util.Locale.ROOT) + " quest - not the Revenant's or Tarantula's");
            return null;
        }
        if (zombie != null && spider != null) {
            log.note("§8Slayer: " + item + " drops from both the Revenant and the Tarantula, and no quest was seen - not played");
            return null;
        }
        return zombie != null ? zombie : spider;
    }

    static String tierOf(String line) {
        for (SlayerTimeline.Tier t : SlayerTimeline.Tier.values()) if (line.startsWith(t.word)) return t.word;
        return null;
    }
}
