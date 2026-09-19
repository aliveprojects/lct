import { useState } from 'preact/hooks';
import { present } from '../../app/actions';
import { APP } from '../../app/meta';
import { nav } from '../../app/nav';
import { act, getCtx, getState, mutate, setState, useAppState } from '../../app/store';
import { ui } from '../../app/ui';
import { TOPICS, content } from '../../content';
import { ECON, coinsText } from '../../domain/econ';
import { ADULT_BONUS_REASONS, adultBonusLeft, grantAdultBonus } from '../../domain/income';
import { savingsTotal } from '../../domain/profile';
import { resetDemoProfile, resetProgress, setMode, withProfile } from '../../domain/state';
import { TRACKS } from '../../platform/music';
import { storage } from '../../platform/storage';
import { Button, Meter } from '../components/kit';
import { Screen } from '../components/Screen';
import { Icon } from '../icons/Icon';
import { openDemoPanel } from './DemoPanel';
import { SettingsForm } from './Settings';

function makeProblem() {
  const a = 12 + Math.floor(Math.random() * 8);
  const b = 6 + Math.floor(Math.random() * 4);
  return { a, b, answer: a * b };
}

function Gate({ onPass }: { onPass: () => void }) {
  const [problem, setProblem] = useState(makeProblem);
  const [value, setValue] = useState('');
  const check = () => {
    if (Number(value) === problem.answer) onPass();
    else {
      ui.toast('Не получилось. Этот раздел для взрослых — попробуйте другой пример.');
      setProblem(makeProblem());
      setValue('');
    }
  };
  return (
    <Screen title="Для взрослых" help={false} hint="Раздел для родителей и учителей. Дети сюда обычно не заходят.">
      <div class="card stack center">
        <Icon name="key" size={64} />
        <p class="lead">Чтобы войти, решите пример:</p>
        <p class="gate__q">{problem.a} × {problem.b} = ?</p>
        <input class="input input--num" inputMode="numeric" pattern="[0-9]*" maxLength={4} aria-label="Ответ на пример" value={value} onInput={(e) => setValue((e.target as HTMLInputElement).value.replace(/\D/g, ''))} onKeyDown={(e) => e.key === 'Enter' && check()} />
        <Button block disabled={!value} onClick={check}>Войти</Button>
      </div>
    </Screen>
  );
}

function Dashboard() {
  const app = useAppState();
  const p = (app.mode === 'demo' ? app.demo : app.main)!;
  const left = adultBonusLeft(p);
  const done = Object.values(p.tasks).filter((t) => t.stars > 0).length;
  const practiced = new Set<number>();
  for (const t of content.tasks) if ((p.tasks[t.id]?.stars ?? 0) > 0) t.competencies.forEach((c) => practiced.add(c));
  const stage = content.pet.stages.find((s) => s.id === p.pet.stage)!;

  const reset = async () => {
    const ok = await ui.confirm({
      title: 'Сбросить прогресс?',
      icon: 'refresh',
      tone: 'careful',
      body: <div class="stack"><p>Игра начнётся заново: монеты, копилка, задания и рост питомца обнулятся.</p><p>Имя игрока и питомец останутся.</p></div>,
      confirmLabel: 'Сбросить прогресс',
      cancelLabel: 'Не надо',
    });
    if (!ok) return;
    setState(withProfile(getState(), resetProgress(p, getCtx())));
    ui.toast('Прогресс сброшен');
    nav.reset('/');
  };
  const remove = async () => {
    const ok = await ui.confirm({
      title: 'Удалить профиль?',
      icon: 'trash',
      tone: 'careful',
      body: <div class="stack"><p>Будут удалены все данные профиля с этого устройства: имя игрока, питомец, монеты, задания.</p><p><b>Вернуть их будет нельзя.</b></p></div>,
      confirmLabel: 'Удалить профиль',
      cancelLabel: 'Оставить',
    });
    if (!ok) return;
    setState(withProfile(getState(), null));
    void storage.clearBackup();
    ui.toast('Профиль удалён');
    nav.reset('/welcome');
  };

  return (
    <Screen title="Для взрослых" help={false} hint="Здесь видно, чему учится ребёнок. Оценок и сравнений нет.">
      <div class="card stack">
        <h2 class="h3">Для чего это приложение</h2>
        <p>Ребёнок 7–11 лет ухаживает за питомцем и принимает простые решения с игровыми монетами: планирует неделю, делит монеты на нужное и желаемое, копит на мечту и решает ситуации в заданиях. Настоящих денег, покупок и рекламы нет.</p>
        <p class="muted">Ваша роль — быть рядом и обсуждать. Решения ребёнок принимает сам.</p>
      </div>

      <div class="card stack">
        <h2 class="h3">Чему учимся</h2>
        <ul class="howto">
          {content.competencies.map((c) => (
            <li key={c.id}>
              <Icon name={practiced.has(c.id) ? 'check' : 'sparkle'} size={26} />
              <span>{c.text}<small class="muted"> — {practiced.has(c.id) ? 'уже пробовали в заданиях' : 'впереди'}</small></span>
            </li>
          ))}
        </ul>
      </div>

      <div class="card stack">
        <h2 class="h3">Общий прогресс</h2>
        <ul class="plain kvlist">
          <li><span>Игровое имя</span><b>{p.playerName}</b></li>
          <li><span>Питомец</span><b>{p.pet.name}, {stage.name}</b></li>
          <li><span>Недель сыграно</span><b>{p.history.length}</b></li>
          <li><span>Заданий пройдено</span><b>{done} из {content.tasks.length}</b></li>
          <li><span>В копилке</span><b>{coinsText(savingsTotal(p))}</b></li>
          <li><span>Мечт исполнено</span><b>{p.goals.filter((g) => g.done).length}</b></li>
        </ul>
        {TOPICS.map((t) => {
          const tasks = content.tasks.filter((x) => x.topic === t.id);
          const stars = tasks.reduce((a, x) => a + (p.tasks[x.id]?.stars ?? 0), 0);
          return <Meter key={t.id} value={stars} max={tasks.length * 3} label={t.title} kind="save" text={`${t.title}: ${stars}/${tasks.length * 3}`} />;
        })}
      </div>

      <div class="card stack">
        <h2 class="h3">Поговорите вместе</h2>
        <ul class="plain">
          <li>Почему ты решил(а) положить столько монет в копилку?</li>
          <li>Чем «нужное» отличается от «хочется»? Приведи пример из жизни.</li>
          <li>Что бы ты сделал(а) по-другому на следующей неделе?</li>
        </ul>
      </div>

      <div class="card stack">
        <h2 class="h3">Бонус от взрослого</h2>
        <p>Можно добавить игровые монеты за помощь или хорошую идею. Не больше {ECON.adultBonusCap} за неделю. Это не деньги: их нельзя обменять на призы. Осталось на этой неделе: <b>{left}</b>.</p>
        <div class="row row--wrap">
          {ADULT_BONUS_REASONS.map((r) => (
            <Button key={r} small variant="secondary" disabled={left <= 0} onClick={async () => { await present(act((pp) => grantAdultBonus(pp, ECON.adultBonusStep, r))); }}>
              +{ECON.adultBonusStep} · {r}
            </Button>
          ))}
        </div>
      </div>

      <div class="card stack">
        <h2 class="h3">Доступность</h2>
        <SettingsForm />
      </div>

      <div class="card stack">
        <h2 class="h3">Данные и приватность</h2>
        <p>Приложение работает без интернета, без аккаунта и не собирает персональные данные. Игровое имя, питомец и прогресс хранятся только на этом устройстве. Разрешений Android не требуется.</p>
        <Button block variant="secondary" icon="refresh" onClick={() => void reset()}>Сбросить прогресс</Button>
        <Button block variant="secondary" icon="trash" onClick={() => void remove()}>Удалить профиль</Button>
      </div>

      <div class="card stack">
        <h2 class="h3">Режим для экспертов</h2>
        {app.mode === 'demo' ? (
          <>
            <p>Сейчас включён демо-режим с тестовым профилем.</p>
            <Button block variant="secondary" onClick={() => openDemoPanel()}>Панель проверки</Button>
            <Button block variant="secondary" onClick={() => { mutate((s) => setMode(s, 'normal')); nav.reset('/'); }}>Выйти из демо-режима</Button>
            <Button block variant="secondary" onClick={async () => {
              const ok = await ui.confirm({ title: 'Сбросить тестовый профиль?', icon: 'refresh', tone: 'careful', body: <p>Тестовый профиль вернётся к исходному состоянию. Игровой профиль не изменится.</p>, confirmLabel: 'Сбросить', cancelLabel: 'Отмена' });
              if (!ok) return;
              mutate(resetDemoProfile);
              nav.reset('/intro');
            }}>Сбросить тестовый профиль</Button>
          </>
        ) : (
          <>
            <p>Тестовый профиль позволяет быстро пройти игровой цикл: все задания открыты, недели переключаются без ожидания. Игровой профиль при этом не меняется.</p>
            <Button block variant="secondary" icon="key" onClick={() => { mutate((s) => setMode(s, 'demo')); nav.reset('/'); }}>Включить демо-режим</Button>
          </>
        )}
      </div>

      <div class="card stack">
        <h2 class="h3">Полезно для семьи</h2>
        <p>Материалы по финансовой грамотности для детей и родителей: раздел «Финансовая грамотность» на портале «Открытый бюджет города Москвы» и просветительский ресурс Банка России «Финансовая культура». В приложении нет внешних ссылок — найдите их через поиск.</p>
      </div>

      <p class="muted center">Музыка: {TRACKS.map((t) => `«${t.title}» — ${t.artist}`).join('; ')}.</p>
      <p class="muted center">{APP.name} · версия {APP.version} ({APP.build}) · шрифт Rubik (лицензия OFL)</p>
    </Screen>
  );
}

export function Adult() {
  const [ok, setOk] = useState(false);
  return ok ? <Dashboard /> : <Gate onPass={() => setOk(true)} />;
}
