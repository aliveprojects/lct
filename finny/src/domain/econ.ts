import type { Direction, PetStat, Stage } from './types';

/** Все числа игровой экономики — в одном месте. Они описаны в docs/ECONOMY.md. */
export const ECON = {
  startBalance: 100,
  weeklyAllowance: 70,
  dailyGift: 5,

  statMax: 100,
  /** Ниже этого значения питомцу «не хватает» чего-то — он просит заботы. */
  statLow: 40,
  /** Выше этого значения покупка почти ничего не даёт — подсказываем об этом. */
  statHigh: 85,
  /** Питомец никогда не опускается ниже этого значения: игра не наказывает. */
  statFloor: 25,
  startStats: { satiety: 60, care: 60, mood: 65 } as Record<PetStat, number>,
  /** Насколько показатели снижаются по итогам периода. */
  periodDecay: { satiety: 30, care: 20, mood: 15 } as Record<PetStat, number>,

  planStep: 5,
  minSaving: 5,
  firstSavingMood: 6,
  planKeptMood: 8,
  goalMood: 40,
  goalGrowth: 3,

  /** Очки роста, с которых начинается стадия 1, 2, 3, 4. */
  stageFrom: [0, 8, 20, 36] as number[],

  adultBonusStep: 5,
  adultBonusCap: 20,

  customGoal: { min: 20, max: 200, step: 10, limit: 4 },
  ledgerView: 30,
} as const;

export const DIRECTIONS: Direction[] = ['must', 'want', 'save'];
export const STATS: PetStat[] = ['satiety', 'care', 'mood'];

export const clamp = (n: number, lo: number, hi: number): number => Math.min(hi, Math.max(lo, n));

export const sum = (xs: number[]): number => xs.reduce((a, b) => a + b, 0);

export function stageForGrowth(growth: number): Stage {
  let stage: Stage = 1;
  ECON.stageFrom.forEach((from, i) => {
    if (growth >= from) stage = (i + 1) as Stage;
  });
  return stage;
}

/** Сколько очков роста не хватает до следующей стадии (null — стадия последняя). */
export function growthToNext(growth: number): number | null {
  const stage = stageForGrowth(growth);
  const next = ECON.stageFrom[stage];
  return next === undefined ? null : next - growth;
}

/** Склонение по-русски: plural(2, ['монета','монеты','монет']) → «монеты». */
export function plural(n: number, forms: [string, string, string]): string {
  const a = Math.abs(n) % 100;
  const b = a % 10;
  if (a > 10 && a < 20) return forms[2];
  if (b > 1 && b < 5) return forms[1];
  if (b === 1) return forms[0];
  return forms[2];
}

export const coinsText = (n: number): string => `${n} ${plural(n, ['монета', 'монеты', 'монет'])}`;
export const weeksText = (n: number): string => `${n} ${plural(n, ['неделя', 'недели', 'недель'])}`;
export const pointsText = (n: number): string => `${n} ${plural(n, ['очко', 'очка', 'очков'])}`;

export const roundDown5 = (n: number): number => Math.floor(n / 5) * 5;
export const roundUp5 = (n: number): number => Math.ceil(n / 5) * 5;

/** Подстановка {имя} в шаблон. */
export function fmt(template: string, vars: Record<string, string | number> = {}): string {
  return template.replace(/\{(\w+)\}/g, (_, k: string) => (k in vars ? String(vars[k]) : `{${k}}`));
}
