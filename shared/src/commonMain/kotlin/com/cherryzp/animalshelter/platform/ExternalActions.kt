package com.cherryzp.animalshelter.platform

/** 앱 밖으로 나가는 동작(전화 걸기·링크 열기). Phase 1은 구현만, 호출은 Phase 3. */
expect object ExternalActions {
    /** 전화 앱을 번호가 채워진 상태로 연다(바로 걸지 않는다). */
    fun dial(tel: String)

    /** 브라우저(웹은 새 탭)로 연다. */
    fun openUrl(url: String)
}
