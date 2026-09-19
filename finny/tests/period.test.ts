import { describe, expect, it } from 'vitest';
import { ECON, stageForGrowth } from '../src/domain/econ';
import { ADULT_BONUS_REASONS, adultBonusLeft, claimDaily, grantAdultBonus } from '../src/domain/income';
import { confirmPlan } from '../src/domain/plan';
import { endPeriod, resolveEvent } from '../src/domain/period';
import { petExpression } from '../src/domain/profile';
import { deposit } from '../src/domain/savings';
import { buy } from '../src/domain/shop';
import type { Plan, Profile } from '../src/domain/types';
import { ctx, makeProfile, must } from './helpers';

interface Week {
  plan?: Plan;
  buys?: string[];
  save?: number;
}

function playWeek(p0: Profile, w: Week): Profile {
  let p = p0;
  if (!p.period.planConfirmed) p = must(confirmPlan(p, w.plan ?? { must: 35, want: 25, save: 30 })).profile;
  for (const id of w.buys ?? []) p = must(buy(p, id, { confirmOverPlan: true }, ctx())).profile;
  const room = p.goals.find((g) => g.id === 'house')!;
  const amount = Math.min(w.save ?? 0, room.cost - room.saved); // в полную копилку класть нельзя
  if (amount > 0) p = must(deposit(p, 'house', amount)).profile;
  return must(endPeriod(p, ctx())).profile;
}

const GOOD: Week = { buys: ['meal', 'brush', 'ball'], save: 30 };

describe('Игровой период: итоги, рост и восстановление', () => {
  it('завершить неделю без подтверждённого плана нельзя', () => {
    const r = endPeriod(makeProfile(), ctx());
    expect(r.ok).toBe(false);
    if (!r.ok) expect(r.error.code).toBe('PLAN_REQUIRED');
  });

  it('хорошая неделя: очки считаются по формуле и объясняются построчно', () => {
    const r = must(endPeriod(
      (() => {
        let p = must(confirmPlan(makeProfile(), { must: 35, want: 25, save: 30 })).profile;
        for (const id of GOOD.buys!) p = must(buy(p, id, {}, ctx())).profile;
        return must(deposit(p, 'house', 30)).profile;
      })(),
      ctx(),
    ));
    expect(r.result.score).toEqual({ needs: 3, plan: 3, saving: 1, total: 7 });
    expect(r.result.lines.reduce((a, l) => a + (l.points ?? 0), 0)).toBe(7);
    expect(r.result.fact).toMatchObject({ must: 22, want: 12, save: 30 });
    expect(r.profile.pet.growth).toBe(7);
    expect(r.profile.history).toHaveLength(1);
    expect(r.profile.period).toMatchObject({ index: 2, planConfirmed: false });
    // остаток 36 + карманные 70
    expect(r.profile.wallet).toBe(36 + ECON.weeklyAllowance);
    expect(r.profile.ledger.at(-1)).toMatchObject({ kind: 'allowance', amount: ECON.weeklyAllowance });
    expect(r.report.changes.some((c) => c.kind === 'growth')).toBe(true);
  });

  it('регулярность: вторая неделя с копилкой подряд даёт дополнительное очко и стадию', () => {
    let p = playWeek(makeProfile(), GOOD);
    expect(p.history[0].checks.regular).toBe(false);
    expect(p.pet.stage).toBe(1);
    p = playWeek(p, GOOD);
    expect(p.history[1].checks.regular).toBe(true);
    expect(p.history[1].score.total).toBe(8);
    expect(p.pet.growth).toBe(15);
    expect(p.pet.stage).toBe(2);
    expect(p.history[1].stageBefore).toBe(1);
    expect(p.history[1].stageAfter).toBe(2);
    expect(p.history[1].summary).toContain('Подросток');
  });

  it('пустая неделя не обнуляет прогресс: рост не уменьшается, показатели не падают ниже границы', () => {
    let p = playWeek(makeProfile(), GOOD);
    const growth = p.pet.growth;
    for (let i = 0; i < 12; i++) {
      p = playWeek(p, { plan: { must: 30, want: 20, save: 20 } });
      expect(p.pet.growth).toBeGreaterThanOrEqual(growth);
      for (const v of Object.values(p.pet.stats)) expect(v).toBeGreaterThanOrEqual(ECON.statFloor);
    }
    expect(p.history.at(-1)!.score.total).toBe(0);
    expect(p.history.at(-1)!.lines.some((l) => l.tip)).toBe(true); // есть подсказка, как исправить
  });

  it('перерасход «Хочется» замечается, но объясняется мягко и с советом', () => {
    const p = playWeek(makeProfile(), { plan: { must: 35, want: 10, save: 30 }, buys: ['meal', 'brush', 'mouse'], save: 30 });
    const h = p.history[0];
    expect(h.checks.wantWithin).toBe(false);
    expect(h.score.plan).toBe(2);
    const line = h.lines.find((l) => l.id === 'want')!;
    expect(line.status).toBe('part');
    expect(line.tip).toContain('не ошибка');
  });

  it('без еды и ухода питомец просит заботы, но не болеет и не «умирает»', () => {
    const p = playWeek(makeProfile(), { plan: { must: 30, want: 20, save: 20 } });
    expect(p.history[0].needs).toEqual({ food: false, care: false });
    expect(p.pet.stats).toEqual({ satiety: 30, care: 40, mood: 50 });
    const e = petExpression(p.pet.stats);
    expect(e.code).toBe('hungry');
    expect(e.label.length).toBeGreaterThan(0);
    expect(e.hint).toContain('магазин');
  });

  it('стадии роста: не меньше трёх и монотонно растут', () => {
    expect(ECON.stageFrom.length).toBeGreaterThanOrEqual(3);
    let prev = 0;
    for (let g = 0; g <= 60; g++) {
      const s = stageForGrowth(g);
      expect(s).toBeGreaterThanOrEqual(prev);
      prev = s;
    }
    expect(stageForGrowth(0)).toBe(1);
    expect(stageForGrowth(8)).toBe(2);
    expect(stageForGrowth(20)).toBe(3);
    expect(stageForGrowth(36)).toBe(4);
  });

  it('непредвиденное событие приходит на 3-й неделе и оплачивается из кошелька', () => {
    let p = playWeek(playWeek(makeProfile(), GOOD), GOOD);
    expect(p.pendingEventId).toBe('vet');
    const care = p.pet.stats.care;
    const wallet = p.wallet;
    const r = must(resolveEvent(p, ctx()));
    p = r.profile;
    expect(p.wallet).toBe(wallet - 15);
    expect(p.period.spent.event).toBe(15);
    expect(p.pet.stats.care).toBe(Math.min(100, care + 20));
    expect(p.pendingEventId).toBeNull();
    expect(r.report.explain.length).toBeGreaterThan(0);
    const again = resolveEvent(p, ctx());
    expect(again.ok).toBe(false);
  });

  it('событие при нехватке монет не блокирует игру: есть варианты', () => {
    let p = playWeek(playWeek(makeProfile(), GOOD), GOOD);
    p = { ...p, wallet: 3 };
    const r = resolveEvent(p, ctx());
    expect(r.ok).toBe(false);
    if (!r.ok) expect(r.error.options!.map((o) => o.id)).toContain('earn');
  });

  it('трата на событие не считается нарушением плана', () => {
    let p = playWeek(playWeek(makeProfile(), GOOD), GOOD);
    p = must(resolveEvent(p, ctx())).profile;
    p = playWeek(p, GOOD);
    expect(p.history[2].fact.event).toBe(15);
    expect(p.history[2].checks.mustWithin).toBe(true);
  });

  it('подарок дня: один раз в сутки, без штрафов за пропуск', () => {
    let p = makeProfile();
    const r = must(claimDaily(p, ctx('2026-09-19')));
    p = r.profile;
    expect(p.wallet).toBe(100 + ECON.dailyGift);
    expect(p.ledger.at(-1)).toMatchObject({ kind: 'daily', amount: ECON.dailyGift });
    expect(claimDaily(p, ctx('2026-09-19')).ok).toBe(false);
    expect(claimDaily(p, ctx('2026-09-25')).ok).toBe(true); // пропустил дни — просто забрал
  });

  it('бонус взрослого: шаг 5 и не больше 20 за неделю; это только игровые монеты', () => {
    let p = makeProfile();
    expect(adultBonusLeft(p)).toBe(20);
    p = must(grantAdultBonus(p, 10, ADULT_BONUS_REASONS[0])).profile;
    expect(grantAdultBonus(p, 15, ADULT_BONUS_REASONS[0]).ok).toBe(false);
    expect(grantAdultBonus(p, 7, ADULT_BONUS_REASONS[0]).ok).toBe(false);
    p = must(grantAdultBonus(p, 10, ADULT_BONUS_REASONS[1])).profile;
    expect(adultBonusLeft(p)).toBe(0);
    expect(p.wallet).toBe(120);
    expect(p.ledger.at(-1)!.title).toContain('Бонус от взрослого');
    // лимит обновляется на новой неделе
    const next = playWeek(p, GOOD);
    expect(adultBonusLeft(next)).toBe(20);
  });
});
