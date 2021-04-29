package com.cherryzp.animalshelter.ui.main.abandonmentpublic

import android.app.ActivityOptions
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableField
import com.bumptech.glide.Glide
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.base.BaseActivity
import com.cherryzp.animalshelter.databinding.ActivityAbandonmentPublicDetailBinding
import com.cherryzp.animalshelter.model.response.AbandonmentPublic
import com.cherryzp.animalshelter.ui.main.MainViewModel
import com.cherryzp.animalshelter.util.CommonUtils
import kotlinx.android.synthetic.main.activity_abandonment_public_detail.*
import kotlinx.android.synthetic.main.recycler_abandonment_public_list_item.view.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class AbandonmentPublicDetailActivity : BaseActivity<ActivityAbandonmentPublicDetailBinding, MainViewModel>() {

    private val TAG = "AbandonmentPublicActivity"

    override val layoutResourceId: Int
        get() = R.layout.activity_abandonment_public_detail
    override val viewModel: MainViewModel by viewModel()

    private var abandonmentPublic: AbandonmentPublic? = null

    override fun initStartView() {
        dataBinding.abandonmentPublicDetailActivity = this

        popfile_iv.setOnClickListener(onClickListener)

        abandonmentPublic = viewModel.abandonmentPublicListLiveData.value?.get(intent.getIntExtra("itemPosition", 0))

        dataBinding.abandonmentPublic = abandonmentPublic
    }

    override fun initDataBinding() {

    }

    override fun initAfterBinding() {

    }

    val onClickListener = View.OnClickListener {
        when (it.id) {
            R.id.popfile_iv ->{
                val intent = Intent(this, DetailPhotoActivity::class.java)
                intent.putExtra("popfile", abandonmentPublic?.popfile)

                startActivity(intent)
            }
        }
    }
}