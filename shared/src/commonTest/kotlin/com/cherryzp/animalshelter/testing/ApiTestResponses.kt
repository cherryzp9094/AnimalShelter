package com.cherryzp.animalshelter.testing

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** MockEngine이 돌려줄 공공데이터 응답 본문을 만든다(테스트 전용). */
object ApiTestResponses {
    fun animalItem(
        desertionNo: String,
        noticeEdt: String? = "20261113",
        happenDt: String? = "20261103",
        processState: String = "보호중",
    ): JsonObject = buildJsonObject {
        put("desertionNo", desertionNo)
        happenDt?.let { put("happenDt", it) }
        noticeEdt?.let { put("noticeEdt", it) }
        put("noticeSdt", "20261103")
        put("processState", processState)
        put("kindNm", "믹스견")
        put("upKindCd", "417000")
        put("sexCd", "M")
    }

    fun envelope(
        items: List<JsonObject>,
        totalCount: Int = items.size,
        pageNo: Int = 1,
        numOfRows: Int = items.size,
        resultCode: String = "00",
    ): String = buildJsonObject {
        put(
            "response",
            buildJsonObject {
                put(
                    "header",
                    buildJsonObject {
                        put("reqNo", 1)
                        put("resultCode", resultCode)
                        put("resultMsg", if (resultCode == "00") "NORMAL SERVICE." else "ERROR")
                    }
                )
                put(
                    "body",
                    buildJsonObject {
                        put(
                            "items",
                            if (items.isEmpty()) {
                                JsonObject(emptyMap())
                            } else {
                                buildJsonObject { put("item", JsonArray(items)) }
                            }
                        )
                        put("numOfRows", numOfRows)
                        put("pageNo", pageNo)
                        put("totalCount", totalCount)
                    }
                )
            }
        )
    }.toString()

    fun animals(vararg desertionNos: String, totalCount: Int = desertionNos.size): String =
        envelope(desertionNos.map { animalItem(it) }, totalCount)

    val empty: String get() = envelope(emptyList(), totalCount = 0)
}
