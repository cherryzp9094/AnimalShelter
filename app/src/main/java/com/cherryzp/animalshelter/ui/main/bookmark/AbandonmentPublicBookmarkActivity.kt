package com.cherryzp.animalshelter.ui.main.bookmark

import android.annotation.SuppressLint
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.base.BaseActivity
import com.cherryzp.animalshelter.databinding.ActivityAbandonmentPublicBookmarkBinding
import com.cherryzp.animalshelter.model.response.AbandonmentPublic
import com.cherryzp.animalshelter.room.entity.AbandonmentPublicEntity
import com.cherryzp.animalshelter.viewmodel.BookmarkViewModel
import com.cherryzp.animalshelter.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.activity_abandonment_public_bookmark.*
import kotlinx.android.synthetic.main.activity_abandonment_public_detail.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class AbandonmentPublicBookmarkActivity : BaseActivity<ActivityAbandonmentPublicBookmarkBinding, BookmarkViewModel>() {
    override val layoutResourceId: Int
        get() = R.layout.activity_abandonment_public_bookmark
    override val viewModel: BookmarkViewModel by viewModel()

    private val abandonmentPublicBookmarkRecyclerAdapter: AbandonmentPublicBookmarkRecyclerAdapter by inject()

    override fun initStartView() {
        bookmark_rv.adapter = abandonmentPublicBookmarkRecyclerAdapter
        bookmark_rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        abandonmentPublicBookmarkRecyclerAdapter.activity = this

        viewModel.animalListLiveData.observe(this) {
            abandonmentPublicBookmarkRecyclerAdapter.setAbandonmentPublicList(it as ArrayList<AbandonmentPublicEntity>)
            abandonmentPublicBookmarkRecyclerAdapter.notifyDataSetChanged()
        }
    }

    override fun initDataBinding() {

    }

    override fun initAfterBinding() {
        viewModel.getAll()
    }
}