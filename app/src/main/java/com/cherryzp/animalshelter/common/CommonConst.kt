package com.cherryzp.animalshelter.common

/*
    * 파라미터
    *
    * @bgnde : 유기날짜 (검색 시작일) (YYYYMMDD)     날짜 파라미터가 없으면 제일 최신순으로 정렬됨
    * @endde : 유기날짜 (검색 종료일) (YYYYMMDD)
    * @upkind : 축종코드 - 개 : 417000 , 고양이 : 422400 , 기타 : 429900
    * @kind : 품종코드
    * @upr_cd : 시도 코드
    * @org_cd : 시군구 코드
    * @care_reg_no : 보호소 번호
    * @state : 상태 - 전체 : null(빈값) , 공고중 : notice , 보호중 : protect
    * @pageNo : 페이지번호
    * @numOfRows : 페이지당 보여줄 개수
    * @neuter_yn : 중성화 여부 y, n
    *
    * */
const val SEARCH_PARAMS_UPKIND = "upkind"
const val SEARCH_PARAMS_BGNDE = "bgnde"
const val SEARCH_PARAMS_ENDDE = "endde"
const val SEARCH_PARAMS_KIND = "kind"
const val SEARCH_PARAMS_UPR_CD = "upr_cd"
const val SEARCH_PARAMS_ORG_CD = "org_cd"
const val SEARCH_PARAMS_CARE_REG_NO = "care_reg_no"
const val SEARCH_PARAMS_STATE = "state"
const val SEARCH_PARAMS_NEUTER_YN = "neuter_yn"