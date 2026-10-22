package com.sbcs.detect.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Whether an open menu should roll, and which chest. Pure, so DungeonChestTest can try every case.
 *
 * <ul>
 *   <li>A reward chest's own menu - the boss room's, or a chest's page at Croesus (it has a back arrow naming its
 *   run): it rolls if its tier is switched on, it is not claimed yet and it has not rolled before. At Croesus only
 *   with "Croesus: every chest" ({@code croesus} 2).</li>
 *   <li>A run's page at Croesus ({@code croesus} 1 or 2): the most profitable chest of the switched-on tiers that is
 *   neither claimed nor rolled (the least loss when every one loses) - Matej's rule. The page is then remembered, so
 *   opening the run again never rolls a second one.</li>
 * </ul>
 *
 * Matej's other rule, kept by the callers: a chest rolled in the boss room is never rolled again at Croesus - the
 * fingerprints match ({@link RewardChest#fingerprint()} uses price keys on both sides).
 */
public final class RollPicker {
    private RollPicker() {}

    public enum Kind {
        /** Not a menu the Ossuary plays on, or nothing on it rolls. */
        NONE,
        /** It may be one, but its contents are not all there yet: keep it covered and look again next tick. */
        WAIT,
        ROLL
    }

    /**
     * @param chest    the chest to roll (ROLL only)
     * @param remember the fingerprints to mark rolled when it starts
     * @param why      a line for the decision log
     */
    public record Decision(Kind kind, RewardChest chest, List<String> remember, String why) {
        static Decision none(String why) {
            return new Decision(Kind.NONE, null, List.of(), why);
        }
    }

    /** Whether this title could be one of ours (so it is covered from its first frame while it loads). */
    public static boolean candidate(String title, int tierBits, int croesus) {
        ChestTier tier = ChestTier.fromTitle(title);
        if (tier != null) return (tierBits & tier.bit()) != 0;
        return croesus > 0 && ChestReader.runFloor(title) != null;
    }

    public static Decision decide(String title, List<SlotView> slots, DungeonFloor floorHint, Map<String, String> names,
                                  int tierBits, int croesus, RollMemory memory, long now, Function<String, Long> price) {
        ChestTier tier = ChestTier.fromTitle(title);
        if (tier != null) {
            if ((tierBits & tier.bit()) == 0) return Decision.none(tier.title + " chests are switched off");
            RewardChest c = ChestReader.readChest(title, slots, floorHint, names);
            if (c == null) return new Decision(Kind.WAIT, null, List.of(), "waiting for the chest's contents");
            boolean atCroesus = false;
            for (SlotView s : slots) if (s.item() != null && s.item().endsWith(":arrow") && s.loreHas("To ")) atCroesus = true;
            if (atCroesus && croesus < 2) return Decision.none("a chest's own page at Croesus (only the run's page rolls)");
            if (c.opened()) return Decision.none("already claimed");
            String fp = c.fingerprint();
            if (memory.rolled(fp, now)) return Decision.none("already rolled");
            return new Decision(Kind.ROLL, c, List.of(fp), (atCroesus ? "Croesus chest page: " : "boss room: ") + fp);
        }
        DungeonFloor run = ChestReader.runFloor(title);
        if (run == null) return Decision.none("not a dungeon reward menu");
        if (croesus <= 0) return Decision.none("Croesus rolls are off");
        List<RewardChest> chests = ChestReader.readRun(title, slots, names);
        if (chests.isEmpty()) return new Decision(Kind.WAIT, null, List.of(), "waiting for the run's chests");
        List<String> fps = new ArrayList<>();
        List<ChestValue> open = new ArrayList<>();
        for (RewardChest c : chests) {
            String fp = c.fingerprint();
            fps.add(fp);
            if (c.opened() || (tierBits & c.tier().bit()) == 0 || memory.rolled(fp, now)) continue;
            open.add(ChestValue.of(c, price));
        }
        // The page itself is remembered too: once it has rolled, opening the run again shows Croesus, not a second
        // gamble. A chest rolled in the boss room does not count - the run's other chests are still unseen.
        fps.sort(null);
        String page = "RUN|" + String.join(";", fps);
        if (memory.rolled(page, now)) return Decision.none("this run's page has rolled already");
        ChestValue best = ChestValue.bestOf(open);
        if (best == null) return Decision.none("no unclaimed, unrolled chest of a switched-on tier");
        return new Decision(Kind.ROLL, best.chest(), List.of(page, best.chest().fingerprint()), "Croesus " + run.label()
                + ": best is " + best.chest().tier().title + " (profit " + best.profit()
                + (best.incomplete() ? ", some prices unknown" : "") + ")");
    }
}
