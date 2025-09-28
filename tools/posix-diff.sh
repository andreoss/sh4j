#!/usr/bin/env bash
set -uo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
CP="$ROOT/target/classes"
JAVA=${JAVA:-java}
REFERENCE=${REFERENCE:-bash}
FAILS=0
CASES=0

if [ ! -d "$CP" ]; then
    echo "missing $CP; run: mvn -o compile" >&2
    exit 2
fi
if ! command -v "$REFERENCE" >/dev/null 2>&1; then
    echo "missing reference shell: $REFERENCE" >&2
    exit 2
fi

run_case() {
    local name=$1
    local source=$2
    local stdin=${3:-/dev/null}
    CASES=$((CASES + 1))
    local dir
    dir=$(mktemp -d)
    mkdir "$dir/ref" "$dir/sh4j"
    printf '%s\n' "$source" > "$dir/ref/case.sh"
    cp "$dir/ref/case.sh" "$dir/sh4j/case.sh"
    (
        cd "$dir/ref"
        timeout 20 "$REFERENCE" --posix case.sh < "$stdin" > "$dir/ref.out" 2>"$dir/ref.err"
        printf '%s' "$?" > "$dir/ref.code"
    )
    (
        cd "$dir/sh4j"
        timeout 20 "$JAVA" -cp "$CP" io.github.andreoss.sh4j.Main case.sh < "$stdin" > "$dir/sh4j.out" 2>"$dir/sh4j.err"
        printf '%s' "$?" > "$dir/sh4j.code"
    )
    if diff -u "$dir/ref.out" "$dir/sh4j.out" >/dev/null \
        && diff -u "$dir/ref.code" "$dir/sh4j.code" >/dev/null; then
        echo "PASS $name"
    else
        echo "FAIL $name"
        echo "  reference stdout: $(tr '\n' '|' < "$dir/ref.out")"
        echo "  sh4j stdout:      $(tr '\n' '|' < "$dir/sh4j.out")"
        echo "  reference status: $(cat "$dir/ref.code")"
        echo "  sh4j status:      $(cat "$dir/sh4j.code")"
        FAILS=$((FAILS + 1))
    fi
    rm -rf "$dir"
}

run_case exit 'exit 3; echo after'
run_case subshell-cd 'cd /tmp; (cd /); pwd'
run_case export-isolation "X=secret; sh -c 'echo \${X-unset}'"
run_case export-quotes "export E='x y'; sh -c 'printf \"%s\\n\" \"\$E\"'"
run_case quoted-at 'set -- "a b" c; for x in "$@"; do echo "[$x]"; done'
run_case star-ifs 'set -- a b; IFS=:; echo "$*"'
run_case command-sub-newlines "X=\$(printf 'a\\n\\n\\n'); printf '<%s>\\n' \"\$X\""
run_case single-quoted-backtick "echo '\`date\`'"
run_case case-pattern "p='h*'; case hi in \$p) echo yes;; *) echo no;; esac"
run_case glob-dotfile 'touch .hidden visible; echo *'
run_case set-plus-e 'set -e; set +e; false; echo alive'
run_case readonly 'readonly R="a b"; echo "$R"; unset R; echo "$R"'
run_case compound-pipeline 'if true; then echo hi; fi | cat'
run_case builtin-pipeline-read "printf 'x\\n' | { read v; echo \"v=\$v\"; }"
run_case trap-exit "trap 'echo caught' EXIT; echo body"

echo "cases=$CASES failures=$FAILS"
exit "$FAILS"
