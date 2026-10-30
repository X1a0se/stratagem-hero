#!/usr/bin/env bash
# Rebuilds dist/StratagemHero-1.2.apk with the plain Android SDK tools.
# Override VERSION_CODE / VERSION_NAME for a different release.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/d/Work of DSH/.toolchains/android-sdk}}"
BT="$SDK/build-tools/34.0.0"
AJAR="$SDK/platforms/android-34/android.jar"
JDK="${JAVA_HOME:-/d/Work of DSH/.toolchains/jdk17/jdk-17.0.21.0.2+1}"
JAVA="$JDK/bin/java"
JAVAC="$JDK/bin/javac"
VERSION_CODE="${VERSION_CODE:-3}"
VERSION_NAME="${VERSION_NAME:-1.2}"
APK_NAME="StratagemHero-$VERSION_NAME.apk"

for f in "$BT/aapt2.exe" "$AJAR" "$JAVA" "$JAVAC" "$ROOT/sign/debug.keystore"; do
    [ -e "$f" ] || { echo "missing: $f" >&2; exit 1; }
done

cd "$ROOT"
rm -rf build/gen build/classes build/dex build/res.zip build/app.unsigned.apk build/app.dex.apk build/app.aligned.apk
mkdir -p build/gen build/classes build/dex dist

"$BT/aapt2.exe" compile --dir app/res -o build/res.zip
"$BT/aapt2.exe" link -o build/app.unsigned.apk -I "$AJAR" \
    --manifest AndroidManifest.xml --java build/gen \
    --min-sdk-version 26 --target-sdk-version 34 \
    --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" build/res.zip
"$JAVAC" -encoding UTF-8 -source 8 -target 8 -bootclasspath "$AJAR" \
    -d build/classes $(find app/src build/gen -name '*.java')
"$JAVA" -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --lib "$AJAR" \
    --min-api 26 --output build/dex $(find build/classes -name '*.class')

python - "$ROOT" <<'PY'
import shutil, sys, zipfile
root = sys.argv[1]
shutil.copyfile(root + '/build/app.unsigned.apk', root + '/build/app.dex.apk')
with zipfile.ZipFile(root + '/build/app.dex.apk', 'a', zipfile.ZIP_DEFLATED) as z:
    z.write(root + '/build/dex/classes.dex', 'classes.dex')
PY

"$BT/zipalign.exe" -f -p 4 build/app.dex.apk build/app.aligned.apk
"$JAVA" -jar "$BT/lib/apksigner.jar" sign \
    --ks sign/debug.keystore --ks-pass pass:android --key-pass pass:android \
    --ks-key-alias androiddebugkey \
    --out "dist/$APK_NAME" build/app.aligned.apk
"$JAVA" -jar "$BT/lib/apksigner.jar" verify --verbose "dist/$APK_NAME"
echo "built: $ROOT/dist/$APK_NAME"
