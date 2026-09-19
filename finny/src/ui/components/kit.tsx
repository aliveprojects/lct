import type { ComponentChildren, JSX } from 'preact';
import { useEffect, useRef } from 'preact/hooks';
import { ui } from '../../app/ui';
import { content } from '../../content';
import { coinsText } from '../../domain/econ';
import type { Change, Direction, PetStat } from '../../domain/types';
import { playSound } from '../../platform/sound';
import { Icon } from '../icons/Icon';

export const MINUS = '−';
export const signed = (n: number): string => (n > 0 ? `+${n}` : n < 0 ? `${MINUS}${Math.abs(n)}` : '0');

export const STAT_ICON: Record<PetStat, string> = { satiety: 'bowl', care: 'bath', mood: 'smile' };
export const STAT_SHORT: Record<PetStat, string> = { satiety: 'Сытость', care: 'Чистота', mood: 'Настроение' };
export const DIR_ICON: Record<Direction, string> = { must: 'bowl', want: 'ball', save: 'jar' };
export const DIR_NAME: Record<Direction, string> = { must: 'Нужное', want: 'Хочется', save: 'Копилка' };

// ---------- Кнопки ----------

interface ButtonProps {
  variant?: 'primary' | 'secondary' | 'ghost' | 'good' | 'gold';
  icon?: string;
  block?: boolean;
  small?: boolean;
  disabled?: boolean;
  onClick?: () => void;
  class?: string;
  children?: ComponentChildren;
  ariaLabel?: string;
  type?: 'button' | 'submit';
}

export function Button({ variant = 'primary', icon, block, small, disabled, onClick, class: cls, children, ariaLabel, type = 'button' }: ButtonProps) {
  return (
    <button
      type={type}
      class={`btn btn--${variant} ${block ? 'btn--block' : ''} ${small ? 'btn--sm' : ''} ${cls ?? ''}`}
      disabled={disabled}
      aria-label={ariaLabel}
      onClick={() => {
        playSound('tap');
        onClick?.();
      }}
    >
      {icon && <Icon name={icon} size={small ? 22 : 26} />}
      <span>{children}</span>
    </button>
  );
}

export function IconButton({ icon, label, onClick, badge }: { icon: string; label: string; onClick: () => void; badge?: boolean }) {
  return (
    <button type="button" class="iconbtn" aria-label={label} onClick={() => { playSound('tap'); onClick(); }}>
      <Icon name={icon} size={26} />
      {badge && <span class="iconbtn__dot" aria-hidden="true" />}
    </button>
  );
}

// ---------- Деньги ----------

export function Coin({ n, big, label }: { n: number; big?: boolean; label?: string }) {
  return (
    <span class={`coin ${big ? 'coin--big' : ''}`} aria-label={label ?? coinsText(n)}>
      <Icon name="coin" size={big ? 30 : 22} />
      <b>{n}</b>
    </span>
  );
}

export function Tag({ dir, children }: { dir: Direction; children?: ComponentChildren }) {
  return (
    <span class={`tag tag--${dir}`}>
      <Icon name={DIR_ICON[dir]} size={18} />
      {children ?? DIR_NAME[dir]}
    </span>
  );
}

// ---------- Показатели ----------

export function Meter({ value, max = 100, label, kind = 'primary', showNumbers = true, text }: { value: number; max?: number; label: string; kind?: string; showNumbers?: boolean; text?: string }) {
  const pct = Math.max(0, Math.min(100, (value / max) * 100));
  return (
    <div class={`meter meter--${kind}`}>
      <div class="meter__track" role="progressbar" aria-valuemin={0} aria-valuemax={max} aria-valuenow={value} aria-label={`${label}: ${value} из ${max}`}>
        <div class="meter__fill" style={{ width: `${pct}%` }} />
      </div>
      {showNumbers && <span class="meter__num">{text ?? `${value}/${max}`}</span>}
    </div>
  );
}

export function Stepper({ value, min = 0, max, step = 5, onChange, label, unit }: { value: number; min?: number; max: number; step?: number; onChange: (v: number) => void; label: string; unit?: string }) {
  const dec = () => onChange(Math.max(min, value - step));
  const inc = () => onChange(Math.min(max, value + step));
  return (
    <div class="stepper" role="group" aria-label={label}>
      <button type="button" class="stepper__btn" aria-label={`Меньше: ${label}`} disabled={value <= min} onClick={() => { playSound('tap'); dec(); }}>
        <Icon name="minus" size={26} />
      </button>
      <output class="stepper__val" aria-live="polite">
        {value}
        {unit && <small>{unit}</small>}
      </output>
      <button type="button" class="stepper__btn" aria-label={`Больше: ${label}`} disabled={value >= max} onClick={() => { playSound('tap'); inc(); }}>
        <Icon name="plus" size={26} />
      </button>
    </div>
  );
}

export function Segmented<T extends string>({ value, options, onChange, label }: { value: T; options: { id: T; label: string; icon?: string }[]; onChange: (v: T) => void; label: string }) {
  return (
    <div class="seg" role="group" aria-label={label}>
      {options.map((o) => (
        <button key={o.id} type="button" class={`seg__btn ${o.id === value ? 'is-on' : ''}`} aria-pressed={o.id === value} onClick={() => { playSound('tap'); onChange(o.id); }}>
          {o.icon && <Icon name={o.icon} size={22} />}
          {o.label}
        </button>
      ))}
    </div>
  );
}

// ---------- Окна ----------

export function SheetFrame({ title, onClose, children, tone }: { title?: string; onClose: () => void; children?: ComponentChildren; tone?: string }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    ref.current?.focus();
  }, []);
  return (
    <div class="overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div
        class={`sheet ${tone ? `sheet--${tone}` : ''}`}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        tabIndex={-1}
        ref={ref}
        onKeyDown={(e: JSX.TargetedKeyboardEvent<HTMLDivElement>) => e.key === 'Escape' && onClose()}
      >
        <div class="sheet__handle" aria-hidden="true" />
        {title && <h2 class="sheet__title">{title}</h2>}
        {children}
      </div>
    </div>
  );
}

// ---------- «Что изменилось и почему» ----------

function changeIcon(c: Change): string {
  if (c.kind === 'wallet') return 'coin';
  if (c.kind === 'savings') return 'jar';
  if (c.kind === 'growth') return 'star';
  if (c.kind === 'stat' && c.stat) return STAT_ICON[c.stat];
  return 'clip';
}

export function ChangeList({ changes }: { changes: Change[] }) {
  if (!changes.length) return null;
  return (
    <ul class="changes">
      {changes.map((c, i) => (
        <li class="change" key={i}>
          <Icon name={changeIcon(c)} size={34} />
          <div class="change__body">
            <div class="change__head">
              <span class="change__label">{c.label}</span>
              {c.kind === 'plan' && c.before === undefined ? (
                <span class="change__delta">{c.delta}</span>
              ) : (
                <span class={`change__delta ${c.delta > 0 ? 'is-up' : c.delta < 0 ? 'is-down' : ''}`}>
                  {c.before !== undefined && c.after !== undefined && (
                    <span class="change__range">
                      {c.before} → {c.after}
                    </span>
                  )}
                  <b>{signed(c.delta)}</b>
                </span>
              )}
            </div>
            <div class="change__why">{c.why}</div>
          </div>
        </li>
      ))}
    </ul>
  );
}

export function Confetti() {
  return (
    <div class="confetti" aria-hidden="true">
      {Array.from({ length: 16 }, (_, i) => (
        <i key={i} />
      ))}
    </div>
  );
}

// ---------- Словарик ----------

export function openTerm(id: string): void {
  const t = content.glossary.find((g) => g.id === id);
  if (!t) return;
  void ui.sheet(
    (close) => (
      <div class="stack">
        <p class="lead">{t.short}</p>
        {t.example && (
          <div class="card card--tint-gold">
            <b>Например:</b> {t.example}
          </div>
        )}
        <Button block onClick={close}>Понятно</Button>
      </div>
    ),
    t.term,
  );
}

/** Слово с подчёркиванием: по нажатию объясняет термин простыми словами. */
export function Term({ id, children }: { id: string; children: ComponentChildren }) {
  return (
    <button type="button" class="term" onClick={() => openTerm(id)}>
      {children}
    </button>
  );
}
