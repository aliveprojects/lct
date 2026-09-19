import { describe, expect, it } from 'vitest';
import { content } from '../src/content';
import { confirmPlan } from '../src/domain/plan';
import { evaluateTask, isUnlocked, nextActiveTask, simWeeks, solveTask, submitTask, worstAnswer } from '../src/domain/tasks';
import type { SimTask } from '../src/content/types';
import { ctx, makeProfile, must, task } from './helpers';

describe('Финансовые задания', () => {
  for (const def of content.tasks) {
    describe(`${def.id} (${def.type}, тема «${def.topic}»)`, () => {
      it('правильный вариант даёт 3 звезды и объяснение', () => {
        const ev = evaluateTask(def, solveTask(def));
        expect(ev.stars).toBe(3);
        expect(ev.summary.length).toBeGreaterThan(0);
        expect(ev.details.length).toBeGreaterThan(0);
        expect(ev.details.every((d) => d.text.length > 0)).toBe(true);
        expect(ev.learn).toBe(def.learn);
      });

      it('ошибочный вариант тоже получает объяснение и путь исправления', () => {
        const ev = evaluateTask(def, worstAnswer(def));
        expect(ev.stars).toBe(1);
        expect(ev.summary.length).toBeGreaterThan(0);
        expect(ev.details.length).toBeGreaterThan(0);
        expect(ev.recovery && ev.recovery.length > 0).toBe(true);
      });
    });
  }

  it('частично верный ответ — не провал: 2 из 3 и 5 из 8 дают две звезды', () => {
    const calc = task('calc-change');
    const two = evaluateTask(calc, { type: 'calc', values: { q1: 24, q2: 7, q3: 5 } });
    expect(two.stars).toBe(2);
    expect(two.details[1].sub).toContain('Правильно: 6');
    const sort = task('sort-need-want');
    const mostly = { type: 'sort' as const, groups: Object.fromEntries((sort as import('../src/content/types').SortTask).items.map((i, k) => [i.id, k < 5 ? i.group : i.group === 'need' ? 'want' : 'need'])) };
    expect(evaluateTask(sort, mostly).stars).toBe(2);
  });

  it('тип ответа должен совпадать с типом задания', () => {
    expect(() => evaluateTask(task('calc-weeks'), { type: 'sim', weekly: 5 })).toThrow();
  });

  it('награда выдаётся один раз, а за улучшение доплачивается разница', () => {
    const def = task('calc-weeks');
    let p = makeProfile(true); // в демо-режиме открыты все задания
    const start = p.wallet;
    const bad = must(submitTask(p, def.id, worstAnswer(def), ctx()));
    p = bad.profile;
    expect(bad.extra).toBe(def.rewards['1']);
    expect(bad.evaluation.stars).toBe(1);
    expect(p.wallet).toBe(start + def.rewards['1']);

    const good = must(submitTask(p, def.id, solveTask(def), ctx()));
    p = good.profile;
    expect(good.improved).toBe(true);
    expect(good.extra).toBe(def.rewards['3'] - def.rewards['1']);
    expect(p.wallet).toBe(start + def.rewards['3']);
    expect(p.tasks[def.id]).toMatchObject({ stars: 3, attempts: 2, paid: def.rewards['3'] });

    const again = must(submitTask(p, def.id, solveTask(def), ctx()));
    expect(again.extra).toBe(0);
    expect(again.profile.wallet).toBe(p.wallet);
    expect(again.profile.ledger.filter((t) => t.kind === 'task')).toHaveLength(2);
  });

  it('начисление за задание попадает в историю с источником и суммой', () => {
    const def = task('sort-need-want');
    const p = must(submitTask(makeProfile(true), def.id, solveTask(def), ctx())).profile;
    expect(p.ledger.at(-1)).toMatchObject({ kind: 'task', title: `Задание «${def.title}»`, amount: def.rewards['3'], balance: p.wallet });
  });

  it('задания открываются по неделям, а в демо-режиме доступны сразу все', () => {
    const normal = makeProfile(false);
    const demo = makeProfile(true);
    const late = content.tasks.filter((t) => t.unlockPeriod > 1);
    expect(late.length).toBeGreaterThan(0);
    for (const t of late) {
      expect(isUnlocked(t, normal)).toBe(false);
      expect(isUnlocked(t, demo)).toBe(true);
    }
    const r = submitTask(normal, late[0].id, solveTask(late[0]), ctx());
    expect(r.ok).toBe(false);
    if (!r.ok) expect(r.error.code).toBe('TASK_LOCKED');
    expect(content.tasks.filter((t) => isUnlocked(t, normal)).length).toBeGreaterThanOrEqual(3);
  });

  it('активное задание: сначала новое, потом то, где можно лучше', () => {
    let p = makeProfile(true);
    const first = nextActiveTask(p, ctx())!;
    p = must(submitTask(p, first.id, solveTask(first), ctx())).profile;
    expect(nextActiveTask(p, ctx())!.id).not.toBe(first.id);
    for (const t of content.tasks) p = must(submitTask(p, t.id, solveTask(t), ctx())).profile;
    expect(nextActiveTask(p, ctx())).toBeNull();
  });

  it('сумма в «распредели» не может превышать доступное', () => {
    const def = task('budget-week');
    const ev = evaluateTask(def, { type: 'allocate', amounts: { must: 40, want: 30, save: 30 } });
    expect(ev.stars).toBe(1);
    expect(ev.summary).toContain('больше');
  });

  it('корзина: нельзя оплатить больше бюджета и забыть нужное', () => {
    const def = task('cart-zoo');
    const over = evaluateTask(def, { type: 'cart', picked: ['food', 'soap', 'ball', 'mouse'] });
    expect(over.stars).toBe(1);
    const noNeeds = evaluateTask(def, { type: 'cart', picked: ['ball'] });
    expect(noNeeds.stars).toBe(1);
    const tight = evaluateTask(def, { type: 'cart', picked: ['food', 'soap', 'ball'] }); // 37, осталось 3 < 5
    expect(tight.stars).toBe(2);
  });

  it('симуляция накоплений: срок считается как «осталось ÷ взнос»', () => {
    const def = task('sim-house') as SimTask;
    expect(simWeeks(def, 15)).toBe(4);
    expect(simWeeks(def, 10)).toBe(6);
    expect(simWeeks(def, 0)).toBeNull();
    expect(evaluateTask(def, { type: 'sim', weekly: 10 }).stars).toBe(2);
    expect(evaluateTask(def, { type: 'sim', weekly: 5 }).stars).toBe(1);
  });

  it('задание не мешает плану и не меняет конверты', () => {
    const p0 = must(confirmPlan(makeProfile(true), { must: 40, want: 30, save: 20 })).profile;
    const def = task('calc-change');
    const p = must(submitTask(p0, def.id, solveTask(def), { ...ctx(), content })).profile;
    expect(p.period.plan).toEqual(p0.period.plan);
  });
});
