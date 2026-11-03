package com.cherryzp.animalshelter.ui.common

import com.cherryzp.animalshelter.data.api.ApiError
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.error_key
import com.cherryzp.animalshelter.resources.error_key_code
import com.cherryzp.animalshelter.resources.error_network
import com.cherryzp.animalshelter.resources.error_parse
import com.cherryzp.animalshelter.resources.error_quota
import com.cherryzp.animalshelter.resources.error_server
import com.cherryzp.animalshelter.resources.sort_too_many

/** 게이트웨이 인증키 오류 코드(02-data 10절). */
private val KEY_ERROR_CODES = setOf("30", "31", "20", "29", "32")

/** 한도 초과 코드. */
private const val QUOTA_CODE = "22"

/**
 * 오류 문구 한곳(02-data 10절, 브리핑 P5·A20). 인증키 오류는 디버그 빌드에서만 코드를 보인다.
 * 예외 메시지·URL은 [ApiError]에 없으므로 문구에도 들어가지 않는다.
 */
fun ApiError.toUiText(isDebug: Boolean): UiText = when (this) {
    is ApiError.Gateway -> when (code) {
        QUOTA_CODE -> uiText(Res.string.error_quota)

        in KEY_ERROR_CODES -> if (isDebug) {
            uiText(Res.string.error_key_code, code)
        } else {
            uiText(Res.string.error_key)
        }

        else -> uiText(Res.string.error_server, code)
    }

    is ApiError.Service -> uiText(Res.string.error_server, code)

    is ApiError.Http -> uiText(Res.string.error_server, status.toString())

    ApiError.Network, ApiError.RelayUnavailable -> uiText(Res.string.error_network)

    is ApiError.Parse -> uiText(Res.string.error_parse)

    is ApiError.TooManyResults -> uiText(Res.string.sort_too_many)
}

/** "다시 시도"를 보일지. 한도 초과(`Gateway("22")`)와 5,000건 초과 안내는 다시 해도 같다. */
fun ApiError.canRetry(): Boolean = when (this) {
    is ApiError.Gateway -> code != QUOTA_CODE
    is ApiError.TooManyResults -> false
    else -> true
}
