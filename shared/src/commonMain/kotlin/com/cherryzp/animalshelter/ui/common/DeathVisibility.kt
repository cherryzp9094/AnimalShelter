package com.cherryzp.animalshelter.ui.common

import com.cherryzp.animalshelter.data.repository.AnimalRepository
import com.cherryzp.animalshelter.data.repository.BookmarkRepository
import com.cherryzp.animalshelter.data.repository.SettingsRepository
import com.cherryzp.animalshelter.model.showsEndedByDeath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * 사망 종료 한 마리를 보일지(5d 1.7) — 북마크돼 있거나 필터가 켜져 있으면(`showsEndedByDeath`) 보인다. 상세·사진
 * 뷰어가 사망 종료 값을 처음 얻을 때 한 번 부른다.
 *
 * 필터 값은 지금 목록 결과를 받은 적이 있으면(`fetchedAt != null`) 그 조건, 아니면 저장된 마지막 조건(없으면 끔) —
 * 링크로 바로 연 앱처럼 목록이 아직 없을 때를 위한 것이다. 읽기가 실패하면 그 쪽은 아니라고 본다(숨김 쪽).
 */
internal suspend fun showsEndedByDeath(
    desertionNo: String,
    animals: AnimalRepository,
    bookmarks: BookmarkRepository,
    settings: SettingsRepository,
): Boolean {
    if (readOrNull { bookmarks.isBookmarkedStream(desertionNo).first() } == true) return true
    val current = animals.getSearchResultsStream().value
    val filter = if (current.fetchedAt != null) {
        current.filter
    } else {
        readOrNull { settings.getSettingsStream().first().lastFilter }
    }
    return filter?.showsEndedByDeath == true
}

private suspend fun <T> readOrNull(read: suspend () -> T): T? = try {
    read()
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}
