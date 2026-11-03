package com.cherryzp.animalshelter.platform

import android.os.Build

actual class PlatformInfo actual constructor(actual val versionName: String) {
    actual val name: String = "Android ${Build.VERSION.RELEASE}"
}
