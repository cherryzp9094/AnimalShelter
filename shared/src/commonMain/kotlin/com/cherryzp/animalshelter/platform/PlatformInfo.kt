package com.cherryzp.animalshelter.platform

/** 실행 중인 플랫폼 정보(설정 화면·공유 문구용). */
expect class PlatformInfo(versionName: String) {
    /** 예: "Android 17", "Web" */
    val name: String

    /** 앱 버전 이름(예: 2.0.0) */
    val versionName: String
}
