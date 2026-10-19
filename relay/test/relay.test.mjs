// Run with: node --test relay/test
import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { check, format } from "../src/index.js";

const sheet = (gameId) => ({
  kind: "score-sheet",
  gameId,
  gameName: "Game",
  gameIds: [gameId],
  document: { id: `community-${gameId}`, rows: [{ kind: "points", id: "a", label: "A" }] },
});

test("a well-formed sheet passes, broken ones say why", () => {
  assert.equal(check(sheet(12)), null);
  assert.equal(check({ ...sheet(12), kind: "dice" }), "Unknown kind");
  assert.equal(check({ ...sheet(12), gameIds: [13] }), "Bad gameIds");
  assert.equal(check({ ...sheet(12), document: { id: "wingspan", rows: [{}] } }), "Sheet id must be community-<gameId>");
  assert.equal(check({ ...sheet(12), document: { id: "community-12", rows: [] } }), "Bad rows");
});

test("a guide must be for the game it is sent for", () => {
  const guide = { kind: "setup-guide", gameId: 13, gameName: "Catan", gameIds: [13, 2807], document: { gameId: 13, sections: [{}] } };
  assert.equal(check(guide), null);
  assert.equal(check({ ...guide, document: { gameId: 14, sections: [{}] } }), "Guide gameId does not match");
});

test("format writes the repo's style, so the index diff is one entry", () => {
  const index = readFileSync(new URL("../../setup-guides/index.json", import.meta.url), "utf8");
  assert.equal(format(JSON.parse(index)), index);
});
