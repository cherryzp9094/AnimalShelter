import java.util.Properties
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

/**
 * 웹 번들에 넣을 상수(`WebConfig`)를 만든다. 중계 서버 주소와 앱 버전만 들어간다.
 * 공공데이터 인증키는 이 경로로 절대 들어가지 않는다(웹은 중계 서버가 키를 붙인다).
 */
abstract class GenerateWebConfigTask : DefaultTask() {
    @get:Input
    abstract val relayBaseUrl: Property<String>

    @get:Input
    abstract val versionName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        fun String.escaped() = replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$")
        val file = outputDir.get().file("com/cherryzp/animalshelter/web/WebConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |// 생성 파일: ./gradlew :webApp:generateWebConfig. 직접 고치지 않는다.
            |package com.cherryzp.animalshelter.web
            |
            |object WebConfig {
            |    /** 중계 서버 주소. 빈 문자열이면 같은 origin의 상대 경로(/api, /img)를 쓴다. */
            |    const val RELAY_BASE_URL: String = "${relayBaseUrl.get().escaped()}"
            |
            |    /** 앱 버전 이름(libs.versions.toml의 appVersionName) */
            |    const val VERSION_NAME: String = "${versionName.get().escaped()}"
            |}
            |
            """.trimMargin()
        )
    }
}

// 우선순위: -PrelayBaseUrl(빈 값 허용) → local.properties의 RELAY_BASE_URL → http://localhost:8788
// local.properties에서는 RELAY_BASE_URL 한 줄만 꺼낸다.
val localRelayBaseUrl: Provider<String> = providers
    .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
    .asText
    .map { text ->
        Properties().apply { load(text.reader()) }.getProperty("RELAY_BASE_URL").orEmpty()
    }
    .filter { it.isNotEmpty() }

val generateWebConfig = tasks.register<GenerateWebConfigTask>("generateWebConfig") {
    relayBaseUrl.set(
        providers.gradleProperty("relayBaseUrl")
            .orElse(localRelayBaseUrl)
            .orElse("http://localhost:8788")
    )
    versionName.set(libs.versions.appVersionName)
    outputDir.set(layout.buildDirectory.dir("generated/webConfig/kotlin"))
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "animalshelter.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain {
            kotlin.srcDir(generateWebConfig.flatMap { it.outputDir })
            dependencies {
                implementation(projects.shared)
                implementation(libs.compose.ui)
            }
        }
    }
}
