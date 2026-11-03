package com.cherryzp.animalshelter.di

import com.cherryzp.animalshelter.platform.DATASTORE_DIR
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // DataStore 파일 위치. createPreferencesStorage·createJsonStorage가 읽는다.
    single(named(DATASTORE_DIR)) { androidContext().filesDir.resolve("datastore").toOkioPath() }
}
