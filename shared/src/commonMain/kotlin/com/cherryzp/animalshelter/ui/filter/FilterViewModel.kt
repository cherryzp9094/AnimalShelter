package com.cherryzp.animalshelter.ui.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryzp.animalshelter.data.repository.AnimalRepository
import com.cherryzp.animalshelter.data.repository.CacheState
import com.cherryzp.animalshelter.data.repository.CodeTable
import com.cherryzp.animalshelter.data.repository.CodeTableRepository
import com.cherryzp.animalshelter.data.repository.LoadState
import com.cherryzp.animalshelter.data.repository.SettingsRepository
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.Kind
import com.cherryzp.animalshelter.model.Neutered
import com.cherryzp.animalshelter.model.NoticeState
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.Sex
import com.cherryzp.animalshelter.model.ShelterCode
import com.cherryzp.animalshelter.model.Sido
import com.cherryzp.animalshelter.model.Sigungu
import com.cherryzp.animalshelter.model.SortOrder
import com.cherryzp.animalshelter.model.todayInSeoul
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.filter_date_invalid
import com.cherryzp.animalshelter.resources.filter_date_range_too_long
import com.cherryzp.animalshelter.ui.common.QuickDate
import com.cherryzp.animalshelter.ui.common.UiText
import com.cherryzp.animalshelter.ui.common.quickDateOf
import com.cherryzp.animalshelter.ui.common.rangeOn
import com.cherryzp.animalshelter.ui.common.uiText
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus

/** 직접 고를 수 있는 날짜 범위의 최대 일수(실측: API 창 제한, 03 2.2). */
const val MAX_DATE_RANGE_DAYS = 365

/** 종료일만 골랐을 때 시작일 = 종료일 − 29일(브리핑 A18). */
private const val DEFAULT_RANGE_DAYS = 30

/**
 * 필터 화면(03 2.2, 브리핑 A17·A18). 시작 조건: 결과가 있거나 받는 중(또는 시도했다 실패)이면 그 조건,
 * 아니면 저장된 마지막 조건(프로세스 종료 뒤 복귀). 코드를 고르면 표시 이름도 함께 채운다(1-6).
 * 적용은 검색을 앱 범위로 시작만 하고 기다리지 않는다 — 화면은 바로 닫히고 저장은 목록 ViewModel이 한다(A16).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FilterViewModel(
    private val animals: AnimalRepository,
    private val codeTables: CodeTableRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {
    private val filter = MutableStateFlow(SearchFilter())
    private val breedQuery = MutableStateFlow("")

    init {
        val results = animals.getSearchResultsStream().value
        val attempted =
            results.fetchedAt != null || results.loading != LoadState.IDLE || results.error != null
        if (attempted) {
            start(results.filter)
        } else {
            viewModelScope.launch {
                start(settings.getSettingsStream().first().lastFilter ?: SearchFilter())
            }
        }
    }

    private val sigungu: Flow<CacheState<List<Sigungu>>?> = filter.map { it.sidoCode }
        .distinctUntilChanged()
        .flatMapLatest { sido -> sido?.let { codeTables.getSigunguStream(it) } ?: flowOf(null) }

    private val shelters: Flow<CacheState<List<ShelterCode>>?> = filter
        .map { it.sidoCode to it.sigunguCode }
        .distinctUntilChanged()
        .flatMapLatest { (sido, sigungu) ->
            if (sido != null &&
                sigungu != null
            ) {
                codeTables.getSheltersStream(sido, sigungu)
            } else {
                flowOf(null)
            }
        }

    private val kinds: Flow<CacheState<List<Kind>>?> = filter.map { it.kind }
        .distinctUntilChanged()
        .flatMapLatest { kind -> kind?.let { codeTables.getKindsStream(it) } ?: flowOf(null) }

    private val closingSoon: Flow<Boolean> = animals.getSearchResultsStream()
        .map { (it.pendingSort ?: it.sort) == SortOrder.CLOSING_SOON }
        .distinctUntilChanged()

    private data class Codes(
        val sido: CacheState<List<Sido>>,
        val sigungu: CacheState<List<Sigungu>>?,
        val shelters: CacheState<List<ShelterCode>>?,
        val kinds: CacheState<List<Kind>>?,
    )

    private val codes: Flow<Codes> =
        combine(codeTables.getSidoStream(), sigungu, shelters, kinds, ::Codes)

    val uiState: StateFlow<FilterUiState> =
        combine(filter, breedQuery, codes, closingSoon) { filter, query, codes, closingSoon ->
            val today = clock.todayInSeoul()
            val dateError = dateErrorOf(filter.dateFrom, filter.dateTo)
            FilterUiState(
                filter = filter,
                sido = codes.sido,
                sigungu = codes.sigungu,
                shelters = codes.shelters,
                kinds = codes.kinds,
                breedQuery = query,
                dateError = dateError,
                quickDate = quickDateOf(filter.dateFrom, filter.dateTo, today),
                today = today,
                showClosingSoonHint = closingSoon,
                canApply = dateError == null
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FilterUiState(today = clock.todayInSeoul())
        )

    /** 축종. 바꾸면 품종(코드·이름·검색어)을 비운다. null = 전체. */
    fun onKindSelected(kind: AnimalKind?) {
        if (kind == filter.value.kind) return
        filter.update { it.copy(kind = kind, kindCode = null, kindName = null) }
        breedQuery.value = ""
    }

    fun onBreedQueryChanged(query: String) {
        breedQuery.value = query
    }

    /** 품종. null = 전체. 검색어는 고른 이름으로 바꾼다. */
    fun onBreedSelected(kind: Kind?) {
        filter.update { it.copy(kindCode = kind?.code, kindName = kind?.name) }
        breedQuery.value = kind?.name.orEmpty()
    }

    /** 빠른 날짜 칩. null = 날짜 지우기. */
    fun onQuickDate(quick: QuickDate?) {
        val range = quick?.rangeOn(clock.todayInSeoul())
        filter.update { it.copy(dateFrom = range?.first, dateTo = range?.second) }
    }

    /** 시작일. 종료일이 비어 있으면 오늘로 채운다. */
    fun onDateFromPicked(date: LocalDate) {
        filter.update { it.copy(dateFrom = date, dateTo = it.dateTo ?: clock.todayInSeoul()) }
    }

    /** 종료일. 시작일이 비어 있으면 종료일 − 29일로 채운다. */
    fun onDateToPicked(date: LocalDate) {
        filter.update {
            it.copy(
                dateFrom = it.dateFrom ?: date.minus(DatePeriod(days = DEFAULT_RANGE_DAYS - 1)),
                dateTo = date
            )
        }
    }

    /** 시도. 바꾸면 시군구·보호소(코드·이름)를 비운다. null = 전체. */
    fun onSidoSelected(sido: Sido?) {
        if (sido?.code == filter.value.sidoCode) return
        filter.update {
            it.copy(
                sidoCode = sido?.code,
                sidoName = sido?.name,
                sigunguCode = null,
                sigunguName = null,
                shelterRegNo = null,
                shelterName = null
            )
        }
    }

    /** 시군구. 바꾸면 보호소를 비운다. */
    fun onSigunguSelected(sigungu: Sigungu?) {
        if (sigungu?.code == filter.value.sigunguCode) return
        filter.update {
            it.copy(
                sigunguCode = sigungu?.code,
                sigunguName = sigungu?.name,
                shelterRegNo = null,
                shelterName = null
            )
        }
    }

    fun onShelterSelected(shelter: ShelterCode?) {
        filter.update { it.copy(shelterRegNo = shelter?.regNo, shelterName = shelter?.name) }
    }

    fun onStateSelected(state: NoticeState) {
        filter.update { it.copy(state = state) }
    }

    fun onSexSelected(sex: Sex?) {
        filter.update { it.copy(sex = sex) }
    }

    fun onNeuteredSelected(neutered: Neutered?) {
        filter.update { it.copy(neutered = neutered) }
    }

    /** 코드표 다시 받기(그 표만). */
    fun onRetryCodes(table: CodeTable) {
        viewModelScope.launch { codeTables.refresh(table) }
    }

    /** 전부 기본값. */
    fun onReset() {
        filter.value = SearchFilter()
        breedQuery.value = ""
    }

    /**
     * 검색을 시작한다(앱 범위, 끝까지 간다 — R1). 화면은 이것을 부른 뒤 바로 닫는다.
     * `UNDISPATCHED`: 닫히며 ViewModel 범위가 취소되기 전에 검색 작업이 시작되게.
     */
    fun onApply() {
        val current = filter.value
        if (dateErrorOf(current.dateFrom, current.dateTo) != null) return
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { animals.search(current) }
    }

    private fun start(initial: SearchFilter) {
        filter.value = initial
        breedQuery.value = initial.kindName.orEmpty()
    }
}

/** 날짜 검증: 종료 < 시작 → `filter_date_invalid`, 범위 > 1년 → `filter_date_range_too_long`. */
fun dateErrorOf(from: LocalDate?, to: LocalDate?): UiText? = when {
    from == null || to == null -> null
    to < from -> uiText(Res.string.filter_date_invalid)
    from.daysUntil(to) > MAX_DATE_RANGE_DAYS -> uiText(Res.string.filter_date_range_too_long)
    else -> null
}
