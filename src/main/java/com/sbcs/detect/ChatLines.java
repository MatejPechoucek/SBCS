package com.sbcs.detect;

import java.util.regex.Pattern;

/**
 * Tells a line another player typed from one the game printed.
 *
 * Fabric's "player chat" and "game message" are no help on Hypixel: it sends everything, players'
 * chat included, as game messages. So "[MVP+] Someone: You dug out a Minos Inquisitor!" reaches the
 * watchers exactly as Hypixel's own dig line does. Most drop lines are safe anyway - they are matched
 * from the start ("RARE DROP! ..."), and a player's line always starts with its sender - but a few
 * patterns look anywhere in the line (Diana's dig, lava catches), and any player nearby could make a
 * creature "ours" with one message. Every watcher drops such lines first.
 *
 * Pure, so tools/ChatSpoofTest checks it against the real lines the detectors were written from.
 */
public final class ChatLines {
    private ChatLines() {}

    /**
     * The sender part of a player's line: an optional channel ("Party > ", "Guild > ", "Co-op > ",
     * "From ", "To "), any bracketed tags or one-glyph emblems ("[302]", "[MVP+]", "♲"), the name, any
     * tags after it ("[GUILD]"), then ": ". Hypixel's own lines never have a lone word followed by ": "
     * at their start - Ragnarok's "The sky darkens ... upon us: Ragnarok is here." has its colon deep in.
     */
    private static final Pattern PLAYER = Pattern.compile(
            "^(?:(?:Party|Guild|Officer|Co-op|Coop) > |From |To )?"
                    + "(?:\\[[^\\]]{1,24}\\] ?|[^\\x00-\\x7F]{1,2} )*"
                    + "[A-Za-z0-9_]{1,16}"
                    + "(?: \\[[^\\]]{1,24}\\]| [^\\x00-\\x7F]{1,2})*"
                    + ": ");

    /** Whether {@code plain} (formatting already stripped) is a line some player typed. */
    public static boolean fromPlayer(String plain) {
        if (plain == null) return false;
        String s = plain.strip();
        // NPCs and bosses talk in the same shape ("[NPC] Kat: ...", "[BOSS] Necron: ..."); they are not drops
        // either, so treating them as typed changes nothing.
        return PLAYER.matcher(s).find();
    }
}
