package com.cherryzp.animalshelter.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.koin.mp.KoinPlatform

actual object ExternalActions {
    // Activity 밖에서 여므로 애플리케이션 Context + NEW_TASK를 쓴다.
    private val context: Context get() = KoinPlatform.getKoin().get()

    actual fun dial(tel: String) {
        start(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")))
    }

    actual fun openUrl(url: String) {
        start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun start(intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // 처리할 앱이 없으면 아무것도 하지 않는다(Phase 3에서 화면 안내로 바꾼다).
        }
    }
}
