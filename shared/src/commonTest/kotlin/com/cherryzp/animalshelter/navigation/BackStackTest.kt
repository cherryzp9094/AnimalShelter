package com.cherryzp.animalshelter.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** back stack 규칙(03 0.3, 브리핑 P11, 중간 검토 F5). */
class BackStackTest {
    private val detailA = AnimalDetailKey("A", "20261103")
    private val detailB = AnimalDetailKey("B", "20261102")
    private val shelterX = ShelterInfoKey("X", fallbackName = "보호소 X")

    @Test
    fun `세 칸에서 다른 동물을 열면 앞 동물의 보호소도 뺀다`() {
        val stack = mutableListOf<NavKey>(AnimalListKey, detailA, shelterX)
        stack.addDetail(detailB)
        assertEquals(listOf<NavKey>(AnimalListKey, detailB), stack)
    }

    @Test
    fun `북마크 목록 위의 상세를 바꾸면 북마크 목록은 둔다`() {
        val stack = mutableListOf<NavKey>(AnimalListKey, BookmarkListKey, detailA)
        stack.addDetail(detailB)
        assertEquals(listOf<NavKey>(AnimalListKey, BookmarkListKey, detailB), stack)
    }

    @Test
    fun `상세가 없으면 더하기만`() {
        val stack = mutableListOf<NavKey>(AnimalListKey)
        stack.addDetail(detailA)
        assertEquals(listOf<NavKey>(AnimalListKey, detailA), stack)
    }

    @Test
    fun `목록까지 되돌리기`() {
        val stack = mutableListOf<NavKey>(AnimalListKey, detailA, shelterX)
        stack.popToList()
        assertEquals(listOf<NavKey>(AnimalListKey), stack)

        val noList = mutableListOf<NavKey>(detailA)
        noList.popToList()
        assertEquals(listOf<NavKey>(AnimalListKey), noList)
    }

    @Test
    fun `목록 옆에 열린 상세 번호는 목록 뒤의 마지막 상세`() {
        assertEquals("A", listOf<NavKey>(AnimalListKey, detailA).openedDetailAfter(AnimalListKey))
        assertEquals(
            "A",
            listOf<NavKey>(AnimalListKey, detailA, shelterX).openedDetailAfter(AnimalListKey)
        )
        assertNull(listOf<NavKey>(AnimalListKey).openedDetailAfter(AnimalListKey))
    }

    @Test
    fun `상세 뒤에 push한 북마크 목록 옆은 자리표시`() {
        val stack = listOf<NavKey>(AnimalListKey, detailA, BookmarkListKey)
        assertNull(stack.openedDetailAfter(BookmarkListKey))
        assertEquals("A", stack.openedDetailAfter(AnimalListKey))
        assertEquals(
            "B",
            listOf<NavKey>(
                AnimalListKey,
                BookmarkListKey,
                detailB
            ).openedDetailAfter(BookmarkListKey)
        )
        assertNull(listOf<NavKey>(AnimalListKey).openedDetailAfter(BookmarkListKey))
    }
}
