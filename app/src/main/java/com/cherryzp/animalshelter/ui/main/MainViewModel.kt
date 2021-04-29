package com.cherryzp.animalshelter.ui.main

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cherryzp.animalshelter.AppApplication
import com.cherryzp.animalshelter.base.BaseViewModel
import com.cherryzp.animalshelter.model.DataModel
import com.cherryzp.animalshelter.model.response.*
import com.cherryzp.animalshelter.network.CustomCallback
import com.cherryzp.animalshelter.repository.ShelterRepository
import com.cherryzp.animalshelter.room.entity.AbandonmentPublicEntity
import com.cherryzp.animalshelter.util.CommonUtils
import com.cherryzp.animalshelter.util.ParseUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import retrofit2.Call
import retrofit2.Response
import java.io.StringReader
import java.lang.Exception

class MainViewModel(private val model: DataModel, private val repository: ShelterRepository): BaseViewModel() {

    val TAG = "MainViewModel"

    private val _sidoListLiveData = MutableLiveData<ArrayList<Sido>>()
    val sidoListLiveData: LiveData<ArrayList<Sido>>
        get() = _sidoListLiveData

    private val _sigunguListLiveData = MutableLiveData<ArrayList<Sigungu>>()
    val sigunguListLiveData: LiveData<ArrayList<Sigungu>>
        get() = _sigunguListLiveData

    private val _shelterListLiveData = MutableLiveData<ArrayList<Shelter>>()
    val shelterListLiveData: LiveData<ArrayList<Shelter>>
        get() = _shelterListLiveData

    private val _abandonmentPublicListLiveData = MutableLiveData<ArrayList<AbandonmentPublic>>()
    val abandonmentPublicListLiveData : LiveData<ArrayList<AbandonmentPublic>>
        get() = _abandonmentPublicListLiveData

    private val _totalCntLiveData = MutableLiveData<Int>()
    val totalCntLiveData : LiveData<Int>
        get() = _totalCntLiveData

    private val sidoList = ArrayList<Sido>()
    private var sigunguList = ArrayList<Sigungu>()
    private var shelterList = ArrayList<Shelter>()
    private var abandonmentPublicList = ArrayList<AbandonmentPublic>()

    val xmlPullParserFactory = XmlPullParserFactory.newInstance()
    val parser = xmlPullParserFactory.newPullParser()

    fun shelter() {
        model.shelter(6410000, 4020000).enqueue(object: CustomCallback<String>() {
            override fun onSuccess(call: Call<String>, response: Response<String>) {
                shelterList.addAll(ParseUtils.parseShelter(response.body().toString()))
                _shelterListLiveData.value = shelterList
            }

            override fun onError(call: Call<String>, response: Response<String>) {

            }

            override fun onFail(call: Call<String>, t: Throwable) {

            }

        })
    }

    fun abandonmentPublic(activity: Activity, map: MutableMap<String, String>) {

        AppApplication.appApplication.progressOn(activity)

        model.abandonmentPublic(map).enqueue(object : CustomCallback<String>() {
            override fun onSuccess(call: Call<String>, response: Response<String>) {
                val pair = ParseUtils.parseAbandonment(response.body().toString())
                _totalCntLiveData.value = Integer.valueOf(pair.second)
                abandonmentPublicList.addAll(pair.first)
                _abandonmentPublicListLiveData.value = abandonmentPublicList
                Log.d(TAG, response.body().toString())
                AppApplication.appApplication.progressOff()
                Log.d(TAG, "화면 종료")
            }

            override fun onError(call: Call<String>, response: Response<String>) {
                AppApplication.appApplication.progressOff()
                Log.d(TAG, "화면 종료")
            }

            override fun onFail(call: Call<String>, t: Throwable) {
                AppApplication.appApplication.progressOff()
                Log.d(TAG, "화면 종료")
            }
        })
    }

    fun insert() {
        val abandonmentPublicEntity = AbandonmentPublicEntity(
            age = "20",
            careAddr = "asdf",
            careNm = "하염",
            careTel = "010101010101",
            chargeNm = "ddd",
            colorCd = "asdf",
            desertionNo = "123",
            filename = "123",
            happenDt = "123",
            happenPlace = "adsf",
            kindCd = "adsf",
            neuterYn = "t",
            noticeEdt = "dfdf",
            noticeNo = "adsf",
            noticeSdt = "adsf",
            officetel = "asdf",
            orgNm = "asdfasdf",
            popfile = "asdf",
            processState = "asdf",
            sexCd = "asdf",
            specialMark = "adsf",
            weight = "asdf"
        )
        repository.insert(abandonmentPublicEntity)
    }
}