module.exports = {
  apps: [{
    name: 'apk-manifest',
    script: '/opt/factory/apk-manifest-route.js',
    env: {
      VPS_MANIFEST_TOKEN: process.env.VPS_MANIFEST_TOKEN,
      MANIFEST_PATH: '/opt/factory/apk-manifest.json',
      MANIFEST_PORT: '3099'
    },
    watch: false,
    restart_delay: 3000,
    max_restarts: 10
  }]
};
