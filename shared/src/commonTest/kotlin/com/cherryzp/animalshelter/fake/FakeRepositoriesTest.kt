package com.cherryzp.animalshelter.fake

import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.model.AnimalStatus
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.SortOrder
import com.cherryzp.animalshelter.model.statusOn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

/** Fake가 Phase 3 ViewModel 테스트에서 쓸 만큼 실제 규칙을 따르는지. */
class FakeRepositoriesTest {
    @Test
    fun `FakeAnimalRepository는 호출을 기록하고 조건·정렬을 반영한다`() = runTest {
        val repo = FakeAnimalRepository()

        repo.search(SearchFilter(sidoCode = "6110000"))
        repo.setSort(SortOrder.CLOSING_SOON)
        repo.loadMore()

        assertEquals(listOf(SearchFilter(sidoCode = "6110000")), repo.searchCalls)
        assertEquals(SortOrder.CLOSING_SOON, repo.getSearchResultsStream().value.sort)
        assertEquals(1, repo.loadMoreCalls)
    }

    @Test
    fun `FakeBookmarkRepository는 유일·역순·happenDate 보존`() = runTest {
        val repo = FakeBookmarkRepository()

        repo.add(testAnimal("A"))
        repo.add(testAnimal("B"))
        repo.add(testAnimal("A", kindName = "갱신"))
        repo.updateSnapshot(testAnimal("B", happenDate = null))

        val list = repo.getBookmarksStream().first()
        assertEquals(listOf("B", "A"), list.map { it.animal.desertionNo })
        assertEquals("갱신", list[1].animal.kindName)
        assertEquals(LocalDate(2026, 11, 3), list[0].animal.happenDate)
        assertTrue(repo.isBookmarkedStream("A").first())
        repo.remove("A")
        assertFalse(repo.isBookmarkedStream("A").first())
    }

    @Test
    fun `FakeSettingsRepository는 Direct 덮어쓰기를 지운다`() = runTest {
        val repo = FakeSettingsRepository()

        repo.setEndpointOverride(ApiEndpoint.Fixture)
        assertEquals(ApiEndpoint.Fixture, repo.getSettingsStream().first().endpointOverride)
        repo.setEndpointOverride(ApiEndpoint.Direct("k"))
        assertNull(repo.getSettingsStream().first().endpointOverride)
    }

    @Test
    fun `testAnimal 기본값은 2026-11-04에 공고중 D-9`() {
        assertEquals(AnimalStatus.Noticing(9), testAnimal().statusOn(LocalDate(2026, 11, 4)))
        assertEquals(5, testAnimals(5).map { it.desertionNo }.toSet().size)
    }

    @Test
    fun `Fake 코드표·보호소는 넣은 상태를 내보낸다`() = runTest {
        val codes = FakeCodeTableRepository()
        val shelters = FakeShelterRepository()

        assertEquals(loading(), codes.getSidoStream().first())
        shelters.state("350000202000001").value = loaded(testShelterInfo())
        assertEquals(
            "제2동물보호센터",
            shelters.getShelterInfoStream("350000202000001").first().data?.name
        )
    }
}
