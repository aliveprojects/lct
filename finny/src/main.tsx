import '@fontsource-variable/rubik/index.css';
import { render } from 'preact';
import { App } from './app/App';
import { flushPersist, schedulePersist } from './app/persist';
import { initStore } from './app/store';
import { content, validateContent } from './content';
import { parseState } from './domain/state';
import { setupNative } from './platform/native';
import { initMusic } from './platform/music';
import { setSoundEnabled } from './platform/sound';
import { storage } from './platform/storage';
import './styles/app.css';

async function boot(): Promise<void> {
  if (import.meta.env.DEV) {
    const problems = validateContent(content);
    if (problems.length) console.warn('Проблемы с контентом:', problems);
  }
  const raw = await storage.load().catch(() => null);
  const { state, recovered } = parseState(raw);
  if (recovered && raw) await storage.backup(raw).catch(() => undefined);
  initStore(state, schedulePersist);
  setSoundEnabled(state.settings.sound);
  initMusic(state.settings.music);
  setupNative(flushPersist);
  render(<App />, document.getElementById('app')!);
}

boot().catch((e) => {
  console.error(e);
  const el = document.getElementById('app');
  if (el) el.textContent = 'Не получилось запустить приложение. Закройте и откройте его снова — прогресс сохранён.';
});
