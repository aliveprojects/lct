package app.finni.kids.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.finni.kids.app.Game
import app.finni.kids.app.Nav
import app.finni.kids.app.Ui
import app.finni.kids.app.present
import app.finni.kids.domain.Change
import app.finni.kids.domain.ChangeKind
import app.finni.kids.domain.ExpressionCode
import app.finni.kids.domain.Mode
import app.finni.kids.domain.NewProfileInput
import app.finni.kids.domain.NextLink
import app.finni.kids.domain.Outcome
import app.finni.kids.domain.PetAppearance
import app.finni.kids.domain.Report
import app.finni.kids.domain.TextScale
import app.finni.kids.domain.Tone
import app.finni.kids.domain.newProfile
import app.finni.kids.domain.withProfile
import app.finni.kids.ui.BtnVariant
import app.finni.kids.ui.FButton
import app.finni.kids.ui.Icon
import app.finni.kids.ui.LinkButton
import app.finni.kids.ui.Muted
import app.finni.kids.ui.SegOpt
import app.finni.kids.ui.Segmented
import app.finni.kids.ui.Screen
import app.finni.kids.ui.Tx
import app.finni.kids.ui.pet.PetView
import app.finni.kids.ui.theme.Txt
import app.finni.kids.ui.theme.pal
import kotlinx.coroutines.launch

// ---------- Приветствие ----------

@Composable
fun WelcomeScreen() {
    val app = Game.app
    val pl = pal
    Box(Modifier.fillMaxSize()) {
        if (!pl.highContrast) Canvas(Modifier.fillMaxSize()) {
            // планета внизу экрана
            val w = size.width * 1.7f
            val top = size.height * 0.80f
            drawOval(
                Brush.radialGradient(0f to Color(0xFF7A63FF), 0.42f to Color(0xFF4436C4), 0.78f to Color(0xFF1C1A78), center = Offset(size.width / 2f, top), radius = w / 2f),
                Offset(-(w - size.width) / 2f, top), androidx.compose.ui.geometry.Size(w, size.height * 0.7f),
            )
        }
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PetView(PetAppearance("cat", "mint", "bow"), 2, Modifier.widthIn(max = 240.dp).fillMaxWidth(0.56f), expression = ExpressionCode.Happy, label = "Финни, питомец-талисман")
                Tx("Финни", Txt.huge.copy(fontSize = Txt.huge.fontSize * 1.05f, brush = if (pl.highContrast) null else Brush.horizontalGradient(listOf(Color(0xFF7FF3E4), Color(0xFFB79BFF), Color(0xFFFF9ECB)))), color = if (pl.highContrast) Color.White else Color(0xFFB79BFF), align = TextAlign.Center)
                Tx("Привет! Меня зовут Финни. Давай растить питомца и учиться обращаться с монетами. Всё понарошку.", Txt.lead, align = TextAlign.Center)
            }
            FButton("Начать", { Nav.go(if (app.introSeen) "/create" else "/intro") }, block = true, icon = "sparkle")
        }
    }
}

// ---------- Знакомство ----------

@Composable
fun IntroScreen(again: Boolean) {
    var i by remember { mutableIntStateOf(0) }
    val total = 3
    val last = i == total - 1
    val finish = {
        Game.mutate { it.copy(introSeen = true) }
        if (again) Nav.back() else Nav.replace("/create")
    }
    Screen(
        "Знакомство", help = false, back = again,
        right = { if (!again) LinkButton("Пропустить", { finish() }) },
        footer = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Dots(total, i)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (i > 0) FButton("Назад", { i-- }, variant = BtnVariant.Secondary)
                    FButton(if (last) (if (again) "Понятно" else "Создать питомца") else "Дальше", { if (last) finish() else i++ }, Modifier.weight(1f), block = true)
                }
            }
        },
    ) {
        val titles = listOf("Привет, я Финни!", "Три решения", "Выбирай с умом")
        Tx(titles[i], Txt.h1, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        when (i) {
            0 -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PetView(PetAppearance("cat", "mint", "bow"), 1, Modifier.widthIn(max = 220.dp).fillMaxWidth(0.54f), expression = ExpressionCode.Happy, label = "Финни, питомец-малыш")
                Tx("Мне нужна твоя забота. Ты решаешь, на что потратить монеты, — и я расту. Твоего питомца тоже можно назвать как хочешь!", Txt.lead, align = TextAlign.Center)
            }
            1 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Tx("С монетами можно сделать три вещи:", Txt.lead)
                ThreeChoices()
            }
            else -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon("coin", 72.dp); Icon("arrow", 32.dp); Icon("jar", 72.dp)
                }
                Tx("Монет не хватит на всё, поэтому выбирай.", Txt.lead, align = TextAlign.Center)
                Tx("Ошибаться можно: ошибка — это урок, и её можно исправить. Монеты в игре не настоящие.", align = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Dots(total: Int, on: Int, upTo: Boolean = false) {
    val pl = pal
    Row(Modifier.fillMaxWidth().semantics { contentDescription = "Шаг ${on + 1} из $total" }, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        repeat(total) { k ->
            val lit = if (upTo) k <= on else k == on
            Box(Modifier.size(12.dp).clip(CircleShape).background(if (lit) pl.primary else Color.White.copy(alpha = 0.28f)))
        }
    }
}

// ---------- Создание профиля и питомца ----------

private fun sanitize(s: String): String = s.filter { it.isLetterOrDigit() || it == ' ' || it == '-' }.take(12)
private fun valid(s: String) = s.trim().length >= 2

@Composable
private fun NameField(label: String, value: String, onChange: (String) -> Unit, placeholder: String) {
    val pl = pal
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Tx(label, Txt.bodyBold)
        val shape = RoundedCornerShape(16.dp)
        BasicTextField(
            value, { onChange(sanitize(it)) }, singleLine = true,
            textStyle = Txt.bodyBold.copy(fontSize = Txt.bodyBold.fontSize * 1.18f, color = pl.ink),
            cursorBrush = SolidColor(pl.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focus.clearFocus() }),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(Color(0x99060928)).border(2.5.dp, pl.line, shape).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Tx(placeholder, Txt.body, color = pl.ink2.copy(alpha = 0.7f))
                    inner()
                }
            },
        )
    }
}

@Composable
private fun PickCard(selected: Boolean, label: String, modifier: Modifier, onClick: () -> Unit, art: @Composable () -> Unit) {
    val pl = pal
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.heightIn(min = 84.dp).clip(shape).background(if (selected) pl.primarySoft else pl.surface).border(2.5.dp, if (selected) pl.primary else pl.line, shape)
            .semantics { contentDescription = label + if (selected) ", выбрано" else "" }.clickable(role = Role.RadioButton) { onClick() }.padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        art()
        Tx(label, Txt.tiny.copy(fontSize = Txt.tiny.fontSize * 1.05f, fontWeight = FontWeight.SemiBold), align = TextAlign.Center)
    }
}

@Composable
fun CreateScreen() {
    val step = (Nav.current.substringAfter('?', "").split('&').firstOrNull { it.startsWith("step=") }?.removePrefix("step=")?.toIntOrNull() ?: 0).coerceIn(0, 2)
    val scope = app.finni.kids.app.Ui.scope
    val content = Game.content
    var player by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("cat") }
    var color by remember { mutableStateOf("peach") }
    var accessory by remember { mutableStateOf("none") }
    var petName by remember { mutableStateOf(content.pet.defaultPetName) }
    val startAcc = content.pet.accessories.filter { it.atStart }
    val appearance = PetAppearance(species, color, accessory)
    val xl = Game.app.settings.textScale == TextScale.XLarge

    suspend fun finish() {
        val ctx = Game.ctx
        val profile = newProfile(NewProfileInput(player.trim(), petName.trim(), appearance, Game.app.mode == Mode.Demo), ctx)
        Game.setState(withProfile(Game.app, profile))
        Nav.reset("/")
        val first = profile.ledger[0]
        val report = Report(
            title = "Стартовый подарок!", tone = Tone.Good,
            changes = listOf(Change(ChangeKind.Wallet, "Монеты", delta = first.amount, before = 0, after = first.balance, why = "Это подарок для начала игры. Монеты в игре не настоящие.")),
            explain = "Теперь у тебя есть монеты. Сначала составь план недели: реши, сколько потратить на нужное, на желаемое и сколько отложить.",
            next = NextLink("Составить план", "/plan"),
        )
        present(Outcome.Ok(profile, report, Unit))
    }

    val ok = when (step) { 0 -> valid(player); 1 -> true; else -> valid(petName) }
    Screen(
        "Новый питомец", back = step > 0,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step > 0) FButton("Назад", { Nav.back() }, variant = BtnVariant.Secondary)
                FButton(if (step == 2) "Готово!" else "Дальше", { if (step == 2) Ui.scope.launch { finish() } else Nav.go("/create?step=${step + 1}") }, Modifier.weight(1f), block = true, enabled = ok)
            }
        },
    ) {
        Dots(3, step, upTo = true)
        Tx(listOf("Как тебя зовут в игре?", "Выбери друга", "Как назовём питомца?")[step], Txt.h1, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        when (step) {
            0 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Tx("Придумай игровое имя. Настоящее имя писать не нужно.", Txt.lead)
                NameField("Игровое имя", player, { player = it }, "Например, Ракета")
                FButton("Придумать за меня", { player = content.pet.playerNames.random() }, variant = BtnVariant.Secondary, icon = "sparkle")
            }
            1 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PetView(appearance, 1, Modifier.widthIn(max = 200.dp).fillMaxWidth(0.5f), expression = ExpressionCode.Happy, label = "Твой питомец") }
                Tx("Кто это?", Txt.bodyBold)
                Grid(content.pet.species, if (xl) 2 else 4) { sp, m ->
                    PickCard(sp.id == species, sp.name, m, { species = sp.id }) {
                        PetView(PetAppearance(sp.id, color, "none"), 2, Modifier.size(64.dp), expression = ExpressionCode.Content)
                    }
                }
                Tx("Цвет", Txt.bodyBold)
                Grid(content.pet.colors, if (xl) 2 else 4) { c, m ->
                    PickCard(c.id == color, c.name, m, { color = c.id }) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(0.6f to hex(c.body), 1f to hex(c.dark))).border(3.dp, Color.White.copy(alpha = 0.85f), CircleShape))
                    }
                }
                Tx("Украшение", Txt.bodyBold)
                Segmented(accessory, startAcc.map { SegOpt(it.id, it.name) }, { accessory = it }, columns = 2)
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PetView(appearance, 1, Modifier.widthIn(max = 200.dp).fillMaxWidth(0.5f), expression = ExpressionCode.Happy, label = "Твой питомец") }
                NameField("Имя питомца", petName, { petName = it }, "Например, Финни")
                FButton("Придумать за меня", { petName = content.pet.petNames.random() }, variant = BtnVariant.Secondary, icon = "sparkle")
            }
        }
        if (!ok) Muted("Нужно хотя бы 2 буквы.", align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

private fun hex(s: String): Color = Color(android.graphics.Color.parseColor(s))

/** Сетка из [columns] столбцов: ячейки одинаковой ширины. */
@Composable
fun <T> Grid(items: List<T>, columns: Int, gap: androidx.compose.ui.unit.Dp = 8.dp, cell: @Composable (T, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { cell(it, Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
