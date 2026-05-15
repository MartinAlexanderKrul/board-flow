#!/usr/bin/env python3
"""
BGG Player Name Normalizer

Fetches all plays for a BGG user, matches player names against a player roster
(with aliases), and edits plays on BGG to use canonical names + BGG usernames.

DRY RUN by default — prints what would change without touching BGG.
Pass --execute to apply changes.

Usage:
    python normalize_bgg_players.py --config bgg_player_config.json
    python normalize_bgg_players.py --config bgg_player_config.json --execute

Config file format (bgg_player_config.json):
    {
      "bgg_username": "YourBGGUsername",
      "bgg_password": "YourBGGPassword",
      "players": [
        {
          "displayName": "Yotam Ophir",
          "aliases": ["Yotke", "Yotam"],
          "bggUsername": "Yotke"
        }
      ]
    }
"""

import argparse
import json
import sys
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from typing import Optional

import requests


# ---------------------------------------------------------------------------
# Data model
# ---------------------------------------------------------------------------

@dataclass
class Player:
    display_name: str
    aliases: list[str] = field(default_factory=list)
    bgg_username: str = ""

    def matches(self, name: str) -> bool:
        name_lower = name.strip().lower()
        return any(a.strip().lower() == name_lower for a in [self.display_name] + self.aliases)

    def already_canonical(self, play_player: dict) -> bool:
        """Return True if this play entry already uses the canonical name (no update needed)."""
        name_ok = play_player["name"].strip() == self.display_name.strip()
        username_ok = play_player.get("username", "") == self.bgg_username
        return name_ok and username_ok


# ---------------------------------------------------------------------------
# BGG API helpers
# ---------------------------------------------------------------------------

session = requests.Session()
session.headers.update({"User-Agent": "BGGPlayerNormalizer/1.0"})


def login(username: str, password: str) -> bool:
    resp = session.post(
        "https://boardgamegeek.com/login/api/v1",
        json={"credentials": {"username": username, "password": password}},
        timeout=30,
    )
    return resp.ok and "SessionID" in session.cookies


def fetch_plays(username: str) -> list[dict]:
    plays = []
    page = 1
    total = None
    print(f"Fetching plays for '{username}'", end="", flush=True)
    while True:
        resp = session.get(
            f"https://boardgamegeek.com/xmlapi2/plays"
            f"?username={requests.utils.quote(username)}&type=thing&page={page}",
            timeout=30,
        )
        if not resp.ok:
            if page == 1:
                raise RuntimeError(f"Failed to fetch plays: HTTP {resp.status_code}")
            break
        root = ET.fromstring(resp.text)
        if total is None:
            total = int(root.attrib.get("total", 0))
        page_plays = root.findall("play")
        if not page_plays:
            break
        for play_el in page_plays:
            game_el = play_el.find("item")
            comments_el = play_el.find("comments")
            players = [
                {
                    "name": p.attrib.get("name", ""),
                    "score": p.attrib.get("score", ""),
                    "win": p.attrib.get("win", "0"),
                    "color": p.attrib.get("color", ""),
                    "rating": p.attrib.get("rating", ""),
                    "new": p.attrib.get("new", "0"),
                    "username": p.attrib.get("username", ""),
                }
                for p in play_el.findall(".//player")
                if p.attrib.get("name", "").strip()
            ]
            plays.append({
                "id": play_el.attrib.get("id"),
                "date": play_el.attrib.get("date", ""),
                "length": play_el.attrib.get("length", "0"),
                "location": play_el.attrib.get("location", ""),
                "quantity": play_el.attrib.get("quantity", "1"),
                "incomplete": play_el.attrib.get("incomplete", "0"),
                "nowinstats": play_el.attrib.get("nowinstats", "1"),
                "game_name": game_el.attrib.get("name", "") if game_el is not None else "",
                "game_id": game_el.attrib.get("objectid", "") if game_el is not None else "",
                "comments": (comments_el.text or "").strip() if comments_el is not None else "",
                "players": players,
            })
        print(".", end="", flush=True)
        if len(plays) >= (total or 0):
            break
        page += 1
        time.sleep(0.3)
    print(f" done ({len(plays)} plays, total={total})")
    return plays


def resolve_player(name: str, roster: list[Player]) -> Optional[Player]:
    for p in roster:
        if p.matches(name):
            return p
    return None


def compute_changes(play: dict, roster: list[Player]) -> list[tuple[dict, Player]]:
    """Return (original_player_dict, canonical_Player) for every player that needs updating."""
    changes = []
    for pp in play["players"]:
        canonical = resolve_player(pp["name"], roster)
        if canonical and not canonical.already_canonical(pp):
            changes.append((pp, canonical))
    return changes


def submit_play_edit(play: dict, roster: list[Player]) -> bool:
    """POST the edited play to BGG. Returns True on apparent success."""
    form: dict[str, str] = {
        "ajax": "1",
        "action": "save",
        "version": "2",
        "objecttype": "thing",
        "objectid": play["game_id"],
        "playdate": play["date"],
        "dateinput": play["date"],
        "length": play["length"],
        "location": play["location"],
        "comments": play["comments"],
        "quantity": play["quantity"],
        "incomplete": play["incomplete"],
        "nowinstats": play["nowinstats"],
        "playid": play["id"],
    }
    for i, pp in enumerate(play["players"]):
        pos = i + 1
        canonical = resolve_player(pp["name"], roster)
        name = canonical.display_name if canonical else pp["name"]
        username = canonical.bgg_username if canonical else pp.get("username", "")
        form[f"players[{pos}][name]"] = name
        form[f"players[{pos}][position]"] = str(pos)
        form[f"players[{pos}][score]"] = pp["score"]
        form[f"players[{pos}][color]"] = pp["color"]
        form[f"players[{pos}][win]"] = pp["win"]
        form[f"players[{pos}][new]"] = pp["new"]
        form[f"players[{pos}][rating]"] = pp["rating"] or "0"
        if username:
            form[f"players[{pos}][selected]"] = "1"
            form[f"players[{pos}][username]"] = username
        else:
            form[f"players[{pos}][selected]"] = "0"
    resp = session.post(
        "https://boardgamegeek.com/geekplay.php",
        data=form,
        headers={
            "Referer": "https://boardgamegeek.com",
            "X-Requested-With": "XMLHttpRequest",
        },
        timeout=30,
    )
    return resp.ok and ("playid" in resp.text or "error" not in resp.text.lower()[:200])


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    parser = argparse.ArgumentParser(
        description="Normalize player names on BGG plays using a player roster with aliases."
    )
    parser.add_argument(
        "--config",
        default="bgg_player_config.json",
        help="Path to JSON config file (default: bgg_player_config.json)",
    )
    parser.add_argument(
        "--execute",
        action="store_true",
        help="Apply changes to BGG. Without this flag the script only prints what would change.",
    )
    args = parser.parse_args()

    # Load config
    try:
        with open(args.config) as f:
            config = json.load(f)
    except FileNotFoundError:
        print(f"Config file not found: {args.config}")
        print("Create it with your BGG credentials and player roster. See script header for format.")
        sys.exit(1)

    bgg_username: str = config["bgg_username"]
    bgg_password: str = config["bgg_password"]
    roster: list[Player] = [
        Player(
            display_name=p["displayName"],
            aliases=p.get("aliases", []),
            bgg_username=p.get("bggUsername", ""),
        )
        for p in config.get("players", [])
    ]

    print(f"Loaded {len(roster)} players from config:")
    for p in roster:
        all_names = [p.display_name] + p.aliases
        bgg = f" [BGG: {p.bgg_username}]" if p.bgg_username else ""
        print(f"  {p.display_name}{bgg}  ←  also matches: {', '.join(p.aliases) or '(none)'}")

    mode = "EXECUTE" if args.execute else "DRY RUN (pass --execute to apply changes)"
    print(f"\nMode: {mode}\n")

    # Login
    print(f"Logging into BGG as '{bgg_username}'...")
    if not login(bgg_username, bgg_password):
        print("Login failed. Check your credentials in the config file.")
        sys.exit(1)
    print("Login successful.\n")

    # Fetch plays
    plays = fetch_plays(bgg_username)

    # Find plays that need changes
    to_update = [(play, compute_changes(play, roster)) for play in plays]
    to_update = [(play, changes) for play, changes in to_update if changes]

    print(f"\nPlays needing update: {len(to_update)} / {len(plays)}\n")

    if not to_update:
        print("Nothing to update. All plays already use canonical player names.")
        return

    # Print the full diff
    for play, changes in to_update:
        print(f"  Play #{play['id']}  {play['date']}  {play['game_name']}")
        for old_pp, canonical in changes:
            old_name = old_pp["name"]
            old_user = old_pp.get("username", "")
            new_name = canonical.display_name
            new_user = canonical.bgg_username
            name_part = f"'{old_name}' → '{new_name}'" if old_name != new_name else f"'{old_name}' (name unchanged)"
            user_part = ""
            if old_user != new_user:
                user_part = f"  username: '{old_user}' → '{new_user}'"
            print(f"    {name_part}{user_part}")
    print()

    if not args.execute:
        print(f"Dry run complete — {len(to_update)} plays would be updated.")
        print("Run with --execute to apply these changes to BGG.")
        return

    # Confirmation prompt before writing
    answer = input(f"About to edit {len(to_update)} plays on BGG. Type YES to continue: ")
    if answer.strip() != "YES":
        print("Aborted.")
        return

    updated = 0
    failed = 0
    for play, _ in to_update:
        ok = submit_play_edit(play, roster)
        if ok:
            print(f"  OK  #{play['id']}  {play['date']}  {play['game_name']}")
            updated += 1
        else:
            print(f"  FAIL #{play['id']}  {play['date']}  {play['game_name']}")
            failed += 1
        time.sleep(0.4)  # Be polite to BGG servers

    print(f"\nDone. Updated: {updated}  Failed: {failed}")


if __name__ == "__main__":
    main()
