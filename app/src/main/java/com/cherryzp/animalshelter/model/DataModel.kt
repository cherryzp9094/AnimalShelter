package com.cherryzp.animalshelter.model

import com.cherryzp.animalshelter.network.CustomCallback
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface DataModel {

    fun sido(): Call<String>

    fun sigungu(uprCd: Int): Call<String>

    fun shelter(uprCd: Int, orgCd: Int): Call<String>

    fun kind(upKindCd: Int, orgCd: Int): Call<String>

    fun abandonmentPublic(pageNo: Int, numOfRows: Int): Call<String>

    fun abandonmentPublic(map: MutableMap<String, String>): Call<String>
}