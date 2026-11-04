package com.cherryzp.animalshelter.navigation

import androidx.navigation3.runtime.NavKey

/**
 * 공유 링크(`https://<host>/a/<15자리 구조번호>`, 끝 `/` 허용 — 05-service 5.2, 브리핑 L3) → 처음 스택
 * `[목록, 상세(번호)]`. 접수일이 없어 상세는 재조회 창 최대 3회(02-data 6절). 링크가 아니거나 형식이 다르면 null —
 * 그때는 목록에서 시작한다. 쿼리·조각(`?…`·`#…`)은 보지 않는다.
 */
fun stackFromShareLink(url: String?, host: String): List<NavKey>? {
    if (url.isNullOrEmpty() || host.isEmpty()) return null
    val match = shareLink.matchEntire(url.substringBefore('#').substringBefore('?')) ?: return null
    val (linkHost, desertionNo) = match.destructured
    if (!linkHost.equals(host, ignoreCase = true)) return null
    return listOf(AnimalListKey, AnimalDetailKey(desertionNo))
}

private val shareLink = Regex("""https://([^/:?#]+)/a/(\d{15})/?""", RegexOption.IGNORE_CASE)

/**
 * Android 진입 연결(L3): 시작 인텐트의 주소가 [host]의 공유 링크면 `[목록, 상세]`, 아니면 목록에서 시작한다.
 * `:androidApp`이 Nav3 타입을 모른 채 부르도록 [NavigationBinding]으로 돌려준다.
 */
fun shareLinkNavigation(url: String?, host: String): NavigationBinding =
    stackFromShareLink(url, host)?.let { NavigationBinding.StartAt(it) } ?: NavigationBinding.None
