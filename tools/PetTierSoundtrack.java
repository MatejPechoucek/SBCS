import com.sbcs.anim.PetTierTimeline;
import com.sbcs.anim.PetTierTimeline.Tier;
import com.sbcs.audio.PetTierSound;
import com.sbcs.audio.SynthEngine;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * Renders the Scatha pet reveal's sounds for a tier to a WAV - the exact cue list the game plays (PetTierSound),
 * mixed offline as HellionSoundtrack does. Time 0 is the head coming free; the price's coin ticks are not in it.
 *
 *   java -cp "out;src/main/resources" PetTierSoundtrack <outdir> <seed> TIER...
 */
public final class PetTierSoundtrack {

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        dir.mkdirs();
        long seed = Long.parseLong(args[1]);
        int rate = (int) SynthEngine.SAMPLE_RATE;
        for (int a = 2; a < args.length; a++) {
            Tier tier = Tier.valueOf(args[a]);
            PetTierTimeline tl = new PetTierTimeline(tier, seed);
            float seconds = tl.total() / 1000f + 1.5f;
            float[] mix = HellionSoundtrack.render(PetTierSound.schedule(tl), seconds);
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
            File f = new File(dir, "scatha_" + tier.name().toLowerCase() + ".wav");
            AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(pcm), fmt, mix.length / 2), AudioFileFormat.Type.WAVE, f);
            System.out.printf("%s: %.1f s, peak %.3f%s%n", tier, seconds, peak, gain < 1f ? " (normalised)" : "");
        }
    }
}
