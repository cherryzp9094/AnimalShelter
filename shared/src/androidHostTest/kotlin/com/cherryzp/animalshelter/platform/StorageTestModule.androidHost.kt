package com.cherryzp.animalshelter.platform

import kotlin.random.Random
import okio.FileSystem
import okio.Path
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

// 호스트 테스트(JVM)에는 Context가 없으므로 filesDir 대신 임시 디렉터리를 준다.
actual fun storageTestModule(): Module = module {
    single<Path>(named(DATASTORE_DIR)) {
        FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "animalshelter-test-${Random.nextLong().toULong()}"
    }
}
