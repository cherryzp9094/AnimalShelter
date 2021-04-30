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
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>() {

    val TAG = "MainActivity"

    enum class FRAGMENT_STATE {
        ANIMAL, SEARCH
    }

    private var fragmentState = FRAGMENT_STATE.ANIMAL

    private val searchFragment  = SearchFragment()
    private val abandonmentPublicFragment = AbandonmentPublicFragment()

    var searchMap = mutableMapOf<String, String>()

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

    override fun initStartView() {
        initAdView()

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
               moveFragment(FRAGMENT_STATE.ANIMAL)
            }
            R.id.btn_2 -> {
               moveFragment(FRAGMENT_STATE.SEARCH)
            }
        }
    }

    //프래그먼트 이동
    private fun moveFragment(state: FRAGMENT_STATE) {
        fragmentState = when (state){
            FRAGMENT_STATE.ANIMAL -> {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_view, abandonmentPublicFragment).commit()
                FRAGMENT_STATE.ANIMAL
            }

            FRAGMENT_STATE.SEARCH -> {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_view, searchFragment).commit()
                FRAGMENT_STATE.SEARCH
            }
        }

    }

    fun searchData(map: MutableMap<String, String>) {
        searchMap = map
        pageNo = 1

        loadData()
        moveFragment(FRAGMENT_STATE.ANIMAL)
    }

    //유기동물 보호 조회
    fun loadData() {
        if (viewModel.totalCntLiveData.value == null || viewModel.totalCntLiveData.value!! - (pageNo * numOfRows) >= 0) {
            if (viewModel.totalCntLiveData.value != null) {
                Log.d(TAG, ""+ (viewModel.totalCntLiveData.value!! - (pageNo * numOfRows)))
                Log.d(TAG, "" + viewModel.totalCntLiveData.value!!)
            }

            searchMap["pageNo"] = pageNo.toString()
            searchMap["numOfRows"] = numOfRows.toString()

            when (pageNo) {
                1 -> viewModel.abandonmentPublic(this, searchMap, true)
                else -> viewModel.abandonmentPublic(this, searchMap, false)
            }

            pageNo++
        }
    }

    //admob 초기화
    private fun initAdView() {

        MobileAds.initialize(this) {}

        val adRequest = AdRequest.Builder().build()
        ad_view.loadAd(adRequest)
    }

    override fun onBackPressed() {
        ActivityCompat.finishAffinity(this)
    }
}