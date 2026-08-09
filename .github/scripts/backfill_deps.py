#!/usr/bin/env python3
"""Backfill dependency metadata on published Reach Display Modrinth versions.

PATCHes /v2/version/{id} for every v3.0.0-* version so launchers auto-install
fabric-api + midnightlib (required) and offer modmenu (optional).
Idempotent: skips versions whose dependency set already matches.
"""
import json
import os
import sys
import urllib.error
import urllib.request

BASE = "https://api.modrinth.com/v2"
TOKEN = os.environ["MODRINTH_TOKEN"]
# slug -> dependency_type
WANTED = {
    "fabric-api": "required",
    "midnightlib": "required",
    "modmenu": "optional",
}


def get(path):
    req = urllib.request.Request(BASE + path)
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


def patch(path, body):
    req = urllib.request.Request(
        BASE + path, data=json.dumps(body).encode(), method="PATCH"
    )
    req.add_header("Authorization", f"Bearer {TOKEN}")
    req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


def main():
    ids = {}
    for slug in WANTED:
        info = get(f"/project/{slug}")
        ids[slug] = info["id"]
        print(f"resolved {slug} -> {info['id']}")

    versions = get("/project/reach-display/version")
    targets = [v for v in versions if v["version_number"].startswith("3.0.0-")]
    if not targets:
        print("no v3.0.0 versions found")
        return 1

    failed = 0
    for v in sorted(targets, key=lambda x: x["version_number"]):
        existing = {(d.get("project_id"), d.get("dependency_type"))
                    for d in v.get("dependencies", [])}
        wanted = {(ids[s], t) for s, t in WANTED.items()}
        if existing == wanted:
            print(f"SKIP  {v['version_number']} already correct")
            continue
        deps = [{"project_id": ids[s], "dependency_type": t} for s, t in WANTED.items()]
        try:
            patch(f"/version/{v['id']}", {"dependencies": deps})
            print(f"OK    {v['version_number']} ({v['id']}) -> {deps}")
        except urllib.error.HTTPError as e:
            failed += 1
            print(f"FAIL  {v['version_number']}: {e.code} {e.read().decode()[:200]}")

    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
