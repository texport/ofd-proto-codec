#!/usr/bin/env bash
# Выпускать ли коммит и под каким номером.
#
# Линия версии (val versionLine) объявлена в сборке LINE_FILE; номер — следующий
# после последней метки этой линии: v1.3.0, v1.3.1, …
#
# EVENT=push         — поставлена метка REF_NAME руками: выпуск под её номером.
# EVENT=workflow_run — зелёный CI на main для коммита SHA: выпуск, если у коммита
#                      ещё нет метки этой линии; AUTO_RELEASE=false это выключает.
#
# Пишет в GITHUB_OUTPUT release=true|false, version, tag.
set -euo pipefail

note() {
    echo "::notice title=Выпуск::$1"
    echo "- $1" >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
}
out() { echo "$1" >> "$GITHUB_OUTPUT"; }

line=$(sed -n 's/^val versionLine = "\(.*\)"$/\1/p' "$LINE_FILE")
test -n "$line" || { echo "::error::В $LINE_FILE нет val versionLine"; exit 1; }
esc=${line//./\\.}

if [ "$EVENT" = push ]; then
    version=${REF_NAME#v}
    if ! printf '%s' "$version" | grep -Eq "^$esc\.[0-9]+$"; then
        echo "::error::Метка $REF_NAME не из линии $line, объявленной в $LINE_FILE"
        exit 1
    fi
    out release=true; out "version=$version"; out "tag=$REF_NAME"
    exit 0
fi

if [ "${AUTO_RELEASE:-true}" != true ]; then
    note "Автоматический выпуск выключен (переменная репозитория AUTO_RELEASE)"
    out release=false
    exit 0
fi

released=$(git tag --points-at "$SHA" | grep -E "^v$esc\.[0-9]+$" | head -1 || true)
if [ -n "$released" ]; then
    note "Коммит уже выпущен как $released"
    out release=false
    exit 0
fi

last=$(git tag -l "v$line.*" | sed -n "s/^v$esc\.\([0-9][0-9]*\)$/\1/p" | sort -n | tail -1)
version="$line.$(( ${last:--1} + 1 ))"
note "Выпуск v$version"
out release=true; out "version=$version"; out "tag=v$version"
