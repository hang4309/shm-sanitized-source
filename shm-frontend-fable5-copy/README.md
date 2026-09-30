# SHM frontend

Vue 3 / Vite frontend for the unified sensor-reading application. The active navigation includes six monitoring views; prediction and crack pages remain placeholders.

Start with the [repository overview](../README.md), [full application runbook](../docs/runbook.md) and [verification scope](../docs/validation.md).

## Development

Node `^20.19.0 || >=22.12.0` is required by `package.json`.

```sh
npm ci
npm run dev -- --config ../tools/demo/vite.config.mjs
```

Run these commands in this directory. The demo override binds the server and API proxy to IPv4 loopback; the original Vite configuration is unchanged. Start the backend and use the SYNTHETIC strain time window from the runbook.

```sh
npm run build
npx --no-install oxlint .
npx --no-install eslint .
```

The last two commands are review checks without auto-fix. The original `npm run lint` scripts do modify fixable files.

All modules display unified primary readings filtered by module mapping. Wavelength is auxiliary. A rendered strain page does not demonstrate a physical strain algorithm or calibration. Static preview is not a production backend proxy/deployment configuration.
