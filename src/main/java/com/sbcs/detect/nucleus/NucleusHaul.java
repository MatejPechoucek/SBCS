package com.sbcs.detect.nucleus;

import com.sbcs.detect.nucleus.NucleusLoot.Entry;
import com.sbcs.detect.nucleus.NucleusLoot.Tier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Everything one Crystal Nucleus Loot Bundle gave up, as its loot message printed it.
 *
 * Lines are kept as printed. A line is not a roll: the message may fold several rolls of one
 * item into a single count, and the seven guaranteed Fine gemstones are in it too, so nothing
 * here claims to know how many rolls the bundle spent.
 */
public final class NucleusHaul {

    /** One line of the loot message. */
    public record Drop(Entry entry, int amount) {
        public String name() { return entry.name(); }
        public boolean unknown() { return entry.weight() == 0 && entry.id() == null; }
    }

    private final List<Drop> drops;

    public NucleusHaul(List<Drop> drops) {
        this.drops = List.copyOf(drops);
    }

    public List<Drop> drops() { return drops; }

    public boolean isEmpty() { return drops.isEmpty(); }

    /** How many of an item the message gave, over every line naming it. */
    public int count(String name) {
        int n = 0;
        for (Drop d : drops) if (d.name().equalsIgnoreCase(name)) n += d.amount();
        return n;
    }

    /**
     * A line's tier in this haul. The same as its entry's, except the Fine Jasper Gemstone:
     * one comes with every bundle, so it is RARE only when the haul holds two or more.
     */
    public Tier tierOf(Drop d) {
        if (d.entry().tier() == Tier.RARE && d.name().equals("Fine Jasper Gemstone")) {
            return count("Fine Jasper Gemstone") >= 2 ? Tier.RARE : Tier.COMMON;
        }
        return d.entry().tier();
    }

    /** The best tier in the haul - what picks the ending. */
    public Tier best() {
        Tier best = Tier.COMMON;
        for (Drop d : drops) {
            Tier t = tierOf(d);
            if (t.ordinal() > best.ordinal()) best = t;
        }
        return best;
    }

    /** True when nothing in it is worth a reveal - the bust. */
    public boolean bust() { return best() == Tier.COMMON; }

    /**
     * True for a line that is only the bundle's guaranteed Fine gemstone - one of the seven
     * colours, exactly one of it. It came with the bundle, not from a roll, so its weight says
     * nothing about it.
     */
    public boolean guaranteed(Drop d) {
        if (count(d.name()) != 1) return false;
        for (String c : NucleusLoot.GUARANTEED_FINE) {
            if (d.name().equals("Fine " + c + " Gemstone")) return true;
        }
        return false;
    }

    /**
     * Worst first, best last; within a tier the guaranteed and unknown lines first, then the
     * commoner item first. Stable otherwise.
     */
    public List<Drop> revealOrder() {
        List<Drop> ordered = new ArrayList<>(drops);
        ordered.sort(Comparator.<Drop>comparingInt(d -> tierOf(d).ordinal())
                .thenComparingInt(d -> d.unknown() || guaranteed(d) ? Integer.MIN_VALUE
                        : d.entry().weight() == NucleusLoot.DYE_WEIGHT ? Integer.MAX_VALUE : -d.entry().weight()));
        return ordered;
    }

    /** The best drop, or null for an empty haul. */
    public Drop headline() {
        List<Drop> ordered = revealOrder();
        return ordered.isEmpty() ? null : ordered.get(ordered.size() - 1);
    }

    /** Every drop of RARE or better, best last. */
    public List<Drop> wins() {
        List<Drop> out = new ArrayList<>();
        for (Drop d : revealOrder()) if (tierOf(d) != Tier.COMMON) out.add(d);
        return out;
    }

    /** "23 line(s), best: Divan's Alloy (jackpot)" - for the decision log and tests. */
    public String describe() {
        if (drops.isEmpty()) return "an empty haul";
        Drop head = headline();
        return drops.size() + " line(s), best: " + head.name()
                + " §8(" + tierOf(head).name().toLowerCase(Locale.ROOT) + ")";
    }
}
