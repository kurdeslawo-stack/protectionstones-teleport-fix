#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
upstream_dir="${1:-protectionstones-build}"
if (( $# > 0 )); then
    shift
fi

git clone --depth 1 --branch 2.10.6 https://github.com/espidev/ProtectionStones.git "$upstream_dir"
expected_commit=7701764b5cda7db4ef59d1c99252ac7cc19c98f8
if [[ "$(git -C "$upstream_dir" rev-parse HEAD)" != "$expected_commit" ]]; then
    echo "The upstream tag does not match the reviewed commit." >&2
    exit 1
fi

git -C "$upstream_dir" apply --check "$repo_dir/protectionstones-2.10.6-teleport-timer.patch"
git -C "$upstream_dir" apply "$repo_dir/protectionstones-2.10.6-teleport-timer.patch"
notice_dir="$upstream_dir/src/main/resources/META-INF/protectionstones-teleport-fix"
mkdir -p "$notice_dir"
cp "$repo_dir/NOTICE.txt" "$notice_dir/NOTICE.txt"
cp "$upstream_dir/LICENSE" "$notice_dir/GPL-3.0.txt"

mvn -B -ntp -f "$upstream_dir/pom.xml" clean verify "$@"
