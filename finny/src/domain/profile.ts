import type { Content } from '../content/types';
import { DIRECTIONS, ECON, STATS, clamp, stageForGrowth, sum } from './econ';
import type {
  Ctx,
  Direction,
  Goal,
  PeriodState,
  Pet,
  PetAppearance,
  PetStat,
  Plan,
  Profile,
  Tx,
  TxKind,
} from './types';

export const DIRECTION_LABEL: Record<Direction, string> = {
  must: 'Нужное',
  want: 'Хочется',
  save: 'Копилка',
};

export const STAT_LABEL: Record<PetStat, string> = {
  satiety: 'Сытость',
  care: 'Чистота и здоровье',
  mood: 'Настроение',
};

export const emptyPlan = (): Plan => ({ must: 0, want: 0, save: 0 });

export function freshPeriod(index: number): PeriodState {
  return {
    index,
    planConfirmed: false,
    startBalance: 0,
    plan: emptyPlan(),
    spent: { must: 0, want: 0, event: 0 },
    needs: { food: 0, care: 0 },
    saved: 0,
    withdrawn: 0,
    earned: 0,
    adultBonus: 0,
  };
}

export function newGoal(id: string, title: string, icon: string, cost: number, custom = false): Goal {
  return { id, title, icon, cost, saved: 0, deposits: {}, custom, done: false };
}

export interface NewProfileInput {
  playerName: string;
  petName: string;
  appearance: PetAppearance;
  isDemo?: boolean;
}

export function newProfile(input: NewProfileInput, ctx: Ctx): Profile {
  const { content, today } = ctx;
  const owned = input.appearance.accessory !== 'none' ? [input.appearance.accessory] : [];
  const pet: Pet = {
    name: input.petName,
    appearance: { ...input.appearance },
    stats: { ...ECON.startStats },
    growth: 0,
    stage: 1,
    ownedAccessories: owned,
    reason: { text: `Привет! Меня зовут ${input.petName}. Давай дружить и учиться вместе!` },
  };
  const profile: Profile = {
    id: `p-${Date.now().toString(36)}`,
    playerName: input.playerName,
    createdAt: today,
    isDemo: !!input.isDemo,
    pet,
    wallet: ECON.startBalance,
    goals: content.goals.map((g) => newGoal(g.id, g.title, g.icon, g.cost)),
    activeGoalId: content.goals[0]?.id ?? null,
    period: freshPeriod(1),
    history: [],
    ledger: [],
    nextTxId: 1,
    tasks: {},
    wishlist: [],
    dailyGiftDate: null,
    pendingEventId: null,
    resolvedEvents: [],
    trophies: [],
  };
  addTx(profile, { kind: 'start', title: 'Стартовый подарок', amount: ECON.startBalance });
  scheduleEvent(profile, content);
  return profile;
}

/** Записывает операцию в историю. Баланс profile.wallet уже должен быть обновлён. */
export function addTx(
  p: Profile,
  tx: { kind: TxKind; title: string; amount: number; savingsDelta?: number; dir?: Tx['dir']; itemId?: string },
): Tx {
  const rec: Tx = { id: p.nextTxId++, period: p.period.index, balance: p.wallet, ...tx };
  p.ledger.push(rec);
  return rec;
}

/** Выставляет непредвиденное событие, если оно назначено на текущий период. */
export function scheduleEvent(p: Profile, content: Content): void {
  if (p.pendingEventId) return;
  const ev = content.events.find((e) => e.period === p.period.index && !p.resolvedEvents.includes(e.id));
  if (ev) p.pendingEventId = ev.id;
}

// ---------- Селекторы ----------

export function usedInDirection(p: Profile, dir: Direction): number {
  const per = p.period;
  if (dir === 'must') return per.spent.must;
  if (dir === 'want') return per.spent.want;
  return Math.max(0, per.saved - per.withdrawn);
}

export interface EnvelopeView {
  dir: Direction;
  planned: number;
  used: number;
  left: number;
  over: number;
}

export function envelope(p: Profile, dir: Direction): EnvelopeView {
  const planned = p.period.plan[dir];
  const used = usedInDirection(p, dir);
  return { dir, planned, used, left: Math.max(0, planned - used), over: Math.max(0, used - planned) };
}

export const envelopes = (p: Profile): EnvelopeView[] => DIRECTIONS.map((d) => envelope(p, d));

/** Монеты, не закреплённые за конвертами (запас). Может быть < 0, если план «не сходится». */
export function freeCoins(p: Profile): number {
  if (!p.period.planConfirmed) return p.wallet;
  return p.wallet - sum(envelopes(p).map((e) => e.left));
}

export const savingsTotal = (p: Profile): number => sum(p.goals.map((g) => g.saved));

export const activeGoal = (p: Profile): Goal | null => p.goals.find((g) => g.id === p.activeGoalId && !g.done) ?? null;

export const goalById = (p: Profile, id: string): Goal | undefined => p.goals.find((g) => g.id === id);

export function applyStat(pet: Pet, stat: PetStat, delta: number): { before: number; after: number; gain: number; wasted: number } {
  const before = pet.stats[stat];
  const after = clamp(before + delta, 0, ECON.statMax);
  pet.stats[stat] = after;
  return { before, after, gain: after - before, wasted: Math.max(0, delta - (after - before)) };
}

export function stageOf(p: Profile) {
  return stageForGrowth(p.pet.growth);
}

export type ExpressionCode = 'happy' | 'content' | 'hungry' | 'dirty' | 'bored';

export interface Expression {
  code: ExpressionCode;
  label: string;
  hint: string;
  need?: PetStat;
}

/** По показателям выбираем выражение мордочки. Всегда есть текст — цвет не единственный признак. */
export function petExpression(stats: Record<PetStat, number>): Expression {
  const low = STATS.filter((s) => stats[s] < ECON.statLow).sort((a, b) => stats[a] - stats[b])[0];
  if (low === 'satiety')
    return { code: 'hungry', label: 'Хочет кушать', hint: 'Я проголодался! Обед и ужин — в магазине, в разделе «Нужное».', need: 'satiety' };
  if (low === 'care')
    return { code: 'dirty', label: 'Хочет помыться', hint: 'Хочу быть чистым! Купание и расчёска — в магазине, в «Нужном».', need: 'care' };
  if (low === 'mood')
    return { code: 'bored', label: 'Скучает', hint: 'Мне скучно. Игрушка из раздела «Хочется» меня развеселит.', need: 'mood' };
  const avg = (stats.satiety + stats.care + stats.mood) / 3;
  return avg >= 80
    ? { code: 'happy', label: 'Счастлив!', hint: 'Мне так хорошо! Спасибо за заботу.' }
    : { code: 'content', label: 'Всё хорошо', hint: 'У меня всё хорошо. Что будем делать?' };
}
