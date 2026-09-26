#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"
if [[ -d "$ROOT/.tools/jdk" ]]; then
  export JAVA_HOME="$ROOT/.tools/jdk"
else
  for candidate in "$ROOT"/.tools/jdk-*; do
    if [[ -x "$candidate/bin/java" ]]; then export JAVA_HOME="$candidate"; break; fi
  done
fi
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
if ! command -v java >/dev/null 2>&1; then
  echo 'Java est absent. Lancez ./scripts/setup.sh.' >&2
  exit 1
fi
if [[ $# -eq 0 ]]; then set -- run; fi
exec ./gradlew "$@" --console=plain
