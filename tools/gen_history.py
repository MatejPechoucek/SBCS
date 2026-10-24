"""Write docs/history/index.html: every commit on every branch of origin, as an interactive page.

Run from the repo root after fetching every branch (the history workflow does this on each push):
    git fetch origin '+refs/heads/*:refs/remotes/origin/*'
    python3 tools/gen_history.py

The page is docs/history/template.html with the log put in for __DATA__. Edit the template, not index.html.
The workflow's own "Update the history page" commits are left out, their children joined to their parents,
so the page does not have to chase itself.
"""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/history/index.html"
TEMPLATE = ROOT / "docs/history/template.html"
BOT_SUBJECT = "Update the history page"


def git(*args):
    return subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True, check=True).stdout


def numstat(*args):
    files = []
    for line in git(*args).strip().splitlines():
        added, removed, path = line.split("\t", 2)
        files.append([path, added, removed])
    return files


def main():
    commits = []
    for h in git("log", "--remotes=origin", "--topo-order", "--format=%H").split():
        parents, refs, author, date, subject, body = git(
            "show", "-s", "--format=%P%x00%D%x00%an%x00%aI%x00%s%x00%b", h).split("\x00")
        parents = parents.split()
        refs = [r.strip()[len("origin/"):] for r in refs.split(",")
                if r.strip().startswith("origin/") and "HEAD" not in r]
        commits.append(dict(h=h, p=parents, r=refs, a=author, d=date, s=subject, b=body.strip()))

    # Skip the bot's commits: a child of one points at the bot commit's own first parent instead.
    bot = {c["h"]: c["p"][0] for c in commits if c["s"] == BOT_SUBJECT and len(c["p"]) == 1}

    def real(h):
        while h in bot:
            h = bot[h]
        return h

    kept = []
    for c in commits:
        if c["h"] in bot:
            # A branch tip that is a bot commit hands its label down.
            if c["r"]:
                target = next((k for k in commits if k["h"] == real(c["h"])), None)
                if target is not None:
                    target["r"] = sorted(set(target["r"]) | set(c["r"]), key=lambda r: r != "main")
            continue
        seen, parents = set(), []
        for p in c["p"]:
            p = real(p)
            if p not in seen:
                seen.add(p)
                parents.append(p)
        c["p"] = parents
        # Merges are diffed against the first parent: what the merge brought in.
        c["f"] = numstat("diff", "--numstat", "-M", c["p"][0], c["h"]) if len(c["p"]) > 1 \
            else numstat("show", "--numstat", "--format=", "-M", c["h"])
        kept.append(c)

    if not kept:
        sys.exit("No commits under refs/remotes/origin - fetch every branch first.")
    data = json.dumps(kept, ensure_ascii=False).replace("</", "<\\/")
    snapshot = max(c["d"] for c in kept)[:10]
    page = TEMPLATE.read_text(encoding="utf-8").replace("__DATA__", data).replace("__SNAP__", snapshot)
    OUT.write_text(page, encoding="utf-8", newline="\n")
    print(f"{OUT.relative_to(ROOT)}: {len(kept)} commits, last {snapshot}")


if __name__ == "__main__":
    main()
