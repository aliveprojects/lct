# Сборка и окружение

## Требования

| Инструмент | Версия | Зачем |
|---|---|---|
| Node.js | 22+ | сборка веб-части, тесты |
| JDK | 21 (подойдёт JBR из Android Studio) | Gradle / Android Gradle Plugin |
| Android SDK | платформа 36, Build-Tools 35+ | сборка APK (Gradle докачает недостающее сам, если приняты лицензии) |
| Gradle | 8.14.3 | скачивается автоматически (Gradle Wrapper) |
| Android Gradle Plugin | 8.13.0 | задан в `android/build.gradle` |
| Capacitor | 8.5.x | мост «веб → Android» |

Проверено на macOS (Apple Silicon). Путь к проекту может содержать кириллицу и пробелы — сборка это выдерживает.

Переменные окружения: `JAVA_HOME` (JDK 21) и `ANDROID_HOME` (папка SDK). Скрипт `scripts/build-release.sh` ищет JDK в Android Studio сам.

## Пошагово: релизный APK

```bash
# 1. Зависимости
npm ci

# 2. Ключ подписи — один раз. Создаётся ВНЕ репозитория: ~/.finni-release/
./scripts/make-keystore.sh
#    Сохраните резервную копию этой папки: без неё нельзя выпустить обновление приложения.

# 3. Проверки и сборка (типы → тесты → веб → sync → Gradle)
npm run release
```

Результат в `release/`:

- `finni-1.0.0-release.apk` — подписанный APK, ставится без среды разработки;
- `finni-1.0.0-release.aab` — Android App Bundle (для RuStore);
- `SHA256SUMS.txt` — контрольные суммы.

Если ключа нет, Gradle соберёт **неподписанный** APK (`app-release-unsigned.apk`); для установки на устройство его нужно подписать или собрать debug: `cd android && ./gradlew assembleDebug`.

Путь к ключу можно задать переменной `FINNI_KEYSTORE_PROPS` (файл со строками `storeFile`, `storePassword`, `keyAlias`, `keyPassword`).

## Разработка

```bash
npm run dev          # веб-версия с горячей перезагрузкой
npm run typecheck    # проверка типов
npm test             # автотесты
npm run docs         # пересоздать docs/generated/* из src/content/*.json
npm run icons        # пересоздать иконки Android и иконку 512×512 из assets/icon-foreground.svg
npm run cap:sync     # собрать веб-часть и скопировать в android/
```

Открыть проект в Android Studio: `npx cap open android`.

## Установка и запуск на устройстве

```bash
adb install -r release/finni-1.0.0-release.apk
adb shell am start -n app.finni.kids/.MainActivity
```

## Версия сборки

Версия хранится в трёх местах, которые нужно менять вместе: `package.json` (`version`), `android/app/build.gradle` (`versionName`, `versionCode`) и `src/app/meta.ts`.

## Требование к WebView

Интерфейс использует возможности Chromium WebView 84+ (2020 г.). На устройствах с Google Play, RuStore или AppGallery «Android System WebView» обновляется автоматически, поэтому это ограничение затрагивает только устройства, где обновления WebView отключены.
