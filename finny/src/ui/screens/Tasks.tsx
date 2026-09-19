import { useState } from 'preact/hooks';
import { nav } from '../../app/nav';
import { act, useAppState } from '../../app/store';
import { TOPICS, content } from '../../content';
import type { AllocateTask, CalcTask, CartTask, SimTask, SortTask, StoryTask, TaskDef } from '../../content/types';
import { DIRECTIONS, coinsText, sum, weeksText } from '../../domain/econ';
import { isUnlocked, simWeeks, submitTask, walkStory } from '../../domain/tasks';
import type { TaskAnswer, TaskEval } from '../../domain/tasks';
import type { Direction } from '../../domain/types';
import { playSound } from '../../platform/sound';
import { Button, Coin, Confetti, DIR_ICON, Stepper } from '../components/kit';
import { Screen } from '../components/Screen';
import { Icon } from '../icons/Icon';

// ---------- Список заданий ----------

export const StarsView = ({ n }: { n: number }) => (
  <span class="stars" role="img" aria-label={`${n} из 3 звёзд`}>
    {[1, 2, 3].map((i) => (
      <Icon key={i} name="star" size={22} class={i <= n ? '' : 'is-off'} />
    ))}
  </span>
);

export function Tasks() {
  const app = useAppState();
  const p = (app.mode === 'demo' ? app.demo : app.main)!;
  return (
    <Screen title="Задания" hint="Играй в ситуации, выбирай и считай. За задания дают монеты.">
      {app.mode === 'demo' && <p class="note"><Icon name="key" size={22} /> Демо-режим: все задания открыты сразу.</p>}
      {TOPICS.map((t) => (
        <section class="stack" key={t.id}>
          <div class="topic">
            <Icon name={t.icon} size={44} />
            <div>
              <h2 class="h3">{t.title}</h2>
              <small class="muted">{t.blurb}</small>
            </div>
          </div>
          {content.tasks
            .filter((x) => x.topic === t.id)
            .map((task) => {
              const open = isUnlocked(task, p);
              const stars = p.tasks[task.id]?.stars ?? 0;
              return (
                <button key={task.id} type="button" class={`taskrow ${open ? '' : 'is-locked'}`} disabled={!open} onClick={() => nav.go(`/task/${task.id}`)}>
                  <Icon name={open ? 'star' : 'locked'} size={44} />
                  <span class="taskrow__body">
                    <b>{task.title}</b>
                    <small>{open ? task.skill : `Откроется на ${task.unlockPeriod}-й неделе`}</small>
                    {open && <span class="taskrow__meta"><StarsView n={stars} /> <small>до {coinsText(task.rewards['3'])}</small></span>}
                  </span>
                </button>
              );
            })}
        </section>
      ))}
    </Screen>
  );
}

// ---------- Игровые формы ----------

interface PlayProps<T extends TaskDef> {
  def: T;
  submit: (a: TaskAnswer) => void;
}

function SortPlay({ def, submit }: PlayProps<SortTask>) {
  const [ans, setAns] = useState<Record<string, 'need' | 'want'>>({});
  const left = def.items.filter((i) => !ans[i.id]).length;
  return (
    <div class="stack">
      <p class="muted">Нажми «Нужное» или «Хочется» у каждой покупки.</p>
      {def.items.map((it) => (
        <div class="sortrow" key={it.id}>
          <Icon name={it.icon} size={44} />
          <b>{it.label}</b>
          <div class="seg seg--sm" role="group" aria-label={it.label}>
            {def.groups.map((g) => (
              <button key={g.id} type="button" class={`seg__btn ${ans[it.id] === g.id ? 'is-on' : ''}`} aria-pressed={ans[it.id] === g.id} onClick={() => { playSound('tap'); setAns((cur) => ({ ...cur, [it.id]: g.id })); }}>
                {g.label}
              </button>
            ))}
          </div>
        </div>
      ))}
      <Button block disabled={left > 0} onClick={() => submit({ type: 'sort', groups: ans })}>
        {left > 0 ? `Осталось выбрать: ${left}` : 'Проверить'}
      </Button>
    </div>
  );
}

function CartPlay({ def, submit }: PlayProps<CartTask>) {
  const [picked, setPicked] = useState<string[]>([]);
  const total = sum(def.items.filter((i) => picked.includes(i.id)).map((i) => i.price));
  const left = def.budget - total;
  const toggle = (id: string) => setPicked(picked.includes(id) ? picked.filter((x) => x !== id) : [...picked, id]);
  return (
    <div class="stack">
      <div class="card card--total">
        <span>Твои монеты</span>
        <Coin n={def.budget} big />
      </div>
      <div class="grid2">
        {def.items.map((it) => {
          const on = picked.includes(it.id);
          return (
            <button key={it.id} type="button" class={`item item--cart ${on ? 'is-picked' : ''}`} aria-pressed={on} onClick={() => { playSound('tap'); toggle(it.id); }}>
              <Icon name={it.icon} size={56} />
              <b class="item__name">{it.label}</b>
              <span class="item__price"><Icon name="coin" size={20} />{it.price}</span>
              {on && <span class="item__badge"><Icon name="check" size={16} /> В корзине</span>}
            </button>
          );
        })}
      </div>
      <div class={`card ${left < 0 ? 'card--tint-warn' : ''}`} aria-live="polite">
        <div class="kv"><span>В корзине</span><b>{total} из {def.budget}</b></div>
        <div class="kv"><span>{left >= 0 ? 'Останется' : 'Не хватает'}</span><b>{Math.abs(left)}</b></div>
      </div>
      <Button block icon="bag" disabled={picked.length === 0 || left < 0} onClick={() => submit({ type: 'cart', picked })}>
        {left < 0 ? 'Убери что-нибудь' : 'Оплатить'}
      </Button>
    </div>
  );
}

function AllocatePlay({ def, submit }: PlayProps<AllocateTask>) {
  const [amounts, setAmounts] = useState<Record<Direction, number>>({ must: 0, want: 0, save: 0 });
  const used = sum(DIRECTIONS.map((d) => amounts[d]));
  const rest = def.total - used;
  return (
    <div class="stack">
      <div class="card card--total"><span>Всего монет</span><Coin n={def.total} big /></div>
      <p class={`remainder ${rest === 0 ? 'is-zero' : ''}`} aria-live="polite">
        {rest === 0 ? 'Всё разложено!' : <>Осталось разложить: <b>{rest}</b></>}
      </p>
      {def.buckets.map((b) => (
        <div class={`envrow envrow--${b.id}`} key={b.id}>
          <div class="envrow__head">
            <Icon name={DIR_ICON[b.id]} size={40} />
            <div><b>{b.label}</b><small>{b.hint}</small></div>
          </div>
          <Stepper label={b.label} value={amounts[b.id]} max={amounts[b.id] + rest} step={def.step} onChange={(v) => setAmounts((cur) => ({ ...cur, [b.id]: v }))} />
        </div>
      ))}
      <Button block disabled={used === 0} onClick={() => submit({ type: 'allocate', amounts })}>Готово</Button>
    </div>
  );
}

function CalcPlay({ def, submit }: PlayProps<CalcTask>) {
  const [vals, setVals] = useState<Record<string, string>>({});
  const filled = def.questions.every((q) => vals[q.id] !== undefined && vals[q.id] !== '');
  return (
    <div class="stack">
      {def.questions.map((q, i) => (
        <label class="calcrow" key={q.id}>
          <span class="calcrow__q"><b>{i + 1}.</b> {q.text}</span>
          <span class="calcrow__in">
            <input
              class="input input--num"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={4}
              autoComplete="off"
              aria-label={`Ответ на вопрос ${i + 1}`}
              value={vals[q.id] ?? ''}
              onInput={(e) => { const val = (e.target as HTMLInputElement).value.replace(/\D/g, ''); setVals((cur) => ({ ...cur, [q.id]: val })); }}
            />
            <small>{q.unit}</small>
          </span>
        </label>
      ))}
      <Button block disabled={!filled} onClick={() => submit({ type: 'calc', values: Object.fromEntries(def.questions.map((q) => [q.id, Number(vals[q.id])])) })}>
        Проверить
      </Button>
    </div>
  );
}

function SimPlay({ def, submit }: PlayProps<SimTask>) {
  const [weekly, setWeekly] = useState(def.step);
  const weeks = simWeeks(def, weekly);
  const shown = Math.min(12, weeks ?? 12);
  const target = def.goalCost - def.start;
  return (
    <div class="stack">
      <div class="card card--total"><span>Мечта стоит</span><Coin n={def.goalCost} big /></div>
      <p class="muted center">Каждую неделю у тебя свободно {coinsText(def.freePerWeek)}. Сколько отложишь?</p>
      <Stepper label="Откладываю в неделю" value={weekly} min={0} max={def.freePerWeek} step={def.step} onChange={setWeekly} unit=" в неделю" />
      <div class="sim" role="img" aria-label={weeks === null ? 'Копилка не растёт' : `Мечта накопится за ${weeksText(weeks)}`}>
        {Array.from({ length: 12 }, (_, i) => {
          const saved = Math.min(target, weekly * (i + 1));
          const on = i < shown && weekly > 0;
          return (
            <div class="sim__col" key={i}>
              <div class={`sim__bar ${on ? 'is-on' : ''} ${saved >= target ? 'is-full' : ''}`} style={{ height: `${on ? Math.max(8, (saved / target) * 100) : 6}%` }} />
              <small>{i + 1}</small>
            </div>
          );
        })}
      </div>
      <p class="center" aria-live="polite">
        {weeks === null ? <b>Копилка не растёт — мечта не накопится.</b> : <>Мечта накопится за <b>{weeksText(weeks)}</b>. На радости останется <b>{def.freePerWeek - weekly}</b> в неделю.</>}
      </p>
      <Button block onClick={() => submit({ type: 'sim', weekly })}>Готово</Button>
    </div>
  );
}

function StoryPlay({ def, submit }: PlayProps<StoryTask>) {
  const [node, setNode] = useState(def.start);
  const [path, setPath] = useState<number[]>([]);
  const cur = def.nodes[node];
  const { steps } = walkStory(def, path);
  const choose = (idx: number) => {
    const c = cur.choices[idx];
    const np = [...path, idx];
    if (c.end) submit({ type: 'story', path: np });
    else {
      setPath(np);
      setNode(c.next!);
    }
  };
  return (
    <div class="stack">
      {steps.map((s, i) => (
        <div class="story__done" key={i}>
          <small>{s.text}</small>
          <b><Icon name="check" size={18} /> {s.choice}</b>
        </div>
      ))}
      <div class="story__now card">
        <p class="lead">{cur.text}</p>
      </div>
      <div class="stack">
        {cur.choices.map((c, i) => (
          <Button key={i} block variant="secondary" class="btn--wrap" onClick={() => choose(i)}>{c.text}</Button>
        ))}
      </div>
    </div>
  );
}

// ---------- Экран задания и результат ----------

function ResultView({ def, ev, extra, retry }: { def: TaskDef; ev: TaskEval; extra: number; retry: () => void }) {
  const app = useAppState();
  const good = ev.stars === 3;
  return (
    <div class="stack result">
      {good && app.settings.animations && <Confetti />}
      <div class={`resulthead resulthead--${ev.stars}`}>
        <StarsView n={ev.stars} />
        <h2 class="h2">{ev.headline}</h2>
        <p class="lead">{ev.summary}</p>
      </div>
      <ul class="verdicts">
        {ev.details.map((d, i) => (
          <li key={i} class={d.ok === true ? 'is-ok' : d.ok === false ? 'is-bad' : ''}>
            <Icon name={d.ok === true ? 'check' : d.ok === false ? 'cross' : 'info'} size={22} />
            <div>
              <span>{d.text}</span>
              {d.sub && <small>{d.sub}</small>}
            </div>
          </li>
        ))}
      </ul>
      {ev.recovery && (
        <div class="card card--tint-gold">
          <b>Как исправить</b>
          <p>{ev.recovery}</p>
        </div>
      )}
      <div class="card card--tint-save">
        <b>Запомни</b>
        <p>{ev.learn}</p>
      </div>
      <div class="card card--reward" aria-live="polite">
        {extra > 0 ? (
          <>
            <Coin n={extra} label={`Награда ${coinsText(extra)}`} />
            <span>Награда за задание «{def.title}». Монеты уже в кошельке.</span>
          </>
        ) : (
          <span>Награда за это задание уже получена. Повторяй его для практики — монеты дадут, только если получится лучше.</span>
        )}
      </div>
      <div class="row row--wrap">
        {!good && <Button block variant="secondary" icon="refresh" onClick={retry}>Попробовать ещё</Button>}
        <Button block onClick={() => nav.back()}>К заданиям</Button>
      </div>
    </div>
  );
}

export function TaskPlayer({ id }: { id: string }) {
  const def = content.tasks.find((t) => t.id === id);
  const [attempt, setAttempt] = useState(0);
  const [result, setResult] = useState<{ ev: TaskEval; extra: number } | null>(null);
  if (!def) return <Screen title="Задание"><p>Такого задания нет.</p></Screen>;

  const submit = (answer: TaskAnswer) => {
    const r = act((pp, c) => submitTask(pp, def.id, answer, c));
    if (!r.ok) return;
    playSound(r.evaluation.stars === 3 ? 'good' : 'oops');
    if (r.extra > 0) setTimeout(() => playSound('coin'), 250);
    setResult({ ev: r.evaluation, extra: r.extra });
  };
  const retry = () => {
    setResult(null);
    setAttempt(attempt + 1);
  };

  const body = () => {
    const props = { key: attempt, submit };
    switch (def.type) {
      case 'sort': return <SortPlay def={def} {...props} />;
      case 'cart': return <CartPlay def={def} {...props} />;
      case 'allocate': return <AllocatePlay def={def} {...props} />;
      case 'calc': return <CalcPlay def={def} {...props} />;
      case 'sim': return <SimPlay def={def} {...props} />;
      case 'story': return <StoryPlay def={def} {...props} />;
    }
  };

  return (
    <Screen title={def.title} hint={result ? undefined : def.intro}>
      {result ? <ResultView def={def} ev={result.ev} extra={result.extra} retry={retry} /> : body()}
    </Screen>
  );
}

