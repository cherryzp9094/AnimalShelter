package com.cherryzp.animalshelter.navigation

import androidx.navigation3.runtime.NavKey

/**
 * 상세 열기(03 0.3, 브리핑 P11): 기존 [AnimalDetailKey]를 모두 빼고 더한다 — 두 칸에서 상세가 쌓이지 않게
 * (Nav3 공식 목록-상세 예제의 `addDetail`).
 */
fun MutableList<NavKey>.addDetail(key: AnimalDetailKey) {
    removeAll { it is AnimalDetailKey }
    add(key)
}
