package com.cherryzp.animalshelter.ui.detail

import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.data.repository.ImageUrlPolicy
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.AnimalStatus
import com.cherryzp.animalshelter.model.Neutered
import com.cherryzp.animalshelter.model.PromotionType
import com.cherryzp.animalshelter.model.Sex
import com.cherryzp.animalshelter.model.ShelterInfo
import com.cherryzp.animalshelter.model.statusOn
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.age_approx
import com.cherryzp.animalshelter.resources.age_under_one
import com.cherryzp.animalshelter.resources.age_under_two_months
import com.cherryzp.animalshelter.resources.age_year
import com.cherryzp.animalshelter.resources.choice_no
import com.cherryzp.animalshelter.resources.choice_unknown
import com.cherryzp.animalshelter.resources.choice_yes
import com.cherryzp.animalshelter.resources.date_range
import com.cherryzp.animalshelter.resources.detail_kind_cat
import com.cherryzp.animalshelter.resources.detail_kind_dog
import com.cherryzp.animalshelter.resources.detail_status_ended_hint
import com.cherryzp.animalshelter.resources.detail_status_noticing_hint
import com.cherryzp.animalshelter.resources.detail_status_protecting_hint
import com.cherryzp.animalshelter.resources.kind_other
import com.cherryzp.animalshelter.resources.label_age
import com.cherryzp.animalshelter.resources.label_color
import com.cherryzp.animalshelter.resources.label_data_updated
import com.cherryzp.animalshelter.resources.label_end_reason
import com.cherryzp.animalshelter.resources.label_happen_date
import com.cherryzp.animalshelter.resources.label_happen_place
import com.cherryzp.animalshelter.resources.label_health
import com.cherryzp.animalshelter.resources.label_health_check
import com.cherryzp.animalshelter.resources.label_neuter
import com.cherryzp.animalshelter.resources.label_notice_no
import com.cherryzp.animalshelter.resources.label_notice_period
import com.cherryzp.animalshelter.resources.label_rfid
import com.cherryzp.animalshelter.resources.label_sex
import com.cherryzp.animalshelter.resources.label_sociability
import com.cherryzp.animalshelter.resources.label_vaccination
import com.cherryzp.animalshelter.resources.label_weight
import com.cherryzp.animalshelter.resources.promo_adoption_process
import com.cherryzp.animalshelter.resources.promo_adoption_support
import com.cherryzp.animalshelter.resources.promo_event
import com.cherryzp.animalshelter.resources.promo_period
import com.cherryzp.animalshelter.resources.promo_volunteer
import com.cherryzp.animalshelter.resources.sex_female
import com.cherryzp.animalshelter.resources.sex_male
import com.cherryzp.animalshelter.resources.weight_kg
import com.cherryzp.animalshelter.ui.common.UiText
import com.cherryzp.animalshelter.ui.common.collapseSpaces
import com.cherryzp.animalshelter.ui.common.formatWeight
import com.cherryzp.animalshelter.ui.common.meaningfulText
import com.cherryzp.animalshelter.ui.common.pluralText
import com.cherryzp.animalshelter.ui.common.titleParts
import com.cherryzp.animalshelter.ui.common.toStatusUi
import com.cherryzp.animalshelter.ui.common.uiText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/** 비었거나 공백뿐이면 null. */
private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/** 값이 하나라도 있으면 행. */
private fun MutableList<DetailRow>.addRow(
    label: org.jetbrains.compose.resources.StringResource,
    vararg values: UiText?,
    numeric: Boolean = false,
) {
    val present = values.filterNotNull()
    if (present.isNotEmpty()) add(DetailRow(uiText(label), present, numeric = numeric))
}

private fun MutableList<DetailRow>.addRaw(
    label: org.jetbrains.compose.resources.StringResource,
    value: String?,
    numeric: Boolean = false,
) {
    value.clean()?.let { add(DetailRow(uiText(label), listOf(UiText.Raw(it)), numeric = numeric)) }
}

/** 날짜 "2026-11-03". */
fun LocalDate.isoText(): String =
    "$year-${month.number.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

/** 공고 기간 "2026-11-03 ~ 11-13" — 끝이 같은 해면 월-일만. 한쪽만 있으면 그것만. */
fun noticePeriodText(start: LocalDate?, end: LocalDate?): UiText? = when {
    start != null && end != null -> {
        val endText = if (end.year == start.year) end.isoText().drop(5) else end.isoText()
        uiText(Res.string.date_range, start.isoText(), endText)
    }

    start != null -> UiText.Raw(start.isoText())

    end != null -> UiText.Raw(end.isoText())

    else -> null
}

/** 수정 시각 원문 `2026-11-03 14:09:49.0` → "2026-11-03 14:09". 모양이 다르면 원문. */
fun updatedAtText(raw: String?): String? {
    val value = raw.clean() ?: return null
    val match = Regex("""^(\d{4}-\d{2}-\d{2})[ T](\d{2}:\d{2})""").find(value)
    return match?.let { "${it.groupValues[1]} ${it.groupValues[2]}" } ?: value
}

/** 나이 값: "2024년생" · "약 2살"(올해 − 출생 연도, 0이면 "1살 미만"), 60일 미만이면 "생후 2개월 미만". */
fun Animal.ageValues(today: LocalDate): List<UiText> {
    val year = birthYear ?: return listOfNotNull(ageText.clean()?.let { UiText.Raw(it) })
    val approx = when {
        isUnderTwoMonths -> uiText(Res.string.age_under_two_months)
        today.year - year <= 0 -> uiText(Res.string.age_under_one)
        else -> pluralText(Res.plurals.age_approx, today.year - year)
    }
    return listOf(uiText(Res.string.age_year, year), approx)
}

/**
 * 상세 요약의 성별 값(중간 검토 F2). 라벨("성별") 아래라 낱말을 되풀이하지 않는다 — 미상은 `choice_unknown`.
 * 요약 칩·카드처럼 혼자 읽히는 곳은 [com.cherryzp.animalshelter.ui.common.toUiText](`sex_*`)를 쓴다.
 */
fun Sex.detailValue(): UiText = when (this) {
    Sex.MALE -> uiText(Res.string.sex_male)
    Sex.FEMALE -> uiText(Res.string.sex_female)
    Sex.UNKNOWN -> uiText(Res.string.choice_unknown)
}

/** 상세 요약의 중성화 값(F2): 필터 선택지와 같은 예 / 아니요 / 미상. 요약 칩은 `neuter_*`. */
fun Neutered.detailValue(): UiText = when (this) {
    Neutered.YES -> uiText(Res.string.choice_yes)
    Neutered.NO -> uiText(Res.string.choice_no)
    Neutered.UNKNOWN -> uiText(Res.string.choice_unknown)
}

/** 축종 보조 제목(단수 — 필터 선택지 `kind_*`의 en 복수형 "Dogs"를 쓰지 않는다, F8). */
fun AnimalKind.toUiText(): UiText = when (this) {
    AnimalKind.DOG -> uiText(Res.string.detail_kind_dog)
    AnimalKind.CAT -> uiText(Res.string.detail_kind_cat)
    AnimalKind.OTHER -> uiText(Res.string.kind_other)
}

/** 상태 보조 문구(03 2.3 섹션 2). 미상이면 없음. */
fun AnimalStatus.hint(): UiText? = when (this) {
    is AnimalStatus.Noticing -> uiText(Res.string.detail_status_noticing_hint)
    AnimalStatus.Protecting -> uiText(Res.string.detail_status_protecting_hint)
    is AnimalStatus.Ended -> uiText(Res.string.detail_status_ended_hint)
    is AnimalStatus.Unknown -> null
}

private fun PromotionType.toUiText(): UiText = when (this) {
    PromotionType.ADOPTION_PROCESS -> uiText(Res.string.promo_adoption_process)
    PromotionType.ADOPTION_SUPPORT -> uiText(Res.string.promo_adoption_support)
    PromotionType.VOLUNTEER -> uiText(Res.string.promo_volunteer)
    PromotionType.EVENT -> uiText(Res.string.promo_event)
}

/**
 * [Animal] → [AnimalDetailUi](03 2.3 섹션 1~8). 값이 없는 행은 빼고, 비는 섹션은 화면이 숨긴다.
 * `etcBigo`는 보이지 않는다(실측 전부 1글자). `today`는 서울 오늘, `endpoint`는 지금 엔드포인트(사진 URL).
 */
fun Animal.toDetailUi(today: LocalDate, endpoint: ApiEndpoint): AnimalDetailUi {
    val status = statusOn(today)
    return AnimalDetailUi(
        desertionNo = desertionNo,
        photoUrls = photos.map { ImageUrlPolicy.displayUrl(it, endpoint) },
        photoOriginals = photos.map(ImageUrlPolicy::normalize),
        titleParts = titleParts(),
        name = kindName.clean(),
        kindLabel = kind?.toUiText(),
        summary = buildList {
            addRow(Res.string.label_sex, sex?.detailValue())
            addRow(Res.string.label_neuter, neutered?.detailValue())
            addRow(Res.string.label_age, *ageValues(today).toTypedArray(), numeric = true)
            addRow(
                Res.string.label_weight,
                weightKg?.let { uiText(Res.string.weight_kg, formatWeight(it)) }
                    ?: weightText.clean()?.let { UiText.Raw(it) },
                numeric = true
            )
            addRaw(Res.string.label_color, color)
        },
        status = status.toStatusUi(),
        statusHint = status.hint(),
        notice = buildList {
            addRow(
                Res.string.label_notice_period,
                noticePeriodText(noticeStart, noticeEnd),
                numeric = true
            )
            addRaw(Res.string.label_notice_no, noticeNo, numeric = true)
            if (status is AnimalStatus.Ended) {
                addRaw(Res.string.label_end_reason, endReason.clean() ?: status.reason)
            }
            addRaw(Res.string.label_data_updated, updatedAtText(updatedAt), numeric = true)
        },
        traits = specialMark.meaningfulText(),
        found = buildList {
            addRaw(Res.string.label_happen_date, happenDate?.isoText(), numeric = true)
            addRaw(Res.string.label_happen_place, happenPlace.collapseSpaces())
        },
        health = buildList {
            vaccinationChecks.mapNotNull { it.clean() }.takeIf { it.isNotEmpty() }?.let {
                add(DetailRow(uiText(Res.string.label_vaccination), chips = it))
            }
            healthChecks.mapNotNull { it.clean() }.takeIf { it.isNotEmpty() }?.let {
                add(DetailRow(uiText(Res.string.label_health_check), chips = it))
            }
            addRaw(Res.string.label_sociability, sociability)
            addRaw(Res.string.label_health, health)
            addRaw(Res.string.label_rfid, rfid, numeric = true)
        },
        shelter = DetailShelterUi(
            regNo = shelter.regNo.clean(),
            name = shelter.name.clean(),
            tel = shelter.tel.clean(),
            address = shelter.address.collapseSpaces(),
            orgName = shelter.orgName.clean()
        ).takeIf { it.name != null || it.tel != null || it.address != null || it.regNo != null },
        promotions = promotions.map { promo ->
            val start = promo.start.clean()
            val end = promo.end.clean()
            PromotionUi(
                type = promo.type.toUiText(),
                title = promo.title.clean(),
                period = if (start != null && end != null) {
                    uiText(Res.string.promo_period, start, end)
                } else {
                    (start ?: end)?.let { UiText.Raw(it) }
                },
                condition = promo.condition.clean(),
                text = promo.text.clean(),
                imageUrl = promo.imageUrl.clean()?.let { ImageUrlPolicy.displayUrl(it, endpoint) }
            )
        }
    )
}

/** 보호소 좌표 → 지도 대상. 좌표가 없으면 null(지도 버튼 숨김). */
fun ShelterInfo.mapTarget(): MapTarget? {
    val lat = latitude ?: return null
    val lng = longitude ?: return null
    return MapTarget(lat, lng, name)
}
