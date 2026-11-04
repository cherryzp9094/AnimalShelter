package com.cherryzp.animalshelter.data.repository

import com.cherryzp.animalshelter.data.api.ApiJson
import com.cherryzp.animalshelter.data.api.dto.AbandonmentItemDto
import com.cherryzp.animalshelter.data.api.dto.ApiEnvelope
import com.cherryzp.animalshelter.data.api.fixture.ApiFixtures
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.Neutered
import com.cherryzp.animalshelter.model.PromotionType
import com.cherryzp.animalshelter.model.Sex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

/** 목록 item 매퍼(02-data 5.1). */
class AnimalMapperTest {
    private val fixtureAnimals = ApiJson.decodeFromString<ApiEnvelope<AbandonmentItemDto>>(
        ApiFixtures.abandonmentPublicV2P1
    ).response.body!!.items.mapNotNull { it.toAnimal() }

    @Test
    fun `실제 응답 5건을 매핑한다`() {
        assertEquals(5, fixtureAnimals.size)
        val first = fixtureAnimals.first()
        assertEquals("450650202602738", first.desertionNo)
        assertEquals(2, first.photos.size)
        assertEquals(Sex.MALE, first.sex)
        assertEquals(Neutered.YES, first.neutered)
        assertEquals(AnimalKind.CAT, first.kind)
        assertEquals(LocalDate(2026, 11, 13), first.noticeEnd)
        assertEquals(LocalDate(2026, 11, 3), first.noticeStart)
        assertEquals(LocalDate(2026, 11, 3), first.happenDate)
        assertEquals(2024, first.birthYear)
        assertEquals(5.0, first.weightKg)
        assertEquals("제2동물보호센터", first.shelter.name)
        // 끝 공백은 지운다
        assertEquals("제주특별자치도 제주시 애월읍 어림비로 616-107", first.shelter.address)
        assertTrue(first.photos.all { it.startsWith("https://openapi.animal.go.kr/") })
        assertEquals("종료(반환)", fixtureAnimals.last().processState)
    }

    @Test
    fun `나이 두 형식`() {
        val plain = AbandonmentItemDto(desertionNo = "1", age = "2024(년생)").toAnimal()!!
        val young = AbandonmentItemDto(desertionNo = "1", age = "2026(60일미만)(년생)").toAnimal()!!

        assertEquals(2024, plain.birthYear)
        assertFalse(plain.isUnderTwoMonths)
        assertEquals(2026, young.birthYear)
        assertTrue(young.isUnderTwoMonths)
        assertEquals("2026(60일미만)(년생)", young.ageText)
    }

    @Test
    fun `체중 오타를 정리하고 0과 100kg 초과는 null`() {
        assertEquals(ParsedWeight(2.0, null), parseWeight("2..00(Kg)"))
        assertEquals(ParsedWeight(3.5, null), parseWeight("3,5(Kg)"))
        assertEquals(ParsedWeight(0.7, null), parseWeight("0.7(Kg)"))
        assertEquals(ParsedWeight(12.0, null), parseWeight("12(Kg)"))
        assertNull(parseWeight("400(Kg)"))
        assertNull(parseWeight("0(Kg)"))
        assertNull(parseWeight("(Kg)"))
        assertNull(parseWeight(null))
        val animal = AbandonmentItemDto(desertionNo = "1", weight = "400(Kg)").toAnimal()!!
        assertNull(animal.weightKg)
        assertNull(animal.weightMaxKg)
        assertEquals("400(Kg)", animal.weightText)
    }

    @Test
    fun `체중 범위형은 앞·뒤 값, 같으면 하나, 역순·무효는 null(L1)`() {
        assertEquals(ParsedWeight(2.0, 2.5), parseWeight("2~2.5(Kg)"))
        assertEquals(ParsedWeight(1.0, 1.5), parseWeight("1~1.5(Kg)"))
        assertEquals(ParsedWeight(2.0, 2.5), parseWeight("2 ~ 2.5(Kg)"))
        assertEquals(ParsedWeight(2.0, null), parseWeight("2~2(Kg)"))
        assertNull(parseWeight("2.5~2(Kg)"))
        assertNull(parseWeight("0~1(Kg)"))
        assertNull(parseWeight("300~450(Kg)"))

        val animal = AbandonmentItemDto(desertionNo = "1", weight = "2~2.5(Kg)").toAnimal()!!
        assertEquals(2.0, animal.weightKg)
        assertEquals(2.5, animal.weightMaxKg)
        assertEquals("2~2.5(Kg)", animal.weightText)
    }

    @Test
    fun `체크 항목은 쉼표로 나누고 빈 항목을 버린다`() {
        val animal = AbandonmentItemDto(
            desertionNo = "1",
            vaccinationChk = "종합백신, 코로나,,",
            healthChk = " "
        ).toAnimal()!!

        assertEquals(listOf("종합백신", "코로나"), animal.vaccinationChecks)
        assertEquals(emptyList(), animal.healthChecks)
    }

    @Test
    fun `사진은 순서대로 중복 없이 https로`() {
        val base = "http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/" +
            "shelter/2026/10/"
        val animal = AbandonmentItemDto(
            desertionNo = "1",
            popfile1 = "${base}a.jpg",
            popfile2 = "${base}a.jpg",
            popfile3 = "",
            popfile5 = "${base}b[1].png"
        ).toAnimal()!!

        val httpsBase = base.replace("http://", "https://")
        assertEquals(listOf("${httpsBase}a.jpg", "${httpsBase}b[1].png"), animal.photos)
    }

    @Test
    fun `빈 문자열은 null, 모르는 코드는 null, 구조번호가 없으면 버린다`() {
        val animal = AbandonmentItemDto(
            desertionNo = "1",
            kindNm = " ",
            sexCd = "X",
            upKindCd = "999",
            noticeEdt = "2026-11-13"
        ).toAnimal()!!

        assertNull(animal.kindName)
        assertNull(animal.sex)
        assertNull(animal.kind)
        assertNull(animal.noticeEnd)
        assertNull(AbandonmentItemDto(desertionNo = " ").toAnimal())
    }

    @Test
    fun `홍보 묶음은 제목이 있을 때만`() {
        val animal = AbandonmentItemDto(
            desertionNo = "1",
            adptnTitle = "입양 절차",
            adptnTxt = "방문 상담",
            srvcTitle = "",
            srvcTxt = "버려지는 내용"
        ).toAnimal()!!

        assertEquals(1, animal.promotions.size)
        assertEquals(PromotionType.ADOPTION_PROCESS, animal.promotions.single().type)
        assertEquals("방문 상담", animal.promotions.single().text)
    }
}
