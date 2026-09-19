import { describe, expect, it } from 'vitest';
import { confirmPlan } from '../src/domain/plan';
import { averageDeposit, completeGoal, createCustomGoal, deposit, etaFor, previewWithdraw, selectGoal, withdraw } from '../src/domain/savings';
import { activeGoal, goalById, savingsTotal } from '../src/domain/profile';
import type { Profile } from '../src/domain/types';
import { ctx, makeProfile, must } from './helpers';

const planned = (): Profile => must(confirmPlan(makeProfile(), { must: 30, want: 20, save: 40 })).profile;

describe('Накопления и финансовая цель', () => {
  it('взнос переводит монеты из кошелька в цель и виден в накоплениях', () => {
    const r = must(deposit(planned(), 'house', 20));
    const p = r.profile;
    expect(p.wallet).toBe(80);
    expect(goalById(p, 'house')!.saved).toBe(20);
    expect(savingsTotal(p)).toBe(20);
    expect(p.period.saved).toBe(20);
    expect(p.ledger.at(-1)).toMatchObject({ kind: 'save', amount: -20, savingsDelta: 20 });
    expect(r.report.changes.map((c) => c.kind)).toEqual(expect.arrayContaining(['wallet', 'savings']));
  });

  it('первый взнос от 5 монет за неделю радует питомца', () => {
    const r = must(deposit(planned(), 'house', 10));
    expect(r.profile.pet.stats.mood).toBe(71);
  });

  it('нельзя отложить больше, чем есть или чем осталось до цели', () => {
    const p = planned();
    const tooMuch = deposit(p, 'house', 500);
    expect(tooMuch.ok).toBe(false);
    if (!tooMuch.ok) expect(tooMuch.error.code).toBe('NOT_ENOUGH');
    const over = deposit(must(deposit(p, 'house', 50)).profile, 'house', 20);
    expect(over.ok).toBe(false);
    if (!over.ok) expect(over.error).toMatchObject({ code: 'GOAL_FULL', max: 10 });
    expect(deposit(makeProfile(), 'house', 10).ok).toBe(false); // без плана
  });

  it('снятие без подтверждения невозможно и ничего не меняет', () => {
    const p = must(deposit(planned(), 'house', 30)).profile;
    const r = withdraw(p, 'house', 10, false);
    expect(r.ok).toBe(false);
    if (!r.ok) expect(r.error.code).toBe('CONFIRM_REQUIRED');
    expect(goalById(p, 'house')!.saved).toBe(30);
  });

  it('снятие с подтверждением возвращает монеты в кошелёк', () => {
    const p = must(deposit(planned(), 'house', 30)).profile;
    const r = must(withdraw(p, 'house', 10, true));
    expect(r.profile.wallet).toBe(80);
    expect(goalById(r.profile, 'house')!.saved).toBe(20);
    expect(r.profile.period.withdrawn).toBe(10);
    expect(r.profile.ledger.at(-1)).toMatchObject({ kind: 'withdraw', amount: 10, savingsDelta: -10 });
    expect(withdraw(p, 'house', 31, true).ok).toBe(false);
  });

  it('предпросмотр снятия показывает накопления и срок ДО подтверждения', () => {
    const p = { ...planned() };
    const g = goalById(p, 'house')!;
    g.saved = 20;
    g.deposits = { 1: 20 };
    p.period = { ...p.period, index: 2 };
    const pv = previewWithdraw(p, 'house', 10)!;
    expect(pv).toMatchObject({ savedBefore: 20, savedAfter: 10, remainingBefore: 40, remainingAfter: 50 });
    expect(pv.etaBefore.weeks).toBe(2); // 40 / 20
    expect(pv.etaAfter.weeks).toBe(3); // 50 / 20 → срок вырос
  });

  it('срок цели считается по среднему регулярному взносу за прошлые недели', () => {
    const g = { ...goalById(makeProfile(), 'house')!, saved: 10, deposits: { 1: 10, 2: 20, 3: 5 } as Record<number, number> };
    expect(averageDeposit(g, 3)).toBe(15); // (10 + 20) / 2, текущая неделя не учитывается
    expect(averageDeposit(g, 1)).toBe(12); // истории нет — берём всё, что есть: (10+20+5)/3 ≈ 12
    expect(etaFor(g, 3).weeks).toBe(4); // осталось 50 при взносе 15 → 4 недели
    expect(etaFor({ ...g, deposits: {} }, 1)).toMatchObject({ avg: null, weeks: null });
  });

  it('исполнение мечты: списывает цену, даёт трофей, рост и переключает цель', () => {
    let p = planned();
    const g = goalById(p, 'house')!;
    expect(completeGoal(p, 'house').ok).toBe(false); // не накоплено
    p = { ...p, wallet: 200, goals: p.goals.map((x) => (x.id === 'house' ? { ...g, saved: 60 } : x)) };
    const r = must(completeGoal(p, 'house'));
    expect(goalById(r.profile, 'house')).toMatchObject({ done: true, saved: 0 });
    expect(r.profile.trophies).toContain('house');
    expect(r.profile.pet.growth).toBe(3);
    expect(r.profile.activeGoalId).toBe('book');
    expect(completeGoal(r.profile, 'house').ok).toBe(false);
  });

  it('выбор цели и создание своей из набора параметров', () => {
    const p = planned();
    expect(activeGoal(must(selectGoal(p, 'park')).profile)!.id).toBe('park');
    const c = must(createCustomGoal(p, { title: 'Подарок другу', icon: 'gift', cost: 50 }, ctx())).profile;
    expect(activeGoal(c)).toMatchObject({ custom: true, cost: 50, title: 'Подарок другу' });
    expect(createCustomGoal(p, { title: 'Свой текст', icon: 'gift', cost: 50 }, ctx()).ok).toBe(false);
    expect(createCustomGoal(p, { title: 'Подарок другу', icon: 'gift', cost: 55 }, ctx()).ok).toBe(false);
    expect(createCustomGoal(p, { title: 'Подарок другу', icon: 'gift', cost: 500 }, ctx()).ok).toBe(false);
  });
});
