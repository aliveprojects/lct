import { Capacitor } from '@capacitor/core';
import { Preferences } from '@capacitor/preferences';

const KEY = 'finni.state.v1';
const BACKUP_KEY = 'finni.state.backup';

/** Хранилище прогресса. На Android — SharedPreferences (через Capacitor), в браузере — localStorage. */
export interface Storage {
  load(): Promise<string | null>;
  save(raw: string): Promise<void>;
  /** Сохраняет копию повреждённых данных, чтобы их можно было разобрать вручную. */
  backup(raw: string): Promise<void>;
  /** Удаляет резервную копию повреждённых данных (при удалении профиля не должно остаться ничего). */
  clearBackup(): Promise<void>;
  clear(): Promise<void>;
}

const nativeStorage: Storage = {
  async load() {
    return (await Preferences.get({ key: KEY })).value;
  },
  async save(raw) {
    await Preferences.set({ key: KEY, value: raw });
  },
  async backup(raw) {
    await Preferences.set({ key: BACKUP_KEY, value: raw });
  },
  async clearBackup() {
    await Preferences.remove({ key: BACKUP_KEY });
  },
  async clear() {
    await Preferences.remove({ key: KEY });
    await Preferences.remove({ key: BACKUP_KEY });
  },
};

const safe = <T>(fn: () => T, fallback: T): T => {
  try {
    return fn();
  } catch {
    return fallback;
  }
};

const webStorage: Storage = {
  async load() {
    return safe(() => window.localStorage.getItem(KEY), null);
  },
  async save(raw) {
    safe(() => window.localStorage.setItem(KEY, raw), undefined);
  },
  async backup(raw) {
    safe(() => window.localStorage.setItem(BACKUP_KEY, raw), undefined);
  },
  async clearBackup() {
    safe(() => window.localStorage.removeItem(BACKUP_KEY), undefined);
  },
  async clear() {
    safe(() => {
      window.localStorage.removeItem(KEY);
      window.localStorage.removeItem(BACKUP_KEY);
    }, undefined);
  },
};

export const storage: Storage = Capacitor.isNativePlatform() ? nativeStorage : webStorage;
