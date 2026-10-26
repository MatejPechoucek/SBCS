package com.sbcs;

/**
 * Animations pulled from players while they are being reworked. One switch each: while it is on, the animation's
 * detection does not run, nothing of it plays (its pre-kill hide and dropped-item hiding included), and the menu shows
 * its card taped over. The developer commands still play it for the accounts in {@link Dev}.
 *
 * No Minecraft in here, so the menu (and its offline preview) can read it too.
 */
public final class Maintenance {
    private Maintenance() {}

    /** The Seraph's Glyph (the Voidgloom Seraph): being reworked before players see it. */
    public static final boolean SERAPH = false;

    /** The Seraph's Ledger chapter, taped while it had no stamps. It has its own since 2026-10-26. */
    public static final boolean SERAPH_LEDGER = false;
}
