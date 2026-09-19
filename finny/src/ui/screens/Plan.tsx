import { useState } from 'preact/hooks';
import { present } from '../../app/actions';
import { nav } from '../../app/nav';
import { act, getCtx, useAppState } from '../../app/store';
import { ui } from '../../app/ui';
import { content } from '../../content';
import { DIRECTIONS, ECON, coinsText } from '../../domain/econ';
import { endPeriod } from '../../domain/period';
import { confirmPlan, needsCost, planRemainder, planTotal, suggestPlan, topUpPlan } from '../../domain/plan';
import { DIRECTION_LABEL, envelope, freeCoins } from '../../domain/profile';
import { wishlistItems } from '../../domain/shop';
import type { Direction, Plan, Profile } from '../../domain/types';
import { Button, Coin, DIR_ICON, Meter, Stepper, Tag, Term } from '../components/kit';
import { Screen } from '../components/Screen';
import { Icon } from '../icons/Icon';

const ROW_HINT: Record<Direction, string> = {
  must: 'Еда и уход за питомцем',
  want: 'Игрушки и украшения',
  save: 'Откладываем на мечту',
};

function DistributionBar({ plan, total }: { plan: Plan; total: number }) {
  const rest = Math.max(0, total - planTotal(plan));
  const seg = (dir: Direction | 'rest', v: number) => (v > 0 ? <span key={dir} class={`dist__seg dist__seg--${dir}`} style={{ flexGrow: v }} title={`${dir}`}>{v >= total * 0.12 ? v : ''}</span> : null);
  return (
    <div class="dist" role="img" aria-label={`Нужное ${plan.must}, хочется ${plan.want}, копилка ${plan.save}, запас ${rest}`}>
      {DIRECTIONS.map((d) => seg(d, plan[d]))}
      {seg('rest', rest)}
    </div>
  );
}

function PlanEditor({ p }: { p: Profile }) {
  const ctx = getCtx();
  const last = p.history[p.history.length - 1];
  const [plan, setPlan] = useState<Plan>({ must: 0, want: 0, save: 0 });
  const total = p.wallet;
  const remainder = planRemainder(total, plan);
  const needs = needsCost(content);
  const wish = wishlistItems(p, ctx);
  const event = p.pendingEventId ? content.events.find((e) => e.id === p.pendingEventId) : undefined;
  const set = (d: Direction, v: number) => setPlan((cur) => ({ ...cur, [d]: v }));

  const confirm = async () => {
    const lowMust = plan.must < needs.total;
    const ok = await ui.confirm({
      title: 'Подтвердить план?',
      icon: 'clip',
      body: (
        <div class="stack">
          <ul class="summary">
            {DIRECTIONS.map((d) => (
              <li key={d}>
                <Icon name={DIR_ICON[d]} size={28} />
                <span>{DIRECTION_LABEL[d]}</span>
                <b>{plan[d]}</b>
              </li>
            ))}
            {remainder > 0 && (
              <li>
                <Icon name="coin" size={28} />
                <span>Запас</span>
                <b>{remainder}</b>
              </li>
            )}
          </ul>
          {remainder > 0 && <p>Монеты в запасе останутся в кошельке — пригодятся, если случится неожиданное.</p>}
          {lowMust && <p class="note">На еду и уход обычно нужно около {coinsText(needs.total)}. Можно подтвердить и так — но питомцу может не хватить.</p>}
          <p class="muted">После подтверждения план менять нельзя — я сравню его с тем, что получилось.</p>
        </div>
      ),
      confirmLabel: 'Подтвердить',
      cancelLabel: 'Изменить',
      tone: lowMust ? 'careful' : 'normal',
    });
    if (ok) await present(act((pp) => confirmPlan(pp, plan)));
  };

  return (
    <Screen
      title={`План: неделя ${p.period.index}`}
      hint="Раздели свои монеты на три части. Что-то можно оставить в запасе."
      footer={
        <Button block disabled={planTotal(plan) === 0} onClick={() => void confirm()}>
          Подтвердить план
        </Button>
      }
    >
      <div class="card card--total">
        <span>Всего у тебя</span>
        <Coin n={total} big />
      </div>

      <DistributionBar plan={plan} total={total} />
      <p class={`remainder ${remainder === 0 ? 'is-zero' : ''}`} aria-live="polite">
        {remainder === 0 ? 'Всё разделено!' : <>Осталось разделить: <b>{remainder}</b></>}
      </p>

      {DIRECTIONS.map((d) => (
        <div key={d} class={`envrow envrow--${d}`}>
          <div class="envrow__head">
            <Icon name={DIR_ICON[d]} size={40} />
            <div>
              <b>{d === 'must' ? <Term id="must">Нужное</Term> : d === 'want' ? <Term id="want">Хочется</Term> : <Term id="savings">Копилка</Term>}</b>
              <small>{ROW_HINT[d]}</small>
            </div>
          </div>
          <Stepper label={DIRECTION_LABEL[d]} value={plan[d]} max={plan[d] + remainder} step={ECON.planStep} onChange={(v) => set(d, v)} />
        </div>
      ))}

      <div class="stack">
        {plan.must < needs.total && plan.must > 0 && (
          <p class="note"><Icon name="info" size={22} /> На еду и уход обычно нужно около {coinsText(needs.total)}.</p>
        )}
        {event && <p class="note"><Icon name="vet" size={22} /> Есть непредвиденная трата: {coinsText(event.cost)}. Оставь немного в запасе.</p>}
        {wish.length > 0 && (
          <p class="note"><Icon name="heart" size={22} /> В списке желаний: {wish.map((w) => `${w.name} (${w.price})`).join(', ')}. Можно заложить на «Хочется».</p>
        )}
      </div>

      <div class="row row--wrap">
        <Button variant="secondary" icon="bulb" onClick={() => setPlan(suggestPlan(p, content))}>Подсказка</Button>
        {last && planTotal(last.plan) <= total && (
          <Button variant="secondary" icon="refresh" onClick={() => setPlan({ ...last.plan })}>Как в прошлый раз</Button>
        )}
      </div>
    </Screen>
  );
}

const STATUS: Record<string, string> = {
  none: 'Ещё не начал',
  going: 'В процессе',
  done: 'Выполнено',
  over: 'Больше плана',
};

function PlanTracker({ p }: { p: Profile }) {
  const free = freeCoins(p);
  const ctx = getCtx();
  const wish = wishlistItems(p, ctx);
  const topUp = async (d: Direction) => {
    await present(act((pp) => topUpPlan(pp, d, Math.min(ECON.planStep, freeCoins(pp)))));
  };
  const finish = async () => {
    const miss: string[] = [];
    if (!p.period.needs.food) miss.push('еду');
    if (!p.period.needs.care) miss.push('уход');
    const ok = await ui.confirm({
      title: `Завершить неделю ${p.period.index}?`,
      icon: 'trophy',
      body: (
        <div class="stack">
          {miss.length > 0 && <p class="note"><Icon name="info" size={22} /> Питомец ещё не получил {miss.join(' и ')}. Хочешь сначала купить?</p>}
          <p>Я сравню план и то, что получилось, посчитаю рост питомца и начну новую неделю. Остаток монет останется у тебя.</p>
        </div>
      ),
      confirmLabel: 'Завершить неделю',
      cancelLabel: miss.length ? 'Вернуться' : 'Ещё не всё',
      tone: 'careful',
    });
    if (!ok) return;
    const r = act((pp, c) => endPeriod(pp, c));
    if (await present(r)) nav.replace('/review');
  };

  return (
    <Screen
      title={`Неделя ${p.period.index}: план и факт`}
      hint="Смотри, сколько ты запланировал и сколько уже потратил."
      footer={<Button block variant="gold" icon="trophy" onClick={() => void finish()}>Завершить неделю</Button>}
    >
      <div class="card card--total">
        <span>В кошельке</span>
        <Coin n={p.wallet} big />
      </div>

      {DIRECTIONS.map((d) => {
        const e = envelope(p, d);
        const state = e.over > 0 ? 'over' : e.used === 0 ? 'none' : e.used >= e.planned ? 'done' : 'going';
        return (
          <div key={d} class={`envcard envcard--${d}`}>
            <div class="envcard__head">
              <Tag dir={d} />
              <span class={`status status--${state}`}>
                <Icon name={state === 'over' ? 'info' : state === 'done' ? 'check' : 'sparkle'} size={18} />
                {STATUS[state]}
              </span>
            </div>
            <div class="envcard__nums">
              <span>План <b>{e.planned}</b></span>
              <span>{d === 'save' ? 'Отложено' : 'Потрачено'} <b>{e.used}</b></span>
              <span>Осталось <b>{e.left}</b></span>
            </div>
            <Meter value={Math.min(e.used, e.planned)} max={Math.max(1, e.planned)} label={`${DIRECTION_LABEL[d]}: план и факт`} kind={d} showNumbers={false} />
            {e.over > 0 && <small class="muted">Больше плана на {e.over}. Это нормально — в итогах я подскажу, как сделать лучше.</small>}
          </div>
        );
      })}

      {free > 0 ? (
        <div class="card card--tint-gold">
          <b>Свободные монеты: {free}</b>
          <p class="muted">Их можно добавить в любой конверт — по {ECON.planStep} монет.</p>
          <div class="row row--wrap">
            {DIRECTIONS.map((d) => (
              <Button key={d} small variant="secondary" icon={DIR_ICON[d]} onClick={() => void topUp(d)}>
                +{Math.min(ECON.planStep, free)} {DIRECTION_LABEL[d]}
              </Button>
            ))}
          </div>
        </div>
      ) : free < 0 ? (
        <div class="card card--tint-warn">
          <b>Для плана не хватает {-free}</b>
          <p>Ты потратил больше запланированного. Можно заработать монеты в заданиях или в конце недели составить план точнее.</p>
          <Button small variant="secondary" onClick={() => nav.go('/tasks')}>К заданиям</Button>
        </div>
      ) : null}

      {wish.length > 0 && (
        <div class="card">
          <b>Список желаний</b>
          <ul class="plain">
            {wish.map((w) => (
              <li key={w.id}>{w.name} — {coinsText(w.price)}</li>
            ))}
          </ul>
        </div>
      )}
    </Screen>
  );
}

export function PlanScreen() {
  const app = useAppState();
  const p = (app.mode === 'demo' ? app.demo : app.main)!;
  return p.period.planConfirmed ? <PlanTracker p={p} /> : <PlanEditor p={p} />;
}
