package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.dto.KindDto
import com.cherryzp.animalshelter.data.api.dto.ShelterDto
import com.cherryzp.animalshelter.data.api.dto.ShelterInfoDto
import com.cherryzp.animalshelter.data.api.dto.SidoDto
import com.cherryzp.animalshelter.data.api.dto.SigunguDto
import com.cherryzp.animalshelter.model.Kind
import com.cherryzp.animalshelter.model.RoomCounts
import com.cherryzp.animalshelter.model.ShelterCode
import com.cherryzp.animalshelter.model.ShelterInfo
import com.cherryzp.animalshelter.model.Sido
import com.cherryzp.animalshelter.model.Sigungu
import com.cherryzp.animalshelter.model.TimeRange

// `shelterInfo_v2`·코드표 item → 도메인(02-data 5.1). 필수 칸이 없는 항목은 null(목록에서 뺀다).

private val MASKED_TEL = Regex("""^\*+$""")

// 00:00 ~ 23:59와 24:00. `:`만 있는 값·다른 형식은 null.
private val HOUR_MINUTE = Regex("""^(?:[01]\d|2[0-3]):[0-5]\d$|^24:00$""")

// 숫자·`+`·공백만 있는 휴무일(예 `0`, `1+15+10`)은 뜻을 알 수 없어 버린다.
private val CODE_ONLY = Regex("""^[\d+\s]+$""")
private val LATITUDE_RANGE = 33.0..39.0
private val LONGITUDE_RANGE = 124.0..132.0

internal fun ShelterInfoDto.toShelterInfo(): ShelterInfo? {
    val regNo = careRegNo.clean() ?: return null
    val name = careNm.clean() ?: return null
    val coordinatesValid = lat != null && lng != null &&
        lat != 0.0 && lng != 0.0 && lat in LATITUDE_RANGE && lng in LONGITUDE_RANGE
    val rooms = RoomCounts(
        medical = medicalCnt,
        breeding = breedCnt,
        quarantine = quarabtineCnt,
        feedStorage = feedCnt
    ).takeIf {
        listOf(it.medical, it.breeding, it.quarantine, it.feedStorage).any { n -> n != null }
    }
    return ShelterInfo(
        regNo = regNo,
        name = name,
        orgName = orgNm.clean(),
        division = divisionNm.clean(),
        targetAnimals = saveTrgtAnimal.clean(),
        roadAddress = careAddr.clean(),
        jibunAddress = jibunAddr.clean(),
        latitude = lat.takeIf { coordinatesValid },
        longitude = lng.takeIf { coordinatesValid },
        designatedOn = dsignationDate.clean(),
        weekdayHours = timeRange(weekOprStime, weekOprEtime),
        weekdayAdoptionHours = timeRange(weekCellStime, weekCellEtime),
        weekendHours = timeRange(weekendOprStime, weekendOprEtime),
        weekendAdoptionHours = timeRange(weekendCellStime, weekendCellEtime),
        closedDay = closeDay.clean()?.takeUnless { CODE_ONLY.matches(it) },
        vetCount = vetPersonCnt,
        keeperCount = specsPersonCnt,
        roomCounts = rooms,
        vehicleCount = transCarCnt,
        tel = careTel.clean()?.takeUnless { MASKED_TEL.matches(it) },
        dataDate = dataStdDt.clean()
    )
}

/**
 * 시작·끝이 둘 다 `HH:mm`일 때만 시간대. 시작과 끝이 같으면(길이 0 — `00:00~00:00`, `09:00~09:00`) 뜻을 알 수 없어
 * null(그 줄을 숨긴다, Phase 3 검토 R10 — 상시로 단정하지 않는다). `00:00~24:00`은 그대로(24시간).
 */
internal fun timeRange(start: String?, end: String?): TimeRange? {
    val s = start.clean()?.takeIf { HOUR_MINUTE.matches(it) } ?: return null
    val e = end.clean()?.takeIf { HOUR_MINUTE.matches(it) } ?: return null
    if (s == e) return null
    return TimeRange(s, e)
}

internal fun SidoDto.toSido(): Sido? {
    val code = orgCd.clean() ?: return null
    return Sido(code = code, name = orgdownNm.clean() ?: return null)
}

internal fun SigunguDto.toSigungu(): Sigungu? {
    val code = orgCd.clean() ?: return null
    return Sigungu(
        uprCode = uprCd.clean().orEmpty(),
        code = code,
        name =
            orgdownNm.clean() ?: return null
    )
}

internal fun ShelterDto.toShelterCode(): ShelterCode? {
    val regNo = careRegNo.clean() ?: return null
    return ShelterCode(regNo = regNo, name = careNm.clean() ?: return null)
}

internal fun KindDto.toKind(): Kind? {
    val code = kindCd.clean() ?: return null
    return Kind(code = code, name = kindNm.clean() ?: return null)
}
