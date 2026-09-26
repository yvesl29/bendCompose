#!/usr/bin/env bash
# Project-local tools only. No sudo, shell profile edits or system packages.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
if [[ "$(uname -s)-$(uname -m)" != "Linux-x86_64" ]]; then
  echo 'Ce bootstrap cible Linux x86_64. Sur macOS/Linux ARM, installez JDK 21 et Bend 2.0.29, puis ./run.sh.' >&2
  exit 1
fi
for tool in curl tar sha256sum clang; do
  command -v "$tool" >/dev/null || { echo "Prérequis manquant : $tool" >&2; exit 1; }
done
mkdir -p .tools
TMP="$(mktemp -d "$ROOT/.tools/setup.XXXXXX")"
trap 'rm -rf "$TMP"' EXIT

fetch() {
  local url="$1" destination="$2" checksum="$3"
  curl --proto '=https' --tlsv1.2 -fL --retry 3 "$url" -o "$destination"
  echo "$checksum  $destination" | sha256sum --check --status
}

if [[ ! -x .tools/jdk/bin/java && ! -x .tools/jdk-21.0.12.1+1/bin/java ]]; then
  fetch 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz' \
    "$TMP/jdk.tar.gz" ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94
  mkdir "$TMP/jdk"
  tar -xzf "$TMP/jdk.tar.gz" --strip-components=1 -C "$TMP/jdk"
  mv "$TMP/jdk" .tools/jdk
fi

if [[ ! -x .tools/bend/bin/bend ]]; then
  fetch 'https://github.com/bendlang/bend/releases/download/v2.0.29/bend-2.0.29-linux-x64.tar.gz' \
    "$TMP/bend.tar.gz" e0ff4fa44581b42f6024d2a1128e7e518219a502cb726030c714a5c23d61726e
  tar -xzf "$TMP/bend.tar.gz" -C "$TMP"
  mv "$TMP/bend" .tools/bend
fi

bash scripts/bend.sh version
./run.sh buildBend jvmJar
echo 'Installation terminée. Lancez ./run.sh ; tests : ./run.sh jvmTest'
