import com.sbcs.ledger.Chapter;
import com.sbcs.ledger.Facts;
import com.sbcs.ledger.Ledger;
import com.sbcs.ledger.LedgerData;
import com.sbcs.ledger.Stamp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Offline checks for the Ledger's rules (com.sbcs.ledger is pure Java).
 *
 *   javac -d out/ledger src/main/java/com/sbcs/ledger/*.java tools/LedgerTest.java
 *   java -cp out/ledger LedgerTest
 *
 * Run from the repository root: it reads the game's drop enums as source text, to check every name a stamp looks for
 * is spelled the way the stats are written.
 */
public class LedgerTest {
    static int pass = 0, fail = 0;

    static void check(String what, boolean ok, String detail) {
        if (ok) pass++;
        else {
            fail++;
            System.out.println("FAIL " + what + (detail.isEmpty() ? "" : " - " + detail));
        }
    }

    static void check(String what, boolean ok) {
        check(what, ok, "");
    }

    /** The stamps whose value is an event, kept by award() - the rest are counted from the stats. */
    static final Set<String> EVENTS = Set.of("house_hot_streak", "house_night_shift", "house_jackpot_777",
            "scatha_back_to_back", "vg_negative_equity", "vg_in_the_black", "diana_shared_custody", "lava_jaw_dropping",
            "fin_bigger_fish", "scatha_beginners_luck", "house_big_ticket", "fin_feeding_frenzy", "wisp_double_haunting", "wisp_poltergeist",
            "wisp_matched_pair", "wisp_witching_hour",
            // The secret stamps (2026-10-17).
            "house_unlucky_13", "house_leap_of_faith", "house_christmas_ham", "house_two_timer", "house_health_inspector",
            "house_natural", "house_loose_change", "scatha_round_number", "scatha_is_that_it", "scatha_cereal_killer",
            "vg_brain_freeze", "diana_nobody_expects", "hellion_number_beast", "hellion_snake_eyes", "lava_hot_potato",
            "fin_bigger_boat", "wisp_aint_afraid",
            // The slayer chapters (2026-10-26).
            "rev_back_from_dead", "rev_trick_or_treat", "tar_eight_legs");

    static final ZoneId UTC = ZoneOffset.UTC;

    static long at(int hour, int minute) {
        return at(16, hour, minute);
    }

    static long at(int day, int hour, int minute) {
        return LocalDateTime.of(2026, 10, day, hour, minute).atZone(UTC).toInstant().toEpochMilli();
    }

    public static void main(String[] a) throws Exception {
        // ---- the table
        check("59 stamps or more", Ledger.STAMPS.size() >= 59, "" + Ledger.STAMPS.size());
        Set<String> ids = new java.util.HashSet<>();
        for (Stamp s : Ledger.STAMPS) {
            check("unique id " + s.id(), ids.add(s.id()));
            if (s.tiered()) {
                check(s.id() + " tiered has tiers", s.tiers().length >= 2);
                for (int i = 1; i < s.tiers().length; i++) check(s.id() + " tiers rise", s.tiers()[i] > s.tiers()[i - 1]);
            }
        }
        for (Chapter c : Chapter.values()) check("chapter " + c + " has stamps", !Ledger.chapter(c).isEmpty());

        // ---- every counted stamp is wired to a number: with everything at plenty, it reads more than 0
        Facts rich = rich();
        LedgerData d0 = new LedgerData();
        d0.cutscenes = 99_999;
        d0.worstBustRun = 999;
        d0.skips = 999;
        d0.coins = 99_000_000_000L;
        d0.firstPetKill = 1;
        for (Stamp s : Ledger.STAMPS) {
            if (EVENTS.contains(s.id())) {
                check(s.id() + " is 0 until it happens", Ledger.value(s, rich, d0) == 0);
                Ledger.award(d0, s.id(), 7);
                check(s.id() + " counts once awarded", Ledger.gradeFor(s, Ledger.value(s, rich, d0)) >= 1);
            } else {
                check(s.id() + " reads the stats", Ledger.value(s, rich, d0) > 0, "value 0 with every number high");
            }
        }

        // ---- the names stamps look for are the game's own
        String hellion = Files.readString(Path.of("src/main/java/com/sbcs/anim/HellionTimeline.java"));
        String eruption = Files.readString(Path.of("src/main/java/com/sbcs/anim/EruptionTimeline.java"));
        String shark = Files.readString(Path.of("src/main/java/com/sbcs/detect/SharkDrops.java"));
        String ghost = Files.readString(Path.of("src/main/java/com/sbcs/detect/GhostDrops.java"));
        String diana = Files.readString(Path.of("src/main/java/com/sbcs/render/DianaItems.java"));
        for (String n : HELLION) check("Hellion names " + n, hellion.contains("\"" + n + "\""));
        for (String n : ERUPTION) check("Eruption names " + n, eruption.contains("\"" + n + "\""));
        String vanguard = Files.readString(Path.of("src/main/java/com/sbcs/detect/vanguard/VanguardLoot.java"));
        for (String n : List.of("Caged Wisp", "Frozen Scute", "Skeleton Key")) {
            check("Vanguard names " + n, vanguard.contains("\"" + n + "\""));
        }
        String chop = Files.readString(Path.of("src/main/java/com/sbcs/anim/ChopTimeline.java"));
        check("Diana creature INQUISITOR", chop.contains("INQUISITOR"));
        for (com.sbcs.ledger.Chapter c : com.sbcs.ledger.Chapter.values()) {
            check("10 or more stamps in " + c, Ledger.chapter(c).size() >= 10, Ledger.chapter(c).size() + "");
        }
        // The slayer chapters fit on one page of the book (12 a page), and each has a secret.
        for (Chapter c : List.of(Chapter.SERAPH, Chapter.REVENANT, Chapter.TARANTULA)) {
            check(c + " fits one page", Ledger.chapter(c).size() <= 12, Ledger.chapter(c).size() + "");
            check(c + " has a secret", Ledger.chapter(c).stream().anyMatch(Stamp::hidden));
        }
        String seraph = Files.readString(Path.of("src/main/java/com/sbcs/anim/SeraphTimeline.java"));
        for (String n : SERAPH) check("Seraph names " + n, seraph.contains("\"" + n + "\""));
        String slayer = Files.readString(Path.of("src/main/java/com/sbcs/anim/SlayerTimeline.java"));
        for (String n : REVENANT) check("Revenant names " + n, slayer.contains("\"" + n + "\""));
        for (String n : TARANTULA) check("Tarantula names " + n, slayer.contains("\"" + n + "\""));
        check("the Revenant's key is zombie", slayer.contains("REVENANT(\"zombie\""));
        check("the Tarantula's key is spider", slayer.contains("TARANTULA(\"spider\""));
        check("the Seraph's key is seraph", Files.readString(Path.of("src/main/java/com/sbcs/gui/SeraphScreen.java"))
                .contains("SOURCE = \"seraph\""));
        icons();
        for (String n : List.of("Megalodon", "Great White Shark Tooth")) check("Fin names " + n, shark.contains("\"" + n + "\""));
        for (String n : List.of("Sorrow", "Plasma")) check("Wisp names " + n, ghost.contains("\"" + n + "\""));
        for (String n : List.of("Brain Food", "Chimera I", "Fateful Stinger", "Manti-core", "Crown of Greed",
                "Shimmering Wool", "Minos Relic", "Mythological Dye")) {
            check("Diana names " + n, diana.contains("\"" + n + "\""));
        }

        // ---- backfill: the stats already on disk earn their stamps, undated and unannounced
        Stamp worm = Ledger.byId("scatha_worm_food");
        Stamp dry = Ledger.byId("scatha_dry_aged");
        Facts f = new Facts();
        f.scathaKills = worm.need(2) + 1;
        f.scathaWorstDry = dry.need(1);
        f.petsEpic = 1;
        LedgerData d = new LedgerData();
        List<Stamp> fresh = Ledger.refresh(f, d, 123, true);
        check("backfill announces nothing", fresh.isEmpty());
        check("backfill grade from kills", Ledger.grade(worm, d) == 2, "" + Ledger.grade(worm, d));
        check("backfill is undated", d.stamps.get(worm.id()).at == 0);
        check("backfill dry aged", Ledger.grade(dry, d) == 1);
        check("backfill epic pet", Ledger.grade(Ledger.byId("scatha_choice_cut"), d) == 1);
        check("no legendary pet", Ledger.grade(Ledger.byId("scatha_prime_cut"), d) == 0);

        // ---- live: a grade is raised, dated and announced once
        f.scathaKills = worm.need(3);
        fresh = Ledger.refresh(f, d, 5_000, false);
        check("raised a grade", fresh.contains(worm) && Ledger.grade(worm, d) == 3);
        check("dated", d.stamps.get(worm.id()).at == 5_000);
        check("fresh ink", !d.seen.contains(worm.id()));
        check("announced once", Ledger.refresh(f, d, 6_000, false).isEmpty());
        f.scathaKills = 0;
        Ledger.refresh(f, d, 7_000, false);
        check("never taken away", Ledger.grade(worm, d) == 3);
        check("a grade past the last tier is the last", Ledger.grade(worm, d) <= worm.grades());

        // ---- luck: hot streak, night shift, jackpot, back to back, beginner's luck
        LedgerData luck = new LedgerData();
        Ledger.drop(luck, "blaze", "Archfiend Dice", 10, at(14, 0), UTC);
        check("one drop is no streak", !luck.flags.containsKey("house_hot_streak"));
        Ledger.drop(luck, "lava", "Flash I", 5, at(14, 9), UTC);
        check("two within 10 minutes", luck.flags.containsKey("house_hot_streak"));
        LedgerData luck2 = new LedgerData();
        Ledger.drop(luck2, "blaze", "Archfiend Dice", 10, at(14, 0), UTC);
        Ledger.drop(luck2, "blaze", "Archfiend Dice", 11, at(14, 11), UTC);
        check("eleven minutes apart is no streak", !luck2.flags.containsKey("house_hot_streak"));
        check("2 PM is not the night shift", !luck2.flags.containsKey("house_night_shift"));
        Ledger.drop(luck2, "diana", "Brain Food", 3, at(17, 3, 30), UTC);
        check("3:30 AM is the night shift", luck2.flags.containsKey("house_night_shift"));
        Ledger.drop(luck2, "diana", "Brain Food", 4, at(17, 5, 0), UTC);
        Ledger.drop(luck2, "lava", "Flash I", 777, at(17, 12, 0), UTC);
        check("777th kill", luck2.flags.get("house_jackpot_777") == 777L);
        Ledger.drop(luck2, "ghost", "Sorrow", 0, at(17, 13, 0), UTC);
        check("a ghost drop has no kill to be 777th", luck2.flags.get("house_jackpot_777") == 777L);

        long within = Ledger.byId("scatha_back_to_back").number(50);
        LedgerData b2b = new LedgerData();
        Ledger.drop(b2b, "scatha", "epic", 100, at(10, 0), UTC);
        Ledger.drop(b2b, "scatha", "rare", 100 + within + 1, at(18, 0), UTC);
        check("pets too far apart", !b2b.flags.containsKey("scatha_back_to_back"));
        Ledger.drop(b2b, "scatha", "rare", 100 + within + 1 + within, at(20, 0), UTC);
        check("pets within reach", b2b.flags.containsKey("scatha_back_to_back"));
        check("first pet kill kept", b2b.firstPetKill == 100);

        // Beginner's Luck: a pet within the day's first N kills, the count starting again each day.
        Stamp beginner = Ledger.byId("scatha_beginners_luck");
        long n = beginner.number(10);
        LedgerData late = new LedgerData();
        for (int i = 0; i < n + 1; i++) Ledger.scathaKill(late, at(9, i), UTC);
        Ledger.drop(late, "scatha", "rare", 500, at(9, 30), UTC);
        Ledger.refresh(new Facts(), late, 1, false);
        check("pet one kill too late in the day", Ledger.grade(beginner, late) == 0);
        Ledger.scathaKill(late, at(17, 8, 0), UTC);
        check("a new day starts the count again", late.scathaDayKills == 1);
        Ledger.drop(late, "scatha", "epic", 501, at(17, 8, 1), UTC);
        Ledger.refresh(new Facts(), late, 1, false);
        check("pet on the first kill of the next day", Ledger.grade(beginner, late) == 1);
        LedgerData lucky = new LedgerData();
        for (int i = 0; i < n; i++) Ledger.scathaKill(lucky, at(9, i), UTC);
        Ledger.drop(lucky, "scatha", "rare", 9000, at(9, 30), UTC);
        check("pet on the day's Nth kill, whatever the lifetime count", lucky.flags.containsKey("scatha_beginners_luck"));

        // ---- the House's runs, corpses' worth
        LedgerData runs = new LedgerData();
        for (int i = 0; i < 5; i++) Ledger.run(runs, false);
        Ledger.run(runs, true);
        Ledger.run(runs, false);
        check("bust run", runs.bustRun == 1 && runs.worstBustRun == 5 && runs.cutscenes == 7);
        Stamp black = Ledger.byId("vg_in_the_black");
        LedgerData worth = new LedgerData();
        Ledger.corpseWorth(worth, -12_000, 40_000, 3);
        check("negative equity", worth.flags.containsKey("vg_negative_equity") && !worth.flags.containsKey("vg_in_the_black"));
        Ledger.corpseWorth(worth, black.number(0), black.number(0) + 50_000, 4);
        check("in the black", worth.flags.get("vg_in_the_black") == 4L);
        check("coins counted", worth.coins == 40_000 + black.number(0) + 50_000);
        check("recent is capped", capped());

        // ---- every event, once awarded, is stamped - numbered ones too (a flag read as 1 never met "50 kills")
        for (String id : EVENTS) {
            Stamp e = Ledger.byId(id);
            LedgerData one = new LedgerData();
            Ledger.award(one, id, 7);
            Ledger.refresh(new Facts(), one, 1, false);
            check(id + " stamps once awarded", Ledger.grade(e, one) == 1, "number " + e.number(0));
        }

        // ---- the Fin and Wisp runs of luck
        LedgerData runs2 = new LedgerData();
        Ledger.drop(runs2, "shark", "Megalodon", 10, at(14, 0), UTC);
        check("one shark drop is no frenzy", !runs2.flags.containsKey("fin_feeding_frenzy"));
        Ledger.drop(runs2, "lava", "Flash I", 5, at(14, 2), UTC);
        check("another source's drop is no frenzy", !runs2.flags.containsKey("fin_feeding_frenzy"));
        Ledger.drop(runs2, "shark", "Great White Shark Tooth", 12, at(14, 8), UTC);
        check("two shark drops in 10 minutes", runs2.flags.containsKey("fin_feeding_frenzy"));
        LedgerData g = new LedgerData();
        long t0 = at(13, 0);
        Ledger.drop(g, "ghost", "Sorrow", 0, t0, UTC);
        Ledger.drop(g, "ghost", "Plasma", 0, t0 + 61_000, UTC);
        check("61 seconds apart is no double haunting", !g.flags.containsKey("wisp_double_haunting"));
        check("61 seconds apart is no matched pair", !g.flags.containsKey("wisp_matched_pair"));
        Ledger.drop(g, "ghost", "Plasma", 0, t0 + 61_000 + 31_000, UTC);
        check("31 seconds apart is no double haunting", !g.flags.containsKey("wisp_double_haunting"));
        check("two Plasmas are no matched pair", !g.flags.containsKey("wisp_matched_pair"));
        check("two in a minute is no poltergeist", !g.flags.containsKey("wisp_poltergeist"));
        Ledger.drop(g, "ghost", "Sorrow", 0, t0 + 61_000 + 31_000 + 20_000, UTC);
        check("double haunting in 20 seconds", g.flags.containsKey("wisp_double_haunting"));
        check("poltergeist: three in 51 seconds", g.flags.containsKey("wisp_poltergeist"));
        check("matched pair within a minute", g.flags.containsKey("wisp_matched_pair"));
        check("1 PM is no witching hour", !g.flags.containsKey("wisp_witching_hour"));
        Ledger.drop(g, "ghost", "Sorrow", 0, at(17, 0, 30), UTC);
        check("witching hour", g.flags.containsKey("wisp_witching_hour"));

        // ---- the secret stamps
        int secrets = 0;
        for (Stamp s : Ledger.STAMPS) if (s.hidden()) {
            secrets++;
            check(s.id() + " has a hint", s.hint() != null && !s.hint().isEmpty());
        }
        check("20 or more secret stamps", secrets >= 20, "" + secrets);
        LedgerData dates = new LedgerData();
        Ledger.drop(dates, "lava", "Flash I", 40, LocalDateTime.of(2026, 11, 13, 12, 0).atZone(UTC).toInstant().toEpochMilli(), UTC);
        check("Friday the 13th", dates.flags.containsKey("house_unlucky_13"));
        Ledger.drop(dates, "lava", "Flash I", 41, LocalDateTime.of(2027, 12, 25, 12, 0).atZone(UTC).toInstant().toEpochMilli(), UTC);
        check("Christmas", dates.flags.containsKey("house_christmas_ham"));
        check("not a leap day", !dates.flags.containsKey("house_leap_of_faith"));
        Ledger.drop(dates, "lava", "Flash I", 42, LocalDateTime.of(2028, 2, 29, 12, 0).atZone(UTC).toInstant().toEpochMilli(), UTC);
        check("leap day", dates.flags.containsKey("house_leap_of_faith"));
        check("a year apart is no hot potato", !dates.flags.containsKey("lava_hot_potato"));
        LedgerData tues = new LedgerData();
        Ledger.drop(tues, "lava", "Flash I", 40, at(13, 12, 0), UTC);     // a Tuesday the 13th
        check("Tuesday the 13th is fine", !tues.flags.containsKey("house_unlucky_13"));
        check("no kill counted today is no natural", !tues.flags.containsKey("house_natural"));
        Ledger.kill(tues, "blaze", at(12, 23, 0), UTC);                    // yesterday's kill
        Ledger.kill(tues, "blaze", at(13, 12, 2), UTC);
        Ledger.kill(tues, "blaze", at(13, 12, 3), UTC);
        Ledger.drop(tues, "blaze", "Duplex I", 300, at(13, 12, 3), UTC);
        check("the day's second kill is no natural", !tues.flags.containsKey("house_natural"));
        Ledger.kill(tues, "blaze", at(14, 9, 0), UTC);                     // a new day starts the count again
        Ledger.drop(tues, "blaze", "Duplex I", 301, at(14, 9, 0), UTC);
        check("natural: a drop on the day's first kill", tues.flags.get("house_natural") == 301L);
        LedgerData scathaDay = new LedgerData();
        Ledger.scathaKill(scathaDay, at(9, 0), UTC);
        Ledger.drop(scathaDay, "scatha", "epic", 900, at(9, 0), UTC);
        check("natural: the Scatha's day count", scathaDay.flags.containsKey("house_natural"));
        LedgerData inq = new LedgerData();
        for (int i = 0; i < 6; i++) Ledger.kill(inq, "diana:INQUISITOR", at(10, i), UTC);
        check("six Inquisitors today", Ledger.killsToday(inq, "diana:INQUISITOR", at(10, 9), UTC) == 6);
        check("a new day has none", Ledger.killsToday(inq, "diana:INQUISITOR", at(17, 10, 0), UTC) == 0);
        check("nobody expects: 5 a day", Ledger.byId("diana_nobody_expects").number(0) == 5);
        check("two-timer: two animations in 3 minutes", tues.flags.containsKey("house_two_timer"));
        Ledger.drop(tues, "lava", "Flash I", 41, at(14, 9, 2), UTC);
        Ledger.drop(tues, "lava", "Flash I", 42, at(14, 9, 4), UTC);
        check("hot potato: two lava drops in 4 minutes", tues.flags.containsKey("lava_hot_potato"));

        LedgerData dice = new LedgerData();
        Ledger.drop(dice, "blaze", "Archfiend Dice", 20, at(10, 0), UTC);
        Ledger.drop(dice, "blaze", "Duplex I", 30, at(11, 0), UTC);
        Ledger.drop(dice, "blaze", "High Class Archfiend Dice", 40, at(12, 0), UTC);
        check("dice with a Duplex between is no snake eyes", !dice.flags.containsKey("hellion_snake_eyes"));
        Ledger.drop(dice, "lava", "Flash I", 9, at(12, 30), UTC);
        Ledger.drop(dice, "blaze", "Archfiend Dice", 666, at(13, 0), UTC);
        check("snake eyes: dice twice in a row (another source between)", dice.flags.get("hellion_snake_eyes") == 666L);
        check("number of the beast", dice.flags.get("hellion_number_beast") == 666L);

        LedgerData pets = new LedgerData();
        Ledger.drop(pets, "scatha", "epic", 250, at(10, 0), UTC);
        check("a first pet at 250 is not 'is that it' (epic)", !pets.flags.containsKey("scatha_is_that_it"));
        Ledger.drop(pets, "scatha", "rare", 500, at(11, 0), UTC);
        check("250 dry is not enough", !pets.flags.containsKey("scatha_is_that_it"));
        check("round number: kill 500", pets.flags.get("scatha_round_number") == 500L);
        Ledger.drop(pets, "scatha", "rare", 801, at(12, 0), UTC);
        check("is that it: a rare pet after 301 dry", pets.flags.get("scatha_is_that_it") == 801L);
        LedgerData firstRare = new LedgerData();
        Ledger.drop(firstRare, "scatha", "rare", 333, at(12, 0), UTC);
        check("is that it: a first pet, rare, at 333", firstRare.flags.containsKey("scatha_is_that_it"));
        LedgerData unknownGap = new LedgerData();
        unknownGap.firstPetKill = 40;                                      // backfilled: a pet, but not in the recent list
        Ledger.drop(unknownGap, "scatha", "rare", 900, at(12, 0), UTC);
        check("is that it: no guess when the last pet is unknown", !unknownGap.flags.containsKey("scatha_is_that_it"));

        LedgerData cereal = new LedgerData();
        Ledger.scathaResult(cereal, null, true, 1);
        Ledger.scathaResult(cereal, null, true, 2);
        Ledger.scathaResult(cereal, null, false, 3);                       // gemstones break the run
        Ledger.scathaResult(cereal, null, true, 4);
        Ledger.scathaResult(cereal, null, true, 5);
        check("two O's, gems, two O's is no cereal killer", !cereal.flags.containsKey("scatha_cereal_killer"));
        Ledger.scathaResult(cereal, null, true, 6);
        check("cereal killer: three O's in a row", cereal.flags.get("scatha_cereal_killer") == 6L);

        LedgerData sea = new LedgerData();
        long s0 = at(15, 0);
        Ledger.drop(sea, "shark", "Great White Shark Tooth", 10, s0, UTC);
        Ledger.drop(sea, "shark", "Great White Shark Tooth", 11, s0 + 4 * 60_000, UTC);
        check("two shark drops are no bigger boat", !sea.flags.containsKey("fin_bigger_boat"));
        Ledger.drop(sea, "shark", "Megalodon", 12, s0 + 9 * 60_000, UTC);
        check("bigger boat: three in 9 minutes", sea.flags.containsKey("fin_bigger_boat"));
        LedgerData boo = new LedgerData();
        for (int i = 0; i < 4; i++) Ledger.drop(boo, "ghost", "Sorrow", 0, s0 + i * 120_000L, UTC);
        check("four ghost drops are not five", !boo.flags.containsKey("wisp_aint_afraid"));
        Ledger.drop(boo, "ghost", "Plasma", 0, s0 + 9 * 60_000, UTC);
        check("ain't afraid: five in 9 minutes", boo.flags.containsKey("wisp_aint_afraid"));

        // ---- the slayer chapters' events
        LedgerData rev = new LedgerData();
        Ledger.drop(rev, "zombie", "Severed Hand", 50, at(10, 0), UTC);
        Ledger.drop(rev, "spider", "Fly Swatter", 51, at(10, 2), UTC);
        check("another slayer's drop is no back from the dead", !rev.flags.containsKey("rev_back_from_dead"));
        Ledger.drop(rev, "zombie", "Scythe Blade", 60, at(10, 6), UTC);
        check("six minutes is no back from the dead", !rev.flags.containsKey("rev_back_from_dead"));
        Ledger.drop(rev, "zombie", "Warden Heart", 61, at(10, 10), UTC);
        check("back from the dead: two in 4 minutes", rev.flags.get("rev_back_from_dead") == 61L);
        check("16 October is no Halloween", !rev.flags.containsKey("rev_trick_or_treat"));
        long halloween = LocalDateTime.of(2026, 10, 31, 20, 0).atZone(UTC).toInstant().toEpochMilli();
        Ledger.drop(rev, "spider", "Fly Swatter", 70, halloween, UTC);
        check("a Tarantula drop is no trick or treat", !rev.flags.containsKey("rev_trick_or_treat"));
        Ledger.drop(rev, "zombie", "Beheaded Horror", 71, halloween + 60_000, UTC);
        check("trick or treat: a Revenant drop on 31 October", rev.flags.get("rev_trick_or_treat") == 71L);

        LedgerData legs = new LedgerData();
        for (int i = 1; i <= 7; i++) Ledger.kill(legs, "spider", at(11, i), UTC);
        Ledger.drop(legs, "spider", "Fly Swatter", 7, at(11, 7), UTC);
        check("the 7th Tarantula is no eight legs", !legs.flags.containsKey("tar_eight_legs"));
        Ledger.kill(legs, "spider", at(11, 8), UTC);
        Ledger.drop(legs, "spider", "Ensnared Snail", 8, at(11, 8), UTC);
        check("eight legs: a drop on the day's 8th Tarantula", legs.flags.get("tar_eight_legs") == 8L);
        LedgerData legs2 = new LedgerData();
        for (int i = 1; i <= 7; i++) Ledger.kill(legs2, "spider", at(11, 23, i), UTC);
        Ledger.kill(legs2, "spider", at(12, 0, 30), UTC);                  // after midnight: the 1st of a new day
        Ledger.drop(legs2, "spider", "Fly Swatter", 8, at(12, 0, 30), UTC);
        check("eight legs counts the day's kills, not the lifetime's", !legs2.flags.containsKey("tar_eight_legs"));

        // The full sets: one of each, not one of any.
        Facts sets = new Facts();
        Map<String, Integer> z = new HashMap<>();
        for (String t : REVENANT) z.put(t, 1);
        z.remove("Matcha Dye");
        sets.drops.put("zombie", z);
        Map<String, Integer> sp = new HashMap<>();
        sp.put("Digested Mosquito", 3);
        sp.put("Shriveled Wasp", 1);
        sets.drops.put("spider", sp);
        LedgerData setsD = new LedgerData();
        check("full autopsy needs the Matcha too", Ledger.value(Ledger.byId("rev_full_autopsy"), sets, setsD) == 0);
        check("food chain needs the snail", Ledger.value(Ledger.byId("tar_food_chain"), sets, setsD) == 0);
        z.put("Matcha Dye", 1);
        sp.put("Ensnared Snail", 2);
        check("full autopsy: every Revenant ending", Ledger.value(Ledger.byId("rev_full_autopsy"), sets, setsD) == 1);
        check("food chain: all three bugs", Ledger.value(Ledger.byId("tar_food_chain"), sets, setsD) == 1);
        z.put("Paragon Shard", 4);
        check("a Paragon off a Revenant is not the Seraph's", Ledger.value(Ledger.byId("ser_paragon"), sets, setsD) == 0);

        // The Seraph's chapter is no longer skipped: its stamps are earned like any other.
        Facts ser = new Facts();
        ser.kills.put("seraph", 30L);
        ser.drops.put("seraph", new HashMap<>(Map.of("Judgement Core", 1, "Summoning Eye", 2)));
        LedgerData serD = new LedgerData();
        List<Stamp> serFresh = Ledger.refresh(ser, serD, 9_000, false);
        check("Seraph kills stamp", Ledger.grade(Ledger.byId("ser_voidwalker"), serD) == 1);
        check("Seraph's secret stamp", serFresh.contains(Ledger.byId("ser_staring_contest")));
        check("Seraph's Judgement Core", Ledger.grade(Ledger.byId("ser_judgement"), serD) == 1);

        // ---- fail-safes: a clock that went back, and a file that was edited by hand
        LedgerData clock = new LedgerData();
        Ledger.drop(clock, "blaze", "Archfiend Dice", 10, at(14, 0), UTC);
        Ledger.drop(clock, "diana", "Brain Food", 3, at(3, 30), UTC);     // earlier than the last drop
        check("clock back: no night shift", !clock.flags.containsKey("house_night_shift"));
        check("clock back: no hot streak", !clock.flags.containsKey("house_hot_streak"));
        check("clock back: the drop is still kept", clock.recent.size() == 2);
        Ledger.drop(clock, "lava", "Flash I", 777, at(3, 35), UTC);       // past the last drop again
        check("clock back: the count needs no clock", clock.flags.get("house_jackpot_777") == 777L);
        check("clock fine again: night shift", clock.flags.containsKey("house_night_shift"));
        check("clock fine again: hot streak", clock.flags.containsKey("house_hot_streak"));

        LedgerData bad = new LedgerData();
        LedgerData.Entry over = new LedgerData.Entry();
        over.grade = 99;
        over.at = -5;
        bad.stamps.put(worm.id(), over);
        LedgerData.Entry zero = new LedgerData.Entry();
        bad.stamps.put(Ledger.STAMPS.get(1).id(), zero);
        bad.stamps.put("no_such_stamp", new LedgerData.Entry());
        bad.stamps.get("no_such_stamp").grade = 2;
        bad.stamps.put("null_entry", null);
        bad.flags.put("house_night_shift", -3L);
        bad.recent.add(null);
        for (int i = 0; i < Ledger.RECENT + 9; i++) bad.recent.add(new LedgerData.Drop());
        bad.cutscenes = -4;
        bad.bustRun = 7;
        bad.worstBustRun = 2;
        bad.coins = -100;
        bad.fix();
        check("clean load: grade capped at the stamp's", bad.stamps.get(worm.id()).grade == worm.grades());
        check("clean load: no negative date", bad.stamps.get(worm.id()).at == 0);
        check("clean load: empty entries dropped", !bad.stamps.containsKey(Ledger.STAMPS.get(1).id())
                && !bad.stamps.containsKey("null_entry"));
        check("clean load: unknown ids kept", bad.stamps.containsKey("no_such_stamp"));
        check("clean load: no negative flag", bad.flags.get("house_night_shift") == 0L);
        check("clean load: recent capped, no nulls", bad.recent.size() == Ledger.RECENT && !bad.recent.contains(null));
        check("clean load: counters", bad.cutscenes == 0 && bad.coins == 0 && bad.worstBustRun == 7);
        check("clean load: the book still counts", Ledger.earnedGrades(bad) >= worm.grades());

        System.out.println(pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }

    static boolean capped() {
        LedgerData d = new LedgerData();
        for (int i = 0; i < Ledger.RECENT + 25; i++) Ledger.drop(d, "lava", "Flash I", i + 1, at(12, 0) + i * 3_600_000L, UTC);
        return d.recent.size() == Ledger.RECENT && d.recent.get(d.recent.size() - 1).kill == Ledger.RECENT + 25;
    }

    /**
     * Every stamp has its own icon in StampArt.ITEMS (read as source text), and each one resolves: a head's name is in
     * its bundled skins, a pack icon names a vanilla fallback, and every vanilla item is one GuiDraw knows - an
     * unknown one draws as paper.
     */
    static void icons() throws Exception {
        String art = Files.readString(Path.of("src/main/java/com/sbcs/gui/menu/StampArt.java"));
        String gui = Files.readString(Path.of("src/main/java/com/sbcs/gui/GuiDraw.java"));
        Map<String, String> items = new HashMap<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Map\\.entry\\(\"([a-z0-9_]+)\", \"([^\"]+)\"\\)").matcher(art);
        while (m.find()) check("one icon for " + m.group(1), items.put(m.group(1), m.group(2)) == null);
        Map<String, String> sets = Map.of("slayer", "slayer_heads", "lava", "lava_heads", "vanguard", "vanguard_heads",
                "diana", "diana_heads", "creatures", "creature_heads", "fishing", "fishing_heads");
        int packs = 0;
        for (Stamp s : Ledger.STAMPS) {
            String it = items.get(s.id());
            check(s.id() + " has its own icon", it != null);
            if (it == null) continue;
            String vanilla = it;
            if (it.startsWith("head:")) {
                String[] p = it.split(":", 3);
                if (p[1].equals("scatha")) continue;
                String file = sets.get(p[1]);
                check(s.id() + " head set " + p[1], file != null);
                if (file == null) continue;
                String json = Files.readString(Path.of("src/main/resources/assets/sbcs/" + file + ".json"));
                check(s.id() + " head " + p[2] + " is bundled", json.contains("\"" + p[2] + "\""));
                continue;
            }
            if (it.startsWith("pack:")) {
                String[] p = it.split(":", 3);
                check(s.id() + " pack icon has a path and a fallback", p.length == 3 && !p[1].isEmpty() && !p[2].isEmpty());
                if (p.length < 3) continue;
                check(s.id() + " pack path is under textures/item, no extension", !p[1].endsWith(".png") && !p[1].startsWith("textures/"));
                vanilla = p[2];
                packs++;
            }
            check(s.id() + " vanilla " + vanilla + " is in GuiDraw", gui.contains("case \"" + vanilla + "\" ->"));
        }
        check("pack icons in use", packs >= 15, "" + packs);
    }

    /** Every number high and every drop had. */
    static final String[] HELLION = {"Duplex I", "Scorched Power Crystal", "Subzero Inverter", "Wilson's Engineering Plans",
            "Paragon Shard", "Archfiend Dice", "High Class Archfiend Dice", "Flame Dye"};
    static final String[] ERUPTION = {"Chain of the End Times", "Severed Pincer", "Flash I", "Brimstone Handle",
            "Radioactive Vial", "Scuttler Shell", "Burnt Texts", "Carmine Dye"};

    static final String[] SERAPH = {"Judgement Core", "Byzantium Dye", "Exceedingly Rare Ender Artifact Upgrade",
            "End Stone Idol", "Summoning Eye", "Etherwarp Merger", "Enchant Rune I", "Paragon Shard"};
    static final String[] REVENANT = {"Beheaded Horror", "Snake Rune I", "Festering Maggot", "Scythe Blade", "Shredded Sinew",
            "Severed Hand", "Warden Heart", "Matcha Dye"};
    static final String[] TARANTULA = {"Primordial Eye", "Brick Red Dye", "Fly Swatter", "Ensnared Snail", "Shriveled Wasp",
            "Digested Mosquito", "Tarantula Talisman"};

    static Facts rich() {
        Facts f = new Facts();
        f.scathaKills = f.petsRare = f.petsEpic = f.petsLegendary = f.scathaWorstDry = f.scathaMisses = 99_999;
        f.corpses = f.corpseLegendaries = f.corpseDyes = f.lockets = f.corpseWorstDry = 99_999;
        f.dianaKills = 99_999;
        f.dwarvenOs = f.corpseRolls = 99_999;
        for (String n : List.of("Caged Wisp", "Frozen Scute", "Skeleton Key")) f.corpseItems.put(n, 3);
        f.dianaKillsBy.put("INQUISITOR", 99_999);
        for (String n : List.of("Brain Food", "Chimera I", "Fateful Stinger", "Manti-core", "Crown of Greed",
                "Shimmering Wool", "Minos Relic", "Mythological Dye")) f.dianaDrops.put(n, 5);
        put(f, "blaze", HELLION);
        put(f, "lava", ERUPTION);
        put(f, "shark", "Megalodon", "Great White Shark Tooth");
        put(f, "ghost", "Sorrow", "Plasma");
        put(f, "seraph", SERAPH);
        put(f, "zombie", REVENANT);
        put(f, "spider", TARANTULA);
        f.drops.get("ghost").put("Sorrow", 99_999);
        return f;
    }

    static void put(Facts f, String source, String... titles) {
        Map<String, Integer> m = new HashMap<>();
        for (String t : titles) m.put(t, 5);
        f.drops.put(source, m);
        f.kills.put(source, 99_999L);
        f.worstDry.put(source, 99_999L);
    }
}
