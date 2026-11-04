package com.cherryzp.animalshelter.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** 화면 하나 = 키 하나. 근거: docs/design/03-screens.md 0.3. 화면 구현은 Phase 3. */
@Serializable
sealed interface AppKey : NavKey

/** 시작 화면: 공고 목록. */
@Serializable
data object AnimalListKey : AppKey

@Serializable
data object FilterKey : AppKey

@Serializable
data class AnimalDetailKey(
    val desertionNo: String,
    /** YYYYMMDD. 재조회 창(02-data 6절). 딥링크에서는 null */
    val happenDate: String? = null,
    val fromBookmarks: Boolean = false,
) : AppKey

/**
 * 사진 뷰어(브리핑 B1). `photos`는 정규화한 **원본** URL(화면용 URL은 뷰어 ViewModel이 만든다).
 * 비어 있으면(Phase 4 주소 진입) 뷰어가 `getAnimal(desertionNo, null)`로 찾는다. `title`은 카드 제목(접근성 문구).
 */
@Serializable
data class PhotoViewerKey(
    val desertionNo: String,
    val index: Int = 0,
    val photos: List<String> = emptyList(),
    val title: String? = null,
) : AppKey

@Serializable
data object BookmarkListKey : AppKey

/**
 * 보호소 정보(브리핑 B1). `fallback*`은 목록이 준 요약 — 보호소 정보가 없을 때 줄인 화면에 쓴다.
 */
@Serializable
data class ShelterInfoKey(
    val regNo: String,
    val fallbackName: String? = null,
    val fallbackTel: String? = null,
    val fallbackAddress: String? = null,
) : AppKey

@Serializable
data object SettingsKey : AppKey

/**
 * 키 직렬화 모듈 — back stack 저장(`AppNavSavedStateConfiguration`)과 웹 기록의 `state`(W3)가 같이 쓴다.
 * 비JVM(wasmJs)은 리플렉션 직렬화가 없어 키를 전부 등록한다. 키를 새로 만들면 여기에도 넣는다.
 */
internal val AppNavSerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(AnimalListKey::class)
        subclass(FilterKey::class)
        subclass(AnimalDetailKey::class)
        subclass(PhotoViewerKey::class)
        subclass(BookmarkListKey::class)
        subclass(ShelterInfoKey::class)
        subclass(SettingsKey::class)
    }
}
