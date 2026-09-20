package app.finni.kids.domain

import app.finni.kids.content.Content
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.time.LocalDate

const val SCHEMA = 1

fun initialApp(): AppState = AppState()

fun activeProfile(s: AppState): Profile? = if (s.mode == Mode.Demo) s.demo else s.main

fun withProfile(s: AppState, p: Profile?): AppState = if (s.mode == Mode.Demo) s.copy(demo = p) else s.copy(main = p)

fun dateKey(d: LocalDate): String = d.toString() // ГГГГ-ММ-ДД

/** Сегодняшняя дата. В демо-режиме календарь можно «прокрутить» вперёд без ожидания. */
fun todayKey(offsetDays: Int = 0, now: LocalDate = LocalDate.now()): String = dateKey(now.plusDays(offsetDays.toLong()))

fun makeCtx(s: AppState, content: Content, now: LocalDate = LocalDate.now()): Ctx =
    Ctx(content, todayKey(if (s.mode == Mode.Demo) s.demoDayOffset else 0, now))

// ---------- Режимы и сброс ----------

fun setMode(s: AppState, mode: Mode): AppState = s.copy(mode = mode)

fun resetDemoProfile(s: AppState): AppState = s.copy(demo = null, demoDayOffset = 0)

fun advanceDemoDay(s: AppState): AppState = s.copy(demoDayOffset = s.demoDayOffset + 1)

/** «Сбросить прогресс»: игра начинается заново, но имя и облик питомца остаются. */
fun resetProgress(p: Profile, ctx: Ctx): Profile {
    val acc = p.pet.appearance.accessory
    val keepAccessory = if (ctx.content.pet.accessories.any { it.id == acc && it.atStart }) acc else "none"
    return newProfile(
        NewProfileInput(p.playerName, p.pet.name, p.pet.appearance.copy(accessory = keepAccessory), p.isDemo),
        ctx,
    )
}

// ---------- Сохранение и разбор ----------

fun serializeState(s: AppState): String = GameJson.encodeToString(s)

data class ParseResult(
    val state: AppState,
    /** true — данные были повреждены, и мы начали заново (старый текст стоит сохранить в резерв). */
    val recovered: Boolean,
)

/** Безопасный разбор: повреждённые данные не должны ронять приложение. */
fun parseState(raw: String?): ParseResult {
    if (raw.isNullOrEmpty()) return ParseResult(initialApp(), false)
    return try {
        val s = GameJson.decodeFromString<AppState>(raw)
        if (s.schema > SCHEMA) throw IllegalStateException("unknown schema")
        ParseResult(s.copy(schema = SCHEMA), false)
    } catch (e: Exception) {
        ParseResult(initialApp(), true)
    }
}
