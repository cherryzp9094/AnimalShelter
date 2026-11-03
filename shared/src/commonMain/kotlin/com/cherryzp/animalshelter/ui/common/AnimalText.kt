package com.cherryzp.animalshelter.ui.common

import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.Neutered
import com.cherryzp.animalshelter.model.Sex
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.age_under_two_months
import com.cherryzp.animalshelter.resources.age_year
import com.cherryzp.animalshelter.resources.neuter_no
import com.cherryzp.animalshelter.resources.neuter_unknown
import com.cherryzp.animalshelter.resources.neuter_yes
import com.cherryzp.animalshelter.resources.sex_female
import com.cherryzp.animalshelter.resources.sex_male
import com.cherryzp.animalshelter.resources.sex_unknown
import com.cherryzp.animalshelter.resources.weight_kg
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

// 동물 문구·숫자 서식(순수 함수, 두 로케일 같음). 문장부호 " · "·", "는 코드에 둔다(브리핑 P12).

/** 부분을 잇는 구분자(제목·보호소 줄·특징). */
const val PART_SEPARATOR = " · "

/** 세 자리마다 쉼표(7544 → "7,544"). 두 로케일 같다. */
fun formatCount(value: Int): String {
    val digits = abs(value.toLong()).toString()
    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return if (value < 0) "-$grouped" else grouped
}

/** 체중 숫자(소수 둘째 자리까지, 끝의 0은 뺀다): 5.0 → "5", 2.90 → "2.9", 0.25 → "0.25". */
fun formatWeight(kg: Double): String {
    val hundredths = (kg * 100).roundToLong()
    val whole = hundredths / 100
    val fraction = (hundredths % 100).toString().padStart(2, '0').trimEnd('0')
    return if (fraction.isEmpty()) whole.toString() else "$whole.$fraction"
}

/** 날짜: 올해면 "MM-dd", 아니면 "yyyy-MM-dd". */
fun formatShortDate(date: LocalDate, today: LocalDate): String {
    val monthDay = "${date.month.number.pad2()}-${date.day.pad2()}"
    return if (date.year == today.year) monthDay else "${date.year}-$monthDay"
}

private fun Int.pad2(): String = toString().padStart(2, '0')

/** 성별 문구. */
fun Sex.toUiText(): UiText = when (this) {
    Sex.MALE -> uiText(Res.string.sex_male)
    Sex.FEMALE -> uiText(Res.string.sex_female)
    Sex.UNKNOWN -> uiText(Res.string.sex_unknown)
}

/** 중성화 문구. */
fun Neutered.toUiText(): UiText = when (this) {
    Neutered.YES -> uiText(Res.string.neuter_yes)
    Neutered.NO -> uiText(Res.string.neuter_no)
    Neutered.UNKNOWN -> uiText(Res.string.neuter_unknown)
}

/**
 * 제목 부분(브리핑 P10): 품종명 · 성별 · `age_year` · (60일 미만이면) `age_under_two_months`.
 * 있는 것만 — 빈 칸이 생기지 않게 리소스 하나(`card_title`)로 묶지 않는다. 카드 접근성 문장·사진 뷰어 제목·
 * (Phase 4) 공유 제목이 쓴다. 카드 글자는 [breedText]·[cardFactParts]로 나눠 보인다(F1).
 */
fun Animal.titleParts(): List<UiText> = buildList {
    breedText()?.let { add(UiText.Raw(it)) }
    sex?.let { add(it.toUiText()) }
    birthYear?.let { add(uiText(Res.string.age_year, it)) }
    if (isUnderTwoMonths) add(uiText(Res.string.age_under_two_months))
}

/** 카드 1행: 품종명(앞뒤 공백 제거). 없으면 null(행을 뺀다). */
fun Animal.breedText(): String? = kindName?.trim()?.takeIf { it.isNotEmpty() }

/**
 * 카드 2행(중간 검토 F1): 성별 · `age_year` · (60일 미만이면) `age_under_two_months` · 체중(`weight_kg`).
 * 있는 것만. 사진이 보여 주지 못하는 성별·나이를 품종과 다른 줄에 두어 2열에서도 잘리지 않게 한다.
 */
fun Animal.cardFactParts(): List<UiText> = buildList {
    sex?.let { add(it.toUiText()) }
    birthYear?.let { add(uiText(Res.string.age_year, it)) }
    if (isUnderTwoMonths) add(uiText(Res.string.age_under_two_months))
    weightKg?.let { add(uiText(Res.string.weight_kg, formatWeight(it))) }
}

/** 카드 4행(1열만): 색상 · 특징. 있는 것만. */
fun Animal.appearanceParts(): List<UiText> = buildList {
    color?.trim()?.takeIf { it.isNotEmpty() }?.let { add(UiText.Raw(it)) }
    specialMark.meaningfulText()?.let { add(UiText.Raw(it)) }
}

/**
 * 글자나 숫자가 하나라도 있는 값만(앞뒤 공백 제거). 실측에 특징이 "."처럼 문장부호뿐인 공고가 있다 —
 * 카드·상세에 빈 점이 보이지 않게 뺀다.
 */
fun String?.meaningfulText(): String? =
    this?.trim()?.takeIf { text -> text.any { it.isLetterOrDigit() } }

/** 연속 공백(줄바꿈·탭 포함)을 하나로 줄이고 앞뒤를 자른다. 비면 null. 주소 원문용(중간 검토 F9). */
fun String?.collapseSpaces(): String? =
    this?.replace(WHITESPACE_RUN, " ")?.trim()?.takeIf { it.isNotEmpty() }

private val WHITESPACE_RUN = Regex("""\s+""")

/** "일요일+공휴일" → "일요일, 공휴일": 원문의 `+` 구분자를 ", "로(구조 대상·휴무일, F9). 빈 부분은 뺀다. */
fun String?.plusSeparated(): String? = this?.split('+')?.mapNotNull { part ->
    part.trim().takeIf { it.isNotEmpty() }
}?.joinToString(", ")?.ifEmpty { null }

/** 보호소 줄: 관할 기관 · 보호소 이름. 둘 다 없으면 null. */
fun Animal.shelterLine(): String? = listOfNotNull(
    shelter.orgName?.trim()?.takeIf { it.isNotEmpty() },
    shelter.name?.trim()?.takeIf { it.isNotEmpty() }
).joinToString(PART_SEPARATOR).ifEmpty { null }

/**
 * 카드 접근성 한 문장(브리핑 P10, 06 6절): 제목 부분 · 보호소 줄 · (1열 카드면) 발견 장소 · 상태 ·
 * (북마크면) 북마크됨을 ", "로 잇는다. 보호소 줄의 " · "는 띄어쓰기로 읽는다. 빈 것은 뺀다.
 */
fun cardA11ySentence(
    title: List<String>,
    shelterLine: String?,
    status: String,
    bookmarked: String?,
    happenPlace: String? = null,
): String = buildList {
    addAll(title)
    shelterLine?.let { add(it.replace(PART_SEPARATOR, " ")) }
    happenPlace?.let { add(it) }
    add(status)
    bookmarked?.let { add(it) }
}.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")
