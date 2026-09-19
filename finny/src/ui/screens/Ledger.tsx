import { ui } from '../../app/ui';
import { getProfile } from '../../app/store';
import { ECON } from '../../domain/econ';
import type { Tx } from '../../domain/types';
import { Button, MINUS } from '../components/kit';
import { Icon } from '../icons/Icon';

const KIND_ICON: Record<Tx['kind'], string> = {
  start: 'gift',
  allowance: 'coin',
  daily: 'gift',
  task: 'star',
  bonus: 'smile',
  purchase: 'bag',
  event: 'vet',
  save: 'jar',
  withdraw: 'jar',
  goal: 'trophy',
};

const KIND_TEXT: Record<Tx['kind'], string> = {
  start: 'Подарок',
  allowance: 'Доход',
  daily: 'Доход',
  task: 'Доход',
  bonus: 'Доход',
  purchase: 'Покупка',
  event: 'Непредвиденное',
  save: 'В копилку',
  withdraw: 'Из копилки',
  goal: 'Мечта',
};

export function LedgerList({ tx, empty }: { tx: Tx[]; empty?: string }) {
  if (!tx.length) return <p class="muted">{empty ?? 'Пока ничего не происходило.'}</p>;
  return (
    <ul class="ledger">
      {[...tx].reverse().map((t) => (
        <li key={t.id} class="ledger__row">
          <Icon name={KIND_ICON[t.kind]} size={34} />
          <div class="ledger__body">
            <b>{t.title}</b>
            <small>
              {KIND_TEXT[t.kind]} · неделя {t.period}
              {t.dir === 'must' ? ' · нужное' : t.dir === 'want' ? ' · хочется' : ''}
            </small>
          </div>
          <div class="ledger__sum">
            {t.amount !== 0 && <b class={t.amount > 0 ? 'is-up' : 'is-down'}>{t.amount > 0 ? `+${t.amount}` : `${MINUS}${Math.abs(t.amount)}`}</b>}
            {t.savingsDelta ? <small>копилка {t.savingsDelta > 0 ? `+${t.savingsDelta}` : `${MINUS}${Math.abs(t.savingsDelta)}`}</small> : null}
            <small>баланс {t.balance}</small>
          </div>
        </li>
      ))}
    </ul>
  );
}

/** «Откуда монеты?»: каждое изменение баланса с источником и суммой. */
export function openLedger(): void {
  const p = getProfile();
  if (!p) return;
  void ui.sheet(
    (close) => (
      <div class="stack">
        <p class="muted">Здесь видно, откуда пришли монеты и куда ушли. Баланс не меняется без объяснения.</p>
        <LedgerList tx={p.ledger.slice(-ECON.ledgerView)} />
        <Button block onClick={close}>Закрыть</Button>
      </div>
    ),
    'История монет',
  );
}
