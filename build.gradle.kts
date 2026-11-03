import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

plugins {
    // KGP 2.4.21을 루트 클래스패스에 먼저 올린다. AGP 9 내장 Kotlin(:androidApp)도 이 버전을 쓴다.
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.spotless)
}

// Spotless는 루트에서만 설정한다. 코드 스타일은 루트 .editorconfig 한 곳에서 정한다.
spotless {
    kotlin {
        target("**/src/**/*.kt")
        targetExclude("**/build/**/*.kt")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("*.gradle.kts", "*/*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
}

// Kotlin/Wasm 도구(Node.js·Yarn·Binaryen)를 받을 저장소는 settings.gradle.kts에 있다.
// downloadBaseUrl을 비워 플러그인이 프로젝트 저장소를 더하지 않게 한다(FAIL_ON_PROJECT_REPOS).
// Node.js·Binaryen 설정은 프로젝트마다, Yarn은 루트에 붙는다.
@OptIn(ExperimentalWasmDsl::class)
allprojects {
    plugins.withType<WasmNodeJsRootPlugin> {
        extensions.getByType<WasmNodeJsEnvSpec>().downloadBaseUrl.set(null as String?)
    }
    plugins.withType<WasmNodeJsPlugin> {
        extensions.getByType<WasmNodeJsEnvSpec>().downloadBaseUrl.set(null as String?)
    }
    plugins.withType<WasmYarnPlugin> {
        extensions.getByType<WasmYarnRootEnvSpec>().downloadBaseUrl.set(null as String?)
    }
    plugins.withType<BinaryenPlugin> {
        extensions.getByType<BinaryenEnvSpec>().downloadBaseUrl.set(null as String?)
    }
}
