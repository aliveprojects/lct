package app.finni.kids.domain

fun canClaimDaily(p: Profile, ctx: Ctx): Boolean = p.dailyGiftDate != ctx.today

/** «Подарок дня»: без серий и штрафов за пропуск — просто маленький приятный доход. */
fun claimDaily(p0: Profile, ctx: Ctx): Res {
    if (!canClaimDaily(p0, ctx)) return fail(ErrorCode.ALREADY_TODAY, "Подарок дня ты уже забрал. Завтра будет новый!")
    val p = p0.deepCopy()
    val before = p.wallet
    p.wallet += Econ.dailyGift
    p.period.earned += Econ.dailyGift
    p.dailyGiftDate = ctx.today
    addTx(p, TxKind.Daily, "Подарок дня", Econ.dailyGift)
    p.pet.reason = PetReason("Ура, подарок! Спасибо, что заглянул ко мне.")
    return done(
        p,
        Report(
            title = "Подарок дня!",
            tone = Tone.Good,
            changes = listOf(walletChange("Монеты", before, p.wallet, "Каждый день можно забрать небольшой подарок. Это игровые монеты, они не настоящие.")),
            explain = "Доход — это монеты, которые к тебе приходят. Планируй их вместе с остальными.",
        ),
    )
}

fun adultBonusLeft(p: Profile): Int = maxOf(0, Econ.adultBonusCap - p.period.adultBonus)

val ADULT_BONUS_REASONS = listOf("Помощь по дому", "Хорошая идея", "Аккуратность", "Добрый поступок")

/**
 * Поощрение от взрослого: до 20 монет за неделю, шаг 5. Это только игровая валюта —
 * без реальной стоимости и без обмена на деньги или призы.
 */
fun grantAdultBonus(p0: Profile, amount: Int, reason: String): Res {
    if (amount <= 0 || amount % Econ.adultBonusStep != 0) return fail(ErrorCode.INVALID_AMOUNT, "Бонус — кратен ${Econ.adultBonusStep} монетам.")
    if (amount > adultBonusLeft(p0)) return fail(ErrorCode.BONUS_LIMIT, "На этой неделе можно добавить ещё не больше ${coinsText(adultBonusLeft(p0))}.", max = adultBonusLeft(p0))
    val p = p0.deepCopy()
    val before = p.wallet
    p.wallet += amount
    p.period.earned += amount
    p.period.adultBonus += amount
    addTx(p, TxKind.Bonus, "Бонус от взрослого: $reason", amount)
    p.pet.reason = PetReason("Тебя похвалили — я так рад за тебя!")
    return done(
        p,
        Report(
            title = "Бонус от взрослого",
            tone = Tone.Good,
            changes = listOf(walletChange("Монеты", before, p.wallet, "Причина: ${reason.lowercase()}.")),
            explain = "Игровые монеты нельзя обменять на деньги или призы — это просто похвала внутри игры.",
        ),
    )
}
