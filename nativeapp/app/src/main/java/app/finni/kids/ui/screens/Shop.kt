package app.finni.kids.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.finni.kids.app.ConfirmOptions
import app.finni.kids.app.Game
import app.finni.kids.app.Nav
import app.finni.kids.app.Ui
import app.finni.kids.app.present
import app.finni.kids.content.Item
import app.finni.kids.domain.Direction
import app.finni.kids.domain.ErrorCode
import app.finni.kids.domain.GameError
import app.finni.kids.domain.Outcome
import app.finni.kids.domain.ShortageId
import app.finni.kids.domain.STAT_LABEL
import app.finni.kids.domain.TxKind
import app.finni.kids.domain.buy
import app.finni.kids.domain.coinsText
import app.finni.kids.domain.envelope
import app.finni.kids.domain.previewPurchase
import app.finni.kids.domain.toggleWishlist
import app.finni.kids.domain.wearAccessory
import app.finni.kids.platform.Beep
import app.finni.kids.platform.Sound
import app.finni.kids.ui.BtnVariant
import app.finni.kids.ui.Card
import app.finni.kids.ui.Coin
import app.finni.kids.ui.DirTag
import app.finni.kids.ui.ButtonRow
import app.finni.kids.ui.FButton
import app.finni.kids.ui.Icon
import app.finni.kids.ui.Meter
import app.finni.kids.ui.MeterKind
import app.finni.kids.ui.Muted
import app.finni.kids.ui.NoteBox
import app.finni.kids.ui.STAT_ICON
import app.finni.kids.ui.Screen
import app.finni.kids.ui.SegOpt
import app.finni.kids.ui.Segmented
import app.finni.kids.ui.Tint
import app.finni.kids.ui.Tx
import app.finni.kids.ui.Tx2
import app.finni.kids.ui.explainError
import app.finni.kids.ui.signed
import app.finni.kids.ui.theme.Txt
import app.finni.kids.ui.theme.pal
import kotlinx.coroutines.launch

@Composable
private fun KV(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Muted(label); content()
    }
}

@Composable
private fun ItemActions(item: Item, close: () -> Unit) {
    val p = Game.profile ?: return
    val pv = previewPurchase(p, item)

    fun doBuy() = Ui.scope.launch {
        val r = Game.act { pp, c -> buy(pp, item.id, false, c) }
        if (r is Outcome.Ok) {
            close()
            present(r)
            return@launch
        }
        val e = (r as Outcome.Err).error
        when (e.code) {
            ErrorCode.CONFIRM_OVER_PLAN -> {
                val ok = Ui.confirm(
                    ConfirmOptions("Подожди! Этого не было в плане", "Купить всё равно", "Подумаю", careful = true, icon = "clip") {
                        Tx("Ты планировал на «Хочется» меньше: не хватает ${coinsText(e.overBy ?: 0)} до плана.")
                        Tx("Подумай: нужно ли это прямо сейчас? Можно отложить покупку в список желаний — это не ошибка.")
                    },
                )
                if (ok) {
                    val r2 = Game.act { pp, c -> buy(pp, item.id, true, c) }
                    close()
                    present(r2)
                }
            }
            ErrorCode.NOT_ENOUGH -> { close(); showShortage(item, e) }
            else -> { close(); explainError(e) }
        }
    }

    if (pv.alreadyOwned) FButton("Уже есть у питомца", {}, block = true, enabled = false)
    else FButton("Купить за ${item.price}", { doBuy() }, block = true, variant = if (item.kind == Direction.Must) BtnVariant.Primary else BtnVariant.Gold, icon = "bag")
    ButtonRow {
        if (item.kind == Direction.Want) FButton(if (pv.inWishlist) "Убрать из желаний" else "В список желаний", {
            val r = Game.act { pp, c -> toggleWishlist(pp, item.id, c) }
            if (r is Outcome.Ok) Ui.toast(r.report.title)
        }, variant = BtnVariant.Secondary, icon = "heart")
        FButton("Закрыть", close, variant = BtnVariant.Ghost)
    }
}

@Composable
private fun ItemBody(item: Item) {
    val p = Game.profile ?: return
    val pv = previewPurchase(p, item)
    val pl = pal
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(item.icon, 84.dp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DirTag(item.kind)
            Muted(item.blurb)
        }
    }
    Card {
        KV("Цена") { Coin(item.price) }
        KV("Останется монет") { Tx(if (pv.affordable) pv.balanceAfter.toString() else "не хватает ${pv.missing}", Txt.bodyBold, color = if (pv.affordable) pl.ink else pl.want) }
        if (pv.planConfirmed) KV("В конверте «${pv.categoryLabel}»") {
            Tx("${pv.envelope.left} → ${pv.envelope.leftAfter}" + if (pv.overPlanBy > 0 && item.kind == Direction.Want) " (больше плана)" else "", Txt.bodyBold)
        }
    }
    Card {
        Tx("Что будет с питомцем", Txt.bodyBold)
        for (e in pv.effects) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(STAT_ICON.getValue(e.stat), 30.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Tx2("${STAT_LABEL.getValue(e.stat)}: ${e.before} → ${e.after} ", "(${signed(e.gain)})")
                    Meter(e.after, label = STAT_LABEL.getValue(e.stat), kind = MeterKind.Ok, showNumbers = false)
                }
            }
        }
        if (pv.wasteful) NoteBox("Питомцу это сейчас почти не нужно — часть пропадёт зря.")
        if (item.accessory != null) Muted("Питомец сразу наденет это украшение.")
    }
    if (!pv.planConfirmed) NoteBox("Сначала составь план недели — тогда можно покупать.")
}

private suspend fun showShortage(item: Item, error: GameError) {
    Sound.play(Beep.Oops)
    Ui.sheet("Пока не хватает монет") { close ->
        val cheaper = error.options?.find { it.id == ShortageId.Cheaper }
        Card(tint = Tint.Warn) {
            Tx("Не хватает ${coinsText(error.missing ?: 0)}", Txt.bodyBold)
            Tx("«${item.name}» стоит ${coinsText(item.price)}, а в кошельке пока меньше. Ничего не потеряно — вот что можно сделать:")
        }
        for (o in error.options.orEmpty()) {
            Card(gap = 6.dp) {
                Tx(o.label, Txt.bodyBold)
                Muted(o.hint)
                o.route?.let { r -> FButton("Перейти", { close(); Nav.go(r) }, small = true, variant = BtnVariant.Secondary) }
                if (o.id == ShortageId.Wishlist) FButton("Добавить в желания", {
                    Game.act { pp, c -> toggleWishlist(pp, item.id, c) }
                    Ui.toast("Добавлено в список желаний"); close()
                }, small = true, variant = BtnVariant.Secondary, icon = "heart")
                if (o.id == ShortageId.Cheaper && cheaper?.itemIds != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (id in cheaper.itemIds) {
                            val it = Game.content.items.first { i -> i.id == id }
                            MiniItem(it) { close(); Ui.scope.launch { openItem(it) } }
                        }
                    }
                }
            }
        }
        FButton("Понятно", close, block = true)
    }
}

@Composable
private fun MiniItem(it: Item, onClick: () -> Unit) {
    val pl = pal
    Row(
        Modifier.heightIn(min = 48.dp).clip(CircleShape).background(pl.primary.copy(alpha = 0.1f)).border(2.dp, pl.primary, CircleShape).clickable(role = Role.Button) { onClick() }.padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(it.icon, 30.dp); Tx(it.name, Txt.bodyBold, color = Color(0xFFDFFCF8)); Tx(it.price.toString(), Txt.bodyBold.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xFFDFFCF8))
    }
}

suspend fun openItem(item: Item) = Ui.sheet(item.name, actions = { close -> ItemActions(item, close) }) { ItemBody(item) }

@Composable
private fun ItemCard(item: Item, modifier: Modifier) {
    val p = Game.profile ?: return
    val pl = pal
    val owned = item.accessory != null && item.accessory in p.pet.ownedAccessories
    val afford = p.wallet >= item.price
    val stripe = if (item.kind == Direction.Must) pl.must else pl.want
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier.heightIn(min = 148.dp).clip(shape).background(if (pl.highContrast) Color.Black else pl.surface)
            .drawBehind { drawRect(stripe, Offset(0f, size.height - 5.dp.toPx()), Size(size.width, 5.dp.toPx())) }
            .border(if (pl.highContrast) 3.dp else 1.5.dp, pl.line, shape)
            .clickable(role = Role.Button) { Sound.play(Beep.Tap); Ui.scope.launch { openItem(item) } }.padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(item.icon, 64.dp)
            Tx(item.name, Txt.bodyBold.copy(fontSize = Txt.bodyBold.fontSize * 0.96f, lineHeight = Txt.bodyBold.fontSize * 1.1f), align = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon("coin", 20.dp); Tx(item.price.toString(), Txt.bodyBold.copy(fontWeight = FontWeight.ExtraBold, fontSize = Txt.bodyBold.fontSize * 1.1f))
            }
            if (owned) Badge("Есть", pl.save, pl.saveBg, "check")
            else if (!afford) Badge("Не хватает ${item.price - p.wallet}", pl.warn, pl.warnBg, null)
        }
        if (item.id in p.wishlist) Box(Modifier.align(Alignment.TopEnd)) { Icon("heart", 18.dp, tint = Color(0xFFFF8FB8), description = "В списке желаний") }
    }
}

@Composable
private fun Badge(text: String, fg: Color, bg: Color, icon: String?) {
    Row(Modifier.clip(CircleShape).background(bg).border(1.dp, fg.copy(alpha = 0.5f), CircleShape).padding(horizontal = 8.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Icon(icon, 16.dp, tint = fg)
        Tx(text, Txt.tiny.copy(fontWeight = FontWeight.Bold), color = fg)
    }
}

@Composable
fun ShopScreen() {
    val p = Game.profile ?: return
    var tab by remember { mutableStateOf(Direction.Must) }
    val items = Game.content.items.filter { it.kind == tab }
    val env = envelope(p, if (tab == Direction.Must) Direction.Must else Direction.Want)
    val owned = p.pet.ownedAccessories
    val thisWeek = p.ledger.filter { it.period == p.period.index && (it.kind == TxKind.Purchase || it.kind == TxKind.Event) }

    Screen("Магазин", hint = "Выбери товар: увидишь цену и что будет с питомцем.") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(Modifier.weight(1f), padding = 10.dp) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Tx("Кошелёк", Txt.body.copy(fontSize = Txt.body.fontSize * 0.92f), maxLines = 1); Coin(p.wallet) } }
            Card(Modifier.weight(1f), padding = 10.dp) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Tx("Конверт", Txt.body.copy(fontSize = Txt.body.fontSize * 0.92f), maxLines = 1); Tx(if (p.period.planConfirmed) "${env.left} из ${env.planned}" else "нет плана", Txt.bodyBold.copy(fontSize = Txt.bodyBold.fontSize * 0.92f), maxLines = 1) } }
        }
        if (!p.period.planConfirmed) Card(tint = Tint.Gold) {
            Tx2("Покупки откроются после плана. ", "Реши, сколько потратить на нужное и желаемое.")
            FButton("К плану", { Nav.go("/plan") }, small = true)
        }
        Segmented(tab, listOf(SegOpt(Direction.Must, "Нужное", "bowl"), SegOpt(Direction.Want, "Хочется", "ball")), { tab = it })
        Grid(items, 2, gap = 10.dp) { it, m -> ItemCard(it, m) }

        if (tab == Direction.Want && owned.isNotEmpty()) Card {
            Tx("Мои украшения", Txt.bodyBold)
            val wearing = p.pet.appearance.accessory
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FButton("Без украшения", { Game.act { pp, _ -> wearAccessory(pp, "none") } }, small = true, variant = if (wearing == "none") BtnVariant.Primary else BtnVariant.Secondary)
                for (a in owned) FButton(Game.content.pet.accessories.find { it.id == a }?.name ?: a, { Game.act { pp, _ -> wearAccessory(pp, a) } }, small = true, variant = if (wearing == a) BtnVariant.Primary else BtnVariant.Secondary)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Tx("Покупки этой недели", Txt.h3)
            LedgerList(thisWeek, "На этой неделе покупок ещё не было.")
        }
    }
}
