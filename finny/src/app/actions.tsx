import { nav } from './nav';
import { ui } from './ui';
import { Button } from '../ui/components/kit';
import type { GameError, Outcome } from '../domain/types';
import { playSound } from '../platform/sound';

/**
 * Показывает результат действия движка. Успех → окно «что изменилось и почему».
 * Ошибка → понятное объяснение и варианты, а не тупик. Возвращает true, если действие удалось.
 */
export async function present<T extends object>(r: Outcome<T>): Promise<boolean> {
  if (!r.ok) {
    await explainError(r.error);
    return false;
  }
  const gainedCoins = r.report.changes.some((c) => c.kind === 'wallet' && c.delta > 0);
  playSound(r.report.changes.some((c) => c.kind === 'growth' && c.delta > 0) ? 'level' : gainedCoins ? 'coin' : r.report.tone === 'good' ? 'good' : 'tap');
  await ui.report(r.report);
  return true;
}

export async function explainError(e: GameError): Promise<void> {
  playSound('oops');
  if (e.code === 'PLAN_REQUIRED') {
    const go = await ui.confirm({
      title: 'Сначала — план недели',
      body: <p>{e.message}</p>,
      confirmLabel: 'Составить план',
      cancelLabel: 'Позже',
      icon: 'clip',
    });
    if (go) nav.go('/plan');
    return;
  }
  await ui.sheet(
    (close) => (
      <div class="stack">
        <p class="lead">{e.message}</p>
        {e.options && e.options.length > 0 && (
          <div class="stack">
            <b>Что можно сделать:</b>
            {e.options.map((o) => (
              <div class="card card--option" key={o.id}>
                <b>{o.label}</b>
                <span class="muted">{o.hint}</span>
                {o.route && (
                  <Button small variant="secondary" onClick={() => { close(); nav.go(o.route!); }}>
                    Перейти
                  </Button>
                )}
              </div>
            ))}
          </div>
        )}
        <Button block onClick={close}>Понятно</Button>
      </div>
    ),
    'Так пока нельзя',
  );
}
