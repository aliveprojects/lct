import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'app.finni.kids',
  appName: 'Финни',
  webDir: 'dist',
  plugins: {
    SystemBars: {
      insetsHandling: 'css',
      initialViewportFitValueHint: 'cover',
      style: 'DARK', // светлые значки на тёмном фоне
    },
  },
  android: {
    allowMixedContent: false,
    // Отладка WebView включается ТОЛЬКО для тестовой сборки: FINNI_WEBVIEW_DEBUG=1 (см. scripts/device-smoke.mjs). В релизе — всегда выключена.
    webContentsDebuggingEnabled: process.env.FINNI_WEBVIEW_DEBUG === '1',
    backgroundColor: '#0A0D35',
  },
};

export default config;
