package com.sbcs.detect.dungeon;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The sidebar scoreboard as plain lines, top first. Hypixel writes each line as a team prefix and suffix
 * round an invisible owner name, so a line is the owner formatted for its team.
 */
public final class Sidebar {
    private Sidebar() {}

    public static List<String> lines(Minecraft client) {
        List<String> out = new ArrayList<>();
        if (client.level == null) return out;
        Scoreboard sb = client.level.getScoreboard();
        Objective obj = sb.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (obj == null) return out;
        List<PlayerScoreEntry> entries = new ArrayList<>(sb.listPlayerScores(obj));
        entries.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed());
        out.add(plain(obj.getDisplayName().getString()));
        for (PlayerScoreEntry e : entries) {
            if (e.isHidden()) continue;
            PlayerTeam team = sb.getPlayersTeam(e.owner());
            String text = PlayerTeam.formatNameForTeam(team, e.ownerName()).getString();
            out.add(plain(text));
        }
        return out;
    }

    /** Colour codes and the invisible padding Hypixel uses stripped. */
    public static String plain(String s) {
        return s.replaceAll("§.", "").replaceAll("[\\p{Cf}\\p{Co}]", "").strip();
    }
}
