import { content } from '../../content';
import { Screen } from '../components/Screen';

export function Glossary() {
  return (
    <Screen title="Словарик" hint="Что значат слова из игры. Нажми на слово, чтобы прочитать.">
      <div class="stack">
        {content.glossary.map((g) => (
          <details class="card term-card" key={g.id}>
            <summary>{g.term}</summary>
            <p>{g.short}</p>
            {g.example && <p class="muted"><b>Например:</b> {g.example}</p>}
          </details>
        ))}
      </div>
    </Screen>
  );
}
