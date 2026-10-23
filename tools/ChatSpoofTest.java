import com.sbcs.anim.ChopTimeline.Creature;
import com.sbcs.anim.ChopTimeline.Verdict;
import com.sbcs.anim.EruptionTimeline;
import com.sbcs.detect.ChatLines;
import com.sbcs.detect.DecisionLog;
import com.sbcs.detect.DianaWatcher;
import com.sbcs.detect.LavaWatcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Hypixel sends players' chat as game messages, so "a player typed it" has to be read off the line itself
 * (detect/ChatLines). Checks that every real line the detectors were written from is still read as the
 * game's, that what a player could type is not, and that a typed dig or catch line no longer makes a
 * creature ours.
 *
 *   javac -d out -cp <fabric+mc classpath> -sourcepath "src/main/java:tools" tools/ChatSpoofTest.java
 */
public final class ChatSpoofTest {
    static int passed, failed;

    static void check(String what, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + what);
        }
    }

    /** Lines Hypixel (or Feesh, or SkyHanni) printed, from the records and logs the detectors were written against. */
    static final String[] GAME = {
            "RARE DROP! Enchanted Book (Flash I) (+436  Magic Find)",
            "RARE DROP! Burnt Texts (+384  Magic Find)",
            "RARE DROP! Nether Star",
            "VERY RARE DROP! (Scorched Power Crystal) (+236%  Magic Find)",
            "VERY RARE DROP! (◆ Enchant Rune I) (+221%  Magic Find)",
            "CRAZY RARE DROP! (Judgement Core) (+221%  Magic Find)",
            "PRAY TO RNGESUS DROP! (Byzantium Dye) (+221%  Magic Find)",
            "RARE DROP! Antique Remedies (+277  Magic Find)",
            "WOW! [MVP+] qMighty found a Mythological Dye!",
            "Oi! You dug out a Minos Inquisitor!",
            "Good Grief! You dug out a Minos Inquisitor!",
            "LOOT SHARE You received loot for assisting Friend5!",
            "LOOT SHARE You received 2 Thunder Shards for assisting Friend7!",
            "CHARM! You charmed the Thunder and received 2 Thunder Shards!",
            "SLAYER BOSS! The Inferno Demonlord IV spawned!",
            "SLAYER QUEST STARTED!",
            "SLAYER QUEST COMPLETE!",
            "SLAYER QUEST FAILED!",
            "You hear a massive rumble as Thunder emerges.",
            "A Thunder has spawned!",
            "DOUBLE HOOK! A Yeti has spawned!",
            "You have angered a legendary creature... Lord Jawbus has arrived.",
            "The sky darkens and the air thickens. The end times are upon us: Ragnarok is here.",
            "☠ You were killed by Ragnarok.",
            "✦ You placed the Topaz Crystal!",
            "  CRYSTAL NUCLEUS LOOT BUNDLE",
            "  VANGUARD CORPSE LOOT!",
            "PET DROP! EPIC Slug (+1300☘)",
            "[SkyHanni] Profit for Crystal Nucleus Run: -829k",
    };

    /** What another player could type: every chat channel Hypixel has, ranks, levels, emblems, guild tags. */
    static final String[] TYPED = {
            "Player123: hey what did you get",
            "[MVP+] Someone: Oi! You dug out a Minos Inquisitor!",
            "[VIP] Someone: You hear a massive rumble as Thunder emerges.",
            "Someone: A Thunder has spawned!",
            "[302] [MVP++] Someone: RARE DROP! Minos Relic",
            "[302] ♲ Someone: You dug out a Sphinx!",
            "[302] ⚒ [MVP+] Someone [GUILD]: Lord Jawbus has arrived.",
            "Party > [MVP+] Someone: You dug out a Manticore!",
            "Guild > [MVP++] Someone: VERY RARE DROP! (Judgement Core)",
            "Guild > [VIP] Someone [Officer]: hi",
            "Co-op > Someone: CHARM! You charmed the Thunder and received",
            "Officer > [MVP+] Someone: SLAYER QUEST COMPLETE!",
            "From [MVP+] Someone: LOOT SHARE You received loot for assisting X",
            "To [MVP+] Someone: Good Grief! You dug out a Minos Inquisitor!",
            "[NPC] Kat: hello",
    };

    public static void main(String[] a) throws Exception {
        for (String s : GAME) check("game line read as typed: " + s, !ChatLines.fromPlayer(s));
        for (String s : TYPED) check("typed line read as the game's: " + s, ChatLines.fromPlayer(s));

        // Every line of the real Nucleus messages too (the fixture holds whole messages, SkyHanni's line in them).
        Path fixture = Path.of("tools/fixtures/nucleus_hauls.txt");
        if (Files.exists(fixture)) {
            int n = 0;
            for (String line : Files.readAllLines(fixture)) {
                if (line.startsWith("#") || line.startsWith("===")) continue;
                n++;
                check("Nucleus fixture line read as typed: " + line, !ChatLines.fromPlayer(DecisionLog.strip(line)));
            }
            check("the Nucleus fixture has lines (" + n + ")", n > 20);
        }

        // A typed dig line must not make a nearby creature ours: the watcher must not even note it.
        List<String> notes = new ArrayList<>();
        DecisionLog.tap = notes::add;
        List<String> heard = new ArrayList<>();
        DianaWatcher diana = new DianaWatcher(new DecisionLog(), new DianaWatcher.Listener() {
            public void kill(Creature c) { heard.add("kill " + c); }
            public void drop(Verdict v, String line) { heard.add("drop " + v); }
        });
        diana.onChatLine("[MVP+] Someone: Oi! You dug out a Minos Inquisitor!");
        check("a typed dig line is ignored (" + notes + ")", notes.stream().noneMatch(n -> n.contains("you dug out")));
        diana.onChatLine("§2Oi! §eYou dug out a §2Minos Inquisitor§e!");
        check("the real dig line is still read (" + notes + ")", notes.stream().anyMatch(n -> n.contains("you dug out")));
        diana.onChatLine("Party > [MVP+] Someone: RARE DROP! Minos Relic");
        check("a typed drop in party chat opens nothing (" + heard + ")", heard.isEmpty());
        DecisionLog.tap = null;

        // A typed catch line must not claim the next Thunder nametag; the real one still does.
        List<String> opened = new ArrayList<>();
        LavaWatcher lava = new LavaWatcher(new DecisionLog(), new LavaWatcher.Listener() {
            public void kill(EruptionTimeline.Creature c, String why) { opened.add("kill " + c); }
            public void resolve(EruptionTimeline.Drop d, String line) { opened.add("resolve " + d); }
            public void killKnown(EruptionTimeline.Creature c, EruptionTimeline.Drop d, String line) { opened.add("known " + c + " " + d); }
        });
        long t = 1_000_000;
        String thu = "[Lv400]  Thunder 3M/35M❤", thuDead = "[Lv400]  Thunder 0/35M❤";
        lava.onChatLine("[VIP] Someone: You hear a massive rumble as Thunder emerges.", t);
        lava.observe(List.of(new LavaWatcher.Tag(60, thu, 12)), false, t + 300);
        lava.observe(List.of(new LavaWatcher.Tag(60, thuDead, 12)), false, t + 9_000);
        lava.afterTick(t + 14_000);
        check("someone else's Thunder after a typed catch line opens nothing (" + opened + ")", opened.isEmpty());
        lava.onChatLine("You hear a massive rumble as Thunder emerges.", t + 30_000);
        lava.observe(List.of(new LavaWatcher.Tag(61, thu, 12)), false, t + 30_300);
        lava.observe(List.of(new LavaWatcher.Tag(61, thuDead, 12)), false, t + 39_000);
        lava.afterTick(t + 44_000);
        check("our own Thunder still opens (" + opened + ")", opened.equals(List.of("kill THUNDER", "resolve BUST")));

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
