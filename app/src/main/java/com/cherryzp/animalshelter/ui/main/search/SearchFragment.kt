package com.cherryzp.animalshelter.ui.main.search

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.databinding.ObservableArrayMap
import androidx.recyclerview.widget.RecyclerView
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
import com.cherryzp.animalshelter.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.fragment_search.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.w3c.dom.Text

private const val SIDO_LINE_COUNT = 2
private const val SIGUNGU_LINE_COUNT = 2
private const val SHELTER_LINE_COUNT = 1

class SearchFragment : BaseFragment<FragmentSearchBinding, MainViewModel>(), SearchItemSelectListener {

    override val layoutResourceId: Int
        get() = R.layout.fragment_search
    override val viewModel: MainViewModel by sharedViewModel()

    private lateinit var mainActivity: MainActivity

    private val searchMap = mutableMapOf<String, String>()

    private val sidoRecyclerAdatper: SearchRecyclerAdapter by inject()
    private val sigunguRecyclerAdapter: SearchRecyclerAdapter by inject()
    private val shelterRecyclerAdatper: SearchRecyclerAdapter by inject()

    override fun initStartView() {
        mainActivity = activity as MainActivity

        initData()

        if (viewModel.sidoListLiveData.value.isNullOrEmpty()) {
            loadData()
        }
        initRecycler()
    }

    //데이터 초기화
    private fun initData() {
        searchMap.clear()
    }

    override fun initDataBinding() {
        observeRecyclerItem()
    }

    override fun initAfterBinding() {
        initListener()
    }

    private fun loadData() {
        viewModel.loadSido(mainActivity)
    }

    //모든 서치 리사이클러 초기화
    private fun initRecycler() {
        val searchItemDecoration = SearchItemDecoration(mainActivity, 8, 4, 8, 4)

        //시도 초기화
        initSearchItemRecycler(sidoRecyclerAdatper, SearchRecyclerAdapter.SearchItemKind.SIDO, SIDO_LINE_COUNT, searchItemDecoration, dataBinding.sidoRecyclerView)

        //시군구 초기화
        initSearchItemRecycler(sigunguRecyclerAdapter, SearchRecyclerAdapter.SearchItemKind.SIGUNGU, SIGUNGU_LINE_COUNT, searchItemDecoration, dataBinding.sigunguRecyclerView)

        //보호소 초기화
        initSearchItemRecycler(shelterRecyclerAdatper, SearchRecyclerAdapter.SearchItemKind.SHELTER, SHELTER_LINE_COUNT, searchItemDecoration, dataBinding.shelterRecyclerView)
    }

    //리사이클러 각 카테고리 아이템 초기화
    private fun initSearchItemRecycler(recyclerAdapter: SearchRecyclerAdapter, itemKind: SearchRecyclerAdapter.SearchItemKind, lineCount: Int, searchItemDecoration: SearchItemDecoration, recyclerView: RecyclerView) {
        recyclerAdapter.activity = mainActivity
        recyclerAdapter.itemKind = itemKind
        recyclerAdapter.searchItemSelectListener = this
        recyclerAdapter.resetActivatedPosition()

        recyclerView.run {
            adapter = recyclerAdapter
            layoutManager =
                StaggeredGridLayoutManager(lineCount, StaggeredGridLayoutManager.VERTICAL)
            addItemDecoration(searchItemDecoration)
        }
    }

    //검색 관련 리스트 아이템 관찰
    private fun observeRecyclerItem() {
        viewModel.sidoListLiveData.observe(this, {
            observeChange(sidoRecyclerAdatper, sido_recycler_view, it as ArrayList<Any>)
        })
        viewModel.sigunguListLiveData.observe(this, {
            observeChange(sigunguRecyclerAdapter, sigungu_recycler_view, it as ArrayList<Any>)
        })
        viewModel.shelterListLiveData.observe(this, {
            observeChange(shelterRecyclerAdatper, shelter_recycler_view, it as ArrayList<Any>)
        })
    }

    // 관찰된 아이템 어뎁터 값 변경
    private fun observeChange(adapter: SearchRecyclerAdapter, recyclerView: RecyclerView ,itemList: ArrayList<Any>) {
        adapter.setSearchList(itemList)
        adapter.notifyDataSetChanged()
        if (itemList.size > 0) {
            recyclerView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.GONE
        }
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
                if (checkDate()) {
                    mainActivity.searchData(searchMap)
                }
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

    private fun checkDate(): Boolean {
        var startDate: String? = null
        val endDate: String?

        if (dataBinding.bgndeEt.text.isNotEmpty() && dataBinding.bgndeEt.text.length == 8) {
            startDate = dataBinding.bgndeEt.text.toString()
            insertSearchMap(SEARCH_PARAMS_BGNDE, startDate)
        }  else if (dataBinding.bgndeEt.text.isNotEmpty() && dataBinding.bgndeEt.text.length < 8) {
            Toast.makeText(mainActivity, "시작 날짜가 잘못 입력되었습니다.", Toast.LENGTH_SHORT).show()
            return false
        } else {
            removeSearchMap(SEARCH_PARAMS_BGNDE)
        }

        if (dataBinding.enddeEt.text.isNotEmpty() && dataBinding.enddeEt.text.length == 8) {
            //시작날짜가 없고 종료 날짜만 있는경우 검색 불가
            if (startDate.isNullOrEmpty()) {
                Toast.makeText(mainActivity, "시작 날짜를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return false
            }

            endDate = dataBinding.enddeEt.text.toString()
            insertSearchMap(SEARCH_PARAMS_ENDDE, endDate)
        } else if (dataBinding.enddeEt.text.isNotEmpty() && dataBinding.enddeEt.text.length < 8) {
            Toast.makeText(mainActivity, "종료 날짜가 잘못 입력되었습니다.", Toast.LENGTH_SHORT).show()
            return false
        } else {
            removeSearchMap(SEARCH_PARAMS_ENDDE)
        }

        return true
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

                    viewModel.resetSigungu()
                    viewModel.resetShelter()

                    sigunguRecyclerAdapter.notifyDataSetChanged()
                    shelterRecyclerAdatper.notifyDataSetChanged()
                    return
                }

                insertSearchMap(SEARCH_PARAMS_UPR_CD, item.orgCd.toString())
                viewModel.loadSigungu(mainActivity, item.orgCd)
                viewModel.resetShelter()
                shelterRecyclerAdatper.notifyDataSetChanged()
            }
            SearchRecyclerAdapter.SearchItemKind.SIGUNGU -> {
                shelterRecyclerAdatper.resetActivatedPosition()
                removeSearchMap(SEARCH_PARAMS_CARE_REG_NO)

                val item: Sigungu = selectItem as Sigungu

                if (isCancel) {
                    removeSearchMap(SEARCH_PARAMS_ORG_CD)

                    viewModel.resetShelter()

                    shelterRecyclerAdatper.notifyDataSetChanged()
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