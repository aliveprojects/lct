#!/usr/bin/env bash
# Воспроизводимая релизная сборка: проверки → веб-сборка → подписанные APK и AAB в папке release/.
# Требуется: Node 22+, JDK 21, Android SDK (переменная ANDROID_HOME). Подпись: ~/.finni-release (см. make-keystore.sh).
set -euo pipefail
cd "$(dirname "$0")/.."

# JDK 21: из JAVA_HOME или из Android Studio
if [ -z "${JAVA_HOME:-}" ]; then
  for c in "/Applications/Android Studio.app/Contents/jbr/Contents/Home" "/usr/lib/jvm/java-21-openjdk-amd64" "/opt/homebrew/opt/openjdk@21"; do
    [ -x "$c/bin/java" ] && export JAVA_HOME="$c" && break
  done
fi
[ -n "${JAVA_HOME:-}" ] || { echo "Не найден JDK 21: задайте JAVA_HOME"; exit 1; }
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
[ -d "$ANDROID_HOME" ] || { echo "Не найден Android SDK: задайте ANDROID_HOME"; exit 1; }
export PATH="$JAVA_HOME/bin:$PATH"

VERSION="$(node -p "require('./package.json').version")"

npm ci
npm run typecheck
npm test
npm run build
npx cap sync android

( cd android && ./gradlew clean assembleRelease bundleRelease )

mkdir -p release
cp android/app/build/outputs/apk/release/app-release.apk "release/finni-${VERSION}-release.apk"
cp android/app/build/outputs/bundle/release/app-release.aab "release/finni-${VERSION}-release.aab"

# Проверка подписи (build-tools ищем автоматически)
APKSIGNER="$(ls -d "$ANDROID_HOME"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1 || true)"
if [ -n "$APKSIGNER" ]; then "$APKSIGNER" verify --print-certs "release/finni-${VERSION}-release.apk" | head -3; fi
( cd release && shasum -a 256 finni-"${VERSION}"-release.* > SHA256SUMS.txt && cat SHA256SUMS.txt )
echo "Готово: release/finni-${VERSION}-release.apk и .aab"
