import com.sbcs.anim.SlayerTimeline.Drop;
import com.sbcs.anim.SlayerTimeline.Source;
import com.sbcs.detect.DecisionLog;
import com.sbcs.detect.HellionWatcher;
import com.sbcs.detect.SlayerDropWatcher;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Offline checks for the slayer drop routing: which boss a drop line belongs to (SlayerQuest), the Revenant's and the
 * Tarantula's drop watcher, and that Hellion no longer plays another boss's drop - the bug Matej hit on 2026-10-25,
 * a Paragon Shard off a Revenant Horror playing the blaze. The replay is Matej's own slayer lines from 2026-10-24/25
 * (tools/fixtures/slayer_lines.txt: 140 Revenant, 100 Tarantula, 17 Seraph and 4 Bloodfiend bosses).
 *
 *   javac -d out -cp <slf4j> -sourcepath "src/main/java;tools" tools/SlayerDetectTest.java
 *   java -cp out:<slf4j> SlayerDetectTest
 */
public final class SlayerDetectTest {
    static int passed, failed;

    static void check(String what, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + what);
        }
    }

    public static void main(String[] args) throws Exception {
        List<String> played = new ArrayList<>(), dry = new ArrayList<>(), blaze = new ArrayList<>();
        SlayerDropWatcher.Listener l = new SlayerDropWatcher.Listener() {
            @Override public void drop(Drop d, String tier, String line) { played.add(d.name() + "|" + tier); }
            @Override public void dryKill(Source s) { dry.add(s.name()); }
        };
        HellionWatcher.Listener hl = new HellionWatcher.Listener() {
            @Override public void kill() { blaze.add("open"); }
            @Override public void resolve(com.sbcs.anim.HellionTimeline.Drop d, String line) { if (d.win()) blaze.add("resolve " + d); }
            @Override public void killKnown(com.sbcs.anim.HellionTimeline.Drop d, String line) { if (d.win()) blaze.add("known " + d); }
        };

        // ---- single lines
        SlayerDropWatcher w = new SlayerDropWatcher(new DecisionLog(), l);
        w.onChatLine("SLAYER BOSS! The Revenant Horror V spawned!", 1000);
        w.onChatLine("PRAY TO RNGESUS DROP! (Warden Heart) (+240% Magic Find)", 9000);
        check("a Warden Heart in a Revenant quest plays (" + played + ")", played.equals(List.of("WARDEN_HEART|PRAY TO RNGESUS DROP!")));
        played.clear();
        w.onChatLine("CRAZY RARE DROP! (◆ Snake Rune I) (+240% Magic Find)", 9500);
        check("a rune keeps its numeral and loses its symbol (" + played + ")", played.equals(List.of("SNAKE_RUNE|CRAZY RARE DROP!")));
        played.clear();
        w.onChatLine("  SLAYER QUEST COMPLETE!", 9800);
        check("a quest complete just after a drop is not a dry kill", dry.isEmpty());
        w.onChatLine("  SLAYER QUEST STARTED!", 9801);
        w.onChatLine("SLAYER BOSS! The Tarantula Broodfather IV spawned!", 20000);
        w.onChatLine("RARE DROP! (Paragon Shard) (+166% Magic Find)", 30000);
        check("a Paragon in a Tarantula quest is the Tarantula's (" + played + ")", played.equals(List.of("PARAGON_TARANTULA|null")));
        played.clear();
        w.onChatLine("  SLAYER QUEST COMPLETE!", 30300);
        w.onChatLine("  SLAYER QUEST STARTED!", 30300);
        w.onChatLine("SLAYER BOSS! The Tarantula Broodfather IV spawned!", 40000);
        w.onChatLine("  SLAYER QUEST COMPLETE!", 52000);
        check("a quest complete with no drop is a dry kill (" + dry + ")", dry.equals(List.of("TARANTULA")));

        w = new SlayerDropWatcher(new DecisionLog(), l);
        w.onChatLine("SLAYER BOSS! The Inferno Demonlord IV spawned!", 0);
        w.onChatLine("CRAZY RARE DROP! (Paragon Shard) (+200% Magic Find)", 5000);
        check("a Paragon in a blaze quest is not the Revenant's or Tarantula's (" + played + ")", played.isEmpty());
        w = new SlayerDropWatcher(new DecisionLog(), l);
        w.onChatLine("CRAZY RARE DROP! (Paragon Shard) (+200% Magic Find)", 5000);
        check("a Paragon with no quest seen plays nothing", played.isEmpty());
        w.onChatLine("CRAZY RARE DROP! (Severed Hand) (+200% Magic Find)", 6000);
        check("a drop only the Revenant gives plays with no quest seen (" + played + ")", played.equals(List.of("SEVERED_HAND|CRAZY RARE DROP!")));
        played.clear();
        w.onChatLine("Guild > [MVP+] Someone: CRAZY RARE DROP! (Severed Hand) (+200% Magic Find)", 7000);
        check("someone else's chat quoting a drop plays nothing", played.isEmpty());
        w.self = "qMighty";
        w.onChatLine("WOW! [MVP+] Someone found a Matcha Dye!", 8000);
        check("someone else's dye plays nothing", played.isEmpty());
        w.onChatLine("WOW! [MVP+] qMighty found a Matcha Dye!", 8100);
        check("your dye plays (" + played + ")", played.equals(List.of("MATCHA_DYE|null")));
        played.clear();

        // ---- the replay: Matej's real lines, through the Revenant/Tarantula watcher and Hellion's
        List<String> lines = Files.readAllLines(Path.of("tools/fixtures/slayer_lines.txt"), StandardCharsets.UTF_8);
        w = new SlayerDropWatcher(new DecisionLog(), l);
        HellionWatcher h = new HellionWatcher(new DecisionLog(), hl);
        com.sbcs.detect.SlayerQuest q = new com.sbcs.detect.SlayerQuest();
        w.quest = q;
        h.quest = q;
        int completes = 0, revenantDrops = 0;
        for (String raw : lines) {
            int sp = raw.indexOf(' ');
            if (sp < 0) continue;
            long now = LocalDateTime.parse("2026-" + raw.substring(0, sp)).toInstant(ZoneOffset.UTC).toEpochMilli();
            String text = raw.substring(sp + 1);
            if (text.contains("SLAYER QUEST COMPLETE!")) completes++;
            w.onChatLine(text, now);
            h.onChatLine(text, now);
        }
        for (String p : played) if (p.startsWith("PARAGON_REVENANT") || p.startsWith("REVENANT_CATALYST") || p.startsWith("UNDEAD_CATALYST")) revenantDrops++;
        check("Hellion plays nothing from these sessions - no Inferno quest in them (" + blaze + ")", blaze.isEmpty());
        check("the Revenant's Paragon on 2026-10-25 15:31 is routed to the Revenant (" + played + ")", played.contains("PARAGON_REVENANT|null"));
        check("the Revenant and Undead Catalysts are the Revenant's (" + revenantDrops + ")", revenantDrops >= 7);
        boolean noSeraph = true;
        for (String p : played) noSeraph &= !p.contains("SERAPH");
        check("no Seraph drop is taken", noSeraph);
        check("every quest completed is a kill or a drop (" + completes + " completes, " + dry.size() + " dry, " + played.size() + " drops)",
                dry.size() + played.size() <= completes + 2 && dry.size() > 100);
        System.out.println("replay: " + played.size() + " drops played " + played + ", " + dry.size() + " dry kills");

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
