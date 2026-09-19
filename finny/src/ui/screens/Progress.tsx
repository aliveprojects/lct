import { nav } from '../../app/nav';
import { useAppState } from '../../app/store';
import { TOPICS, content } from '../../content';
import { ECON, coinsText, growthToNext, pointsText } from '../../domain/econ';
import { DIRECTION_LABEL, STAT_LABEL, savingsTotal } from '../../domain/profile';
import type { PeriodResult, Profile } from '../../domain/types';
import { Button, Confetti, DIR_ICON, Meter, STAT_ICON, signed } from '../components/kit';
import { Screen } from '../components/Screen';
import { Icon } from '../icons/Icon';
import { PetView, PetOf } from '../pet/PetView';
import { LedgerList, openLedger } from './Ledger';
import { StarsView } from './Tasks';

function StageTrack({ p }: { p: Profile }) {
  return (
    <ol class="stages">
      {content.pet.stages.map((s) => {
        const reached = p.pet.stage >= s.id;
        return (
          <li key={s.id} class={`stagei ${reached ? 'is-on' : ''} ${p.pet.stage === s.id ? 'is-now' : ''}`}>
            <div class="stagei__art">
              <PetView appearance={p.pet.appearance} stage={s.id} expression="content" size={64} uid={`st${s.id}`} />
              {!reached && <span class="stagei__lock"><Icon name="lock" size={18} /></span>}
            </div>
            <b>{s.name}</b>
            <small>{reached ? (p.pet.stage === s.id ? 'сейчас' : 'пройдено') : `от ${ECON.stageFrom[s.id - 1]} очков`}</small>
          </li>
        );
      })}
    </ol>
  );
}

export function PlanFactRows({ r }: { r: PeriodResult }) {
  const rows = [
    { d: 'must' as const, plan: r.plan.must, fact: r.fact.must, ok: r.checks.mustWithin, verb: 'потрачено' },
    { d: 'want' as const, plan: r.plan.want, fact: r.fact.want, ok: r.checks.wantWithin, verb: 'потрачено' },
    { d: 'save' as const, plan: r.plan.save, fact: r.fact.save, ok: r.checks.savedPlan, verb: 'отложено' },
  ];
  return (
    <ul class="pf">
      {rows.map((x) => (
        <li key={x.d} class={x.ok ? 'is-ok' : ''}>
          <Icon name={DIR_ICON[x.d]} size={34} />
          <div>
            <b>{DIRECTION_LABEL[x.d]}</b>
            <small>план {x.plan} · {x.verb} {x.fact}</small>
          </div>
          <span class="pf__mark">
            <Icon name={x.ok ? 'check' : 'info'} size={20} />
            {x.ok ? 'по плану' : x.d === 'save' ? 'меньше плана' : x.fact === 0 ? 'не тратил' : 'больше плана'}
          </span>
        </li>
      ))}
      {r.fact.event > 0 && (
        <li class="is-ok">
          <Icon name="vet" size={34} />
          <div>
            <b>Непредвиденное</b>
            <small>потрачено {r.fact.event} · в оценку плана не входит</small>
          </div>
        </li>
      )}
    </ul>
  );
}

export function Progress() {
  const app = useAppState();
  const p = (app.mode === 'demo' ? app.demo : app.main)!;
  const stage = content.pet.stages.find((s) => s.id === p.pet.stage)!;
  const toNext = growthToNext(p.pet.growth);
  const from = ECON.stageFrom[p.pet.stage - 1];
  const nextFrom = ECON.stageFrom[p.pet.stage];
  const nextName = content.pet.stages.find((s) => s.id === p.pet.stage + 1)?.name;
  const last = p.history[p.history.length - 1];
  const doneTasks = Object.values(p.tasks).filter((t) => t.stars > 0).length;

  return (
    <Screen title="Успехи" hint="Смотри, как растёт питомец и что уже получилось.">
      <div class="card petcard">
        <div class="petcard__art"><PetOf profile={p} uid="prog" /></div>
        <div class="petcard__body">
          <h2 class="h2">{p.pet.name}</h2>
          <b>{stage.name}</b>
          <small class="muted">{stage.blurb}</small>
        </div>
      </div>

      <div class="card">
        <b>Очки роста: {p.pet.growth}</b>
        {toNext !== null && nextFrom !== undefined ? (
          <>
            <Meter value={p.pet.growth - from} max={nextFrom - from} label="До следующей стадии" kind="save" text={`ещё ${toNext}`} />
            <p class="muted">Ещё {pointsText(toNext)} до стадии «{nextName}». Очки не пропадают.</p>
          </>
        ) : (
          <p class="muted">Это самая большая стадия. Питомец вырос!</p>
        )}
        <StageTrack p={p} />
      </div>

      <div class="card">
        <b>Как растёт питомец</b>
        <ul class="howto">
          <li><Icon name="bowl" size={30} /><span><b>Нужное — до 3 очков.</b> Купи и еду, и уход за неделю.</span></li>
          <li><Icon name="clip" size={30} /><span><b>План — до 3 очков.</b> Не превышай план на нужное и на желаемое, отложи, сколько задумал.</span></li>
          <li><Icon name="jar" size={30} /><span><b>Копилка — до 2 очков.</b> Откладывай каждую неделю.</span></li>
        </ul>
      </div>

      {last ? (
        <div class="card">
          <b>Итоги недели {last.index}</b>
          <p>{last.summary}</p>
          <PlanFactRows r={last} />
          <Button block variant="secondary" onClick={() => nav.go('/review')}>Все подробности</Button>
        </div>
      ) : (
        <div class="card card--tint-gold"><b>Итоги появятся после первой недели.</b> Составь план, сделай покупки и заверши неделю.</div>
      )}

      <section class="stack">
        <h2 class="h3">Задания: {doneTasks} из {content.tasks.length}</h2>
        {TOPICS.map((t) => (
          <div class="card" key={t.id}>
            <div class="topic"><Icon name={t.icon} size={36} /><b>{t.title}</b></div>
            <ul class="plain">
              {content.tasks.filter((x) => x.topic === t.id).map((x) => (
                <li class="tasksum" key={x.id}>
                  <span>{x.title}</span>
                  <StarsView n={p.tasks[x.id]?.stars ?? 0} />
                </li>
              ))}
            </ul>
          </div>
        ))}
      </section>

      <div class="card">
        <b>Мечты</b>
        <p>Накоплено: {coinsText(savingsTotal(p))}. Исполнено мечт: {p.goals.filter((g) => g.done).length}.</p>
        <Button small variant="secondary" onClick={() => nav.go('/goals')}>К копилке</Button>
      </div>

      {p.history.length > 1 && (
        <div class="card">
          <b>Все недели</b>
          <ul class="plain">
            {[...p.history].reverse().map((h) => (
              <li class="tasksum" key={h.index}>
                <span>Неделя {h.index}: +{pointsText(h.score.total)}</span>
                <small>{content.pet.stages.find((s) => s.id === h.stageAfter)?.name}</small>
              </li>
            ))}
          </ul>
        </div>
      )}

      <div class="row row--wrap">
        <Button variant="secondary" icon="history" onClick={() => openLedger()}>История монет</Button>
        <Button variant="secondary" icon="book" onClick={() => nav.go('/glossary')}>Словарик</Button>
      </div>
      <details class="card">
        <summary><b>Последние операции</b></summary>
        <LedgerList tx={p.ledger.slice(-8)} />
      </details>
    </Screen>
  );
}

export function Review({ index }: { index?: number }) {
  const app = useAppState();
  const p = (app.mode === 'demo' ? app.demo : app.main)!;
  const r = index ? p.history.find((h) => h.index === index) : p.history[p.history.length - 1];
  if (!r)
    return (
      <Screen title="Итоги недели">
        <div class="card card--tint-gold stack center">
          <Icon name="trophy" size={64} />
          <b>Итогов пока нет</b>
          <p>Составь план, сделай покупки и заверши неделю — тогда я покажу, что получилось.</p>
          <Button onClick={() => nav.go('/plan')}>К плану</Button>
        </div>
      </Screen>
    );
  const up = r.stageAfter > r.stageBefore;
  const stage = content.pet.stages.find((s) => s.id === r.stageAfter)!;
  return (
    <Screen
      title={`Итоги недели ${r.index}`}
      hint="Вот что получилось и почему питомец растёт."
      footer={
        <div class="row row--wrap">
          <Button block variant="secondary" onClick={() => nav.reset('/')}>На главный</Button>
          <Button block icon="clip" onClick={() => { nav.replace('/plan'); }}>К плану</Button>
        </div>
      }
    >
      {up && app.settings.animations && <Confetti />}
      <div class={`card reviewhero ${up ? 'is-up' : ''}`}>
        <div class="reviewhero__pet">
          <PetView appearance={p.pet.appearance} stage={r.stageAfter} stats={r.statsAfter} size="100%" uid="rev" label={`${p.pet.name}: ${stage.name}`} />
        </div>
        <h2 class="h2 center">{up ? `Новая стадия: ${stage.name}!` : stage.name}</h2>
        <p class="lead center">{r.summary}</p>
        <Meter value={r.score.total} max={8} label="Очки роста за неделю" kind="save" text={`+${r.score.total} из 8`} />
      </div>

      <div class="card">
        <b>План и факт</b>
        <PlanFactRows r={r} />
      </div>

      <div class="card">
        <b>Почему столько очков</b>
        <ul class="lines">
          {r.lines.map((l) => (
            <li key={l.id} class={`line line--${l.status}`}>
              <span class="line__mark"><Icon name={l.status === 'ok' ? 'check' : l.status === 'part' ? 'info' : 'sparkle'} size={22} /></span>
              <div>
                <span>{l.text}</span>
                {l.tip && <small class="line__tip"><Icon name="bulb" size={18} /> {l.tip}</small>}
              </div>
              <b class="line__pts">{l.points ? `+${l.points}` : '0'}</b>
            </li>
          ))}
        </ul>
      </div>

      <div class="card">
        <b>Питомец после недели</b>
        <ul class="feelings feelings--compact">
          {(['satiety', 'care', 'mood'] as const).map((s) => (
            <li key={s}>
              <Icon name={STAT_ICON[s]} size={30} />
              <div>
                <span>{STAT_LABEL[s]}: {r.statsBefore[s]} → {r.statsAfter[s]} <b>({signed(r.statsAfter[s] - r.statsBefore[s])})</b></span>
              </div>
            </li>
          ))}
        </ul>
        <p class="muted">Каждую неделю показатели чуть снижаются. Это нормально — заботься о питомце снова.</p>
      </div>
    </Screen>
  );
}
