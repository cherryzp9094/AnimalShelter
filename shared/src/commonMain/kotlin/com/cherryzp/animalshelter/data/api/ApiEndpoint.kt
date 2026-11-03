package com.cherryzp.animalshelter.data.api

/**
 * 공공데이터 API를 어디로 부를지. 근거: docs/design/02-data.md 6절. Phase 1은 타입만 둔다.
 * `toString()`은 인증키를 가린다(로그·크래시 보고에 키가 찍히지 않게).
 */
sealed interface ApiEndpoint {
    /** Android: 공공데이터 API를 직접 부른다. 키는 BuildConfig에서 온다. */
    data class Direct(
        val serviceKey: String,
        val baseUrl: String = "https://apis.data.go.kr/1543061",
    ) : ApiEndpoint {
        override fun toString(): String = "Direct(key=***, baseUrl=$baseUrl)"
    }

    /** 웹(그리고 Android 디버그 시험): 키를 가진 중계 서버를 부른다. 빈 문자열이면 같은 origin의 상대 경로. */
    data class Relay(val baseUrl: String) : ApiEndpoint

    /** 키가 없을 때·테스트: MockEngine이 docs/api/samples 픽스처를 돌려준다(Phase 2). */
    data object Fixture : ApiEndpoint
}
