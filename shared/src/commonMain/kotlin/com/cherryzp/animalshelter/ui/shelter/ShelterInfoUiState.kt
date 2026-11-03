package com.cherryzp.animalshelter.ui.shelter

import androidx.compose.runtime.Immutable
import com.cherryzp.animalshelter.ui.common.UiMessage
import com.cherryzp.animalshelter.ui.common.UiText
import com.cherryzp.animalshelter.ui.detail.DetailRow
import com.cherryzp.animalshelter.ui.detail.MapTarget

/**
 * 보호소 정보 상태(브리핑 3b 3절·B12).
 * @property title `fallbackName` → 받은 뒤 `name`
 * @property info 보호소 정보 캐시에서 찾았을 때만
 * @property fallback 목록이 준 이름·전화·주소(줄인 화면)
 * @property isLoading 첫 로딩(가운데 진행 표시)
 * @property isLimited 전체 목록에 없는 보호소 → `shelter_limited_info` 배너 + 줄인 화면
 * @property error 받지 못했다(줄인 화면 + 다시 시도)
 * @property today 오늘 운영시간 부분(B7)
 * @property isDataOld 데이터 기준일이 2년 넘게 지났다 → `shelter_stale_notice`
 */
@Immutable
data class ShelterInfoUiState(
    val title: String? = null,
    val info: ShelterInfoUi? = null,
    val fallback: ShelterFallbackUi = ShelterFallbackUi(),
    val isLoading: Boolean = false,
    val isLimited: Boolean = false,
    val error: UiText? = null,
    val canRetry: Boolean = false,
    val today: List<UiText> = emptyList(),
    val isDataOld: Boolean = false,
    val message: UiMessage? = null,
)

/**
 * 보호소 정보 한 곳(03 2.6, 05 4.4). 행은 값이 있는 것만.
 * @property tel 보호소 정보의 전화(가려진 값이면 목록의 전화)
 * @property facilities 시설 줄마다 부분(" · "로 잇는다) — 수의사·관리사 / 방 / 차량
 */
@Immutable
data class ShelterInfoUi(
    val header: List<DetailRow>,
    val tel: String?,
    val roadAddress: String?,
    val jibunAddress: String?,
    val map: MapTarget?,
    val hours: List<DetailRow>,
    val targetAnimals: String?,
    val facilities: List<List<UiText>>,
    val designatedOn: String?,
    val dataDate: String?,
)

/** 줄인 화면(목록이 준 요약). */
@Immutable
data class ShelterFallbackUi(
    val name: String? = null,
    val tel: String? = null,
    val address: String? = null,
)
