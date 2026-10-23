import com.sbcs.Safety;
import com.sbcs.audio.SynthEngine;

/**
 * Offline checks for the fail-safes that can be checked without a game: which server counts as Hypixel, and the
 * synth's guard against a voice that puts NaN or infinity into a block (it would sit in the reverb for good).
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
        float[] ok = new float[512];
        for (int i = 0; i < ok.length; i++) ok[i] = (float) Math.sin(i * 0.1) * 3f;
        check("a normal block is finite", SynthEngine.finite(ok));
        check("silence is finite", SynthEngine.finite(new float[512]));
        float[] nan = ok.clone();
        nan[300] = Float.NaN;
        check("a NaN is caught", !SynthEngine.finite(nan));
        float[] inf = ok.clone();
        inf[7] = Float.NEGATIVE_INFINITY;
        check("an infinity is caught", !SynthEngine.finite(inf));
        float[] big = ok.clone();
        big[0] = Float.MAX_VALUE;
        big[1] = Float.MAX_VALUE;
        check("huge but finite samples are not mistaken for infinity", SynthEngine.finite(big));
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
