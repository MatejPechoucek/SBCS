/*
 * Offline checks for the Ossuary's detection (detect/dungeon/, the pure parts): floors, chest tiers, price keys,
 * a reward chest's menu, a Croesus run page, the chest's value and which chest a run rolls, the roll memory, and the
 * bundled reel pool. Gson is the only library:
 *
 *   GSON=$(find ~/.gradle/caches/modules-2 -name 'gson-*.jar' ! -name '*sources*' | head -1)
 *   javac -d out/dc -cp "$GSON" src/main/java/com/sbcs/detect/dungeon/{DungeonFloor,ChestTier,Loot,PriceKeys,SlotView,RewardChest,ChestReader,ChestValue,RollMemory,DungeonPool}.java tools/DungeonChestTest.java
 *   java -cp "out/dc:$GSON" DungeonChestTest        (";" between entries on Windows)
 *
 * Run from the repository root (it reads the bundled pool). The menus here are built from the strings Skyblocker,
 * SkyOcean and SkyHanni match; once Matej's records are in, real ones go in tools/fixtures/ beside them.
 */
import com.sbcs.detect.dungeon.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DungeonChestTest {
    static int pass = 0, fail = 0;

    static void check(String what, boolean ok, String detail) {
        System.out.printf("  %s %-60s %s%n", ok ? "PASS" : "FAIL", what, detail);
        if (ok) pass++; else fail++;
    }

    static SlotView slot(int i, String name, String item, String id, Map<String, Integer> ench, String... lore) {
        return new SlotView(i, name, item, id, ench, null, null, List.of(lore), 1, null);
    }

    static SlotView pet(int i, String name, String type, String tier) {
        return new SlotView(i, name, "minecraft:player_head", "PET", null, type, tier, List.of(), 1, null);
    }

    static SlotView pane(int i) {
        return slot(i, " ", "minecraft:black_stained_glass_pane", null, null);
    }

    public static void main(String[] a) throws Exception {
        DungeonPool pool = DungeonPool.parse(Files.readString(Path.of("src/main/resources/assets/sbcs/dungeon_pool.json")));
        Map<String, String> names = pool.names();

        System.out.println("Floors");
        check("sidebar F7", DungeonFloor.fromSidebar(List.of("Skyblock", " The Catacombs (F7)")).equals(new DungeonFloor(7, false)), "");
        check("sidebar M5", DungeonFloor.fromSidebar(List.of("The Catacombs (M5)")).equals(new DungeonFloor(5, true)), "");
        check("sidebar Entrance", DungeonFloor.fromSidebar(List.of("The Catacombs (E)")).number() == 0, "");
        check("outside a run", DungeonFloor.fromSidebar(List.of("Dungeon Hub", "Purse: 4")) == null, "");
        check("title Master Catacombs - Floor VII", DungeonFloor.fromTitle("Master Catacombs - Floor VII").equals(new DungeonFloor(7, true)), "");
        check("title Catacombs - Floor IV", DungeonFloor.fromTitle("Catacombs - Floor IV").equals(new DungeonFloor(4, false)), "");
        check("labels", new DungeonFloor(7, true).label().equals("M7") && new DungeonFloor(0, false).label().equals("E"), "");

        System.out.println("Chest tiers");
        check("\"Obsidian Chest\" and \"Obsidian\"", ChestTier.fromTitle("Obsidian Chest") == ChestTier.OBSIDIAN
                && ChestTier.fromTitle("Obsidian") == ChestTier.OBSIDIAN, "");
        check("Kuudra's \"Paid Chest\" is not one", ChestTier.fromTitle("Paid Chest") == null, "");
        check("default tiers are Obsidian and Bedrock", ChestTier.DEFAULT_BITS == (1 << 4 | 1 << 5), "");

        System.out.println("Price keys");
        check("book from data", "ENCHANTMENT_ULTIMATE_WISE_2".equals(PriceKeys.fromData("ENCHANTED_BOOK", Map.of("ultimate_wise", 2), null, null)), "");
        check("pet from data", "PET_SPIRIT?Rarity=LEGENDARY".equals(PriceKeys.fromData("PET", null, "SPIRIT", "LEGENDARY")), "");
        check("plain id", "NECRON_HANDLE".equals(PriceKeys.fromData("NECRON_HANDLE", null, null, null)), "");
        check("book by name: Ultimate Wise II", "ENCHANTMENT_ULTIMATE_WISE_2".equals(PriceKeys.book("Ultimate Wise II")), "");
        check("book by name: One For All I", "ENCHANTMENT_ULTIMATE_ONE_FOR_ALL_1".equals(PriceKeys.book("One For All I")), "");
        check("book by name: No Pain No Gain II", "ENCHANTMENT_ULTIMATE_NO_PAIN_NO_GAIN_2".equals(PriceKeys.book("No Pain No Gain II")), "");
        check("book by name: Infinite Quiver VI", "ENCHANTMENT_INFINITE_QUIVER_6".equals(PriceKeys.book("Infinite Quiver VI")), "");
        check("book names round trip", "Soul Eater I".equals(PriceKeys.bookName("ENCHANTMENT_ULTIMATE_SOUL_EATER_1"))
                && "Ultimate Jerry III".equals(PriceKeys.bookName("ENCHANTMENT_ULTIMATE_JERRY_3")), PriceKeys.bookName("ENCHANTMENT_ULTIMATE_SOUL_EATER_1"));
        check("essence", "ESSENCE_WITHER".equals(PriceKeys.fromName("Wither Essence", names)), "");
        Object[] nc = PriceKeys.splitCount("Undead Essence x24");
        check("a count off the name", nc[0].equals("Undead Essence") && (Integer) nc[1] == 24, Arrays.toString(nc));
        check("Croesus book line", "ENCHANTMENT_ULTIMATE_BANK_1".equals(PriceKeys.fromName("Enchanted Book (Bank I)", names)), "");
        check("a pool item by name", "RECOMBOBULATOR_3000".equals(PriceKeys.fromName("Recombobulator 3000", names)), "");
        check("stars stripped", "NECRON_HANDLE".equals(PriceKeys.fromName("Necron's Handle ✪✪", names)), "");
        check("Shiny is the same item", "NECRON_HANDLE".equals(PriceKeys.fromName("Shiny Necron's Handle", names)), "");
        check("unknown is null", PriceKeys.fromName("Mystery Thing", names) == null, "");

        System.out.println("A reward chest's menu (boss room)");
        List<SlotView> run = new ArrayList<>();
        for (int i = 0; i < 9; i++) run.add(pane(i));
        run.add(slot(11, "Necron's Handle", "minecraft:stick", "NECRON_HANDLE", null));
        run.add(slot(12, "Enchanted Book", "minecraft:enchanted_book", "ENCHANTED_BOOK", Map.of("ultimate_one_for_all", 1)));
        run.add(slot(13, "Wither Essence x20", "minecraft:player_head", null, null));
        run.add(slot(31, "Open Reward Chest", "minecraft:chest", null, null, "Cost", "4,000,000 Coins", "", "Click to open!"));
        run.add(slot(49, "Close", "minecraft:barrier", null, null));
        run.add(slot(50, "Reroll Chest", "minecraft:feather", null, null, "Uses a Kismet Feather"));
        DungeonFloor f7 = new DungeonFloor(7, false);
        RewardChest ch = ChestReader.readChest("Bedrock Chest", run, f7, names);
        check("read", ch != null && ch.tier() == ChestTier.BEDROCK, String.valueOf(ch));
        check("three items, the buttons left out", ch != null && ch.items().size() == 3, "");
        check("cost 4,000,000", ch != null && ch.cost() == 4_000_000, "");
        check("the floor from the sidebar", ch != null && ch.floor().equals(f7), "");
        check("the book named by its enchantment", ch != null && ch.items().get(1).name().equals("One For All I"), ch == null ? "" : ch.items().get(1).name());
        check("essence counted", ch != null && ch.items().get(2).count() == 20 && "ESSENCE_WITHER".equals(ch.items().get(2).key()), "");
        List<SlotView> loading = new ArrayList<>(run);
        loading.removeIf(s -> s.name().contains("Open Reward"));
        check("not ready until Open Reward Chest is there", ChestReader.readChest("Bedrock Chest", loading, f7, names) == null, "");
        check("another menu is not a chest", ChestReader.readChest("Bazaar", run, f7, names) == null, "");

        System.out.println("A chest's page at Croesus");
        List<SlotView> page = new ArrayList<>(run);
        page.add(slot(48, "Go Back", "minecraft:arrow", null, null, "To Master Catacombs - Floor VII"));
        RewardChest cp = ChestReader.readChest("Bedrock", page, null, names);
        check("the floor from the back arrow", cp != null && cp.floor().equals(new DungeonFloor(7, true)), String.valueOf(cp == null ? null : cp.floor()));

        System.out.println("A run's page at Croesus");
        List<SlotView> runPage = new ArrayList<>();
        runPage.add(slot(10, "Wood", "minecraft:player_head", null, null, "Contents", "Wither Essence x5", "", "Cost", "FREE"));
        runPage.add(slot(13, "Obsidian", "minecraft:player_head", null, null, "Contents", "Enchanted Book (Bank I)",
                "Recombobulator 3000", "Wither Essence x12", "", "Cost", "2,000,000 Coins"));
        runPage.add(slot(14, "Bedrock", "minecraft:player_head", null, null, "Contents", "Necron's Handle", "Wither Essence x30",
                "", "Cost", "4,000,000 Coins", "Dungeon Chest Key"));
        runPage.add(slot(49, "Go Back", "minecraft:arrow", null, null, "To Croesus"));
        List<RewardChest> chests = ChestReader.readRun("Catacombs - Floor VII", runPage, names);
        check("three chests read", chests.size() == 3, chests.size() + "");
        check("Bedrock needs a key", chests.size() == 3 && chests.get(2).key() && chests.get(2).cost() == 4_000_000, "");
        check("Obsidian's contents keyed", chests.size() == 3 && "ENCHANTMENT_ULTIMATE_BANK_1".equals(chests.get(1).items().get(0).key())
                && "RECOMBOBULATOR_3000".equals(chests.get(1).items().get(1).key()), "");
        check("a run page's floor", ChestReader.runFloor("Master Catacombs - Floor III").equals(new DungeonFloor(3, true)), "");
        check("Croesus's own menu is not a run page", ChestReader.runFloor("Croesus") == null, "");

        System.out.println("Fingerprints: the boss room and Croesus agree");
        RewardChest roomBedrock = ChestReader.readChest("Bedrock Chest",
                List.of(slot(11, "Necron's Handle", "minecraft:stick", "NECRON_HANDLE", null),
                        slot(13, "Wither Essence x30", "minecraft:player_head", null, null),
                        slot(31, "Open Reward Chest", "minecraft:chest", null, null, "4,000,000 Coins")), f7, names);
        check("same fingerprint", roomBedrock != null && roomBedrock.fingerprint().equals(chests.get(2).fingerprint()),
                roomBedrock == null ? "" : roomBedrock.fingerprint() + " / " + chests.get(2).fingerprint());

        System.out.println("Value and the best chest");
        Map<String, Long> prices = Map.of("NECRON_HANDLE", 600_000_000L, "ESSENCE_WITHER", 3_000L, "RECOMBOBULATOR_3000", 6_000_000L,
                "ENCHANTMENT_ULTIMATE_BANK_1", 900_000L, "DUNGEON_CHEST_KEY", 1_200_000L);
        ChestValue bed = ChestValue.of(chests.get(2), prices::get);
        check("best is the handle", bed.bestItem().key().equals("NECRON_HANDLE"), "");
        check("profit takes the cost and the key off", bed.profit() == 600_000_000L + 90_000L - 4_000_000L - 1_200_000L, bed.profit() + "");
        ChestValue obs = ChestValue.of(chests.get(1), prices::get);
        check("best of the run is Bedrock", ChestValue.bestOf(List.of(obs, bed)) == bed, "");
        Map<String, Long> cheap = Map.of("NECRON_HANDLE", 1L, "ESSENCE_WITHER", 1L, "RECOMBOBULATOR_3000", 1L,
                "ENCHANTMENT_ULTIMATE_BANK_1", 1L, "DUNGEON_CHEST_KEY", 1L);
        ChestValue o2 = ChestValue.of(chests.get(1), cheap::get), b2 = ChestValue.of(chests.get(2), cheap::get);
        check("all losing: the least negative", ChestValue.bestOf(List.of(o2, b2)) == o2, o2.profit() + " vs " + b2.profit());
        ChestValue essOnly = ChestValue.of(chests.get(0), prices::get);
        check("essence is the best only when alone", essOnly.bestItem().key().equals("ESSENCE_WITHER"), "");
        check("unknown prices flagged", ChestValue.of(chests.get(1), k -> null).incomplete(), "");

        System.out.println("Roll memory");
        RollMemory mem = new RollMemory();
        mem.remember("x", 1000);
        check("remembered", mem.rolled("x", 2000), "");
        check("forgotten after 72 hours", !mem.rolled("x", 1000 + RollMemory.KEEP_MS), "");
        mem.load(Map.of("old", 0L, "new", RollMemory.KEEP_MS), RollMemory.KEEP_MS + 5);
        check("load drops the expired", !mem.entries().containsKey("old") && mem.entries().containsKey("new"), "");

        System.out.println("Which menu rolls, and which chest");
        int bits = ChestTier.DEFAULT_BITS;
        RollMemory m2 = new RollMemory();
        check("candidate: Bedrock Chest", RollPicker.candidate("Bedrock Chest", bits, 1), "");
        check("candidate: not Wood by default", !RollPicker.candidate("Wood Chest", bits, 1), "");
        check("candidate: a run page", RollPicker.candidate("Catacombs - Floor VII", bits, 1), "");
        check("candidate: no run page with Croesus off", !RollPicker.candidate("Catacombs - Floor VII", bits, 0), "");
        RollPicker.Decision d = RollPicker.decide("Bedrock Chest", run, f7, names, bits, 1, m2, 1000, prices::get);
        check("the boss room's chest rolls", d.kind() == RollPicker.Kind.ROLL && d.chest().tier() == ChestTier.BEDROCK, d.why());
        check("loading: wait", RollPicker.decide("Bedrock Chest", loading, f7, names, bits, 1, m2, 1000, prices::get).kind() == RollPicker.Kind.WAIT, "");
        check("a switched-off tier does not", RollPicker.decide("Bedrock Chest", run, f7, names, ChestTier.OBSIDIAN.bit(), 1, m2, 1000, prices::get).kind()
                == RollPicker.Kind.NONE, "");
        for (String fp : d.remember()) m2.remember(fp, 1000);
        check("rolled once, never again", RollPicker.decide("Bedrock Chest", run, f7, names, bits, 1, m2, 2000, prices::get).kind()
                == RollPicker.Kind.NONE, "");
        check("a chest's own page at Croesus: only with 'every chest'",
                RollPicker.decide("Bedrock", page, null, names, bits, 1, new RollMemory(), 1000, prices::get).kind() == RollPicker.Kind.NONE
                        && RollPicker.decide("Bedrock", page, null, names, bits, 2, new RollMemory(), 1000, prices::get).kind() == RollPicker.Kind.ROLL, "");
        RollMemory m3 = new RollMemory();
        RollPicker.Decision rp = RollPicker.decide("Catacombs - Floor VII", runPage, null, names, bits, 1, m3, 1000, prices::get);
        check("the run page rolls its best: Bedrock", rp.kind() == RollPicker.Kind.ROLL && rp.chest().tier() == ChestTier.BEDROCK, rp.why());
        for (String fp : rp.remember()) m3.remember(fp, 1000);
        check("the page rolls once", RollPicker.decide("Catacombs - Floor VII", runPage, null, names, bits, 1, m3, 2000, prices::get).kind()
                == RollPicker.Kind.NONE, "");
        RollMemory m4 = new RollMemory();
        m4.remember(chests.get(2).fingerprint(), 1000);
        RollPicker.Decision after = RollPicker.decide("Catacombs - Floor VII", runPage, null, names, bits, 1, m4, 2000, prices::get);
        check("Bedrock rolled in the boss room: the page rolls Obsidian", after.kind() == RollPicker.Kind.ROLL
                && after.chest().tier() == ChestTier.OBSIDIAN, after.why());
        check("Croesus off: the page does nothing", RollPicker.decide("Catacombs - Floor VII", runPage, null, names, bits, 0, new RollMemory(), 1000, prices::get).kind()
                == RollPicker.Kind.NONE, "");
        check("Wood switched on: still picks by profit", RollPicker.decide("Catacombs - Floor VII", runPage, null, names, 0x3F, 1, new RollMemory(), 1000, prices::get)
                .chest().tier() == ChestTier.BEDROCK, "");

        System.out.println("The reel pool");
        check("every floor has an Obsidian pool", List.of("F1", "F2", "F3", "F4", "F5", "F6", "F7", "M1", "M7").stream()
                .allMatch(k -> !pool.pool(DungeonFloor.fromLabel(k), ChestTier.OBSIDIAN).isEmpty()), "");
        check("F7 Bedrock holds the handle", pool.pool(f7, ChestTier.BEDROCK).stream().anyMatch(e -> e.look().key().equals("NECRON_HANDLE")), "");
        check("a Wood chest borrows a pool", !pool.pool(new DungeonFloor(2, false), ChestTier.WOOD).isEmpty(), "");
        check("the Entrance borrows Floor I's", !pool.pool(new DungeonFloor(0, false), ChestTier.OBSIDIAN).isEmpty(), "");
        check("Thorn's boots are THORNS_BOOTS", pool.look("THORNS_BOOTS") != null, "");

        System.out.printf("%n%d passed, %d failed%n", pass, fail);
        if (fail > 0) System.exit(1);
    }
}
