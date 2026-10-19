# Contribution relay

When a user marks a drafted guide or score sheet as reviewed, or saves a guide in the editor, the
app shares it with every BoardFlow user. It sends the guide or sheet to this Cloudflare Worker, and
the Worker opens a pull request on the repo. There is no share button. The app holds no GitHub
credentials, because anyone could pull them out of the APK. The Worker holds them, as a GitHub App.

What happens:

1. **The app** (`data/contribution/Contributions.kt`) waits until it's online, then posts
   `{kind, gameId, gameName, gameIds, document, appVersion}`.
   - It sends only the guide or sheet, without the author or anything about the user.
   - The same content is never sent twice.
2. **The Worker** opens the pull request.
   - It checks the payload's shape and limits each IP address to 10 submissions an hour.
   - It writes `setup-guides/<gameId>.json` or `score-sheets/<gameId>.json`, and adds the game's
     entry to that folder's `index.json` with the next version number.
   - It opens one pull request per game, on branch `contrib/<kind>-<gameId>`. A newer submission
     for the same game replaces the branch's commit and adds a comment to the open pull request.
3. **CI** (`.github/workflows/content.yml`) checks every guide and sheet in the repo with the
   app's validators.
4. **You** check the pull request against the rulebook and merge it. Every install then downloads
   it from the catalog within a day.

## Setup (once)

1. **Create a GitHub App** (GitHub > Settings > Developer settings > GitHub Apps > New):
   - Homepage URL: the repo. Turn off the webhook.
   - Repository permissions: *Contents: Read and write*, *Pull requests: Read and write*,
     *Issues: Read and write* (for labels and comments). Nothing else.
   - Create it, note the **App ID**, and generate a **private key** (a `.pem` file is downloaded).
2. **Install the app** on `MartinAlexanderKrul/board-flow` only. The number at the end of the
   installation page's URL is the **installation ID**.
3. **Convert the key** to PKCS#8, which is what the Worker reads:

   ```bash
   openssl pkcs8 -topk8 -nocrypt -in boardflow-relay.private-key.pem -out relay-key.pem
   ```

4. **Deploy the Worker** (needs a free Cloudflare account):

   ```bash
   cd relay
   npx wrangler login
   npx wrangler kv namespace create RATE        # put the printed id into wrangler.toml
   npx wrangler secret put GITHUB_APP_ID
   npx wrangler secret put GITHUB_INSTALLATION_ID
   npx wrangler secret put GITHUB_APP_PRIVATE_KEY < relay-key.pem
   npx wrangler deploy
   ```

   Delete both `.pem` files once the secret is stored.
5. **Point the app at it:** put the Worker's URL into `local.properties` as
   `CONTRIBUTION_RELAY_URL=https://boardflow-relay.<you>.workers.dev`, or set it as an environment
   variable, then build. A build without the URL doesn't share anything.
6. Optional: create the labels `contribution`, `setup-guide` and `score-sheet` in the repo.

## Tests

```bash
node --test relay/test/relay.test.mjs
```
