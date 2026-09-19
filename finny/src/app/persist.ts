import { serializeState } from '../domain/state';
import type { AppState } from '../domain/types';
import { storage } from '../platform/storage';

let timer: ReturnType<typeof setTimeout> | undefined;
let latest: AppState | null = null;

async function write(): Promise<void> {
  if (!latest) return;
  const raw = serializeState(latest);
  latest = null;
  try {
    await storage.save(raw);
  } catch {
    /* прогресс остаётся в памяти; следующая попытка — при следующем изменении */
  }
}

/** Сохраняет с небольшой задержкой, чтобы серия быстрых действий писалась одним разом. */
export function schedulePersist(state: AppState): void {
  latest = state;
  clearTimeout(timer);
  timer = setTimeout(() => void write(), 120);
}

/** Немедленно записывает всё, что не успело сохраниться (сворачивание, закрытие). */
export function flushPersist(): void {
  clearTimeout(timer);
  void write();
}
