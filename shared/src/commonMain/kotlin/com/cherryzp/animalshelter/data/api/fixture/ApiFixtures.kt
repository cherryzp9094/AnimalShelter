// 생성 파일: python3 -I tools/gen_fixtures.py (직접 고치지 않는다)
// 원본: docs/api/samples (실제 응답, 인증키 없음). Fixture 엔드포인트와 테스트가 같이 쓴다.
@file:Suppress("ktlint")

package com.cherryzp.animalshelter.data.api.fixture

internal object ApiFixtures {
    /** docs/api/samples/live-2026-11-03/abandonmentPublic_v2_empty.json */
    val abandonmentPublicV2Empty: String = """
{
  "response" : {
    "header" : {
      "reqNo" : 64554594,
      "resultCode" : "00",
      "resultMsg" : "NORMAL SERVICE."
    },
    "body" : {
      "items" : { },
      "numOfRows" : 5,
      "pageNo" : 1,
      "totalCount" : 0
    }
  }
}
"""

    /** docs/api/samples/live-2026-11-03/abandonmentPublic_v2_p1.json */
    val abandonmentPublicV2P1: String = """
{
  "response" : {
    "header" : {
      "reqNo" : 64554591,
      "resultCode" : "00",
      "resultMsg" : "NORMAL SERVICE."
    },
    "body" : {
      "items" : {
        "item" : [ {
          "desertionNo" : "450650202602738",
          "happenDt" : "20261103",
          "happenPlace" : "제주시 애월읍 하가로2길 32-1",
          "kindFullNm" : "[고양이] 한국 고양이",
          "upKindCd" : "422400",
          "upKindNm" : "고양이",
          "kindCd" : "000200",
          "kindNm" : "한국 고양이",
          "colorCd" : "기타(고등어)",
          "age" : "2024(년생)",
          "weight" : "5(Kg)",
          "noticeNo" : "제주-제주-2026-02647",
          "noticeSdt" : "20261103",
          "noticeEdt" : "20261113",
          "popfile1" : "http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/10/20261103141175.jpg",
          "popfile2" : "http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/10/20261103141190.jpg",
          "processState" : "보호중",
          "sexCd" : "M",
          "neuterYn" : "Y",
          "specialMark" : "개체관리번호 26652 - 목에 플라스틱통 끼임",
          "careRegNo" : "350650202600001",
          "careNm" : "제2동물보호센터",
          "careTel" : "064-710-4805",
          "careAddr" : "제주특별자치도 제주시 애월읍 어림비로 616-107  ",
          "careOwnerNm" : "(가림)",
          "orgNm" : "제주특별자치도",
          "updTm" : "2026-11-03 14:09:49.0"
        }, {
          "desertionNo" : "450650202602737",
          "happenDt" : "20261103",
          "happenPlace" : "서귀포시 안덕면 사계로218",
          "kindFullNm" : "[고양이] 한국 고양이",
          "upKindCd" : "422400",
          "upKindNm" : "고양이",
          "kindCd" : "000200",
          "kindNm" : "한국 고양이",
          "colorCd" : "기타(흰갈)",
          "age" : "2026(년생)",
          "weight" : "0.7(Kg)",
          "noticeNo" : "제주-제주-2026-02646",
          "noticeSdt" : "20261103",
          "noticeEdt" : "20261113",
          "popfile1" : "http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/10/202611031411477.jpg",
          "popfile2" : "http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/10/202611031411491.jpg",
          "processState" : "보호중",
          "sexCd" : "M",
          "neuterYn" : "N",
          "specialMark" : "개체관리번호 26651 - 3개월",
          "careRegNo" : "350650202600001",
          "careNm" : "제2동물보호센터",
          "careTel" : "064-710-4805",
          "careAddr" : "제주특별자치도 제주시 애월읍 어림비로 616-107  ",
          "careOwnerNm" : "(가림)",
          "orgNm" : "제주특별자치도",
          "updTm" : "2026-11-03 14:09:49.0"
        }, {
          "desertionNo" : "450650202602736",
          "happenDt" : "20261103",
          "happenPlace" : "서귀포시 성산읍 수산리4083",
          "kindFullNm" : "[고양이] 한국 고양이",
          "upKindCd" : "422400",
          "upKindNm" : "고양이",
          "kindCd" : "000200",
          "kindNm" : "한국 고양이",
          "colorCd" : "기타(치즈)",
          "age" : "2026(년생)",
          "weight" : "1(Kg)",
          "noticeNo" : "제주-제주-2026-02645",
          "noticeSdt" : "20261103",
          "noticeEdt" : "20261113",
          "popfile1" : "http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/10/202611031411996.jpg",
          "popfile2" : "http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/10/20261103141110.jpg",
          "processState" : "보호중",
          "sexCd" : "F",
          "neuterYn" : "N",
          "specialMark" : "개체관리번호 26650 - 4개월",
          "careRegNo" : "350650202600001",
          "careNm" : "제2동물보호센터",
          "careTel" : "064-710-4805",
          "careAddr" : "제주특별자치도 제주시 애월읍 어림비로 616-107  ",
          "careOwnerNm" : "(가림)",
          "orgNm" : "제주특별자치도",
          "updTm" : "2026-11-03 14:09:49.0"
        }, {
          "desertionNo" : "450650202602735",
          "happenDt" : "20261103",
          "happenPlace" : "제주시 화북일동 진남로10길44",
          "kindFullNm" : "[고양이] 믹스묘",
          "upKindCd" : "422400",
          "upKindNm" : "고양이",
          "kindCd" : "000212",
          "kindNm" : "믹스묘",
          "colorCd" : "기타(갈흰)",
          "age" : "2023(년생)",
          "weight" : "3(Kg)",
          "noticeNo" : "제주-제주-2026-02644",
          "noticeSdt" : "20261103",
          "noticeEdt" : "20261113",
          "popfile1" : "http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/10/202611031411286.jpg",
          "popfile2" : "http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/10/202611031411299.jpg",
          "processState" : "보호중",
          "sexCd" : "M",
          "neuterYn" : "N",
          "specialMark" : "개체관리번호 26649 - 유기묘/칩없음",
          "careRegNo" : "350650202600001",
          "careNm" : "제2동물보호센터",
          "careTel" : "064-710-4805",
          "careAddr" : "제주특별자치도 제주시 애월읍 어림비로 616-107  ",
          "careOwnerNm" : "(가림)",
          "orgNm" : "제주특별자치도",
          "updTm" : "2026-11-03 14:28:21.0"
        }, {
          "desertionNo" : "448567202601800",
          "happenDt" : "20261103",
          "happenPlace" : "의창구 동읍 신방리 칠성그린아파트 내",
          "kindFullNm" : "[개] 말티즈",
          "upKindCd" : "417000",
          "upKindNm" : "개",
          "kindCd" : "000072",
          "kindNm" : "말티즈",
          "colorCd" : "흰색",
          "age" : "2020(년생)",
          "weight" : "5.6(Kg)",
          "noticeNo" : "경남-창원1-2026-00782",
          "noticeSdt" : "20261103",
          "noticeEdt" : "20261113",
          "popfile1" : "http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/10/202611031111593.jpg",
          "popfile2" : "http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/10/202611031111662.jpg",
          "processState" : "종료(반환)",
          "sexCd" : "F",
          "neuterYn" : "U",
          "specialMark" : "미용되어 있음, 희미한 샴푸 냄새, 체리로 된 땡땡이 옷 착용",
          "careRegNo" : "348527200900001",
          "careNm" : "창원동물보호센터",
          "careTel" : "055-225-5701",
          "careAddr" : "경상남도 창원시 성산구 공단로474번길 117 (상복동) 창원동물보호센터",
          "careOwnerNm" : "(가림)",
          "orgNm" : "경상남도 창원시 의창성산구",
          "updTm" : "2026-11-03 12:30:48.0"
        } ]
      },
      "numOfRows" : 5,
      "pageNo" : 1,
      "totalCount" : 7544
    }
  }
}
"""

    /** docs/api/samples/live-2026-11-03/kind_v2_dog.json */
    val kindV2Dog: String = """
{
  "response" : {
    "header" : {
      "reqNo" : 64554590,
      "resultCode" : "00",
      "resultMsg" : "NORMAL SERVICE."
    },
    "body" : {
      "items" : {
        "item" : [ {
          "kindCd" : "000245",
          "kindNm" : "고든 세터"
        }, {
          "kindCd" : "000054",
          "kindNm" : "골든 리트리버"
        }, {
          "kindCd" : "000056",
          "kindNm" : "그레이 하운드"
        }, {
          "kindCd" : "000055",
          "kindNm" : "그레이트 덴"
        }, {
          "kindCd" : "000118",
          "kindNm" : "그레이트 피레니즈"
        }, {
          "kindCd" : "000249",
          "kindNm" : "그리펀 벨지언"
        }, {
          "kindCd" : "000115",
          "kindNm" : "기타"
        }, {
          "kindCd" : "000037",
          "kindNm" : "꼬똥 드 뚤레아"
        }, {
          "kindCd" : "000081",
          "kindNm" : "네오폴리탄 마스티프"
        }, {
          "kindCd" : "000204",
          "kindNm" : "노르포크 테리어"
        } ]
      },
      "numOfRows" : 10,
      "pageNo" : 1,
      "totalCount" : 206
    }
  }
}
"""

    /** docs/api/samples/live-2026-11-03/shelterInfo_v2.json */
    val shelterInfoV2: String = """
{
  "response" : {
    "header" : {
      "reqNo" : 64554595,
      "resultCode" : "00",
      "resultMsg" : "NORMAL SERVICE."
    },
    "body" : {
      "items" : {
        "item" : [ {
          "careNm" : "(사)대구수의사회",
          "careRegNo" : "327346201500001",
          "orgNm" : "대구광역시 수성구",
          "divisionNm" : "법인",
          "saveTrgtAnimal" : "개+고양이+기타",
          "careAddr" : "대구광역시 북구 호국로 229 (서변동) 6층",
          "jibunAddr" : " 6층",
          "lat" : 35.922276,
          "lng" : 128.59904,
          "dsignationDate" : "2017-01-01",
          "weekOprStime" : "00:00",
          "weekOprEtime" : "24:00",
          "closeDay" : "0",
          "vetPersonCnt" : 0,
          "specsPersonCnt" : 0,
          "careTel" : "053-764-3708",
          "dataStdDt" : "2025-01-03"
        }, {
          "careNm" : "(사)동물보호관리협회",
          "careRegNo" : "348535202400001",
          "orgNm" : "경상남도 김해시",
          "divisionNm" : "법인",
          "saveTrgtAnimal" : "개+고양이+기타",
          "careAddr" : "부산광역시 강서구 가락대로1283번길 25-2 (봉림동) ",
          "jibunAddr" : " ",
          "lat" : 35.178925,
          "lng" : 128.89975,
          "dsignationDate" : "2025-01-01",
          "weekOprStime" : "09:00",
          "weekOprEtime" : "17:00",
          "weekCellStime" : "13:00",
          "weekCellEtime" : "15:00",
          "closeDay" : "토요일+일요일",
          "vetPersonCnt" : 1,
          "specsPersonCnt" : 6,
          "medicalCnt" : 1,
          "breedCnt" : 1,
          "quarabtineCnt" : 1,
          "feedCnt" : 1,
          "transCarCnt" : 3,
          "careTel" : "051-971-6208",
          "dataStdDt" : "2026-01-02"
        }, {
          "careNm" : "(사)동부동물보호협회",
          "careRegNo" : "326331201100001",
          "orgNm" : "부산광역시 남구",
          "divisionNm" : "법인",
          "saveTrgtAnimal" : "개+고양이+기타",
          "careAddr" : "부산광역시 해운대구 송정2로13번길 46 (송정동) ",
          "jibunAddr" : "부산광역시 해운대구 송정동 85-1 ",
          "lat" : 35.181606,
          "lng" : 129.19846,
          "dsignationDate" : "2025-01-01",
          "weekOprStime" : "09:00",
          "weekOprEtime" : "18:00",
          "weekCellStime" : "14:00",
          "weekCellEtime" : "16:00",
          "weekendOprStime" : "10:00",
          "weekendOprEtime" : "14:00",
          "weekendCellStime" : "10:00",
          "weekendCellEtime" : "13:00",
          "closeDay" : "일요일",
          "vetPersonCnt" : 2,
          "specsPersonCnt" : 5,
          "medicalCnt" : 3,
          "breedCnt" : 15,
          "quarabtineCnt" : 3,
          "feedCnt" : 1,
          "transCarCnt" : 3,
          "careTel" : "051-701-7599",
          "dataStdDt" : "2026-07-18"
        } ]
      },
      "numOfRows" : 3,
      "pageNo" : 1,
      "totalCount" : 861
    }
  }
}
"""

    /** docs/api/samples/live-2026-11-03/sido_v2.json */
    val sidoV2: String = """
{
  "response" : {
    "header" : {
      "reqNo" : 64554588,
      "resultCode" : "00",
      "resultMsg" : "NORMAL SERVICE."
    },
    "body" : {
      "items" : {
        "item" : [ {
          "orgCd" : "6110000",
          "orgdownNm" : "서울특별시"
        }, {
          "orgCd" : "6260000",
          "orgdownNm" : "부산광역시"
        }, {
          "orgCd" : "6270000",
          "orgdownNm" : "대구광역시"
        }, {
          "orgCd" : "6280000",
          "orgdownNm" : "인천광역시"
        }, {
          "orgCd" : "5690000",
          "orgdownNm" : "세종특별자치시"
        }, {
          "orgCd" : "6300000",
          "orgdownNm" : "대전광역시"
        }, {
          "orgCd" : "6310000",
          "orgdownNm" : "울산광역시"
        }, {
          "orgCd" : "6410000",
          "orgdownNm" : "경기도"
        }, {
          "orgCd" : "6530000",
          "orgdownNm" : "강원특별자치도"
        }, {
          "orgCd" : "6430000",
          "orgdownNm" : "충청북도"
        }, {
          "orgCd" : "6440000",
          "orgdownNm" : "충청남도"
        }, {
          "orgCd" : "6540000",
          "orgdownNm" : "전북특별자치도"
        }, {
          "orgCd" : "6470000",
          "orgdownNm" : "경상북도"
        }, {
          "orgCd" : "6480000",
          "orgdownNm" : "경상남도"
        }, {
          "orgCd" : "6500000",
          "orgdownNm" : "제주특별자치도"
        }, {
          "orgCd" : "6130000",
          "orgdownNm" : "전남광주통합특별시"
        } ]
      },
      "numOfRows" : 20,
      "pageNo" : 1,
      "totalCount" : 16
    }
  }
}
"""

    /** docs/api/samples/live-2026-11-03/sigungu_v2_seoul.json */
    val sigunguV2Seoul: String = """
{
  "response" : {
    "header" : {
      "reqNo" : 64554589,
      "resultCode" : "00",
      "resultMsg" : "NORMAL SERVICE."
    },
    "body" : {
      "items" : {
        "item" : [ {
          "uprCd" : "6110000",
          "orgCd" : "6119999",
          "orgdownNm" : "가정보호"
        }, {
          "uprCd" : "6110000",
          "orgCd" : "3220000",
          "orgdownNm" : "강남구"
        }, {
          "uprCd" : "6110000",
          "orgCd" : "3240000",
          "orgdownNm" : "강동구"
        }, {
          "uprCd" : "6110000",
          "orgCd" : "3080000",
          "orgdownNm" : "강북구"
        }, {
          "uprCd" : "6110000",
          "orgCd" : "3150000",
          "orgdownNm" : "강서구"
        }, {
          "uprCd" : "6110000",
          "orgCd" : "3200000",
          "orgdownNm" : "관악구"
        }, {
          "uprCd" : "6110000",
          "orgCd" : "3040000",
          "orgdownNm" : "광진구"
        }, {
          "uprCd" : "6110000",
          "orgCd" : "3160000",
          "orgdownNm" : "구로구"
        }, {
          "uprCd" : "6110000",
          "orgCd" : "3170000",
          "orgdownNm" : "금천구"
        }, {
          "uprCd" : "6110000",
          "orgCd" : "3100000",
          "orgdownNm" : "노원구"
        } ]
      },
      "numOfRows" : 10,
      "pageNo" : 1,
      "totalCount" : 28
    }
  }
}
"""

    /** docs/api/samples/gateway_error_30.json */
    val gatewayError30: String = """
{
  "OpenAPI_ServiceResponse": {
    "cmmMsgHeader": {
      "errMsg": "SERVICE_KEY_IS_NOT_REGISTERED_ERROR",
      "returnAuthMsg": "등록되지 않은 서비스키",
      "returnReasonCode": "30"
    }
  }
}
"""
}
