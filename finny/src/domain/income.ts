import { ECON, coinsText } from './econ';
import { addTx } from './profile';
import type { Ctx, Outcome, Profile } from './types';
import { clone, done, fail, isInt, walletChange } from './util';

export const canClaimDaily = (p: Profile, ctx: Ctx): boolean => p.dailyGiftDate !== ctx.today;

/** «Подарок дня»: без серий и штрафов за пропуск — просто маленький приятный доход. */
export function claimDaily(p0: Profile, ctx: Ctx): Outcome {
  if (!canClaimDaily(p0, ctx)) return fail('ALREADY_TODAY', 'Подарок дня ты уже забрал. Завтра будет новый!');
  const p = clone(p0);
  const before = p.wallet;
  p.wallet += ECON.dailyGift;
  p.period.earned += ECON.dailyGift;
  p.dailyGiftDate = ctx.today;
  addTx(p, { kind: 'daily', title: 'Подарок дня', amount: ECON.dailyGift });
  p.pet.reason = { text: 'Ура, подарок! Спасибо, что заглянул ко мне.' };
  return done(p, {
    title: 'Подарок дня!',
    tone: 'good',
    changes: [walletChange('Монеты', before, p.wallet, 'Каждый день можно забрать небольшой подарок. Это игровые монеты, они не настоящие.')],
    explain: 'Доход — это монеты, которые к тебе приходят. Планируй их вместе с остальными.',
  });
}

export const adultBonusLeft = (p: Profile): number => Math.max(0, ECON.adultBonusCap - p.period.adultBonus);

export const ADULT_BONUS_REASONS = ['Помощь по дому', 'Хорошая идея', 'Аккуратность', 'Добрый поступок'] as const;

/**
 * Поощрение от взрослого: до 20 монет за неделю, шаг 5. Это только игровая валюта —
 * без реальной стоимости и без обмена на деньги или призы.
 */
export function grantAdultBonus(p0: Profile, amount: number, reason: string): Outcome {
  if (!isInt(amount) || amount <= 0 || amount % ECON.adultBonusStep !== 0) return fail('INVALID_AMOUNT', `Бонус — кратен ${ECON.adultBonusStep} монетам.`);
  if (amount > adultBonusLeft(p0)) return fail('BONUS_LIMIT', `На этой неделе можно добавить ещё не больше ${coinsText(adultBonusLeft(p0))}.`, { max: adultBonusLeft(p0) });
  const p = clone(p0);
  const before = p.wallet;
  p.wallet += amount;
  p.period.earned += amount;
  p.period.adultBonus += amount;
  addTx(p, { kind: 'bonus', title: `Бонус от взрослого: ${reason}`, amount });
  p.pet.reason = { text: 'Тебя похвалили — я так рад за тебя!' };
  return done(p, {
    title: 'Бонус от взрослого',
    tone: 'good',
    changes: [walletChange('Монеты', before, p.wallet, `Причина: ${reason.toLowerCase()}.`)],
    explain: 'Игровые монеты нельзя обменять на деньги или призы — это просто похвала внутри игры.',
  });
}
