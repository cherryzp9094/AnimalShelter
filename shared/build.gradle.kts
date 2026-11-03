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
        // Compose UI 흐름 테스트(Phase 3)를 에뮬레이터에서 돌린다.
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
            implementation(libs.ktor.client.mock)
            implementation(libs.koin.test)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.ext.junit)
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
