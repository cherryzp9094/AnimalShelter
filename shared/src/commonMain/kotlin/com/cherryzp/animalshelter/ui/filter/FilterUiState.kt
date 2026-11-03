package com.cherryzp.animalshelter.ui.filter

import androidx.compose.runtime.Immutable
import com.cherryzp.animalshelter.data.repository.CacheState
import com.cherryzp.animalshelter.model.Kind
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.ShelterCode
import com.cherryzp.animalshelter.model.Sido
import com.cherryzp.animalshelter.model.Sigungu
import com.cherryzp.animalshelter.ui.common.QuickDate
import com.cherryzp.animalshelter.ui.common.UiText
import kotlinx.datetime.LocalDate

/**
 * 필터 화면 상태(브리핑 3a 5절, 03 2.2).
 * @property sigungu 시도를 안 골랐으면 null
 * @property shelters 시군구를 안 골랐으면 null
 * @property kinds 축종이 전체면 null
 * @property breedQuery 품종 칸에 쓴 글자(목록을 거른다)
 * @property dateError 날짜 인라인 오류(있으면 적용 불가)
 * @property quickDate 지금 날짜가 빠른 칩 하나와 같으면 그것
 * @property today 서울 기준 오늘(날짜 선택의 상한, 빠른 칩)
 */
@Immutable
data class FilterUiState(
    val filter: SearchFilter = SearchFilter(),
    val sido: CacheState<List<Sido>> = CacheState(null, false, null),
    val sigungu: CacheState<List<Sigungu>>? = null,
    val shelters: CacheState<List<ShelterCode>>? = null,
    val kinds: CacheState<List<Kind>>? = null,
    val breedQuery: String = "",
    val dateError: UiText? = null,
    val quickDate: QuickDate? = null,
    val today: LocalDate = LocalDate(2026, 1, 1),
    val showClosingSoonHint: Boolean = false,
    val canApply: Boolean = true,
)
