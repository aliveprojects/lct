import { content } from '../src/content';
import { newProfile } from '../src/domain/profile';
import type { Ctx, Outcome, Profile } from '../src/domain/types';

export const ctx = (today = '2026-09-19'): Ctx => ({ content, today });

export const makeProfile = (isDemo = false): Profile =>
  newProfile(
    { playerName: 'Тест', petName: 'Тестик', appearance: { species: 'cat', color: 'peach', accessory: 'none' }, isDemo },
    ctx(),
  );

/** Достаёт успешный результат или падает с понятным сообщением. */
export function must<T extends object>(o: Outcome<T>): Extract<Outcome<T>, { ok: true }> {
  if (!o.ok) throw new Error(`Ожидался успех, а вышла ошибка ${o.error.code}: ${o.error.message}`);
  return o;
}

export const item = (id: string) => {
  const it = content.items.find((i) => i.id === id);
  if (!it) throw new Error(`нет товара ${id}`);
  return it;
};

export const task = (id: string) => {
  const t = content.tasks.find((x) => x.id === id);
  if (!t) throw new Error(`нет задания ${id}`);
  return t;
};
