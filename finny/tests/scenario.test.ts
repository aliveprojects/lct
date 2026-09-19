import { describe, expect, it } from 'vitest';
import { content } from '../src/content';
import { playPeriodAuto } from '../src/domain/demo';
import { ADULT_BONUS_REASONS, grantAdultBonus } from '../src/domain/income';
import { confirmPlan, suggestPlan } from '../src/domain/plan';
import { endPeriod } from '../src/domain/period';
import { activeGoal, envelope, freeCoins } from '../src/domain/profile';
import { deposit, selectGoal, withdraw } from '../src/domain/savings';
import { activeProfile, initialApp, parseState, resetProgress, serializeState, withProfile } from '../src/domain/state';
import { buy } from '../src/domain/shop';
import { availableTasks, nextActiveTask, solveTask, submitTask, worstAnswer } from '../src/domain/tasks';
import { ctx, makeProfile, must } from './helpers';

describe('Сквозной сценарий (Приложение А ТЗ), шаги 2–12', () => {
  it('проходит весь игровой цикл без ошибок и тупиков', () => {
    // 2–3. Локальный профиль без имени/телефона/e-mail, питомец с именем
    let p = makeProfile();
    expect(p.pet.name).toBe('Тестик');
    expect(JSON.stringify(p)).not.toMatch(/@|\+7|phone|email/i);

    // 4. Стартовый бюджет, текущая цель и доступные задания
    expect(p.wallet).toBe(100);
    expect(activeGoal(p)!.id).toBe('house');
    expect(availableTasks(p, ctx()).length).toBeGreaterThanOrEqual(3);
    expect(nextActiveTask(p, ctx())).not.toBeNull();

    // 5. Распределение: нельзя больше бюджета, остаток виден, потом подтверждение
    expect(confirmPlan(p, { must: 70, want: 30, save: 20 }).ok).toBe(false);
    const suggestion = suggestPlan(p, content);
    p = must(confirmPlan(p, suggestion)).profile;
    expect(freeCoins(p)).toBe(100 - suggestion.must - suggestion.want - suggestion.save);

    // 6. Задание: ошибочный, затем правильный вариант — оба с объяснением
    const t = nextActiveTask(p, ctx())!;
    const wrong = must(submitTask(p, t.id, worstAnswer(t), ctx()));
    expect(wrong.evaluation.details.length).toBeGreaterThan(0);
    const right = must(submitTask(wrong.profile, t.id, solveTask(t), ctx()));
    p = right.profile;
    expect(p.wallet).toBe(100 + t.rewards['3']);

    // 7. Обязательная и необязательная покупка + попытка покупки при нехватке средств
    p = must(buy(p, 'meal', {}, ctx())).profile;
    p = must(buy(p, 'ball', {}, ctx())).profile;
    const short = buy(p, 'castle', {}, ctx());
    expect(short.ok).toBe(false);
    if (!short.ok) {
      expect(short.error.message).toMatch(/Не хватает/);
      expect(short.error.options!.length).toBeGreaterThan(1);
    }
    expect(p.ledger.filter((x) => x.kind === 'purchase').map((x) => x.dir)).toEqual(['must', 'want']);

    // 8. Выбор цели и пополнение накоплений, снятие — только с подтверждением
    p = must(selectGoal(p, 'book')).profile;
    p = must(deposit(p, 'book', 20)).profile;
    expect(activeGoal(p)).toMatchObject({ id: 'book', saved: 20 });
    expect(withdraw(p, 'book', 5, false).ok).toBe(false);

    // 9. Обратная связь: баланс, план, состояние питомца
    expect(envelope(p, 'save').used).toBe(20);
    expect(p.pet.reason?.text.length).toBeGreaterThan(0);

    // 10. Переход в новый период; изменение прогресса
    const before = p.pet.growth;
    const end = must(endPeriod(p, ctx()));
    p = end.profile;
    expect(end.result.summary.length).toBeGreaterThan(0);
    expect(p.pet.growth).toBeGreaterThan(before);
    expect(p.period.index).toBe(2);

    // 11. Закрытие и повторный запуск: всё сохраняется
    const saved = serializeState(withProfile(initialApp(), p));
    const reopened = activeProfile(parseState(saved).state)!;
    expect(reopened).toEqual(p);

    // 12. Раздел взрослого: бонус, сброс
    const bonus = must(grantAdultBonus(reopened, 10, ADULT_BONUS_REASONS[0])).profile;
    expect(bonus.wallet).toBe(reopened.wallet + 10);
    const reset = resetProgress(bonus, ctx());
    expect(reset).toMatchObject({ wallet: 100, history: [] });
  });
});

describe('Демонстрационный режим: 5 периодов подряд без ожидания', () => {
  const play = () => {
    let p = makeProfile(true);
    const snapshots = [p];
    for (let i = 0; i < 5; i++) {
      p = playPeriodAuto(p, ctx(`2026-09-${20 + i}`));
      snapshots.push(p);
    }
    return { p, snapshots };
  };

  it('проходит 5 последовательных периодов, рост объясним и не убывает', () => {
    const { p, snapshots } = play();
    expect(p.history).toHaveLength(5);
    expect(p.period.index).toBe(6);
    const growth = snapshots.map((s) => s.pet.growth);
    for (let i = 1; i < growth.length; i++) expect(growth[i]).toBeGreaterThanOrEqual(growth[i - 1]);
    expect(p.history.every((h) => h.summary.length > 0 && h.lines.length > 0)).toBe(true);
    for (const h of p.history) expect(h.lines.reduce((a, l) => a + (l.points ?? 0), 0)).toBe(h.score.total);
  });

  it('питомец меняет стадию как результат серии решений', () => {
    const { p } = play();
    const stages = p.history.map((h) => h.stageAfter);
    expect(p.pet.stage).toBeGreaterThanOrEqual(3);
    expect(new Set([1, ...stages]).size).toBeGreaterThanOrEqual(3); // видны не менее трёх стадий
    expect(p.history.some((h) => h.stageAfter > h.stageBefore)).toBe(true);
  });

  it('все девять заданий выполнены, хотя бы одна мечта исполнена', () => {
    let { p } = play();
    for (const t of content.tasks) p = must(submitTask(p, t.id, solveTask(t), ctx())).profile;
    expect(Object.values(p.tasks).every((x) => x.stars === 3)).toBe(true);
    expect(p.trophies.length).toBeGreaterThanOrEqual(1);
  });

  it('история операций целостна: баланс сходится, минуса нет, у каждой записи есть источник', () => {
    const { p } = play();
    let bal = 0;
    for (const tx of p.ledger) {
      bal += tx.amount;
      expect(tx.balance).toBe(bal);
      expect(tx.balance).toBeGreaterThanOrEqual(0);
      expect(tx.title.length).toBeGreaterThan(0);
    }
    expect(bal).toBe(p.wallet);
  });

  it('детерминирован: два прогона дают одинаковый результат (воспроизводимая демонстрация)', () => {
    const a = play().p;
    const b = play().p;
    expect(JSON.stringify({ ...a, id: '', createdAt: '' })).toBe(JSON.stringify({ ...b, id: '', createdAt: '' }));
  });
});
