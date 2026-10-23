import com.sbcs.detect.DropDetector.DropResult;

import java.util.Locale;

/**
 * Text handling must not depend on the player's language. In Turkish, "EPIC".toLowerCase() is "epıc" (dotless i),
 * which matched no rarity anywhere: an epic Scatha pet lost its colour, sound and stats line.
 *
 *   javac -d out -cp <fabric+mc classpath> -sourcepath "src/main/java:tools" tools/LocaleTest.java
 */
public final class LocaleTest {
    static int passed, failed;

    static void check(String what, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + what);
        }
    }

    public static void main(String[] a) {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        check("Turkish really lower-cases I to dotless i here", "EPIC".toLowerCase().equals("epıc"));
        for (String r : new String[]{"RARE", "EPIC", "LEGENDARY"}) {
            String got = DropResult.pet(r).rarity;
            check(r + " pet reads as " + r.toLowerCase(Locale.ROOT) + " (got " + got + ")", got.equals(r.toLowerCase(Locale.ROOT)));
        }
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
