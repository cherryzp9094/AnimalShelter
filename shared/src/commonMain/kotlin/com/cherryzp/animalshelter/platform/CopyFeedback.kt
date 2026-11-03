package com.cherryzp.animalshelter.platform

/**
 * 복사 결과(브리핑 B3). Android 13 이상은 시스템이 복사 확인을 보여 주므로 앱은 스낵바를 내지 않는다
 * (Android 공식 권고). 12 이하와 웹은 앱이 `address_copied`를 보인다.
 */
enum class CopyFeedback { SHOWN_BY_SYSTEM, NEEDS_APP_MESSAGE, FAILED }
