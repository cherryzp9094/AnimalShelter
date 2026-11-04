package com.cherryzp.animalshelter.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Android App Links 진입(브리핑 L3): `https://<host>/a/<15자리>`만 `[목록, 상세]`. */
class ShareLinkTest {
    private val host = "animalshelter.example"
    private val no = "450650202602738"

    @Test
    fun `공유 링크는 목록 위 상세에서 시작한다`() {
        val expected = listOf(AnimalListKey, AnimalDetailKey(no))
        assertEquals(expected, stackFromShareLink("https://$host/a/$no", host))
        assertEquals(expected, stackFromShareLink("https://$host/a/$no/", host))
        assertEquals(
            expected,
            stackFromShareLink("https://ANIMALSHELTER.example/a/$no?utm=x#y", host)
        )
    }

    @Test
    fun `다른 모양은 받지 않는다`() {
        listOf(
            null,
            "",
            "https://$host/a/123",
            "https://$host/a/${no}1",
            "https://$host/b/$no",
            "https://$host/a/$no/x",
            "http://$host/a/$no",
            "https://other.example/a/$no",
            "https://$host.evil.example/a/$no",
            "https://$host:8443/a/$no",
            "intent://$host/a/$no"
        ).forEach { assertNull(stackFromShareLink(it, host), "$it") }
        assertNull(stackFromShareLink("https://$host/a/$no", ""))
    }
}
