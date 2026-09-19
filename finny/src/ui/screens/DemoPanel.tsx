import { present } from '../../app/actions';
import { nav } from '../../app/nav';
import { getCtx, getProfile, getState, mutate, setState } from '../../app/store';
import { ui } from '../../app/ui';
import { content } from '../../content';
import { playPeriodAuto } from '../../domain/demo';
import { advanceDemoDay, resetDemoProfile, setMode, withProfile } from '../../domain/state';
import { Button } from '../components/kit';
import { Icon } from '../icons/Icon';

/** Панель проверки: помогает экспертам пройти обязательный сценарий без ожидания календарных сроков. */
export function openDemoPanel(): void {
  void ui.sheet(
    (close) => {
      const p = getProfile();
      const stage = p ? content.pet.stages.find((s) => s.id === p.pet.stage)?.name : '';
      return (
        <div class="stack">
          <div class="card card--tint-gold">
            <b><Icon name="key" size={22} /> Тестовый профиль</b>
            <p>Хранится отдельно от игрового. {p ? `Сейчас: неделя ${p.period.index}, ${stage}, очков роста ${p.pet.growth}.` : ''}</p>
            <p class="muted">Все задания открыты сразу. Недели переключаются без ожидания.</p>
          </div>

          <Button block variant="secondary" icon="gift" onClick={() => { mutate(advanceDemoDay); ui.toast('Наступил следующий день: подарок дня снова доступен'); close(); }}>
            Пропустить день (подарок дня)
          </Button>
          <Button
            block
            variant="secondary"
            icon="sparkle"
            onClick={async () => {
              const cur = getProfile();
              if (!cur) return;
              close();
              const after = playPeriodAuto(cur, getCtx());
              setState(withProfile(getState(), after));
              const last = after.history[after.history.length - 1];
              if (last) await present({ ok: true, profile: after, report: { title: `Неделя ${last.index} сыграна`, tone: 'good', changes: [], explain: `${last.summary} Так бы выглядела разумная неделя: план, задания, покупки, копилка.`, next: { label: 'Смотреть итоги', route: '/review' } } });
            }}
          >
            Сыграть неделю автоматически
          </Button>
          <Button block variant="secondary" icon="trophy" onClick={() => { close(); nav.go('/review'); }}>Итоги последней недели</Button>
          <hr class="sep" />
          <Button
            block
            variant="gold"
            icon="refresh"
            onClick={async () => {
              const ok = await ui.confirm({ title: 'Сбросить тестовый профиль?', icon: 'refresh', tone: 'careful', body: <p>Тестовый профиль вернётся к исходному состоянию, и можно пройти сценарий заново. Игровой профиль не изменится.</p>, confirmLabel: 'Сбросить', cancelLabel: 'Отмена' });
              if (!ok) return;
              mutate(resetDemoProfile);
              close();
              nav.reset('/intro');
            }}
          >
            Сбросить тестовый профиль
          </Button>
          <Button
            block
            variant="secondary"
            onClick={() => {
              mutate((s) => setMode(s, 'normal'));
              close();
              nav.reset('/');
            }}
          >
            Выйти из демо-режима
          </Button>
          <Button block variant="ghost" onClick={close}>Закрыть</Button>
        </div>
      );
    },
    'Демо-режим',
  );
}
