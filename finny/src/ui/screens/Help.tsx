import { nav } from '../../app/nav';
import { ui } from '../../app/ui';
import { Icon } from '../icons/Icon';
import { Button } from '../components/kit';

/** Три типа решений — одинаково показываются в знакомстве и в подсказке «?». */
export function ThreeChoices() {
  return (
    <div class="choices">
      <div class="choice choice--must">
        <Icon name="bowl" size={44} />
        <div>
          <b>Нужное</b>
          <span>Еда и уход. Без этого питомцу трудно.</span>
        </div>
      </div>
      <div class="choice choice--want">
        <Icon name="ball" size={44} />
        <div>
          <b>Хочется</b>
          <span>Игрушки и украшения. Радуют, но можно подождать.</span>
        </div>
      </div>
      <div class="choice choice--save">
        <Icon name="jar" size={44} />
        <div>
          <b>Отложить</b>
          <span>Копилка для мечты. Монеты не тратятся сразу.</span>
        </div>
      </div>
    </div>
  );
}

export function HelpContent({ close }: { close: () => void }) {
  return (
    <div class="stack">
      <p class="lead">Ты растишь питомца. С монетами можно сделать три вещи:</p>
      <ThreeChoices />
      <p>Монет не хватит на всё, поэтому выбирай. Ошибаться можно: ошибка — это урок. Монеты в игре не настоящие.</p>
      <div class="row row--wrap">
        <Button variant="secondary" icon="book" onClick={() => { close(); nav.go('/glossary'); }}>Словарик</Button>
        <Button variant="secondary" icon="sparkle" onClick={() => { close(); nav.go('/intro?again=1'); }}>Знакомство ещё раз</Button>
      </div>
      <Button block onClick={close}>Понятно</Button>
    </div>
  );
}

export const openHelp = (): Promise<void> => ui.sheet((close) => <HelpContent close={close} />, 'Как играть');
