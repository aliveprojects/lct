import { describe, expect, it } from 'vitest';
import { confirmPlan } from '../src/domain/plan';
import { envelope } from '../src/domain/profile';
import { buy, previewPurchase, toggleWishlist } from '../src/domain/shop';
import { content } from '../src/content';
import type { Profile } from '../src/domain/types';
import { ctx, item, makeProfile, must } from './helpers';

const planned = (plan = { must: 40, want: 30, save: 20 }): Profile => must(confirmPlan(makeProfile(), plan)).profile;

describe('Покупки и расходы', () => {
  it('обязательная покупка: списывает монеты, меняет показатель и пишет историю', () => {
    const r = must(buy(planned(), 'meal', {}, ctx()));
    const p = r.profile;
    expect(p.wallet).toBe(85);
    expect(p.period.spent.must).toBe(15);
    expect(p.period.needs.food).toBe(1);
    expect(p.pet.stats.satiety).toBe(90);
    expect(p.ledger.at(-1)).toMatchObject({ kind: 'purchase', title: 'Обед', amount: -15, balance: 85, dir: 'must' });
    // объяснение: баланс, показатель питомца и конверт
    const kinds = r.report.changes.map((c) => c.kind);
    expect(kinds).toEqual(expect.arrayContaining(['wallet', 'stat', 'plan']));
    expect(r.report.changes.every((c) => c.why.length > 0)).toBe(true);
    expect(p.pet.reason?.text).toContain('Сытость +30');
  });

  it('необязательная покупка: списывает из конверта «Хочется»', () => {
    const p = must(buy(planned(), 'ball', {}, ctx())).profile;
    expect(p.period.spent.want).toBe(12);
    expect(envelope(p, 'want').left).toBe(18);
    expect(p.pet.stats.mood).toBe(90);
  });

  it('до подтверждения плана покупать нельзя, и объясняется почему', () => {
    const r = buy(makeProfile(), 'meal', {}, ctx());
    expect(r.ok).toBe(false);
    if (!r.ok) expect(r.error.code).toBe('PLAN_REQUIRED');
  });

  it('нехватка средств: покупка отклонена, баланс не меняется, есть варианты', () => {
    const p0 = planned();
    const before = JSON.stringify(p0);
    const r = buy(p0, 'castle', {}, ctx()); // 120 > 100
    expect(r.ok).toBe(false);
    if (!r.ok) {
      expect(r.error.code).toBe('NOT_ENOUGH');
      expect(r.error.missing).toBe(20);
      const ids = r.error.options!.map((o) => o.id);
      expect(ids).toEqual(expect.arrayContaining(['earn', 'cheaper', 'wishlist', 'wait']));
    }
    expect(JSON.stringify(p0)).toBe(before);
  });

  it('баланс никогда не уходит в минус, сколько бы ни покупали', () => {
    let p = planned({ must: 40, want: 40, save: 20 });
    for (let i = 0; i < 40; i++) {
      for (const it of content.items) {
        const r = buy(p, it.id, { confirmOverPlan: true }, ctx());
        if (r.ok) p = r.profile;
        expect(p.wallet).toBeGreaterThanOrEqual(0);
      }
    }
  });

  it('покупка «Хочется» сверх плана требует отдельного подтверждения', () => {
    const p = planned({ must: 40, want: 10, save: 20 });
    const r = buy(p, 'ball', {}, ctx()); // 12 > 10
    expect(r.ok).toBe(false);
    if (!r.ok) {
      expect(r.error.code).toBe('CONFIRM_OVER_PLAN');
      expect(r.error.overBy).toBe(2);
    }
    const okBuy = must(buy(p, 'ball', { confirmOverPlan: true }, ctx()));
    expect(okBuy.profile.period.spent.want).toBe(12);
    expect(okBuy.report.explain).toContain('превышен');
  });

  it('украшение надевается сразу и покупается один раз', () => {
    const p = must(buy(planned(), 'hat', {}, ctx())).profile;
    expect(p.pet.appearance.accessory).toBe('hat');
    expect(p.pet.ownedAccessories).toContain('hat');
    const again = buy(p, 'hat', {}, ctx());
    expect(again.ok).toBe(false);
    if (!again.ok) expect(again.error.code).toBe('ALREADY_OWNED');
  });

  it('показатели питомца ограничены 100, а предпросмотр предупреждает о лишнем', () => {
    let p = planned();
    p = must(buy(p, 'meal', {}, ctx())).profile; // 60 → 90
    const pv = previewPurchase(p, item('meal'));
    expect(pv.effects[0]).toMatchObject({ before: 90, after: 100, gain: 10, wasted: 20 });
    expect(pv.wasteful).toBe(true);
    const after = must(buy(p, 'meal', {}, ctx())).profile;
    expect(after.pet.stats.satiety).toBe(100);
  });

  it('предпросмотр показывает цену, категорию и влияние до покупки', () => {
    const pv = previewPurchase(planned(), item('bath'));
    expect(pv).toMatchObject({ price: 10, kind: 'must', categoryLabel: 'Нужное', affordable: true, balanceAfter: 90 });
    expect(pv.effects[0]).toMatchObject({ stat: 'care', gain: 30 });
  });

  it('список желаний: добавить, убрать, покупка убирает из списка', () => {
    let p = must(toggleWishlist(planned(), 'ball', ctx())).profile;
    expect(p.wishlist).toEqual(['ball']);
    p = must(buy(p, 'ball', {}, ctx())).profile;
    expect(p.wishlist).toEqual([]);
    p = must(toggleWishlist(p, 'cake', ctx())).profile;
    p = must(toggleWishlist(p, 'cake', ctx())).profile;
    expect(p.wishlist).toEqual([]);
  });
});
