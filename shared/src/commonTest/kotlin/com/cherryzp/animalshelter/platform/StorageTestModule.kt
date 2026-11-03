package com.cherryzp.animalshelter.platform

import org.koin.core.module.Module

/** 저장소 팩토리 테스트용 플랫폼 바인딩(Android 호스트: 임시 디렉터리, 웹: 없음). */
expect fun storageTestModule(): Module
