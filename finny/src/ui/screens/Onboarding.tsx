import { useState } from 'preact/hooks';
import { present } from '../../app/actions';
import { nav, useRoute } from '../../app/nav';
import { getCtx, getState, mutate, setState, useAppState } from '../../app/store';
import { content } from '../../content';
import { newProfile } from '../../domain/profile';
import { withProfile } from '../../domain/state';
import type { Report } from '../../domain/types';
import { Button, Segmented } from '../components/kit';
import { Screen } from '../components/Screen';
import { Icon } from '../icons/Icon';
import { PetView } from '../pet/PetView';
import { ThreeChoices } from './Help';

// ---------- Приветствие ----------

export function Welcome() {
  const app = useAppState();
  return (
    <main class="screen welcome">
      <div class="welcome__hero">
        <div class="welcome__pet">
          <PetView appearance={{ species: 'cat', color: 'mint', accessory: 'bow' }} stage={2} expression="happy" uid="w" label="Финни, питомец-талисман" />
        </div>
        <h1 class="welcome__title">Финни</h1>
        <p class="lead">Привет! Меня зовут Финни. Давай растить питомца и учиться обращаться с монетами. Всё понарошку.</p>
      </div>
      <div class="welcome__actions">
        <Button block icon="sparkle" onClick={() => nav.go(app.introSeen ? '/create' : '/intro')}>
          Начать
        </Button>
      </div>
    </main>
  );
}

// ---------- Знакомство ----------

export function Intro({ again }: { again: boolean }) {
  const [i, setI] = useState(0);
  const slides = [
    {
      title: 'Привет, я Финни!',
      body: (
        <div class="stack center">
          <div class="intro__pet">
            <PetView appearance={{ species: 'cat', color: 'mint', accessory: 'bow' }} stage={1} expression="happy" uid="i1" label="Финни, питомец-малыш" />
          </div>
          <p class="lead">Мне нужна твоя забота. Ты решаешь, на что потратить монеты, — и я расту. Твоего питомца тоже можно назвать как хочешь!</p>
        </div>
      ),
    },
    {
      title: 'Три решения',
      body: (
        <div class="stack">
          <p class="lead">С монетами можно сделать три вещи:</p>
          <ThreeChoices />
        </div>
      ),
    },
    {
      title: 'Выбирай с умом',
      body: (
        <div class="stack center">
          <div class="row row--center">
            <Icon name="coin" size={72} />
            <Icon name="arrow" size={32} />
            <Icon name="jar" size={72} />
          </div>
          <p class="lead">Монет не хватит на всё, поэтому выбирай.</p>
          <p>Ошибаться можно: ошибка — это урок, и её можно исправить. Монеты в игре не настоящие.</p>
        </div>
      ),
    },
  ];
  const last = i === slides.length - 1;
  const finish = () => {
    mutate((s) => ({ ...s, introSeen: true }));
    if (again) nav.back();
    else nav.replace('/create');
  };
  return (
    <Screen
      title="Знакомство"
      help={false}
      back={again}
      right={!again ? <button type="button" class="linkbtn" onClick={finish}>Пропустить</button> : undefined}
      footer={
        <div class="stack">
          <div class="dots" role="img" aria-label={`Шаг ${i + 1} из ${slides.length}`}>
            {slides.map((_, k) => (
              <span key={k} class={k === i ? 'is-on' : ''} />
            ))}
          </div>
          <div class="row">
            {i > 0 && (
              <Button variant="secondary" onClick={() => setI(i - 1)}>
                Назад
              </Button>
            )}
            <Button block onClick={() => (last ? finish() : setI(i + 1))}>
              {last ? (again ? 'Понятно' : 'Создать питомца') : 'Дальше'}
            </Button>
          </div>
        </div>
      }
    >
      <h2 class="h1 center">{slides[i].title}</h2>
      {slides[i].body}
    </Screen>
  );
}

// ---------- Создание профиля и питомца ----------

const sanitize = (s: string): string => s.replace(/[^\p{L}\p{N} -]/gu, '').slice(0, 12);
const valid = (s: string): boolean => s.trim().length >= 2;
const pick = <T,>(xs: T[]): T => xs[Math.floor(Math.random() * xs.length)];

/** Шаг мастера хранится в маршруте (/create?step=1): стрелка в шапке и аппаратная кнопка «назад» идут по шагам. */
function useStep(): number {
  const route = useRoute();
  return Number(new URLSearchParams(route.split('?')[1] ?? '').get('step') ?? 0) || 0;
}

export function Create() {
  const step = useStep();
  const [player, setPlayer] = useState('');
  const [species, setSpecies] = useState('cat');
  const [color, setColor] = useState('peach');
  const [accessory, setAccessory] = useState('none');
  const [petName, setPetName] = useState(content.pet.defaultPetName);
  const app = useAppState();
  const startAcc = content.pet.accessories.filter((a) => a.atStart);
  const appearance = { species, color, accessory };

  const finish = async () => {
    const ctx = getCtx();
    const profile = newProfile({ playerName: player.trim(), petName: petName.trim(), appearance, isDemo: app.mode === 'demo' }, ctx);
    setState(withProfile(getState(), profile));
    nav.reset('/');
    const first = profile.ledger[0];
    const report: Report = {
      title: 'Стартовый подарок!',
      tone: 'good',
      changes: [{ kind: 'wallet', label: 'Монеты', before: 0, after: first.balance, delta: first.amount, why: 'Это подарок для начала игры. Монеты в игре не настоящие.' }],
      explain: 'Теперь у тебя есть монеты. Сначала составь план недели: реши, сколько потратить на нужное, на желаемое и сколько отложить.',
      next: { label: 'Составить план', route: '/plan' },
    };
    await present({ ok: true, profile, report });
  };

  const steps = [
    {
      title: 'Как тебя зовут в игре?',
      ok: valid(player),
      body: (
        <div class="stack">
          <p class="lead">Придумай игровое имя. Настоящее имя писать не нужно.</p>
          <label class="field">
            <span class="field__label">Игровое имя</span>
            <input class="input" value={player} maxLength={12} autoComplete="off" autoCapitalize="words" placeholder="Например, Ракета" onInput={(e) => setPlayer(sanitize((e.target as HTMLInputElement).value))} />
          </label>
          <Button variant="secondary" icon="sparkle" onClick={() => setPlayer(pick(content.pet.playerNames))}>Придумать за меня</Button>
        </div>
      ),
    },
    {
      title: 'Выбери друга',
      ok: true,
      body: (
        <div class="stack">
          <div class="create__preview">
            <PetView appearance={appearance} stage={1} expression="happy" uid="c" label="Твой питомец" />
          </div>
          <fieldset class="fieldset">
            <legend>Кто это?</legend>
            <div class="picker">
              {content.pet.species.map((sp) => (
                <button key={sp.id} type="button" class={`pick ${sp.id === species ? 'is-on' : ''}`} aria-pressed={sp.id === species} onClick={() => setSpecies(sp.id)}>
                  <PetView appearance={{ species: sp.id, color, accessory: 'none' }} stage={2} expression="content" size={64} uid={`s-${sp.id}`} />
                  <span>{sp.name}</span>
                </button>
              ))}
            </div>
          </fieldset>
          <fieldset class="fieldset">
            <legend>Цвет</legend>
            <div class="picker">
              {content.pet.colors.map((c) => (
                <button key={c.id} type="button" class={`pick pick--color ${c.id === color ? 'is-on' : ''}`} aria-pressed={c.id === color} onClick={() => setColor(c.id)}>
                  <span class="swatch" style={{ background: `linear-gradient(135deg, ${c.body} 60%, ${c.dark})` }} />
                  <span>{c.name}</span>
                </button>
              ))}
            </div>
          </fieldset>
          <fieldset class="fieldset">
            <legend>Украшение</legend>
            <Segmented label="Украшение" value={accessory} onChange={setAccessory} options={startAcc.map((a) => ({ id: a.id, label: a.name }))} />
          </fieldset>
        </div>
      ),
    },
    {
      title: 'Как назовём питомца?',
      ok: valid(petName),
      body: (
        <div class="stack">
          <div class="create__preview">
            <PetView appearance={appearance} stage={1} expression="happy" uid="c2" label="Твой питомец" />
          </div>
          <label class="field">
            <span class="field__label">Имя питомца</span>
            <input class="input" value={petName} maxLength={12} autoComplete="off" autoCapitalize="words" placeholder="Например, Финни" onInput={(e) => setPetName(sanitize((e.target as HTMLInputElement).value))} />
          </label>
          <Button variant="secondary" icon="sparkle" onClick={() => setPetName(pick(content.pet.petNames))}>Придумать за меня</Button>
        </div>
      ),
    },
  ];
  const cur = steps[step];
  return (
    <Screen
      title="Новый питомец"
      back={step > 0}
      footer={
        <div class="row">
          {step > 0 && (
            <Button variant="secondary" onClick={() => nav.back()}>
              Назад
            </Button>
          )}
          <Button block disabled={!cur.ok} onClick={() => (step === steps.length - 1 ? void finish() : nav.go(`/create?step=${step + 1}`))}>
            {step === steps.length - 1 ? 'Готово!' : 'Дальше'}
          </Button>
        </div>
      }
    >
      <div class="dots" role="img" aria-label={`Шаг ${step + 1} из ${steps.length}`}>
        {steps.map((_, k) => (
          <span key={k} class={k <= step ? 'is-on' : ''} />
        ))}
      </div>
      <h2 class="h1 center">{cur.title}</h2>
      {cur.body}
      {!cur.ok && <p class="muted center">Нужно хотя бы 2 буквы.</p>}
    </Screen>
  );
}

