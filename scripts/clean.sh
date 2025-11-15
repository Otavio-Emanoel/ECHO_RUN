#!/usr/bin/env sh
set -eu

OUT_DIR="out"

if [ -d "$OUT_DIR" ]; then
  rm -rf "$OUT_DIR"
  echo "Removido: $OUT_DIR"
else
  echo "Nada para limpar."
fi
