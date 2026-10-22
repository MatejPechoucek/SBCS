package com.sbcs.detect.nucleus;

import com.sbcs.detect.DecisionLog;
import com.sbcs.detect.nucleus.NucleusLoot.Entry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the Crystal Nucleus's loot message - the VanguardWatcher's design, for the same reason:
 * the whole haul is printed before anything is drawn, so there is nothing to infer and no
 * window to hold open.
 *
 * <h2>The message, as Matej's game logs printed it (15 runs, 2026-10-08 .. 10-21)</h2>
 *
 * <pre>
 * ✦ You placed the Topaz Crystal!          the fifth crystal - the haul follows in the same tick
 * ▬▬▬... (64)                              opening rule, ignored: we start on the header
 *   CRYSTAL NUCLEUS LOOT BUNDLE            header (a trailing space)
 *                                          blank
 *   REWARDS                                heading
 *     Pickonimbus 2000                     rolled items, four spaces
 *      Fine Amber Gemstone                 gemstones, a symbol (dropped by the log) and four spaces
 *      Flawed Topaz Gemstone x86           every roll of one item folded into one count
 *     +800 HOTM Exp                        not loot
 *     Enchanted Book (Lapidary I)
 * [SkyHanni] Profit for Crystal Nucleus Run: -829k   another mod's line, written into the middle
 * ▬▬▬... (64)                              closes it
 * Pick it up near the Nucleus Vault!
 * </pre>
 *
 * So the haul is known <b>the moment the fifth crystal goes in</b>, before the bundle is picked
 * up - SkyHanni's "earned a Crystal Loot Bundle" wording is not printed any more. The header goes
 * to {@code onBundle}, which arms the black box where the bundle is about to appear. Lines that
 * belong to the loot message return true from {@link #onChatLine}, so the caller can hide them:
 * the message is the reveal. {@link #parsing()} lets the caller hold other chat while it is open,
 * because SkyHanni's profit line lands in the middle of it and gives the outcome away.
 *
 * Folded counts mean a line is not a roll, and the seven guaranteed Fine gemstones are in the
 * list: a "Fine Amber Gemstone x2" is the guaranteed one plus a rolled one.
 */
public final class NucleusWatcher {

    private static final Pattern START = Pattern.compile("^\\s*CRYSTAL NUCLEUS LOOT BUNDLE.*$");
    private static final Pattern PICK_UP = Pattern.compile("(?i).*pick it up near the nucleus vault.*");
    private static final Pattern ITEM =
            Pattern.compile("^\\s+(?<item>.+?)(?:\\s+x(?<amount>[\\d,]+))?\\s*$");
    private static final Pattern END = Pattern.compile("^\\s*[\\u25AC]{10,}\\s*$");
    private static final Pattern COLLAPSED = Pattern.compile("\\s*\\(\\d+\\)\\s*$");

    /** 21 rolls, 7 guaranteed gems, experience and powder, and room for the message's furniture. */
    private static final int MAX_LINES = 45;
    private static final int TIMEOUT_TICKS = 40;

    private final DecisionLog log;
    private final Consumer<NucleusHaul> onHaul;
    private final Runnable onBundle;

    private boolean parsing;
    private int lines;
    private int afterHeading;
    private int ticksOpen;
    private final List<NucleusHaul.Drop> collected = new ArrayList<>();

    public NucleusWatcher(DecisionLog log, Consumer<NucleusHaul> onHaul, Runnable onBundle) {
        this.log = log;
        this.onHaul = onHaul;
        this.onBundle = onBundle;
    }

    public boolean parsing() { return parsing; }

    /**
     * Offer one chat message (stripped or not). A message carrying several lines is read line by
     * line.
     *
     * @return true if it belongs to a loot message and should be hidden from chat
     */
    public boolean onChatLine(String raw) {
        if (raw.indexOf('\n') >= 0) {
            boolean ours = false;
            for (String part : raw.split("\n")) ours |= onOneLine(part);
            return ours;
        }
        return onOneLine(raw);
    }

    private boolean onOneLine(String raw) {
        String text = COLLAPSED.matcher(DecisionLog.strip(raw)).replaceFirst("");

        if (START.matcher(text).matches()) {
            if (parsing) {
                log.note("§eA second Nucleus loot header arrived before the first closed §8- dropping "
                        + collected.size() + " line(s) and starting again");
            }
            open();
            log.note("§aCrystal Nucleus run complete §8- reading the loot bundle");
            onBundle.run();
            return true;
        }

        if (!parsing) {
            if (PICK_UP.matcher(text).matches()) log.note("§8Nucleus: \"" + trim(text) + "\"");
            return false;
        }

        ticksOpen = 0;

        if (END.matcher(text).matches()) {
            finish("the message closed normally");
            return true;
        }

        if (++lines > MAX_LINES) {
            abandon("more than " + MAX_LINES + " lines arrived without a closing rule");
            return false;
        }

        if (text.isBlank()) return true;

        if (isHeading(text)) {
            // Anything above the REWARDS heading is a notice, not loot - the corpse's "+1 bonus
            // drop!" sits there, and the Nucleus may have its own.
            afterHeading = collected.size();
            return true;
        }

        Matcher item = ITEM.matcher(text);
        if (!item.matches()) {
            log.note("§8Ignoring a line inside the Nucleus loot message: \"" + trim(text) + "\"");
            return false;
        }

        String name = item.group("item").trim();
        if (name.isEmpty()) return true;
        if (NucleusLoot.ignored(name)) {
            log.note("§8Nucleus: not a roll, \"" + trim(name) + "\"");
            return true;
        }

        int amount = 1;
        String count = item.group("amount");
        if (count != null) {
            try {
                amount = Math.max(1, Integer.parseInt(count.replace(",", "")));
            } catch (NumberFormatException e) {
                log.note("§cCould not read the count \"" + count + "\" on \"" + name + "\" §8- treating it as 1");
            }
        }

        Entry entry = NucleusLoot.find(name);
        if (entry == null) {
            log.note("§6Unknown Nucleus drop \"" + name + "\" §8- counting it as a common. "
                    + "Add it to NucleusLoot with its weight.");
            entry = NucleusLoot.findOrCommon(name);
        }
        collected.add(new NucleusHaul.Drop(entry, amount));
        return true;
    }

    /** Every client tick, so an unclosed message cannot stay open. */
    public void tick() {
        if (!parsing) return;
        if (++ticksOpen > TIMEOUT_TICKS) {
            if (collected.isEmpty()) {
                abandon("nothing arrived within " + TIMEOUT_TICKS + " ticks of the header");
            } else {
                finish("no closing rule arrived within " + TIMEOUT_TICKS + " ticks");
            }
        }
    }

    /** Feed a haul straight in, for the test commands. */
    public void inject(NucleusHaul haul) {
        log.note("§bTest Nucleus bundle §8- " + haul.describe());
        onHaul.accept(haul);
    }

    private void open() {
        parsing = true;
        lines = 0;
        afterHeading = 0;
        ticksOpen = 0;
        collected.clear();
    }

    private void finish(String why) {
        if (afterHeading > 0) {
            List<NucleusHaul.Drop> dropped = new ArrayList<>(collected.subList(0, afterHeading));
            collected.subList(0, afterHeading).clear();
            for (NucleusHaul.Drop d : dropped) {
                log.note("§8Not a drop, it was above the heading: \"" + d.name() + "\"");
            }
        }
        NucleusHaul haul = new NucleusHaul(collected);
        parsing = false;
        collected.clear();

        if (haul.isEmpty()) {
            log.note("§cA Nucleus bundle was opened but nothing could be read from it §8(" + why + ")");
            return;
        }
        log.note("§aNucleus bundle opened §8- " + haul.describe() + " (" + why + ")");
        onHaul.accept(haul);
    }

    private void abandon(String why) {
        log.note("§cGave up reading a Nucleus loot message §8- " + why
                + (collected.isEmpty() ? "" : " §8(" + collected.size() + " line(s) discarded)"));
        parsing = false;
        collected.clear();
    }

    /** "REWARDS": letters and not one of them lower case. No item is printed that way. */
    private static boolean isHeading(String text) {
        boolean letter = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLowerCase(c)) return false;
            letter |= Character.isLetter(c);
        }
        return letter;
    }

    private static String trim(String s) {
        return s.length() <= 50 ? s : s.substring(0, 50) + "...";
    }
}
