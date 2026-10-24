#!/usr/bin/env python3
"""Writes src/main/resources/assets/sbcs/dungeon_heads.json - the Ossuary's dealers' heads, from the NEU repo.

    python3 tools/gen_dungeon_heads.py [neu cache dir]

The bosses themselves have no heads anywhere (they are not in NEU's bestiary), so each dealer wears its floor's
own head item: Bonzo's Mask, Scarf's Studies, the Adaptive Helmet, the Spirit Mask, a Shadow Assassin's helmet for
Livid, the Terracotta (NEU's bestiary) for Sadan, and each Wither Lord's own helmet. Keys are what OssuaryScene asks for.
"""
import json, os, re, sys, urllib.request

CACHE = sys.argv[1] if len(sys.argv) > 1 else "/tmp/neu_items"
OUT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/sbcs/dungeon_heads.json")
REPO = "https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/"
os.makedirs(CACHE, exist_ok=True)

ITEMS = {
    "BONZO": "BONZO_MASK", "SCARF": "SCARF_STUDIES", "PROFESSOR": "ADAPTIVE_HELMET", "THORN": "SPIRIT_MASK",
    "LIVID": "SHADOW_ASSASSIN_HELMET", "MAXOR": "SPEED_WITHER_HELMET", "STORM": "WISE_WITHER_HELMET",
    "GOLDOR": "TANK_WITHER_HELMET", "NECRON": "POWER_WITHER_HELMET",
}


def fetch(path, name):
    local = os.path.join(CACHE, name)
    if not os.path.exists(local):
        open(local, "wb").write(urllib.request.urlopen(REPO + path, timeout=30).read())
    return json.load(open(local, encoding="utf-8"))


out = {}
for key, item in ITEMS.items():
    d = fetch("items/" + item + ".json", item + ".json")
    m = re.search(r'Value:"([A-Za-z0-9+/=]{40,})"', d["nbttag"])
    out[key] = m.group(1)
best = fetch("constants/bestiary.json", "bestiary.json")
for mob in best["catacombs"]["mobs"]:
    if "Terracotta" in mob["name"]:
        out["SADAN"] = mob["texture"]
open(OUT, "w").write(json.dumps(out, indent=1) + "\n")
print("wrote", len(out), "heads")
