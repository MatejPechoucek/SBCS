package com.sbcs.detect.dungeon;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Catacombs floor: 0 is the Entrance, 1-7 the floors, {@code master} for Master Mode. Pure.
 *
 * In a run the floor is on the sidebar ("The Catacombs (F7)", "(M7)", "(E)"); at Croesus it is in the run page's
 * title ("Master Catacombs - Floor VII") and in a chest page's back arrow ("To Catacombs - Floor VII", SkyOcean's
 * CroesusImpl reads that line).
 */
public record DungeonFloor(int number, boolean master) {

    public static final DungeonFloor UNKNOWN = new DungeonFloor(7, false);

    private static final Pattern SIDEBAR = Pattern.compile("The Catacombs \\((E|[FM][1-7])\\)");
    private static final Pattern TITLE = Pattern.compile("(Master )?Catacombs - (?:Floor ([IV]+)|(Entrance))");
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII"};

    /** "F7", "M7", "E". */
    public String label() {
        return number == 0 ? "E" : (master ? "M" : "F") + number;
    }

    /** "Floor VII", "Master Mode Floor VII", "the Entrance". */
    public String title() {
        if (number == 0) return "The Entrance";
        return (master ? "Master Mode Floor " : "Floor ") + ROMAN[number];
    }

    /** The floor named on the sidebar's lines, or null outside a run. */
    public static DungeonFloor fromSidebar(List<String> lines) {
        for (String l : lines) {
            Matcher m = SIDEBAR.matcher(l);
            if (m.find()) return fromLabel(m.group(1));
        }
        return null;
    }

    /** "F7" / "M3" / "E", or null. */
    public static DungeonFloor fromLabel(String s) {
        if (s == null) return null;
        s = s.trim().toUpperCase(java.util.Locale.ROOT);
        if (s.equals("E")) return new DungeonFloor(0, false);
        if (s.length() == 2 && (s.charAt(0) == 'F' || s.charAt(0) == 'M') && s.charAt(1) >= '1' && s.charAt(1) <= '7') {
            return new DungeonFloor(s.charAt(1) - '0', s.charAt(0) == 'M');
        }
        return null;
    }

    /** "Master Catacombs - Floor VII", "Catacombs - Entrance", or anything containing them; null otherwise. */
    public static DungeonFloor fromTitle(String s) {
        if (s == null) return null;
        Matcher m = TITLE.matcher(s);
        if (!m.find()) return null;
        boolean master = m.group(1) != null;
        if (m.group(3) != null) return new DungeonFloor(0, false);
        for (int i = 1; i < ROMAN.length; i++) if (ROMAN[i].equals(m.group(2))) return new DungeonFloor(i, master);
        return null;
    }

    /** The key SkyOcean's pools use: "F7", "M7"; the Entrance borrows Floor I's. */
    public String poolKey() {
        return number == 0 ? "F1" : label();
    }
}
