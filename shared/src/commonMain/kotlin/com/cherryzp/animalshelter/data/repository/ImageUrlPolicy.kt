package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.ApiEndpoint

/**
 * 사진 URL 규칙(02-data 5.3).
 * - 저장(북마크 포함)은 [normalize]한 원본만. 중계 주소는 저장하지 않는다.
 * - 화면에 낼 때는 [displayUrl] — 웹(Relay)은 사진 서버에 CORS가 없어 중계 `/img`를 거친다.
 */
object ImageUrlPolicy {
    private const val HTTP_ORIGIN = "http://openapi.animal.go.kr/"
    private const val HTTPS_ORIGIN = "https://openapi.animal.go.kr/"
    private const val FILES_PREFIX =
        "https://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/"

    /** `http://openapi.animal.go.kr/` → `https://`(같은 파일이 200으로 온다). 다른 호스트는 그대로. */
    fun normalize(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith(
                HTTP_ORIGIN
            )
        ) {
            HTTPS_ORIGIN + trimmed.removePrefix(HTTP_ORIGIN)
        } else {
            trimmed
        }
    }

    /**
     * 화면용 URL. Relay면 `…/fileDownloadSrvc/files/<rest>` → `<base>/img/<rest>`(`[`·`]`는 `%5B`·`%5D`,
     * base가 빈 문자열이면 같은 origin의 `/img/<rest>`). Direct·Fixture는 [normalize] 결과 그대로.
     */
    fun displayUrl(url: String, endpoint: ApiEndpoint): String {
        val normalized = normalize(url)
        if (endpoint !is ApiEndpoint.Relay ||
            !normalized.startsWith(FILES_PREFIX)
        ) {
            return normalized
        }
        val rest = normalized.removePrefix(FILES_PREFIX).replace("[", "%5B").replace("]", "%5D")
        return "${endpoint.baseUrl.trimEnd('/')}/img/$rest"
    }

    /** 작은 사진으로 바꿀 수 있는 주소: 마지막 조각이 `<이름>.<jpg|jpeg|png>`(대소문자 무시). */
    private val THUMBNAIL_SOURCE =
        Regex("""^(.*/)?([^/]+)\.(jpe?g|png)$""", RegexOption.IGNORE_CASE)

    /**
     * 사진 서버의 작은 사진(가로 120px, 평균 4.5KB) 주소(5c). `…/<이름>.<jpg|jpeg|png>` → `…/<이름>_s.jpg` —
     * 원본 확장자와 무관하게 늘 `.jpg`다(같은 확장자 `_s.jpeg`·`_s.png`는 404). 사진 서버의 **비공식** 규칙이다 —
     * survey 7절, 2026-11-06 실측 14/14·첫 화면 30/30이 200. 원본 주소와 중계 주소(`/img/…%5B1%5D.jpg`) 둘 다 받는다.
     * 이름이 이미 `_s`로 끝나거나, 쿼리·조각(`?`·`#`)이 있거나, 형식이 맞지 않으면 null.
     */
    fun thumbnailOf(url: String): String? {
        if ('?' in url || '#' in url) return null
        val match = THUMBNAIL_SOURCE.matchEntire(url) ?: return null
        val (prefix, name) = match.destructured
        if (name.endsWith("_s", ignoreCase = true)) return null
        return "$prefix${name}_s.jpg"
    }
}
