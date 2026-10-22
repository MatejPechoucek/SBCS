/*
 * Offline checks for the Crystal Nucleus parser and loot table (detect/nucleus/, no Minecraft
 * apart from BundleSpotter, which is left out). Built the way VgTest is: DecisionLog needs only slf4j.
 *
 *   SLF=$(find ~/.gradle/caches/modules-2 -name 'slf4j-api-*.jar' ! -name '*sources*' | head -1)
 *   javac -d out/nuc -cp "$SLF" src/main/java/com/sbcs/detect/DecisionLog.java \
 *         src/main/java/com/sbcs/detect/nucleus/Nucleus*.java tools/NucleusTest.java
 *   java -cp "out/nuc:$SLF" NucleusTest            (";" between entries on Windows)
 *
 * Run from the repository root: it reads tools/fixtures/nucleus_hauls.txt, real messages from the
 * designer's game logs.
 */
import com.sbcs.detect.DecisionLog;
import com.sbcs.detect.nucleus.*;
import com.sbcs.detect.nucleus.NucleusLoot.Tier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class NucleusTest {
    static int pass = 0, fail = 0;
    static void check(String what, boolean ok, String detail) {
        System.out.printf("  %s %-58s %s%n", ok ? "PASS" : "FAIL", what, detail);
        if (ok) pass++; else fail++;
    }

    static final List<NucleusHaul> caught = new ArrayList<>();
    static int bundles;
    static NucleusWatcher fresh(DecisionLog log) {
        caught.clear();
        bundles = 0;
        return new NucleusWatcher(log, caught::add, () -> bundles++);
    }

    static final String RULE = "▬".repeat(64);

    public static void main(String[] a) throws Exception {
        DecisionLog log = new DecisionLog();

        System.out.println("The table");
        int sum = 0;
        for (NucleusLoot.Entry e : NucleusLoot.all()) if (e.weight() > 0) sum += e.weight();
        check("weights add to TOTAL_WEIGHT", sum == NucleusLoot.TOTAL_WEIGHT, sum + "");
        check("Flawless Ruby is 15 in 307,212",
                Math.abs(NucleusLoot.find("Flawless Ruby Gemstone").perRoll() - 15.0 / 307_212) < 1e-12, "");
        check("the symbol in front of a gem is ignored", NucleusLoot.find("❤ Fine Ruby Gemstone") != null, "");
        check("a book is found by its enchantment",
                NucleusLoot.find("Enchanted Book (Lapidary I)") != null
                        && NucleusLoot.find("Enchanted Book (Lapidary I)").name().equals("Lapidary I"), "");
        check("Divan's Alloy is a jackpot", NucleusLoot.find("Divan's Alloy").tier() == Tier.JACKPOT, "");
        check("the Jade Dye is its own tier", NucleusLoot.find("Jade Dye").tier() == Tier.DYE, "");
        check("HotM experience is not loot", NucleusLoot.ignored("+800 HOTM Exp"), "");
        check("powder is not loot", NucleusLoot.ignored("Gemstone Powder"), "");
        double jackpot = NucleusLoot.atLeast(Tier.JACKPOT, NucleusLoot.AVG_ROLLS);
        check("a jackpot is roughly 1 in 166 bundles", jackpot > 1 / 200.0 && jackpot < 1 / 140.0,
                String.format(Locale.ROOT, "1 in %.0f", 1 / jackpot));

        System.out.println("Real messages (tools/fixtures/nucleus_hauls.txt)");
        List<String> lines = Files.readAllLines(Path.of("tools/fixtures/nucleus_hauls.txt"), StandardCharsets.UTF_8);
        NucleusWatcher w = fresh(log);
        int blocks = 0, hidden = 0, shown = 0;
        for (String line : lines) {
            if (line.startsWith("#")) continue;
            if (line.startsWith("===")) { blocks++; w.tick(); continue; }
            if (w.onChatLine(line)) hidden++; else shown++;
        }
        for (int i = 0; i < 45; i++) w.tick();
        check("every message read", caught.size() == blocks, caught.size() + " of " + blocks);
        check("the bundle is armed once per message", bundles == blocks, bundles + "");
        check("not left open", !w.parsing(), "");
        boolean allBust = true, noUnknown = true, eightLeast = true;
        for (NucleusHaul h : caught) {
            allBust &= h.bust();
            for (NucleusHaul.Drop d : h.drops()) noUnknown &= !d.unknown();
            eightLeast &= h.drops().size() >= 8;
        }
        check("every item is in the table", noUnknown, "");
        check("all four were busts (they were)", allBust, "");
        check("each has the gems and its rolls", eightLeast, "");
        NucleusHaul first = caught.get(0);
        check("first: 15 lines", first.drops().size() == 15, first.drops().size() + "");
        check("first: Flawed Topaz x86", first.count("Flawed Topaz Gemstone") == 86, "");
        check("first: one Fine Jasper is the guaranteed one, common",
                first.tierOf(first.drops().stream().filter(d -> d.name().equals("Fine Jasper Gemstone")).findFirst().get()) == Tier.COMMON, "");
        check("first: headline is the Pickonimbus (rarest common)", first.headline().name().equals("Pickonimbus 2000"),
                first.headline().name());
        check("third: Fine Amber x2 read", caught.get(2).count("Fine Amber Gemstone") == 2, "");
        check("third: the book counted", caught.get(2).count("Lapidary I") == 1, "");
        check("SkyHanni's profit line is not ours (left to chat)", shown >= 4, shown + " shown");
        NucleusWatcher w2 = fresh(log);
        check("the place line and the opening rule are not ours",
                !w2.onChatLine("\u2726 You placed the Topaz Crystal!") && !w2.onChatLine(RULE) && !w2.parsing(), "");

        System.out.println("Wins");
        w = fresh(log);
        feed(w, "  CRYSTAL NUCLEUS LOOT BUNDLE ", "", "  REWARDS", "    Divan's Alloy",
                "     Fine Jasper Gemstone x2", "     Flawed Ruby Gemstone x12", RULE);
        NucleusHaul win = caught.get(0);
        check("Divan's Alloy wins", win.best() == Tier.JACKPOT, win.describe());
        check("its headline is the Alloy", win.headline().name().equals("Divan's Alloy"), "");
        check("Fine Jasper x2 is a rare", win.wins().size() == 2, win.wins().size() + "");

        w = fresh(log);
        feed(w, "  CRYSTAL NUCLEUS LOOT BUNDLE ", "  REWARDS", "    Jade Dye", "    Quick Claw",
                "    ☘ Flawless Jade Gemstone", RULE);
        check("the Dye beats a Flawless", caught.get(0).best() == Tier.DYE
                && caught.get(0).headline().name().equals("Jade Dye"), caught.get(0).describe());
        List<NucleusHaul.Drop> order = caught.get(0).revealOrder();
        check("reveal order climbs", order.get(0).name().equals("Quick Claw")
                && order.get(1).name().startsWith("Flawless"), order.get(0).name() + ", " + order.get(1).name());

        System.out.println("Robustness");
        w = fresh(log);
        feed(w, "  CRYSTAL NUCLEUS LOOT BUNDLE  (2)", "  REWARDS", "    Oil Barrel x2 (2)", RULE + " (3)");
        check("chat-dedup suffixes stripped", caught.size() == 1 && caught.get(0).count("Oil Barrel") == 2, "");
        w = fresh(log);
        feed(w, "  CRYSTAL NUCLEUS LOOT BUNDLE ", "  REWARDS", "    Mystery Thing x3");
        for (int i = 0; i < 45; i++) w.tick();
        check("no closing rule: closes on the timeout", caught.size() == 1 && caught.get(0).drops().get(0).unknown(), "");
        w = fresh(log);
        feed(w, "  CRYSTAL NUCLEUS LOOT BUNDLE ");
        for (int i = 0; i < 45; i++) w.tick();
        check("header alone: abandoned, nothing emitted", caught.isEmpty() && !w.parsing(), "");
        w = fresh(log);
        w.onChatLine("  CRYSTAL NUCLEUS LOOT BUNDLE \n  REWARDS\n    Bob-omb x20\n" + RULE);
        check("a multi-line message is read line by line", caught.size() == 1 && caught.get(0).count("Bob-omb") == 20, "");
        w = fresh(log);
        feed(w, "    Pickonimbus 2000", RULE, "  REWARDS");
        check("item-shaped lines outside a message are ignored", caught.isEmpty() && !w.parsing(), "");

        System.out.printf("%n%d passed, %d failed%n", pass, fail);
        if (fail > 0) System.exit(1);
    }

    static void feed(NucleusWatcher w, String... lines) {
        for (String l : lines) w.onChatLine(l);
    }
}
