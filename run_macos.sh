#!/bin/bash
set -e
PROJECT="$(cd "$(dirname "$0")" && pwd)"
LIB="$PROJECT/lib"
OUT="$PROJECT/out"
SOURCES="$PROJECT/sources.txt"

# ── 確認 Java 已安裝 ──────────────────────────────────────────────────────────
if ! command -v java &>/dev/null; then
    echo "[錯誤] 找不到 Java，請先安裝 JDK 17："
    echo "  https://adoptium.net/"
    exit 1
fi

# ── 偵測 CPU 架構（Intel = x86_64 / Apple Silicon = arm64）────────────────────
ARCH=$(uname -m)
if [ "$ARCH" = "arm64" ]; then
    PLATFORM="mac-aarch64"
else
    PLATFORM="mac"
fi

# ── 收集 module-path：平台無關 + 本機平台 JAR（排除其他平台）─────────────────
JARS=""
for jar in "$LIB"/*.jar; do
    filename=$(basename "$jar")
    [[ "$filename" == *"-win.jar"         ]] && continue
    [[ "$filename" == *"-mac.jar"         && "$PLATFORM" != "mac"         ]] && continue
    [[ "$filename" == *"-mac-aarch64.jar" && "$PLATFORM" != "mac-aarch64" ]] && continue
    JARS="${JARS:+$JARS:}$jar"
done

if [ -z "$JARS" ]; then
    echo "[錯誤] lib/ 資料夾中找不到 JavaFX JAR！"
    exit 1
fi

# ── 編譯 ──────────────────────────────────────────────────────────────────────
mkdir -p "$OUT"
find "$PROJECT/src" -name "*.java" > "$SOURCES"

echo "🔨 編譯中（$PLATFORM）..."
javac -encoding UTF-8 \
      --module-path "$JARS" \
      --add-modules javafx.controls,javafx.media \
      -d "$OUT" \
      @"$SOURCES"
rm -f "$SOURCES"

# ── 啟動遊戲 ──────────────────────────────────────────────────────────────────
echo "🚀 啟動遊戲..."
java --module-path "$JARS" \
     --add-modules javafx.controls,javafx.media \
     -cp "$OUT" \
     catcatch.CatCatchApp
