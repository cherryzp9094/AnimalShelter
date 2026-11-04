package com.cherryzp.animalshelter.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * 웹 주소(해시)와 back stack(03 0.5, 브리핑 W2). 주소는 **맨 위 화면만** 나타내고, back stack 전체는 기록의
 * `state`가 든다(W3 — [encodeHistoryState]). 순수 함수라 두 타깃에서 테스트한다.
 */
object WebRoutes {
    private val desertionNo = Regex("""\d{15}""")
    private val photoIndex = Regex("""\d{1,9}""")
    private val careRegNo = Regex("""\d{1,20}""")

    /** 화면 하나의 해시. 목록 `#/`. */
    fun hashOf(key: NavKey): String = when (key) {
        is AnimalDetailKey -> "#/animal/${key.desertionNo}"
        is PhotoViewerKey -> "#/animal/${key.desertionNo}/photo/${key.index}"
        is ShelterInfoKey -> "#/shelter/${key.regNo}"
        FilterKey -> "#/filter"
        BookmarkListKey -> "#/bookmarks"
        SettingsKey -> "#/settings"
        LicensesKey -> "#/settings/licenses"
        else -> "#/"
    }

    /** back stack의 주소 = 맨 위 화면의 해시. */
    fun hashOf(stack: List<NavKey>): String = hashOf(stack.lastOrNull() ?: AnimalListKey)

    /**
     * 해시 → 계층형 스택(W3): 상세 `[목록, 상세]`, 사진 `[목록, 상세, 사진]`, 북마크·필터·설정·보호소 `[목록, 그 화면]`,
     * 오픈소스 라이선스 `[목록, 설정, 라이선스]`(5a O1).
     * 빈 해시·모르는 경로·형식 위반(구조번호 15자리 숫자, 사진 번호 0 이상 정수, 보호소 번호 숫자)은 목록.
     * 주소로 연 상세는 접수일이 없다(재조회 창 최대 3회, 02-data 6절), 사진은 `photos`가 비어 뷰어가 찾는다(W6).
     */
    fun stackFromHash(hash: String): List<NavKey> {
        val path = hash.removePrefix("#")
        val parts = if (path.startsWith(
                "/"
            )
        ) {
            path.substring(1).split("/")
        } else {
            return listOf(AnimalListKey)
        }
        val list: NavKey = AnimalListKey
        return when {
            parts == listOf("filter") -> listOf(list, FilterKey)

            parts == listOf("bookmarks") -> listOf(list, BookmarkListKey)

            parts == listOf("settings") -> listOf(list, SettingsKey)

            parts == listOf("settings", "licenses") -> listOf(list, SettingsKey, LicensesKey)

            parts.size == 2 && parts[0] == "animal" && desertionNo.matches(parts[1]) ->
                listOf(list, AnimalDetailKey(parts[1]))

            parts.size == 4 && parts[0] == "animal" && desertionNo.matches(parts[1]) &&
                parts[2] == "photo" && photoIndex.matches(parts[3]) -> listOf(
                list,
                AnimalDetailKey(parts[1]),
                PhotoViewerKey(parts[1], index = parts[3].toInt())
            )

            parts.size == 2 && parts[0] == "shelter" && careRegNo.matches(parts[1]) ->
                listOf(list, ShelterInfoKey(parts[1]))

            else -> listOf(list)
        }
    }
}

/** 기록 하나가 든 것(W3): 이 앱이 이 문서에서 쌓은 기록 중 위치([depth], 0부터)와 그때의 back stack. */
data class HistoryEntry(val depth: Int, val stack: List<NavKey>)

private const val STATE_VERSION = 1

private val historyJson = Json {
    serializersModule = AppNavSerializersModule
    ignoreUnknownKeys = true
}

private val stackSerializer = ListSerializer(PolymorphicSerializer(NavKey::class))

/** 기록의 `state`(JSON 문자열) = `{"v":1,"d":깊이,"s":back stack}`. 키는 저장 설정과 같은 직렬화 모듈로. */
fun encodeHistoryState(entry: HistoryEntry): String = buildJsonObject {
    put("v", STATE_VERSION)
    put("d", entry.depth)
    put("s", historyJson.encodeToJsonElement(stackSerializer, entry.stack))
}.toString()

/**
 * [encodeHistoryState]의 반대. 읽을 수 없으면(다른 판·손상·우리 것이 아님) null — 그때는 해시를 본다.
 * 스택은 목록으로 시작해야 한다(계층형).
 */
fun decodeHistoryState(raw: String?): HistoryEntry? {
    if (raw.isNullOrEmpty()) return null
    return try {
        val root = historyJson.parseToJsonElement(raw) as? JsonObject ?: return null
        if (root["v"]?.jsonPrimitive?.int != STATE_VERSION) return null
        val depth = root["d"]?.jsonPrimitive?.int ?: return null
        val stack = historyJson.decodeFromJsonElement(stackSerializer, root["s"] ?: return null)
        if (depth < 0 || stack.firstOrNull() != AnimalListKey) null else HistoryEntry(depth, stack)
    } catch (_: IllegalArgumentException) {
        null // SerializationException·NumberFormatException 모두 이것의 하위
    }
}

/** 브라우저 기록에 할 일(웹 연결이 실행한다). */
sealed interface HistoryCommand {
    data class Push(val state: String, val url: String) : HistoryCommand

    data class Replace(val state: String, val url: String) : HistoryCommand

    /** `history.go(delta)`(음수 = 뒤로). 이어지는 `popstate`가 스택을 맞춘다. */
    data class Go(val delta: Int) : HistoryCommand
}

/**
 * `popstate` 처리 결과. [apply]가 있으면 back stack을 그 스택으로 맞춘다(같은 앞부분은 두고 꼬리만 — entry 상태가
 * 남게). [deferred]는 popstate 처리 안에서 바로 하지 말고 다음 차례로 미뤄 실행한다(크롬은 popstate 안의
 * `pushState`·`replaceState`에서 기록이 꼬일 수 있다).
 */
data class PopOutcome(val apply: List<NavKey>?, val deferred: HistoryCommand?)

/**
 * back stack ↔ 브라우저 기록 동기화(브리핑 W3, 계층형 + 앞으로 지원). 상태 기계만 — 브라우저 API는 웹 연결이 부른다.
 *
 * - 앱이 바꿀 때([onAppChanged]): 같으면 없음 · 위에 키 하나가 더해졌으면 `Push`(깊이 + 1) · 위에서 n개가 빠진 앞부분이고
 *   깊이 ≥ n이면 `Go(-n)`(도착할 `popstate`까지 지금 기록·깊이를 그대로 둔다) · 그 밖(두 칸의 상세 바꾸기, 모양 바뀜, 앱
 *   기록보다 많이 빠짐)은 `Replace`(깊이 그대로).
 * - `popstate`([onPopState]): 우리가 건 `Go`의 도착이면 앱 스택(기대 스택)은 그대로 두고, 도착한 기록이 다르면(주소창으로
 *   만든 기록이 끼어 있던 경우) 그 기록을 앱 스택으로 고친다(미뤄서). 그 밖(브라우저 뒤로·앞으로)은 기록의 `state`대로 앱
 *   스택을 맞추고, `state`가 없으면(주소창·해시 링크) 해시를 계층형 스택으로 열고 깊이 + 1로 `Replace`(미뤄서).
 */
class WebHistorySync private constructor(lastSynced: List<NavKey>, depth: Int) {
    /** 지금 기록이 든 스택. */
    var lastSynced: List<NavKey> = lastSynced
        private set

    /** 지금 기록의 깊이. */
    var depth: Int = depth
        private set

    /** 우리가 건 `Go`: 그 목표 스택과 칸 수. `popstate`가 오면 지운다. */
    private var pendingGo: Pair<List<NavKey>, Int>? = null

    /** `Go`를 기다리는 동안의 앱 스택(그 사이 앱이 또 바꾸면 마지막 것). */
    private var latestApp: List<NavKey> = lastSynced

    /** 앱 스택이 바뀌었다. 할 일이 없으면 null. */
    fun onAppChanged(stack: List<NavKey>): HistoryCommand? {
        latestApp = stack
        if (pendingGo != null) return null
        return commandFor(stack)
    }

    /** `popstate`가 왔다. [rawState]는 그 기록의 `state`(문자열이 아니면 null), [hash]는 `location.hash`. */
    fun onPopState(rawState: String?, hash: String): PopOutcome {
        val arrived = decodeHistoryState(rawState)
        val pending = pendingGo
        if (pending != null) {
            pendingGo = null
            val (target, steps) = pending
            val expected = latestApp
            if (arrived != null && arrived.stack == target) {
                // 기대한 기록에 닿았다. 그 사이 앱이 또 바꿨으면 이어서 맞춘다(빠르게 두 번 뒤로 등)
                lastSynced = arrived.stack
                depth = arrived.depth
                return PopOutcome(apply = null, deferred = commandFor(expected))
            }
            // 주소창으로 만든 기록이 끼어 있었다 — 앱은 기대 스택 그대로, 그 기록을 기대 스택으로 고친다
            depth = arrived?.depth ?: maxOf(0, depth - steps)
            return PopOutcome(apply = null, deferred = replace(expected))
        }
        if (arrived != null) {
            lastSynced = arrived.stack
            latestApp = arrived.stack
            depth = arrived.depth
            return PopOutcome(apply = arrived.stack, deferred = null)
        }
        val stack = WebRoutes.stackFromHash(hash)
        depth += 1
        latestApp = stack
        return PopOutcome(apply = stack, deferred = replace(stack))
    }

    private fun commandFor(stack: List<NavKey>): HistoryCommand? {
        val last = lastSynced
        return when {
            stack == last -> null

            stack.size == last.size + 1 && stack.subList(0, last.size) == last -> {
                depth += 1
                lastSynced = stack
                HistoryCommand.Push(
                    encodeHistoryState(HistoryEntry(depth, stack)),
                    WebRoutes.hashOf(stack)
                )
            }

            stack.isNotEmpty() && stack.size < last.size && last.subList(0, stack.size) == stack &&
                depth >= last.size - stack.size -> {
                val steps = last.size - stack.size
                pendingGo = stack to steps
                HistoryCommand.Go(-steps)
            }

            else -> replace(stack)
        }
    }

    private fun replace(stack: List<NavKey>): HistoryCommand.Replace {
        lastSynced = stack
        return HistoryCommand.Replace(
            encodeHistoryState(HistoryEntry(depth, stack)),
            WebRoutes.hashOf(stack)
        )
    }

    companion object {
        /**
         * 시작(W3): 기록의 `state`가 읽히면 그 스택·깊이(새로고침), 아니면 해시를 계층형 스택으로 깊이 0 — 주소를
         * 정규화하는 `Replace`를 함께 돌려준다. 목록용 가짜 기록은 앞에 끼우지 않는다(공유 링크로 온 사람의 브라우저
         * 뒤로는 온 곳으로, 앱 안 ←는 목록으로).
         */
        fun start(rawState: String?, hash: String): Pair<WebHistorySync, HistoryCommand.Replace?> {
            decodeHistoryState(rawState)?.let { return WebHistorySync(it.stack, it.depth) to null }
            val stack = WebRoutes.stackFromHash(hash)
            val sync = WebHistorySync(stack, 0)
            return sync to sync.replace(stack)
        }
    }
}
