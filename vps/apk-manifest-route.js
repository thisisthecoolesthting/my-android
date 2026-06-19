/**
 * APK Manifest endpoint — drop this into your existing Express/Next.js API or
 * run as a standalone micro-service on the VPS.
 *
 * GET  /api/apk/manifest   → returns latest manifest JSON (public)
 * POST /api/apk/manifest   → updates manifest (requires Bearer token)
 *
 * Standalone usage:
 *   node apk-manifest-route.js
 *   # Listens on port 3099 by default (Caddy proxies /api/apk/* → :3099)
 *
 * Environment vars:
 *   VPS_MANIFEST_TOKEN  — shared secret for POST (set in /opt/factory/.env)
 *   MANIFEST_PATH       — where to store the JSON file (default: /opt/factory/apk-manifest.json)
 */

const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.MANIFEST_PORT || 3099;
const TOKEN = process.env.VPS_MANIFEST_TOKEN || '';
const MANIFEST_PATH = process.env.MANIFEST_PATH || '/opt/factory/apk-manifest.json';

// Ensure the manifest file exists
if (!fs.existsSync(MANIFEST_PATH)) {
  fs.writeFileSync(MANIFEST_PATH, JSON.stringify({
    version: '0.0.0',
    versionCode: 0,
    buildAt: new Date().toISOString(),
    apkUrl: '',
    apkSize: 0,
    commitSha: '',
    appName: 'My Android',
    notes: 'No builds yet'
  }, null, 2));
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url, `http://localhost`);

  if (url.pathname !== '/api/apk/manifest') {
    res.writeHead(404);
    return res.end('Not found');
  }

  // CORS headers so the phone app (and any debug browser) can reach it
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Authorization, Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    return res.end();
  }

  // ── GET: return current manifest ──────────────────────────────────────────
  if (req.method === 'GET') {
    const manifest = fs.readFileSync(MANIFEST_PATH, 'utf8');
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(manifest);
  }

  // ── POST: update manifest ─────────────────────────────────────────────────
  if (req.method === 'POST') {
    const authHeader = req.headers['authorization'] || '';
    if (!TOKEN || authHeader !== `Bearer ${TOKEN}`) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ error: 'Unauthorized' }));
    }

    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const incoming = JSON.parse(body);
        const required = ['version', 'versionCode', 'apkUrl'];
        for (const field of required) {
          if (!incoming[field]) {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            return res.end(JSON.stringify({ error: `Missing field: ${field}` }));
          }
        }
        fs.writeFileSync(MANIFEST_PATH, JSON.stringify(incoming, null, 2));
        console.log(`[apk-manifest] Updated → v${incoming.version} (${incoming.commitSha})`);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ ok: true, version: incoming.version }));
      } catch (e) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ error: 'Invalid JSON' }));
      }
    });
    return;
  }

  res.writeHead(405);
  res.end('Method not allowed');
});

server.listen(PORT, () => {
  console.log(`[apk-manifest] Listening on :${PORT}`);
});
