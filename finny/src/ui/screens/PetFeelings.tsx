import { ui } from '../../app/ui';
import { getProfile } from '../../app/store';
import { ECON, STATS } from '../../domain/econ';
import { STAT_LABEL, petExpression } from '../../domain/profile';
import { Button, Meter, STAT_ICON } from '../components/kit';
import { Icon } from '../icons/Icon';
import { nav } from '../../app/nav';

const WHY: Record<string, { low: string; ok: string; fix: string }> = {
  satiety: { low: 'Давно не было еды.', ok: 'Питомец сыт.', fix: 'Еда — в магазине, раздел «Нужное».' },
  care: { low: 'Питомцу не хватает ухода.', ok: 'Питомец чистый и здоровый.', fix: 'Купание и расчёска — в «Нужном».' },
  mood: { low: 'Питомцу скучно.', ok: 'Питомцу весело.', fix: 'Игрушки — в разделе «Хочется». Копить и держаться плана тоже радует.' },
};

/** Объясняет, почему питомец так себя чувствует, и что можно сделать. */
export function openPetFeelings(): void {
  const p = getProfile();
  if (!p) return;
  const expr = petExpression(p.pet.stats);
  void ui.sheet(
    (close) => (
      <div class="stack">
        <p class="lead">{p.pet.reason?.text ?? expr.hint}</p>
        <ul class="feelings">
          {STATS.map((s) => {
            const low = p.pet.stats[s] < ECON.statLow;
            return (
              <li key={s}>
                <Icon name={STAT_ICON[s]} size={36} />
                <div>
                  <b>{STAT_LABEL[s]}: {p.pet.stats[s]} из 100</b>
                  <Meter value={p.pet.stats[s]} label={STAT_LABEL[s]} kind={low ? 'low' : 'ok'} showNumbers={false} />
                  <span>{low ? WHY[s].low : WHY[s].ok} {low && WHY[s].fix}</span>
                </div>
              </li>
            );
          })}
        </ul>
        <p class="muted">Каждую неделю показатели чуть снижаются — так бывает у всех. Питомец никогда не болеет из-за твоих ошибок.</p>
        <div class="row row--wrap">
          <Button variant="secondary" onClick={() => { close(); nav.go('/shop'); }}>В магазин</Button>
          <Button onClick={close}>Понятно</Button>
        </div>
      </div>
    ),
    `${p.pet.name}: ${expr.label.toLowerCase()}`,
  );
}
