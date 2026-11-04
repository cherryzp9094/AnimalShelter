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
private val WEIGHT_NUMBER = Regex("""^([\d.,]+)\s*(?:~\s*([\d.,]+))?""")
private val SEPARATOR_RUN = Regex("""[.,]+""")
private val MASKED = Regex("""^\*+$""")
private const val UNDER_TWO_MONTHS = "60일미만"
private const val MAX_WEIGHT_KG = 100.0

/** 구조번호가 없으면 null(목록에서 뺀다). */
internal fun AbandonmentItemDto.toAnimal(): Animal? {
    val no = desertionNo.clean() ?: return null
    val ageRaw = age.clean()
    val weightRaw = weight.clean()
    val parsedWeight = parseWeight(weightRaw)
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
        weightKg = parsedWeight?.kg,
        weightMaxKg = parsedWeight?.maxKg,
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

/** 체중 원문을 읽은 값. 범위형(`2~2.5(Kg)`)이면 [maxKg]가 뒤 값, 아니면 null. */
internal data class ParsedWeight(val kg: Double, val maxKg: Double?)

/**
 * 체중 원문 → kg(02-data 5.1). 앞의 숫자, 그리고 `~` 뒤의 숫자(범위형 — 5a-live M2)를 잡는다. 숫자마다 연속된
 * `.`·`,`를 `.` 하나로 정리한다(실측 오타 `2..00(Kg)`, `3,5(Kg)`). 유효 = 0 초과 100kg 이하.
 * 범위는 둘 다 유효하고 앞 < 뒤일 때만(같으면 값 하나), 역순·한쪽 무효면 null. g으로 고쳐 읽지 않는다(05 2.3).
 */
internal fun parseWeight(text: String?): ParsedWeight? {
    val match = text?.trim()?.let { WEIGHT_NUMBER.find(it) } ?: return null
    val first = weightNumber(match.groupValues[1]) ?: return null
    val secondText = match.groupValues[2]
    if (secondText.isEmpty()) return ParsedWeight(first, null)
    val second = weightNumber(secondText) ?: return null
    return when {
        first < second -> ParsedWeight(first, second)
        first == second -> ParsedWeight(first, null)
        else -> null
    }
}

private fun weightNumber(text: String): Double? = text.replace(SEPARATOR_RUN, ".").trim('.')
    .toDoubleOrNull()?.takeIf { it > 0.0 && it <= MAX_WEIGHT_KG }

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
