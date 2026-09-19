import type { ComponentChildren } from 'preact';
import { mutate, useAppState } from '../../app/store';
import { ui } from '../../app/ui';
import type { Settings, TextScale } from '../../domain/types';
import { setMusicEnabled } from '../../platform/music';
import { setSoundEnabled, playSound } from '../../platform/sound';
import { Button, Segmented } from '../components/kit';

export function Switch({ label, hint, checked, onChange }: { label: string; hint?: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <button type="button" role="switch" aria-checked={checked} class={`switch ${checked ? 'is-on' : ''}`} onClick={() => onChange(!checked)}>
      <span class="switch__text">
        <b>{label}</b>
        {hint && <small>{hint}</small>}
      </span>
      <span class="switch__state">{checked ? 'Вкл' : 'Выкл'}</span>
      <span class="switch__track" aria-hidden="true"><i /></span>
    </button>
  );
}

export function useSettingsUpdate() {
  return (patch: Partial<Settings>) =>
    mutate((s) => {
      const settings = { ...s.settings, ...patch };
      setSoundEnabled(settings.sound);
      setMusicEnabled(settings.music);
      return { ...s, settings };
    });
}

export function SettingsForm(): ComponentChildren {
  const app = useAppState();
  const set = useSettingsUpdate();
  const st = app.settings;
  return (
    <div class="stack">
      <Switch label="Фоновая музыка" hint="Спокойная музыка во время игры. Её можно выключить в любой момент." checked={st.music} onChange={(v) => set({ music: v })} />
      <Switch label="Звуки" hint="Короткие сигналы при действиях. Важное всегда есть и в тексте." checked={st.sound} onChange={(v) => { set({ sound: v }); if (v) setTimeout(() => playSound('good'), 50); }} />
      <Switch label="Анимации" hint="Движение питомца и конфетти." checked={st.animations} onChange={(v) => set({ animations: v })} />
      <Switch label="Высокий контраст" hint="Чёрный фон, белый текст, жёлтые кнопки и чёткие рамки." checked={st.highContrast} onChange={(v) => set({ highContrast: v })} />
      <div class="stack">
        <b>Размер текста</b>
        <Segmented<TextScale>
          label="Размер текста"
          value={st.textScale}
          onChange={(v) => set({ textScale: v })}
          options={[
            { id: 'normal', label: 'Обычный' },
            { id: 'large', label: 'Крупный' },
            { id: 'xlarge', label: 'Очень крупный' },
          ]}
        />
        <small class="muted">Приложение также подстраивается под размер шрифта в настройках телефона.</small>
      </div>
    </div>
  );
}

export function openSettings(): void {
  void ui.sheet(
    (close) => (
      <div class="stack">
        <SettingsForm />
        <Button block onClick={close}>Готово</Button>
      </div>
    ),
    'Настройки',
  );
}
