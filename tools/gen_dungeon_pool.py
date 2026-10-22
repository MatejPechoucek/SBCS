#!/usr/bin/env python3
"""Writes src/main/resources/assets/sbcs/dungeon_pool.json - the Ossuary's reel filler.

    python3 tools/gen_dungeon_pool.py <SkyOcean's src/repo/dungeon_chests.json> [neu cache dir]

The pools and weights are SkyOcean's (meowdding/SkyOcean, MIT - credited in the output). They are NOT Hypixel's
real chances, so the Ossuary only uses them to fill the reel, never for odds. Each id is looked up in the NEU item
repo for its name, its vanilla item, its Hypixel item model and, for a head, its skin. Edit this script, not the JSON.
"""
import json, os, re, sys, urllib.request

SRC = sys.argv[1]
CACHE = sys.argv[2] if len(sys.argv) > 2 else "/tmp/neu_items"
OUT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/sbcs/dungeon_pool.json")
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


src = json.load(open(SRC))
cache = {}
pools = {}
for floor, chests in src.items():
    pools[floor] = {}
    for tier, items in chests.items():
        rows = []
        for e in items:
            sid = e["id"]
            if sid not in cache:
                cache[sid] = entry(sid)
                print(sid, "->", cache[sid]["key"], "|", cache[sid]["name"])
            row = dict(cache[sid])
            row["weight"] = e["weight"]
            rows.append(row)
        pools[floor][tier] = rows

out = {
    "note": "Reel filler for the Ossuary. Pools and weights from SkyOcean (meowdding/SkyOcean, src/repo/dungeon_chests.json, "
            "MIT licence, Copyright (c) meowdding) - not Hypixel's real chances. Names, items and skins from the NEU item repo. "
            "Generated by tools/gen_dungeon_pool.py - edit that, not this.",
    "pools": pools,
}
open(OUT, "w", encoding="utf-8").write(json.dumps(out, indent=1, ensure_ascii=False) + "\n")
print("wrote", OUT)
