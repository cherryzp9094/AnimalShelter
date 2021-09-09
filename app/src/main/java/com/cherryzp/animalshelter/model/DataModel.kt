package com.cherryzp.animalshelter.model

import com.cherryzp.animalshelter.network.CustomCallback
import io.reactivex.Single
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface DataModel {

    fun sido(): Single<String>

    fun sigungu(uprCd: Int): Single<String>

    fun shelter(uprCd: Int, orgCd: Int): Single<String>

    fun kind(upKindCd: Int, orgCd: Int): Single<String>

    fun abandonmentPublic(pageNo: Int, numOfRows: Int): Single<String>

    fun abandonmentPublic(map: MutableMap<String, String>): Single<String>
}