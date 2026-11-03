package com.cherryzp.animalshelter.ui.shelter

import com.cherryzp.animalshelter.model.ShelterInfo
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.label_adoption_hours_weekday
import com.cherryzp.animalshelter.resources.label_adoption_hours_weekend
import com.cherryzp.animalshelter.resources.label_closed_day
import com.cherryzp.animalshelter.resources.label_hours_weekday
import com.cherryzp.animalshelter.resources.label_hours_weekend
import com.cherryzp.animalshelter.resources.label_org_name
import com.cherryzp.animalshelter.resources.label_shelter_type
import com.cherryzp.animalshelter.resources.label_vehicles
import com.cherryzp.animalshelter.resources.room_breeding
import com.cherryzp.animalshelter.resources.room_feed
import com.cherryzp.animalshelter.resources.room_medical
import com.cherryzp.animalshelter.resources.room_quarantine
import com.cherryzp.animalshelter.resources.staff_keepers
import com.cherryzp.animalshelter.resources.staff_vets
import com.cherryzp.animalshelter.ui.common.UiText
import com.cherryzp.animalshelter.ui.common.collapseSpaces
import com.cherryzp.animalshelter.ui.common.plusSeparated
import com.cherryzp.animalshelter.ui.common.uiText
import com.cherryzp.animalshelter.ui.detail.DetailRow
import com.cherryzp.animalshelter.ui.detail.mapTarget
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.StringResource

/** 기준일이 이만큼 지나면 "오래된 정보일 수 있어요"(03 2.6). */
private const val STALE_YEARS = 2

private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/** 시설 줄(B7 — `label_staff`·`label_rooms`는 만들지 않는다): 있는 수만 부분으로. */
fun ShelterInfo.facilityLines(): List<List<UiText>> = listOf(
    listOfNotNull(
        vetCount?.let { uiText(Res.string.staff_vets, it) },
        keeperCount?.let { uiText(Res.string.staff_keepers, it) }
    ),
    listOfNotNull(
        roomCounts?.medical?.let { uiText(Res.string.room_medical, it) },
        roomCounts?.breeding?.let { uiText(Res.string.room_breeding, it) },
        roomCounts?.quarantine?.let { uiText(Res.string.room_quarantine, it) },
        roomCounts?.feedStorage?.let { uiText(Res.string.room_feed, it) }
    ),
    listOfNotNull(vehicleCount?.let { uiText(Res.string.label_vehicles, it) })
).filter { it.isNotEmpty() }

/** 데이터 기준일(`yyyy-mm-dd`)이 [STALE_YEARS]년 넘게 지났는가. 읽지 못하면 false. */
fun isShelterDataOld(dataDate: String?, today: LocalDate): Boolean {
    val text = dataDate.clean()?.take(10) ?: return false
    val date = try {
        LocalDate.parse(text)
    } catch (_: IllegalArgumentException) {
        return false
    }
    return date.plus(STALE_YEARS, DateTimeUnit.YEAR) < today
}

private fun row(label: StringResource, value: UiText, numeric: Boolean = false) =
    DetailRow(uiText(label), listOf(value), numeric = numeric)

private fun row(label: StringResource, value: String?) =
    value.clean()?.let { row(label, UiText.Raw(it)) }

/** [ShelterInfo] → [ShelterInfoUi]. 전화는 보호소 정보 값, 가려져 없으면 목록이 준 값(`fallbackTel`). */
fun ShelterInfo.toUi(fallbackTel: String?): ShelterInfoUi = ShelterInfoUi(
    header = listOfNotNull(
        row(Res.string.label_shelter_type, division),
        row(Res.string.label_org_name, orgName)
    ),
    tel = tel.clean() ?: fallbackTel.clean(),
    roadAddress = roadAddress.collapseSpaces(),
    jibunAddress = jibunAddress.collapseSpaces(),
    map = mapTarget(),
    hours = listOfNotNull(
        weekdayHours?.let { row(Res.string.label_hours_weekday, it.toUiText(), numeric = true) },
        weekendHours?.let { row(Res.string.label_hours_weekend, it.toUiText(), numeric = true) },
        weekdayAdoptionHours?.let {
            row(Res.string.label_adoption_hours_weekday, it.toUiText(), numeric = true)
        },
        weekendAdoptionHours?.let {
            row(Res.string.label_adoption_hours_weekend, it.toUiText(), numeric = true)
        },
        row(Res.string.label_closed_day, closedDay.plusSeparated())
    ),
    targetAnimals = targetAnimals.plusSeparated(),
    facilities = facilityLines(),
    designatedOn = designatedOn.clean(),
    dataDate = dataDate.clean()
)
