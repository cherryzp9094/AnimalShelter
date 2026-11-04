package com.cherryzp.animalshelter.ui.common

/**
 * UI 흐름 테스트가 로케일과 무관하게 찾는 태그(브리핑 C6). 화면 동작에는 영향이 없다.
 */
object TestTags {
    const val OPEN_FILTER = "open_filter"
    const val OPEN_BOOKMARKS = "open_bookmarks"
    const val DETAIL_BOOKMARK = "detail_bookmark"
    const val FILTER_APPLY = "filter_apply"
    const val SUMMARY_CHIPS = "summary_chips"

    fun card(desertionNo: String): String = "card:$desertionNo"

    fun cardBookmark(desertionNo: String): String = "card_bookmark:$desertionNo"

    /** 필터 세그먼트 하나: 묶음 이름 + 값(`null`이면 "all"). */
    fun choice(group: String, value: Any?): String = "choice:$group:${value ?: "all"}"
}
