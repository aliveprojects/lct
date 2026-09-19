import type { Content, TaskDef } from './types';

/** Проверяет целостность учебного контента. Возвращает список проблем (пусто — всё хорошо). */
export function validateContent(c: Content): string[] {
  const errs: string[] = [];
  const dup = (label: string, ids: string[]) => {
    const seen = new Set<string>();
    for (const id of ids) {
      if (seen.has(id)) errs.push(`${label}: повторяется id «${id}»`);
      seen.add(id);
    }
  };
  dup('items', c.items.map((i) => i.id));
  dup('goals', c.goals.map((g) => g.id));
  dup('tasks', c.tasks.map((t) => t.id));
  dup('glossary', c.glossary.map((g) => g.id));
  dup('events', c.events.map((e) => e.id));

  const accessoryIds = new Set(c.pet.accessories.map((a) => a.id));
  for (const i of c.items) {
    if (!Number.isInteger(i.price) || i.price <= 0) errs.push(`item ${i.id}: цена должна быть целым положительным числом`);
    if (!i.name || !i.note || !i.icon) errs.push(`item ${i.id}: не заполнены name/note/icon`);
    if (i.kind === 'must' && !i.need) errs.push(`item ${i.id}: у обязательного товара нужно указать need (food/care)`);
    if (Object.keys(i.effects).length === 0) errs.push(`item ${i.id}: нет эффектов`);
    if (i.accessory && !accessoryIds.has(i.accessory)) errs.push(`item ${i.id}: неизвестное украшение ${i.accessory}`);
  }
  for (const g of c.goals) if (!Number.isInteger(g.cost) || g.cost <= 0 || g.cost % 5 !== 0) errs.push(`goal ${g.id}: цена должна быть кратна 5`);
  for (const e of c.events) if (!Number.isInteger(e.period) || e.period < 1) errs.push(`event ${e.id}: неверный период`);
  for (const t of c.tasks) errs.push(...validateTask(t));
  return errs;
}

function validateTask(t: TaskDef): string[] {
  const errs: string[] = [];
  const e = (m: string) => errs.push(`task ${t.id}: ${m}`);
  if (!t.title || !t.intro || !t.learn || !t.skill) e('не заполнены title/intro/learn/skill');
  if (!t.competencies?.length) e('не указаны компетенции');
  if (!(t.rewards['1'] > 0 && t.rewards['2'] >= t.rewards['1'] && t.rewards['3'] >= t.rewards['2'])) e('награды должны расти: 1 ≤ 2 ≤ 3');
  switch (t.type) {
    case 'sort':
      if (t.items.length < 4) e('слишком мало предметов');
      for (const g of ['need', 'want'] as const) if (!t.items.some((i) => i.group === g)) e(`нет предметов группы ${g}`);
      for (const i of t.items) if (!i.why) e(`предмет ${i.id} без объяснения`);
      break;
    case 'cart': {
      const needs = t.items.filter((i) => i.need).reduce((a, i) => a + i.price, 0);
      if (needs + t.minLeft > t.budget) e('на нужное не хватает бюджета');
      if (!t.items.some((i) => !i.need)) e('нет необязательных товаров');
      break;
    }
    case 'allocate': {
      const mins = t.rules.filter((r) => r.op === 'min').reduce((a, r) => a + r.value, 0);
      if (mins > t.total) e('правила невыполнимы: минимумы больше суммы');
      for (const r of t.rules) if (!t.buckets.some((b) => b.id === r.bucket)) e(`правило ${r.id}: неизвестный конверт`);
      break;
    }
    case 'calc':
      if (!t.questions.length) e('нет вопросов');
      for (const q of t.questions) if (!Number.isFinite(q.answer) || !q.explain) e(`вопрос ${q.id}: нужен числовой ответ и объяснение`);
      break;
    case 'sim':
      if (t.freePerWeek < t.step) e('свободных монет меньше шага');
      break;
    case 'story': {
      if (!t.nodes[t.start]) e('нет стартового узла');
      let hasBad = false;
      let hasGood = false;
      for (const [id, n] of Object.entries(t.nodes)) {
        if (n.choices.length < 2) e(`узел ${id}: нужно минимум 2 варианта`);
        for (const c of n.choices) {
          if (!c.end && (!c.next || !t.nodes[c.next])) e(`узел ${id}: вариант «${c.text}» ведёт в никуда`);
          if (c.end) {
            if (c.end.stars === 1) hasBad = true;
            if (c.end.stars === 3) hasGood = true;
            if (!c.end.explain || !c.end.consequence) e(`узел ${id}: у концовки нет объяснения`);
          }
        }
      }
      if (!hasBad || !hasGood) e('нужна и удачная, и неудачная концовка');
      break;
    }
  }
  return errs;
}
