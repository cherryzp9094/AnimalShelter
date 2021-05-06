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

    enum class FragmentState {
        ANIMAL, SEARCH
    }

    private var fragmentState = FragmentState.ANIMAL

    private val searchFragment  = SearchFragment()
    private val abandonmentPublicFragment = AbandonmentPublicFragment()

    var searchMap = mutableMapOf<String, String>()

    override val layoutResourceId: Int
        get() = R.layout.activity_main
    override val viewModel: MainViewModel by viewModel()

    var pageNo = 1
    private val numOfRows = 30

    override fun initStartView() {
        initAdView()

        btn_1.setOnClickListener(onClickListener)
        btn_2.setOnClickListener(onClickListener)

        supportFragmentManager.beginTransaction().replace(R.id.fragment_view, abandonmentPublicFragment).commit()

        viewModel.insert()
    }

    override fun initDataBinding() {

    }

    override fun initAfterBinding() {

    }

    private val onClickListener = View.OnClickListener {
        when (it.id) {
            R.id.btn_1 -> {
               moveFragment(FragmentState.ANIMAL)
            }
            R.id.btn_2 -> {
               moveFragment(FragmentState.SEARCH)
            }
        }
    }

    //프래그먼트 이동
    private fun moveFragment(state: FragmentState) {
        fragmentState = when (state){
            FragmentState.ANIMAL -> {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_view, abandonmentPublicFragment).commit()
                FragmentState.ANIMAL
            }

            FragmentState.SEARCH -> {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_view, searchFragment).commit()
                FragmentState.SEARCH
            }
        }

    }

    //조건 검색
    fun searchData(map: MutableMap<String, String>) {
        searchMap = map
        pageNo = 1
        viewModel.resetAbandonmentPublic()

        loadData()
        moveFragment(FragmentState.ANIMAL)
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

            viewModel.loadAbandonmentPublic(this, searchMap)

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