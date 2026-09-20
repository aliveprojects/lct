package app.finni.kids.domain

import app.finni.kids.content.Item

data class EffectPreview(val stat: PetStat, val before: Int, val after: Int, val gain: Int, val wasted: Int)

data class EnvelopePreview(val planned: Int, val used: Int, val left: Int, val leftAfter: Int)

data class PurchasePreview(
    val item: Item,
    val price: Int,
    val kind: Direction,
    val categoryLabel: String,
    val effects: List<EffectPreview>,
    val planConfirmed: Boolean,
    val affordable: Boolean,
    val missing: Int,
    val envelope: EnvelopePreview,
    val overPlanBy: Int,
    val wasteful: Boolean,
    val alreadyOwned: Boolean,
    val inWishlist: Boolean,
    val balanceAfter: Int,
)

fun previewPurchase(p: Profile, item: Item): PurchasePreview {
    val env = envelope(p, item.kind)
    val effects = STATS.filter { (item.effects[it] ?: 0) != 0 }.map { stat ->
        val before = p.pet.stats[stat]
        val raw = item.effects[stat] ?: 0
        val after = minOf(Econ.statMax, before + raw)
        EffectPreview(stat, before, after, gain = after - before, wasted = raw - (after - before))
    }
    val alreadyOwned = item.accessory != null && item.accessory in p.pet.ownedAccessories
    val overPlanBy = maxOf(0, item.price - env.left)
    return PurchasePreview(
        item = item,
        price = item.price,
        kind = item.kind,
        categoryLabel = DIRECTION_LABEL.getValue(item.kind),
        effects = effects,
        planConfirmed = p.period.planConfirmed,
        affordable = p.wallet >= item.price,
        missing = maxOf(0, item.price - p.wallet),
        envelope = EnvelopePreview(env.planned, env.used, env.left, leftAfter = maxOf(0, env.left - item.price)),
        overPlanBy = overPlanBy,
        wasteful = effects.isNotEmpty() && effects.all { it.before >= Econ.statHigh },
        alreadyOwned = alreadyOwned,
        inWishlist = item.id in p.wishlist,
        balanceAfter = p.wallet - item.price,
    )
}

/** Что можно сделать, если монет не хватает: понятные варианты вместо тупика. */
fun shortageOptions(p: Profile, item: Item, ctx: Ctx): List<ShortageOption> {
    val options = mutableListOf<ShortageOption>()
    val missing = item.price - p.wallet
    val openTasks = availableTasks(p, ctx).filter { (p.tasks[it.id]?.stars ?: 0) < 3 }
    if (openTasks.isNotEmpty()) {
        val best = openTasks.maxOf { it.rewards.three }
        options += ShortageOption(ShortageId.Earn, "Заработать монеты", "За задания дают до ${coinsText(best)}.", route = "/tasks")
    }
    val cheaper = ctx.content.items
        .filter { it.kind == item.kind && it.price <= p.wallet && it.id != item.id && !(it.accessory != null && it.accessory in p.pet.ownedAccessories) }
        .sortedByDescending { it.price }
        .take(3)
        .map { it.id }
    if (cheaper.isNotEmpty()) options += ShortageOption(ShortageId.Cheaper, "Выбрать подешевле", "Есть похожие вещи, которые ты можешь купить сейчас.", itemIds = cheaper)
    if (item.kind == Direction.Want && item.id !in p.wishlist)
        options += ShortageOption(ShortageId.Wishlist, "Отложить в список желаний", "Вернёшься к этой вещи на следующей неделе. Это не ошибка.")
    if (item.kind == Direction.Must && savingsTotal(p) + p.wallet >= item.price)
        options += ShortageOption(ShortageId.Savings, "Взять из копилки", "Не хватает ${coinsText(missing)}. Копилку можно потратить, но сначала подумай.", route = "/goals")
    options += ShortageOption(ShortageId.Wait, "Подождать новую неделю", "В начале недели приходят карманные монеты.")
    return options
}

fun buy(p0: Profile, itemId: String, confirmOverPlan: Boolean, ctx: Ctx): Res {
    val item = ctx.content.items.find { it.id == itemId } ?: return fail(ErrorCode.ITEM_NOT_FOUND, "Такого товара нет.")
    if (!p0.period.planConfirmed) return fail(ErrorCode.PLAN_REQUIRED, "Сначала составь план недели — так ты будешь знать, сколько можно потратить.")
    val pv = previewPurchase(p0, item)
    if (pv.alreadyOwned) return fail(ErrorCode.ALREADY_OWNED, "Это украшение у питомца уже есть.")
    if (!pv.affordable)
        return fail(ErrorCode.NOT_ENOUGH, "Не хватает ${coinsText(pv.missing)}.", missing = pv.missing, options = shortageOptions(p0, item, ctx))
    if (item.kind == Direction.Want && pv.overPlanBy > 0 && !confirmOverPlan)
        return fail(ErrorCode.CONFIRM_OVER_PLAN, "Этого нет в плане: на «Хочется» не хватает ${coinsText(pv.overPlanBy)}.", overBy = pv.overPlanBy)

    val p = p0.deepCopy()
    val before = p.wallet
    p.wallet -= item.price
    when (item.kind) {
        Direction.Must -> p.period.spent.must += item.price
        Direction.Want -> p.period.spent.want += item.price
        Direction.Save -> Unit
    }
    if (item.need == "food") p.period.needs.food += 1
    if (item.need == "care") p.period.needs.care += 1
    p.wishlist = p.wishlist.filter { it != item.id }.toMutableList()
    if (item.accessory != null) {
        p.pet.ownedAccessories.add(item.accessory)
        p.pet.appearance.accessory = item.accessory
    }
    addTx(p, TxKind.Purchase, item.name, -item.price, dir = item.kind.key, itemId = item.id)

    val changes = mutableListOf(
        walletChange("Монеты", before, p.wallet, "Ты заплатил за «${item.name}». Это ${if (item.kind == Direction.Must) "нужное" else "желаемое"}."),
    )
    var mainStat: PetStat? = null
    var mainGain = -1
    for (e in pv.effects) {
        val r = applyStat(p.pet, e.stat, item.effects[e.stat] ?: 0)
        changes += Change(
            ChangeKind.Stat, STAT_LABEL.getValue(e.stat), delta = r.gain, before = r.before, after = r.after, stat = e.stat,
            why = if (r.gain > 0) item.note else "Этот показатель уже почти полный — лишнее не пригодилось.",
        )
        if (mainStat == null || r.gain > mainGain) {
            mainStat = e.stat
            mainGain = r.gain
        }
    }
    val envAfter = envelope(p, item.kind)
    changes += Change(
        ChangeKind.Plan, "Конверт «${DIRECTION_LABEL.getValue(item.kind)}»",
        delta = envAfter.left - pv.envelope.left, before = pv.envelope.left, after = envAfter.left,
        why = if (envAfter.over > 0) "Ты потратил на ${coinsText(envAfter.over)} больше, чем запланировал." else "В конверте осталось ${coinsText(envAfter.left)} из ${envAfter.planned}.",
    )
    val gainText = if (mainStat != null && mainGain > 0) " ${STAT_LABEL.getValue(mainStat)} +$mainGain." else ""
    p.pet.reason = PetReason("${item.note}$gainText", stat = mainStat, delta = if (mainStat != null) mainGain else null)

    val foodDone = p.period.needs.food > 0
    val careDone = p.period.needs.care > 0
    val next: NextLink
    val hint: String
    if (item.kind == Direction.Must) {
        if (foodDone && careDone) {
            hint = "Нужное на эту неделю обеспечено. Теперь можно отложить в копилку или порадовать питомца."
            next = NextLink("В копилку", "/goals")
        } else {
            hint = "Ещё нужно купить ${if (foodDone) "уход (купание, расчёска)" else "еду (обед, ужин)"}."
            next = NextLink("Ещё в магазин", "/shop")
        }
    } else {
        hint = "Желаемое радует, но не заменяет заботу. Не забудь про копилку."
        next = NextLink("В копилку", "/goals")
    }
    val over = envAfter.over > 0
    return done(
        p,
        Report(
            title = "${item.name}: куплено!",
            tone = Tone.Good,
            changes = changes,
            explain = if (over) "$hint План на «${DIRECTION_LABEL.getValue(item.kind)}» превышен — на следующей неделе заложи побольше." else hint,
            next = next,
        ),
    )
}

fun toggleWishlist(p0: Profile, itemId: String, ctx: Ctx): Res {
    ctx.content.items.find { it.id == itemId } ?: return fail(ErrorCode.ITEM_NOT_FOUND, "Такого товара нет.")
    val p = p0.deepCopy()
    val has = itemId in p.wishlist
    p.wishlist = if (has) p.wishlist.filter { it != itemId }.toMutableList() else (p.wishlist + itemId).toMutableList()
    return done(
        p,
        Report(
            title = if (has) "Убрано из списка желаний" else "Добавлено в список желаний",
            tone = Tone.Info,
            changes = emptyList(),
            explain = if (has) "Ты передумал — и это нормально." else "Подумай до следующей недели. Если всё ещё захочется — запланируй эту покупку.",
        ),
    )
}

fun wearAccessory(p0: Profile, accessory: String): Res {
    if (accessory != "none" && accessory !in p0.pet.ownedAccessories) return fail(ErrorCode.ITEM_NOT_FOUND, "Этого украшения у питомца пока нет.")
    val p = p0.deepCopy()
    p.pet.appearance.accessory = accessory
    return done(p, Report("Готово!", Tone.Info, emptyList(), "Питомец переоделся."))
}

fun wishlistItems(p: Profile, ctx: Ctx): List<Item> = p.wishlist.mapNotNull { id -> ctx.content.items.find { it.id == id } }
