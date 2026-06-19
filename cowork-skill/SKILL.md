# send-to-android

Trigger the "My Android" CI pipeline: build APK and push to phone.

## Triggers
- "build and send that apk to my android"
- "send apk to my android"
- "push build to my phone"
- "deploy apk"
- "ship it to my android"

## What to do

1. Read the GitHub repo name from context (or ask if unknown — it should be in the user's Android project repo).
2. Call the GitHub Actions `workflow_dispatch` API to trigger `build-and-distribute.yml`.
3. Report the run URL back to the user.
4. Optionally: poll the manifest endpoint after ~3 min to confirm the version bumped.

## GitHub Actions trigger

```python
import os, requests

GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN")  # personal access token with workflow:write
REPO = "thisisthecoolesthting/<your-android-repo>"  # update this
WORKFLOW = "build-and-distribute.yml"
BRANCH = "main"
NOTES = ""  # optional build notes

resp = requests.post(
    f"https://api.github.com/repos/{REPO}/actions/workflows/{WORKFLOW}/dispatches",
    headers={
        "Authorization": f"Bearer {GITHUB_TOKEN}",
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28"
    },
    json={"ref": BRANCH, "inputs": {"notes": NOTES}}
)
print(resp.status_code)  # 204 = success
```

## Confirming delivery

After ~3 minutes, check the manifest:

```bash
curl https://rickyscontrolcenter.com/api/apk/manifest
```

The phone will pick it up within 30 seconds of the manifest updating.

## Required secrets / env

| Key | Where |
|-----|-------|
| `GITHUB_TOKEN` | GitHub PAT (workflow:write scope) — store in `.env` |
| `GDRIVE_SA_JSON` | Google service account JSON — GH secret `GDRIVE_SA_JSON` |
| `GDRIVE_FOLDER_ID` | Drive folder ID — GH secret `GDRIVE_FOLDER_ID` |
| `VPS_MANIFEST_TOKEN` | Shared secret — GH secret + VPS `/opt/factory/.env` |
