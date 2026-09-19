import { useEffect } from 'preact/hooks';
import { activeProfile } from '../domain/state';
import { Adult } from '../ui/screens/Adult';
import { Glossary } from '../ui/screens/Glossary';
import { Goals } from '../ui/screens/Goals';
import { Home } from '../ui/screens/Home';
import { Create, Intro, Welcome } from '../ui/screens/Onboarding';
import { PlanScreen } from '../ui/screens/Plan';
import { Progress, Review } from '../ui/screens/Progress';
import { Shop } from '../ui/screens/Shop';
import { TaskPlayer, Tasks } from '../ui/screens/Tasks';
import { OverlayHost } from './OverlayHost';
import { useRoute } from './nav';
import { useAppState } from './store';

const NO_PROFILE_OK = new Set(['/welcome', '/intro', '/create']);

function applySettings(s: { textScale: string; highContrast: boolean; animations: boolean }): void {
  const root = document.documentElement;
  root.dataset.scale = s.textScale;
  root.dataset.contrast = s.highContrast ? 'high' : 'normal';
  root.dataset.motion = s.animations ? 'on' : 'off';
}

export function App() {
  const app = useAppState();
  const route = useRoute();
  useEffect(() => applySettings(app.settings), [app.settings]);

  const profile = activeProfile(app);
  const [raw, query = ''] = route.split('?');
  // Без профиля показываем только знакомство и создание питомца; с профилем — не возвращаем на приветствие.
  let path = raw;
  if (!profile && !NO_PROFILE_OK.has(raw)) path = app.mode === 'demo' ? '/intro' : '/welcome';
  if (profile && (raw === '/welcome' || raw === '/create')) path = '/';

  let view;
  if (path === '/welcome') view = <Welcome />;
  else if (path === '/intro') view = <Intro again={query.includes('again=1') || !!profile} />;
  else if (path === '/create') view = <Create />;
  else if (path === '/plan') view = <PlanScreen />;
  else if (path === '/shop') view = <Shop />;
  else if (path === '/goals') view = <Goals />;
  else if (path === '/tasks') view = <Tasks />;
  else if (path.startsWith('/task/')) view = <TaskPlayer id={path.slice(6)} />;
  else if (path === '/progress') view = <Progress />;
  else if (path === '/review') view = <Review />;
  else if (path === '/glossary') view = <Glossary />;
  else if (path === '/adult') view = <Adult />;
  else view = <Home />;

  return (
    <div class="app">
      <div class="page" key={path}>
        {view}
      </div>
      <OverlayHost />
    </div>
  );
}
