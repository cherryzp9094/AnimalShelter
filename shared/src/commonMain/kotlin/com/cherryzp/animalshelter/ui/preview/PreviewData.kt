package com.cherryzp.animalshelter.ui.preview

import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.Neutered
import com.cherryzp.animalshelter.model.Promotion
import com.cherryzp.animalshelter.model.PromotionType
import com.cherryzp.animalshelter.model.Sex
import com.cherryzp.animalshelter.model.ShelterSummary
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.hours_range
import com.cherryzp.animalshelter.resources.today_closed
import com.cherryzp.animalshelter.resources.today_hours
import com.cherryzp.animalshelter.ui.common.AnimalCardUi
import com.cherryzp.animalshelter.ui.common.toCardUi
import com.cherryzp.animalshelter.ui.common.uiText
import com.cherryzp.animalshelter.ui.detail.AnimalDetailUiState
import com.cherryzp.animalshelter.ui.detail.MapTarget
import com.cherryzp.animalshelter.ui.detail.toDetailUi
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

    /** 상세: 값이 많은 공고(건강·안내 섹션 포함, 사진 2장 자리). 대표자 이름은 두지 않는다. */
    val richAnimal: Animal = animals[0].copy(
        noticeNo = "제주-제주-2026-01234",
        noticeStart = LocalDate(2026, 11, 3),
        neutered = Neutered.NO,
        updatedAt = "2026-11-03 14:09:49.0",
        vaccinationChecks = listOf("종합백신", "광견병"),
        healthChecks = listOf("기생충 검사"),
        sociability = "사람을 잘 따름",
        health = "양호",
        photos = listOf("https://example.invalid/a.jpg", "https://example.invalid/b.jpg"),
        shelter = ShelterSummary(
            regNo = "311322200900001",
            name = "제2동물보호센터",
            tel = "064-710-4805",
            address = "제주특별자치도 제주시 애월읍 어림비로 616-107",
            orgName = "제주특별자치도"
        ),
        promotions = listOf(
            Promotion(
                type = PromotionType.ADOPTION_PROCESS,
                title = "입양 절차 안내",
                start = "2026-11-01",
                end = "2026-12-31",
                condition = "만 19세 이상",
                text = "방문 상담 뒤 입양 신청서를 씁니다."
            )
        )
    )

    /** 상세: 값이 적은 공고(요약·공고·출처만). */
    val sparseAnimal: Animal = Animal(
        desertionNo = "450650202600009",
        processState = "종료(반환)",
        kind = AnimalKind.DOG,
        kindName = "믹스견"
    )

    /** 상세 상태(보호소 오늘 운영시간·지도 포함). */
    fun detailState(animal: Animal, bookmarked: Boolean = false): AnimalDetailUiState =
        AnimalDetailUiState(
            detail = animal.toDetailUi(today, ApiEndpoint.Fixture),
            isBookmarked = bookmarked,
            shelterToday = if (animal.shelter.regNo != null) {
                listOf(
                    uiText(
                        Res.string.today_hours,
                        uiText(Res.string.hours_range, "09:00", "18:00")
                    ),
                    uiText(Res.string.today_closed, "일요일")
                )
            } else {
                emptyList()
            },
            map = if (animal.shelter.regNo != null) MapTarget(33.45, 126.4, "제2동물보호센터") else null
        )
}
