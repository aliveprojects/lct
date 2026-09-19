import { afterAll, describe, expect, it } from 'vitest';
import { content } from '../src/content';
import { ECON, stageForGrowth, sum } from '../src/domain/econ';
import { ADULT_BONUS_REASONS, claimDaily, grantAdultBonus } from '../src/domain/income';
import { confirmPlan, topUpPlan } from '../src/domain/plan';
import { endPeriod, resolveEvent } from '../src/domain/period';
import { completeGoal, createCustomGoal, deposit, selectGoal, withdraw } from '../src/domain/savings';
import { buy, toggleWishlist } from '../src/domain/shop';
import { solveTask, submitTask, worstAnswer } from '../src/domain/tasks';
import type { Outcome, Profile } from '../src/domain/types';
import { ctx, makeProfile } from './helpers';

/** Детерминированный генератор (mulberry32): тест воспроизводим. */
function rng(seed: number) {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function invariants(p: Profile, prevGrowth: number): void {
  expect(Number.isInteger(p.wallet)).toBe(true);
  expect(p.wallet).toBeGreaterThanOrEqual(0);
  for (const g of p.goals) {
    expect(g.saved).toBeGreaterThanOrEqual(0);
    expect(g.saved).toBeLessThanOrEqual(g.cost);
  }
  for (const v of Object.values(p.pet.stats)) {
    expect(v).toBeGreaterThanOrEqual(0);
    expect(v).toBeLessThanOrEqual(ECON.statMax);
  }
  expect(p.pet.growth).toBeGreaterThanOrEqual(prevGrowth);
  expect(p.pet.stage).toBe(stageForGrowth(p.pet.growth));
  // история монет: баланс = сумма всех изменений, каждая запись согласована с предыдущей
  let bal = 0;
  for (const tx of p.ledger) {
    bal += tx.amount;
    expect(tx.balance).toBe(bal);
    expect(tx.title.length).toBeGreaterThan(0);
  }
  expect(bal).toBe(p.wallet);
  // деньги не появляются из воздуха: то, что лежит в копилках и потрачено на мечты + кошелёк = все доходы − покупки
  const income = sum(p.ledger.filter((t) => ['start', 'allowance', 'daily', 'task', 'bonus'].includes(t.kind)).map((t) => t.amount));
  const spent = -sum(p.ledger.filter((t) => t.kind === 'purchase' || t.kind === 'event').map((t) => t.amount));
  const goalsDone = sum(p.goals.filter((g) => g.done).map((g) => g.cost));
  const savings = sum(p.goals.map((g) => g.saved));
  expect(p.wallet + savings + goalsDone).toBe(income - spent);
  // показатели периода не отрицательны
  const per = p.period;
  for (const v of [per.spent.must, per.spent.want, per.spent.event, per.saved, per.withdrawn, per.earned, per.plan.must, per.plan.want, per.plan.save]) expect(v).toBeGreaterThanOrEqual(0);
}

describe('Случайные последовательности действий не ломают экономику', () => {
  const okByKind = new Map<number, number>();
  const items = content.items;
  const tasks = content.tasks;

  for (let seed = 1; seed <= 150; seed++) {
    it(`seed ${seed}: 120 случайных действий`, () => {
      const r = rng(seed);
      const pick = <T,>(xs: T[]): T => xs[Math.floor(r() * xs.length)];
      const int = (lo: number, hi: number) => lo + Math.floor(r() * (hi - lo + 1));
      let p = makeProfile(r() < 0.5);
      let day = 19;

      for (let step = 0; step < 120; step++) {
        const before = JSON.stringify(p);
        const prevGrowth = p.pet.growth;
        const c = ctx(`2026-09-${String(1 + (day % 28)).padStart(2, '0')}`);
        const goal = pick(p.goals);
        const kind = int(0, 13);
        let out: Outcome;
        switch (kind) {
          case 0:
            out = claimDaily(p, ctx(`2026-10-${String(1 + (day++ % 28)).padStart(2, '0')}`));
            break;
          case 1:
            out = confirmPlan(p, { must: int(0, 8) * 5, want: int(0, 6) * 5, save: int(0, 6) * 5 });
            break;
          case 2:
            out = topUpPlan(p, pick(['must', 'want', 'save'] as const), int(-1, 6) * 5);
            break;
          case 3:
          case 4:
            out = buy(p, pick(items).id, { confirmOverPlan: r() < 0.5 }, c);
            break;
          case 5:
            out = toggleWishlist(p, pick(items).id, c);
            break;
          case 6:
            out = deposit(p, goal.id, int(-1, 12) * 5);
            break;
          case 7:
            out = withdraw(p, goal.id, int(-1, 8) * 5, r() < 0.7);
            break;
          case 8:
            out = completeGoal(p, goal.id);
            break;
          case 9:
            out = r() < 0.5 ? selectGoal(p, goal.id) : createCustomGoal(p, { title: 'Подарок другу', icon: 'gift', cost: int(1, 21) * 10 }, c);
            break;
          case 10: {
            const t = pick(tasks);
            out = submitTask(p, t.id, r() < 0.6 ? solveTask(t) : worstAnswer(t), c);
            break;
          }
          case 11:
            out = resolveEvent(p, c);
            break;
          case 12:
            out = grantAdultBonus(p, int(0, 5) * 5, ADULT_BONUS_REASONS[0]);
            break;
          default:
            out = r() < 0.6 ? endPeriod(p, c) : claimDaily(p, c);
        }

        if (out.ok) {
          okByKind.set(kind, (okByKind.get(kind) ?? 0) + 1);
          p = out.profile;
          invariants(p, prevGrowth);
          expect(out.report.title.length).toBeGreaterThan(0);
        } else {
          // отклонённое действие ничего не меняет и объясняется человеку
          expect(JSON.stringify(p)).toBe(before);
          expect(out.error.message.length).toBeGreaterThan(0);
        }
      }
    });
  }

  afterAll(() => {
    // тест бесполезен, если почти все действия отклоняются: убеждаемся, что каждый вид действий регулярно проходит
    const n = (k: number) => okByKind.get(k) ?? 0;
    expect(n(0)).toBeGreaterThan(50); // подарок дня
    expect(n(1)).toBeGreaterThan(100); // подтверждение плана
    expect(n(3) + n(4)).toBeGreaterThan(500); // покупки
    expect(n(6)).toBeGreaterThan(300); // взносы
    expect(n(7)).toBeGreaterThan(50); // снятия
    expect(n(8)).toBeGreaterThan(3); // исполнение мечт (в случайной игре это редкое событие)
    expect(n(10)).toBeGreaterThan(500); // задания
    expect(n(13)).toBeGreaterThan(300); // завершение недель
  });
});
