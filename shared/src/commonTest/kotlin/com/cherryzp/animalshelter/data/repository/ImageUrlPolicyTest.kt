package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.ApiEndpoint
import kotlin.test.Test
import kotlin.test.assertEquals

/** 사진 URL 정책(02-data 5.3). */
class ImageUrlPolicyTest {
    private val http = "http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/" +
        "shelter/2026/10/20261103141175.jpg"
    private val https = http.replace("http://", "https://")

    @Test
    fun `http는 https로, 다른 호스트는 그대로`() {
        assertEquals(https, ImageUrlPolicy.normalize(http))
        assertEquals(https, ImageUrlPolicy.normalize(https))
        assertEquals(
            "http://example.com/a.jpg",
            ImageUrlPolicy.normalize("http://example.com/a.jpg")
        )
    }

    @Test
    fun `Relay면 중계 img 경로로 다시 쓴다`() {
        assertEquals(
            "http://localhost:8788/img/shelter/2026/10/20261103141175.jpg",
            ImageUrlPolicy.displayUrl(http, ApiEndpoint.Relay("http://localhost:8788/"))
        )
    }

    @Test
    fun `Relay 주소가 비면 같은 origin의 상대 경로`() {
        assertEquals(
            "/img/shelter/2026/10/20261103141175.jpg",
            ImageUrlPolicy.displayUrl(http, ApiEndpoint.Relay(""))
        )
    }

    @Test
    fun `대괄호는 Relay로 다시 쓸 때 퍼센트 인코딩한다`() {
        val bracket = https.replace("20261103141175.jpg", "2026110314[1].jpg")
        assertEquals(
            "https://relay.example/img/shelter/2026/10/2026110314%5B1%5D.jpg",
            ImageUrlPolicy.displayUrl(bracket, ApiEndpoint.Relay("https://relay.example"))
        )
    }

    @Test
    fun `Direct·Fixture는 정규화한 원본`() {
        assertEquals(https, ImageUrlPolicy.displayUrl(http, ApiEndpoint.Direct("k")))
        assertEquals(https, ImageUrlPolicy.displayUrl(http, ApiEndpoint.Fixture))
        assertEquals(
            "https://example.com/a.jpg",
            ImageUrlPolicy.displayUrl("https://example.com/a.jpg", ApiEndpoint.Relay("x"))
        )
    }
}
