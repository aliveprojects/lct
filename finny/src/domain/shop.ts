import type { Item } from '../content/types';
import { ECON, STATS, coinsText } from './econ';
import { DIRECTION_LABEL, STAT_LABEL, addTx, applyStat, envelope, savingsTotal } from './profile';
import { availableTasks } from './tasks';
import type { Change, Ctx, Outcome, PetStat, Profile, ShortageOption } from './types';
import { clone, done, fail, walletChange } from './util';

export interface EffectPreview {
  stat: PetStat;
  before: number;
  after: number;
  gain: number;
  wasted: number;
}

export interface PurchasePreview {
  item: Item;
  price: number;
  kind: 'must' | 'want';
  categoryLabel: string;
  effects: EffectPreview[];
  planConfirmed: boolean;
  affordable: boolean;
  missing: number;
  envelope: { planned: number; used: number; left: number; leftAfter: number };
  overPlanBy: number;
  wasteful: boolean;
  alreadyOwned: boolean;
  inWishlist: boolean;
  balanceAfter: number;
}

export function previewPurchase(p: Profile, item: Item): PurchasePreview {
  const env = envelope(p, item.kind);
  const effects: EffectPreview[] = STATS.filter((s) => item.effects[s]).map((stat) => {
    const before = p.pet.stats[stat];
    const raw = item.effects[stat] ?? 0;
    const after = Math.min(ECON.statMax, before + raw);
    return { stat, before, after, gain: after - before, wasted: raw - (after - before) };
  });
  const alreadyOwned = !!item.accessory && p.pet.ownedAccessories.includes(item.accessory);
  const overPlanBy = Math.max(0, item.price - env.left);
  return {
    item,
    price: item.price,
    kind: item.kind,
    categoryLabel: DIRECTION_LABEL[item.kind],
    effects,
    planConfirmed: p.period.planConfirmed,
    affordable: p.wallet >= item.price,
    missing: Math.max(0, item.price - p.wallet),
    envelope: { planned: env.planned, used: env.used, left: env.left, leftAfter: Math.max(0, env.left - item.price) },
    overPlanBy,
    wasteful: effects.length > 0 && effects.every((e) => e.before >= ECON.statHigh),
    alreadyOwned,
    inWishlist: p.wishlist.includes(item.id),
    balanceAfter: p.wallet - item.price,
  };
}

/** Что можно сделать, если монет не хватает: понятные варианты вместо тупика. */
export function shortageOptions(p: Profile, item: Item, ctx: Ctx): ShortageOption[] {
  const options: ShortageOption[] = [];
  const missing = item.price - p.wallet;
  const openTasks = availableTasks(p, ctx).filter((t) => (p.tasks[t.id]?.stars ?? 0) < 3);
  if (openTasks.length) {
    const best = Math.max(...openTasks.map((t) => t.rewards['3']));
    options.push({ id: 'earn', label: 'Заработать монеты', hint: `За задания дают до ${coinsText(best)}.`, route: '/tasks' });
  }
  const cheaper = ctx.content.items
    .filter((i) => i.kind === item.kind && i.price <= p.wallet && i.id !== item.id && !(i.accessory && p.pet.ownedAccessories.includes(i.accessory)))
    .sort((a, b) => b.price - a.price)
    .slice(0, 3)
    .map((i) => i.id);
  if (cheaper.length) options.push({ id: 'cheaper', label: 'Выбрать подешевле', hint: 'Есть похожие вещи, которые ты можешь купить сейчас.', itemIds: cheaper });
  if (item.kind === 'want' && !p.wishlist.includes(item.id))
    options.push({ id: 'wishlist', label: 'Отложить в список желаний', hint: 'Вернёшься к этой вещи на следующей неделе. Это не ошибка.' });
  if (item.kind === 'must' && savingsTotal(p) + p.wallet >= item.price)
    options.push({ id: 'savings', label: 'Взять из копилки', hint: `Не хватает ${coinsText(missing)}. Копилку можно потратить, но сначала подумай.`, route: '/goals' });
  options.push({ id: 'wait', label: 'Подождать новую неделю', hint: 'В начале недели приходят карманные монеты.' });
  return options;
}

export interface BuyOptions {
  confirmOverPlan?: boolean;
}

export function buy(p0: Profile, itemId: string, opts: BuyOptions, ctx: Ctx): Outcome {
  const item = ctx.content.items.find((i) => i.id === itemId);
  if (!item) return fail('ITEM_NOT_FOUND', 'Такого товара нет.');
  if (!p0.period.planConfirmed) return fail('PLAN_REQUIRED', 'Сначала составь план недели — так ты будешь знать, сколько можно потратить.');
  const pv = previewPurchase(p0, item);
  if (pv.alreadyOwned) return fail('ALREADY_OWNED', 'Это украшение у питомца уже есть.');
  if (!pv.affordable)
    return fail('NOT_ENOUGH', `Не хватает ${coinsText(pv.missing)}.`, { missing: pv.missing, options: shortageOptions(p0, item, ctx) });
  if (item.kind === 'want' && pv.overPlanBy > 0 && !opts.confirmOverPlan)
    return fail('CONFIRM_OVER_PLAN', `Этого нет в плане: на «Хочется» не хватает ${coinsText(pv.overPlanBy)}.`, { overBy: pv.overPlanBy });

  const p = clone(p0);
  const before = p.wallet;
  p.wallet -= item.price;
  p.period.spent[item.kind] += item.price;
  if (item.need === 'food') p.period.needs.food += 1;
  if (item.need === 'care') p.period.needs.care += 1;
  p.wishlist = p.wishlist.filter((id) => id !== item.id);
  if (item.accessory) {
    p.pet.ownedAccessories.push(item.accessory);
    p.pet.appearance.accessory = item.accessory;
  }
  addTx(p, { kind: 'purchase', title: item.name, amount: -item.price, dir: item.kind, itemId: item.id });

  const changes: Change[] = [
    walletChange(
      'Монеты',
      before,
      p.wallet,
      `Ты заплатил за «${item.name}». Это ${item.kind === 'must' ? 'нужное' : 'желаемое'}.`,
    ),
  ];
  let main: { stat: PetStat; gain: number } | null = null;
  for (const e of pv.effects) {
    const r = applyStat(p.pet, e.stat, item.effects[e.stat] ?? 0);
    changes.push({ kind: 'stat', stat: e.stat, label: STAT_LABEL[e.stat], before: r.before, after: r.after, delta: r.gain, why: r.gain > 0 ? item.note : 'Этот показатель уже почти полный — лишнее не пригодилось.' });
    if (!main || r.gain > main.gain) main = { stat: e.stat, gain: r.gain };
  }
  const envAfter = envelope(p, item.kind);
  changes.push({
    kind: 'plan',
    label: `Конверт «${DIRECTION_LABEL[item.kind]}»`,
    before: pv.envelope.left,
    after: envAfter.left,
    delta: envAfter.left - pv.envelope.left,
    why:
      envAfter.over > 0
        ? `Ты потратил на ${coinsText(envAfter.over)} больше, чем запланировал.`
        : `В конверте осталось ${coinsText(envAfter.left)} из ${envAfter.planned}.`,
  });
  p.pet.reason = { text: `${item.note}${main && main.gain > 0 ? ` ${STAT_LABEL[main.stat]} +${main.gain}.` : ''}`, stat: main?.stat, delta: main?.gain };

  const foodDone = p.period.needs.food > 0;
  const careDone = p.period.needs.care > 0;
  let next: { label: string; route: string } | undefined;
  let hint: string;
  if (item.kind === 'must') {
    if (foodDone && careDone) {
      hint = 'Нужное на эту неделю обеспечено. Теперь можно отложить в копилку или порадовать питомца.';
      next = { label: 'В копилку', route: '/goals' };
    } else {
      hint = `Ещё нужно купить ${foodDone ? 'уход (купание, расчёска)' : 'еду (обед, ужин)'}.`;
      next = { label: 'Ещё в магазин', route: '/shop' };
    }
  } else {
    hint = 'Желаемое радует, но не заменяет заботу. Не забудь про копилку.';
    next = { label: 'В копилку', route: '/goals' };
  }
  const over = envAfter.over > 0;
  return done(p, {
    title: `${item.name}: куплено!`,
    tone: 'good',
    changes,
    explain: over ? `${hint} План на «${DIRECTION_LABEL[item.kind]}» превышен — на следующей неделе заложи побольше.` : hint,
    next,
  });
}

export function toggleWishlist(p0: Profile, itemId: string, ctx: Ctx): Outcome {
  const item = ctx.content.items.find((i) => i.id === itemId);
  if (!item) return fail('ITEM_NOT_FOUND', 'Такого товара нет.');
  const p = clone(p0);
  const has = p.wishlist.includes(itemId);
  p.wishlist = has ? p.wishlist.filter((id) => id !== itemId) : [...p.wishlist, itemId];
  return done(p, {
    title: has ? 'Убрано из списка желаний' : 'Добавлено в список желаний',
    tone: 'info',
    changes: [],
    explain: has ? 'Ты передумал — и это нормально.' : 'Подумай до следующей недели. Если всё ещё захочется — запланируй эту покупку.',
  });
}

export function wearAccessory(p0: Profile, accessory: string): Outcome {
  if (accessory !== 'none' && !p0.pet.ownedAccessories.includes(accessory)) return fail('ITEM_NOT_FOUND', 'Этого украшения у питомца пока нет.');
  const p = clone(p0);
  p.pet.appearance.accessory = accessory;
  return done(p, { title: 'Готово!', tone: 'info', changes: [], explain: 'Питомец переоделся.' });
}

export const wishlistItems = (p: Profile, ctx: Ctx): Item[] =>
  p.wishlist.map((id) => ctx.content.items.find((i) => i.id === id)).filter((i): i is Item => !!i);
