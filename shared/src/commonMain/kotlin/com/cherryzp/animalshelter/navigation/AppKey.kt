package com.cherryzp.animalshelter.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

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

@Serializable
data class PhotoViewerKey(val desertionNo: String, val index: Int = 0) : AppKey

@Serializable
data object BookmarkListKey : AppKey

@Serializable
data class ShelterInfoKey(val regNo: String, val fallbackName: String? = null) : AppKey

@Serializable
data object SettingsKey : AppKey
