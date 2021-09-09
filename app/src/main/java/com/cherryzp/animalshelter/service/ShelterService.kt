package com.cherryzp.animalshelter.service

import com.cherryzp.animalshelter.model.response.Sido
import com.cherryzp.animalshelter.network.CustomCallback
import io.reactivex.Single
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ShelterService {

    //시도 검색
    @GET("sido")
    fun sido(@Query(value="serviceKey", encoded = true) serviceKey: String, @Query(value="numOfRows", encoded = true) numOfRows: Int): Single<String>

    //시도 검색
    @GET("sigungu")
    fun sigungu(@Query(value="serviceKey", encoded = true) serviceKey: String, @Query("upr_cd") uprCd : Int): Single<String>

    //보호소 검색
    @GET("shelter")
    fun shelter(@Query(value="serviceKey", encoded = true) serviceKey: String, @Query("upr_cd") uprCd: Int, @Query("org_cd") orgCd: Int): Single<String>

    //품종 검색
    @GET("kind")
    fun kind(@Query(value="serviceKey", encoded = true) serviceKey: String, @Query("up_kind_cd") upKindCd: Int): Single<String>

    //유기동물 조회
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
    @GET("abandonmentPublic")
    fun abandonmentPublic(
            @QueryMap(encoded = true) map: Map<String, String>
    ): Single<String>

    @GET("abandonmentPublic")
    fun abandonmentPublic(
        @Query(value="serviceKey", encoded = true) serviceKey: String,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int
    ): Single<String>

//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereDate(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("bgnde") bgnde: Int,
//        @Query("endde") endde: Int
//    ): Single<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereDateAndUpkind(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("bgnde") bgnde: Int,
//        @Query("endde") endde: Int,
//        @Query("upkind") upkind: Int
//    ): Single<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereDateAndUpkindAndUprCd(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("bgnde") bgnde: Int,
//        @Query("endde") endde: Int,
//        @Query("upkind") upkind: Int,
//        @Query("upr_cd") uprCd: Int
//    ): Single<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereDateAndUpkindAndUprCdAndState(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("bgnde") bgnde: Int,
//        @Query("endde") endde: Int,
//        @Query("upkind") upkind: Int,
//        @Query("upr_cd") uprCd: Int,
//        @Query("state") state: String
//    ): Single<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereDateAndUpkindAndOrgCd(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("bgnde") bgnde: Int,
//        @Query("endde") endde: Int,
//        @Query("upkind") upkind: Int,
//        @Query("org_cd") orgCd: Int
//    ): Call<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereDateAndUpkindAndCareRegNo(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("bgnde") bgnde: Int,
//        @Query("endde") endde: Int,
//        @Query("upkind") upkind: Int,
//        @Query("care_reg_no") careRegNo: Int
//    ): Call<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereDateAndUpkindAndState(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("bgnde") bgnde: Int,
//        @Query("endde") endde: Int,
//        @Query("upkind") upkind: Int,
//        @Query("state") state: String
//    ): Call<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereDateAndUpkindAndNeuterYn(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("bgnde") bgnde: Int,
//        @Query("endde") endde: Int,
//        @Query("upkind") upkind: Int,
//        @Query("neuter_yn") neuterYn: String
//    ): Call<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereDateAndKind(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("bgnde") bgnde: Int,
//        @Query("endde") endde: Int,
//        @Query("kind") kind: Int
//    ): Call<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereDateAndUprCd(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("bgnde") bgnde: Int,
//        @Query("endde") endde: Int,
//        @Query("upr_cd") uprCd: Int
//    ): Call<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereDateAndOrgCd(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("bgnde") bgnde: Int,
//        @Query("endde") endde: Int,
//        @Query("org_cd") OrgCd: Int
//    ): Call<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereDateAndCareRegNo(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("bgnde") bgnde: Int,
//        @Query("endde") endde: Int,
//        @Query("care_reg_no") careRegNo: Int
//    ): Call<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereDateAndState(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("bgnde") bgnde: Int,
//        @Query("endde") endde: Int,
//        @Query("state") state: String
//    ): Call<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereDateAndNeuterYn(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("bgnde") bgnde: Int,
//        @Query("endde") endde: Int,
//        @Query("neuter_yn") neuterYn: String
//    ): Call<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereUpkind(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("upkind") upkind: Int
//    ): Call<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereKind(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("kind") kind: Int
//    ): Call<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereUprCd(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("upr_cd") uprCd: Int
//    ): Call<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereOrgCd(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("org_cd") orgCd: Int
//    ): Call<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereCareRegNo(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("care_reg_no") careRegNo: Int
//    ): Call<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereState(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("state") state: String
//    ): Call<String>
//
//    @GET("abandonmentPublic")
//    fun abandonmentPublicWhereNeuterYn(
//        @Query(value="serviceKey", encoded = true) serviceKey: String,
//        @Query("pageNo") pageNo: Int,
//        @Query("numOfRows") numOfRows: Int,
//        @Query("neuter_yn") neuterYn: String
//    ): Call<String>

}