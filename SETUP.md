# My Android — Setup Guide

One-time setup to go from this repo to "say 3 words and your phone installs the build."

---

## Step 1 — Google Cloud (5 min)

1. Go to [console.cloud.google.com](https://console.cloud.google.com) → create or reuse a project.
2. **Enable the Drive API**: APIs & Services → Library → search "Drive API" → Enable.
3. **Create a service account**: IAM & Admin → Service Accounts → Create.
   - Name: `apk-distributor`
   - No roles needed at project level
   - Create a JSON key → download it (you'll use this as `GDRIVE_SA_JSON`)
4. **Create a Drive folder**:
   - Go to drive.google.com → New → Folder → name it "My Android Builds"
   - Right-click the folder → Share → paste the service account email (ends in `@...gserviceaccount.com`) → Editor
   - Copy the folder ID from the URL: `drive.google.com/drive/folders/`**`THIS_PART`**

---

## Step 2 — VPS manifest service (10 min)

SSH into your VPS and run:

```bash
# Copy the service script
cp apps/my-android/vps/apk-manifest-route.js /opt/factory/

# Add the token to .env
echo "VPS_MANIFEST_TOKEN=<generate a random 32-char string>" >> /opt/factory/.env

# Start with PM2
source /opt/factory/.env
pm2 start /opt/factory/apk-manifest-route.js --name apk-manifest \
  --env VPS_MANIFEST_TOKEN=$VPS_MANIFEST_TOKEN

pm2 save
```

Add the Caddy proxy (edit `/etc/caddy/Caddyfile`, find the `rickyscontrolcenter.com` block):

```
handle /api/apk/* {
    reverse_proxy localhost:3099
}
```

Then: `systemctl reload caddy`

Verify: `curl https://rickyscontrolcenter.com/api/apk/manifest`
Should return `{"version":"0.0.0",...}`

---

## Step 3 — GitHub secrets

In your Android repo → Settings → Secrets → Actions, add:

| Secret | Value |
|--------|-------|
| `GDRIVE_SA_JSON` | Full contents of the service account JSON file |
| `GDRIVE_FOLDER_ID` | The folder ID from Step 1 |
| `VPS_MANIFEST_TOKEN` | The token you set in Step 2 |

---

## Step 4 — Build and install the My Android app itself

```bash
# From this directory
cd apps/my-android
./gradlew assembleDebug

# ADB install onto your connected phone
adb install app/build/outputs/apk/debug/app-debug.apk
```

This first install has to be done via ADB or USB. After that, all future builds are OTA via the app.

---

## Step 5 — Enable Unknown Sources on your phone

- Settings → Apps → Special app access → Install unknown apps
- Find "My Android" → Allow from this source

---

## Step 6 — Test the full loop

1. Make a small change in your Android project.
2. Bump `versionCode` in `app/build.gradle.kts` (e.g., 1 → 2).
3. Push to `main` — or say **"build and send that apk to my android"** in Cowork.
4. Watch GitHub Actions run (~3 min).
5. Your phone shows "New build ready — v1.0.1" notification.
6. Tap → Install. Done.

---

## Cowork trigger (the "system rule")

Add `GITHUB_TOKEN` (PAT with `workflow` scope) to your `.env`.
Cowork will pick up phrases like:
- "build and send that apk to my android"
- "push build to my phone"
- "ship it to my android"

…and trigger the workflow automatically via the GitHub API.

---

## File structure recap

```
apps/my-android/
├── app/                          Android source (Kotlin + Compose)
├── .github/workflows/
│   └── build-and-distribute.yml  CI: build → Drive → manifest update
├── vps/
│   ├── apk-manifest-route.js     Tiny Node server for GET/POST /api/apk/manifest
│   ├── caddy-snippet.txt         Paste into Caddyfile
│   └── pm2-manifest.config.js    PM2 config
└── cowork-skill/
    └── SKILL.md                  Cowork natural-language trigger
```
