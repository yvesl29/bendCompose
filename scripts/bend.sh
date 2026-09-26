#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
export BEND_NO_TELEMETRY=1
if [[ -x "$ROOT/.tools/bend/bin/bend" ]]; then
  exec "$ROOT/.tools/bend/bin/bend" "$@"
elif command -v bend >/dev/null 2>&1; then
  exec bend "$@"
else
  echo 'Bend est absent. Lancez ./scripts/setup.sh.' >&2
  exit 1
fi
