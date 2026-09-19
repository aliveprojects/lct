import { ECON, STATS, coinsText, pointsText, stageForGrowth } from './econ';
import {
  STAT_LABEL,
  addTx,
  applyStat,
  freshPeriod,
  petExpression,
  savingsTotal,
  scheduleEvent,
} from './profile';
import type { Change, Ctx, Outcome, PeriodResult, PetStat, Profile, ReviewLine, ShortageOption, Stage } from './types';
import { clone, done, fail, walletChange } from './util';

const DECAY_WHY = 'С каждой неделей питомец немного устаёт и хочет есть — так бывает.';

/** Считает итоги периода: А (0–3) + Б (0–3) + В (0–2) = до 8 очков роста. Формулы — в docs/ECONOMY.md. */
export function scorePeriod(p: Profile) {
  const per = p.period;
  const net = Math.max(0, per.saved - per.withdrawn);
  const food = per.needs.food > 0;
  const care = per.needs.care > 0;
  const prev = p.history[p.history.length - 1];

  // A. Обязательное: еда и уход.
  const needs = food && care ? 3 : food || care ? 1 : 0;
  // B. План и факт. Очки даём только если неделя не пустая: подтвердить план и ничего не делать — не решение.
  const active = per.spent.must + per.spent.want + net > 0;
  const mustWithin = per.spent.must > 0 && per.spent.must <= per.plan.must;
  const wantWithin = active && per.spent.want <= per.plan.want;
  const savedPlan = per.plan.save > 0 && net >= per.plan.save;
  const plan = active ? (mustWithin ? 1 : 0) + (wantWithin ? 1 : 0) + (savedPlan ? 1 : 0) : 0;
  // C. Регулярные накопления.
  const savedSome = net >= ECON.minSaving;
  const regular = savedSome && !!prev && prev.fact.save >= ECON.minSaving;
  const saving = (savedSome ? 1 : 0) + (regular ? 1 : 0);

  return {
    net,
    food,
    care,
    mustWithin,
    wantWithin,
    savedSome,
    savedPlan,
    regular,
    score: { needs, plan, saving, total: needs + plan + saving },
  };
}

function buildLines(p: Profile, s: ReturnType<typeof scorePeriod>): ReviewLine[] {
  const per = p.period;
  const lines: ReviewLine[] = [];
  if (s.food && s.care) lines.push({ id: 'needs', status: 'ok', points: 3, text: 'Еда и уход куплены. Питомец сыт и ухожен.' });
  else if (s.food || s.care)
    lines.push({
      id: 'needs',
      status: 'part',
      points: 1,
      text: `Куплено только: ${s.food ? 'еда' : 'уход'}. ${s.food ? 'Уход' : 'Еда'} тоже нужен.`,
      tip: `Начни новую неделю с ${s.food ? 'ухода (купание, расчёска)' : 'еды (обед, ужин)'}.`,
    });
  else lines.push({ id: 'needs', status: 'todo', points: 0, text: 'На этой неделе не было ни еды, ни ухода.', tip: 'Начни новую неделю с нужного: оно есть в магазине.' });

  lines.push(
    s.mustWithin
      ? { id: 'must', status: 'ok', points: 1, text: `Нужное: потрачено ${per.spent.must} из ${per.plan.must}. В плане!` }
      : per.spent.must === 0
        ? { id: 'must', status: 'todo', points: 0, text: 'На нужное ничего не потрачено.', tip: 'Питомцу нужны еда и уход — они есть в магазине.' }
        : { id: 'must', status: 'part', points: 0, text: `На нужное ушло ${per.spent.must}, а по плану было ${per.plan.must}.`, tip: 'В следующий раз заложи на нужное побольше.' },
  );
  lines.push(
    s.wantWithin
      ? { id: 'want', status: 'ok', points: 1, text: per.spent.want === 0 ? 'Хочется: ты не потратил лишнего. Это тоже хороший выбор!' : `Хочется: потрачено ${per.spent.want} из ${per.plan.want}. В плане!` }
      : { id: 'want', status: 'part', points: 0, text: `На «Хочется» ушло ${per.spent.want} при плане ${per.plan.want}.`, tip: 'В следующий раз выбери подешевле или отложи покупку на потом — это не ошибка.' },
  );
  lines.push(
    per.plan.save === 0
      ? { id: 'save-plan', status: 'todo', points: 0, text: 'В плане не было копилки.', tip: 'Попробуй заложить в план хоть немного на мечту.' }
      : s.savedPlan
        ? { id: 'save-plan', status: 'ok', points: 1, text: `Копилка: по плану ${per.plan.save}, отложено ${s.net}. Как задумано!` }
        : { id: 'save-plan', status: 'part', points: 0, text: `По плану нужно было отложить ${per.plan.save}, а отложено ${s.net}.`, tip: 'Откладывай сразу после плана, пока монеты не потрачены.' },
  );
  lines.push(
    s.savedSome
      ? { id: 'save', status: 'ok', points: 1, text: `В копилку отложено ${coinsText(s.net)}.` }
      : { id: 'save', status: 'todo', points: 0, text: 'В копилку ничего не отложено.', tip: `Даже ${ECON.minSaving} монет в неделю — уже привычка.` },
  );
  lines.push(
    s.regular
      ? { id: 'regular', status: 'ok', points: 1, text: 'Ты копишь уже несколько недель подряд — это привычка!' }
      : { id: 'regular', status: 'part', points: 0, text: 'Регулярность: чем чаще ты копишь неделя за неделей, тем быстрее растёт питомец.' },
  );
  return lines;
}

export function endPeriod(p0: Profile, ctx: Ctx): Outcome<{ result: PeriodResult }> {
  if (!p0.period.planConfirmed) return fail('PLAN_REQUIRED', 'Неделю можно завершить после плана: сначала составь и подтверди его.');

  const p = clone(p0);
  const per = p.period;
  const s = scorePeriod(p);
  const stageBefore = p.pet.stage;
  const growthBefore = p.pet.growth;
  const statsBefore = { ...p.pet.stats };
  const walletBefore = p.wallet;

  // 1. Показатели питомца немного снижаются, но не ниже нижней границы.
  for (const stat of STATS) {
    const cur = p.pet.stats[stat];
    if (cur > ECON.statFloor) p.pet.stats[stat] = Math.max(ECON.statFloor, cur - ECON.periodDecay[stat]);
  }
  // 2. Держался плана — питомец гордится (настроение растёт).
  const kept = s.mustWithin && s.wantWithin;
  if (kept) applyStat(p.pet, 'mood', ECON.planKeptMood);
  const statsAfter = { ...p.pet.stats };

  // 3. Рост питомца копится и никогда не уменьшается.
  p.pet.growth += s.score.total;
  p.pet.stage = stageForGrowth(p.pet.growth);
  const stagesDef = ctx.content.pet.stages;
  const stageName = (st: Stage) => stagesDef.find((x) => x.id === st)?.name ?? '';
  const stageUp = p.pet.stage !== stageBefore;

  const lines = buildLines(p0, s);
  const head = stageUp
    ? `Питомец вырос! Теперь он «${stageName(p.pet.stage)}».`
    : s.score.total >= 6
      ? 'Отличная неделя!'
      : s.score.total >= 3
        ? 'Хорошая неделя. Есть что улучшить.'
        : 'Ничего страшного — на новой неделе всё получится.';
  const summary = `${head} Ты получил ${pointsText(s.score.total)} роста.`;

  const result: PeriodResult = {
    index: per.index,
    plan: { ...per.plan },
    fact: { must: per.spent.must, want: per.spent.want, save: s.net, event: per.spent.event, earned: per.earned },
    needs: { food: s.food, care: s.care },
    checks: { mustWithin: s.mustWithin, wantWithin: s.wantWithin, savedSome: s.savedSome, savedPlan: s.savedPlan, regular: s.regular },
    score: s.score,
    growthBefore,
    growthAfter: p.pet.growth,
    stageBefore,
    stageAfter: p.pet.stage,
    balanceEnd: p.wallet,
    statsBefore,
    statsAfter,
    lines,
    summary,
  };
  p.history.push(result);

  // 4. Новая неделя: карманные монеты, свежий план, возможное событие.
  p.period = freshPeriod(per.index + 1);
  p.wallet += ECON.weeklyAllowance;
  addTx(p, { kind: 'allowance', title: 'Карманные монеты на неделю', amount: ECON.weeklyAllowance });
  scheduleEvent(p, ctx.content);

  const expr = petExpression(p.pet.stats);
  p.pet.reason = { text: `Началась новая неделя! ${expr.hint}` };

  const changes: Change[] = [
    { kind: 'growth', label: 'Очки роста', before: growthBefore, after: p.pet.growth, delta: s.score.total, why: 'Очки за заботу, план и копилку. Они не пропадают.' },
  ];
  for (const stat of STATS as PetStat[]) {
    if (statsBefore[stat] !== statsAfter[stat])
      changes.push({ kind: 'stat', stat, label: STAT_LABEL[stat], before: statsBefore[stat], after: statsAfter[stat], delta: statsAfter[stat] - statsBefore[stat], why: kept && stat === 'mood' ? 'Ты держался плана — питомец гордится (+8), но неделя всё равно немного утомила.' : DECAY_WHY });
  }
  changes.push(walletChange('Монеты', walletBefore, p.wallet, `Карманные монеты на новую неделю. Остаток ${coinsText(walletBefore)} тоже остался у тебя.`));

  return done(
    p,
    {
      title: `Неделя ${per.index} завершена`,
      tone: 'good',
      changes,
      explain: summary,
      next: { label: 'Смотреть итоги', route: '/review' },
    },
    { result },
  );
}

/** Ответить на непредвиденное событие: заплатить из кошелька. */
export function resolveEvent(p0: Profile, ctx: Ctx): Outcome {
  const ev = ctx.content.events.find((e) => e.id === p0.pendingEventId);
  if (!ev) return fail('NO_EVENT', 'Сейчас непредвиденных трат нет.');
  if (p0.wallet < ev.cost) {
    const options: ShortageOption[] = [
      { id: 'earn', label: 'Заработать монеты', hint: 'Выполни задание — и вернись.', route: '/tasks' },
    ];
    if (savingsTotal(p0) > 0)
      options.push({ id: 'savings', label: 'Взять из копилки', hint: 'Для таких случаев копилка и нужна. Но сначала подумай.', route: '/goals' });
    return fail('NOT_ENOUGH', `Не хватает ${coinsText(ev.cost - p0.wallet)}.`, { missing: ev.cost - p0.wallet, options });
  }
  const p = clone(p0);
  const before = p.wallet;
  p.wallet -= ev.cost;
  p.period.spent.event += ev.cost;
  p.pendingEventId = null;
  p.resolvedEvents.push(ev.id);
  addTx(p, { kind: 'event', title: ev.title, amount: -ev.cost, dir: 'event' });

  const changes: Change[] = [walletChange('Монеты', before, p.wallet, `Непредвиденная трата: ${ev.title.toLowerCase()}.`)];
  for (const stat of STATS) {
    const eff = ev.effects[stat];
    if (!eff) continue;
    const r = applyStat(p.pet, stat, eff);
    changes.push({ kind: 'stat', stat, label: STAT_LABEL[stat], before: r.before, after: r.after, delta: r.gain, why: 'Врач помог — питомец в порядке.' });
  }
  p.pet.reason = { text: 'Спасибо, что позаботился! Мне уже лучше.', stat: 'care' };
  return done(p, {
    title: 'Питомцу помогли',
    tone: 'care',
    changes,
    explain: `${ev.tip} Эта трата не считается ошибкой плана.`,
  });
}
