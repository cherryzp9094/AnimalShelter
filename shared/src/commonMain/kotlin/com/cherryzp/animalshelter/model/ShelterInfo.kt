package com.cherryzp.animalshelter.model

import kotlinx.serialization.Serializable

/**
 * 동물보호센터 정보(`shelterInfo_v2`, 02-data 1.5·4절). 보호소 전체 캐시에 그대로 저장한다.
 * 실측에서 채움률이 낮은 칸이 많아 거의 전부 nullable이다.
 */
@Serializable
data class ShelterInfo(
    val regNo: String,
    val name: String,
    val orgName: String? = null,
    /** 센터 유형(예 `법인`). */
    val division: String? = null,
    /** 구조 대상 동물 원문(예 `개+고양이+기타`). 표시 때 `+`를 `, `로. */
    val targetAnimals: String? = null,
    val roadAddress: String? = null,
    val jibunAddress: String? = null,
    /** 위도·경도. 0이거나 한국 범위 밖이면 둘 다 null. */
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** 지정일자 원문(`yyyy-mm-dd`). */
    val designatedOn: String? = null,
    val weekdayHours: TimeRange? = null,
    val weekdayAdoptionHours: TimeRange? = null,
    val weekendHours: TimeRange? = null,
    val weekendAdoptionHours: TimeRange? = null,
    /** 휴무일 문구. 숫자·`+`만 있는 코드 값은 null. */
    val closedDay: String? = null,
    val vetCount: Int? = null,
    /** 사양관리사 수(`specsPersonCnt`). */
    val keeperCount: Int? = null,
    val roomCounts: RoomCounts? = null,
    val vehicleCount: Int? = null,
    /** 전화. `***`로 가려진 값은 null(목록의 전화로 대신한다). */
    val tel: String? = null,
    /** 데이터 기준일 원문. */
    val dataDate: String? = null,
)

/** 운영·분양 시간(`HH:mm`). `00:00`~`24:00`이면 [isAllDay]. */
@Serializable
data class TimeRange(val start: String, val end: String) {
    val isAllDay: Boolean get() = start == "00:00" && end == "24:00"
}

/** 시설 수. 넷 다 비면 [ShelterInfo.roomCounts]가 null. */
@Serializable
data class RoomCounts(
    val medical: Int? = null,
    val breeding: Int? = null,
    val quarantine: Int? = null,
    val feedStorage: Int? = null,
)
