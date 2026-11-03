package com.cherryzp.animalshelter.platform

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import org.koin.mp.KoinPlatform

actual object ExternalActions {
    // Activity 밖에서 여므로 애플리케이션 Context + NEW_TASK를 쓴다.
    private val context: Context get() = KoinPlatform.getKoin().get()

    actual fun dial(tel: String): Boolean = start(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")))

    actual fun openUrl(url: String): Boolean = start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    /** `geo:lat,lng?q=lat,lng(label)` — 지도 앱이 핀과 이름을 함께 보인다. */
    actual fun openMap(lat: Double, lng: Double, label: String): Boolean = start(
        Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})"))
    )

    actual fun copyText(text: String): CopyFeedback {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
            ?: return CopyFeedback.FAILED
        return try {
            clipboard.setPrimaryClip(ClipData.newPlainText(null, text))
            // Android 13(API 33)부터 시스템이 복사 확인을 보여 준다 — 앱이 겹쳐 알리지 않는다
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                CopyFeedback.SHOWN_BY_SYSTEM
            } else {
                CopyFeedback.NEEDS_APP_MESSAGE
            }
        } catch (_: RuntimeException) {
            CopyFeedback.FAILED
        }
    }

    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false // 처리할 앱이 없다 — 화면이 안내한다(R7)
    }
}
