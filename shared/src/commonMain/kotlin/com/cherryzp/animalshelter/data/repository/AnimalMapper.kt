package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.dto.AbandonmentItemDto
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.Neutered
import com.cherryzp.animalshelter.model.Promotion
import com.cherryzp.animalshelter.model.PromotionType
import com.cherryzp.animalshelter.model.Sex
import com.cherryzp.animalshelter.model.ShelterSummary
import kotlinx.datetime.LocalDate

// `abandonmentPublic_v2` item → Animal(02-data 5.1). 버리는 필드: etcBigo(전부 1글자), careOwnerNm(개인 이름 가능).

private val YEAR = Regex("""^(\d{4})""")
private val WEIGHT_NUMBER = Regex("""^([\d.,]+)""")
private val SEPARATOR_RUN = Regex("""[.,]+""")
private val MASKED = Regex("""^\*+$""")
private const val UNDER_TWO_MONTHS = "60일미만"
private const val MAX_WEIGHT_KG = 100.0

/** 구조번호가 없으면 null(목록에서 뺀다). */
internal fun AbandonmentItemDto.toAnimal(): Animal? {
    val no = desertionNo.clean() ?: return null
    val ageRaw = age.clean()
    val weightRaw = weight.clean()
    return Animal(
        desertionNo = no,
        noticeNo = noticeNo.clean(),
        noticeStart = parseBasicDate(noticeSdt),
        noticeEnd = parseBasicDate(noticeEdt),
        happenDate = parseBasicDate(happenDt),
        happenPlace = happenPlace.clean(),
        kind = AnimalKind.fromCode(upKindCd),
        kindCode = kindCd.clean(),
        kindName = kindNm.clean(),
        kindFullName = kindFullNm.clean(),
        color = colorCd.clean(),
        ageText = ageRaw,
        birthYear = ageRaw?.let { YEAR.find(it)?.groupValues?.get(1)?.toIntOrNull() },
        isUnderTwoMonths = ageRaw?.contains(UNDER_TWO_MONTHS) == true,
        weightText = weightRaw,
        weightKg = parseWeightKg(weightRaw),
        sex = Sex.fromCode(sexCd),
        neutered = Neutered.fromCode(neuterYn),
        specialMark = specialMark.clean(),
        sociability = sfeSoci.clean(),
        health = sfeHealth.clean(),
        vaccinationChecks = splitChecks(vaccinationChk),
        healthChecks = splitChecks(healthChk),
        processState = processState.clean(),
        endReason = endReason.clean(),
        updatedAt = updTm.clean(),
        photos = listOf(
            popfile1,
            popfile2,
            popfile3,
            popfile4,
            popfile5,
            popfile6,
            popfile7,
            popfile8
        )
            .mapNotNull { it.clean()?.let(ImageUrlPolicy::normalize) }
            .distinct(),
        shelter = ShelterSummary(
            regNo = careRegNo.clean(),
            name = careNm.clean(),
            tel = careTel.clean()?.takeUnless { MASKED.matches(it) },
            address = careAddr.clean(),
            orgName = orgNm.clean()
        ),
        promotions = listOfNotNull(
            promotion(
                PromotionType.ADOPTION_PROCESS,
                adptnTitle,
                adptnSDate,
                adptnEDate,
                adptnConditionLimitTxt,
                adptnTxt,
                adptnImg
            ),
            promotion(
                PromotionType.ADOPTION_SUPPORT,
                sprtTitle,
                sprtSDate,
                sprtEDate,
                sprtConditionLimitTxt,
                sprtTxt,
                sprtImg
            ),
            promotion(
                PromotionType.VOLUNTEER,
                srvcTitle,
                srvcSDate,
                srvcEDate,
                srvcConditionLimitTxt,
                srvcTxt,
                srvcImg
            ),
            promotion(
                PromotionType.EVENT,
                evntTitle,
                evntSDate,
                evntEDate,
                evntConditionLimitTxt,
                evntTxt,
                evntImg
            )
        ),
        rfid = rfidCd.clean()
    )
}

/** 앞뒤 공백을 지우고 빈 값은 null. */
internal fun String?.clean(): String? = this?.trim()?.ifEmpty { null }

/** `YYYYMMDD` → 날짜. 형식이 다르면 null. */
internal fun parseBasicDate(text: String?): LocalDate? =
    text.clean()?.let { LocalDate.Formats.ISO_BASIC.parseOrNull(it) }

/**
 * 체중 원문 → kg. 앞의 숫자 부분을 잡아 연속된 `.`·`,`를 `.` 하나로 정리한다(실측 오타 `2..00(Kg)`, `3,5(Kg)`).
 * 읽을 수 없거나 0 이하·100kg 초과면 null.
 */
internal fun parseWeightKg(text: String?): Double? {
    val number = text?.let { WEIGHT_NUMBER.find(it)?.groupValues?.get(1) } ?: return null
    val value = number.replace(SEPARATOR_RUN, ".").trim('.').toDoubleOrNull() ?: return null
    return value.takeIf { it > 0.0 && it <= MAX_WEIGHT_KG }
}

/** 쉼표 구분 체크 항목 → 목록(빈 항목 제거). */
internal fun splitChecks(text: String?): List<String> =
    text?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

private fun promotion(
    type: PromotionType,
    title: String?,
    start: String?,
    end: String?,
    condition: String?,
    text: String?,
    image: String?,
): Promotion? {
    val cleanTitle = title.clean() ?: return null
    return Promotion(
        type = type,
        title = cleanTitle,
        start = start.clean(),
        end = end.clean(),
        condition = condition.clean(),
        text = text.clean(),
        imageUrl = image.clean()?.let(ImageUrlPolicy::normalize)
    )
}
