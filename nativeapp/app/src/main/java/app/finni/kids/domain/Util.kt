package app.finni.kids.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val GameJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    coerceInputValues = true
}

/** Профиль — простые данные, поэтому копируем через JSON: копия никак не связана с оригиналом. */
fun Profile.deepCopy(): Profile = GameJson.decodeFromString(GameJson.encodeToString(this))

fun fail(code: ErrorCode, message: String, missing: Int? = null, overBy: Int? = null, max: Int? = null, options: List<ShortageOption>? = null): Outcome.Err =
    Outcome.Err(GameError(code, message, missing, overBy, max, options))

fun done(profile: Profile, report: Report): Res = Outcome.Ok(profile, report, Unit)

fun <T> done(profile: Profile, report: Report, extra: T): Outcome<T> = Outcome.Ok(profile, report, extra)

fun walletChange(label: String, before: Int, after: Int, why: String): Change =
    Change(ChangeKind.Wallet, label, delta = after - before, before = before, after = after, why = why)
