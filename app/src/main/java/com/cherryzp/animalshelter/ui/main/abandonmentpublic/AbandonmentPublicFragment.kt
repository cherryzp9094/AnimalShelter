package com.cherryzp.animalshelter.ui.main.abandonmentpublic

import android.app.ProgressDialog
import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.base.BaseFragment
import com.cherryzp.animalshelter.component.LoadingProgressDialog
import com.cherryzp.animalshelter.databinding.FragmentAbandonmentPublicBinding
import com.cherryzp.animalshelter.ui.main.MainActivity
import com.cherryzp.animalshelter.ui.main.MainViewModel
import kotlinx.android.synthetic.main.fragment_abandonment_public.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import retrofit2.http.QueryMap

class AbandonmentPublicFragment : BaseFragment<FragmentAbandonmentPublicBinding, MainViewModel>(){

    private val TAG = "AbandonmentPublicFragment"

    override val layoutResourceId: Int
        get() = R.layout.fragment_abandonment_public
    override val viewModel: MainViewModel by viewModel()

    lateinit var mainActivity: MainActivity

    private val abandonmentPublicRecyclerAdapter: AbandonmentPublicRecyclerAdapter by inject()

    override fun initStartView() {
        mainActivity = activity as MainActivity
        abandonmentPublicRecyclerAdapter.activity = mainActivity

        dataBinding.abandonmentPublicRv.run {
            adapter = abandonmentPublicRecyclerAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    //하단 스크롤시 추가 데이터 로드
                    if (!recyclerView.canScrollVertically(1)) {
                        mainActivity.loadData()
                    }
                }
            })
        }
    }

    override fun initDataBinding() {
        viewModel.abandonmentPublicListLiveData.observe(this, Observer {
            Log.d(TAG, "${it.size}")
            abandonmentPublicRecyclerAdapter.setAbandonmentPublicList(it)
            abandonmentPublicRecyclerAdapter.notifyDataSetChanged()
        })
    }

    override fun initAfterBinding() {

    }



}