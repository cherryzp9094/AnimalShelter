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
}
