import java.util.Properties

plugins {
    // AGP 9 내장 Kotlin을 쓴다. org.jetbrains.kotlin.android는 적용하지 않는다.
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

// local.properties(git 제외)를 읽는다. 값은 빌드 로그에 찍지 않는다.
val localProperties: Properties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use(::load)
}
// -Pfixture=true면 키가 있어도 빈 문자열을 넣는다 → 앱이 Fixture 엔드포인트로 뜬다(캡처·수동 확인용).
val forceFixture: Boolean = providers.gradleProperty("fixture").orNull == "true"
val serviceKey: String =
    if (forceFixture) "" else localProperties.getProperty("SERVICE_KEY").orEmpty()

// 에뮬레이터에서 호스트의 중계 서버(wrangler dev)에 닿는 주소가 기본값이다.
val relayBaseUrl: String = localProperties.getProperty("RELAY_BASE_URL") ?: "http://10.0.2.2:8788"

// 공유 링크 호스트(gradle.properties의 shareHost, -PshareHost=…). 비면 App Links 호스트는 자리값
// animalshelter.invalid(RFC 2606 — 검증이 늘 실패해 다른 링크를 가로채지 않는다)이고 공유 주소는 빈 값(공유 숨김).
val shareHostProperty: String = providers.gradleProperty("shareHost").orNull.orEmpty().trim()

// 4a 검토 F6: 호스트 이름만 받는다(소문자, 점 하나 이상, 스킴·포트·경로 없음). App Links 호스트는 소문자여야 하고,
// 스킴을 넣으면 공유 주소가 https://https://…가 된다 — 설정 단계에서 멈춘다.
val shareHostPattern =
    Regex("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+$")
if (shareHostProperty.isNotEmpty() && !shareHostPattern.matches(shareHostProperty)) {
    throw GradleException(
        "shareHost는 호스트 이름만 적는다(소문자, 예 animal.example.com): 받은 값 '$shareHostProperty'"
    )
}
val appLinkHost: String = shareHostProperty.ifEmpty { "animalshelter.invalid" }
val shareBaseUrl: String = if (shareHostProperty.isEmpty()) "" else "https://$shareHostProperty"

// 릴리스 서명. 로컬 전용 signing/keystore.properties(git 제외)가 있을 때만 붙는다. 없으면 서명 없는 release.
val releaseSigning: Properties? = rootDir.resolve("signing/keystore.properties")
    .takeIf { it.isFile }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } }

android {
    namespace = "com.cherryzp.animalshelter"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.cherryzp.animalshelter"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()

        buildConfigField("String", "SERVICE_KEY", "\"$serviceKey\"")
        buildConfigField("String", "RELAY_BASE_URL", "\"$relayBaseUrl\"")
        buildConfigField("String", "SHARE_HOST", "\"$appLinkHost\"")
        buildConfigField("String", "SHARE_BASE_URL", "\"$shareBaseUrl\"")
        manifestPlaceholders["shareHost"] = appLinkHost
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        releaseSigning?.let { props ->
            create("release") {
                storeFile = rootDir.resolve("signing").resolve(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // AGP 9.3 이상의 최적화 DSL(R8). keep 규칙은 src/<variant>/keepRules/*.keep 에 둔다.
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.findByName("release")
        }
    }

    // 번역이 있는 언어만 담는다. 기본(values/)이 한국어, values-en/ 이 영어다.
    androidResources {
        localeFilters += listOf("ko", "en")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.koin.android)
}
