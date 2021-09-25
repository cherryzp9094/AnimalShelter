package com.cherryzp.animalshelter.ui.main

import android.os.HandlerThread
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentTransaction
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.base.BaseActivity
import com.cherryzp.animalshelter.component.AppFinishDialog
import com.cherryzp.animalshelter.databinding.ActivityMainBinding
import com.cherryzp.animalshelter.ui.main.abandonmentpublic.AbandonmentPublicFragment
import com.cherryzp.animalshelter.ui.main.search.SearchFragment
import com.cherryzp.animalshelter.util.CommonUtils
import com.cherryzp.animalshelter.viewmodel.MainViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.logging.Handler

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>() {

    val TAG = "MainActivity"

    enum class FragmentState {
        ANIMAL, SEARCH
    }

    private var isMoveFragment = false;

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

        abandonment_fragment_btn.setOnClickListener(onClickListener)
        search_fragment_btn.setOnClickListener(onClickListener)
        back_btn.setOnClickListener(onClickListener)

        supportFragmentManager.beginTransaction().replace(R.id.fragment_view, abandonmentPublicFragment).commit()

    }

    override fun initDataBinding() {

    }

    override fun initAfterBinding() {

    }

    private val onClickListener = View.OnClickListener {
        CommonUtils.hideKeyboard(this)

        when (it.id) {
            R.id.abandonment_fragment_btn -> {
               moveFragment(FragmentState.ANIMAL)
            }
            R.id.search_fragment_btn -> {
               moveFragment(FragmentState.SEARCH)
            }
            R.id.back_btn -> {
                moveFragment(FragmentState.ANIMAL)
            }
        }
    }

    //프래그먼트 이동
    private fun moveFragment(state: FragmentState) {

        if (isMoveFragment) return

        isMoveFragment = true

        val fragmentTransaction = supportFragmentManager.beginTransaction();
        fragmentState = when (state){
            FragmentState.ANIMAL -> {
                fragmentTransaction.setCustomAnimations(R.anim.none, R.anim.vertical_exit)
                fragmentTransaction.replace(R.id.fragment_view, abandonmentPublicFragment).commit()
                FragmentState.ANIMAL
            }

            FragmentState.SEARCH -> {
                fragmentTransaction.setCustomAnimations(R.anim.vertical_enter, R.anim.none)
                fragmentTransaction.replace(R.id.fragment_view, searchFragment).commit()
                FragmentState.SEARCH
            }
        }

        isShowBackBtn(state)

        isMoveFragment = false
    }

    // 뒤로가기 버튼 생성여부
    private fun isShowBackBtn(state: FragmentState) {
        when (state) {
            FragmentState.ANIMAL -> back_btn.visibility = View.GONE
            FragmentState.SEARCH -> back_btn.visibility = View.VISIBLE
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
        when (fragmentState) {
            FragmentState.ANIMAL -> {
                val appFinishDialog = AppFinishDialog(this)
                appFinishDialog.showContactDialog()
            }
            FragmentState.SEARCH -> {
                moveFragment(FragmentState.ANIMAL)
            }
        }
    }
}