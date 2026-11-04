package com.cherryzp.animalshelter.ui.licenses

import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.licenses_web_components
import com.cherryzp.animalshelter.ui.common.UiText
import com.cherryzp.animalshelter.ui.common.uiText

// 오픈소스 고지 목록(5a O1). 고유 이름·저작권자·라이선스 이름·주소·전문은 번역하지 않는 자료라 코드 상수와
// `composeResources/files/licenses/`의 파일에 둔다(P12의 예외 — 화면 문구만 문자열 리소스). 버전은 적지 않는다
// (버전은 `gradle/libs.versions.toml`). 근거: 2026-11-04 조사(`--offline` 의존성 트리 → POM `<licenses>`·
// `<organization>`·`<developers>`, APK·웹 번들·skiko.wasm 안의 고지·표시 문자열). 저작권자 칸(5a-fixes F2)은 그
// 프로젝트가 스스로 적은 저작권 줄의 이름(LICENSE·NOTICE·소스 머리 — 이 맥의 사본으로 확인한 것). 확인하지 못했으면
// POM의 조직·팀 이름(… Team·… Project·… Contributors·회사 이름), POM `<developers>`의 개인 이름은 저작권 줄로
// 확인될 때만 — 아니면 비운다(고지 화면에 저작권자 이름은 의무가 아니다).

/** 그 구성 요소가 든 앱. */
internal enum class NoticePlatform { BOTH, ANDROID, WEB }

/**
 * 고지 줄 하나.
 * @property holder 저작권자(조사 표에 있을 때만)
 * @property license 라이선스 이름 — 묶음 제목이 라이선스가 아닐 때만(웹 구성 요소 묶음)
 */
internal data class NoticeEntry(
    val name: String,
    val url: String,
    val holder: String? = null,
    val license: String? = null,
    val platform: NoticePlatform = NoticePlatform.BOTH,
)

/**
 * 고지 묶음. [textFile]은 `Res.readBytes`로 읽는 전문 경로(없으면 [licenseUrl]로 갈음 — MPL 2.0 3.2(a)).
 * [key]는 펼침 상태 저장과 테스트 태그에 쓴다.
 */
internal data class NoticeGroup(
    val key: String,
    val title: UiText,
    val textFile: String?,
    val entries: List<NoticeEntry>,
    val licenseUrl: String? = null,
)

/** 전문 파일 폴더(`composeResources/files/`). */
internal const val LICENSE_FILES_DIR = "files/licenses/"

internal const val APACHE_TEXT = LICENSE_FILES_DIR + "Apache-2.0.txt"
internal const val OFL_TEXT = LICENSE_FILES_DIR + "Pretendard-OFL.txt"
internal const val WEB_NOTICES_TEXT = LICENSE_FILES_DIR + "web-notices.txt"
internal const val ANDROID_NOTICES_TEXT = LICENSE_FILES_DIR + "android-notices.txt"

/** 전문 파일 전부(테스트가 묶음의 경로를 이것과 맞춘다). */
internal val LicenseTextFiles =
    listOf(APACHE_TEXT, OFL_TEXT, WEB_NOTICES_TEXT, ANDROID_NOTICES_TEXT)

private const val AOSP = "The Android Open Source Project"
private const val CMP_TEAM = "Compose Multiplatform Team"
private const val SQUARE = "Square, Inc."

/** 화면에 보이는 차례(브리핑 O1 1~4, 그 뒤 조사에서 나온 Apache가 아닌 Android 구성 요소 둘). */
internal val OpenSourceNotices: List<NoticeGroup> = listOf(
    NoticeGroup(
        key = "apache",
        title = UiText.Raw("Apache License 2.0"),
        textFile = APACHE_TEXT,
        entries = listOf(
            NoticeEntry(
                "Material Symbols",
                "https://github.com/google/material-design-icons",
                holder = "Google"
            ),
            NoticeEntry("Kotlin · kotlinx", "https://kotlinlang.org/", holder = "JetBrains Team"),
            NoticeEntry(
                "Compose Multiplatform",
                "https://github.com/JetBrains/compose-multiplatform",
                holder = CMP_TEAM
            ),
            NoticeEntry(
                "AndroidX · Jetpack Compose",
                "https://developer.android.com/jetpack/androidx",
                holder = AOSP
            ),
            NoticeEntry("Ktor", "https://github.com/ktorio/ktor", holder = "JetBrains Team"),
            NoticeEntry(
                "OkHttp",
                "https://github.com/square/okhttp",
                holder = SQUARE,
                platform = NoticePlatform.ANDROID
            ),
            NoticeEntry("Okio", "https://github.com/square/okio", holder = SQUARE),
            NoticeEntry("Koin", "https://insert-koin.io/"),
            NoticeEntry("Coil", "https://github.com/coil-kt/coil", holder = "Coil Contributors"),
            NoticeEntry("Stately", "https://github.com/touchlab/Stately"),
            NoticeEntry(
                "kaml",
                "https://github.com/charleskorn/kaml",
                platform = NoticePlatform.ANDROID
            ),
            NoticeEntry(
                "snakeyaml-engine-kmp",
                "https://github.com/krzema12/snakeyaml-engine-kmp",
                platform = NoticePlatform.ANDROID
            ),
            NoticeEntry(
                "Accompanist",
                "https://github.com/google/accompanist",
                holder = "Google",
                platform = NoticePlatform.ANDROID
            ),
            NoticeEntry(
                "Guava ListenableFuture",
                "https://github.com/google/guava",
                holder = "The Guava Authors",
                platform = NoticePlatform.ANDROID
            ),
            NoticeEntry(
                "JSpecify",
                "https://jspecify.dev/",
                holder = "The JSpecify Authors",
                platform = NoticePlatform.ANDROID
            ),
            NoticeEntry(
                "urlencoder",
                "https://github.com/ethauvin/urlencoder",
                platform = NoticePlatform.ANDROID
            ),
            NoticeEntry(
                "JetBrains Java Annotations",
                "https://github.com/JetBrains/java-annotations",
                holder = "JetBrains s.r.o.",
                platform = NoticePlatform.ANDROID
            ),
            NoticeEntry(
                "Skiko",
                "https://github.com/JetBrains/skiko",
                holder = CMP_TEAM,
                platform = NoticePlatform.WEB
            ),
            NoticeEntry(
                "Wuffs (Skiko)",
                "https://github.com/google/wuffs",
                platform = NoticePlatform.WEB
            )
        )
    ),
    NoticeGroup(
        key = "ofl",
        title = UiText.Raw("SIL Open Font License 1.1"),
        textFile = OFL_TEXT,
        entries = listOf(
            NoticeEntry(
                "Pretendard",
                "https://github.com/orioncactus/pretendard",
                holder = "Kil Hyung-jin (Reserved Font Name \"Pretendard\")"
            )
        )
    ),
    NoticeGroup(
        key = "web",
        title = uiText(Res.string.licenses_web_components),
        textFile = WEB_NOTICES_TEXT,
        entries = listOf(
            NoticeEntry("js-joda", "https://js-joda.github.io/js-joda/", license = "BSD-3-Clause"),
            NoticeEntry("Skia", "https://skia.org/", license = "BSD-3-Clause"),
            NoticeEntry("HarfBuzz", "https://harfbuzz.github.io/", license = "MIT"),
            NoticeEntry("ICU", "https://icu.unicode.org/", license = "Unicode-3.0"),
            NoticeEntry("FreeType", "https://freetype.org/", license = "FTL"),
            NoticeEntry(
                "libpng",
                "https://www.libpng.org/pub/png/libpng.html",
                license = "libpng-2.0"
            ),
            NoticeEntry(
                "libjpeg-turbo",
                "https://libjpeg-turbo.org/",
                license = "IJG · BSD-3-Clause · Zlib"
            ),
            NoticeEntry(
                "libwebp",
                "https://chromium.googlesource.com/webm/libwebp",
                license = "BSD-3-Clause"
            ),
            NoticeEntry("expat", "https://libexpat.github.io/", license = "MIT"),
            NoticeEntry("zlib", "https://zlib.net/", license = "Zlib"),
            NoticeEntry("Brotli", "https://github.com/google/brotli", license = "MIT")
        )
    ),
    NoticeGroup(
        key = "mpl",
        title = UiText.Raw("Mozilla Public License 2.0"),
        textFile = null,
        licenseUrl = "https://mozilla.org/MPL/2.0/",
        entries = listOf(
            NoticeEntry(
                "Public Suffix List (OkHttp)",
                "https://publicsuffix.org/",
                platform = NoticePlatform.ANDROID
            )
        )
    ),
    NoticeGroup(
        key = "mit",
        title = UiText.Raw("MIT License"),
        textFile = ANDROID_NOTICES_TEXT,
        entries = listOf(
            NoticeEntry(
                "SLF4J API",
                "https://www.slf4j.org/",
                holder = "QOS.ch",
                platform = NoticePlatform.ANDROID
            )
        )
    ),
    NoticeGroup(
        key = "bsd3",
        title = UiText.Raw("BSD 3-Clause License"),
        textFile = ANDROID_NOTICES_TEXT,
        entries = listOf(
            NoticeEntry(
                "Protocol Buffers (DataStore)",
                "https://github.com/protocolbuffers/protobuf",
                holder = "Google Inc.",
                platform = NoticePlatform.ANDROID
            )
        )
    )
)
