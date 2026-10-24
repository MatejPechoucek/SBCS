#!/usr/bin/env python3
"""Writes src/main/resources/assets/sbcs/dungeon_pool.json - every reward chest's pool for the Ossuary's reel.

    python3 tools/gen_dungeon_pool.py <SkyOcean's src/repo/dungeon_chests.json> [neu cache dir]

Where each chest's pool comes from, best first:
  1. The wiki (hypixelskyblock.minecraft.wiki, The_Catacombs_-_Floor_N/Loot and /Master_Mode_Loot), copied as data
     into tools/fixtures/dungeon_loot/<floor>.csv: item, quality, added cost, weight, first-roll chance. Read for F2,
     F7 and M2-M7; the other floors' pages would not load. The fetch stops ~110k characters in, so a long Bedrock
     table (F7, M5, M6, M7) loses its last rows.
  2. SkyOcean's pools (meowdding/SkyOcean, src/repo/dungeon_chests.json, MIT, credited in the output) - Obsidian and
     Bedrock only. Their weights are Hypixel's: every total checked matches the wiki's within 0.1%. They complete a cut
     wiki table (only items it is missing, at SkyOcean's weight) and stand alone where no wiki page was read.
  3. A floor without a wiki page borrows the Master Mode page's Wood-Emerald chests (F2 and F7 show those are the same
     items and weights in both modes). F1 and M1 have neither: F2's chests with Bonzo's items in Scarf's places.
     Marked "derived" in the output.

The dyes (Livid's, Necron's) are not pool rolls - Hypixel rolls them separately, after the pool - so they are kept
under "extras", for their looks only. Each item is looked up in the NEU item repo for its name, vanilla item,
Hypixel item model and, for a head, its skin. Edit this script or the CSVs, not the JSON.
"""
import csv, glob, json, os, re, statistics, sys, urllib.request

SRC = sys.argv[1]
CACHE = sys.argv[2] if len(sys.argv) > 2 else "/tmp/neu_items"
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src/main/resources/assets/sbcs/dungeon_pool.json")
WIKI = os.path.join(HERE, "fixtures", "dungeon_loot")
os.makedirs(CACHE, exist_ok=True)

ROMAN = ["", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"]
# Hypixel drops "Ultimate" from every ultimate enchantment's name but these two.
KEEP_ULTIMATE = {"wise", "jerry"}
# 1.8 item ids the NEU repo still uses, to their 26.2 names.
LEGACY = {
    "minecraft:skull": "minecraft:player_head", "minecraft:dye": "minecraft:gray_dye",
    "minecraft:leather_chestplate": "minecraft:leather_chestplate", "minecraft:fireworks": "minecraft:firework_rocket",
    "minecraft:golden_sword": "minecraft:golden_sword", "minecraft:gold_sword": "minecraft:golden_sword",
    "minecraft:wood_sword": "minecraft:wooden_sword", "minecraft:stone_sword": "minecraft:stone_sword",
    "minecraft:iron_sword": "minecraft:iron_sword", "minecraft:diamond_sword": "minecraft:diamond_sword",
    "minecraft:book": "minecraft:book", "minecraft:enchanted_book": "minecraft:enchanted_book",
    "minecraft:speckled_melon": "minecraft:glistering_melon_slice", "minecraft:fish": "minecraft:cod",
    "minecraft:raw_fish": "minecraft:tropical_fish", "minecraft:potion": "minecraft:potion",
    "minecraft:bow": "minecraft:bow", "minecraft:nether_star": "minecraft:nether_star",
    "minecraft:feather": "minecraft:feather", "minecraft:paper": "minecraft:paper",
}


# SkyOcean ids that are not the item's own id.
ALIAS = {"THORN_BOOTS": "THORNS_BOOTS"}
# Items NEU does not have (hunting shards): key, name, item.
MISSING = {"SHARD_WITHER": ("Wither Shard", "minecraft:prismarine_shard")}
# What an item drawn through Hypixel's pack (paper underneath) looks like without the pack, by the model's folder.
FALLBACK = {"reforge_stones": "minecraft:amethyst_shard", "power_stones": "minecraft:ender_eye",
            "island_relevant/dungeons": "minecraft:bone", "uncategorized": "minecraft:bone"}


def neu(name):
    path = os.path.join(CACHE, name + ".json")
    if not os.path.exists(path):
        url = "https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/items/" + name + ".json"
        try:
            data = urllib.request.urlopen(url, timeout=20).read()
        except Exception as e:
            print("  no NEU item", name, e)
            return None
        open(path, "wb").write(data)
    return json.load(open(path, encoding="utf-8"))


def strip(s):
    return re.sub("§.", "", s).strip()


def entry(sid):
    kind, rest = sid.split(":", 1)
    out = {}
    if kind == "enchantment":
        ench, lvl = rest.rsplit(":", 1)
        lvl = int(lvl)
        out["key"] = "ENCHANTMENT_" + ench.upper() + "_" + str(lvl)
        words = ench.split("_")
        if words[0] == "ultimate" and words[1] not in KEEP_ULTIMATE:
            words = words[1:]
        out["name"] = " ".join(w.capitalize() for w in words).replace("For All", "For All") + " " + ROMAN[lvl]
        out["book"] = True
        out["ultimate"] = ench.startswith("ultimate")
        out["item"] = "minecraft:enchanted_book"
        return out
    if kind == "pet":
        pet, rarity = rest.split(":")
        out["key"] = "PET_" + pet.upper() + "?Rarity=" + rarity
        d = neu(pet.upper() + ";" + str(["COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC"].index(rarity)))
        out["name"] = "[Lvl 1] " + pet.capitalize()
    else:
        out["key"] = ALIAS.get(rest.upper(), rest.upper())
        d = neu(out["key"])
        out["name"] = strip(d["displayname"]) if d else rest.replace("_", " ").title()
    if d:
        item = d.get("itemid", "minecraft:paper")
        out["item"] = LEGACY.get(item, item)
        nbt = d.get("nbttag", "")
        m = re.search(r'ItemModel:"([^"]+)"', nbt)
        if m:
            model = m.group(1)
            if model.startswith("minecraft:"):
                out["item"] = model
            else:
                out["model"] = model
                if out["item"] == "minecraft:paper":
                    for folder, fb in FALLBACK.items():
                        if "/" + folder + "/" in model:
                            out["item"] = fb
        m = re.search(r'color:(\d+)', nbt)
        if m and "leather" in out["item"]:
            out["color"] = int(m.group(1))
        m = re.search(r'textures:\[0:\{Value:"([^"]+)"', nbt) or re.search(r'Value:"([A-Za-z0-9+/=]{40,})"', nbt)
        if m:
            out["skin"] = m.group(1)
            out["item"] = "minecraft:player_head"
        if "ench:[" in nbt or "Enchantments" in nbt:
            out["glint"] = True
    else:
        out["item"] = "minecraft:paper"
    return out



TIERS = ["wood", "gold", "diamond", "emerald", "obsidian", "bedrock"]
FLOORS = ["F%d" % i for i in range(1, 8)] + ["M%d" % i for i in range(1, 8)]
DYES = {"item:dye_livid", "item:dye_necron"}

# The wiki's names, as SkyOcean-style ids. Books are worked out from the name.
NAMES = {
    "Adaptive Belt": "item:adaptive_belt", "Adaptive Blade": "item:stone_blade", "Adaptive Boots": "item:adaptive_boots",
    "Adaptive Chestplate": "item:adaptive_chestplate", "Adaptive Helmet": "item:adaptive_helmet",
    "Adaptive Leggings": "item:adaptive_leggings", "Auto Recombobulator": "item:auto_recombobulator",
    "Dark Claymore": "item:dark_claymore", "Dark Orb": "item:dark_orb", "EPIC Spirit Pet": "pet:spirit:EPIC",
    "LEGENDARY Spirit Pet": "pet:spirit:LEGENDARY", "Fel Skull": "item:fel_skull",
    "First Master Star": "item:first_master_star", "Second Master Star": "item:second_master_star",
    "Third Master Star": "item:third_master_star", "Fourth Master Star": "item:fourth_master_star",
    "Fifth Master Star": "item:fifth_master_star", "Fuming Potato Book": "item:fuming_potato_book",
    "Hot Potato Book": "item:hot_potato_book", "Giant Tooth": "item:giant_tooth", "Giant's Sword": "item:giants_sword",
    "Implosion": "item:implosion_scroll", "Shadow Warp": "item:shadow_warp_scroll", "Wither Shield": "item:wither_shield_scroll",
    "Last Breath": "item:last_breath", "Livid Dagger": "item:livid_dagger",
    "Master Skull - Tier 1": "item:master_skull_tier_1", "Master Skull - Tier 2": "item:master_skull_tier_2",
    "Master Skull - Tier 3": "item:master_skull_tier_3", "Master Skull - Tier 4": "item:master_skull_tier_4",
    "Master Skull - Tier 5": "item:master_skull_tier_5",
    "Necromancer Lord Boots": "item:necromancer_lord_boots", "Necromancer Lord Chestplate": "item:necromancer_lord_chestplate",
    "Necromancer Lord Helmet": "item:necromancer_lord_helmet", "Necromancer Lord Leggings": "item:necromancer_lord_leggings",
    "Necromancer Sword": "item:necromancer_sword", "Necromancer's Brooch": "item:necromancer_brooch",
    "Necron's Handle": "item:necron_handle", "Precursor Eye": "item:precursor_eye", "Precursor Gear": "item:precursor_gear",
    "Recombobulator 3000": "item:recombobulator_3000", "Red Scarf": "item:red_scarf", "Scarf's Studies": "item:scarf_studies",
    "Sadan's Brooch": "item:sadan_brooch", "Shadow Assassin Boots": "item:shadow_assassin_boots",
    "Shadow Assassin Chestplate": "item:shadow_assassin_chestplate", "Shadow Assassin Cloak": "item:shadow_assassin_cloak",
    "Shadow Assassin Helmet": "item:shadow_assassin_helmet", "Shadow Assassin Leggings": "item:shadow_assassin_leggings",
    "Shadow Fury": "item:shadow_fury", "Soulweaver Gloves": "item:soulweaver_gloves", "Spirit Bone": "item:spirit_bone",
    "Spirit Boots": "item:thorn_boots", "Spirit Mask": "item:spirit_mask", "Spirit Shortbow": "item:item_spirit_bow",
    "Spirit Stone": "item:spirit_decoy", "Spirit Sword": "item:spirit_sword", "Spirit Wing": "item:spirit_wing",
    "Summoning Ring": "item:summoning_ring", "Suspicious Vial": "item:suspicious_vial", "Warped Stone": "item:aote_stone",
    "Wither Blood": "item:wither_blood", "Wither Boots": "item:wither_boots", "Wither Catalyst": "item:wither_catalyst",
    "Wither Chestplate": "item:wither_chestplate", "Wither Cloak Sword": "item:wither_cloak",
    "Wither Helmet": "item:wither_helmet", "Wither Leggings": "item:wither_leggings", "Wither Shard": "item:shard_wither",
    "Red Nose": "item:red_nose", "Bonzo's Mask": "item:bonzo_mask", "Bonzo's Staff": "item:bonzo_staff",
}
ULT = {"bank", "combo", "last_stand", "legion", "no_pain_no_gain", "one_for_all", "rend", "soul_eater", "swarm",
       "wisdom", "wise", "jerry"}
LVL = {"I": 1, "II": 2, "III": 3, "IV": 4, "V": 5, "VI": 6, "VII": 7}


def sid_of(name):
    m = re.match(r"^Enchanted Book \((.+) ([IV]+)\)$", name)
    if m:
        ench = m.group(1).lower().replace(" ", "_")
        if ench.startswith("ultimate_"):
            ench = ench[len("ultimate_"):]
        if ench in ULT:
            ench = "ultimate_" + ench
        return "enchantment:%s:%d" % (ench, LVL[m.group(2)])
    if name not in NAMES:
        sys.exit("no id for the wiki's " + repr(name) + " - add it to NAMES")
    return NAMES[name]


def read_wiki(floor):
    """{tier: (rows [(sid, weight, quality, cost)], total)} from the floor's CSV, or {}."""
    path = os.path.join(WIKI, floor + ".csv")
    if not os.path.exists(path):
        return {}
    by = {}
    for r in csv.reader(l for l in open(path, encoding="utf-8") if not l.startswith("#")):
        tier, base, name, quality, cost, weight, chance = r
        by.setdefault(tier, ([], []))
        by[tier][0].append((sid_of(name), int(weight), int(quality), int(cost)))
        if float(chance) > 0:
            by[tier][1].append(int(weight) / float(chance) * 100)
    return {t: (rows, round(statistics.median(tot))) for t, (rows, tot) in by.items()}


def look(sid):
    if sid not in cache:
        if sid == "item:shard_wither":
            n, item = MISSING["SHARD_WITHER"]
            cache[sid] = {"key": "SHARD_WITHER", "name": n, "item": item}
        else:
            cache[sid] = entry(sid)
        print(sid, "->", cache[sid]["key"], "|", cache[sid]["name"])
    return dict(cache[sid])


src = json.load(open(SRC))
cache = {}
pools, sources, totals, extras = {}, {}, {}, {}
for floor in FLOORS:
    wiki = read_wiki(floor)
    mm = read_wiki("M" + floor[1:]) if floor[0] == "F" else {}
    pools[floor], sources[floor], totals[floor] = {}, {}, {}
    sky = src.get(floor, {})
    for tier in TIERS:
        if tier == "bedrock" and int(floor[1]) < 5:
            continue
        rows, src_note, total = None, None, None
        if tier in wiki:
            rows, total = [list(r) for r in wiki[tier][0]], wiki[tier][1]
            src_note = "wiki"
        elif tier not in ("obsidian", "bedrock") and tier in mm:
            rows, total = [list(r) for r in mm[tier][0]], mm[tier][1]
            src_note = "wiki (Master Mode's, same items as normal)"
        if tier in ("obsidian", "bedrock") and tier in sky:
            have = {r[0] for r in rows} if rows else set()
            extra = []
            for e in sky[tier]:
                if e["id"] in DYES:
                    extras[e["id"].split(":")[1].upper()] = look(e["id"])
                    continue
                if e["id"] not in have:
                    extra.append([e["id"], e["weight"], None, None])
                    have.add(e["id"])
            if rows is None:
                rows, src_note = extra, "SkyOcean"
            elif extra and total and sum(r[1] for r in rows) + 1 < total * 0.995:
                # A cut wiki table: SkyOcean has the missing rows.
                rows += extra
                src_note = "wiki, completed from SkyOcean"
        if rows is None and floor[1] == "1" and tier in read_wiki("F2"):
            swap = {"item:scarf_studies": "item:red_nose", "item:red_scarf": "item:bonzo_mask",
                    "item:stone_blade": "item:bonzo_staff"}
            rows = [[swap.get(r[0], r[0]), r[1], r[2], r[3]] for r in read_wiki("F2")[tier][0]
                    if r[0] != "item:adaptive_belt"]
            src_note = "derived from F2 (no wiki page read)"
        if rows is None:
            print("NO POOL", floor, tier)
            continue
        out = []
        for sid, weight, quality, cost in rows:
            row = look(sid)
            row["weight"] = weight
            if quality is not None:
                row["quality"] = quality
            if cost:
                row["cost"] = cost
            out.append(row)
        pools[floor][tier] = out
        sources[floor][tier] = src_note
        if total and src_note.startswith("wiki"):
            totals[floor][tier] = total

out = {
    "note": "Every dungeon reward chest's pool, for the Ossuary's reel and the test chests. Weights are Hypixel's "
            "(first-roll chance = weight / total, the total counting one Wither Essence roll the reel leaves out). "
            "From the Hypixel SkyBlock Wiki (hypixelskyblock.minecraft.wiki, the floors' Loot pages) and SkyOcean "
            "(meowdding/SkyOcean, src/repo/dungeon_chests.json, MIT licence, Copyright (c) meowdding); 'sources' "
            "says which per chest. Names, items and skins from the NEU item repo. Generated by "
            "tools/gen_dungeon_pool.py - edit that, not this.",
    "sources": sources,
    "totals": totals,
    "extras": extras,
    "pools": pools,
}
open(OUT, "w", encoding="utf-8").write(json.dumps(out, indent=1, ensure_ascii=False) + "\n")
print("wrote", OUT)
