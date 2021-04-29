package com.cherryzp.animalshelter.model

import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.network.CustomCallback
import com.cherryzp.animalshelter.service.ShelterService
import com.cherryzp.animalshelter.util.CommonUtils
import retrofit2.Call

class DataModelImpl(private val service: ShelterService) : DataModel {

    val serviceKey = CommonUtils.getContext().getString(R.string.service_key)

    override fun sido(): Call<String> {
        return service.sido(serviceKey)
    }

    override fun sigungu(uprCd: Int): Call<String> {
        return service.sigungu(serviceKey, uprCd)
    }

    override fun shelter(uprCd: Int, orgCd: Int): Call<String> {
        return service.shelter(serviceKey, uprCd, orgCd)
    }

    override fun kind(upKindCd: Int, orgCd: Int): Call<String> {
        return service.shelter(serviceKey, upKindCd, orgCd)
    }

    override fun abandonmentPublic(pageNo: Int, numOfRows: Int): Call<String> {
        return service.abandonmentPublic(serviceKey, pageNo, numOfRows)
    }

    override fun abandonmentPublic(map: MutableMap<String, String>): Call<String> {
        map.put("serviceKey", serviceKey)
        return service.abandonmentPublic(map)
    }


}