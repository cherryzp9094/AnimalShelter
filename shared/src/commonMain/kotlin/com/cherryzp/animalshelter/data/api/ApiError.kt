package com.cherryzp.animalshelter.data.api

/**
 * API 오류(02-data 10절). 화면 문구는 UI가 이 타입으로 고른다.
 * 키 유출을 막으려고 예외 메시지·cause는 담지 않는다(D5) — 모든 값은 응답 본문이나 상태 코드에서 온다.
 */
sealed interface ApiError {
    /** 공공데이터 게이트웨이 오류(`returnReasonCode`: 22 한도 초과, 30 미등록 키 …). */
    data class Gateway(val code: String, val message: String?) : ApiError

    /** 서비스 오류(`resultCode != "00"`). */
    data class Service(val code: String, val message: String?) : ApiError

    /** 그 밖의 HTTP 상태. */
    data class Http(val status: Int) : ApiError

    /** 연결 실패·타임아웃(Direct·Fixture). */
    data object Network : ApiError

    /** 응답을 읽지 못함. `detail`은 본문 앞 200자(키는 가린다). */
    data class Parse(val detail: String) : ApiError

    /** 중계 서버 연결 실패 또는 중계의 업스트림 실패(502 `relayError`). */
    data object RelayUnavailable : ApiError

    /** 마감 임박순 수집 상한(5,000건) 초과 — 오류라기보다 안내. */
    data class TooManyResults(val totalCount: Int) : ApiError
}

/** API 결과. 예외는 [AnimalShelterApi] 안에서 전부 [ApiError]로 바뀐다. */
sealed interface ApiResult<out T> {
    data class Success<out T>(val value: T) : ApiResult<T>

    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

/** 성공 값을 바꾼다. */
inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Failure -> this
}

/** 성공 값 또는 null. */
fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.value

/** 실패의 오류 또는 null. */
fun ApiResult<*>.errorOrNull(): ApiError? = (this as? ApiResult.Failure)?.error
