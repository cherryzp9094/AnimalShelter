package com.cherryzp.animalshelter.model

import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.network.CustomCallback
import com.cherryzp.animalshelter.service.ShelterService
import com.cherryzp.animalshelter.util.CommonUtils
import io.reactivex.Single
import retrofit2.Call

class DataModelImpl(private val service: ShelterService) : DataModel {

    private val serviceKey = CommonUtils.getContext().getString(R.string.service_key)
    private val sidoNumOfRows = 30

    override fun sido(): Single<String> {
        return service.sido(serviceKey, sidoNumOfRows)
    }

    override fun sigungu(uprCd: Int): Single<String> {
        return service.sigungu(serviceKey, uprCd)
    }

    override fun shelter(uprCd: Int, orgCd: Int): Single<String> {
        return service.shelter(serviceKey, uprCd, orgCd)
    }

    override fun kind(upKindCd: Int, orgCd: Int): Single<String> {
        return service.shelter(serviceKey, upKindCd, orgCd)
    }

    override fun abandonmentPublic(pageNo: Int, numOfRows: Int): Single<String> {
        return service.abandonmentPublic(serviceKey, pageNo, numOfRows)
    }

    override fun abandonmentPublic(map: MutableMap<String, String>): Single<String> {
        map.put("serviceKey", serviceKey)
        return service.abandonmentPublic(map)
    }


}