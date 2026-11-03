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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 오류 문구 표(02-data 10절, 브리핑 P5·A20). */
class ErrorTextTest {
    @Test
    fun `한도 초과는 error_quota이고 다시 시도 없음`() {
        val quota = ApiError.Gateway("22", "LIMITED")
        assertEquals(uiText(Res.string.error_quota), quota.toUiText(isDebug = true))
        assertFalse(quota.canRetry())
    }

    @Test
    fun `인증키 오류는 릴리즈에 코드 없이, 디버그에 코드와 함께`() {
        for (code in listOf("30", "31", "20", "29", "32")) {
            val error = ApiError.Gateway(code, null)
            assertEquals(uiText(Res.string.error_key), error.toUiText(isDebug = false))
            assertEquals(uiText(Res.string.error_key_code, code), error.toUiText(isDebug = true))
            assertTrue(error.canRetry())
        }
    }

    @Test
    fun `그 밖의 게이트웨이·서비스·HTTP는 error_server 코드`() {
        assertEquals(
            uiText(Res.string.error_server, "12"),
            ApiError.Gateway("12", null).toUiText(false)
        )
        assertEquals(
            uiText(Res.string.error_server, "99"),
            ApiError.Service("99", "x").toUiText(false)
        )
        assertEquals(uiText(Res.string.error_server, "500"), ApiError.Http(500).toUiText(false))
    }

    @Test
    fun `연결 실패·중계 실패는 error_network, 파싱은 error_parse`() {
        assertEquals(uiText(Res.string.error_network), ApiError.Network.toUiText(false))
        assertEquals(uiText(Res.string.error_network), ApiError.RelayUnavailable.toUiText(false))
        assertEquals(uiText(Res.string.error_parse), ApiError.Parse("<html>").toUiText(false))
        assertTrue(ApiError.Network.canRetry())
    }

    @Test
    fun `5000건 초과는 sort_too_many이고 다시 시도 없음`() {
        val tooMany = ApiError.TooManyResults(7544)
        assertEquals(uiText(Res.string.sort_too_many), tooMany.toUiText(false))
        assertFalse(tooMany.canRetry())
    }
}
