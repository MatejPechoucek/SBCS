"""The mod icon: a gold pixel-art S on a strip of film, on a dark badge.

32 x 32 pixel art, written 8x up (256 x 256, nearest neighbour) to src/main/resources/assets/sbcs/icon.png.
The menu shows it at its native 32 (MenuIcon halves it back down exactly), Mod Menu and Modrinth at whatever size.

    python3 tools/gen_icon.py
"""
import os
from PIL import Image, ImageDraw

N = 32
INK = (16, 12, 18, 255)
LIGHT, GOLD, SHADE, DEPTH = (255, 222, 128, 255), (240, 178, 64, 255), (184, 116, 34, 255), (122, 72, 22, 255)
FILM, SPROCKET = (20, 20, 26, 255), (200, 190, 170, 255)

# The S: 10 x 10, a 3-pixel stroke, square enough to read at 16 px.
S = [".########.",
     "##########",
     "###....###",
     "###.......",
     "#########.",
     ".#########",
     ".......###",
     "###....###",
     "##########",
     ".########."]
OX, OY = 11, 10


def badge():
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((0, 0, N - 1, N - 1), radius=5, fill=(28, 31, 40, 255), outline=(60, 52, 40, 255))
    d.line((5, 1, N - 6, 1), fill=(90, 80, 60, 255))
    return im


def film(im):
    """A band of film across the badge, sprocket holes along both edges."""
    d = ImageDraw.Draw(im)
    d.rectangle((1, 5, N - 2, N - 6), fill=FILM)
    for x in range(3, N - 2, 4):
        d.rectangle((x, 6, x + 1, 7), fill=SPROCKET)
        d.rectangle((x, N - 8, x + 1, N - 7), fill=SPROCKET)


def letter():
    """The S's pixels, lit from the top left: edges facing up or left light, down or right shaded."""
    px = {}
    h = len(S)
    for r, row in enumerate(S):
        w = len(row)
        for c, ch in enumerate(row):
            if ch != '#':
                continue
            up = r == 0 or S[r - 1][c] != '#'
            left = c == 0 or row[c - 1] != '#'
            down = r == h - 1 or S[r + 1][c] != '#'
            right = c == w - 1 or row[c + 1] != '#'
            col = GOLD
            if up or left:
                col = LIGHT
            if down or right:
                col = SHADE
            px[(OX + c, OY + r)] = col
    return px


def extrude(px):
    """A pixel of depth down and to the right."""
    out = dict(px)
    for (x, y) in px:
        if (x + 1, y + 1) not in out:
            out[(x + 1, y + 1)] = DEPTH
    return out


def outlined(px):
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    p = im.load()
    for (x, y), c in px.items():
        p[x, y] = c
    out = im.copy()
    o = out.load()
    for y in range(N):
        for x in range(N):
            if p[x, y][3]:
                continue
            if any(0 <= x + dx < N and 0 <= y + dy < N and p[x + dx, y + dy][3]
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                o[x, y] = INK
    return out


def icon():
    out = badge()
    film(out)
    out.alpha_composite(outlined(extrude(letter())))
    return out


if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    dest = os.path.join(here, "..", "src", "main", "resources", "assets", "sbcs", "icon.png")
    icon().resize((N * 8, N * 8), Image.NEAREST).save(dest)
    print("wrote", os.path.normpath(dest))
