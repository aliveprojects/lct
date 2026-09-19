import { useState } from 'preact/hooks';
import { present } from '../../app/actions';
import { nav } from '../../app/nav';
import { act, useAppState } from '../../app/store';
import { ui } from '../../app/ui';
import { content } from '../../content';
import { ECON, clamp, coinsText, weeksText } from '../../domain/econ';
import { activeGoal, envelope, savingsTotal } from '../../domain/profile';
import { completeGoal, createCustomGoal, deposit, etaFor, previewWithdraw, selectGoal, weeksAt, withdraw } from '../../domain/savings';
import type { Goal, Profile } from '../../domain/types';
import { Button, Coin, Meter, Stepper } from '../components/kit';
import { Screen } from '../components/Screen';
import { Icon } from '../icons/Icon';

function WithdrawSheet({ goalId, close }: { goalId: string; close: () => void }) {
  const app = useAppState();
  const p = (app.mode === 'demo' ? app.demo : app.main)!;
  const goal = p.goals.find((g) => g.id === goalId)!;
  const [amt, setAmt] = useState(Math.min(goal.saved, ECON.planStep));
  const pv = previewWithdraw(p, goalId, amt)!;
  return (
    <div class="stack">
      <p class="lead">Копилка — это монеты на мечту. Если заберёшь их, мечта станет дальше. Проверь, как всё изменится.</p>
      <Stepper label="Сколько забрать" value={amt} min={1} max={goal.saved} step={ECON.planStep} onChange={setAmt} unit=" монет" />
      <div class="card">
        <div class="kv"><span>Накоплено</span><b>{pv.savedBefore} → {pv.savedAfter}</b></div>
        <div class="kv"><span>До мечты останется</span><b>{pv.remainingBefore} → {pv.remainingAfter}</b></div>
        <div class="kv"><span>Кошелёк</span><b>{pv.walletBefore} → {pv.walletAfter}</b></div>
        <div class="kv">
          <span>Срок</span>
          <b>
            {pv.etaBefore.weeks === null ? 'пока нельзя посчитать' : `${weeksText(pv.etaBefore.weeks)} → ${weeksText(pv.etaAfter.weeks ?? 0)}`}
          </b>
        </div>
      </div>
      <div class="row row--wrap">
        <Button block variant="secondary" onClick={close}>Оставить в копилке</Button>
        <Button
          block
          variant="gold"
          onClick={async () => {
            const r = act((pp) => withdraw(pp, goalId, amt, true));
            close();
            await present(r);
          }}
        >
          Забрать {amt}
        </Button>
      </div>
    </div>
  );
}

function CustomGoalSheet({ close }: { close: () => void }) {
  const { min, max, step } = ECON.customGoal;
  const [pick, setPick] = useState(0);
  const [cost, setCost] = useState(50);
  const presets = content.customGoal.presets;
  return (
    <div class="stack">
      <p class="lead">Выбери мечту и назначь ей цену.</p>
      <div class="picker picker--3">
        {presets.map((pr, i) => (
          <button key={pr.title} type="button" class={`pick ${i === pick ? 'is-on' : ''}`} aria-pressed={i === pick} onClick={() => setPick(i)}>
            <Icon name={pr.icon} size={48} />
            <span>{pr.title}</span>
          </button>
        ))}
      </div>
      <Stepper label="Цена мечты" value={cost} min={min} max={max} step={step} onChange={setCost} unit=" монет" />
      <p class="muted">Цена — от {min} до {max} монет.</p>
      <Button
        block
        onClick={async () => {
          const r = act((pp, c) => createCustomGoal(pp, { title: presets[pick].title, icon: presets[pick].icon, cost }, c));
          close();
          await present(r);
        }}
      >
        Создать мечту
      </Button>
    </div>
  );
}

function ActiveGoal({ p, goal }: { p: Profile; goal: Goal }) {
  const remaining = goal.cost - goal.saved;
  const env = envelope(p, 'save');
  const max = Math.min(p.wallet, remaining);
  const suggested = env.left > 0 ? Math.min(env.left, max) : Math.min(10, max);
  const [raw, setRaw] = useState(0);
  const amount = clamp(raw || suggested, 0, max);
  const eta = etaFor(goal, p.period.index);
  const reached = goal.saved >= goal.cost;
  const pct = Math.round((goal.saved / goal.cost) * 100);

  return (
    <div class="goalhero">
      <div class="goalhero__art">
        <Icon name={goal.icon} size={96} />
      </div>
      <h2 class="h2 center">{goal.title}</h2>
      <p class="center">
        Цена мечты: <Coin n={goal.cost} />
      </p>
      <Meter value={goal.saved} max={goal.cost} label={`Накоплено на «${goal.title}»`} kind="save" text={`${pct}%`} />
      <div class="goalhero__nums">
        <div><span>Накоплено</span><b>{goal.saved}</b></div>
        <div><span>Осталось</span><b>{remaining}</b></div>
        <div><span>Цена</span><b>{goal.cost}</b></div>
      </div>
      <p class="eta"><Icon name="history" size={22} /> {eta.text}</p>
      {!eta.avg && remaining > 0 && (
        <p class="muted">Например: если откладывать по 10 монет в неделю, это {weeksText(weeksAt(remaining, 10))}.</p>
      )}

      {reached ? (
        <Button block variant="gold" icon="trophy" onClick={async () => { await present(act((pp) => completeGoal(pp, goal.id))); }}>
          Исполнить мечту!
        </Button>
      ) : (
        <div class="stack">
          {!p.period.planConfirmed && (
            <p class="note"><Icon name="clip" size={22} /> Откладывать можно после плана недели.</p>
          )}
          <Stepper label="Сколько отложить" value={amount} min={0} max={max} step={ECON.planStep} onChange={setRaw} unit=" монет" />
          {p.period.planConfirmed && env.planned > 0 && <p class="muted center">В плане на копилку: {env.planned}, уже отложено: {env.used}.</p>}
          <Button
            block
            variant="good"
            icon="jar"
            disabled={amount <= 0}
            onClick={async () => {
              const ok = await present(act((pp) => deposit(pp, goal.id, amount)));
              if (ok) setRaw(0);
            }}
          >
            Отложить {amount || ''}
          </Button>
          {!p.period.planConfirmed && <Button variant="secondary" onClick={() => nav.go('/plan')}>Составить план</Button>}
        </div>
      )}

      {goal.saved > 0 && (
        <Button
          block
          variant="secondary"
          icon="refresh"
          onClick={() => void ui.sheet((close) => <WithdrawSheet goalId={goal.id} close={close} />, 'Забрать из копилки')}
        >
          Забрать монеты из копилки
        </Button>
      )}
    </div>
  );
}

export function Goals() {
  const app = useAppState();
  const p = (app.mode === 'demo' ? app.demo : app.main)!;
  const goal = activeGoal(p);
  const others = p.goals.filter((g) => !g.done && g.id !== goal?.id);
  const done = p.goals.filter((g) => g.done);

  return (
    <Screen title="Копилка" hint="Выбери мечту и откладывай на неё монеты.">
      <div class="row shopbar">
        <div class="card card--mini"><span>Кошелёк</span><Coin n={p.wallet} /></div>
        <div class="card card--mini"><span>В копилке</span><Coin n={savingsTotal(p)} /></div>
      </div>

      {goal ? (
        <ActiveGoal p={p} goal={goal} />
      ) : (
        <div class="card card--tint-gold center stack">
          <Icon name="trophy" size={64} />
          <b>Все мечты исполнены! Выбери новую.</b>
        </div>
      )}

      {others.length > 0 && (
        <section class="stack">
          <h2 class="h3">Другие мечты</h2>
          {others.map((g) => (
            <div class="goalrow" key={g.id}>
              <Icon name={g.icon} size={48} />
              <div class="goalrow__body">
                <b>{g.title}</b>
                <Meter value={g.saved} max={g.cost} label={`Накоплено на «${g.title}»`} kind="save" text={`${g.saved} из ${g.cost}`} />
              </div>
              <Button small variant="secondary" onClick={async () => { await present(act((pp) => selectGoal(pp, g.id))); }}>Выбрать</Button>
            </div>
          ))}
        </section>
      )}

      <Button variant="secondary" icon="plus" block onClick={() => void ui.sheet((close) => <CustomGoalSheet close={close} />, 'Своя мечта')}>
        Создать свою мечту
      </Button>

      {done.length > 0 && (
        <section class="stack">
          <h2 class="h3">Исполненные мечты</h2>
          <div class="row row--wrap">
            {done.map((g) => (
              <span class="trophy" key={g.id}>
                <Icon name={g.icon} size={36} /> {g.title}
              </span>
            ))}
          </div>
        </section>
      )}
      <span class="sr-only">{coinsText(savingsTotal(p))} в копилке</span>
    </Screen>
  );
}
