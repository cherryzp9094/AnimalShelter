#!/usr/bin/env python3
"""웹 사진 측정(5c — docs/design/briefs/phase5c-web-images.md I1). 배포 주소의 카드 30장 바이트·첫 사진 시각을 잰다.

실행(저장소 루트, Playwright가 사용자 site-packages에 있어 `-I` 없이):
  /usr/bin/python3 tools/measure_web_images.py --base https://animalshelter.mulddae.workers.dev \\
    --list local-runs/2026-11-04-phase5a-live/raw/A/abandonment_none_30.json \\
    --label before --out local-runs/<날짜>-phase5c/measure
옵션: --profiles desktop,mobile,mobile4g(기본) · --runs 3(mobile4g는 늘 1회)

- `/api/**`는 Playwright가 표본으로 응답한다(공공데이터 실호출 0): 목록은 --list 본문(totalCount 30 — 다음 쪽 없음),
  그 밖 오퍼레이션은 docs/api/samples/live-2026-11-03, OPTIONS는 204. `/a/**`는 끊는다. 정적 자산·`/img`만 네트워크로 간다.
- 회차마다 새 컨텍스트(브라우저 캐시가 빈다), 서비스 워커 막음, ko-KR.
- 첫 `/img/` 요청을 T0로 0.5·1·2·4초에 캡처 → 첫 화면 사진이 다 오거나 30초 → 휠로 끝까지(카드 30장 본 사진이 다 오거나
  60초) → 3초 쉬고 Resource Timing을 모은다. 시각은 페이지 시계의 첫 `/img/` startTime 기준(ms).
- 카드 판별: --list 30건의 첫 비지 않은 popfile → `/img/<rest>`. 본 사진 = 그 경로(쿼리 무시), 자리표시 = `<이름>_s.jpg`.
- 출력: 회차마다 JSON 하나 + summary.md(폴더의 JSON 전부로 다시 쓴다). 키·careOwnerNm·응답 본문은 쓰지 않는다 —
  주소는 `/img/` 경로만, 헤더는 X-Relay-*·cf-ray 끝(데이터센터)·상태만.
"""

import argparse
import copy
import glob
import json
import os
import re
import sys
import time
import urllib.parse

from playwright.sync_api import sync_playwright

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SAMPLES = os.path.join(ROOT, "docs", "api", "samples", "live-2026-11-03")
FILES_MARK = "/fileDownloadSrvc/files/"

PROFILES = {
    "desktop": {"viewport": {"width": 1280, "height": 800}, "device_scale_factor": 1},
    "mobile": {
        "viewport": {"width": 390, "height": 844},
        "device_scale_factor": 3,
        "is_mobile": True,
        "has_touch": True,
    },
}
PROFILES["mobile4g"] = PROFILES["mobile"]
# mobile4g: 지연 60ms, 내려받기 9Mbps, 올리기 4Mbps(바이트/초)
THROTTLE = {"offline": False, "latency": 60, "downloadThroughput": 9_000_000 / 8, "uploadThroughput": 4_000_000 / 8}
CAPTURES = [0.5, 1, 2, 4]
FIRST_SCREEN_LIMIT_S = 30
SCROLL_LIMIT_S = 60
QUIET_S = 1.0
SETTLE_S = 3


def load_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def items_of(body):
    item = body["response"]["body"]["items"]
    item = item.get("item", []) if isinstance(item, dict) else []
    return item if isinstance(item, list) else [item]


def card_paths(list_body):
    """목록 30건 → 카드마다 (본 사진 경로, 자리표시 경로) — 퍼센트 디코딩한 `/img/…` 경로. 사진이 없으면 (None, None)."""
    cards = []
    for it in items_of(list_body)[:30]:
        photos = [it.get(f"popfile{n}") for n in range(1, 9)]
        photo = next((p.strip() for p in photos if isinstance(p, str) and p.strip()), None)
        at = photo.find(FILES_MARK) if photo else -1
        if at < 0:
            cards.append((None, None))
            continue
        main = "/img/" + photo[at + len(FILES_MARK):]
        thumb = re.sub(r"\.(jpe?g|png)$", "_s.jpg", main, flags=re.I)
        cards.append((main, thumb))
    return cards


class ApiSamples:
    """`/api/**` 표본 응답기(실호출 0)."""

    def __init__(self, list_body):
        body = copy.deepcopy(list_body)
        body["response"]["body"]["totalCount"] = 30
        body["response"]["body"]["items"]["item"] = items_of(body)[:30]
        self.list_text = json.dumps(body, ensure_ascii=False)
        self.empty_text = json.dumps(load_json(os.path.join(SAMPLES, "abandonmentPublic_v2_empty.json")), ensure_ascii=False)
        self.by_op = {}
        for op, name in [("sido_v2", "sido_v2.json"), ("sigungu_v2", "sigungu_v2_seoul.json"),
                         ("kind_v2", "kind_v2_dog.json"), ("shelterInfo_v2", "shelterInfo_v2.json")]:
            self.by_op[op] = json.dumps(load_json(os.path.join(SAMPLES, name)), ensure_ascii=False)
        self.ops = []

    def handle(self, route):
        req = route.request
        if req.method == "OPTIONS":
            return route.fulfill(status=204)
        u = urllib.parse.urlparse(req.url)
        op = u.path.rstrip("/").split("/")[-1]
        self.ops.append(op)
        query = dict(urllib.parse.parse_qsl(u.query))
        if op == "abandonmentPublic_v2":
            text = self.empty_text if query.get("desertion_no") else self.list_text
        else:
            text = self.by_op.get(op, self.empty_text)
        return route.fulfill(status=200, content_type="application/json; charset=utf-8",
                             headers={"Cache-Control": "no-store", "X-Relay-Cache": "PLAYWRIGHT"}, body=text)


def classify(url, cards):
    """`/img/` 주소 → ("main"|"placeholder"|"other", 카드 번호 또는 None)."""
    path = urllib.parse.unquote(urllib.parse.urlparse(url).path)
    for index, (main, thumb) in enumerate(cards):
        if main is not None and path == urllib.parse.unquote(main):
            return "main", index
        if thumb is not None and path == urllib.parse.unquote(thumb):
            return "placeholder", index
    return "other", None


def img_path_only(url):
    u = urllib.parse.urlparse(url)
    return u.path + (f"?{u.query}" if u.query else "")


class ImgLog:
    """`/img/` 요청·응답(경로·상태·진단 헤더만)."""

    def __init__(self, cards):
        self.cards = cards
        self.t0 = None
        self.requests = []  # (모노토닉 초, 경로, 종류, 카드)
        self.done = set()  # 끝난 요청의 id(성공·실패)
        self.failed = 0  # requestfailed(끊김·취소 포함)
        self.pending = {}
        self.last_start = None
        self.responses = []

    def on_request(self, req):
        if "/img/" not in urllib.parse.urlparse(req.url).path:
            return
        now = time.monotonic()
        if self.t0 is None:
            self.t0 = now
        kind, index = classify(req.url, self.cards)
        self.requests.append((now, img_path_only(req.url), kind, index))
        self.pending[id(req)] = (kind, index)
        self.last_start = now

    def on_finished(self, req):
        if id(req) in self.pending:
            self.done.add(id(req))

    def on_failed(self, req):
        if id(req) in self.pending:
            self.done.add(id(req))
            self.failed += 1

    def on_response(self, resp):
        if "/img/" not in urllib.parse.urlparse(resp.url).path:
            return
        h = resp.headers
        kind, index = classify(resp.url, self.cards)
        self.responses.append({
            "path": img_path_only(resp.url),
            "kind": kind,
            "card": index,
            "status": resp.status,
            "content_type": h.get("content-type"),
            "x_relay_cache": h.get("x-relay-cache"),
            "x_relay_image": h.get("x-relay-image"),
            "x_relay_upstream_cache": h.get("x-relay-upstream-cache"),
            "colo": (h.get("cf-ray") or "").split("-")[-1] or None,
        })

    def all_finished(self):
        return all(k in self.done for k in self.pending)

    def mains_finished(self):
        """카드 30장(사진이 있는 것)의 본 사진이 모두 요청되고 끝났는지."""
        wanted = {i for i, (m, _) in enumerate(self.cards) if m is not None}
        finished = set()
        for key, (kind, index) in self.pending.items():
            if kind == "main" and key in self.done:
                finished.add(index)
        return wanted <= finished


RT_SCRIPT = """() => performance.getEntriesByType('resource')
  .filter(e => new URL(e.name).pathname.startsWith('/img/'))
  .map(e => ({name: e.name, startTime: e.startTime, responseEnd: e.responseEnd,
              encodedBodySize: e.encodedBodySize, transferSize: e.transferSize,
              responseStatus: e.responseStatus === undefined ? null : e.responseStatus}))"""


def counts(values):
    out = {}
    for v in values:
        key = v if v is not None else "-"
        out[key] = out.get(key, 0) + 1
    return dict(sorted(out.items()))


def run_once(browser, base, profile, list_body, out_dir, label, run_no, extra_routes=None):
    """한 회차. extra_routes(ctx)는 측정 도구의 route보다 먼저 붙는다(나중에 붙인 route가 먼저 돈다 — 시험용)."""
    cards = card_paths(list_body)
    ctx = browser.new_context(locale="ko-KR", service_workers="block", **PROFILES[profile])
    if extra_routes:
        extra_routes(ctx)
    api = ApiSamples(list_body)
    host = re.escape(base.rstrip("/"))
    ctx.route(re.compile("^" + host + r"/api/.*"), api.handle)
    ctx.route(re.compile("^" + host + r"/a/.*"), lambda route: route.abort())
    ctx.add_init_script("performance.setResourceTimingBufferSize(2000)")
    page = ctx.new_page()
    log = ImgLog(cards)
    page.on("request", log.on_request)
    page.on("requestfinished", log.on_finished)
    page.on("requestfailed", log.on_failed)
    page.on("response", log.on_response)
    if profile == "mobile4g":
        cdp = ctx.new_cdp_session(page)
        cdp.send("Network.enable")
        cdp.send("Network.emulateNetworkConditions", THROTTLE)

    stem = f"{label}-{profile}-{run_no}"
    started = time.monotonic()
    page.goto(base.rstrip("/") + "/", wait_until="commit")
    captures = []
    pending_captures = list(CAPTURES)
    timed_out = []
    # 1) 첫 화면: 캡처 넷 + 첫 화면 사진이 다 올 때까지(조용히 1초) 또는 30초
    while True:
        page.wait_for_timeout(50)
        now = time.monotonic()
        if log.t0 is None:
            if now - started > 90:
                timed_out.append("no-img")
                break
            continue
        since = now - log.t0
        while pending_captures and since >= pending_captures[0]:
            at = pending_captures.pop(0)
            name = f"{stem}-t{at:g}.png"
            actual = time.monotonic() - log.t0
            page.screenshot(path=os.path.join(out_dir, name))
            captures.append({"planned_s": at, "actual_s": round(actual, 3), "file": name})
            since = time.monotonic() - log.t0
        quiet = log.last_start is not None and now - log.last_start >= QUIET_S
        if not pending_captures and log.all_finished() and quiet:
            break
        if since > FIRST_SCREEN_LIMIT_S:
            timed_out.append("first-screen")
            break
    wheel_start = page.evaluate("() => performance.now()")
    first_screen_requests = len(log.requests)

    # 2) 휠로 끝까지 — 카드 30장 본 사진이 다 오거나 60초. 한 번에 화면 높이의 60%만 내리고, 받는 중인 사진이 끝난 뒤
    #    다음으로 간다 — 빨리 내리면 화면을 벗어난 카드의 요청을 Coil이 취소한다(M1 첫 실행에서 봤다).
    vw = PROFILES[profile]["viewport"]["width"]
    vh = PROFILES[profile]["viewport"]["height"]
    # 두 칸 데스크톱은 목록 칸이 왼쪽이다
    page.mouse.move(min(vw / 2, 200), vh / 2)
    scroll_started = time.monotonic()
    while not log.mains_finished():
        if time.monotonic() - scroll_started > SCROLL_LIMIT_S:
            timed_out.append("scroll")
            break
        page.mouse.wheel(0, vh * 0.6)
        page.wait_for_timeout(300)
        while not log.all_finished() and time.monotonic() - scroll_started <= SCROLL_LIMIT_S:
            page.wait_for_timeout(50)
    page.wait_for_timeout(SETTLE_S * 1000)

    # 3) 모으기
    entries = page.evaluate(RT_SCRIPT)
    ctx.close()

    t0_page = min((e["startTime"] for e in entries), default=None)
    rows = []
    for e in entries:
        kind, index = classify(e["name"], cards)
        rows.append({
            "path": img_path_only(e["name"]),
            "kind": kind,
            "card": index,
            "start_ms": round(e["startTime"] - t0_page, 1),
            "end_ms": round(e["responseEnd"] - t0_page, 1),
            "encoded": e["encodedBodySize"],
            "transfer": e["transferSize"],
            "status": e["responseStatus"],
            "first_screen": e["startTime"] < wheel_start,
        })

    def end_of(kind, card):
        ends = [r["end_ms"] for r in rows if r["kind"] == kind and r["card"] == card]
        return min(ends) if ends else None

    first_card = next((i for i, (m, _) in enumerate(cards) if m is not None), None)
    first_screen_mains = [r for r in rows if r["kind"] == "main" and r["first_screen"]]
    main_rows = [r for r in rows if r["kind"] == "main"]
    ph_rows = [r for r in rows if r["kind"] == "placeholder"]
    metrics = {
        "first_placeholder_end_ms": end_of("placeholder", first_card),
        "first_main_end_ms": end_of("main", first_card),
        "first_screen_main_last_end_ms": max((r["end_ms"] for r in first_screen_mains), default=None),
        "first_screen_main_count": len(first_screen_mains),
        "cards_with_main": len({r["card"] for r in main_rows}),
        "bytes_main": sum(r["encoded"] for r in main_rows),
        "bytes_placeholder": sum(r["encoded"] for r in ph_rows),
        "bytes_other": sum(r["encoded"] for r in rows if r["kind"] == "other"),
        "img_requests": len(log.requests),
        "img_failed": log.failed,
        "img_requests_first_screen": first_screen_requests,
        "non_200": sum(1 for r in log.responses if r["status"] != 200),
        "status": counts(r["status"] for r in log.responses),
        "x_relay_cache": counts(r["x_relay_cache"] for r in log.responses),
        "x_relay_image": counts(r["x_relay_image"] for r in log.responses),
        "x_relay_upstream_cache": counts(r["x_relay_upstream_cache"] for r in log.responses),
        "colo": counts(r["colo"] for r in log.responses),
        "status_503": sum(1 for r in log.responses if r["status"] == 503),
    }
    result = {
        "label": label,
        "profile": profile,
        "run": run_no,
        "base": base,
        "time": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
        "api_ops": api.ops,
        "timed_out": timed_out,
        "captures": captures,
        "metrics": metrics,
        "resources": rows,
        "responses": log.responses,
    }
    with open(os.path.join(out_dir, stem + ".json"), "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=1)
    return result


def fmt(value):
    return "-" if value is None else (f"{value:,.0f}" if isinstance(value, (int, float)) else str(value))


def kv(d):
    return " ".join(f"{k}:{v}" for k, v in d.items()) if d else "-"


def write_summary(out_dir):
    results = []
    for path in sorted(glob.glob(os.path.join(out_dir, "*.json"))):
        try:
            data = load_json(path)
        except (OSError, ValueError):
            continue
        if "metrics" in data:
            results.append(data)
    order = {"desktop": 0, "mobile": 1, "mobile4g": 2}
    results.sort(key=lambda r: (r["label"], order.get(r["profile"], 9), r["run"]))
    lines = [
        "# 웹 사진 측정 (tools/measure_web_images.py)",
        "",
        "- 시각: 페이지 시계의 첫 `/img/` startTime 기준(ms). 바이트: Resource Timing `encodedBodySize` 합.",
        "- 자리표시 = `_s.jpg`, 본 사진 = 카드 경로(쿼리 무시). 첫 화면 = 휠 전에 요청된 것.",
        "- 다 받은 카드 = 본 사진이 200·바이트 > 0인 카드 수(30이 아니면 스크롤 중 취소된 요청이 있다 — 바이트가 덜 잡혔다).",
        "",
        "| 라벨 | 프로필 | 회차 | 첫 자리표시 끝 | 첫 본 사진 끝 | 첫 화면 본 사진 마지막 끝(장) | 본 사진 바이트(다 받은 카드) | 자리표시 바이트 | "
        "/img 요청 | 200 아님 | X-Relay-Cache | X-Relay-Image | 업스트림 캐시 | 데이터센터 | 시간 초과 |",
        "|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|",
    ]
    total = 0
    for r in results:
        m = r["metrics"]
        total += m["img_requests"]
        complete = len({x["card"] for x in r["resources"]
                        if x["kind"] == "main" and x["status"] == 200 and x["encoded"] > 0})
        lines.append(
            f"| {r['label']} | {r['profile']} | {r['run']} | {fmt(m['first_placeholder_end_ms'])} | "
            f"{fmt(m['first_main_end_ms'])} | {fmt(m['first_screen_main_last_end_ms'])} ({m['first_screen_main_count']}) | "
            f"{fmt(m['bytes_main'])} ({complete}) | {fmt(m['bytes_placeholder'])} | {m['img_requests']} | "
            f"{m['non_200']} | {kv(m['x_relay_cache'])} | {kv(m['x_relay_image'])} | {kv(m['x_relay_upstream_cache'])} | "
            f"{kv(m['colo'])} | {','.join(r['timed_out']) or '-'} |"
        )
    lines += ["", f"- `/img` 요청 합: {total}"]
    with open(os.path.join(out_dir, "summary.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    return total


def main(argv=None, extra_routes=None):
    parser = argparse.ArgumentParser(description="웹 사진 측정(5c)")
    parser.add_argument("--base", required=True)
    parser.add_argument("--list", required=True, help="목록 응답 JSON(30건)")
    parser.add_argument("--label", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--profiles", default="desktop,mobile,mobile4g")
    parser.add_argument("--runs", type=int, default=3)
    args = parser.parse_args(argv)
    os.makedirs(args.out, exist_ok=True)
    list_body = load_json(args.list)
    requests = 0
    with sync_playwright() as pw:
        browser = pw.chromium.launch(channel="chrome", headless=True)
        try:
            for profile in [p.strip() for p in args.profiles.split(",") if p.strip()]:
                if profile not in PROFILES:
                    raise SystemExit(f"모르는 프로필: {profile}")
                runs = 1 if profile == "mobile4g" else args.runs
                for run_no in range(1, runs + 1):
                    result = run_once(browser, args.base, profile, list_body, args.out, args.label, run_no, extra_routes)
                    m = result["metrics"]
                    requests += m["img_requests"]
                    print(f"{args.label} {profile} {run_no}: 첫 본 사진 {fmt(m['first_main_end_ms'])}ms, "
                          f"본 사진 {fmt(m['bytes_main'])}B({m['cards_with_main']}장), 자리표시 {fmt(m['bytes_placeholder'])}B, "
                          f"/img {m['img_requests']}, 시간 초과 {result['timed_out'] or '-'}", flush=True)
        finally:
            browser.close()
    write_summary(args.out)
    print(f"/img 요청 수(이번 실행): {requests}")
    return requests


if __name__ == "__main__":
    sys.exit(0 if main() >= 0 else 1)
