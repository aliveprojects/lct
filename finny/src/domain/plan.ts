import type { Content } from '../content/types';
import { DIRECTIONS, ECON, coinsText, roundDown5, roundUp5, sum } from './econ';
import { DIRECTION_LABEL, freeCoins } from './profile';
import type { Direction, Outcome, Plan, Profile } from './types';
import { clone, done, fail, isInt } from './util';

const DIR_WHY: Record<Direction, string> = {
  must: 'На еду и уход за питомцем.',
  want: 'На игрушки и украшения.',
  save: 'Столько ты хочешь отложить на мечту.',
};

export interface NeedsCost {
  food: number;
  care: number;
  total: number;
}

/** Самая дешёвая еда + самый дешёвый уход: минимум на нужное за неделю. */
export function needsCost(content: Content): NeedsCost {
  const cheapest = (need: 'food' | 'care') =>
    Math.min(...content.items.filter((i) => i.kind === 'must' && i.need === need).map((i) => i.price));
  const food = cheapest('food');
  const care = cheapest('care');
  return { food, care, total: food + care };
}

export const planTotal = (plan: Plan): number => sum(DIRECTIONS.map((d) => plan[d]));

export const planRemainder = (available: number, plan: Plan): number => available - planTotal(plan);

export type PlanProblem = 'invalid' | 'empty' | 'over';

/** Проверка плана до подтверждения: сумма не больше доступного бюджета. */
export function validatePlan(available: number, plan: Plan): { ok: boolean; remainder: number; problem?: PlanProblem } {
  const remainder = planRemainder(available, plan);
  if (!DIRECTIONS.every((d) => isInt(plan[d]) && plan[d] >= 0)) return { ok: false, remainder, problem: 'invalid' };
  if (planTotal(plan) === 0) return { ok: false, remainder, problem: 'empty' };
  if (remainder < 0) return { ok: false, remainder, problem: 'over' };
  return { ok: true, remainder };
}

/** Пример разделения для кнопки «Подсказка». Ребёнок может изменить любое число. */
export function suggestPlan(p: Profile, content: Content): Plan {
  const avail = p.wallet;
  const event = p.pendingEventId ? content.events.find((e) => e.id === p.pendingEventId) : undefined;
  const reserve = event ? event.cost : 0;
  let must = Math.max(roundUp5(needsCost(content).total), roundDown5(avail * 0.35));
  let save = roundDown5(avail * 0.3);
  let want = roundDown5(avail * 0.25);
  const room = avail - reserve;
  while (must + save + want > room && want > 0) want -= ECON.planStep;
  while (must + save + want > room && save > 0) save -= ECON.planStep;
  must = Math.min(must, Math.max(0, room - save - want));
  return { must, want, save };
}

export function confirmPlan(p0: Profile, plan: Plan): Outcome {
  if (p0.period.planConfirmed) return fail('PLAN_ALREADY_CONFIRMED', 'План этой недели уже подтверждён.');
  const check = validatePlan(p0.wallet, plan);
  if (check.problem === 'over')
    return fail('PLAN_OVER_BUDGET', `В плане на ${coinsText(-check.remainder)} больше, чем у тебя есть.`, { overBy: -check.remainder });
  if (!check.ok) return fail('PLAN_INVALID', 'Раздели хотя бы несколько монет по конвертам.');

  const p = clone(p0);
  p.period.plan = { must: plan.must, want: plan.want, save: plan.save };
  p.period.planConfirmed = true;
  p.period.startBalance = p.wallet;

  const changes = DIRECTIONS.map((d) => ({
    kind: 'plan' as const,
    label: DIRECTION_LABEL[d],
    delta: plan[d],
    after: plan[d],
    why: DIR_WHY[d],
  }));
  if (check.remainder > 0)
    changes.push({
      kind: 'plan',
      label: 'Запас',
      delta: check.remainder,
      after: check.remainder,
      why: 'Эти монеты остаются в кошельке — на неожиданный случай.',
    });
  return done(p, {
    title: 'План на неделю готов!',
    tone: 'good',
    changes,
    explain: 'Теперь можно покупать и откладывать. В конце недели я сравню план с тем, что получилось.',
    next: { label: 'В магазин', route: '/shop' },
  });
}

/** Новые монеты (заработанные уже после плана) можно добавить в любой конверт. */
export function topUpPlan(p0: Profile, dir: Direction, amount: number): Outcome {
  if (!p0.period.planConfirmed) return fail('PLAN_REQUIRED', 'Сначала составь план недели.');
  if (!isInt(amount) || amount <= 0) return fail('INVALID_AMOUNT', 'Выбери, сколько монет добавить.');
  const free = freeCoins(p0);
  if (amount > free) return fail('NOT_ENOUGH', `Свободных монет только ${free}.`, { missing: amount - Math.max(0, free) });

  const p = clone(p0);
  p.period.plan[dir] += amount;
  return done(p, {
    title: 'Добавлено в план',
    tone: 'info',
    changes: [
      {
        kind: 'plan',
        label: DIRECTION_LABEL[dir],
        delta: amount,
        before: p0.period.plan[dir],
        after: p.period.plan[dir],
        why: 'Новые монеты ты сам разложил по конвертам.',
      },
    ],
    explain: 'Каждая монета получила своё дело. Так проще не потратить лишнего.',
  });
}
