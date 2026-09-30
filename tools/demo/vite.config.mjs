// SYNTHETIC local demo only: preserve the app config and use explicit IPv4 loopback.
import originalConfig from "../../shm-frontend-fable5-copy/vite.config.js";

export default {
  ...originalConfig,
  server: {
    ...originalConfig.server,
    host: "127.0.0.1",
    strictPort: true,
    proxy: {
      ...originalConfig.server.proxy,
      "/api": {
        ...originalConfig.server.proxy["/api"],
        target: "http://127.0.0.1:8080",
      },
    },
  },
};
