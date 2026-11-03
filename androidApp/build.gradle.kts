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
val serviceKey: String = localProperties.getProperty("SERVICE_KEY").orEmpty()

// 에뮬레이터에서 호스트의 중계 서버(wrangler dev)에 닿는 주소가 기본값이다.
val relayBaseUrl: String = localProperties.getProperty("RELAY_BASE_URL") ?: "http://10.0.2.2:8788"

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
