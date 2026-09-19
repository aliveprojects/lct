import { ECON, coinsText, stageForGrowth, sum, weeksText } from './econ';
import { STAT_LABEL, addTx, applyStat, goalById, newGoal } from './profile';
import type { Change, Ctx, Goal, Outcome, Profile } from './types';
import { clone, done, fail, isInt, walletChange } from './util';

export interface EtaInfo {
  /** Средний взнос за неделю (null — пока не было взносов). */
  avg: number | null;
  /** Сколько недель осталось при таком взносе (null — посчитать нельзя). */
  weeks: number | null;
  remaining: number;
  text: string;
}

/**
 * Средний регулярный взнос: среднее по прошлым неделям, где были взносы.
 * Если завершённых недель ещё нет — берём текущую. Снятия на среднее не влияют.
 */
export function averageDeposit(goal: Goal, currentPeriod: number): number | null {
  const entries = Object.entries(goal.deposits)
    .map(([period, amount]) => ({ period: Number(period), amount }))
    .filter((e) => e.amount > 0);
  const past = entries.filter((e) => e.period < currentPeriod).map((e) => e.amount);
  const pool = past.length ? past : entries.map((e) => e.amount);
  if (!pool.length) return null;
  return Math.max(1, Math.round(sum(pool) / pool.length));
}

export function etaFor(goal: Goal, currentPeriod: number, savedOverride?: number): EtaInfo {
  const saved = savedOverride ?? goal.saved;
  const remaining = Math.max(0, goal.cost - saved);
  const avg = averageDeposit(goal, currentPeriod);
  if (remaining === 0) return { avg, weeks: 0, remaining, text: 'Цель накоплена!' };
  if (!avg) return { avg: null, weeks: null, remaining, text: 'Срок появится, когда ты отложишь первые монеты.' };
  const weeks = Math.ceil(remaining / avg);
  return { avg, weeks, remaining, text: `Примерно ${weeksText(weeks)}: ты откладываешь в среднем ${coinsText(avg)} в неделю.` };
}

/** «Если откладывать по N в неделю» — подсказка без истории. */
export const weeksAt = (remaining: number, perWeek: number): number => (perWeek > 0 ? Math.ceil(remaining / perWeek) : 0);

export function deposit(p0: Profile, goalId: string, amount: number): Outcome {
  if (!p0.period.planConfirmed) return fail('PLAN_REQUIRED', 'Сначала составь план недели — в нём ты решаешь, сколько отложить.');
  const goal = goalById(p0, goalId);
  if (!goal) return fail('GOAL_NOT_FOUND', 'Такой цели нет.');
  if (goal.done) return fail('GOAL_DONE', 'Эта мечта уже исполнена.');
  if (!isInt(amount) || amount <= 0) return fail('INVALID_AMOUNT', 'Выбери, сколько монет отложить.');
  if (amount > p0.wallet) return fail('NOT_ENOUGH', `В кошельке только ${coinsText(p0.wallet)}.`, { missing: amount - p0.wallet });
  const remaining = goal.cost - goal.saved;
  if (amount > remaining) return fail('GOAL_FULL', `До цели осталось всего ${coinsText(remaining)}.`, { max: remaining });

  const p = clone(p0);
  const g = goalById(p, goalId)!;
  const walletBefore = p.wallet;
  const savedBefore = g.saved;
  const periodSavedBefore = p.period.saved;
  p.wallet -= amount;
  g.saved += amount;
  g.deposits[p.period.index] = (g.deposits[p.period.index] ?? 0) + amount;
  p.period.saved += amount;
  addTx(p, { kind: 'save', title: `В копилку: ${g.title}`, amount: -amount, savingsDelta: amount });

  const changes: Change[] = [
    walletChange('Монеты', walletBefore, p.wallet, 'Эти монеты переехали из кошелька в копилку.'),
    { kind: 'savings', label: 'Копилка', before: savedBefore, after: g.saved, delta: amount, why: `Копится на мечту «${g.title}».` },
  ];
  if (periodSavedBefore < ECON.minSaving && p.period.saved >= ECON.minSaving) {
    const r = applyStat(p.pet, 'mood', ECON.firstSavingMood);
    changes.push({ kind: 'stat', stat: 'mood', label: STAT_LABEL.mood, before: r.before, after: r.after, delta: r.gain, why: 'Питомец гордится: ты начал копить на этой неделе.' });
    p.pet.reason = { text: 'Я горжусь тобой: ты копишь на мечту!', stat: 'mood', delta: r.gain };
  } else {
    p.pet.reason = { text: 'Копилка растёт — мечта всё ближе!' };
  }

  const eta = etaFor(g, p.period.index);
  const reached = g.saved >= g.cost;
  return done(p, {
    title: reached ? 'Мечта накоплена!' : 'Отложено!',
    tone: 'good',
    changes,
    explain: reached ? 'Ты накопил всю сумму. Теперь мечту можно исполнить!' : `Осталось накопить ${coinsText(eta.remaining)}. ${eta.text}`,
    next: reached ? { label: 'Исполнить мечту', route: '/goals' } : undefined,
  });
}

export interface WithdrawPreview {
  goalId: string;
  amount: number;
  savedBefore: number;
  savedAfter: number;
  walletBefore: number;
  walletAfter: number;
  remainingBefore: number;
  remainingAfter: number;
  etaBefore: EtaInfo;
  etaAfter: EtaInfo;
}

/** Показывает последствия снятия ДО подтверждения: накопления и срок цели. */
export function previewWithdraw(p: Profile, goalId: string, amount: number): WithdrawPreview | null {
  const g = goalById(p, goalId);
  if (!g) return null;
  const amt = Math.min(Math.max(0, amount), g.saved);
  const savedAfter = g.saved - amt;
  return {
    goalId,
    amount: amt,
    savedBefore: g.saved,
    savedAfter,
    walletBefore: p.wallet,
    walletAfter: p.wallet + amt,
    remainingBefore: g.cost - g.saved,
    remainingAfter: g.cost - savedAfter,
    etaBefore: etaFor(g, p.period.index),
    etaAfter: etaFor(g, p.period.index, savedAfter),
  };
}

export function withdraw(p0: Profile, goalId: string, amount: number, confirmed: boolean): Outcome {
  const goal = goalById(p0, goalId);
  if (!goal) return fail('GOAL_NOT_FOUND', 'Такой цели нет.');
  if (goal.done) return fail('GOAL_DONE', 'Эта мечта уже исполнена.');
  if (!isInt(amount) || amount <= 0) return fail('INVALID_AMOUNT', 'Выбери, сколько монет забрать.');
  if (amount > goal.saved) return fail('NOT_ENOUGH', `В копилке только ${coinsText(goal.saved)}.`, { max: goal.saved });
  if (!confirmed) return fail('CONFIRM_REQUIRED', 'Забрать монеты из копилки можно только после подтверждения.');

  const pv = previewWithdraw(p0, goalId, amount)!;
  const p = clone(p0);
  const g = goalById(p, goalId)!;
  p.wallet += amount;
  g.saved -= amount;
  p.period.withdrawn += amount;
  addTx(p, { kind: 'withdraw', title: `Из копилки: ${g.title}`, amount, savingsDelta: -amount });
  p.pet.reason = { text: 'Ты забрал монеты из копилки. Иногда так нужно — главное, чтобы это было осознанно.' };

  const slower = pv.etaBefore.weeks !== null && pv.etaAfter.weeks !== null && pv.etaAfter.weeks > pv.etaBefore.weeks;
  return done(p, {
    title: 'Монеты забраны из копилки',
    tone: 'info',
    changes: [
      { kind: 'savings', label: 'Копилка', before: pv.savedBefore, after: pv.savedAfter, delta: -amount, why: `Цель «${g.title}» теперь дальше на ${coinsText(amount)}.` },
      walletChange('Монеты', pv.walletBefore, pv.walletAfter, 'Монеты вернулись в кошелёк.'),
    ],
    explain: slower
      ? `Срок цели вырос: было ${weeksText(pv.etaBefore.weeks!)}, стало ${weeksText(pv.etaAfter.weeks!)}. Верни монеты в копилку, когда сможешь.`
      : 'Копилка стала меньше, поэтому мечта чуть дальше.',
  });
}

export function completeGoal(p0: Profile, goalId: string): Outcome {
  const goal = goalById(p0, goalId);
  if (!goal) return fail('GOAL_NOT_FOUND', 'Такой цели нет.');
  if (goal.done) return fail('GOAL_DONE', 'Эта мечта уже исполнена.');
  if (goal.saved < goal.cost) return fail('GOAL_NOT_REACHED', `Ещё не хватает ${coinsText(goal.cost - goal.saved)}.`, { missing: goal.cost - goal.saved });

  const p = clone(p0);
  const g = goalById(p, goalId)!;
  const savedBefore = g.saved;
  g.saved -= g.cost;
  g.done = true;
  p.trophies.push(g.id);
  const growthBefore = p.pet.growth;
  p.pet.growth += ECON.goalGrowth;
  const stageBefore = p.pet.stage;
  p.pet.stage = stageForGrowth(p.pet.growth);
  const mood = applyStat(p.pet, 'mood', ECON.goalMood);
  addTx(p, { kind: 'goal', title: `Мечта исполнена: ${g.title}`, amount: 0, savingsDelta: -g.cost });
  p.activeGoalId = p.goals.find((x) => !x.done)?.id ?? null;
  p.pet.reason = { text: `Ура! Мечта «${g.title}» исполнена — ты накопил её сам!`, stat: 'mood', delta: mood.gain };

  const changes: Change[] = [
    { kind: 'savings', label: 'Копилка', before: savedBefore, after: g.saved, delta: -g.cost, why: `Монеты потрачены на мечту «${g.title}».` },
    { kind: 'stat', stat: 'mood', label: STAT_LABEL.mood, before: mood.before, after: mood.after, delta: mood.gain, why: 'Исполнить мечту — большая радость!' },
    { kind: 'growth', label: 'Очки роста', before: growthBefore, after: p.pet.growth, delta: ECON.goalGrowth, why: 'Ты долго и регулярно копил — питомец растёт.' },
  ];
  return done(p, {
    title: 'Мечта исполнена!',
    tone: 'good',
    changes,
    explain: stageBefore !== p.pet.stage ? 'Питомец перешёл на новую стадию. Это результат твоих решений!' : 'Ты выбрал цель, копил и дошёл до конца. Так работают накопления.',
    next: p.activeGoalId ? { label: 'Выбрать новую цель', route: '/goals' } : undefined,
  });
}

export function selectGoal(p0: Profile, goalId: string): Outcome {
  const goal = goalById(p0, goalId);
  if (!goal) return fail('GOAL_NOT_FOUND', 'Такой цели нет.');
  if (goal.done) return fail('GOAL_DONE', 'Эта мечта уже исполнена.');
  const p = clone(p0);
  p.activeGoalId = goalId;
  return done(p, {
    title: `Цель: ${goal.title}`,
    tone: 'info',
    changes: [],
    explain: `Теперь копим на «${goal.title}». Уже накопленные монеты по другим целям никуда не делись.`,
  });
}

export interface CustomGoalInput {
  title: string;
  icon: string;
  cost: number;
}

export function createCustomGoal(p0: Profile, input: CustomGoalInput, ctx: Ctx): Outcome {
  const { min, max, step, limit } = ECON.customGoal;
  const preset = ctx.content.customGoal.presets.find((x) => x.icon === input.icon && x.title === input.title);
  if (!preset) return fail('INVALID_AMOUNT', 'Выбери мечту из списка.');
  if (!isInt(input.cost) || input.cost < min || input.cost > max || input.cost % step !== 0)
    return fail('INVALID_AMOUNT', `Цена мечты — от ${min} до ${max} монет, шаг ${step}.`);
  if (p0.goals.filter((g) => g.custom && !g.done).length >= limit)
    return fail('INVALID_AMOUNT', `Своих мечт может быть не больше ${limit}. Исполни одну из них.`);

  const p = clone(p0);
  const n = p.goals.filter((g) => g.custom).length + 1;
  const g = newGoal(`custom-${n}`, input.title, input.icon, input.cost, true);
  p.goals.push(g);
  p.activeGoalId = g.id;
  return done(p, {
    title: 'Новая мечта!',
    tone: 'good',
    changes: [],
    explain: `«${g.title}» стоит ${coinsText(g.cost)}. Теперь это твоя текущая цель.`,
  });
}

