package com.cherryzp.animalshelter.ui.main

import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.base.BaseActivity
import com.cherryzp.animalshelter.databinding.ActivityMainBinding
import com.cherryzp.animalshelter.ui.main.abandonmentpublic.AbandonmentPublicFragment
import com.cherryzp.animalshelter.ui.main.search.SearchFragment
import com.google.android.gms.ads.AdRequest

import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>() {

    val TAG = "MainActivity"

    enum class FRAGMENT_STATE {
        ANIMAL, SEARCH
    }

    var fragmentState = FRAGMENT_STATE.ANIMAL

    override val layoutResourceId: Int
        get() = R.layout.activity_main
    override val viewModel: MainViewModel by viewModel()

    var pageNo = 1
    private val numOfRows = 30

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
    var bgnde: String? = null
    var endde: String? = null
    var upkind: String? = null
    var kind: String? = null
    var uprCd: String? = null
    var orgCd: String? = null
    var careRegNo: String? = null
    var state: String? = null
    var neuterYn: String? = null

    override fun initStartView() {
        initAdView()

        loadData()

        btn_1.setOnClickListener(onClickListener)
        btn_2.setOnClickListener(onClickListener)

        supportFragmentManager.beginTransaction().replace(R.id.fragment_view, AbandonmentPublicFragment()).commit()

        viewModel.insert()
    }

    override fun initDataBinding() {

    }

    override fun initAfterBinding() {

    }

    private val onClickListener = View.OnClickListener {
        when (it.id) {
            R.id.btn_1 -> {
                if (fragmentState == FRAGMENT_STATE.SEARCH) {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_view, AbandonmentPublicFragment()).commit()
                    fragmentState = FRAGMENT_STATE.ANIMAL
                }
            }
            R.id.btn_2 -> {
                if (fragmentState == FRAGMENT_STATE.ANIMAL) {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_view, SearchFragment()).commit()
                    fragmentState = FRAGMENT_STATE.SEARCH
                }
            }
        }
    }

    fun loadData() {
        if (viewModel.totalCntLiveData.value == null || viewModel.totalCntLiveData.value!! - (pageNo * numOfRows) >= 0) {
            if (viewModel.totalCntLiveData.value != null) {
                Log.d(TAG, ""+ (viewModel.totalCntLiveData.value!! - (pageNo * numOfRows)))
                Log.d(TAG, "" + viewModel.totalCntLiveData.value!!)
            }
            val map = mutableMapOf<String, String>()

            map.put("pageNo", pageNo.toString())
            map.put("numOfRows", numOfRows.toString())

            if (bgnde != null) map.put("bgnde", bgnde!!)
            if (endde != null) map.put("endde", endde!!)
            if (upkind != null) map.put("upkind", upkind!!)
            if (kind != null) map.put("kind", kind!!)
            if (uprCd != null) map.put("upr_cd", uprCd!!)
            if (orgCd != null) map.put("org_cd", orgCd!!)
            if (careRegNo != null) map.put("care_reg_no", careRegNo!!)
            if (state != null) map.put("state", state!!)
            if (neuterYn != null) map.put("neuter_yn", neuterYn!!)

            Log.d(TAG, map.toString())

            viewModel.abandonmentPublic(this, map)

            pageNo++
        }
    }

    //admob 초기화
    private fun initAdView() {

        MobileAds.initialize(this, OnInitializationCompleteListener {})

        val adRequest = AdRequest.Builder().build()
        ad_view.loadAd(adRequest)
    }

    override fun onBackPressed() {
        ActivityCompat.finishAffinity(this)
    }
}