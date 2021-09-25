package com.cherryzp.animalshelter.ui.main.bookmark

import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.base.BaseActivity
import com.cherryzp.animalshelter.databinding.ActivityAbandonmentPublicBookmarkBinding
import com.cherryzp.animalshelter.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class AbandonmentPublicBookmarkActivity : BaseActivity<ActivityAbandonmentPublicBookmarkBinding, MainViewModel>() {
    override val layoutResourceId: Int
        get() = R.layout.activity_abandonment_public_bookmark
    override val viewModel: MainViewModel by viewModel()

    override fun initStartView() {
        TODO("Not yet implemented")
    }

    override fun initDataBinding() {
        TODO("Not yet implemented")
    }

    override fun initAfterBinding() {
        TODO("Not yet implemented")
    }
}