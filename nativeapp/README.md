# Финни — нативное приложение для Android

Игра-питомец, которая учит детей 7–11 лет обращаться с монетами: план недели (нужное / хочется / копилка), магазин с объяснением последствий, накопления на мечту, задания, рост питомца, раздел для взрослых. Работает без интернета, без аккаунта, без разрешений Android.

Это **полностью нативная** версия проекта на **Kotlin + Jetpack Compose**. Веб-версия (Preact + Capacitor) лежит рядом, в `../finny`, и работает по адресу https://aliveapps.ru/finni.

| | |
|---|---|
| Пакет | `app.finni.kids` (тот же, что у веб-версии, — обновляет её поверх, подпись та же) |
| Версия | 2.0.0 (код 2) |
| Экраны | Jetpack Compose (Material 3 только как основа, весь вид — свой: тема «Космическое приключение») |
| Логика | чистые функции на Kotlin, порт движка веб-версии; 73 автотеста, включая 150 случайных прогонов |
| Контент | JSON в `app/src/main/assets/content` (то же содержимое, что в веб-версии) |
| Хранение | SharedPreferences внутри приложения (JSON, `kotlinx.serialization`) |
| Питомец и иконки | векторные SVG (собираются кодом / лежат в `assets/icons`), рисует AndroidSVG |
| Размер | APK ≈ 6,5 МБ, AAB ≈ 8,5 МБ |
| minSdk / targetSdk | 26 / 36 (Android 8.0 — Android 16) |

## Как собрать и проверить

Нужны JDK 17+ (подойдёт JBR из Android Studio) и Android SDK.

```bash
# тесты движка и контента (JUnit, без эмулятора)
./gradlew testDebugUnitTest

# отладочная сборка
./gradlew assembleDebug

# подписанный релиз (тесты → APK → AAB → проверка подписи и разрешений → контрольные суммы)
./scripts/build-release.sh          # результат в release/

# проверка на эмуляторе или устройстве: устанавливает APK и проходит игровой цикл (18 проверок, кадры в store/screenshots)
node scripts/device-smoke.mjs
```

Ключ подписи лежит вне репозитория: `~/.finni-release/keystore.properties` (создаётся скриптом `../finny/scripts/make-keystore.sh`, у веб-версии тот же).

## Структура

```
app/src/main/java/app/finni/kids/
  domain/     игровой движок: типы, экономика, план, покупки, копилка, задания, периоды, состояние (без Android)
  content/    модели контента, загрузка из JSON, проверка целостности
  platform/   хранилище, звуки-сигналы, фоновая музыка
  app/        Game (единственный источник состояния), Nav (навигация), Ui (окна поверх экрана)
  ui/         тема, общие компоненты, окна, питомец, экраны
app/src/main/assets/   content/*.json, icons/*.svg, music/*.m4a
app/src/test/          тесты движка (порт веб-тестов)
scripts/               build-release.sh, device-smoke.mjs
docs/                  ARCHITECTURE.md, WEB-VS-NATIVE.md, TESTING.md, LICENSES.md
```

Подробнее: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/WEB-VS-NATIVE.md](docs/WEB-VS-NATIVE.md), [docs/TESTING.md](docs/TESTING.md).
