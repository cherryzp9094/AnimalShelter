package com.cherryzp.animalshelter.platform

/**
 * 앱 밖으로 나가는 동작(브리핑 B3). 화면(Composable)이 부르고 결과를 ViewModel에 알린다 — ViewModel은 부르지 않는다.
 * `false`·[CopyFeedback.FAILED]면 화면이 `external_open_failed`를 보인다(R7).
 */
expect object ExternalActions {
    /** 전화 앱을 번호가 채워진 상태로 연다(바로 걸지 않는다). 열 앱이 없으면 false. */
    fun dial(tel: String): Boolean

    /** 브라우저(웹은 새 탭)로 연다. 열 수 없으면 false. */
    fun openUrl(url: String): Boolean

    /** 지도 앱(웹은 Google 지도 새 탭)으로 좌표를 연다. 열 수 없으면 false. */
    fun openMap(lat: Double, lng: Double, label: String): Boolean

    /** 글자를 클립보드에 복사한다. 앱이 확인 문구를 내야 하는지를 돌려준다. */
    fun copyText(text: String): CopyFeedback
}
