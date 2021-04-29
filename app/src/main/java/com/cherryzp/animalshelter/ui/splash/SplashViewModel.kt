package com.cherryzp.animalshelter.ui.splash

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.base.BaseViewModel
import com.cherryzp.animalshelter.model.DataModel
import com.cherryzp.animalshelter.model.response.Sido
import com.cherryzp.animalshelter.model.response.Sigungu
import com.cherryzp.animalshelter.network.CustomCallback
import com.cherryzp.animalshelter.util.CommonUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.lang.Exception

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

    fun sido() {
        if (!File(CommonUtils.getContext().filesDir.absolutePath + "/" + sidoFile).exists()) {
            model.sido().enqueue(object : CustomCallback<String>() {
                override fun onSuccess(call: Call<String>, response: Response<String>) {
                    Log.e(TAG, response.body().toString())

                    saveSidoFile(response.body().toString(), sidoFile)
                    parseSidoFile(sidoFile)
                    sigungu(sidoList)
                }

                override fun onError(call: Call<String>, response: Response<String>) {

                }

                override fun onFail(call: Call<String>, t: Throwable) {
                    t.printStackTrace()
                }
            })
        } else {
            parseSidoFile(sidoFile)
            sigungu(sidoList)
        }
    }

    fun sigungu(sidoList: ArrayList<Sido>) {
        for (sido in sidoList) {
            val file = File(CommonUtils.getContext().filesDir.absolutePath + "/" + sido.orgCd + sigunguFile)

            if (!file.exists()) {
                model.sigungu(sido.orgCd).enqueue(object: CustomCallback<String>() {
                    override fun onSuccess(call: Call<String>, response: Response<String>) {
                        Log.e(TAG, response.body().toString())
                        saveSigunguFile(response.body().toString(), sido.orgCd.toString() + sigunguFile)
                    }

                    override fun onError(call: Call<String>, response: Response<String>) {

                    }

                    override fun onFail(call: Call<String>, t: Throwable) {

                    }

                })
            } else {
                loadSigunguCnt++

                Log.e(TAG, "$loadSigunguCnt 카운트"

                )

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

            Log.e(TAG, "$fileName -> 저장완료 //// $loadSigunguCnt")

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
                            Log.e(TAG, "orgCd : $orgCd")
                        } else if (xmlPullParser.name.equals("orgdownNm")) {
                            orgdownNm = xmlPullParser.nextText()
                            Log.e(TAG, "orgdownNm : $orgdownNm")
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (xmlPullParser.name.equals("item")) {
                            val sido = Sido(orgCd!!, orgdownNm!!)
                            Log.e(TAG, "sido : $sido")
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