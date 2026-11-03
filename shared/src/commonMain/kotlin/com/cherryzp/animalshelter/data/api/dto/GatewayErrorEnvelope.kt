package com.cherryzp.animalshelter.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 공공데이터 게이트웨이 오류 본문(02-data 1.2). 상태 코드는 403 등이지만 200일 때도 있다.
 * 예: `{"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"errMsg":…,"returnAuthMsg":…,"returnReasonCode":"30"}}}`
 */
@Serializable
internal data class GatewayErrorEnvelope(
    @SerialName("OpenAPI_ServiceResponse") val response: GatewayError,
)

@Serializable
internal data class GatewayError(val cmmMsgHeader: CmmMsgHeader? = null)

@Serializable
internal data class CmmMsgHeader(
    val errMsg: String? = null,
    val returnAuthMsg: String? = null,
    val returnReasonCode: String? = null,
)
