#!/usr/bin/env sh
set -eu

SRC_DIR="src/main/java"
OUT_DIR="out"
MAIN_CLASS="com.echorun.EchoRun"

mkdir -p "$OUT_DIR"

# Compile all sources
# Use a temporary file to store the list of sources
TMP_SOURCES="$(mktemp)"
find "$SRC_DIR" -type f -name "*.java" > "$TMP_SOURCES"

if [ ! -s "$TMP_SOURCES" ]; then
  echo "Nenhum arquivo .java encontrado em $SRC_DIR" 1>&2
  rm -f "$TMP_SOURCES"
  exit 1
fi

javac -encoding UTF-8 -d "$OUT_DIR" @"$TMP_SOURCES"
rm -f "$TMP_SOURCES"

# Run
exec java -cp "$OUT_DIR" "$MAIN_CLASS"
