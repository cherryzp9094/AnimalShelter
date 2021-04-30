package com.cherryzp.animalshelter.ui.main.search

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.base.BaseFragment
import com.cherryzp.animalshelter.databinding.FragmentSearchBinding
import com.cherryzp.animalshelter.ui.main.MainActivity
import com.cherryzp.animalshelter.ui.main.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchFragment : BaseFragment<FragmentSearchBinding, MainViewModel>() {

    override val layoutResourceId: Int
        get() = R.layout.fragment_search
    override val viewModel: MainViewModel by viewModel()

    private lateinit var mainActivity: MainActivity

    private val searchMap = mutableMapOf<String, String>()

    override fun initStartView() {
        mainActivity = activity as MainActivity

        initListener()
    }

    override fun initDataBinding() {

    }

    override fun initAfterBinding() {

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListener() {
        //동물 품종 선택
        dataBinding.catBtn.setOnTouchListener(upkindTouchListener)
        dataBinding.dogBtn.setOnTouchListener(upkindTouchListener)
        dataBinding.etcBtn.setOnTouchListener(upkindTouchListener)
        dataBinding.totalBtn.setOnTouchListener(upkindTouchListener)

        //검색하기 버튼
        dataBinding.searchBtn.setOnTouchListener(upkindTouchListener)
    }

    //축종 리스너
    @SuppressLint("ClickableViewAccessibility")
    private val upkindTouchListener = View.OnTouchListener { v, event ->

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                resetUpkindState()
            }

            MotionEvent.ACTION_UP -> {
                selectUpkind(v)
            }
        }

        false
    }

    private fun selectUpkind(view: View) {
        when (view.id) {
            R.id.dog_btn -> {
                searchMap["upkind"] = resources.getString(R.string.kind_dog)
                dataBinding.dogBtn.isActivated = true
            }
            R.id.cat_btn -> {
                searchMap["upkind"] = resources.getString(R.string.kind_cat)
                dataBinding.catBtn.isActivated = true
            }
            R.id.etc_btn -> {
                searchMap["upkind"] = resources.getString(R.string.kind_etc)
                dataBinding.etcBtn.isActivated = true
            }
            R.id.total_btn -> {
                searchMap.remove("upkind")
                dataBinding.totalBtn.isActivated = true
            }
            R.id.search_btn -> {
                mainActivity.searchData(searchMap)
            }
        }
    }

    private fun resetUpkindState() {
        dataBinding.catBtn.isActivated = false
        dataBinding.dogBtn.isActivated = false
        dataBinding.etcBtn.isActivated = false
        dataBinding.totalBtn.isActivated = false
    }
}