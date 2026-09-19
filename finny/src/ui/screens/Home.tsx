import { present } from '../../app/actions';
import { nav } from '../../app/nav';
import { act, getCtx, useAppState } from '../../app/store';
import { content } from '../../content';
import { coinsText, growthToNext } from '../../domain/econ';
import { canClaimDaily, claimDaily } from '../../domain/income';
import { activeGoal, petExpression, savingsTotal, STAT_LABEL } from '../../domain/profile';
import { resolveEvent } from '../../domain/period';
import { nextActiveTask } from '../../domain/tasks';
import { STATS } from '../../domain/econ';
import { Button, IconButton, Meter, STAT_ICON, STAT_SHORT } from '../components/kit';
import { Icon } from '../icons/Icon';
import { PetOf } from '../pet/PetView';
import { openHelp } from './Help';
import { openLedger } from './Ledger';
import { openSettings } from './Settings';
import { openDemoPanel } from './DemoPanel';
import { openPetFeelings } from './PetFeelings';

const NAV: { route: string; icon: string; label: string; aria: string }[] = [
  { route: '/plan', icon: 'clip', label: 'План', aria: 'План личного бюджета' },
  { route: '/tasks', icon: 'star', label: 'Задания', aria: 'Задания' },
  { route: '/shop', icon: 'bag', label: 'Магазин', aria: 'Покупки' },
  { route: '/goals', icon: 'jar', label: 'Копилка', aria: 'Накопления и цели' },
  { route: '/progress', icon: 'trophy', label: 'Успехи', aria: 'Прогресс питомца' },
];

export function Home() {
  const app = useAppState();
  const p = (app.mode === 'demo' ? app.demo : app.main)!;
  const ctx = getCtx();
  const expr = petExpression(p.pet.stats);
  const goal = activeGoal(p);
  const task = nextActiveTask(p, ctx);
  const gift = canClaimDaily(p, ctx);
  const event = p.pendingEventId ? content.events.find((e) => e.id === p.pendingEventId) : undefined;
  const stage = content.pet.stages.find((s) => s.id === p.pet.stage)!;
  const toNext = growthToNext(p.pet.growth);
  const bubble = p.pet.reason?.text ?? expr.hint;
  const needPlan = !p.period.planConfirmed;

  return (
    <main class="screen home">
      <header class="home__top">
        <button type="button" class="chip chip--coin" onClick={() => openLedger()} aria-label={`Баланс: ${coinsText(p.wallet)}. Открыть историю`}>
          <Icon name="coin" size={28} />
          <b>{p.wallet}</b>
        </button>
        <button type="button" class="chip chip--save" onClick={() => nav.go('/goals')} aria-label={`Накопления: ${coinsText(savingsTotal(p))}`}>
          <Icon name="jar" size={28} />
          <b>{savingsTotal(p)}</b>
        </button>
        <span class="spacer" />
        <IconButton icon="lock" label="Раздел для взрослого" onClick={() => nav.go('/adult')} />
        <IconButton icon="settings" label="Настройки" onClick={() => openSettings()} />
        <IconButton icon="help" label="Подсказка: как играть" onClick={() => void openHelp()} />
      </header>

      {app.mode === 'demo' && (
        <button type="button" class="demobar" onClick={() => openDemoPanel()}>
          <Icon name="key" size={22} />
          <span>Демо-режим · неделя {p.period.index}</span>
          <b>Панель проверки</b>
        </button>
      )}

      {(gift || event) && (
        <div class="banners">
          {event && (
            <button type="button" class="banner banner--care" onClick={async () => { await present(act((pp, c) => resolveEvent(pp, c))); }}>
              <Icon name="vet" size={34} />
              <span>
                <b>{event.title}</b>
                <small>{event.short}</small>
              </span>
              <em>Помочь</em>
            </button>
          )}
          {gift && (
            <button type="button" class="banner banner--gift" onClick={async () => { await present(act((pp, c) => claimDaily(pp, c))); }}>
              <Icon name="gift" size={34} />
              <span>
                <b>Подарок дня</b>
                <small>+5 монет</small>
              </span>
              <em>Забрать</em>
            </button>
          )}
        </div>
      )}

      <section class="home__pet" aria-label="Питомец">
        <button type="button" class="bubble" onClick={() => openPetFeelings()} aria-label="Почему питомец так себя чувствует">
          <span class="bubble__text">{bubble}</span>
        </button>
        <div class="home__petart">
          <PetOf profile={p} uid="home" />
        </div>
        <div class="petname">
          <b>{p.pet.name}</b>
          <span class="petname__stage">{stage.name}</span>
          <span class={`mood mood--${expr.code}`}>{expr.label}</span>
        </div>
      </section>

      <section class="statrow" aria-label="Состояние питомца">
        {STATS.map((s) => (
          <div class="stat" key={s}>
            <div class="stat__head">
              <Icon name={STAT_ICON[s]} size={22} />
              <span>{STAT_SHORT[s]}</span>
            </div>
            <Meter value={p.pet.stats[s]} label={STAT_LABEL[s]} kind={p.pet.stats[s] < 40 ? 'low' : 'ok'} text={String(p.pet.stats[s])} />
          </div>
        ))}
      </section>

      {goal ? (
        <button type="button" class="goalcard" onClick={() => nav.go('/goals')}>
          <Icon name={goal.icon} size={40} />
          <span class="goalcard__body">
            <b>Мечта: {goal.title}</b>
            <Meter value={goal.saved} max={goal.cost} label={`Накоплено на «${goal.title}»`} kind="save" text={`${goal.saved} из ${goal.cost}`} />
          </span>
        </button>
      ) : (
        <button type="button" class="goalcard" onClick={() => nav.go('/goals')}>
          <Icon name="star" size={40} />
          <span class="goalcard__body">
            <b>Выбери новую мечту</b>
            <small>Нажми, чтобы выбрать цель для копилки</small>
          </span>
        </button>
      )}

      {needPlan ? (
        <div class="taskcard taskcard--plan">
          <Icon name="clip" size={40} />
          <div class="taskcard__body">
            <b>План недели {p.period.index}</b>
            <small>Раздели {coinsText(p.wallet)}</small>
          </div>
          <Button small class="pulse" onClick={() => nav.go('/plan')}>К плану</Button>
        </div>
      ) : task ? (
        <div class="taskcard">
          <Icon name="star" size={40} />
          <div class="taskcard__body">
            <b>Задание: {task.title}</b>
            <small>до {coinsText(task.rewards['3'])}</small>
          </div>
          <Button small onClick={() => nav.go(`/task/${task.id}`)}>Играть</Button>
        </div>
      ) : (
        <div class="taskcard">
          <Icon name="trophy" size={40} />
          <div class="taskcard__body">
            <b>Все задания освоены!</b>
            <small>Повтори любое для практики</small>
          </div>
          <Button small variant="secondary" onClick={() => nav.go('/tasks')}>Задания</Button>
        </div>
      )}

      <nav class="tabs" aria-label="Разделы">
        {NAV.map((n) => (
          <button key={n.route} type="button" class="tab" aria-label={n.aria} onClick={() => nav.go(n.route)}>
            <Icon name={n.icon} size={34} />
            <span>{n.label}</span>
            {n.route === '/plan' && needPlan && <i class="tab__dot" aria-hidden="true" />}
          </button>
        ))}
      </nav>
      <span class="sr-only">
        До следующей стадии {toNext === null ? 'больше нет' : `осталось ${toNext}`} очков роста.
      </span>
    </main>
  );
}

