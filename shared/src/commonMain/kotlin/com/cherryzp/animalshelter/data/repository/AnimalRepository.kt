package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.ApiError
import com.cherryzp.animalshelter.data.api.ApiResult
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.SortOrder
import kotlin.time.Instant
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

/**
 * 목록 검색 결과(메모리 SSOT, 02-data 9절·브리핑 4.7).
 * - `filter`는 사용자가 고른 그대로다. 마감 임박순은 조회에만 `state = NOTICE`를 쓴다(D13).
 * - `error`는 다음 사용자 동작(`search`·`setSort`·`refresh`·`loadMore`)이 시작될 때 지워진다.
 */
data class SearchResults(
    val filter: SearchFilter = SearchFilter(),
    val sort: SortOrder = SortOrder.LATEST,
    val items: List<Animal> = emptyList(),
    /** 아직 모르면 null. 마감 임박순은 모은 건수. */
    val totalCount: Int? = null,
    /** 마지막으로 받은 페이지(LATEST). 마감 임박순은 0. */
    val pageNo: Int = 0,
    /** 다음 페이지가 있는지(D9). 마감 임박순은 늘 false. */
    val hasMore: Boolean = false,
    val loading: LoadState = LoadState.IDLE,
    val error: ApiError? = null,
    /** 지금 목록(첫 페이지)을 받은 시각. 상세 재조회 생략(10분)에 쓴다. */
    val fetchedAt: Instant? = null,
)

/** 로딩 종류. INITIAL = 빈 목록에서 첫 로딩, MORE = 다음 페이지, REFRESH = 기존 목록을 둔 채 다시 받기. */
enum class LoadState { IDLE, INITIAL, MORE, REFRESH }

/**
 * 구조동물 목록·상세(02-data 9절). 로딩은 Repository의 앱 범위에서 돈다(D8) — 부른 쪽 범위가 취소돼도 끝까지 간다.
 * `search`·`setSort`·`refresh`는 진행 중인 로딩을 취소하고 새로 시작해 끝날 때까지 기다린다.
 */
interface AnimalRepository {
    fun getSearchResultsStream(): StateFlow<SearchResults>

    /** 조건 바꾸기 = 결과 리셋 + 1페이지(LATEST) 또는 전체 수집(CLOSING_SOON). */
    suspend fun search(filter: SearchFilter)

    /**
     * 정렬 바꾸기. 다 받은 뒤 바꾸고, 받는 동안은 기존 목록을 둔 채 `loading = REFRESH`.
     * 마감 임박순은 같은 조건이면 10분 메모리 캐시. 5,000건을 넘으면 기존 목록·LATEST를 그대로 두고
     * `error = TooManyResults`.
     */
    suspend fun setSort(order: SortOrder)

    /** LATEST에서 `hasMore`일 때만 다음 페이지. 로딩 중이면 무시한다. */
    suspend fun loadMore()

    /** 지금 조건·정렬로 다시 받는다(마감 임박순 캐시는 무시). */
    suspend fun refresh()

    /**
     * 한 마리. 지금 결과를 받은 지 10분 안이고 그 안에 있으면 호출 없이 그것, 아니면 API(재조회 창 규칙).
     * 찾지 못하면 `Success(null)`.
     */
    suspend fun getAnimal(desertionNo: String, happenDate: LocalDate?): ApiResult<Animal?>
}
