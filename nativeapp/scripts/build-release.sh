#!/usr/bin/env bash
# Сборка подписанного релиза нативного приложения: тесты → APK → AAB → проверка подписи и разрешений → контрольные суммы.
# Требуется: JDK 17+ (берётся из Android Studio, если JAVA_HOME не задан), Android SDK (ANDROID_HOME).
# Подпись: ~/.finni-release/keystore.properties (ключ лежит вне репозитория; тот же, что у веб-версии).
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -z "${JAVA_HOME:-}" ]; then
  for c in "/Applications/Android Studio.app/Contents/jbr/Contents/Home" "$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home"; do
    [ -x "$c/bin/java" ] && export JAVA_HOME="$c" && break
  done
fi
[ -n "${JAVA_HOME:-}" ] || { echo "Не найден JDK: задайте JAVA_HOME"; exit 1; }
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
[ -d "$ANDROID_HOME" ] || { echo "Не найден Android SDK: задайте ANDROID_HOME"; exit 1; }
[ -f "${FINNI_KEYSTORE_PROPS:-$HOME/.finni-release/keystore.properties}" ] || { echo "Нет ключа подписи. Создайте его: ../finny/scripts/make-keystore.sh"; exit 1; }

VERSION=$(grep -E 'versionName' app/build.gradle.kts | head -1 | sed -E 's/.*"([^"]+)".*/\1/')
echo "== Версия $VERSION: тесты, APK и AAB"
./gradlew --console=plain testDebugUnitTest assembleRelease bundleRelease

APK=app/build/outputs/apk/release/app-release.apk
AAB=app/build/outputs/bundle/release/app-release.aab
OUT=release
mkdir -p "$OUT"
cp "$APK" "$OUT/finni-$VERSION-native-release.apk"
cp "$AAB" "$OUT/finni-$VERSION-native-release.aab"

echo "== Проверка"
APKSIGNER="$(ls -d "$ANDROID_HOME"/build-tools/*/apksigner | sort -V | tail -1)"
AAPT="$(ls -d "$ANDROID_HOME"/build-tools/*/aapt2 | sort -V | tail -1)"
"$APKSIGNER" verify --print-certs "$OUT/finni-$VERSION-native-release.apk" | grep -E "certificate (DN|SHA-256)" | head -2 || true
if "$AAPT" dump permissions "$OUT/finni-$VERSION-native-release.apk" | grep -q "uses-permission"; then
  echo "ОШИБКА: в манифесте появились разрешения"; exit 1
fi
echo "Разрешений в манифесте: нет"

(cd "$OUT" && shasum -a 256 "finni-$VERSION-native-release.apk" "finni-$VERSION-native-release.aab" > SHA256SUMS.txt)
ls -la "$OUT"
cat "$OUT/SHA256SUMS.txt"
