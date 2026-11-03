package com.cherryzp.animalshelter.data.api

import com.cherryzp.animalshelter.data.api.dto.AbandonmentItemDto
import com.cherryzp.animalshelter.data.api.dto.ApiBody
import com.cherryzp.animalshelter.data.api.dto.KindDto
import com.cherryzp.animalshelter.data.api.dto.ShelterDto
import com.cherryzp.animalshelter.data.api.dto.ShelterInfoDto
import com.cherryzp.animalshelter.data.api.dto.SidoDto
import com.cherryzp.animalshelter.data.api.dto.SigunguDto
import com.cherryzp.animalshelter.data.repository.toAnimal
import com.cherryzp.animalshelter.data.repository.toKind
import com.cherryzp.animalshelter.data.repository.toShelterCode
import com.cherryzp.animalshelter.data.repository.toShelterInfo
import com.cherryzp.animalshelter.data.repository.toSido
import com.cherryzp.animalshelter.data.repository.toSigungu
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.AnimalPage
import com.cherryzp.animalshelter.model.Kind
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.ShelterCode
import com.cherryzp.animalshelter.model.ShelterInfo
import com.cherryzp.animalshelter.model.Sido
import com.cherryzp.animalshelter.model.Sigungu
import com.cherryzp.animalshelter.model.todayInSeoul
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer

/** 목록 한 페이지 기본 크기. */
internal const val PAGE_SIZE = 30

/** 마감 임박순 수집 한 페이지(= API 최대). */
internal const val SEARCH_ALL_PAGE_SIZE = 1000

/** 재조회 창 연도의 하한. */
private const val MIN_LOOKUP_YEAR = 2000

/**
 * 공공데이터 API 데이터 소스(02-data 6절). 엔드포인트는 요청마다 [EndpointSelector]로 정한다(D3).
 * 예외는 전부 [ApiError]로 바꾸고(취소는 다시 던진다), 예외 메시지·URL은 어디에도 내보내지 않는다(D5).
 *
 * @param client 플랫폼 엔진(Android OkHttp, 웹 Js)의 클라이언트
 * @param fixtureClient Fixture 엔드포인트용(MockEngine). 처음 쓸 때 만든다.
 */
internal class AnimalShelterApi(
    private val client: HttpClient,
    private val fixtureClient: Lazy<HttpClient>,
    private val endpoints: EndpointSelector,
    private val clock: Clock,
) {
    suspend fun searchAnimals(
        filter: SearchFilter,
        pageNo: Int,
        numOfRows: Int = PAGE_SIZE,
    ): ApiResult<AnimalPage> = fetchAnimals(filter.toQuery(), pageNo, numOfRows)

    /**
     * 한 마리 조회(`desertion_no`, `numOfRows=1`). 날짜 없이 부르면 최근 약 1개월만 찾으므로 창을 바꿔 가며
     * 최대 3회 부른다(02-data 6절). 찾지 못하면 `Success(null)`, 도중 실패는 그 오류.
     */
    suspend fun getAnimal(desertionNo: String, happenDate: LocalDate? = null): ApiResult<Animal?> {
        val windows = lookupWindows(desertionNo, happenDate, clock.todayInSeoul().year)
        for (window in windows) {
            val params = buildList {
                add("desertion_no" to desertionNo)
                window?.let { (from, to) ->
                    add("bgnde" to from.toBasicString())
                    add("endde" to to.toBasicString())
                }
            }
            when (val result = fetchAnimals(params, pageNo = 1, numOfRows = 1)) {
                is ApiResult.Failure -> return result

                is ApiResult.Success -> {
                    val found = result.value.items.firstOrNull { it.desertionNo == desertionNo }
                    if (found != null) return ApiResult.Success(found)
                }
            }
        }
        return ApiResult.Success(null)
    }

    /**
     * 조건에 맞는 전부(마감 임박순). `numOfRows=1000`으로 최대 [maxPages]페이지.
     * 첫 페이지 `totalCount`가 `maxPages * 1000`을 넘으면 더 부르지 않고 [ApiError.TooManyResults].
     * `desertionNo`가 겹치면 앞의 것만 둔다.
     */
    suspend fun searchAll(filter: SearchFilter, maxPages: Int = 5): ApiResult<List<Animal>> {
        val query = filter.toQuery()
        val collected = LinkedHashMap<String, Animal>()
        var pageNo = 1
        while (true) {
            val page = when (val result = fetchAnimals(query, pageNo, SEARCH_ALL_PAGE_SIZE)) {
                is ApiResult.Failure -> return result
                is ApiResult.Success -> result.value
            }
            if (pageNo == 1 && page.totalCount > maxPages * SEARCH_ALL_PAGE_SIZE) {
                return ApiResult.Failure(ApiError.TooManyResults(page.totalCount))
            }
            page.items.forEach { collected.getOrPut(it.desertionNo) { it } }
            if (!page.hasMore || pageNo >= maxPages) break
            pageNo++
        }
        return ApiResult.Success(collected.values.toList())
    }

    suspend fun getSido(): ApiResult<List<Sido>> =
        call(ApiOperation.SIDO, paging(1, 100), SidoDto.serializer())
            .map { body -> body.items.mapNotNull { it.toSido() } }

    suspend fun getSigungu(sidoCode: String): ApiResult<List<Sigungu>> = call(
        ApiOperation.SIGUNGU,
        listOf("upr_cd" to sidoCode) + paging(1, 100),
        SigunguDto.serializer()
    ).map { body -> body.items.mapNotNull { it.toSigungu() } }

    suspend fun getShelters(sidoCode: String, sigunguCode: String): ApiResult<List<ShelterCode>> =
        call(
            ApiOperation.SHELTERS,
            listOf("upr_cd" to sidoCode, "org_cd" to sigunguCode) + paging(1, 200),
            ShelterDto.serializer()
        ).map { body -> body.items.mapNotNull { it.toShelterCode() } }

    /** 품종 조회는 `up_kind_cd`(목록의 `upkind`와 다르다). */
    suspend fun getKinds(kind: AnimalKind): ApiResult<List<Kind>> = call(
        ApiOperation.KINDS,
        listOf("up_kind_cd" to kind.code) + paging(1, 500),
        KindDto.serializer()
    ).map { body -> body.items.mapNotNull { it.toKind() } }

    /** 보호소 전체(`numOfRows=1000` 1회 = 전체, 실측 338건). */
    suspend fun getAllShelterInfo(): ApiResult<List<ShelterInfo>> =
        call(ApiOperation.SHELTER_INFO, paging(1, 1000), ShelterInfoDto.serializer())
            .map { body -> body.items.mapNotNull { it.toShelterInfo() } }

    private suspend fun fetchAnimals(
        params: List<Pair<String, String>>,
        pageNo: Int,
        numOfRows: Int,
    ): ApiResult<AnimalPage> = call(
        ApiOperation.ANIMALS,
        params + paging(pageNo, numOfRows),
        AbandonmentItemDto.serializer()
    ).map { body ->
        AnimalPage(
            items = body.items.mapNotNull { it.toAnimal() },
            pageNo = pageNo,
            numOfRows = numOfRows,
            totalCount = body.totalCount
        )
    }

    private suspend fun <T> call(
        operation: ApiOperation,
        params: List<Pair<String, String>>,
        itemSerializer: KSerializer<T>,
    ): ApiResult<ApiBody<T>> {
        val endpoint = endpoints.current()
        return when (val raw = fetchRaw(endpoint, operation, params)) {
            is ApiResult.Failure -> raw
            is ApiResult.Success -> classifyResponse(raw.value, endpoint, itemSerializer)
        }
    }

    /**
     * 요청을 보내고 상태·본문을 받는다. 연결 실패·타임아웃은 Relay면 [ApiError.RelayUnavailable],
     * 아니면 [ApiError.Network](D6 ①). 예외는 버린다 — Ktor 예외 메시지에는 요청 URL(=키)이 들어간다.
     */
    internal suspend fun fetchRaw(
        endpoint: ApiEndpoint,
        operation: ApiOperation,
        params: List<Pair<String, String>>,
    ): ApiResult<RawResponse> {
        val httpClient = if (endpoint is ApiEndpoint.Fixture) fixtureClient.value else client
        return try {
            val response = httpClient.get { apiRequest(endpoint, operation, params) }
            ApiResult.Success(RawResponse(response.status.value, response.bodyAsText()))
        } catch (_: CancellationException) {
            // 호출한 코루틴이 취소됐으면 다시 던지고, 엔진 안쪽의 취소(타임아웃 등)면 연결 실패로 본다.
            currentCoroutineContext().ensureActive()
            ApiResult.Failure(connectionError(endpoint))
        } catch (_: Throwable) {
            ApiResult.Failure(connectionError(endpoint))
        }
    }

    private fun connectionError(endpoint: ApiEndpoint): ApiError =
        if (endpoint is ApiEndpoint.Relay) ApiError.RelayUnavailable else ApiError.Network
}

/**
 * `desertion_no` 재조회 창(02-data 6절). null = 날짜 없이.
 * `happenDate`가 있으면 그날 하루 1회. 없으면 ① 날짜 없이 → ② 구조번호 7~10째 자리 연도 한 해 → ③ 그 전 해.
 * 연도가 2000~올해 밖이면 ②③은 건너뛴다.
 */
internal fun lookupWindows(
    desertionNo: String,
    happenDate: LocalDate?,
    thisYear: Int,
): List<Pair<LocalDate, LocalDate>?> {
    if (happenDate != null) return listOf(happenDate to happenDate)
    val year = desertionNo.getOrNull(9)?.let { desertionNo.substring(6, 10).toIntOrNull() }
    if (year == null || year !in MIN_LOOKUP_YEAR..thisYear) return listOf(null)
    return buildList {
        add(null)
        add(LocalDate(year, 1, 1) to LocalDate(year, 12, 31))
        if (year - 1 >=
            MIN_LOOKUP_YEAR
        ) {
            add(LocalDate(year - 1, 1, 1) to LocalDate(year - 1, 12, 31))
        }
    }
}
