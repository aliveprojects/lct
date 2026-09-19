// Типы игровой модели. Здесь нет ни интерфейса, ни хранилища — только данные.

export type Direction = 'must' | 'want' | 'save';
export type PetStat = 'satiety' | 'care' | 'mood';
export type Topic = 'budget' | 'saving' | 'purchases';
export type NeedTag = 'food' | 'care';
export type Stars = 0 | 1 | 2 | 3;
export type Stage = 1 | 2 | 3 | 4;

export type Plan = Record<Direction, number>;

export interface PetAppearance {
  species: string;
  color: string;
  accessory: string;
}

export interface PetReason {
  text: string;
  stat?: PetStat;
  delta?: number;
}

export interface Pet {
  name: string;
  appearance: PetAppearance;
  stats: Record<PetStat, number>;
  /** Очки роста: копятся по итогам периодов и никогда не уменьшаются. */
  growth: number;
  stage: Stage;
  ownedAccessories: string[];
  reason: PetReason | null;
}

export type TxKind =
  | 'start'
  | 'allowance'
  | 'daily'
  | 'task'
  | 'bonus'
  | 'purchase'
  | 'event'
  | 'save'
  | 'withdraw'
  | 'goal';

/** Запись в истории. amount — изменение баланса (кошелька); savingsDelta — изменение накоплений. */
export interface Tx {
  id: number;
  period: number;
  kind: TxKind;
  title: string;
  amount: number;
  savingsDelta?: number;
  balance: number;
  dir?: Direction | 'event';
  itemId?: string;
}

export interface Goal {
  id: string;
  title: string;
  icon: string;
  cost: number;
  saved: number;
  /** Сколько положено в цель в каждом периоде (без учёта снятий). */
  deposits: Record<number, number>;
  custom: boolean;
  done: boolean;
}

export interface PeriodState {
  index: number;
  planConfirmed: boolean;
  startBalance: number;
  plan: Plan;
  spent: { must: number; want: number; event: number };
  needs: { food: number; care: number };
  saved: number;
  withdrawn: number;
  earned: number;
  adultBonus: number;
}

export type ReviewStatus = 'ok' | 'part' | 'todo';

export interface ReviewLine {
  id: string;
  status: ReviewStatus;
  text: string;
  points?: number;
  tip?: string;
}

export interface PeriodResult {
  index: number;
  plan: Plan;
  fact: { must: number; want: number; save: number; event: number; earned: number };
  needs: { food: boolean; care: boolean };
  checks: {
    mustWithin: boolean;
    wantWithin: boolean;
    savedSome: boolean;
    savedPlan: boolean;
    regular: boolean;
  };
  score: { needs: number; plan: number; saving: number; total: number };
  growthBefore: number;
  growthAfter: number;
  stageBefore: Stage;
  stageAfter: Stage;
  balanceEnd: number;
  statsBefore: Record<PetStat, number>;
  statsAfter: Record<PetStat, number>;
  lines: ReviewLine[];
  summary: string;
}

export interface TaskProgress {
  stars: Stars;
  attempts: number;
  paid: number;
}

export interface Profile {
  id: string;
  playerName: string;
  createdAt: string;
  isDemo: boolean;
  pet: Pet;
  wallet: number;
  goals: Goal[];
  activeGoalId: string | null;
  period: PeriodState;
  history: PeriodResult[];
  ledger: Tx[];
  nextTxId: number;
  tasks: Record<string, TaskProgress>;
  wishlist: string[];
  dailyGiftDate: string | null;
  pendingEventId: string | null;
  resolvedEvents: string[];
  trophies: string[];
}

export type TextScale = 'normal' | 'large' | 'xlarge';

export interface Settings {
  /** Фоновая музыка (отдельно от звуков-сигналов). */
  music: boolean;
  sound: boolean;
  animations: boolean;
  textScale: TextScale;
  highContrast: boolean;
}

export type Mode = 'normal' | 'demo';

export interface AppState {
  schema: number;
  settings: Settings;
  introSeen: boolean;
  mode: Mode;
  main: Profile | null;
  demo: Profile | null;
  /** Виртуальный сдвиг календаря в демо-режиме (дни), чтобы не ждать «подарок дня». */
  demoDayOffset: number;
}

// ---------- Результат действий ----------

export type ErrorCode =
  | 'PLAN_REQUIRED'
  | 'PLAN_ALREADY_CONFIRMED'
  | 'PLAN_INVALID'
  | 'PLAN_OVER_BUDGET'
  | 'NOT_ENOUGH'
  | 'CONFIRM_OVER_PLAN'
  | 'CONFIRM_REQUIRED'
  | 'ALREADY_OWNED'
  | 'GOAL_NOT_FOUND'
  | 'GOAL_FULL'
  | 'GOAL_NOT_REACHED'
  | 'GOAL_DONE'
  | 'INVALID_AMOUNT'
  | 'TASK_LOCKED'
  | 'TASK_NOT_FOUND'
  | 'ITEM_NOT_FOUND'
  | 'ALREADY_TODAY'
  | 'BONUS_LIMIT'
  | 'NO_EVENT'
  | 'NO_PROFILE';

export interface ShortageOption {
  id: 'earn' | 'cheaper' | 'wishlist' | 'savings' | 'wait';
  label: string;
  hint: string;
  route?: string;
  itemIds?: string[];
}

export interface GameError {
  code: ErrorCode;
  message: string;
  missing?: number;
  overBy?: number;
  max?: number;
  options?: ShortageOption[];
}

export interface Change {
  kind: 'wallet' | 'savings' | 'stat' | 'growth' | 'plan';
  label: string;
  delta: number;
  before?: number;
  after?: number;
  stat?: PetStat;
  why: string;
}

export interface Report {
  title: string;
  tone: 'good' | 'info' | 'care';
  changes: Change[];
  explain: string;
  next?: { label: string; route: string };
}

export type Outcome<T extends object = object> =
  | ({ ok: true; profile: Profile; report: Report } & T)
  | { ok: false; error: GameError };

export interface Ctx {
  content: import('../content/types').Content;
  /** Дата в формате ГГГГ-ММ-ДД (в демо-режиме может быть сдвинута). */
  today: string;
}
