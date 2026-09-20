#!/usr/bin/env bash
# Создаёт ключ подписи релиза ВНЕ репозитория: ~/.finni-release/
# Ключ и пароли нельзя коммитить. Сохраните резервную копию папки — без неё нельзя выпустить обновление в RuStore.
set -euo pipefail

DIR="${FINNI_RELEASE_DIR:-$HOME/.finni-release}"
if [ -f "$DIR/keystore.properties" ]; then
  echo "Ключ уже существует: $DIR (ничего не меняю)"; exit 0
fi

# keytool берём из JAVA_HOME, из Android Studio или из PATH
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/keytool" ]; then KEYTOOL="$JAVA_HOME/bin/keytool"
elif [ -x "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" ]; then KEYTOOL="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"
else KEYTOOL="$(command -v keytool)"; fi

mkdir -p "$DIR"; chmod 700 "$DIR"
PASS="$(openssl rand -hex 16)"
"$KEYTOOL" -genkeypair -v -keystore "$DIR/finni-release.jks" -alias finni \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$PASS" -keypass "$PASS" \
  -dname "CN=Finni, OU=Prototype, O=Finni Team, C=RU" >/dev/null 2>&1

cat > "$DIR/keystore.properties" <<PROPS
storeFile=$DIR/finni-release.jks
storePassword=$PASS
keyAlias=finni
keyPassword=$PASS
PROPS
chmod 600 "$DIR/keystore.properties" "$DIR/finni-release.jks"
echo "Готово. Ключ: $DIR/finni-release.jks"
echo "Сделайте резервную копию папки $DIR и не публикуйте её."
