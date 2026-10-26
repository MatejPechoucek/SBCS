package com.sbcs.detect;

import java.util.List;
import java.util.Locale;

/**
 * Which slayer boss the player's own quest is for, so a drop line is only ever read as the right boss's
 * (pure, tools/SlayerQuestTest).
 *
 * The bug this exists for (2026-10-25, Matej's records): a Paragon Shard off a <b>Revenant Horror IV</b> played
 * Hellion. The Paragon Shard drops from every slayer, its line ("RARE DROP! (Paragon Shard) (+166% ✯ Magic Find)")
 * names no boss, and the Demonlord's watcher played any win at "SLAYER QUEST COMPLETE!". Now every slayer watcher
 * asks this first.
 *
 * Two sources, both read from the records:
 * <ul>
 *   <li>The spawn line, "SLAYER BOSS! The Revenant Horror IV spawned!" - every boss of the day had one (Revenant,
 *       Tarantula, Voidgloom, Demonlord, Bloodfiend).</li>
 *   <li>The sidebar, which names the quest's boss under "Slayer Quest" the whole time it runs ("Slayer Quest",
 *       "Voidgloom Seraph III", "(220/11k) Combat XP") - the only source before the boss spawns, and for a boss
 *       that comes with the quest.</li>
 * </ul>
 * The sidebar wins while it shows one; the spawn line covers a sidebar a mod has hidden. Unknown is unknown: a
 * watcher that needs the boss and cannot tell plays nothing.
 */
public final class SlayerQuest {

    /** The six slayer bosses, by the names Hypixel gives every tier of each. */
    public enum Boss {
        REVENANT("Revenant Horror", "Atoned Horror"),
        TARANTULA("Tarantula Broodfather", "Primordial Broodfather", "Conjoined Brood"),
        SVEN("Sven Packmaster"),
        VOIDGLOOM("Voidgloom Seraph"),
        INFERNO("Inferno Demonlord"),
        VAMPIRE("Bloodfiend");

        final String[] names;

        Boss(String... names) {
            this.names = names;
        }

        /** The boss a name belongs to ("Riftstalker Bloodfiend V", "Atoned Horror"...), or null. */
        public static Boss named(String text) {
            if (text == null) return null;
            for (Boss b : values()) for (String n : b.names) if (text.contains(n)) return b;
            return null;
        }
    }

    private Boss lineBoss, sidebarBoss;
    private int lineTier, sidebarTier;
    private long lineAt = -1, sidebarAt = -1;
    /** A sidebar reading older than this is not trusted (the sidebar stopped being read, or the player left). */
    private static final long SIDEBAR_FRESH_MS = 5_000;

    /** A raw game chat line (colour codes and all): lines players typed are left out. */
    public void onGameLine(String raw, long now) {
        if (raw == null) return;
        String s = DianaWatcher.plain(raw).trim();
        if (!ChatLines.fromPlayer(s)) onChatLine(s, now);
    }

    /** A game chat line, colour codes already stripped. */
    public void onChatLine(String plain, long now) {
        if (plain == null) return;
        String s = plain.trim();
        if (s.startsWith("SLAYER BOSS!") && s.contains("spawned")) {
            Boss b = Boss.named(s);
            if (b != null) {
                lineBoss = b;
                lineTier = tierIn(s);
                lineAt = now;
            }
        } else if (s.startsWith("SLAYER QUEST FAILED!") || s.startsWith("Your Slayer Quest has been cancelled")) {
            lineBoss = null;
        }
    }

    /** The sidebar's lines, top first (any formatting already stripped). */
    public void onSidebar(List<String> lines, long now) {
        if (lines == null) return;
        for (int i = 0; i < lines.size() - 1; i++) {
            if (!lines.get(i).trim().equalsIgnoreCase("Slayer Quest")) continue;
            Boss b = Boss.named(lines.get(i + 1));
            if (b != null) {
                sidebarBoss = b;
                sidebarTier = tierIn(lines.get(i + 1));
                sidebarAt = now;
                return;
            }
        }
        // A sidebar without a slayer quest on it: no quest is running.
        if (!lines.isEmpty()) {
            sidebarBoss = null;
            sidebarAt = now;
        }
    }

    /** The boss of the quest running now, or null when it cannot be told. */
    public Boss boss(long now) {
        if (sidebarAt >= 0 && now - sidebarAt <= SIDEBAR_FRESH_MS) return sidebarBoss != null ? sidebarBoss : lineBoss;
        return lineBoss;
    }

    /** The quest's tier, 1..5, or 0 when it cannot be told. */
    public int tier(long now) {
        if (sidebarAt >= 0 && now - sidebarAt <= SIDEBAR_FRESH_MS && sidebarBoss != null) return sidebarTier;
        return lineBoss != null ? lineTier : 0;
    }

    /** Whether the quest is known to be for a boss other than {@code b} - the only case a watcher refuses a line on. */
    public boolean otherThan(Boss b, long now) {
        Boss q = boss(now);
        return q != null && q != b;
    }

    /** The roman numeral after a boss's name: "Revenant Horror IV" -> 4. */
    static int tierIn(String s) {
        // The tier V bosses go by their own names, without a numeral.
        if (s.contains("Atoned Horror") || s.contains("Primordial Broodfather") || s.contains("Conjoined Brood")) return 5;
        String[] words = s.replaceAll("[^A-Za-z ]", " ").trim().split("\\s+");
        for (String w : words) {
            switch (w.toUpperCase(Locale.ROOT)) {
                case "I": return 1;
                case "II": return 2;
                case "III": return 3;
                case "IV": return 4;
                case "V": return 5;
                default:
            }
        }
        return 0;
    }
}
