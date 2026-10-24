package com.sbcs.anim;

/**
 * A cutscene's own clock: milliseconds of the scene since it opened, run at the player's speed setting for that
 * animation (the menu's "Speed", 50-200%).
 *
 * Everything a cutscene times - its picture, its cue list, its skip target - reads this, so the whole animation
 * plays faster or slower together. A skip jumps the scene time; the real time it opened is kept apart for what
 * needs the wall clock (the kill's time, seeds).
 */
public final class SceneClock {
    private final float speed;
    private final long openedAt = System.currentTimeMillis();
    /** The scene time at {@link #origin}, and the wall time it was set. A jump moves both. */
    private long base, origin = openedAt;

    /** @param percent the speed, 100 = as authored */
    public SceneClock(int percent) {
        this.speed = Math.max(0.25f, Math.min(4f, percent / 100f));
    }

    /** The scene holds here until {@link #release} (Hellion's hits, landed by a click); MAX_VALUE for none. */
    private long gate = Long.MAX_VALUE;

    /** Scene milliseconds since the cutscene opened. */
    public long t() {
        return Math.min(gate, raw());
    }

    private long raw() {
        return base + (long) ((System.currentTimeMillis() - origin) * speed);
    }

    /** Moves the scene to {@code sceneMs} (a skip). Lifts any hold. */
    public void jump(long sceneMs) {
        origin = System.currentTimeMillis();
        base = sceneMs;
        gate = Long.MAX_VALUE;
    }

    /** Holds the scene at {@code sceneMs} once it gets there, until {@link #release}. */
    public void holdAt(long sceneMs) {
        gate = sceneMs;
    }

    /** Whether a hold is set (reached or not). */
    public boolean holding() {
        return gate != Long.MAX_VALUE;
    }

    /** Whether the scene is standing at its hold now. */
    public boolean held() {
        return gate != Long.MAX_VALUE && raw() >= gate;
    }

    /** Wall milliseconds the scene has stood at its hold, 0 if it is not. */
    public long heldFor() {
        if (!held()) return 0;
        return (long) ((raw() - gate) / speed);
    }

    /** Lets the scene go on from its hold, from where it stood. */
    public void release() {
        if (gate == Long.MAX_VALUE) return;
        long at = Math.min(gate, raw());
        jump(at);
    }

    /** The wall-clock time the cutscene opened. */
    public long openedAt() {
        return openedAt;
    }

    public float speed() {
        return speed;
    }
}
