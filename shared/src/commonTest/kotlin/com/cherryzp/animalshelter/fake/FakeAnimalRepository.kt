package com.cherryzp.animalshelter.fake

import com.cherryzp.animalshelter.data.api.ApiResult
import com.cherryzp.animalshelter.data.repository.AnimalRepository
import com.cherryzp.animalshelter.data.repository.SearchResults
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.SortOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

/**
 * 가짜 목록 Repository. [results]를 직접 바꾸고 호출을 기록한다.
 * 동작이 필요하면 `onSearch` 등에 넣는다(기본: 조건·정렬만 반영).
 */
class FakeAnimalRepository(initial: SearchResults = SearchResults()) : AnimalRepository {
    val results = MutableStateFlow(initial)

    val searchCalls = mutableListOf<SearchFilter>()
    val sortCalls = mutableListOf<SortOrder>()
    var loadMoreCalls = 0
        private set
    var refreshCalls = 0
        private set
    val getAnimalCalls = mutableListOf<Pair<String, LocalDate?>>()

    var onSearch: suspend (SearchFilter) -> Unit = { filter ->
        results.value = results.value.copy(filter = filter, error = null)
    }
    var onSetSort: suspend (SortOrder) -> Unit = { order ->
        results.value = results.value.copy(sort = order, error = null)
    }
    var onLoadMore: suspend () -> Unit = {}
    var onRefresh: suspend () -> Unit = {}

    /** `getAnimal`의 응답. 기본: 지금 결과에서 찾고 없으면 `Success(null)`. */
    var animalResult: (String) -> ApiResult<Animal?> = { no ->
        ApiResult.Success(results.value.items.firstOrNull { it.desertionNo == no })
    }

    /** 있으면 `getAnimal`이 이것이 끝날 때까지 기다린다(조회 중 상태를 보려고). */
    var getAnimalGate: CompletableDeferred<Unit>? = null

    override fun getSearchResultsStream(): StateFlow<SearchResults> = results

    override suspend fun search(filter: SearchFilter) {
        searchCalls += filter
        onSearch(filter)
    }

    override suspend fun setSort(order: SortOrder) {
        sortCalls += order
        onSetSort(order)
    }

    override suspend fun loadMore() {
        loadMoreCalls++
        onLoadMore()
    }

    override suspend fun refresh() {
        refreshCalls++
        onRefresh()
    }

    override suspend fun getAnimal(
        desertionNo: String,
        happenDate: LocalDate?,
    ): ApiResult<Animal?> {
        getAnimalCalls += desertionNo to happenDate
        getAnimalGate?.await()
        return animalResult(desertionNo)
    }
}
