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
run_case line-continuation 'echo a\
b'
run_case tilde-home 'HOME=/tmp; echo ~'
run_case param-prefix-suffix 'v=abcabc; echo "${v#a*} ${v##a*} ${v%b*} ${v%%b*}"'
run_case param-default-assign 'unset u; echo "${u:=fallback}"; echo "$u"'
run_case param-error-unset 'unset e; echo "${e:?boom}"; echo after'
run_case arith-precedence 'echo $((2+3*4)) $((1<<3)) $((7/2)) $((0?1:2))'
run_case arith-assign 'i=1; i=$((i+1)); echo $i'
run_case field-split-empty 'IFS=:; v="a::b"; for f in $v; do echo "[$f]"; done'
run_case star-unquoted 'set -- "a b" c; for x in $*; do echo "[$x]"; done'
run_case glob-no-match 'cd /tmp; echo no-such-pattern-xyz*'
run_case glob-question 'cd /tmp; touch q1 q2 q3; echo q?'
run_case glob-range 'cd /tmp; touch r1 r2; echo r[12]'
run_case redir-order 'cd /tmp; echo hi 2>&1 >reorder.txt; echo status=$?; cat reorder.txt'
run_case redir-append-missing-dir 'echo x >> /nonexistent-dir-xyz/f; echo status=$?'
run_case heredoc-unquoted-expand $'x=1; cat <<EOF\nvalue=$x\nEOF'
run_case heredoc-quoted-literal $'x=1; cat <<\'EOF\'\nvalue=$x\nEOF'
run_case heredoc-strip-tabs $'cat <<-EOF\n\tindented\n\tEOF'
run_case readwrite-fd 'cd /tmp; f=rw.txt; printf "seed\n" > $f; exec 3<>$f; read v <&3; echo "v=$v"; exec 3<&-'
run_case readonly-fd 'cd /tmp; f=ro.txt; printf "seed\n" > $f; exec 4<$f; read w <&4; echo "w=$w"'
run_case fd-close 'cd /tmp; f=cl.txt; printf "x\n" > $f; exec 5<$f; exec 5<&-; read y <&5; echo "y=[$y]"'
run_case fd-open-missing 'exec 9</nonexistent-dir-xyz/f; echo rc=$?'
run_case exec-out-missing 'exec >/nonexistent-dir-xyz/f; echo after'
run_case fd-close-unbound 'exec 8<&-; echo rc=$?'
run_case fd-unbound-read 'read v <&9; echo "v=[$v] rc=$?"'
run_case pipeline-status 'false | true; echo $?'
run_case pipeline-status-rev 'true | false; echo $?'
run_case background-wait 'sleep 0.1 & wait; echo done'
run_case for-default-positional 'set -- a b; for x; do echo "[$x]"; done'
run_case until-loop 'i=0; until [ $i -ge 2 ]; do i=$((i+1)); echo $i; done'
run_case case-alternation 'case bb in a|b*) echo alt;; *) echo no;; esac'
run_case function-status 'f() { return 7; }; f; echo $?'
run_case command-sub-status "X=\$(false); echo status=\$?; echo \"x=\$X\""
run_case command-sub-trim 'X=$(printf "a\n\n\n"); printf "<%s>\n" "$X"'
run_case nested-quotes "echo \"a'b\"c"
run_case dollar-at-empty 'set --; for x in "$@"; do echo "[$x]"; done; echo "count=$#"'
run_case exit-in-function 'f() { exit 4; }; f; echo after'
run_case dot-script 'cd /tmp; echo "echo sourced-value=ok" > inc.sh; . ./inc.sh'

run_case ifs-default-split 'v="a  b"; for f in $v; do echo "[$f]"; done'
run_case quoted-empty 'set -- ""; echo "count=$#"; echo "[$1]"'
run_case status-after-builtins 'true; echo $?; false; echo $?'
run_case status-command-not-found 'nosuchcmd_xyz 2>/dev/null; echo $?'
run_case trap-reset "trap 'echo t' USR1; trap - USR1; echo ok"
run_case getopts-invalid 'set -- -z; getopts ab o; echo "o=$o"'
run_case readonly-assign 'readonly R=1; R=2; echo "rc=$?"; echo "R=$R"'
run_case nested-functions 'f() { g; }; g() { echo g; }; f'
run_case background-wait-pid 'sleep 0.1 & wait $!; echo done'
run_case umask-print 'umask'
run_case heredoc-tabs-quoted $'cat <<-\'EOF\'\n\tliteral\n\tEOF'
run_case case-no-terminator 'case x in x) echo one;; esac; case y in y) echo two esac'
run_case param-length 'v=abc; echo "${#v}"'
run_case glob-subdir 'cd /tmp; mkdir -p dsh4j; touch dsh4j/a.txt dsh4j/b.log; echo dsh4j/*.txt'
run_case and-or-chain 'false && echo a || echo b'
run_case while-read-loop "printf 'a\\nb\\n' | while read l; do echo \"[\$l]\"; done"
run_case subshell-status '(false); echo $?'
run_case arith-modulo-negative 'echo $((-7 % 3))'
run_case ifs-custom-join 'IFS=,; set -- a b c; echo "$*"'
run_case dollar-hyphen 'set -e; case $- in *e*) echo has-e;; *) echo no-e;; esac; set +e'
run_case set-u-unset 'set -u; echo $UNSET_X; echo after'
run_case set-f-noglob 'set -f; echo *'
run_case shift-beyond 'set -- a; shift 5; echo "rc=$?"'
run_case break-outside-loop 'break; echo after'
run_case colon-args ': a b c; echo $?'
run_case cd-oldpwd 'cd /; cd /tmp; cd -; echo "PWD=$PWD"'
run_case cd-missing 'cd /nonexistent-xyz-abc; echo "rc=$?"'
run_case func-missing-arg 'f() { echo "[$1-$2]"; }; f a'
run_case ifs-space-only 'IFS=" "; v="a  b"; for f in $v; do echo "[$f]"; done'
run_case param-minus-vs-colon 'unset u; echo "[${u-x}][${u:-x}]"; u=; echo "[${u-y}][${u:-y}]"'
run_case case-quoted-pattern 'v="a*"; case aX in "$v") echo lit;; *) echo no;; esac'
run_case arith-unset-var 'echo $((undef+2))'
run_case subshell-var-scope 'x=1; (x=2; echo $x); echo $x'
run_case loop-last-status 'for i in 1; do false; done; echo $?'
run_case until-break 'i=0; until false; do i=$((i+1)); [ $i -ge 2 ] && break; done; echo $i'
run_case glob-question-dir 'cd /tmp; mkdir -p g2; touch g2/x1 g2/x2 g2/y1; echo g2/x?'
run_case pipeline-stderr 'false 2>/dev/null | true; echo $?'
run_case read-backslash 'cd /tmp; printf "x\\\\y\n" > rb.txt; read v < rb.txt; echo "[$v]"'
run_case read-r-flag 'cd /tmp; printf "x\\\\y\n" > rr.txt; read -r v < rr.txt; echo "[$v]"'
run_case trap-exit-status "trap 'echo exit=\$?' EXIT; false"
run_case positional-tenth 'set -- 1 2 3 4 5 6 7 8 9 10 11; echo "$9 ${10}"'
run_case command-builtin 'command echo hi'
run_case unset-multiple 'a=1; b=2; unset a b; echo "[$a][$b]"'
run_case wait-invalid-pid 'wait 999999; echo "rc=$?"'
run_case heredoc-with-substitution $'x=1; cat <<EOF\nsub=$(echo S)\nEOF'
run_case pipe-triple 'echo a | cat | cat'
run_case background-wait-status '(exit 3) & wait $!; echo "rc=$?"'
run_case if-elif-else 'if false; then :; elif true; then echo e; else echo no; fi'
run_case while-false-status 'while false; do :; done; echo $?'
run_case case-multi-pattern 'case b in a|b) echo ab;; *) echo no;; esac'
run_case backslash-escape-in-echo 'echo "a\"b"'
run_case tilde-unset-home 'unset HOME; echo "~"'
run_case fd-output-dup 'cd /tmp; rm -f od1.txt; exec 3>od1.txt; echo y >&3; exec 3>&-; cat od1.txt'
run_case fd-readwrite-write 'cd /tmp; rm -f od2.txt; exec 3<>od2.txt; echo z >&3; exec 3>&-; cat od2.txt'
run_case fd-dup-unbound 'echo x >&7; echo "rc=$?"'
run_case getopts-optarg 'set -- -a val; getopts a: o; echo "o=$o arg=$OPTARG"'
run_case getopts-colon-mode 'set -- -z; getopts :ab o; echo "o=$o arg=$OPTARG"'
run_case trap-exit-status 'trap "echo st=\$?" EXIT; exit 7'
run_case recursive-function 'f() { if [ $1 -gt 0 ]; then f $(($1-1)); else echo done; fi; }; f 3'
run_case umask-symbolic-set 'umask 022; umask -S'
run_case case-star-first 'case x in *) echo any;; esac'
run_case assign-subst-status 'X=$(exit 3); echo "rc=$?"'
run_case star-length 'set -- a b c; echo "${#*}"'
run_case ifs-join-star 'IFS=-; set -- a b; echo "$*"'
run_case stderr-to-stdout-order 'echo err 1>&2 2>&1 | cat; echo done'
run_case sourced-return 'cd /tmp; printf "echo before; return 5; echo after\n" > r.sh; . ./r.sh; echo "rc=$?"'
run_case shift-twice 'set -- a; shift; echo "n=$#"; shift; echo "rc=$?"'
echo "cases=$CASES failures=$FAILS"
exit "$FAILS"
