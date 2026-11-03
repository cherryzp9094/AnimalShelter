package com.cherryzp.animalshelter.ui.common

/**
 * 사진 주소를 Coil이 받을 절대 주소로 바꾼다. 웹은 같은 origin 중계(`Relay("")`)면 화면용 주소가 `/img/…`처럼
 * 상대 경로인데, Coil의 네트워크 fetcher는 http·https만 받는다 — 웹 `platformModule()`이 지금 페이지의
 * origin을 붙인다. Android는 그대로.
 */
fun interface ImageUrlResolver {
    fun resolve(url: String): String
}
