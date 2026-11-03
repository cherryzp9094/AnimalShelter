package com.cherryzp.animalshelter.ui.preview

import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.Sex
import com.cherryzp.animalshelter.model.ShelterSummary
import com.cherryzp.animalshelter.ui.common.AnimalCardUi
import com.cherryzp.animalshelter.ui.common.toCardUi
import kotlinx.datetime.LocalDate

/**
 * `@Preview`용 데이터(브리핑 P15). 테스트 소스셋의 `testAnimal`은 미리보기에서 못 쓰므로 여기 따로 둔다.
 * 사진 URL은 없다(placeholder가 보인다) — 미리보기는 네트워크를 쓰지 않는다.
 */
object PreviewData {
    val today: LocalDate = LocalDate(2026, 11, 4)

    private fun animal(
        no: String,
        processState: String,
        noticeEnd: LocalDate?,
        kindName: String = "한국 고양이",
        kind: AnimalKind = AnimalKind.CAT,
        sex: Sex = Sex.MALE,
    ) = Animal(
        desertionNo = no,
        noticeEnd = noticeEnd,
        happenDate = LocalDate(2026, 11, 3),
        happenPlace = "제주시 애월읍 어림비로 616",
        kind = kind,
        kindName = kindName,
        color = "기타(고등어)",
        birthYear = 2024,
        weightKg = 2.9,
        sex = sex,
        specialMark = "목에 플라스틱통 끼임, 사람을 잘 따름",
        processState = processState,
        shelter = ShelterSummary(name = "제2동물보호센터", orgName = "제주특별자치도")
    )

    /** 배지 네 갈래: 공고중 D-9 / 마감 임박 D-2 / 보호중 / 종료(반환). */
    val animals: List<Animal> = listOf(
        animal("450650202600001", "보호중", LocalDate(2026, 11, 13)),
        animal("450650202600002", "보호중", LocalDate(2026, 11, 6), "믹스견", AnimalKind.DOG, Sex.FEMALE),
        animal("450650202600003", "보호중", LocalDate(2026, 10, 20), "포메라니안", AnimalKind.DOG),
        animal("450650202600004", "종료(반환)", LocalDate(2026, 10, 30))
    )

    val cards: List<AnimalCardUi> = animals.mapIndexed { index, it ->
        it.toCardUi(today, ApiEndpoint.Fixture, isBookmarked = index == 1)
    }
}
