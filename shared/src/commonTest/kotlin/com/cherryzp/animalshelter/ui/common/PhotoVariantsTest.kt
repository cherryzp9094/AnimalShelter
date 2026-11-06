package com.cherryzp.animalshelter.ui.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 웹 사진 변형(5c — 브리핑 1.5 (b)·(c)). */
class PhotoVariantsTest {
    private val relay = "/img/shelter/2026/10/202611041511187.jpg"
    private val relayAbsolute =
        "http://localhost:8788/img/shelter/2026/10/202611041011648%5B1%5D.jpg"
    private val origin =
        "https://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/10/x.jpg"

    @Test
    fun `Relay 카드는 680px까지 sq480, 넘으면 fit1080`() {
        for (width in listOf(0, 480, 680)) {
            assertEquals(
                "$relay?size=sq480",
                RelayPhotoVariants.main(relay, PhotoFrame.CARD, width)
            )
        }
        for (width in listOf(681, 1200)) {
            assertEquals(
                "$relay?size=fit1080",
                RelayPhotoVariants.main(relay, PhotoFrame.CARD, width)
            )
        }
        assertEquals(
            "$relayAbsolute?size=sq480",
            RelayPhotoVariants.main(relayAbsolute, PhotoFrame.CARD, 420)
        )
    }

    @Test
    fun `Relay 상세는 늘 fit1080`() {
        for (width in listOf(0, 5000)) {
            assertEquals(
                "$relay?size=fit1080",
                RelayPhotoVariants.main(relay, PhotoFrame.DETAIL, width)
            )
        }
    }

    @Test
    fun `Relay는 중계 사진 주소가 아니면 그대로, 자리표시 없음`() {
        for (url in listOf(
            origin,
            "photo-1",
            "$relay?size=sq480",
            "/img/shelter/2026/10/x_s.jpg"
        )) {
            assertEquals(url, RelayPhotoVariants.main(url, PhotoFrame.CARD, 400))
            assertEquals(url, RelayPhotoVariants.main(url, PhotoFrame.DETAIL, 400))
            assertNull(RelayPhotoVariants.placeholder(url))
        }
    }

    @Test
    fun `Relay 자리표시는 _s jpg`() {
        assertEquals(
            "/img/shelter/2026/10/202611041511187_s.jpg",
            RelayPhotoVariants.placeholder(relay)
        )
        assertEquals(
            "http://localhost:8788/img/shelter/2026/10/202611041011648%5B1%5D_s.jpg",
            RelayPhotoVariants.placeholder(relayAbsolute)
        )
    }

    @Test
    fun `Original은 그대로·자리표시 없음`() {
        for (url in listOf(relay, origin, "photo-1")) {
            assertEquals(url, PhotoVariants.Original.main(url, PhotoFrame.CARD, 400))
            assertEquals(url, PhotoVariants.Original.main(url, PhotoFrame.DETAIL, 2000))
            assertNull(PhotoVariants.Original.placeholder(url))
        }
    }
}
