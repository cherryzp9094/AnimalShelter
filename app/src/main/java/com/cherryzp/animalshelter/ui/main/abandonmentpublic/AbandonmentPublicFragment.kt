package com.cherryzp.animalshelter.ui.main.abandonmentpublic

import android.app.ActivityOptions
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.base.BaseFragment
import com.cherryzp.animalshelter.databinding.FragmentAbandonmentPublicBinding
import com.cherryzp.animalshelter.model.response.AbandonmentPublic
import com.cherryzp.animalshelter.ui.main.MainActivity
import com.cherryzp.animalshelter.util.CommonUtils
import com.cherryzp.animalshelter.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.recycler_abandonment_public_list_item.view.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class AbandonmentPublicFragment : BaseFragment<FragmentAbandonmentPublicBinding, MainViewModel>(){

    private val TAG = "AbandonPublicFragment"

    override val layoutResourceId: Int
        get() = R.layout.fragment_abandonment_public
    override val viewModel: MainViewModel by sharedViewModel()

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
        viewModel.abandonmentPublicListLiveData.observe(this) {
            Log.d(TAG, "${it.size}")

            if (it.size > 0) {
                dataBinding.searchNoItemTv.visibility = View.GONE
                abandonmentPublicRecyclerAdapter.setAbandonmentPublicList(it)
            } else {
                dataBinding.searchNoItemTv.visibility = View.VISIBLE
            }
            abandonmentPublicRecyclerAdapter.notifyDataSetChanged()

        }
    }

    override fun initAfterBinding() {

    }

    override fun onResume() {
        super.onResume()

        if (mainActivity.pageNo == 1) mainActivity.loadData()
    }
}
