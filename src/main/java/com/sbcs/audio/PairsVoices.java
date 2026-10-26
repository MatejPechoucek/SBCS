package com.sbcs.audio;

import static com.sbcs.audio.Voices.after;
import static com.sbcs.audio.Voices.noise;
import static com.sbcs.audio.Voices.tone;

/**
 * The cues only Superpairs uses: the hunt in the menu (a box landing on a tile, the match) and the reveal's own (the
 * cracks ticking up, the pages riffling, the petals). Pure, like Voices: every voice is seeded, so it renders the same
 * offline. Built from Voices' two ingredients, a tone and filtered noise.
 */
public final class PairsVoices {
    private PairsVoices() {}

    /** E minor pentatonic, the enchanting table's air. */
    private static final float[] NOTES = {329.63f, 392f, 440f, 493.88f, 587.33f, 659.25f, 783.99f, 880f};

    /** A box landing on a tile in the menu: a soft felt thud and a low bell under it. Quiet - the player is playing. */
    public static Voice[] boxLand(long seed) {
        return new Voice[]{
                tone(74f, 52f, 0.22f, 0.32f, 0.002f, new float[]{1f, 0.4f}, 0.4f),
                noise(320f, 1.0f, 0.10f, 0.14f, 0.001f, 0.3f, seed),
                after(0.04f, tone(164.81f, 164.81f, 1.4f, 0.07f, 0.004f, new float[]{1f, 0.5f, 0.3f, 0.1f}, 0.85f)),
                after(0.04f, tone(164.81f * 2.76f, 164.81f * 2.76f, 0.6f, 0.015f, 0.004f, new float[]{1f}, 0.85f)),
        };
    }

    /** A pair found: a crack of light between the boxes and one chord, the heartbeat's end. */
    public static Voice[] match(long seed) {
        java.util.List<Voice> out = new java.util.ArrayList<>();
        out.add(noise(5200f, 1.6f, 0.18f, 0.08f, 0.002f, 0.6f, seed));
        out.add(tone(1318.5f, 659.25f, 0.18f, 0.05f, 0.002f, new float[]{1f, 0.3f}, 0.7f));
        float[] chord = {329.63f, 493.88f, 659.25f, 987.77f};
        for (int i = 0; i < chord.length; i++) {
            out.add(after(0.06f + i * 0.03f, tone(chord[i], chord[i], 2.2f, 0.07f, 0.006f, new float[]{1f, 0.35f, 0.12f}, 0.95f)));
        }
        return out.toArray(new Voice[0]);
    }

    /** A crack of light spreading over the box, a step higher each time round the colours. */
    public static Voice[] crack(int step, long seed) {
        float hz = NOTES[step % NOTES.length] * 2f;
        return new Voice[]{
                noise(4600f, 2.8f, 0.05f, 0.12f, 0.0005f, 0.4f, seed),
                tone(hz, hz, 0.22f, 0.06f, 0.002f, new float[]{1f, 0.25f}, 0.7f),
                tone(hz * 1.5f, hz * 1.5f, 0.14f, 0.025f, 0.002f, new float[]{1f}, 0.7f),
        };
    }

    /** The box flying in from the corner: air rushing past and a thump as it stops in the middle. */
    public static Voice[] boxIn(long seed) {
        return new Voice[]{
                noise(1200f, 0.9f, 0.55f, 0.08f, 0.4f, 0.4f, seed),
                after(0.5f, tone(82f, 55f, 0.4f, 0.3f, 0.002f, new float[]{1f, 0.5f, 0.2f}, 0.5f)),
                after(0.5f, noise(380f, 1.1f, 0.12f, 0.16f, 0.001f, 0.4f, seed + 1)),
        };
    }

    /** The lid of a box that got away, lifting: a dry creak and nothing behind it. */
    public static Voice[] missed(long seed) {
        return new Voice[]{
                tone(110f, 92f, 1.0f, 0.14f, 0.004f, new float[]{1f, 0.4f}, 0.8f),
                after(0.2f, noise(2600f, 0.7f, 0.9f, 0.04f, 0.2f, 0.6f, seed)),
        };
    }

    /** Pages riffling, faster and faster: paper flicks for {@code seconds}, cut by the flash. */
    public static Voice[] riffle(float seconds, long seed) {
        java.util.List<Voice> out = new java.util.ArrayList<>();
        java.util.Random r = new java.util.Random(seed);
        float t = 0f;
        int i = 0;
        while (t < seconds) {
            out.add(after(t, noise(3000f + r.nextFloat() * 1800f, 1.8f, 0.05f, 0.06f + 0.04f * t / seconds, 0.003f, 0.3f, seed + i)));
            float gap = 0.14f - 0.1f * (t / seconds);
            t += gap + r.nextFloat() * 0.01f;
            i++;
        }
        return out.toArray(new Voice[0]);
    }

    /** The petals bursting out and storming round: a long airy sweep and a scatter of high bells. */
    public static Voice[] petals(float seconds, long seed) {
        java.util.List<Voice> out = new java.util.ArrayList<>();
        out.add(noise(1800f, 0.7f, seconds, 0.12f, 0.05f, 0.9f, seed));
        out.add(noise(5200f, 0.9f, seconds, 0.05f, seconds * 0.5f, 0.9f, seed + 1));
        java.util.Random r = new java.util.Random(seed);
        for (int i = 0; i < 18; i++) {
            float hz = NOTES[r.nextInt(NOTES.length)] * 2f;
            out.add(after(r.nextFloat() * seconds, tone(hz, hz, 0.7f, 0.035f, 0.002f, new float[]{1f, 0.2f}, 0.95f)));
        }
        return out.toArray(new Voice[0]);
    }
}
