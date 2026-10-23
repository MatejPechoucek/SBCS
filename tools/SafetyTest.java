import com.sbcs.Safety;

/**
 * Offline checks for the fail-safes that can be checked without a game: which server counts as Hypixel.
 *
 *   javac -d out -cp <fabric+mc classpath> -sourcepath "src/main/java:tools" tools/SafetyTest.java
 */
public final class SafetyTest {
    static int passed, failed;

    static void check(String what, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + what);
        }
    }

    public static void main(String[] a) {
        for (String yes : new String[]{"hypixel.net", "mc.hypixel.net", "MC.Hypixel.NET", "hypixel.net:25565",
                "mc.hypixel.net:25565", " mc.hypixel.net ", "alpha.hypixel.net", "hypixel.net."}) {
            check("Hypixel: \"" + yes + "\"", Safety.isHypixelAddress(yes));
        }
        for (String no : new String[]{null, "", "localhost", "nothypixel.net", "hypixel.net.example.com",
                "hypixel.network", "fakehypixel.net:25565", "hypixel.net.evil.org:25565", "192.168.0.2"}) {
            check("not Hypixel: \"" + no + "\"", !Safety.isHypixelAddress(no));
        }
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
