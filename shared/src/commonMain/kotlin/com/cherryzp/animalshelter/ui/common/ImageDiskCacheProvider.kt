package com.cherryzp.animalshelter.ui.common

import coil3.disk.DiskCache

/**
 * Coil 디스크 캐시 공급자(브리핑 A5). 플랫폼마다 `platformModule()`이 Koin으로 준다 —
 * Android는 `cacheDir/image_cache` 200MB, 웹은 null(메모리만). expect/actual을 늘리지 않으려고 인터페이스로 둔다.
 */
fun interface ImageDiskCacheProvider {
    fun diskCache(): DiskCache?
}
