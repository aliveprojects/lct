import { App } from '@capacitor/app';
import { Capacitor } from '@capacitor/core';
import { nav, canGoBack } from '../app/nav';
import { ui } from '../app/ui';
import { setMusicBackgrounded } from './music';

/** Подключает системные события Android: кнопка «назад» и сохранение при сворачивании. */
export function setupNative(flush: () => void): void {
  document.addEventListener('visibilitychange', () => {
    setMusicBackgrounded(document.visibilityState === 'hidden');
    if (document.visibilityState === 'hidden') flush();
  });
  window.addEventListener('pagehide', flush);

  if (!Capacitor.isNativePlatform()) return;
  void App.addListener('pause', () => {
    setMusicBackgrounded(true);
    flush();
  });
  void App.addListener('resume', () => setMusicBackgrounded(false));
  void App.addListener('backButton', () => {
    if (ui.closeTop()) return;
    if (canGoBack()) {
      nav.back();
      return;
    }
    flush();
    void App.exitApp();
  });
}
