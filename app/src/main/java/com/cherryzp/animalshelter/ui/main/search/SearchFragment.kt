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
import com.cherryzp.animalshelter.util.CommonUtils
import kotlinx.android.synthetic.main.fragment_search.*
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

    private fun initListener() {
        //동물 품종 선택
        dataBinding.catBtn.setOnClickListener(upkindClickListener)
        dataBinding.dogBtn.setOnClickListener(upkindClickListener)
        dataBinding.etcBtn.setOnClickListener(upkindClickListener)
        dataBinding.totalBtn.setOnClickListener(upkindClickListener)

        //동물 상태 선택
        dataBinding.stateTotalBtn.setOnClickListener(stateClickListener)
        dataBinding.stateNoticeBtn.setOnClickListener(stateClickListener)
        dataBinding.stateProtectBtn.setOnClickListener(stateClickListener)

        //중성화 선택
        dataBinding.neuterYBtn.setOnClickListener(neuterClickListener)
        dataBinding.neuterNBtn.setOnClickListener(neuterClickListener)

        //검색하기 버튼 (upkind 리스너에 있음)
        dataBinding.searchBtn.setOnClickListener(upkindClickListener)
    }

    //축종 리스너
    private val upkindClickListener = View.OnClickListener {
        resetUpkindState()
        selectUpkind(it)
    }

    //상태 리스너
    private val stateClickListener = View.OnClickListener {
        resetState()
        selectState(it)
    }

    //중성화 리스너
    private val neuterClickListener = View.OnClickListener {
        resetNeuter()
        selectNeuter(it)
    }

    private fun selectUpkind(view: View) {
        CommonUtils.hideKeyboard(mainActivity)

        when (view.id) {
            R.id.dog_btn -> {
                insertSearchMap(SEARCH_PARAMS_UPKIND, UPKIND_DOG)
                dataBinding.dogBtn.isActivated = true
            }
            R.id.cat_btn -> {
                insertSearchMap(SEARCH_PARAMS_UPKIND, UPKIND_CAT)
                dataBinding.catBtn.isActivated = true
            }
            R.id.etc_btn -> {
                insertSearchMap(SEARCH_PARAMS_UPKIND, UPKIND_ETC)
                dataBinding.etcBtn.isActivated = true
            }
            R.id.total_btn -> {
                removeSearchMap(SEARCH_PARAMS_UPKIND)
                dataBinding.totalBtn.isActivated = true
            }
            R.id.search_btn -> {
                checkDate()
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

    private fun selectState(view: View) {
        CommonUtils.hideKeyboard(mainActivity)

        when (view.id) {
            R.id.state_notice_btn -> {
                insertSearchMap(SEARCH_PARAMS_STATE, STATE_NOTICE)
                dataBinding.stateNoticeBtn.isActivated = true
            }
            R.id.state_protect_btn -> {
                insertSearchMap(SEARCH_PARAMS_STATE, STATE_PROTECT)
                dataBinding.stateProtectBtn.isActivated = true
            }
            R.id.state_total_btn -> {
                removeSearchMap(SEARCH_PARAMS_STATE)
                dataBinding.stateTotalBtn.isActivated = true
            }
        }
    }

    private fun resetState() {
        dataBinding.stateTotalBtn.isActivated = false
        dataBinding.stateNoticeBtn.isActivated = false
        dataBinding.stateProtectBtn.isActivated = false
    }

    private fun selectNeuter(view: View) {
        CommonUtils.hideKeyboard(mainActivity)

        when (view.id) {
            R.id.neuter_y_btn -> {
                insertSearchMap(SEARCH_PARAMS_NEUTER_YN, NEUTER_Y)
                dataBinding.neuterYBtn.isActivated = true
            }
            R.id.neuter_n_btn -> {
                insertSearchMap(SEARCH_PARAMS_NEUTER_YN, NEUTER_N)
                dataBinding.neuterNBtn.isActivated = true
            }
        }
    }

    private fun resetNeuter() {
        dataBinding.neuterYBtn.isActivated = false
        dataBinding.neuterNBtn.isActivated = false
    }

    private fun checkDate() {
        val startDate: String?
        val endDate: String?

        if (dataBinding.bgndeEt.text.isNotEmpty() && dataBinding.bgndeEt.text.length == 8) {
            startDate = dataBinding.bgndeEt.text.toString()
            insertSearchMap(SEARCH_PARAMS_BGNDE, startDate)
        } else {
            removeSearchMap(SEARCH_PARAMS_BGNDE)
        }

        if (dataBinding.enddeEt.text.isNotEmpty() && dataBinding.enddeEt.text.length == 8) {
            endDate = dataBinding.enddeEt.text.toString()
            insertSearchMap(SEARCH_PARAMS_ENDDE, endDate)
        } else {
            removeSearchMap(SEARCH_PARAMS_ENDDE)
        }

    }

    //선택된 아이템 리스너 ( 시/도, 시/군/구, 보호소 )
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