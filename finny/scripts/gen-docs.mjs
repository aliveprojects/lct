// Генерирует таблицы документации прямо из учебного контента (src/content/*.json),
// чтобы документ и приложение не расходились. Запуск: npm run docs
import fs from 'node:fs';
import path from 'node:path';

const ROOT = path.resolve(import.meta.dirname, '..');
const C = path.join(ROOT, 'src/content');
const read = (f) => JSON.parse(fs.readFileSync(path.join(C, f), 'utf8'));
const tasks = fs.readdirSync(path.join(C, 'tasks')).filter((f) => f.endsWith('.json')).map((f) => read(`tasks/${f}`));
const topics = read('topics.json');
const items = read('items.json');
const goals = read('goals.json');
const competencies = read('competencies.json');
const glossary = read('glossary.json');
const events = read('events.json');
const pet = read('pet.json');

const TYPE = { allocate: 'разложить монеты по конвертам', sort: 'рассортировать покупки', cart: 'собрать корзину', calc: 'посчитать', sim: 'симулятор накоплений', story: 'история с выбором и последствиями' };
const md = (s) => String(s).replace(/\|/g, '\\|').replace(/\n/g, ' ');
const order = (a, b) => a.unlockPeriod - b.unlockPeriod || a.id.localeCompare(b.id);

function correctLogic(t) {
  switch (t.type) {
    case 'allocate': return t.rules.map((r) => `${t.buckets.find((b) => b.id === r.bucket).label} ${r.op === 'min' ? '≥' : '≤'} ${r.value}`).join('; ') + `; всего ${t.total}`;
    case 'sort': return t.items.map((i) => `${i.label} → ${t.groups.find((g) => g.id === i.group).label}`).join('; ');
    case 'cart': return `Купить все нужные (${t.items.filter((i) => i.need).map((i) => i.label).join(', ')}), уложиться в ${t.budget} и оставить ≥ ${t.minLeft}`;
    case 'calc': return t.questions.map((q) => `${q.answer} ${q.unit}`).join('; ');
    case 'sim': return `Цель ${t.goalCost}; свободно ${t.freePerWeek}/нед.; срок ≤ ${t.targetWeeks} нед. (взнос ≥ ${Math.ceil(t.goalCost / t.targetWeeks / t.step) * t.step})`;
    case 'story': {
      const ends = [];
      const walk = (id, path) => t.nodes[id].choices.forEach((c, i) => (c.end ? ends.push({ path: [...path, c.text], e: c.end }) : walk(c.next, [...path, c.text])));
      walk(t.start, []);
      const best = ends.find((x) => x.e.stars === 3);
      return `Лучший путь: «${best.path.join('» → «')}»`;
    }
  }
}

let out = '# Карта образовательного контента\n\n> Файл создан скриптом `npm run docs` из `src/content/*.json` — не редактируйте вручную.\n\n';
out += '## Компетенции (Единая рамка, начальное общее образование)\n\n| № | Что должен уметь ребёнок |\n|---|---|\n' + competencies.map((c) => `| ${c.id} | ${md(c.text)} |`).join('\n') + '\n\n';
out += '## Задания\n\nВ каждом задании после ответа показывается объяснение — независимо от правильности. Награда за задание выплачивается один раз, при улучшении результата доплачивается разница.\n\n';
for (const topic of topics) {
  out += `### ${topic.title}\n\n| Задание | Ожидаемый навык | Сценарий | Правильная логика | Объяснение для ребёнка («Запомни») | Награда ★/★★/★★★ | Откроется |\n|---|---|---|---|---|---|---|\n`;
  for (const t of tasks.filter((x) => x.topic === topic.id).sort(order))
    out += `| **${md(t.title)}** (\`${t.id}\`) | ${md(t.skill)} | ${TYPE[t.type]}: ${md(t.intro)} | ${md(correctLogic(t))} | ${md(t.learn)} | ${t.rewards['1']}/${t.rewards['2']}/${t.rewards['3']} | неделя ${t.unlockPeriod} |\n`;
  out += '\n';
}
out += '## Соответствие заданий компетенциям\n\n| Задание | Компетенции |\n|---|---|\n' + tasks.sort(order).map((t) => `| ${md(t.title)} | ${t.competencies.join(', ')} |`).join('\n') + '\n\n';
fs.writeFileSync(path.join(ROOT, 'docs/generated/CONTENT-MAP.md'), out);

let cat = '# Каталог покупок, цели, события и словарик\n\n> Файл создан скриптом `npm run docs` из `src/content/*.json` — не редактируйте вручную.\n\n';
cat += '## Покупки\n\n| Товар | Тип | Цена | Что даёт питомцу | Засчитывается как |\n|---|---|---|---|---|\n';
const STAT = { satiety: 'сытость', care: 'чистота и здоровье', mood: 'настроение' };
for (const i of items) cat += `| ${i.name} | ${i.kind === 'must' ? 'нужное' : 'хочется'} | ${i.price} | ${Object.entries(i.effects).map(([k, v]) => `${STAT[k]} +${v}`).join(', ')}${i.accessory ? '; украшение' : ''} | ${i.need === 'food' ? 'еда' : i.need === 'care' ? 'уход' : '—'} |\n`;
cat += '\n## Цели накопления\n\n| Цель | Цена | Описание |\n|---|---|---|\n' + goals.map((g) => `| ${g.title} | ${g.cost} | ${md(g.blurb)} |`).join('\n') + '\n\n';
cat += '## Непредвиденные события\n\n| Событие | Неделя | Цена | Пояснение |\n|---|---|---|---|\n' + events.map((e) => `| ${e.title} | ${e.period} | ${e.cost} | ${md(e.tip)} |`).join('\n') + '\n\n';
cat += '## Внешний вид питомца\n\n' + `Видов: ${pet.species.length} (${pet.species.map((s) => s.name).join(', ')}); цветов: ${pet.colors.length} (${pet.colors.map((s) => s.name).join(', ')}); украшений при создании: ${pet.accessories.filter((a) => a.atStart).length}. Комбинаций при создании: **${pet.species.length * pet.colors.length * pet.accessories.filter((a) => a.atStart).length}**; ещё ${pet.accessories.filter((a) => !a.atStart).length} украшения открываются в магазине. Питомец-талисман: **${pet.defaultPetName}**.\n\n`;
cat += '## Стадии развития\n\n| № | Название | Описание |\n|---|---|---|\n' + pet.stages.map((s) => `| ${s.id} | ${s.name} | ${md(s.blurb)} |`).join('\n') + '\n\n';
cat += '## Словарик\n\n| Термин | Объяснение | Пример |\n|---|---|---|\n' + glossary.map((g) => `| ${md(g.term)} | ${md(g.short)} | ${md(g.example ?? '')} |`).join('\n') + '\n';
fs.writeFileSync(path.join(ROOT, 'docs/generated/CATALOG.md'), cat);
console.log(`Документация обновлена: ${tasks.length} заданий, ${items.length} товаров, ${goals.length} целей, ${glossary.length} терминов.`);
