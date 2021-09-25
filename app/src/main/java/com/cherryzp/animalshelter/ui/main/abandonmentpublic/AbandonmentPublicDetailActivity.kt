package com.cherryzp.animalshelter.ui.main.abandonmentpublic

import android.content.Intent
import android.util.Log
import android.view.View
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.base.BaseActivity
import com.cherryzp.animalshelter.databinding.ActivityAbandonmentPublicDetailBinding
import com.cherryzp.animalshelter.model.response.AbandonmentPublic
import com.cherryzp.animalshelter.viewmodel.BookmarkViewModel
import com.cherryzp.animalshelter.viewmodel.MainViewModel
import com.cherryzp.animalshelter.viewmodel.SplashViewModel
import kotlinx.android.synthetic.main.activity_abandonment_public_detail.*
import kotlinx.android.synthetic.main.recycler_abandonment_public_list_item.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.getScopeId

class AbandonmentPublicDetailActivity : BaseActivity<ActivityAbandonmentPublicDetailBinding, BookmarkViewModel>() {

    private val TAG = "AbandonmentPublicActivity"

    override val layoutResourceId: Int
        get() = R.layout.activity_abandonment_public_detail
    override val viewModel: BookmarkViewModel by viewModel()

    private var abandonmentPublic: AbandonmentPublic? = null

    override fun initStartView() {
        dataBinding.abandonmentPublicDetailActivity = this

        popfile_iv.setOnClickListener(onClickListener)
        contact_btn.setOnClickListener(onClickListener)
        bookmark_iv.setOnClickListener(onClickListener)

//        abandonmentPublic = viewModel.abandonmentPublicListLiveData.value?.get(intent.getIntExtra("itemPosition", 0))

        abandonmentPublic = AbandonmentPublic()
        abandonmentPublic?.run {
            age = intent.getStringExtra("age")
            careAddr = intent.getStringExtra("careAddr")
            careNm = intent.getStringExtra("careNm")
            careTel = intent.getStringExtra("careTel")
            chargeNm = intent.getStringExtra("chargeNm")
            colorCd = intent.getStringExtra("colorCd")
            desertionNo = intent.getStringExtra("desertionNo")
            filename = intent.getStringExtra("filename")
            happenDt = intent.getStringExtra("happenDt")
            happenPlace = intent.getStringExtra("happenPlace")
            kindCd = intent.getStringExtra("kindCd")
            neuterYn = intent.getStringExtra("neuterYn")
            noticeEdt = intent.getStringExtra("noticeEdt")
            noticeNo = intent.getStringExtra("noticeNo")
            noticeSdt = intent.getStringExtra("noticeSdt")
            officetel = intent.getStringExtra("officetel")
            orgNm = intent.getStringExtra("orgNm")
            popfile = intent.getStringExtra("popfile")
            processState = intent.getStringExtra("processState")
            sexCd = intent.getStringExtra("sexCd")
            specialMark = intent.getStringExtra("specialMark")
            weight = intent.getStringExtra("weight")
        }

        dataBinding.abandonmentPublic = abandonmentPublic
    }

    override fun initDataBinding() {

    }

    override fun initAfterBinding() {

    }

    private val onClickListener = View.OnClickListener {
        when (it.id) {
            R.id.popfile_iv ->{
                val intent = Intent(this, DetailPhotoActivity::class.java)
                intent.putExtra("popfile", abandonmentPublic?.popfile)

                startActivity(intent)
            }
            R.id.contact_btn -> {
                val contactDialog = ContactDialog(this,  abandonmentPublic?.careTel, abandonmentPublic?.officetel)
                contactDialog.showContactDialog()
            }
            R.id.bookmark_iv -> {
                if (bookmark_iv.isChecked) {
                    abandonmentPublic?.let { abandonment -> viewModel.insert(abandonment) }
                } else {
                    viewModel.getAll()
                }
            }
        }
    }
}