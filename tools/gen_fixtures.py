#!/usr/bin/env python3
"""docs/api/samples의 실제 응답 JSON을 Kotlin 문자열 상수로 만든다.

실행(저장소 루트): python3 -I tools/gen_fixtures.py
출력: shared/src/commonMain/kotlin/com/cherryzp/animalshelter/data/api/fixture/ApiFixtures.kt
입력: docs/api/samples/live-*/*.json(정렬) + docs/api/samples/gateway_error_30.json

Fixture 엔드포인트(키 없는 빌드·디버그 강제 픽스처)와 테스트가 같이 쓰므로 commonMain에 둔다.
리소스 파일 읽기는 wasmJs에서 불안정하므로 문자열 상수로 넣는다(02-data 11절).
상수 이름이 겹치면 실패한다.
Kotlin raw string(\"\"\"...\"\"\")에 넣으므로 `$`와 `\"\"\"`가 있으면 실패한다.
"""

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SAMPLES = ROOT / "docs" / "api" / "samples"
SOURCES = sorted(SAMPLES.glob("live-*/*.json")) + [SAMPLES / "gateway_error_30.json"]
OUT = ROOT / "shared/src/commonMain/kotlin/com/cherryzp/animalshelter/data/api/fixture/ApiFixtures.kt"


def kotlin_name(path: Path) -> str:
    """sido_v2 → sidoV2, abandonmentPublic_v2_empty → abandonmentPublicV2Empty"""
    parts = re.split(r"[_\-]", path.stem)
    head, tail = parts[0], parts[1:]
    return head[0].lower() + head[1:] + "".join(p[:1].upper() + p[1:] for p in tail)


def main() -> int:
    entries = []
    for src in SOURCES:
        text = src.read_text(encoding="utf-8")
        json.loads(text)  # 깨진 JSON이면 여기서 멈춘다
        if "$" in text or '"""' in text:
            print(f"gen_fixtures: {src.relative_to(ROOT)}에 $ 또는 \"\"\"가 있어 raw string에 넣을 수 없다", file=sys.stderr)
            return 1
        name = kotlin_name(src)
        if any(name == e[0] for e in entries):
            print(f"gen_fixtures: 상수 이름 {name}이(가) 겹친다({src.relative_to(ROOT)})", file=sys.stderr)
            return 1
        entries.append((name, src.relative_to(ROOT).as_posix(), text.rstrip("\n")))

    lines = [
        "// 생성 파일: python3 -I tools/gen_fixtures.py (직접 고치지 않는다)",
        "// 원본: docs/api/samples (실제 응답, 인증키 없음). Fixture 엔드포인트와 테스트가 같이 쓴다.",
        "@file:Suppress(\"ktlint\")",
        "",
        "package com.cherryzp.animalshelter.data.api.fixture",
        "",
        "internal object ApiFixtures {",
    ]
    for name, rel, text in entries:
        lines.append(f"    /** {rel} */")
        lines.append(f'    val {name}: String = """')
        lines.append(text)
        lines.append('"""')
        lines.append("")
    lines[-1] = "}"
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"gen_fixtures: {len(entries)}개 → {OUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
