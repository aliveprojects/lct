import { useState } from 'preact/hooks';
import { explainError, present } from '../../app/actions';
import { nav } from '../../app/nav';
import { act, useAppState } from '../../app/store';
import { ui } from '../../app/ui';
import { content } from '../../content';
import type { Item } from '../../content/types';
import { coinsText } from '../../domain/econ';
import { envelope, STAT_LABEL } from '../../domain/profile';
import { buy, previewPurchase, toggleWishlist, wearAccessory } from '../../domain/shop';
import type { Direction, Profile } from '../../domain/types';
import { playSound } from '../../platform/sound';
import { Button, Coin, Meter, Segmented, STAT_ICON, Tag, signed } from '../components/kit';
import { Screen } from '../components/Screen';
import { Icon } from '../icons/Icon';
import { LedgerList } from './Ledger';

function ItemSheet({ item, close }: { item: Item; close: () => void }) {
  const app = useAppState();
  const p = (app.mode === 'demo' ? app.demo : app.main)!;
  const pv = previewPurchase(p, item);

  const doBuy = async () => {
    const r = act((pp, c) => buy(pp, item.id, {}, c));
    if (r.ok) {
      close();
      await present(r);
      return;
    }
    if (r.error.code === 'CONFIRM_OVER_PLAN') {
      const ok = await ui.confirm({
        title: 'Подожди! Этого не было в плане',
        icon: 'clip',
        tone: 'careful',
        body: (
          <div class="stack">
            <p>Ты планировал на «Хочется» меньше: не хватает {coinsText(r.error.overBy ?? 0)} до плана.</p>
            <p>Подумай: нужно ли это прямо сейчас? Можно отложить покупку в список желаний — это не ошибка.</p>
          </div>
        ),
        confirmLabel: 'Купить всё равно',
        cancelLabel: 'Подумаю',
      });
      if (ok) {
        const r2 = act((pp, c) => buy(pp, item.id, { confirmOverPlan: true }, c));
        close();
        await present(r2);
      }
      return;
    }
    if (r.error.code === 'NOT_ENOUGH') {
      close();
      await showShortage(item, r.error);
      return;
    }
    close();
    await explainError(r.error);
  };

  const wish = async () => {
    const r = act((pp, c) => toggleWishlist(pp, item.id, c));
    if (r.ok) ui.toast(r.report.title);
  };

  return (
    <div class="stack">
      <div class="itemhero">
        <Icon name={item.icon} size={84} />
        <div>
          <Tag dir={item.kind} />
          <p class="muted">{item.blurb}</p>
        </div>
      </div>

      <div class="card">
        <div class="kv"><span>Цена</span><Coin n={item.price} /></div>
        <div class="kv"><span>Останется монет</span><b class={pv.affordable ? '' : 'is-down'}>{pv.affordable ? pv.balanceAfter : `не хватает ${pv.missing}`}</b></div>
        {pv.planConfirmed && (
          <div class="kv">
            <span>В конверте «{pv.categoryLabel}»</span>
            <b>{pv.envelope.left} → {pv.envelope.leftAfter}{pv.overPlanBy > 0 && item.kind === 'want' ? ' (больше плана)' : ''}</b>
          </div>
        )}
      </div>

      <div class="card">
        <b>Что будет с питомцем</b>
        <ul class="effects">
          {pv.effects.map((e) => (
            <li key={e.stat}>
              <Icon name={STAT_ICON[e.stat]} size={30} />
              <div>
                <span>{STAT_LABEL[e.stat]}: {e.before} → {e.after} <b>({signed(e.gain)})</b></span>
                <Meter value={e.after} label={STAT_LABEL[e.stat]} kind="ok" showNumbers={false} />
              </div>
            </li>
          ))}
        </ul>
        {pv.wasteful && <p class="note"><Icon name="info" size={22} /> Питомцу это сейчас почти не нужно — часть пропадёт зря.</p>}
        {item.accessory && <p class="muted">Питомец сразу наденет это украшение.</p>}
      </div>

      {!pv.planConfirmed && <p class="note"><Icon name="clip" size={22} /> Сначала составь план недели — тогда можно покупать.</p>}

      <div class="stack">
        {pv.alreadyOwned ? (
          <Button block disabled>Уже есть у питомца</Button>
        ) : (
          <Button block variant={item.kind === 'must' ? 'primary' : 'gold'} icon="bag" onClick={() => void doBuy()}>
            Купить за {item.price}
          </Button>
        )}
        <div class="row row--wrap">
          {item.kind === 'want' && (
            <Button variant="secondary" icon="heart" onClick={() => void wish()}>
              {pv.inWishlist ? 'Убрать из желаний' : 'В список желаний'}
            </Button>
          )}
          <Button variant="ghost" onClick={close}>Закрыть</Button>
        </div>
      </div>
    </div>
  );
}

async function showShortage(item: Item, error: import('../../domain/types').GameError): Promise<void> {
  playSound('oops');
  await ui.sheet(
    (close) => {
      const cheaper = error.options?.find((o) => o.id === 'cheaper');
      return (
        <div class="stack">
          <div class="card card--tint-warn">
            <b>Не хватает {coinsText(error.missing ?? 0)}</b>
            <p>«{item.name}» стоит {coinsText(item.price)}, а в кошельке пока меньше. Ничего не потеряно — вот что можно сделать:</p>
          </div>
          {error.options?.map((o) => (
            <div class="card card--option" key={o.id}>
              <b>{o.label}</b>
              <span class="muted">{o.hint}</span>
              {o.route && <Button small variant="secondary" onClick={() => { close(); nav.go(o.route!); }}>Перейти</Button>}
              {o.id === 'wishlist' && (
                <Button small variant="secondary" icon="heart" onClick={() => { act((pp, c) => toggleWishlist(pp, item.id, c)); ui.toast('Добавлено в список желаний'); close(); }}>
                  Добавить в желания
                </Button>
              )}
              {o.id === 'cheaper' && cheaper?.itemIds && (
                <div class="row row--wrap">
                  {cheaper.itemIds.map((id) => {
                    const it = content.items.find((i) => i.id === id)!;
                    return (
                      <button key={id} type="button" class="minitem" onClick={() => { close(); void openItem(it); }}>
                        <Icon name={it.icon} size={30} />
                        <span>{it.name}</span>
                        <b>{it.price}</b>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
          ))}
          <Button block onClick={close}>Понятно</Button>
        </div>
      );
    },
    'Пока не хватает монет',
  );
}

function openItem(item: Item): Promise<void> {
  return ui.sheet((close) => <ItemSheet item={item} close={close} />, item.name);
}

function Card({ item, p }: { item: Item; p: Profile }) {
  const owned = !!item.accessory && p.pet.ownedAccessories.includes(item.accessory);
  const afford = p.wallet >= item.price;
  return (
    <button type="button" class={`item item--${item.kind} ${owned ? 'is-owned' : ''}`} onClick={() => void openItem(item)}>
      <Icon name={item.icon} size={64} />
      <b class="item__name">{item.name}</b>
      <span class="item__price">
        <Icon name="coin" size={20} />
        {item.price}
      </span>
      {owned && <span class="item__badge"><Icon name="check" size={16} /> Есть</span>}
      {!owned && !afford && <span class="item__badge item__badge--need">Не хватает {item.price - p.wallet}</span>}
      {p.wishlist.includes(item.id) && <span class="item__heart" aria-label="В списке желаний"><Icon name="heart" size={18} /></span>}
    </button>
  );
}

export function Shop() {
  const app = useAppState();
  const p = (app.mode === 'demo' ? app.demo : app.main)!;
  const [tab, setTab] = useState<Direction>('must');
  const items = content.items.filter((i) => i.kind === tab);
  const env = envelope(p, tab === 'must' ? 'must' : 'want');
  const owned = p.pet.ownedAccessories;
  const thisWeek = p.ledger.filter((t) => t.period === p.period.index && (t.kind === 'purchase' || t.kind === 'event'));

  return (
    <Screen title="Магазин" hint="Выбери товар: увидишь цену и что будет с питомцем.">
      <div class="row shopbar">
        <div class="card card--mini">
          <span>Кошелёк</span>
          <Coin n={p.wallet} />
        </div>
        <div class="card card--mini">
          <span>Конверт</span>
          <b>{p.period.planConfirmed ? `${env.left} из ${env.planned}` : 'нет плана'}</b>
        </div>
      </div>
      {!p.period.planConfirmed && (
        <div class="card card--tint-gold">
          <p><b>Покупки откроются после плана.</b> Реши, сколько потратить на нужное и желаемое.</p>
          <Button small onClick={() => nav.go('/plan')}>К плану</Button>
        </div>
      )}
      <Segmented
        label="Тип покупок"
        value={tab}
        onChange={(v) => setTab(v)}
        options={[
          { id: 'must', label: 'Нужное', icon: 'bowl' },
          { id: 'want', label: 'Хочется', icon: 'ball' },
        ]}
      />
      <div class="grid2">
        {items.map((it) => (
          <Card key={it.id} item={it} p={p} />
        ))}
      </div>

      {tab === 'want' && owned.length > 0 && (
        <div class="card">
          <b>Мои украшения</b>
          <div class="row row--wrap">
            <Button small variant={p.pet.appearance.accessory === 'none' ? 'primary' : 'secondary'} onClick={() => act((pp) => wearAccessory(pp, 'none'))}>Без украшения</Button>
            {owned.map((a) => (
              <Button key={a} small variant={p.pet.appearance.accessory === a ? 'primary' : 'secondary'} onClick={() => act((pp) => wearAccessory(pp, a))}>
                {content.pet.accessories.find((x) => x.id === a)?.name ?? a}
              </Button>
            ))}
          </div>
        </div>
      )}

      <div class="stack">
        <h2 class="h3">Покупки этой недели</h2>
        <LedgerList tx={thisWeek} empty="На этой неделе покупок ещё не было." />
      </div>
    </Screen>
  );
}
