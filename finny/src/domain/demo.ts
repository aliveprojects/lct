import { claimDaily } from './income';
import { confirmPlan, suggestPlan } from './plan';
import { endPeriod, resolveEvent } from './period';
import { envelope, goalById } from './profile';
import { completeGoal, deposit, selectGoal } from './savings';
import { buy } from './shop';
import { availableTasks, solveTask, submitTask, taskStars } from './tasks';
import type { Ctx, Outcome, Profile } from './types';

/**
 * Демонстрационный помощник: за один вызов проигрывает разумные решения одной недели —
 * план, задания, покупки нужного и желаемого, взнос в копилку — и завершает период.
 * Использует только публичные действия движка, поэтому подчиняется тем же правилам, что и ребёнок.
 */
export function playPeriodAuto(start: Profile, ctx: Ctx): Profile {
  let p = start;
  const run = (o: Outcome): boolean => {
    if (o.ok) p = o.profile;
    return o.ok;
  };
  const { items } = ctx.content;

  run(claimDaily(p, ctx));
  if (p.pendingEventId) run(resolveEvent(p, ctx));
  if (!p.period.planConfirmed) run(confirmPlan(p, suggestPlan(p, ctx.content)));

  for (const t of availableTasks(p, ctx).filter((x) => taskStars(p, x.id) < 3).slice(0, 2)) run(submitTask(p, t.id, solveTask(t), ctx));

  for (const need of ['food', 'care'] as const) {
    if (p.period.needs[need] > 0) continue;
    const cheapest = items.filter((i) => i.kind === 'must' && i.need === need).sort((a, b) => a.price - b.price);
    for (const it of cheapest) if (run(buy(p, it.id, {}, ctx))) break;
  }

  const wantLeft = envelope(p, 'want').left;
  const wants = items
    .filter((i) => i.kind === 'want' && i.price <= Math.min(wantLeft, p.wallet) && !(i.accessory && p.pet.ownedAccessories.includes(i.accessory)))
    .sort((a, b) => b.price - a.price);
  if (wants[0]) run(buy(p, wants[0].id, {}, ctx));

  if (!p.activeGoalId) {
    const next = p.goals.find((g) => !g.done);
    if (next) run(selectGoal(p, next.id));
  }
  const goal = p.activeGoalId ? goalById(p, p.activeGoalId) : undefined;
  if (goal) {
    const amount = Math.min(envelope(p, 'save').left, p.wallet, goal.cost - goal.saved);
    if (amount > 0) run(deposit(p, goal.id, amount));
    const g2 = goalById(p, goal.id)!;
    if (g2.saved >= g2.cost) run(completeGoal(p, g2.id));
  }

  run(endPeriod(p, ctx));
  return p;
}
