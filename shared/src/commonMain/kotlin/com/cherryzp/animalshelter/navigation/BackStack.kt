package com.cherryzp.animalshelter.navigation

import androidx.navigation3.runtime.NavKey

/**
 * 상세 열기(03 0.3, 브리핑 P11): 기존 [AnimalDetailKey]와 [ShelterInfoKey]를 모두 빼고 더한다 — 두 칸에서 상세가
 * 쌓이지 않게(Nav3 공식 목록-상세 예제의 `addDetail`), 세 칸에서 다른 동물을 열면 앞 동물의 보호소 칸이 남지 않게
 * (중간 검토 F5 — 남으면 상세 B에서 뒤로 갈 때 A의 보호소로 간다).
 */
fun MutableList<NavKey>.addDetail(key: AnimalDetailKey) {
    removeAll { it is AnimalDetailKey || it is ShelterInfoKey }
    add(key)
}

/** 목록까지 되돌린다: 맨 아래 [AnimalListKey]보다 뒤의 키를 모두 뺀다(목록이 없으면 목록만 남긴다). */
fun MutableList<NavKey>.popToList() {
    val index = indexOfFirst { it is AnimalListKey }
    if (index < 0) {
        clear()
        add(AnimalListKey)
        return
    }
    while (size > index + 1) removeAt(lastIndex)
}
