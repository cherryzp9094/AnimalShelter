package com.cherryzp.animalshelter.ui.main.search

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.base.BaseFragment
import com.cherryzp.animalshelter.common.*
import com.cherryzp.animalshelter.databinding.FragmentSearchBinding
import com.cherryzp.animalshelter.model.response.Shelter
import com.cherryzp.animalshelter.model.response.Sido
import com.cherryzp.animalshelter.model.response.Sigungu
import com.cherryzp.animalshelter.ui.main.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val SIDO_LINE_COUNT = 2
private const val SIGUNGU_LINE_COUNT = 2
private const val SHELTER_LINE_COUNT = 1

class SearchFragment : BaseFragment<FragmentSearchBinding, MainViewModel>(), SearchItemSelectListener {

    override val layoutResourceId: Int
        get() = R.layout.fragment_search
    override val viewModel: MainViewModel by viewModel()

    private lateinit var mainActivity: MainActivity

    private val searchMap = mutableMapOf<String, String>()

    private val sidoRecyclerAdatper: SearchRecyclerAdapter by inject()
    private val sigunguRecyclerAdapter: SearchRecyclerAdapter by inject()
    private val shelterRecyclerAdatper: SearchRecyclerAdapter by inject()

    override fun initStartView() {
        mainActivity = activity as MainActivity

        searchMap.clear()

        if (viewModel.sidoListLiveData.value.isNullOrEmpty()) {
            loadData()
        }
        initSearchRecycler()
    }

    override fun initDataBinding() {
        observeSearchRecycler()
    }

    override fun initAfterBinding() {
        initListener()
    }

    private fun loadData() {
        viewModel.loadSido(mainActivity)
    }

    //모든 서치 리사이클러 초기화
    private fun initSearchRecycler() {
        val searchItemDecoration = SearchItemDecoration(mainActivity, 8, 4, 8, 4)

        //시도 초기화
        sidoRecyclerAdatper.activity = mainActivity
        sidoRecyclerAdatper.itemKind = SearchRecyclerAdapter.SearchItemKind.SIDO
        sidoRecyclerAdatper.searchItemSelectListener = this

        dataBinding.sidoRecyclerView.run {
            adapter = sidoRecyclerAdatper
            layoutManager = StaggeredGridLayoutManager(SIDO_LINE_COUNT, StaggeredGridLayoutManager.VERTICAL)
            addItemDecoration(searchItemDecoration)
        }

        //시군구 초기화
        sigunguRecyclerAdapter.activity = mainActivity
        sigunguRecyclerAdapter.itemKind = SearchRecyclerAdapter.SearchItemKind.SIGUNGU
        sigunguRecyclerAdapter.searchItemSelectListener = this

        dataBinding.sigunguRecyclerView.run {
            adapter = sigunguRecyclerAdapter
            layoutManager = StaggeredGridLayoutManager(SIGUNGU_LINE_COUNT, StaggeredGridLayoutManager.VERTICAL)
            addItemDecoration(searchItemDecoration)
        }

        //보호소 초기화
        shelterRecyclerAdatper.activity = mainActivity
        shelterRecyclerAdatper.itemKind = SearchRecyclerAdapter.SearchItemKind.SHELTER
        shelterRecyclerAdatper.searchItemSelectListener = this

        dataBinding.shelterRecyclerView.run {
            adapter = shelterRecyclerAdatper
            layoutManager = StaggeredGridLayoutManager(SHELTER_LINE_COUNT, StaggeredGridLayoutManager.VERTICAL)
            addItemDecoration(searchItemDecoration)
        }
    }

    private fun observeSearchRecycler() {
        viewModel.sidoListLiveData.observe(this, {
            sidoRecyclerAdatper.setSearchList(it as ArrayList<Any>)
            sidoRecyclerAdatper.notifyDataSetChanged()
        })
        viewModel.sigunguListLiveData.observe(this, {
            sigunguRecyclerAdapter.setSearchList(it as ArrayList<Any>)
            sigunguRecyclerAdapter.notifyDataSetChanged()
        })
        viewModel.shelterListLiveData.observe(this, {
            shelterRecyclerAdatper.setSearchList(it as ArrayList<Any>)
            shelterRecyclerAdatper.notifyDataSetChanged()
        })
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
                insertSearchMap(SEARCH_PARAMS_UPKIND, resources.getString(R.string.kind_dog))
                dataBinding.dogBtn.isActivated = true
            }
            R.id.cat_btn -> {
                insertSearchMap(SEARCH_PARAMS_UPKIND, resources.getString(R.string.kind_cat))
                dataBinding.catBtn.isActivated = true
            }
            R.id.etc_btn -> {
                insertSearchMap(SEARCH_PARAMS_UPKIND, resources.getString(R.string.kind_etc))
                dataBinding.etcBtn.isActivated = true
            }
            R.id.total_btn -> {
                removeSearchMap(SEARCH_PARAMS_UPKIND)
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

    //선택된 아이템 리스너
    override fun itemSelectedListener(
        itemKind: SearchRecyclerAdapter.SearchItemKind,
        selectItem: Any,
        isCancel: Boolean
    ) {
        when (itemKind) {
            SearchRecyclerAdapter.SearchItemKind.SIDO -> {
                sigunguRecyclerAdapter.resetActivatedPosition()
                shelterRecyclerAdatper.resetActivatedPosition()
                removeSearchMap(SEARCH_PARAMS_ORG_CD)
                removeSearchMap(SEARCH_PARAMS_CARE_REG_NO)

                val item: Sido = selectItem as Sido

                if (isCancel) {
                    removeSearchMap(SEARCH_PARAMS_UPR_CD)
                    return
                }

                insertSearchMap(SEARCH_PARAMS_UPR_CD, item.orgCd.toString())
                viewModel.loadSigungu(mainActivity, item.orgCd)
                viewModel.resetShelter()
            }
            SearchRecyclerAdapter.SearchItemKind.SIGUNGU -> {
                shelterRecyclerAdatper.resetActivatedPosition()
                removeSearchMap(SEARCH_PARAMS_CARE_REG_NO)

                val item: Sigungu = selectItem as Sigungu

                if (isCancel) {
                    removeSearchMap(SEARCH_PARAMS_ORG_CD)
                    return
                }

                insertSearchMap(SEARCH_PARAMS_ORG_CD, item.orgCd.toString())
                viewModel.loadShelter(mainActivity, item.uprCd, item.orgCd)
            }
            SearchRecyclerAdapter.SearchItemKind.SHELTER -> {
                val item: Shelter = selectItem as Shelter

                if (isCancel) {
                    removeSearchMap(SEARCH_PARAMS_CARE_REG_NO)
                    return
                }

                insertSearchMap(SEARCH_PARAMS_CARE_REG_NO, item.careRegNo.toString())
            }
        }
    }

    private fun insertSearchMap(key: String, value: String) {
        searchMap[key] = value
    }

    private fun removeSearchMap(key: String) {
        searchMap.remove(key)
    }
}