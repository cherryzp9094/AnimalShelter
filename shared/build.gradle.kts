import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    compilerOptions {
        // expect/actual 클래스(PlatformInfo, ExternalActions)는 Beta 기능이라 경고를 끈다.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.cherryzp.animalshelter.shared"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        // Compose Multiplatform 리소스를 Android 자산으로 묶는다.
        androidResources {
            enable = true
        }
        // commonTest를 JVM(호스트)에서 돌린다.
        withHostTest {}
        // Compose UI 흐름 테스트(Phase 3c)를 에뮬레이터에서 돌린다.
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        // 테스트 번들에 Skiko(Compose UI 런타임)를 넣으려면 실행 바이너리가 있어야 한다(CMP-4906).
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.animation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.core)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.material3.adaptive.layout)
            implementation(libs.compose.material3.adaptive.navigation3)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.jetbrains.navigation3.ui)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)

            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)

            implementation(libs.androidx.datastore.core)
            implementation(libs.androidx.datastore.core.okio)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.okio)
            // Fixture 엔드포인트(키 없는 빌드·디버그 강제 픽스처)가 런타임에 MockEngine을 쓴다.
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            // 미리보기 호스트용
            implementation(libs.androidx.activity.compose)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
            // UI 흐름 본문(runComposeUiTest). 호스트 JVM에서는 흐름을 돌리지 않는다(@Test 래퍼가 wasmJsTest·androidDeviceTest에만)
            implementation(libs.compose.ui.test)
        }
        getByName("androidDeviceTest") {
            // 흐름 본문·Fake·테스트 시계만 commonTest에서 가져온다(브리핑 C6). commonTest 전체를 소스셋 트리로 잇지 않는
            // 이유: 테스트 이름의 공백을 D8이 DEX 040(API 30) 미만에서 받지 않는다(minSdk 26).
            val shared = "src/commonTest/kotlin/com/cherryzp/animalshelter"
            kotlin.srcDir("$shared/ui/flow")
            kotlin.srcDir("$shared/fake")
            kotlin.srcDir("$shared/testing")
            kotlin.exclude("**/FakeRepositoriesTest.kt", "**/TestStores.kt")
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.ext.junit)
            // Compose ui-test 1.12.1이 끌어오는 espresso-core 3.5.0은 Android 17(API 37)에서 사라진 숨은 API
            // (InputManager.getInstance)를 불러 UI 테스트가 시작 전에 실패한다 → runner 1.7.0과 같은 androidx.test 묶음의
            // 3.7.0으로 맞춘다(기기 테스트 APK에만, 앱에는 들어가지 않는다. 브리핑 C6 — 보고에 적음)
            implementation(libs.androidx.test.espresso.core)
            implementation(libs.androidx.compose.ui.test.junit4.android)
            implementation(libs.androidx.compose.ui.test.manifest)
        }
    }
}

// Android Studio 미리보기 렌더링
dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

compose.resources {
    packageOfResClass = "com.cherryzp.animalshelter.resources"
    publicResClass = false
}
