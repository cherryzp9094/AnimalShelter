package com.cherryzp.animalshelter.ui.splash

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cherryzp.animalshelter.AppApplication
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.base.BaseViewModel
import com.cherryzp.animalshelter.model.DataModel
import com.cherryzp.animalshelter.model.response.Sido
import com.cherryzp.animalshelter.model.response.Sigungu
import com.cherryzp.animalshelter.network.CustomCallback
import com.cherryzp.animalshelter.util.CommonUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import retrofit2.Call
import retrofit2.Response
import java.io.File

class SplashViewModel(private val model: DataModel): BaseViewModel() {

    val TAG = "MainViewModel"

    val sidoFile = CommonUtils.getContext().getString(R.string.sido_file)
    val sigunguFile = CommonUtils.getContext().getString(R.string.sigungu_file)

    private val _loadData = MutableLiveData<Boolean>()
    val loadData: LiveData<Boolean>
        get() = _loadData

    private val _sidoLiveData = MutableLiveData<List<Sido>>()
    val sidoLiveData: LiveData<List<Sido>>
        get() = _sidoLiveData

    val sidoList: ArrayList<Sido> = ArrayList()
    val sigunguList: ArrayList<Sigungu> = ArrayList()

    //시군구 총 개수
    var totalSigunguCnt = 0
    //시군구 로드된 개수
    var loadSigunguCnt = 0

    val xmlPullParserFactory = XmlPullParserFactory.newInstance()
    val xmlPullParser = xmlPullParserFactory.newPullParser()

    fun sido(activity: Activity) {
        if (!File(CommonUtils.getContext().filesDir.absolutePath + "/" + sidoFile).exists()) {
            AppApplication.appApplication.progressOn(activity)

            model.sido()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    saveSidoFile(it, sidoFile)
                    parseSidoFile(sidoFile)
                    sigungu(activity, sidoList)
                }, {

                })

//            model.sido().enqueue(object : CustomCallback<String>() {
//                override fun onSuccess(call: Call<String>, response: Response<String>) {
//                    saveSidoFile(response.body().toString(), sidoFile)
//                    parseSidoFile(sidoFile)
//                    sigungu(activity, sidoList)
//                }
//
//                override fun onError(call: Call<String>, response: Response<String>) {
//
//                }
//
//                override fun onFail(call: Call<String>, t: Throwable) {
//                    t.printStackTrace()
//                }
//            })
        } else {
            parseSidoFile(sidoFile)
            sigungu(activity, sidoList)
        }
    }

    fun sigungu(activity: Activity, sidoList: ArrayList<Sido>) {
        for (sido in sidoList) {
            val file = File(CommonUtils.getContext().filesDir.absolutePath + "/" + sido.orgCd + sigunguFile)

            if (!file.exists()) {
                AppApplication.appApplication.progressOn(activity)

                model.sigungu(sido.orgCd)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        saveSigunguFile(it, sido.orgCd.toString() + sigunguFile)
                    }, {

                    })

//                model.sigungu(sido.orgCd).enqueue(object: CustomCallback<String>() {
//                    override fun onSuccess(call: Call<String>, response: Response<String>) {
//                        saveSigunguFile(response.body().toString(), sido.orgCd.toString() + sigunguFile)
//                    }
//
//                    override fun onError(call: Call<String>, response: Response<String>) {
//
//                    }
//
//                    override fun onFail(call: Call<String>, t: Throwable) {
//
//                    }
//
//                })
            } else {
                loadSigunguCnt++

                if (loadSigunguCnt == totalSigunguCnt) {
                    _loadData.value = true
                }
            }

        }
    }

    fun saveSidoFile(xmlData: String, fileName: String) {
        try {
            val fos = CommonUtils.getContext().openFileOutput(fileName, Context.MODE_PRIVATE)
            fos.write(xmlData.toByteArray())
            fos.close()
            fos.flush()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveSigunguFile(xmlData: String, fileName: String) {
        try {
            val fos = CommonUtils.getContext().openFileOutput(fileName, Context.MODE_PRIVATE)
            fos.write(xmlData.toByteArray())
            fos.close()
            fos.flush()

            loadSigunguCnt++

            Log.d(TAG, "$fileName : 저장완료 , 카운트 : $loadSigunguCnt")

            if (loadSigunguCnt == totalSigunguCnt) {
                _loadData.value = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun parseSidoFile(fileName: String) {
        try {
            val fis = CommonUtils.getContext().openFileInput(fileName)

            xmlPullParser.setInput(fis, "UTF-8")
            var event = xmlPullParser.eventType

            var orgCd: Int? = null
            var orgdownNm: String? = null

            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        if (xmlPullParser.name.equals("orgCd")) {
                            orgCd = Integer.parseInt(xmlPullParser.nextText())
                        } else if (xmlPullParser.name.equals("orgdownNm")) {
                            orgdownNm = xmlPullParser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (xmlPullParser.name.equals("item")) {
                            val sido = Sido(orgCd!!, orgdownNm!!)
                            sidoList.add(sido)

                            orgCd = null
                            orgdownNm = null
                        }
                    }
                }
                event = xmlPullParser.next()
            }
            _sidoLiveData.value = sidoList

            totalSigunguCnt = sidoList.size

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}