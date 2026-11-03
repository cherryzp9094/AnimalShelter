#!/bin/sh
# 스테이징된 변경(git diff --cached)에 local.properties·relay/.dev.vars의 값이 들어 있으면 실패한다.
# 값은 절대 출력하지 않는다. 일치한 키 이름만 출력한다.
set -u

root=$(git rev-parse --show-toplevel) || exit 1
diff_file=$(mktemp)
trap 'rm -f "$diff_file"' EXIT
git diff --cached --no-color > "$diff_file"

found=0
for src in "$root/local.properties" "$root/relay/.dev.vars"; do
    [ -f "$src" ] || continue
    # KEY=value 줄만 읽는다. 주석·빈 줄은 건너뛴다.
    while IFS= read -r line || [ -n "$line" ]; do
        case "$line" in
            ''|'#'*) continue ;;
            *=*) ;;
            *) continue ;;
        esac
        name=${line%%=*}
        value=${line#*=}
        # 따옴표로 감싼 값(.dev.vars)은 벗긴다.
        value=${value#\"}; value=${value%\"}
        # 비밀값이 아닌 설정(SDK 경로, 중계 주소, 허용 origin)은 건너뛴다.
        case "$name" in sdk.dir|ndk.dir|RELAY_BASE_URL|ALLOWED_ORIGINS) continue ;; esac
        [ ${#value} -ge 8 ] || continue
        if grep -qF -- "$value" "$diff_file"; then
            echo "check_secrets: 스테이징된 변경에 $(basename "$src")의 $name 값이 들어 있다. 커밋을 막는다." >&2
            found=1
        fi
    done < "$src"
done

exit $found
