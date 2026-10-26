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

    /** A covered reward's box. */
    public static final class Box {
        public final PairsReward reward;
        public final char mark;
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

        Box(PairsReward reward, char mark, long firstSeen, int turnsAtStart) {
            this.reward = reward;
            this.mark = mark;
            this.firstSeen = firstSeen;
            this.turnsAtStart = turnsAtStart;
        }
    }

    /** What changed in one update, for the DecisionLog and the hunt's sounds. */
    public record Event(Kind kind, Box box, int slot, int other) {
        public enum Kind { BOX, MATCH, TURN }
    }

    private final Map<String, Box> boxes = new LinkedHashMap<>();
    private final Map<Integer, String> lastSig = new HashMap<>();
    private boolean primed;
    private int turns;
    private long lastTurnAt = Long.MIN_VALUE / 2;
    private Integer clicksLeft, clicksMax;
    private final String stake;
    public final long startedAt;

    public PairsGame(String stake, long now) {
        this.stake = stake;
        this.startedAt = now;
    }

    public String stake() { return stake; }

    /** The box for this reward, lettered now if it is new. Null for a reward that is not covered. */
    public Box box(PairsReward r, long now) {
        if (r == null || !r.covered()) return null;
        Box b = boxes.get(r.key());
        if (b == null) {
            char mark = (char) ('A' + Math.min(25, boxes.size()));
            b = new Box(r, mark, now, turns);
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

    /** Whether any box is still waiting for its pair. */
    public boolean hunting() {
        for (Box b : boxes.values()) if (!b.matched) return true;
        return false;
    }

    /** Turns since the oldest unmatched box appeared: the hunt's pulse. */
    public int turnsHunting() {
        int best = 0;
        for (Box b : boxes.values()) if (!b.matched) best = Math.max(best, turns - b.turnsAtStart);
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
            // Turns: the grid changed since last tick.
            if (primed) {
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
            readClicks(slots);
        }
        for (Map.Entry<String, List<Integer>> e : faceUp.entrySet()) {
            Box b = boxes.get(e.getKey());
            if (b.matched) continue;
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
