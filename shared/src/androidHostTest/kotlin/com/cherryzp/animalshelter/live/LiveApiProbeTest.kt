package com.cherryzp.animalshelter.live

import com.cherryzp.animalshelter.data.api.AnimalShelterApi
import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.data.api.ApiJson
import com.cherryzp.animalshelter.data.api.ApiOperation
import com.cherryzp.animalshelter.data.api.ApiResult
import com.cherryzp.animalshelter.data.api.EndpointSelector
import com.cherryzp.animalshelter.data.api.RawResponse
import com.cherryzp.animalshelter.data.api.classifyResponse
import com.cherryzp.animalshelter.data.api.createApiHttpClient
import com.cherryzp.animalshelter.data.api.dto.AbandonmentItemDto
import com.cherryzp.animalshelter.data.api.dto.ShelterInfoDto
import com.cherryzp.animalshelter.data.api.fixture.createFixtureEngine
import com.cherryzp.animalshelter.data.api.paging
import com.cherryzp.animalshelter.data.api.toBasicString
import com.cherryzp.animalshelter.data.repository.toAnimal
import com.cherryzp.animalshelter.data.repository.toShelterInfo
import com.cherryzp.animalshelter.di.AppConfig
import com.cherryzp.animalshelter.di.createOkHttpEngine
import com.cherryzp.animalshelter.model.NoticeState
import com.cherryzp.animalshelter.model.SearchFilter
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import java.io.File
import java.time.LocalTime
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assume.assumeTrue

/**
 * 실제 API를 정해진 횟수(6회 안팎)만 불러 키 인코딩 방식과 단건 `item` 모양을 확인한다(브리핑 6절, D19).
 * CLAUDE.md "실제 서버를 치는 테스트 금지"의 유일한 예외 — 환경 변수 `ANIMALSHELTER_LIVE=1`일 때만 돈다.
 *
 * 실행: `ANIMALSHELTER_LIVE=1 ./gradlew :shared:testAndroidHostTest`
 *   `--tests '*LiveApiProbeTest' --rerun`
 * 결과: `local-runs/<날짜>-phase2/live/`(git 제외). 키·URL·예외 메시지는 출력하지 않는다.
 */
class LiveApiProbeTest {
    @OptIn(ExperimentalSerializationApi::class)
    private val pretty = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    @Test
    fun probe() = runBlocking {
        assumeTrue("ANIMALSHELTER_LIVE=1일 때만 실행", System.getenv("ANIMALSHELTER_LIVE") == "1")
        val root = repoRoot()
        assumeTrue("저장소 루트를 찾지 못함", root != null)
        val key = readServiceKey(root!!)
        assumeTrue("local.properties에 SERVICE_KEY 없음", !key.isNullOrBlank())

        val outDir = File(root, "local-runs/2026-11-04-phase2/live").apply { mkdirs() }
        val summary = StringBuilder()
        var calls = 0
        // 앱과 같은 엔진 설정(platformModule)
        val client = createApiHttpClient(createOkHttpEngine())
        client.plugin(HttpSend).intercept { request ->
            calls++
            execute(request)
        }
        val endpoint = ApiEndpoint.Direct(key!!)
        val api = AnimalShelterApi(
            client = client,
            fixtureClient = lazy { createApiHttpClient(createFixtureEngine()) },
            endpoints = EndpointSelector(AppConfig(endpoint, false, "probe")) { null },
            clock = Clock.System
        )

        fun log(line: String) {
            summary.appendLine("${LocalTime.now().withNano(0)} $line")
        }

        suspend fun <T> timed(block: suspend () -> T): Pair<T, Long> {
            val start = System.nanoTime()
            val value = block()
            return value to (System.nanoTime() - start) / 1_000_000
        }

        try {
            // ① 시도: 성공이면 encodedParameters로 붙인 키가 맞다.
            val before1 = calls
            val (sido, ms1) = timed { api.getSido() }
            log(
                "1 sido_v2 calls=${calls - before1} ${describe(sido)} " +
                    "count=${(sido as? ApiResult.Success)?.value?.size} ${ms1}ms"
            )
            if (sido is ApiResult.Failure) {
                writeSummary(outDir, summary, calls)
                error("getSido 실패: ${sido.error} — 멈추고 보고한다")
            }

            // ② 목록 numOfRows=1 원본
            val before2 = calls
            val (single, ms2) = timed { api.fetchRaw(endpoint, ApiOperation.ANIMALS, paging(1, 1)) }
            val singleRaw = rawOrStop(single, "2", ms2, ::log)
            val singleJson = ApiJson.parseToJsonElement(singleRaw.body)
            val shape2 = itemShape(singleJson)
            log(
                "2 abandonmentPublic_v2(numOfRows=1) calls=${calls - before2} " +
                    "http=${singleRaw.status} ${headerOf(singleJson)} item=$shape2 ${ms2}ms"
            )
            saveSample(File(outDir, "abandonmentPublic_v2_single.json"), singleJson)
            val singleBody = classifyResponse(singleRaw, endpoint, AbandonmentItemDto.serializer())
            val first = (singleBody as ApiResult.Success).value.items.first().toAnimal()!!
            val desertionNo = first.desertionNo
            val happenDate = first.happenDate

            // ③ 그 구조번호 + 접수일 창 1회(원본 저장, getAnimal과 같은 파라미터)
            val before3 = calls
            val params3 = buildList {
                add("desertion_no" to desertionNo)
                happenDate?.let {
                    add("bgnde" to it.toBasicString())
                    add("endde" to it.toBasicString())
                }
            } + paging(1, 1)
            val (byNo, ms3) = timed { api.fetchRaw(endpoint, ApiOperation.ANIMALS, params3) }
            val byNoRaw = rawOrNull(byNo, "3", ms3, ::log)
            if (byNoRaw != null) {
                val byNoJson = ApiJson.parseToJsonElement(byNoRaw.body)
                val byNoFile = File(outDir, "abandonmentPublic_v2_desertion_no.json")
                saveSample(byNoFile, byNoJson)
                val itemSerializer = AbandonmentItemDto.serializer()
                val byNoBody = classifyResponse(byNoRaw, endpoint, itemSerializer)
                val found3 = (byNoBody as? ApiResult.Success)?.value?.items
                    ?.mapNotNull { it.toAnimal() }
                    ?.firstOrNull { it.desertionNo == desertionNo }
                log(
                    "3 abandonmentPublic_v2(desertion_no, bgnde=endde=happenDt) " +
                        "calls=${calls - before3} " +
                        "http=${byNoRaw.status} ${headerOf(byNoJson)} " +
                        "item=${itemShape(byNoJson)} " +
                        "found=${found3 != null} ${ms3}ms"
                )
            }

            // ④ 날짜 없이: 창 규칙 1회차에서 찾는지
            val before4 = calls
            val (noDate, ms4) = timed { api.getAnimal(desertionNo, null) }
            log(
                "4 getAnimal(no, null) calls=${calls - before4} ${describe(noDate)} " +
                    "found=${(noDate as? ApiResult.Success)?.value != null} ${ms4}ms"
            )

            // ⑤ 보호소 전체: 건수와 파싱 실패 0(D7)
            val before5 = calls
            val (shelters, ms5) = timed {
                api.fetchRaw(endpoint, ApiOperation.SHELTER_INFO, paging(1, 1000))
            }
            val sheltersRaw = rawOrNull(shelters, "5", ms5, ::log)
            if (sheltersRaw != null) {
                val sheltersJson = ApiJson.parseToJsonElement(sheltersRaw.body)
                val shelterSerializer = ShelterInfoDto.serializer()
                val shelterBody = classifyResponse(sheltersRaw, endpoint, shelterSerializer)
                val dtoCount = (shelterBody as? ApiResult.Success)?.value?.items?.size
                val mappedCount = (shelterBody as? ApiResult.Success)?.value?.items
                    ?.mapNotNull { it.toShelterInfo() }?.size
                log(
                    "5 shelterInfo_v2(numOfRows=1000) calls=${calls - before5} " +
                        "http=${sheltersRaw.status} ${headerOf(sheltersJson)} " +
                        "raw=${rawItemCount(sheltersJson)} " +
                        "dto=$dtoCount mapped=$mappedCount " +
                        "classify=${describe(shelterBody)} ${ms5}ms"
                )
            }

            // ⑥ 서울 공고중 전부(마감 임박순 경로)
            val before6 = calls
            val seoulNotice = SearchFilter(sidoCode = "6110000", state = NoticeState.NOTICE)
            val (all, ms6) = timed { api.searchAll(seoulNotice) }
            log(
                "6 searchAll(서울, notice) calls(pages)=${calls - before6} ${describe(all)} " +
                    "count=${(all as? ApiResult.Success)?.value?.size} ${ms6}ms"
            )

            log("happenDt(2)=${happenDate?.toBasicString()} noticeEnd(2)=${first.noticeEnd}")
        } finally {
            writeSummary(outDir, summary, calls)
            client.close()
        }

        assertTrue(calls <= 10, "호출 상한")
        assertEquals(true, File(outDir, "summary.txt").isFile)
    }

    /** 원본 응답 또는 null(실패를 기록한다). */
    private fun rawOrNull(
        result: ApiResult<RawResponse>,
        step: String,
        ms: Long,
        log: (String) -> Unit,
    ): RawResponse? = when (result) {
        is ApiResult.Success -> result.value

        is ApiResult.Failure -> {
            log("$step failed ${result.error} ${ms}ms")
            null
        }
    }

    /** 원본 응답. 실패면 기록하고 멈춘다(뒤 단계가 이 응답의 구조번호를 쓴다). */
    private fun rawOrStop(
        result: ApiResult<RawResponse>,
        step: String,
        ms: Long,
        log: (String) -> Unit,
    ): RawResponse = rawOrNull(result, step, ms, log) ?: error("$step 단계 실패 — 멈춘다")

    private fun describe(result: ApiResult<*>): String = when (result) {
        is ApiResult.Success -> "result=Success"
        is ApiResult.Failure -> "result=${result.error}"
    }

    private fun headerOf(json: JsonElement): String {
        val response = (json as? JsonObject)?.get("response")?.jsonObject
        val code = (response?.get("header") as? JsonObject)?.get("resultCode")
        val total = (response?.get("body") as? JsonObject)?.get("totalCount")
        return "resultCode=$code totalCount=$total"
    }

    private fun itemShape(json: JsonElement): String {
        val items = ((json as? JsonObject)?.get("response") as? JsonObject)
            ?.get("body")?.let { it as? JsonObject }?.get("items")
        val item = (items as? JsonObject)?.get("item")
        return when (item) {
            is JsonArray -> "array(${item.size})"
            is JsonObject -> "object"
            null -> if (items is JsonObject) "empty{}" else "none"
            else -> "other"
        }
    }

    private fun rawItemCount(json: JsonElement): Int {
        val body = ((json as? JsonObject)?.get("response") as? JsonObject)?.get("body")
        val item = ((body as? JsonObject)?.get("items") as? JsonObject)?.get("item")
        return when (item) {
            is JsonArray -> item.size
            is JsonObject -> 1
            else -> 0
        }
    }

    /** 2칸 들여쓰기로 저장한다. */
    private fun saveSample(file: File, json: JsonElement) {
        file.writeText(pretty.encodeToString(JsonElement.serializer(), maskOwner(json)) + "\n")
    }

    /** 보호소 대표자 이름은 저장하지 않는다(docs/api/README 규칙). */
    private fun maskOwner(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.mapValues { (name, value) ->
                if (name == "careOwnerNm") JsonPrimitive("(가림)") else maskOwner(value)
            }
        )

        is JsonArray -> JsonArray(element.map(::maskOwner))

        else -> element
    }

    private fun writeSummary(outDir: File, summary: StringBuilder, calls: Int) {
        File(outDir, "summary.txt").writeText(
            "# LiveApiProbeTest — 실제 호출 기록(키·URL 없음)\n" + summary +
                "total calls=$calls\n"
        )
    }

    private fun repoRoot(): File? {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").isFile) dir = dir.parentFile
        return dir
    }

    private fun readServiceKey(root: File): String? {
        val file = File(root, "local.properties")
        if (!file.isFile) return null
        return Properties().apply { file.inputStream().use(::load) }.getProperty("SERVICE_KEY")
    }
}
