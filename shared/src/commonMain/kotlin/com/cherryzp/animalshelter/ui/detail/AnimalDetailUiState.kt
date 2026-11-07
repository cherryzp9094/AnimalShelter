package com.cherryzp.animalshelter.ui.detail

import androidx.compose.runtime.Immutable
import com.cherryzp.animalshelter.ui.common.StatusUi
import com.cherryzp.animalshelter.ui.common.UiMessage
import com.cherryzp.animalshelter.ui.common.UiText

/**
 * 상세 화면 상태(브리핑 3b 3절·B6).
 * @property detail 보일 것(스냅샷 또는 조회 결과). 없으면 로딩·오류·없음 중 하나
 * @property isLoading 조회 중(스냅샷이 보일 수 있다 — 그때는 상단 선)
 * @property isStale 다시 조회했더니 0건 → 저장된 정보(`detail_notice_gone` 배너)
 * @property notFound 어디에도 없다 → `detail_not_found` + 목록으로
 * @property error 보일 것이 없을 때만(가운데 오류). 보이는 것이 있으면 스낵바(`message`)
 * @property shelterToday 보호소 정보 캐시가 있으면 오늘 운영시간 부분(B7)
 * @property map 보호소 좌표가 있을 때만
 * @property isHidden 사망 종료이고 북마크가 아니며 필터가 꺼져 있어 보이지 않는다(5d) — `detail_hidden` + 목록으로.
 *   그때 `detail`·`shareUrl`은 null이고 로딩·오류·없음도 아니다
 * @property shareUrl 공유 링크(`<shareBaseUrl>/a/<구조번호>`, 4a S4). 공유 주소가 없으면(Android `shareHost` 없음) null —
 *   공유 버튼이 없다
 */
@Immutable
data class AnimalDetailUiState(
    val detail: AnimalDetailUi? = null,
    val isLoading: Boolean = false,
    val isBookmarked: Boolean = false,
    val isStale: Boolean = false,
    val notFound: Boolean = false,
    val error: UiText? = null,
    val canRetry: Boolean = false,
    val shelterToday: List<UiText> = emptyList(),
    val map: MapTarget? = null,
    val message: UiMessage? = null,
    val shareUrl: String? = null,
    val isHidden: Boolean = false,
)

/**
 * 상세에 보일 한 마리(03 2.3 섹션 1~8). 행은 값이 있는 것만 — 매퍼([toDetailUi])가 뺀다.
 * @property photoUrls 화면용 URL(엔드포인트 반영)
 * @property photoOriginals 정규화한 원본 URL — 사진 뷰어 키에 싣는다(B1)
 * @property titleParts 카드 제목 부분(P10) — 사진 뷰어 제목·접근성·공유 제목
 * @property shelterLine 카드의 보호소 줄(공유 본문, S3)
 * @property noticeEnd 공고 종료일 글자(공고 기간의 끝과 같은 꼴 — 공유 본문 `notice_until`, S3)
 * @property name 요약 제목(품종명)
 * @property kindLabel 보조 제목(축종)
 */
@Immutable
data class AnimalDetailUi(
    val desertionNo: String,
    val photoUrls: List<String>,
    val photoOriginals: List<String>,
    val titleParts: List<UiText>,
    val shelterLine: String? = null,
    val noticeEnd: String? = null,
    val name: String?,
    val kindLabel: UiText?,
    val summary: List<DetailRow>,
    val status: StatusUi,
    val statusHint: UiText?,
    val notice: List<DetailRow>,
    val traits: String?,
    val found: List<DetailRow>,
    val health: List<DetailRow>,
    val shelter: DetailShelterUi?,
    val promotions: List<PromotionUi>,
)

/**
 * 라벨 위에 값(06 4.2 — 표 금지). `values`는 " · "로 잇고, `chips`는 칩으로 나열한다(백신·건강 검사).
 * @property numeric 숫자가 많은 값이면 고정폭 숫자(P7)
 */
@Immutable
data class DetailRow(
    val label: UiText,
    val values: List<UiText> = emptyList(),
    val chips: List<String> = emptyList(),
    val numeric: Boolean = false,
)

/** 상세의 보호소 카드(목록이 준 요약). 전화는 목록 값(보호소 정보의 가려진 번호가 아니다). */
@Immutable
data class DetailShelterUi(
    val regNo: String?,
    val name: String?,
    val tel: String?,
    val address: String?,
    val orgName: String?,
)

/** 보호소 안내 카드 하나. `imageUrl`은 화면용 URL. */
@Immutable
data class PromotionUi(
    val type: UiText,
    val title: String?,
    val period: UiText?,
    val condition: String?,
    val text: String?,
    val imageUrl: String?,
)

/** 지도 열기 대상(보호소 좌표). */
@Immutable
data class MapTarget(val lat: Double, val lng: Double, val label: String)
