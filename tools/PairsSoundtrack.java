import com.sbcs.anim.PairsTimeline;
import com.sbcs.audio.PairsSound;
import com.sbcs.audio.SynthEngine;
import com.sbcs.audio.Voice;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Renders the Hunt's soundtrack for a test word to a WAV - the exact cue list the game plays (PairsSound.schedule),
 * mixed offline; a cut silences everything sounding, as SbcsAudio.silence() does. The price's coin ticks are not in it.
 *
 *   java -cp out:src/main/resources:tools PairsSoundtrack <outdir> <seed> WORD...
 */
public final class PairsSoundtrack {

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        dir.mkdirs();
        long seed = Long.parseLong(args[1]);
        int rate = (int) SynthEngine.SAMPLE_RATE;
        for (int a = 2; a < args.length; a++) {
            PairsTimeline tl = PairsTimeline.test(args[a], seed);
            float seconds = tl.total() / 1000f + 1.5f;
            float[] mix = render(PairsSound.schedule(tl), seconds);
            float peak = 0f;
            for (float v : mix) peak = Math.max(peak, Math.abs(v));
            float gain = peak > 0.98f ? 0.98f / peak : 1f;
            byte[] pcm = new byte[mix.length * 2];
            for (int i = 0; i < mix.length; i++) {
                int s = Math.round(Math.max(-1f, Math.min(1f, mix[i] * gain)) * 32767f);
                pcm[i * 2] = (byte) s;
                pcm[i * 2 + 1] = (byte) (s >> 8);
            }
            AudioFormat fmt = new AudioFormat(rate, 16, 2, true, false);
            File f = new File(dir, "pairs_" + args[a] + ".wav");
            AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(pcm), fmt, mix.length / 2), AudioFileFormat.Type.WAVE, f);
            System.out.printf("%s: %.1f s, peak %.3f%s%n", args[a], seconds, peak, gain < 1f ? " (normalised)" : "");
        }
    }

    static float[] render(List<PairsSound.Cue> cues, float seconds) {
        int rate = (int) SynthEngine.SAMPLE_RATE;
        float[] mix = new float[(int) (seconds * rate) * 2];
        for (PairsSound.Cue c : cues) {
            int at = (int) (c.at * rate / 1000L) * 2;
            if (at >= mix.length) continue;
            if (c.cut) Arrays.fill(mix, at, mix.length, 0f);
            for (Voice v : c.voices) {
                float[] one = SynthEngine.renderOffline(v, seconds - c.at / 1000f);
                for (int i = 0; i < one.length && at + i < mix.length; i++) mix[at + i] += one[i];
            }
        }
        return mix;
    }
}
