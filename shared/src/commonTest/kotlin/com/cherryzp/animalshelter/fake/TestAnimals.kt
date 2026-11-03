package com.cherryzp.animalshelter.fake

import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.Sex
import com.cherryzp.animalshelter.model.ShelterInfo
import com.cherryzp.animalshelter.model.ShelterSummary
import com.cherryzp.animalshelter.model.TimeRange
import kotlinx.datetime.LocalDate

/** 테스트 데이터(Phase 3 ViewModel 테스트용). 기본값은 2026-11-04 기준 공고중인 고양이. */
fun testAnimal(
    desertionNo: String = "450650202602738",
    processState: String? = "보호중",
    noticeEnd: LocalDate? = LocalDate(2026, 11, 13),
    happenDate: LocalDate? = LocalDate(2026, 11, 3),
    kind: AnimalKind? = AnimalKind.CAT,
    kindName: String? = "한국 고양이",
    sex: Sex? = Sex.MALE,
    birthYear: Int? = 2024,
    isUnderTwoMonths: Boolean = false,
    photos: List<String> = listOf(
        "https://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/10/a.jpg"
    ),
    shelter: ShelterSummary = ShelterSummary(
        regNo = "350000202000001",
        name = "제2동물보호센터",
        tel = "064-710-4805",
        address = "제주특별자치도 제주시 애월읍 어림비로 616-107",
        orgName = "제주특별자치도"
    ),
): Animal = Animal(
    desertionNo = desertionNo,
    noticeNo = "제주-제주-2026-00001",
    noticeStart = happenDate,
    noticeEnd = noticeEnd,
    happenDate = happenDate,
    happenPlace = "제주시 애월읍",
    kind = kind,
    kindCode = "000116",
    kindName = kindName,
    kindFullName = kindName?.let { "[고양이] $it" },
    color = "기타(고등어)",
    ageText = birthYear?.let { if (isUnderTwoMonths) "$it(60일미만)(년생)" else "$it(년생)" },
    birthYear = birthYear,
    isUnderTwoMonths = isUnderTwoMonths,
    weightText = "5(Kg)",
    weightKg = 5.0,
    sex = sex,
    specialMark = "목에 플라스틱통 끼임",
    processState = processState,
    photos = photos,
    shelter = shelter
)

/** 서로 다른 구조번호 n마리. */
fun testAnimals(count: Int, prefix: String = "4506502026"): List<Animal> =
    (1..count).map { testAnimal(desertionNo = prefix + it.toString().padStart(5, '0')) }

fun testShelterInfo(regNo: String = "350000202000001", name: String = "제2동물보호센터"): ShelterInfo =
    ShelterInfo(
        regNo = regNo,
        name = name,
        orgName = "제주특별자치도",
        roadAddress = "제주특별자치도 제주시 애월읍 어림비로 616-107",
        latitude = 33.45,
        longitude = 126.4,
        weekdayHours = TimeRange("09:00", "18:00"),
        tel = "064-710-4805"
    )
