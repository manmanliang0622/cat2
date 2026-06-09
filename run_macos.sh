#!/bin/bash
set -e

PROJECT="$(cd "$(dirname "$0")" && pwd)"
JAVAFX="$HOME/Documents/javafx-sdk-21.0.11/lib"
OUT="$PROJECT/out"

mkdir -p "$OUT"

echo "🔨 編譯中..."
javac --module-path "$JAVAFX" \
      --add-modules javafx.controls,javafx.media \
      -d "$OUT" \
      "$PROJECT/src/catcatch/"*.java

echo "🚀 啟動遊戲..."
java --module-path "$JAVAFX" \
     --add-modules javafx.controls,javafx.media \
     -cp "$OUT" \
     catcatch.CatCatchApp
