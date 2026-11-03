package com.cherryzp.animalshelter.data.api

import com.cherryzp.animalshelter.data.api.dto.ApiBody
import com.cherryzp.animalshelter.data.api.dto.ApiEnvelope
import com.cherryzp.animalshelter.data.api.dto.GatewayErrorEnvelope
import com.cherryzp.animalshelter.data.repository.clean
import io.ktor.http.decodeURLQueryComponent
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject

/** 받은 응답(상태 코드 + 본문 글자). */
internal data class RawResponse(val status: Int, val body: String)

private const val GATEWAY_ROOT = "OpenAPI_ServiceResponse"
private const val RELAY_ERROR = "relayError"
private const val PARSE_DETAIL_LENGTH = 200
private val XML_REASON_CODE = Regex("""<returnReasonCode>\s*(\d+)\s*<""")
private val XML_AUTH_MSG = Regex("""<returnAuthMsg>([^<]*)<""")
private val XML_ERR_MSG = Regex("""<errMsg>([^<]*)<""")
private val XML_RESULT_CODE = Regex("""<resultCode>\s*(\d+)\s*<""")
private val XML_RESULT_MSG = Regex("""<resultMsg>([^<]*)<""")
private val SERVICE_KEY_PARAM = Regex("""(?i)(serviceKey=)[^&\s"'<>]*""")

/**
 * 응답을 분류한다(D6). 순서:
 * ② 게이트웨이 오류 JSON(상태 코드 무관) → ③ XML 본문 → ④ Relay 502 + `relayError` → ⑤ 상태 ≠ 200
 * → ⑥ 봉투 파싱 실패 → ⑦ `resultCode != "00"`. ①(연결 실패)은 호출하는 쪽이 먼저 가른다.
 */
internal fun <T> classifyResponse(
    raw: RawResponse,
    endpoint: ApiEndpoint,
    itemSerializer: KSerializer<T>,
): ApiResult<ApiBody<T>> {
    val text = raw.body.trimStart()
    val json = if (text.startsWith("{")) {
        runCatching { ApiJson.parseToJsonElement(text) as? JsonObject }.getOrNull()
    } else {
        null
    }

    if (json != null && GATEWAY_ROOT in json) {
        val header = runCatching {
            ApiJson.decodeFromJsonElement(GatewayErrorEnvelope.serializer(), json)
        }.getOrNull()?.response?.cmmMsgHeader
        val code = header?.returnReasonCode.clean() ?: return parseFailure(raw.body, endpoint)
        return ApiResult.Failure(
            ApiError.Gateway(code, header?.returnAuthMsg.clean() ?: header?.errMsg.clean())
        )
    }

    if (text.startsWith("<")) return ApiResult.Failure(classifyXml(text, raw.body, endpoint))

    if (endpoint is ApiEndpoint.Relay && raw.status == 502 && json != null && RELAY_ERROR in json) {
        return ApiResult.Failure(ApiError.RelayUnavailable)
    }

    if (raw.status != 200) return ApiResult.Failure(ApiError.Http(raw.status))

    val envelope = json?.let {
        runCatching { ApiJson.decodeFromJsonElement(ApiEnvelope.serializer(itemSerializer), it) }
            .getOrNull()
    } ?: return parseFailure(raw.body, endpoint)

    val header = envelope.response.header
    if (header.resultCode.trim() != "00") {
        return ApiResult.Failure(
            ApiError.Service(
                header.resultCode.trim(),
                header.resultMsg.clean() ?: header.errorMsg.clean()
            )
        )
    }
    return ApiResult.Success(envelope.response.body ?: ApiBody())
}

private fun classifyXml(text: String, body: String, endpoint: ApiEndpoint): ApiError {
    XML_REASON_CODE.find(text)?.let { match ->
        val message = XML_AUTH_MSG.find(text)?.groupValues?.get(1).clean()
            ?: XML_ERR_MSG.find(text)?.groupValues?.get(1).clean()
        return ApiError.Gateway(match.groupValues[1], message)
    }
    XML_RESULT_CODE.find(text)?.let { match ->
        val code = match.groupValues[1]
        if (code != "00") {
            return ApiError.Service(code, XML_RESULT_MSG.find(text)?.groupValues?.get(1).clean())
        }
    }
    return ApiError.Parse(parseDetail(body, endpoint))
}

private fun parseFailure(body: String, endpoint: ApiEndpoint): ApiResult.Failure =
    ApiResult.Failure(ApiError.Parse(parseDetail(body, endpoint)))

/**
 * `Parse.detail`: 본문 앞 200자. 본문에 요청 주소가 섞여 올 때를 대비해 키(인코딩형·디코딩형)와
 * `serviceKey=` 값을 가린다(D5).
 */
internal fun parseDetail(body: String, endpoint: ApiEndpoint): String {
    var text = body
    if (endpoint is ApiEndpoint.Direct && endpoint.serviceKey.isNotEmpty()) {
        text = text.replace(endpoint.serviceKey, "***")
        val decoded = runCatching { endpoint.serviceKey.decodeURLQueryComponent() }.getOrNull()
        if (!decoded.isNullOrEmpty()) text = text.replace(decoded, "***")
    }
    return text.replace(SERVICE_KEY_PARAM, "$1***").take(PARSE_DETAIL_LENGTH)
}
