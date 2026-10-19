// BoardFlow contribution relay: turns a guide or score sheet the user reviewed in the app into a
// pull request on the repo. The app holds no GitHub credentials; this Worker does, as a GitHub App.
//
// POST /  {kind, gameId, gameName, gameIds, document, appVersion}
//   kind "setup-guide" -> setup-guides/<gameId>.json + setup-guides/index.json
//   kind "score-sheet" -> score-sheets/<gameId>.json + score-sheets/index.json
// One branch and one open PR per game and kind (contrib/<kind>-<gameId>); a newer submission
// replaces the branch's commit, so the PR always shows the latest version. The content checks in
// .github/workflows/content.yml run on the PR; the maintainer merges it.

const MAX_BODY_BYTES = 256 * 1024;
const RATE_LIMIT_PER_HOUR = 10;
const KINDS = {
  "setup-guide": { dir: "setup-guides", listKey: "guides", label: "Quick Setup guide" },
  "score-sheet": { dir: "score-sheets", listKey: "sheets", label: "Score sheet" },
};

export default {
  async fetch(request, env) {
    if (request.method !== "POST") return json(405, { error: "POST only" });
    try {
      const text = await request.text();
      if (text.length > MAX_BODY_BYTES) return json(413, { error: "Too large" });
      const ip = request.headers.get("CF-Connecting-IP") || "unknown";
      if (!(await underRateLimit(env, ip))) return json(429, { error: "Too many submissions, try later" });

      const payload = JSON.parse(text);
      const problem = check(payload);
      if (problem) return json(400, { error: problem });

      const url = await openPullRequest(env, payload);
      return json(201, { pullRequest: url });
    } catch (e) {
      if (e instanceof SyntaxError) return json(400, { error: "Not JSON" });
      console.error(e.stack || String(e));
      return json(502, { error: "Could not open the pull request" });
    }
  },
};

/** Shape checks only; the real validation is the CI run on the pull request. */
export function check(p) {
  if (!p || typeof p !== "object") return "No payload";
  if (!KINDS[p.kind]) return "Unknown kind";
  if (!Number.isInteger(p.gameId) || p.gameId <= 0) return "Bad gameId";
  if (typeof p.gameName !== "string" || !p.gameName.trim() || p.gameName.length > 200) return "Bad gameName";
  if (!Array.isArray(p.gameIds) || !p.gameIds.includes(p.gameId) || p.gameIds.some((id) => !Number.isInteger(id) || id <= 0) || p.gameIds.length > 50)
    return "Bad gameIds";
  const d = p.document;
  if (!d || typeof d !== "object") return "No document";
  if (p.kind === "score-sheet") {
    if (d.id !== `community-${p.gameId}`) return "Sheet id must be community-<gameId>";
    if (!Array.isArray(d.rows) || d.rows.length === 0 || d.rows.length > 80) return "Bad rows";
  } else {
    if (d.gameId !== p.gameId) return "Guide gameId does not match";
    if (!Array.isArray(d.sections) || d.sections.length === 0) return "Bad sections";
  }
  return null;
}

async function openPullRequest(env, p) {
  const kind = KINDS[p.kind];
  const gh = await github(env);
  const repo = `/repos/${env.GITHUB_OWNER}/${env.GITHUB_REPO}`;
  const base = env.BASE_BRANCH || "master";
  const branch = `contrib/${p.kind}-${p.gameId}`;
  const file = `${p.gameId}.json`;

  const baseRef = await gh("GET", `${repo}/git/ref/heads/${base}`);
  const baseCommit = await gh("GET", `${repo}/git/commits/${baseRef.object.sha}`);
  const indexPath = `${kind.dir}/index.json`;
  const index = JSON.parse(decodeBase64(
    (await gh("GET", `${repo}/contents/${indexPath}?ref=${base}`)).content
  ));
  const entries = index[kind.listKey] || [];

  // The catalog takes a higher version as an update; the index and the file must agree.
  let document = p.document;
  let entry;
  if (p.kind === "setup-guide") {
    const old = entries.find((e) => e.gameId === p.gameId);
    const version = (old ? old.version : 0) + 1;
    document = { ...document, version };
    entry = { gameId: p.gameId, gameName: p.gameName, gameIds: [...p.gameIds].sort((a, b) => a - b), version, schemaVersion: document.schemaVersion || 1, path: file };
    index.guides = upsert(entries, entry, (e) => e.gameId === p.gameId).sort((a, b) => a.gameId - b.gameId);
  } else {
    const old = entries.find((e) => e.id === document.id);
    const version = (old ? old.version : 0) + 1;
    entry = { id: document.id, gameName: p.gameName, gameIds: [...p.gameIds].sort((a, b) => a - b), version, path: file };
    index.sheets = upsert(entries, entry, (e) => e.id === document.id).sort((a, b) => a.id.localeCompare(b.id));
  }

  const tree = await gh("POST", `${repo}/git/trees`, {
    base_tree: baseCommit.tree.sha,
    tree: [
      { path: `${kind.dir}/${file}`, mode: "100644", type: "blob", content: format(document) },
      { path: indexPath, mode: "100644", type: "blob", content: format(index) },
    ],
  });
  const commit = await gh("POST", `${repo}/git/commits`, {
    message: `${kind.label} for ${p.gameName} (BGG ${p.gameId})\n\nShared from BoardFlow ${p.appVersion || ""} after the user marked it reviewed.`,
    tree: tree.sha,
    parents: [baseRef.object.sha],
  });

  // Always based on the latest master, so an older submission's branch is simply replaced.
  const existing = await gh("GET", `${repo}/git/ref/heads/${branch}`, undefined, true);
  if (existing) await gh("PATCH", `${repo}/git/refs/heads/${branch}`, { sha: commit.sha, force: true });
  else await gh("POST", `${repo}/git/refs`, { ref: `refs/heads/${branch}`, sha: commit.sha });

  const open = await gh("GET", `${repo}/pulls?state=open&head=${env.GITHUB_OWNER}:${branch}`);
  if (open.length > 0) {
    await gh("POST", `${repo}/issues/${open[0].number}/comments`, { body: "Updated with a newer version from the app." });
    return open[0].html_url;
  }
  const pr = await gh("POST", `${repo}/pulls`, {
    title: `${kind.label}: ${p.gameName}`,
    head: branch,
    base,
    body: prBody(p, kind, document),
  });
  // Labels are a convenience; a missing label must not fail the submission.
  await gh("POST", `${repo}/issues/${pr.number}/labels`, { labels: ["contribution", p.kind] }, true);
  return pr.html_url;
}

function prBody(p, kind, document) {
  const provenance = document.provenance || {};
  const lines = [
    `**${kind.label}** for [${p.gameName}](https://boardgamegeek.com/boardgame/${p.gameId}), shared from BoardFlow ${p.appVersion || ""}.`,
    "",
    "The user marked it as checked against the rulebook in the app. Check it against the rulebook too before merging; once merged, every install downloads it.",
    "",
  ];
  if (provenance.aiModel) lines.push(`- Drafted by ${provenance.aiModel}`);
  if (provenance.sources && provenance.sources.length) lines.push(`- From: ${provenance.sources.join(", ")}`);
  if (p.kind === "score-sheet") lines.push(`- ${document.rows.length} rows, ${(document.examples || []).length} worked examples`);
  else lines.push(`- ${document.sections.length} sections`);
  return lines.join("\n");
}

function upsert(list, entry, same) {
  return [...list.filter((e) => !same(e)), entry];
}

/** The repo's JSON style: 2-space indent, arrays of numbers on one line, a final newline. */
export function format(value) {
  return JSON.stringify(value, null, 2).replace(/\[\s*(-?\d+(?:\s*,\s*-?\d+)*)\s*\]/g, (_, nums) =>
    `[${nums.split(",").map((n) => n.trim()).join(", ")}]`
  ) + "\n";
}

function decodeBase64(b64) {
  const bytes = Uint8Array.from(atob(b64.replace(/\n/g, "")), (c) => c.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}

async function underRateLimit(env, ip) {
  if (!env.RATE) return true;
  const key = `rate:${ip}:${Math.floor(Date.now() / 3600_000)}`;
  const count = parseInt((await env.RATE.get(key)) || "0", 10);
  if (count >= RATE_LIMIT_PER_HOUR) return false;
  await env.RATE.put(key, String(count + 1), { expirationTtl: 3700 });
  return true;
}

function json(status, body) {
  return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
}

// --- GitHub App auth -------------------------------------------------------------------------

/** A GitHub API caller authenticated as the app's installation on the repo. */
async function github(env) {
  const jwt = await appJwt(env.GITHUB_APP_ID, env.GITHUB_APP_PRIVATE_KEY);
  const tokenResponse = await fetch(`https://api.github.com/app/installations/${env.GITHUB_INSTALLATION_ID}/access_tokens`, {
    method: "POST",
    headers: headers(`Bearer ${jwt}`),
  });
  if (!tokenResponse.ok) throw new Error(`Installation token: ${tokenResponse.status} ${await tokenResponse.text()}`);
  const { token } = await tokenResponse.json();

  return async (method, path, body, optional = false) => {
    const response = await fetch(`https://api.github.com${path}`, {
      method,
      headers: headers(`token ${token}`),
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    if (!response.ok) {
      if (optional) return null;
      throw new Error(`${method} ${path}: ${response.status} ${await response.text()}`);
    }
    return response.status === 204 ? null : response.json();
  };
}

function headers(authorization) {
  return {
    authorization,
    accept: "application/vnd.github+json",
    "user-agent": "boardflow-contribution-relay",
    "x-github-api-version": "2022-11-28",
  };
}

/** The app's JWT (RS256, 9 minutes), signed with its private key in PKCS#8 PEM form. */
async function appJwt(appId, pem) {
  const der = Uint8Array.from(atob(pem.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "")), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey("pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const now = Math.floor(Date.now() / 1000);
  const encode = (obj) => base64url(new TextEncoder().encode(JSON.stringify(obj)));
  const unsigned = `${encode({ alg: "RS256", typ: "JWT" })}.${encode({ iat: now - 60, exp: now + 540, iss: String(appId) })}`;
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned));
  return `${unsigned}.${base64url(new Uint8Array(signature))}`;
}

function base64url(bytes) {
  let s = "";
  bytes.forEach((b) => (s += String.fromCharCode(b)));
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
