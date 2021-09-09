package com.cherryzp.animalshelter.ui.main

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cherryzp.animalshelter.AppApplication
import com.cherryzp.animalshelter.base.BaseViewModel
import com.cherryzp.animalshelter.model.DataModel
import com.cherryzp.animalshelter.model.response.*
import com.cherryzp.animalshelter.repository.ShelterRepository
import com.cherryzp.animalshelter.room.entity.AbandonmentPublicEntity
import com.cherryzp.animalshelter.util.ParseUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

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

    private var sidoList = ArrayList<Sido>()
    private var sigunguList = ArrayList<Sigungu>()
    private var abandonmentPublicList = ArrayList<AbandonmentPublic>()

    //보호소 조회
    fun loadShelter(activity: Activity, uprCd: Int, orgCd: Int) {
        AppApplication.appApplication.progressOn(activity)

        addDisposable(model.shelter(uprCd, orgCd)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                _shelterListLiveData.value = ParseUtils.parseShelter(it)
                AppApplication.appApplication.progressOff()

            },{
                AppApplication.appApplication.progressOff()

            })
        )

//        model.shelter(uprCd, orgCd).enqueue(object: CustomCallback<String>() {
//            override fun onSuccess(call: Call<String>, response: Response<String>) {
//                _shelterListLiveData.value = ParseUtils.parseShelter(response.body().toString())
//            }
//
//            override fun onError(call: Call<String>, response: Response<String>) {
//
//            }
//
//            override fun onFail(call: Call<String>, t: Throwable) {
//
//            }
//
//        })
    }

    //유기동물 조회
    fun loadAbandonmentPublic(activity: Activity, map: MutableMap<String, String>) {
        AppApplication.appApplication.progressOn(activity)

        addDisposable(model.abandonmentPublic(map)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                val pair = ParseUtils.parseAbandonment(it)
                _totalCntLiveData.value = Integer.valueOf(pair.second)
                abandonmentPublicList.addAll(pair.first)
                _abandonmentPublicListLiveData.value = abandonmentPublicList
                AppApplication.appApplication.progressOff()

            }, {
                AppApplication.appApplication.progressOff()

            }))

//        model.abandonmentPublic(map).enqueue(object : CustomCallback<String>() {
//            override fun onSuccess(call: Call<String>, response: Response<String>) {
//                val pair = ParseUtils.parseAbandonment(response.body().toString())
//                _totalCntLiveData.value = Integer.valueOf(pair.second)
//                abandonmentPublicList.addAll(pair.first)
//                _abandonmentPublicListLiveData.value = abandonmentPublicList
//            }
//
//            override fun onError(call: Call<String>, response: Response<String>) {
//
//            }
//
//            override fun onFail(call: Call<String>, t: Throwable) {
//
//            }
//        })
    }

    fun loadSido(activity: Activity) {
        AppApplication.appApplication.progressOn(activity)

        addDisposable(model.sido()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                sidoList = ParseUtils.parseSido(it)
                _sidoListLiveData.value = sidoList
                AppApplication.appApplication.progressOff()

            }, {
                AppApplication.appApplication.progressOff()

            })
        )

//        model.sido().enqueue(object : CustomCallback<String>() {
//            override fun onSuccess(call: Call<String>, response: Response<String>) {
//                sidoList = ParseUtils.parseSido(response.body().toString())
//                _sidoListLiveData.value = sidoList
//            }
//
//            override fun onError(call: Call<String>, response: Response<String>) {
//
//            }
//
//            override fun onFail(call: Call<String>, t: Throwable) {
//
//            }
//
//        })
    }

    fun loadSigungu(activity: Activity, uprCd: Int) {
        AppApplication.appApplication.progressOn(activity)

        addDisposable(model.sigungu(uprCd)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                sigunguList = ParseUtils.parseSigungu(it)
                _sigunguListLiveData.value = sigunguList
                AppApplication.appApplication.progressOff()

            }, {
                AppApplication.appApplication.progressOff()

            })
        )

//        model.sigungu(uprCd).enqueue(object : CustomCallback<String>() {
//            override fun onSuccess(call: Call<String>, response: Response<String>) {
//                sigunguList = ParseUtils.parseSigungu(response.body().toString())
//                _sigunguListLiveData.value = sigunguList
//            }
//
//            override fun onError(call: Call<String>, response: Response<String>) {
//
//            }
//
//            override fun onFail(call: Call<String>, t: Throwable) {
//
//            }
//
//        })
    }

    //유기동물 리스트 리셋
    fun resetAbandonmentPublic() {
        abandonmentPublicList.clear()
        _totalCntLiveData.value = null
    }

    fun resetSigungu() {
        _sigunguListLiveData.value?.clear()
    }

    //보호소 정보 리셋
    fun resetShelter() {
        _shelterListLiveData.value?.clear()
    }

    //테스트
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

    override fun onCleared() {
        super.onCleared()

        Log.e(TAG, "OnCleared Call")
    }
}