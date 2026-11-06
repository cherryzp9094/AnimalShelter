package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.ApiEndpoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    fun `thumbnailOf - 원본 jpg·jpeg·png·대문자 확장자는 이름_s jpg로(5c)`() {
        val base = https.removeSuffix("20261103141175.jpg")
        assertEquals(
            "${base}202611041511187_s.jpg",
            ImageUrlPolicy.thumbnailOf("${base}202611041511187.jpg")
        )
        assertEquals(
            "${base}202611042111228_s.jpg",
            ImageUrlPolicy.thumbnailOf("${base}202611042111228.jpeg")
        )
        assertEquals("${base}x_s.jpg", ImageUrlPolicy.thumbnailOf("${base}x.png"))
        assertEquals("${base}x_s.jpg", ImageUrlPolicy.thumbnailOf("${base}x.JPG"))
        assertEquals(
            "http://openapi.animal.go.kr/a/x_s.jpg",
            ImageUrlPolicy.thumbnailOf("http://openapi.animal.go.kr/a/x.jpg")
        )
    }

    @Test
    fun `thumbnailOf - 대괄호 원문·인코딩, 중계 상대·절대 주소`() {
        val base = https.removeSuffix("20261103141175.jpg")
        assertEquals(
            "${base}202611041011648[1]_s.jpg",
            ImageUrlPolicy.thumbnailOf("${base}202611041011648[1].jpg")
        )
        assertEquals(
            "/img/shelter/2026/10/202611041011648%5B1%5D_s.jpg",
            ImageUrlPolicy.thumbnailOf("/img/shelter/2026/10/202611041011648%5B1%5D.jpg")
        )
        assertEquals(
            "http://localhost:8788/img/shelter/2026/10/x_s.jpg",
            ImageUrlPolicy.thumbnailOf("http://localhost:8788/img/shelter/2026/10/x.jpeg")
        )
    }

    @Test
    fun `thumbnailOf - 이미 _s·쿼리·확장자 없음은 null`() {
        assertNull(ImageUrlPolicy.thumbnailOf("/img/shelter/2026/10/x_s.jpg"))
        assertNull(ImageUrlPolicy.thumbnailOf("/img/shelter/2026/10/x.jpg?size=sq480"))
        assertNull(ImageUrlPolicy.thumbnailOf("https://example.com/x.jpg#a"))
        assertNull(ImageUrlPolicy.thumbnailOf("photo-1"))
        assertNull(ImageUrlPolicy.thumbnailOf("https://example.com/x.gif"))
        assertNull(ImageUrlPolicy.thumbnailOf("https://example.com/.jpg"))
    }
}
