package com.cherryzp.animalshelter.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import com.cherryzp.animalshelter.data.repository.ImageUrlPolicy

/** 사진을 어떤 틀에 그리는지(5c). 웹은 이것과 실제 표시 폭으로 중계 변환 크기를 고른다. */
enum class PhotoFrame { CARD, DETAIL }

/**
 * 화면용 사진 주소의 변형(5c — 02 7절 `/img?size=`). 플랫폼마다 `platformModule()`이 준다(Android [Original],
 * 웹 [RelayPhotoVariants]). 화면은 [LocalPhotoVariants]로 읽는다.
 */
interface PhotoVariants {
    /** [url](화면용 주소)을 [frame]·[widthPx](표시 폭 px)에 맞는 주소로. 변환하지 않으면 그대로. */
    fun main(url: String, frame: PhotoFrame, widthPx: Int): String

    /** 먼저 흐리게 띄울 작은 사진 주소, 쓰지 않으면 null. */
    fun placeholder(url: String): String?

    companion object {
        /** 원본 그대로·자리표시 없음 — Android, 미리 보기, 테스트 기본. */
        val Original: PhotoVariants = object : PhotoVariants {
            override fun main(url: String, frame: PhotoFrame, widthPx: Int): String = url

            override fun placeholder(url: String): String? = null
        }
    }
}

/** 중계 변환 토큰: 카드(정사각 480 자르기). */
const val SIZE_CARD = "sq480"

/** 중계 변환 토큰: 상세·넓은 카드(1080 틀 안). */
const val SIZE_LARGE = "fit1080"

/**
 * 카드에 [SIZE_CARD]를 쓰는 최대 표시 폭(px). `sq480`을 1.4배 넘게 키우지 않는다(680 ÷ 480 ≈ 1.42) — 그보다 넓은
 * 카드(1열·큰 글자 1열)는 상세와 같은 [SIZE_LARGE]를 잘라 그린다. 크기를 기기별로 나누지 않는 것은 고유 변환 수
 * (무료 5,000/달) 때문이다(브리핑 1.3).
 */
const val CARD_SMALL_MAX_PX = 680

/** 중계 사진 주소(절대·같은 origin 상대 둘 다, 쿼리 없음). 그 밖(Fixture·원본 주소)은 바꾸지 않는다. */
private val RELAY_PHOTO =
    Regex(
        """(^|/)img/shelter/\d{4}/\d{2}/[A-Za-z0-9_.%\[\]-]+\.(jpe?g|png)$""",
        RegexOption.IGNORE_CASE
    )

/**
 * 웹: 중계 `/img`에 `?size=`를 붙이고, 작은 사진 `_s`를 자리표시로 준다(5c). 카드는 표시 폭 ≤ [CARD_SMALL_MAX_PX]면
 * [SIZE_CARD], 넘으면 [SIZE_LARGE], 상세는 늘 [SIZE_LARGE]. 중계 사진 주소가 아니거나 이미 작은 사진이면 그대로·null.
 */
object RelayPhotoVariants : PhotoVariants {
    override fun main(url: String, frame: PhotoFrame, widthPx: Int): String {
        if (!isRelayPhoto(url) || ImageUrlPolicy.thumbnailOf(url) == null) return url
        val size = if (frame == PhotoFrame.CARD &&
            widthPx <= CARD_SMALL_MAX_PX
        ) {
            SIZE_CARD
        } else {
            SIZE_LARGE
        }
        return "$url?size=$size"
    }

    override fun placeholder(url: String): String? =
        if (isRelayPhoto(url)) ImageUrlPolicy.thumbnailOf(url) else null

    private fun isRelayPhoto(url: String): Boolean =
        '?' !in url && '#' !in url && RELAY_PHOTO.containsMatchIn(url)
}

/** 화면이 읽는 사진 변형. 기본은 [PhotoVariants.Original] — 미리 보기·테스트에 안전하다. `App()`이 Koin 값을 넣는다. */
val LocalPhotoVariants = staticCompositionLocalOf<PhotoVariants> { PhotoVariants.Original }
