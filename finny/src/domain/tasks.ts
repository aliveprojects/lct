import type {
  AllocateTask,
  CalcTask,
  CartTask,
  SimTask,
  SortTask,
  StoryEnd,
  StoryTask,
  TaskDef,
} from '../content/types';
import { DIRECTIONS, coinsText, sum, weeksText } from './econ';
import { addTx } from './profile';
import type { Ctx, Direction, Outcome, Profile, Stars } from './types';
import { clone, done, fail } from './util';

// ---------- Ответы ----------

export type TaskAnswer =
  | { type: 'sort'; groups: Record<string, 'need' | 'want'> }
  | { type: 'cart'; picked: string[] }
  | { type: 'allocate'; amounts: Record<Direction, number> }
  | { type: 'calc'; values: Record<string, number | null> }
  | { type: 'sim'; weekly: number }
  | { type: 'story'; path: number[] };

export interface TaskEvalDetail {
  ok: boolean | null;
  text: string;
  sub?: string;
}

export interface TaskEval {
  stars: 1 | 2 | 3;
  headline: string;
  summary: string;
  details: TaskEvalDetail[];
  recovery?: string;
  learn: string;
}

const HEADLINE: Record<1 | 2 | 3, string> = {
  3: 'Отлично!',
  2: 'Хорошо!',
  1: 'Есть над чем подумать',
};

const RETRY = 'Попробуй ещё раз — за лучший результат дадут ещё монеты.';

/** Все верно — 3 звезды; 60% и больше — 2; иначе 1 (с разбором и возможностью повторить). */
const starsFromRatio = (ok: number, total: number): 1 | 2 | 3 => (ok === total ? 3 : ok / total >= 0.6 ? 2 : 1);

// ---------- Оценка ----------

export function evaluateTask(def: TaskDef, answer: TaskAnswer): TaskEval {
  if (def.type !== answer.type) throw new Error(`Ответ типа ${answer.type} не подходит заданию ${def.type}`);
  switch (def.type) {
    case 'sort':
      return evalSort(def, answer as Extract<TaskAnswer, { type: 'sort' }>);
    case 'cart':
      return evalCart(def, answer as Extract<TaskAnswer, { type: 'cart' }>);
    case 'allocate':
      return evalAllocate(def, answer as Extract<TaskAnswer, { type: 'allocate' }>);
    case 'calc':
      return evalCalc(def, answer as Extract<TaskAnswer, { type: 'calc' }>);
    case 'sim':
      return evalSim(def, answer as Extract<TaskAnswer, { type: 'sim' }>);
    case 'story':
      return evalStory(def, answer as Extract<TaskAnswer, { type: 'story' }>);
  }
}

function evalSort(def: SortTask, a: Extract<TaskAnswer, { type: 'sort' }>): TaskEval {
  const label = (id: string) => def.groups.find((g) => g.id === id)?.label ?? id;
  const details = def.items.map((it) => {
    const ok = a.groups[it.id] === it.group;
    return { ok, text: it.label, sub: `${ok ? 'Верно. ' : `Лучше в «${label(it.group)}». `}${it.why}` };
  });
  const correct = details.filter((d) => d.ok).length;
  const stars = starsFromRatio(correct, details.length);
  return {
    stars,
    headline: HEADLINE[stars],
    summary: `Верно: ${correct} из ${details.length}.`,
    details,
    recovery: stars < 3 ? RETRY : undefined,
    learn: def.learn,
  };
}

function evalCart(def: CartTask, a: Extract<TaskAnswer, { type: 'cart' }>): TaskEval {
  const picked = def.items.filter((i) => a.picked.includes(i.id));
  const total = sum(picked.map((i) => i.price));
  const left = def.budget - total;
  const missing = def.items.filter((i) => i.need && !a.picked.includes(i.id));
  const details = picked.map((i) => ({ ok: null, text: `${i.label} — ${coinsText(i.price)}`, sub: i.why }) as TaskEvalDetail);
  details.push({ ok: left >= 0, text: `Итого: ${coinsText(total)} из ${def.budget}`, sub: left >= 0 ? `Осталось ${coinsText(left)}.` : `Не хватает ${coinsText(-left)}.` });

  let stars: 1 | 2 | 3;
  let summary: string;
  let recovery: string | undefined;
  if (left < 0) {
    stars = 1;
    summary = `В корзине на ${coinsText(-left)} больше, чем у тебя есть.`;
    recovery = 'Убери что-нибудь необязательное — и оплата пройдёт.';
  } else if (missing.length) {
    stars = 1;
    summary = `В корзине нет: ${missing.map((m) => m.label.toLowerCase()).join(', ')}. Без этого питомцу будет трудно.`;
    recovery = 'Сначала кладём нужное. На него монет хватит.';
  } else if (left >= def.minLeft) {
    stars = 3;
    summary = 'Нужное куплено, и ещё осталось про запас. Так и надо!';
  } else {
    stars = 2;
    summary = 'Нужное куплено, но монет почти не осталось. Хорошо бы оставлять хоть немного.';
    recovery = 'Попробуй убрать одну мелочь из необязательного.';
  }
  return { stars, headline: HEADLINE[stars], summary, details, recovery, learn: def.learn };
}

function evalAllocate(def: AllocateTask, a: Extract<TaskAnswer, { type: 'allocate' }>): TaskEval {
  const used = sum(DIRECTIONS.map((d) => a.amounts[d] ?? 0));
  const details: TaskEvalDetail[] = def.rules.map((r) => {
    const v = a.amounts[r.bucket] ?? 0;
    const ok = r.op === 'min' ? v >= r.value : v <= r.value;
    return { ok, text: ok ? r.ok : r.fail };
  });
  const passes = details.filter((d) => d.ok).length;
  if (used > def.total) {
    return {
      stars: 1,
      headline: HEADLINE[1],
      summary: `Ты разложил ${used}, а всего монет ${def.total}. Так нельзя: расходы не должны быть больше доходов.`,
      details,
      recovery: RETRY,
      learn: def.learn,
    };
  }
  const stars: 1 | 2 | 3 = passes === def.rules.length ? 3 : passes === def.rules.length - 1 ? 2 : 1;
  return {
    stars,
    headline: HEADLINE[stars],
    summary: stars === 3 ? 'Монет хватает на всё: и на нужное, и на копилку.' : 'Получилось не всё. Посмотри, что можно поправить.',
    details,
    recovery: stars < 3 ? RETRY : undefined,
    learn: def.learn,
  };
}

function evalCalc(def: CalcTask, a: Extract<TaskAnswer, { type: 'calc' }>): TaskEval {
  const details = def.questions.map((q) => {
    const v = a.values[q.id];
    const ok = v === q.answer;
    return { ok, text: q.text, sub: ok ? `Верно! ${q.explain}` : `Твой ответ: ${v ?? '—'}. Правильно: ${q.answer}. ${q.explain}` };
  });
  const correct = details.filter((d) => d.ok).length;
  const stars = starsFromRatio(correct, details.length);
  return {
    stars,
    headline: HEADLINE[stars],
    summary: `Верных ответов: ${correct} из ${details.length}.`,
    details,
    recovery: stars < 3 ? RETRY : undefined,
    learn: def.learn,
  };
}

export const simWeeks = (def: SimTask, weekly: number): number | null =>
  weekly > 0 ? Math.ceil((def.goalCost - def.start) / weekly) : null;

function evalSim(def: SimTask, a: Extract<TaskAnswer, { type: 'sim' }>): TaskEval {
  const weekly = Math.min(Math.max(0, a.weekly), def.freePerWeek);
  const weeks = simWeeks(def, weekly);
  let stars: 1 | 2 | 3;
  let summary: string;
  if (weeks === null) {
    stars = 1;
    summary = def.texts.none;
  } else if (weeks <= def.targetWeeks) {
    stars = 3;
    summary = def.texts.great;
  } else if (weeks <= def.targetWeeks * 2) {
    stars = 2;
    summary = def.texts.good;
  } else {
    stars = 1;
    summary = def.texts.slow;
  }
  const details: TaskEvalDetail[] =
    weeks === null
      ? [{ ok: false, text: 'Ты решил не откладывать', sub: 'Копилка так и останется пустой.' }]
      : [
          { ok: null, text: `Откладываешь по ${coinsText(weekly)} в неделю`, sub: `Цель в ${coinsText(def.goalCost)} накопится за ${weeksText(weeks)}.` },
          { ok: null, text: `На радости остаётся ${coinsText(def.freePerWeek - weekly)} в неделю`, sub: 'Это монеты, которые можно потратить на желаемое.' },
        ];
  return { stars, headline: HEADLINE[stars], summary, details, recovery: stars < 3 ? RETRY : undefined, learn: def.learn };
}

/** Проходит историю по выбранным вариантам и возвращает концовку. */
export function walkStory(def: StoryTask, path: number[]): { end: StoryEnd | null; steps: { text: string; choice: string }[] } {
  let node = def.nodes[def.start];
  const steps: { text: string; choice: string }[] = [];
  for (const idx of path) {
    const choice = node?.choices[idx];
    if (!choice) break;
    steps.push({ text: node.text, choice: choice.text });
    if (choice.end) return { end: choice.end, steps };
    node = def.nodes[choice.next ?? ''];
  }
  return { end: null, steps };
}

function evalStory(def: StoryTask, a: Extract<TaskAnswer, { type: 'story' }>): TaskEval {
  const { end, steps } = walkStory(def, a.path);
  if (!end) throw new Error('История не дошла до концовки');
  const details: TaskEvalDetail[] = steps.map((s) => ({ ok: null, text: s.text, sub: `Ты выбрал: «${s.choice}»` }));
  details.push({ ok: end.stars === 3 ? true : null, text: 'Почему так?', sub: end.explain });
  return { stars: end.stars, headline: HEADLINE[end.stars], summary: end.consequence, details, recovery: end.recovery, learn: def.learn };
}

// ---------- Решатели (для тестов и демо-режима) ----------

/** Лучший ответ на задание. */
export function solveTask(def: TaskDef): TaskAnswer {
  switch (def.type) {
    case 'sort':
      return { type: 'sort', groups: Object.fromEntries(def.items.map((i) => [i.id, i.group])) };
    case 'cart': {
      const picked = def.items.filter((i) => i.need).map((i) => i.id);
      let total = sum(def.items.filter((i) => i.need).map((i) => i.price));
      for (const it of [...def.items].filter((i) => !i.need).sort((x, y) => x.price - y.price)) {
        if (def.budget - (total + it.price) >= def.minLeft) {
          picked.push(it.id);
          total += it.price;
          break;
        }
      }
      return { type: 'cart', picked };
    }
    case 'allocate': {
      const amounts: Record<Direction, number> = { must: 0, want: 0, save: 0 };
      for (const r of def.rules) if (r.op === 'min') amounts[r.bucket] = Math.max(amounts[r.bucket], r.value);
      let rest = def.total - sum(DIRECTIONS.map((d) => amounts[d]));
      const order: Direction[] = ['must', 'save', 'want'];
      for (let i = 0; rest >= def.step && i < 1000; i++) {
        const d = order[i % order.length];
        const cap = def.rules.find((r) => r.bucket === d && r.op === 'max')?.value ?? Infinity;
        if (amounts[d] + def.step <= cap) {
          amounts[d] += def.step;
          rest -= def.step;
        }
      }
      return { type: 'allocate', amounts };
    }
    case 'calc':
      return { type: 'calc', values: Object.fromEntries(def.questions.map((q) => [q.id, q.answer])) };
    case 'sim': {
      for (let w = def.step; w <= def.freePerWeek; w += def.step) if ((simWeeks(def, w) ?? Infinity) <= def.targetWeeks) return { type: 'sim', weekly: w };
      return { type: 'sim', weekly: def.freePerWeek };
    }
    case 'story':
      return { type: 'story', path: findStoryPath(def, (s) => s === 3) };
  }
}

/** Заведомо неудачный ответ — нужен, чтобы проверять «ошибочный вариант» и путь восстановления. */
export function worstAnswer(def: TaskDef): TaskAnswer {
  switch (def.type) {
    case 'sort':
      return { type: 'sort', groups: Object.fromEntries(def.items.map((i) => [i.id, i.group === 'need' ? 'want' : 'need'])) };
    case 'cart': {
      const picked: string[] = [];
      let total = 0;
      for (const it of [...def.items].filter((i) => !i.need).sort((x, y) => x.price - y.price)) {
        if (total + it.price <= def.budget) {
          picked.push(it.id);
          total += it.price;
        }
      }
      return { type: 'cart', picked };
    }
    case 'allocate': {
      const amounts: Record<Direction, number> = { must: 0, want: 0, save: 0 };
      amounts.want = def.total;
      return { type: 'allocate', amounts };
    }
    case 'calc':
      return { type: 'calc', values: Object.fromEntries(def.questions.map((q) => [q.id, q.answer + 1])) };
    case 'sim':
      return { type: 'sim', weekly: 0 };
    case 'story':
      return { type: 'story', path: findStoryPath(def, (s) => s === 1) };
  }
}

function findStoryPath(def: StoryTask, want: (stars: number) => boolean): number[] {
  const search = (nodeId: string, path: number[]): number[] | null => {
    const node = def.nodes[nodeId];
    for (let i = 0; i < node.choices.length; i++) {
      const c = node.choices[i];
      if (c.end && want(c.end.stars)) return [...path, i];
      if (c.next) {
        const r = search(c.next, [...path, i]);
        if (r) return r;
      }
    }
    return null;
  };
  const found = search(def.start, []);
  if (!found) throw new Error(`В задании ${def.id} нет подходящей концовки`);
  return found;
}

// ---------- Доступность и выдача наград ----------

export const isUnlocked = (def: TaskDef, p: Profile): boolean => p.isDemo || def.unlockPeriod <= p.period.index;

export const availableTasks = (p: Profile, ctx: Ctx): TaskDef[] => ctx.content.tasks.filter((t) => isUnlocked(t, p));

export const taskStars = (p: Profile, id: string): Stars => p.tasks[id]?.stars ?? 0;

/** Активное задание для главного экрана: сначала новое, потом то, где можно лучше. */
export function nextActiveTask(p: Profile, ctx: Ctx): TaskDef | null {
  const open = availableTasks(p, ctx).filter((t) => taskStars(p, t.id) < 3);
  return open.find((t) => taskStars(p, t.id) === 0) ?? open[0] ?? null;
}

export function submitTask(p0: Profile, taskId: string, answer: TaskAnswer, ctx: Ctx): Outcome<{ evaluation: TaskEval; extra: number; improved: boolean }> {
  const def = ctx.content.tasks.find((t) => t.id === taskId);
  if (!def) return fail('TASK_NOT_FOUND', 'Такого задания нет.');
  if (!isUnlocked(def, p0)) return fail('TASK_LOCKED', `Это задание откроется на ${def.unlockPeriod}-й неделе.`);

  const evaluation = evaluateTask(def, answer);
  const p = clone(p0);
  const prog = p.tasks[taskId] ?? { stars: 0 as Stars, attempts: 0, paid: 0 };
  prog.attempts += 1;
  const improved = evaluation.stars > prog.stars;
  let extra = 0;
  if (improved) {
    extra = Math.max(0, def.rewards[String(evaluation.stars) as '1' | '2' | '3'] - prog.paid);
    prog.paid += extra;
    prog.stars = evaluation.stars;
  }
  p.tasks[taskId] = prog;

  const before = p.wallet;
  if (extra > 0) {
    p.wallet += extra;
    p.period.earned += extra;
    addTx(p, { kind: 'task', title: `Задание «${def.title}»`, amount: extra });
    p.pet.reason = { text: 'Ты справился с заданием — молодец! Питомцу интересно учиться вместе с тобой.' };
  }
  return done(
    p,
    {
      title: `Задание: ${def.title}`,
      tone: evaluation.stars === 3 ? 'good' : 'info',
      changes:
        extra > 0
          ? [{ kind: 'wallet', label: 'Монеты', before, after: p.wallet, delta: extra, why: `Награда за задание: ${'★'.repeat(evaluation.stars)} из 3.` }]
          : [],
      explain: evaluation.summary,
    },
    { evaluation, extra, improved },
  );
}
