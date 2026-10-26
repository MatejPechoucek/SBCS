import com.sbcs.anim.WaterTimeline.Creature;
import com.sbcs.anim.WaterTimeline.Drop;
import com.sbcs.detect.DecisionLog;
import com.sbcs.detect.FishingWatcher;
import com.sbcs.detect.FishingWatcher.Tag;
import com.sbcs.detect.WaterDrops;

import java.util.ArrayList;
import java.util.List;

/**
 * Offline checks for the water mythics' detection (the Titanoboa, the Wiki Tiki). <b>No real line or nametag has been
 * recorded yet</b>: the catch lines are the wiki's, the drop, charm and loot share lines are lava fishing's proven
 * shapes with these creatures' names, and the nametags are lava's format. Replace them with real ones from the first
 * {@code fishing_line} / {@code water_mob} records.
 *
 *   javac -d out -cp <fabric+mc classpath> -sourcepath "src/main/java;tools" tools/WaterDetectTest.java
 */
public final class WaterDetectTest {
    static int passed, failed;

    static void check(String what, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + what);
        }
    }

    static final String BOA_CATCH = "A massive Titanoboa surfaces. Its body stretches as far as the eye can see.";
    static final String TIKI_CATCH = "The water bubbles and froths. A massive form emerges- you have disturbed the Wiki Tiki! You shall pay the price.";

    static String boa(String hp) {
        return "﴾ [Lv400]  Titanoboa " + hp + "/45M❤ ﴿";
    }

    static String tiki(String hp) {
        return "﴾ [Lv450]  Wiki Tiki " + hp + "/75M❤ ﴿";
    }

    public static void main(String[] a) {
        // ---- the lines
        check("the Titanoboa's catch line (wiki)", WaterDrops.ofCatch(BOA_CATCH) == Creature.TITANOBOA);
        check("the Wiki Tiki's catch line (wiki)", WaterDrops.ofCatch(TIKI_CATCH) == Creature.WIKI_TIKI);
        check("with its colour codes", WaterDrops.ofCatch("§aA massive §5Titanoboa §asurfaces. Its body stretches as far as the eye can see.") == Creature.TITANOBOA);
        check("a double hook catch", WaterDrops.ofCatch("DOUBLE HOOK! " + BOA_CATCH) == Creature.TITANOBOA);
        check("Feesh's rewrite", WaterDrops.ofCatch("A Titanoboa has spawned!") == Creature.TITANOBOA);
        check("Feesh's rewrite of the Tiki", WaterDrops.ofCatch("A Wiki Tiki has spawned!") == Creature.WIKI_TIKI);
        check("a shard line is not a catch", WaterDrops.ofCatch("You received 2 Titanoboa Shards") == null);
        check("a death is not a catch", WaterDrops.ofCatch("☠ You were killed by Titanoboa.") == null);
        check("a Laser Totem is never the Tiki", WaterDrops.ofCatch("The Wiki Tiki's Laser Totem has spawned!") == null);
        check("the Shed", WaterDrops.of("RARE DROP! Titanoboa Shed (+312 ✯ Magic Find)", "qMighty") == Drop.SHED);
        check("the Shed, bracketed as a slayer's", WaterDrops.of("CRAZY RARE DROP! (Titanoboa Shed) (+312% ✯ Magic Find)", "qMighty") == Drop.SHED);
        check("Snake Eyes", WaterDrops.of("CRAZY RARE DROP! Snake Eyes (+312 ✯ Magic Find)", "qMighty") == Drop.SNAKE_EYES);
        check("the Troubled Bubble", WaterDrops.of("RARE DROP! Troubled Bubble (+200 ✯ Magic Find)", "qMighty") == Drop.BUBBLE);
        check("the Tiki Mask", WaterDrops.of("CRAZY RARE DROP! Tiki Mask (+200 ✯ Magic Find)", "qMighty") == Drop.TIKI_MASK);
        check("Bobbin' Scriptures", WaterDrops.of("RARE DROP! Bobbin' Scriptures (+200 ✯ Magic Find)", "qMighty") == Drop.SCRIPTURES);
        check("the dye's drop line", WaterDrops.of("PRAY TO RNGESUS DROP! Aquamarine Dye (+200 ✯ Magic Find)", "qMighty") == Drop.AQUAMARINE);
        check("the dye's WOW! line, ours", WaterDrops.of("WOW! qMighty found an Aquamarine Dye!", "qMighty") == Drop.AQUAMARINE);
        check("someone else's WOW! line is not ours", WaterDrops.of("WOW! SomeoneElse found an Aquamarine Dye!", "qMighty") == null);
        check("another dye's WOW! is not ours", WaterDrops.of("WOW! qMighty found a Carmine Dye!", "qMighty") == null);
        check("a Condensed Lily Pad is a bust", WaterDrops.of("RARE DROP! Condensed Lily Pad (+200 ✯ Magic Find)", "qMighty") == Drop.BUST);
        check("a loot share is not a drop", WaterDrops.of("LOOT SHARE You received loot for assisting Friend!", "qMighty") == null);
        check("a share naming the Titanoboa", WaterDrops.ofShare("§e§lLOOT SHARE §fYou received §b2 §5Titanoboa §fShards for assisting §bFriend§f!") == Creature.TITANOBOA);
        check("loot for assisting names nobody", WaterDrops.ofShare("LOOT SHARE You received loot for assisting Friend!") == WaterDrops.SHARE_UNNAMED);
        check("a charm", WaterDrops.ofCharm("CHARM! You charmed the Titanoboa and received 2 Titanoboa Shards!") == Creature.TITANOBOA);
        check("a nametag with health", WaterDrops.ofNametag(boa("30.2M")) == Creature.TITANOBOA);
        check("the Tiki's nametag", WaterDrops.ofNametag(tiki("75M")) == Creature.WIKI_TIKI);
        check("a Laser Totem's nametag is not the Tiki's", WaterDrops.ofNametag("[Lv1] Wiki Tiki Laser Totem 5/5❤") == null);
        check("a nametag without health is not taken new", WaterDrops.ofNametag("﴾ [Lv450]  Wiki Tiki ﴿") == null);
        check("a foraging Tiki critter is not the Wiki Tiki", WaterDrops.ofNametag("[Lv1] Sneaky Tiki 20/20❤") == null);

        // ---- the watcher
        List<String> opened = new ArrayList<>();
        FishingWatcher w = new FishingWatcher(new DecisionLog(), new FishingWatcher.Listener() {
            public void kill(Creature c, String why) { opened.add("kill " + c); }
            public void resolve(Drop d, String line) { opened.add("resolve " + d); }
            public void killKnown(Creature c, Drop d, String line) { opened.add("known " + c + " " + d); }
        });
        w.self = "qMighty";
        long t = 1_000_000;

        // Our own Titanoboa: caught, its segments' nametags (two carry health), killed, a Shed.
        w.onChatLine(BOA_CATCH, t);
        w.observe(List.of(new Tag(10, boa("45M"), 36), new Tag(14, boa("45M"), 64)), false, t + 800);
        w.observe(List.of(new Tag(10, boa("2M"), 36), new Tag(14, boa("2M"), 64)), false, t + 30_000);
        w.observe(List.of(new Tag(10, boa("0"), 36), new Tag(14, boa("0"), 64)), false, t + 31_000);
        check("our Titanoboa's death opens once, pending (" + opened + ")", opened.equals(List.of("kill TITANOBOA")));
        w.onChatLine("RARE DROP! Titanoboa Shed (+312 ✯ Magic Find)", t + 31_900);
        check("its Shed resolves it (" + opened + ")", opened.equals(List.of("kill TITANOBOA", "resolve SHED")));

        // Our own Wiki Tiki: immune while its totems stand (no health on its nametag), then killed - no drop: a bust.
        opened.clear();
        t += 100_000;
        w.onChatLine(TIKI_CATCH, t);
        w.observe(List.of(new Tag(20, tiki("75M"), 49), new Tag(21, "[Lv1] Laser Totem 5/5❤", 25)), false, t + 1_000);
        w.observe(List.of(new Tag(20, "﴾ [Lv450]  Wiki Tiki ﴿ INVULNERABLE", 49), new Tag(21, "[Lv1] Laser Totem 5/5❤", 25)), false, t + 10_000);
        w.observe(List.of(new Tag(20, "﴾ [Lv450]  Wiki Tiki ﴿ INVULNERABLE", 49)), false, t + 11_000);
        check("the Tiki's immune phase and a totem's death open nothing (" + opened + ")", opened.isEmpty());
        w.observe(List.of(new Tag(20, tiki("0"), 49)), false, t + 40_000);
        check("its death opens pending (" + opened + ")", opened.equals(List.of("kill WIKI_TIKI")));
        w.afterTick(t + 43_200);
        check("no drop in 3 s is a bust (" + opened + ")", opened.equals(List.of("kill WIKI_TIKI", "resolve BUST")));

        // Someone else's Titanoboa: dies near us; nothing opens without a win.
        opened.clear();
        t += 100_000;
        w.observe(List.of(new Tag(30, boa("10M"), 100)), false, t);
        w.observe(List.of(new Tag(30, boa("0"), 100)), false, t + 5_000);
        check("a party member's Titanoboa opens nothing by itself (" + opened + ")", opened.isEmpty());
        // ... unless its win comes to us by loot share.
        w.observe(List.of(new Tag(31, boa("10M"), 100)), false, t + 60_000);
        w.observe(List.of(new Tag(31, boa("0"), 100)), false, t + 65_000);
        w.onChatLine("LOOT SHARE You received 2 Titanoboa Shards for assisting Friend!", t + 65_040);
        w.onChatLine("CRAZY RARE DROP! Snake Eyes (+312 ✯ Magic Find)", t + 65_080);
        check("a loot-shared Snake Eyes plays it, known (" + opened + ")", opened.equals(List.of("known TITANOBOA SNAKE_EYES")));

        // A drop line before the nametag reads 0: the outcome is known at the death.
        opened.clear();
        t += 100_000;
        w.onChatLine("A Wiki Tiki has spawned!", t);
        w.observe(List.of(new Tag(40, tiki("75M"), 16)), false, t + 500);
        w.onChatLine("CRAZY RARE DROP! Tiki Mask (+200 ✯ Magic Find)", t + 40_000);
        w.observe(List.of(new Tag(40, tiki("0"), 16)), false, t + 40_040);
        check("the mask before the 0 opens known (" + opened + ")", opened.equals(List.of("known WIKI_TIKI TIKI_MASK")));

        // A Tiki-only drop cannot resolve a Titanoboa's kill.
        opened.clear();
        t += 100_000;
        w.onChatLine(BOA_CATCH, t);
        w.observe(List.of(new Tag(50, boa("45M"), 16)), false, t + 500);
        w.observe(List.of(new Tag(50, boa("0"), 16)), false, t + 30_000);
        w.onChatLine("RARE DROP! Troubled Bubble (+200 ✯ Magic Find)", t + 30_500);
        check("a Troubled Bubble is not the Titanoboa's (" + opened + ")", opened.equals(List.of("kill TITANOBOA")));
        w.onChatLine("WOW! qMighty found an Aquamarine Dye!", t + 30_900);
        check("the dye resolves either (" + opened + ")", opened.equals(List.of("kill TITANOBOA", "resolve AQUAMARINE")));

        // A nametag going at low health, close, counts as the kill; going at high health does not.
        opened.clear();
        t += 100_000;
        w.onChatLine(BOA_CATCH, t);
        w.observe(List.of(new Tag(60, boa("45M"), 16)), false, t + 500);
        w.observe(List.of(new Tag(60, boa("30M"), 16)), false, t + 9_000);
        w.observe(List.of(), false, t + 10_000);
        check("gone at 30M is not a kill (" + opened + ")", opened.isEmpty());
        w.onChatLine(BOA_CATCH, t + 20_000);
        w.observe(List.of(new Tag(61, boa("45M"), 16)), false, t + 20_500);
        w.observe(List.of(new Tag(61, boa("1.2M"), 16)), false, t + 50_000);
        w.observe(List.of(), false, t + 50_050);
        check("gone at 1.2M of 45M is the kill (" + opened + ")", opened.equals(List.of("kill TITANOBOA")));

        // The dye with no kill seen at all (out of range): ours by its own line, from the creature we last caught.
        opened.clear();
        w.afterTick(t + 60_000);
        opened.clear();
        t += 100_000;
        w.onChatLine(TIKI_CATCH, t);
        w.onChatLine("PRAY TO RNGESUS DROP! Aquamarine Dye (+200 ✯ Magic Find)", t + 50_000);
        check("a dye with no kill seen plays for the last catch (" + opened + ")", opened.equals(List.of("known WIKI_TIKI AQUAMARINE")));

        // The pre-kill hide: only our own, at the threshold.
        FishingWatcher h = new FishingWatcher(new DecisionLog(), new FishingWatcher.Listener() {
            public void kill(Creature c, String why) { }
            public void resolve(Drop d, String line) { }
            public void killKnown(Creature c, Drop d, String line) { }
        });
        long u = 5_000_000;
        h.observe(List.of(new Tag(70, boa("2M"), 16)), false, u);
        h.arm(10, u + 1_000);
        check("someone else's low Titanoboa does not hide the HUD", !h.armedAt(u + 1_000));
        h.onChatLine(BOA_CATCH, u + 5_000);
        h.observe(List.of(new Tag(71, boa("45M"), 16)), false, u + 5_500);
        h.arm(10, u + 6_000);
        check("our full-health Titanoboa does not", !h.armedAt(u + 6_000));
        h.observe(List.of(new Tag(71, boa("4M"), 16)), false, u + 30_000);
        h.arm(10, u + 30_000);
        check("ours at 4M of 45M does (10%)", h.armedAt(u + 30_000));

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
