#!/usr/bin/env bash
# Заметки о выпуске в notes.md: файл RELEASE_NOTES.md, а без него — сообщения
# коммитов от предыдущей метки MATCH до SHA.
set -euo pipefail
if [ -f RELEASE_NOTES.md ]; then
    cp RELEASE_NOTES.md notes.md
    exit 0
fi
prev=$(git describe --tags --abbrev=0 --match "${MATCH:-v*}" "$SHA^" 2>/dev/null || true)
{
    if [ -n "$prev" ]; then echo "Изменения с $prev:"; else echo "Изменения:"; fi
    echo
    git log --no-merges --format='- %s' "${prev:+$prev..}$SHA"
} > notes.md
