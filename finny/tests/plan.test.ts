import { describe, expect, it } from 'vitest';
import { ECON } from '../src/domain/econ';
import { confirmPlan, needsCost, suggestPlan, topUpPlan, validatePlan } from '../src/domain/plan';
import { envelope, freeCoins } from '../src/domain/profile';
import { content } from '../src/content';
import { makeProfile, must } from './helpers';

describe('Планирование бюджета', () => {
  it('стартовый профиль получает стартовый бюджет с объяснением', () => {
    const p = makeProfile();
    expect(p.wallet).toBe(ECON.startBalance);
    expect(p.ledger[0]).toMatchObject({ kind: 'start', title: 'Стартовый подарок', amount: ECON.startBalance, balance: ECON.startBalance });
  });

  it('validatePlan: показывает остаток и не даёт превысить бюджет', () => {
    expect(validatePlan(100, { must: 40, want: 30, save: 20 })).toEqual({ ok: true, remainder: 10 });
    expect(validatePlan(100, { must: 60, want: 30, save: 20 })).toMatchObject({ ok: false, remainder: -10, problem: 'over' });
    expect(validatePlan(100, { must: 0, want: 0, save: 0 })).toMatchObject({ ok: false, problem: 'empty' });
    expect(validatePlan(100, { must: -5, want: 30, save: 20 })).toMatchObject({ ok: false, problem: 'invalid' });
    expect(validatePlan(100, { must: 2.5, want: 30, save: 20 })).toMatchObject({ ok: false, problem: 'invalid' });
  });

  it('confirmPlan: подтверждает и фиксирует стартовый баланс периода', () => {
    const p = must(confirmPlan(makeProfile(), { must: 40, want: 30, save: 20 })).profile;
    expect(p.period.planConfirmed).toBe(true);
    expect(p.period.startBalance).toBe(100);
    expect(p.period.plan).toEqual({ must: 40, want: 30, save: 20 });
    expect(freeCoins(p)).toBe(10); // запас
  });

  it('confirmPlan: превышение бюджета отклоняется и сообщает, на сколько', () => {
    const r = confirmPlan(makeProfile(), { must: 80, want: 30, save: 20 });
    expect(r.ok).toBe(false);
    if (!r.ok) {
      expect(r.error.code).toBe('PLAN_OVER_BUDGET');
      expect(r.error.overBy).toBe(30);
    }
  });

  it('confirmPlan: план нельзя переподтвердить и менять после подтверждения', () => {
    const p = must(confirmPlan(makeProfile(), { must: 40, want: 30, save: 20 })).profile;
    const again = confirmPlan(p, { must: 10, want: 10, save: 10 });
    expect(again.ok).toBe(false);
    if (!again.ok) expect(again.error.code).toBe('PLAN_ALREADY_CONFIRMED');
  });

  it('confirmPlan не изменяет исходный профиль (иммутабельность)', () => {
    const p0 = makeProfile();
    const snapshot = JSON.stringify(p0);
    must(confirmPlan(p0, { must: 40, want: 30, save: 20 }));
    expect(JSON.stringify(p0)).toBe(snapshot);
  });

  it('suggestPlan: подсказка укладывается в бюджет и покрывает минимум нужного', () => {
    const p = makeProfile();
    const s = suggestPlan(p, content);
    expect(s.must + s.want + s.save).toBeLessThanOrEqual(p.wallet);
    expect(s.must).toBeGreaterThanOrEqual(needsCost(content).total);
    expect(validatePlan(p.wallet, s).ok).toBe(true);
  });

  it('topUpPlan: свободные монеты можно добавить в конверт, больше — нельзя', () => {
    const p = must(confirmPlan(makeProfile(), { must: 40, want: 30, save: 20 })).profile; // запас 10
    const t = must(topUpPlan(p, 'want', 5)).profile;
    expect(t.period.plan.want).toBe(35);
    expect(envelope(t, 'want').left).toBe(35);
    const over = topUpPlan(p, 'want', 15);
    expect(over.ok).toBe(false);
    if (!over.ok) expect(over.error.code).toBe('NOT_ENOUGH');
    expect(topUpPlan(makeProfile(), 'want', 5).ok).toBe(false); // до плана нельзя
  });
});
