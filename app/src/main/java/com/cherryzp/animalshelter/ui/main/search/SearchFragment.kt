package com.cherryzp.animalshelter.ui.main.search

import android.view.View
import android.widget.Toast
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

    lateinit var mainActivity: MainActivity

    override fun initStartView() {
        mainActivity = activity as MainActivity

        dataBinding.catBtn.setOnClickListener(onClickListener)
        dataBinding.dogBtn.setOnClickListener(onClickListener)
        dataBinding.etcBtn.setOnClickListener(onClickListener)
    }

    override fun initDataBinding() {

    }

    override fun initAfterBinding() {

    }

    val onClickListener = View.OnClickListener {
        when (it.id) {
            R.id.dog_btn -> {
                mainActivity.kind = resources.getString(R.string.kind_dog)
            }
            R.id.cat_btn -> {
                mainActivity.kind = resources.getString(R.string.kind_cat)
            }
            R.id.etc_btn -> {
                mainActivity.kind = resources.getString(R.string.kind_etc)
            }
        }
    }
}