package com.sbcs.detect.superpairs;

import com.sbcs.detect.dungeon.SlotView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One game of Superpairs, as the player has seen it so far. Pure: fed the menu's slots once a tick (and asked about
 * single slots every frame by the cover), so the whole thing can be replayed offline from recorded menus.
 *
 * <ul>
 *   <li><b>Boxes.</b> Every reward from the {@link PairsReward.Tier#RARE} row up gets a box the first time any of its
 *   tiles is seen, lettered A, B, C... in that order. Both tiles of a pair share the letter - it tells the player which
 *   boxes match and nothing about what is under them.</li>
 *   <li><b>Matched.</b> A pair is matched when both its tiles show at once: Hypixel turns a missed pair back over, so
 *   the only way two tiles of one reward are face up together is a match (a Diamond's instant find too). A rewards
 *   menu listing it counts as well. <b>Theory until the records:</b> how a matched pair looks afterwards.</li>
 *   <li><b>Turns.</b> Any change on the grid, at most one per {@link #TURN_GAP_MS}, so the hunt's heartbeat can speed up
 *   as the player flips and misses.</li>
 * </ul>
 * Only what the menu shows is read: a face-down tile is nothing to us, as it is to the player.
 */
public final class PairsGame {

    /** Two changes closer than this are one turn (a flip and its pair turning back are one). */
    static final long TURN_GAP_MS = 250;

    /** A rare reward on the board: boxed ({@link #hidden}) or shown as Hypixel draws it, under the price floor. */
    public static final class Box {
        public final PairsReward reward;
        /** Its letter; only hidden boxes are lettered (0 for one that is shown). */
        public final char mark;
        /** Covered by a black box: decided once, when it is first seen, so a late price never uncovers it. */
        public final boolean hidden;
        public final long firstSeen;
        /** The slots it has been seen in this game (both, once matched). */
        public final TreeSet<Integer> slots = new TreeSet<>();
        public boolean matched;
        public long matchedAt;
        boolean announced;
        /** The two slots it matched in, -1 if it was the rewards menu that said so. */
        public int slotA = -1, slotB = -1;
        /** Turns played when the hunt for it began. */
        final int turnsAtStart;

        Box(PairsReward reward, char mark, boolean hidden, long firstSeen, int turnsAtStart) {
            this.reward = reward;
            this.mark = mark;
            this.hidden = hidden;
            this.firstSeen = firstSeen;
            this.turnsAtStart = turnsAtStart;
        }
    }

    /** What changed in one update, for the DecisionLog and the hunt's sounds. */
    public record Event(Kind kind, Box box, int slot, int other) {
        public enum Kind { BOX, MATCH, TURN, UNMATCH }
    }

    private final Map<String, Box> boxes = new LinkedHashMap<>();
    private final Map<Integer, String> lastSig = new HashMap<>();
    private boolean primed;
    private int turns;
    private long lastTurnAt = Long.MIN_VALUE / 2;
    private Integer clicksLeft, clicksMax;
    /**
     * The board turned over whole: with no clicks left Hypixel shows every tile, so two of a pair face up then is not a
     * match (0.4.0 called a Serum found that way). Seen in Matej's records, 2026-10-28.
     */
    private boolean revealed;
    /** Which rare rewards get a black box: those at or over the price floor, or of unknown worth. */
    private java.util.function.Predicate<PairsReward> hide = r -> true;
    private final String stake;
    public final long startedAt;

    public PairsGame(String stake, long now) {
        this.stake = stake;
        this.startedAt = now;
    }

    public String stake() { return stake; }

    public PairsGame hiding(java.util.function.Predicate<PairsReward> hide) {
        this.hide = hide;
        return this;
    }

    /** Whether the board has been turned over whole (no clicks left). */
    public boolean revealed() { return revealed; }

    /** The box for this reward, made now if it is new. Null for a reward that is not rare. */
    public Box box(PairsReward r, long now) {
        if (r == null || !r.covered()) return null;
        Box b = boxes.get(r.key());
        if (b == null) {
            boolean hidden = hide.test(r);
            int lettered = 0;
            for (Box x : boxes.values()) if (x.hidden) lettered++;
            char mark = hidden ? (char) ('A' + Math.min(25, lettered)) : 0;
            b = new Box(r, mark, hidden, now, turns);
            boxes.put(r.key(), b);
        }
        return b;
    }

    /** The box already lettered for this reward, or null - never makes one. */
    public Box existing(String key) {
        return boxes.get(key);
    }

    public List<Box> boxes() {
        return new ArrayList<>(boxes.values());
    }

    public int turns() { return turns; }
    public Integer clicksLeft() { return clicksLeft; }
    public Integer clicksMax() { return clicksMax; }

    /** Whether any hidden box is still waiting for its pair. */
    public boolean hunting() {
        for (Box b : boxes.values()) if (b.hidden && !b.matched) return true;
        return false;
    }

    /** Turns since the oldest unmatched box appeared: the hunt's pulse. */
    public int turnsHunting() {
        int best = 0;
        for (Box b : boxes.values()) if (b.hidden && !b.matched) best = Math.max(best, turns - b.turnsAtStart);
        return best;
    }

    /**
     * One tick of the game menu. {@code slots} is the menu's own part (the parsers' view); {@code rewards} each slot's
     * reading by slot; {@code rewardsMenu} when this is the after-game rewards page, where anything listed is won.
     */
    public List<Event> update(List<SlotView> slots, Map<Integer, PairsReward> rewards, boolean rewardsMenu, long now) {
        List<Event> out = new ArrayList<>();
        Map<String, List<Integer>> faceUp = new LinkedHashMap<>();
        Map<Integer, String> sig = new HashMap<>();
        for (SlotView s : slots) {
            sig.put(s.slot(), s.name() + "|" + s.item());
            PairsReward r = rewards.get(s.slot());
            if (r == null || !r.covered()) continue;
            // The cover may have lettered it already, on the frame the tile turned; it is announced here once.
            Box b = box(r, now);
            if (!b.announced) {
                b.announced = true;
                out.add(new Event(Event.Kind.BOX, b, s.slot(), -1));
            }
            b.slots.add(s.slot());
            faceUp.computeIfAbsent(r.key(), k -> new ArrayList<>()).add(s.slot());
        }
        if (!rewardsMenu) {
            Integer before = clicksLeft;
            readClicks(slots);
            // Turns: the grid changed since last tick.
            if (primed) {
                int flipped = 0;
                for (Map.Entry<Integer, String> e : sig.entrySet()) if (!e.getValue().equals(lastSig.get(e.getKey()))) flipped++;
                // A click turns one tile (plus the clicks and the timer); the end turns the rest over at once.
                if (flipped > 4 && (Integer.valueOf(0).equals(clicksLeft) || Integer.valueOf(0).equals(before))) revealed = true;
                boolean changed = !sig.equals(lastSig);
                if (changed && now - lastTurnAt >= TURN_GAP_MS) {
                    turns++;
                    lastTurnAt = now;
                    out.add(new Event(Event.Kind.TURN, null, -1, -1));
                }
            }
            primed = true;
            lastSig.clear();
            lastSig.putAll(sig);
        }
        List<String> listed = rewardsMenu ? rewardsListed(slots) : null;
        if (listed != null) {
            // The rewards page names everything won: it settles every box, both ways.
            for (Box b : boxes.values()) {
                boolean won = false;
                String t = b.reward.title().toLowerCase(Locale.ROOT);
                for (String l : listed) if (l.contains(t)) won = true;
                if (won && !b.matched) {
                    b.matched = true;
                    b.matchedAt = now;
                    out.add(new Event(Event.Kind.MATCH, b, -1, -1));
                } else if (!won && b.matched) {
                    b.matched = false;
                    out.add(new Event(Event.Kind.UNMATCH, b, -1, -1));
                }
            }
            return out;
        }
        for (Map.Entry<String, List<Integer>> e : faceUp.entrySet()) {
            Box b = boxes.get(e.getKey());
            if (b.matched || revealed && !rewardsMenu) continue;
            if (rewardsMenu || e.getValue().size() >= 2) {
                b.matched = true;
                b.matchedAt = now;
                if (!rewardsMenu) {
                    b.slotA = e.getValue().get(0);
                    b.slotB = e.getValue().get(1);
                }
                out.add(new Event(Event.Kind.MATCH, b, b.slotA, b.slotB));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ the menu's own words (theory)

    /** "Superpairs (Metaphysical)" - from other mods and memory; the recorder will settle it. */
    private static final Pattern GAME = Pattern.compile("(?i)^\\s*superpairs\\s*\\(([^)]+)\\)");
    private static final Pattern REWARDS = Pattern.compile("(?i)superpairs.*rewards?|experiment.*(over|rewards?)");
    private static final Pattern CLICKS_A = Pattern.compile("(?i)(\\d+)\\s*(?:/\\s*(\\d+)\\s*)?clicks?\\s*(?:left|remaining)");
    private static final Pattern CLICKS_B = Pattern.compile("(?i)(?:clicks?\\s*(?:left|remaining)|remaining\\s*clicks?)\\s*:?\\s*(\\d+)(?:\\s*/\\s*(\\d+))?");

    /** The stake of a Superpairs game menu ("Metaphysical"), or null for any other title. */
    public static String gameStake(String title) {
        if (title == null) return null;
        Matcher m = GAME.matcher(title);
        return m.find() ? m.group(1).trim() : null;
    }

    /** Whether this is the page after a game that lists what was won (theory). */
    public static boolean isRewards(String title) {
        return title != null && REWARDS.matcher(title).find();
    }

    /** Any menu the Superpairs cover and recorder look at. */
    public static boolean isSuperpairs(String title) {
        if (title == null) return false;
        String t = title.toLowerCase(Locale.ROOT);
        return t.contains("superpairs") || isRewards(title);
    }

    /**
     * The rewards page's own list: the lines after "Rewards:" in the Superpairs item's lore ("+ Titanic Experience
     * Bottle"), lower case. Null when the page has no such list. Read from Matej's records, 2026-10-28.
     */
    static List<String> rewardsListed(List<SlotView> slots) {
        for (SlotView s : slots) {
            List<String> lore = s.lore();
            int at = -1;
            for (int i = 0; i < lore.size(); i++) if (lore.get(i).trim().equalsIgnoreCase("Rewards:")) at = i;
            if (at < 0) continue;
            List<String> out = new ArrayList<>();
            for (int i = at + 1; i < lore.size(); i++) {
                String l = lore.get(i).trim();
                if (l.startsWith("+")) out.add(l.toLowerCase(Locale.ROOT));
            }
            return out;
        }
        return null;
    }

    /** Clicks left, from any slot's name or lore ("Clicks Remaining: 7", "7 clicks left") - theory. */
    void readClicks(List<SlotView> slots) {
        for (SlotView s : slots) {
            if (readClicks(s.name())) return;
            for (String l : s.lore()) if (readClicks(l)) return;
        }
    }

    private boolean readClicks(String line) {
        if (line == null) return false;
        Matcher m = CLICKS_B.matcher(line);
        if (!m.find()) {
            m = CLICKS_A.matcher(line);
            if (!m.find()) return false;
        }
        clicksLeft = Integer.parseInt(m.group(1));
        if (m.group(2) != null) clicksMax = Integer.parseInt(m.group(2));
        else if (clicksMax == null || clicksLeft > clicksMax) clicksMax = clicksLeft;
        return true;
    }
}
