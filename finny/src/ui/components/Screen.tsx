import type { ComponentChildren } from 'preact';
import { nav } from '../../app/nav';
import { IconButton } from './kit';
import { openHelp } from '../screens/Help';

interface ScreenProps {
  title: string;
  /** Одна короткая фраза: что здесь можно сделать. */
  hint?: string;
  back?: boolean;
  help?: boolean;
  right?: ComponentChildren;
  footer?: ComponentChildren;
  children?: ComponentChildren;
  class?: string;
}

/** Единый каркас: кнопка «назад» всегда слева, подсказка «?» — справа. */
export function Screen({ title, hint, back = true, help = true, right, footer, children, class: cls }: ScreenProps) {
  return (
    <main class={`screen ${cls ?? ''}`}>
      <header class="topbar">
        {back ? <IconButton icon="back" label="Назад" onClick={() => (nav.back() ? undefined : nav.reset('/'))} /> : <span class="topbar__gap" />}
        <div class="topbar__text">
          <h1 class="topbar__title">{title}</h1>
        </div>
        {right}
        {help && <IconButton icon="help" label="Подсказка: как играть" onClick={() => void openHelp()} />}
      </header>
      {hint && <p class="hint">{hint}</p>}
      <div class="content">{children}</div>
      {footer && <footer class="footer">{footer}</footer>}
    </main>
  );
}
