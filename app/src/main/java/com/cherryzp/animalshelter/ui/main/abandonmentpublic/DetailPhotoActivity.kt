package com.cherryzp.animalshelter.ui.main.abandonmentpublic

import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.bumptech.glide.Glide
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.base.BaseActivity
import com.cherryzp.animalshelter.databinding.ActivityDetailPhotoBinding
import com.cherryzp.animalshelter.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.activity_detail_photo.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class DetailPhotoActivity : BaseActivity<ActivityDetailPhotoBinding, MainViewModel>() {

    override val layoutResourceId: Int
        get() = R.layout.activity_detail_photo
    override val viewModel: MainViewModel by viewModel()

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor: Float = 1.0F

    override fun initStartView() {
        Glide.with(this).load(intent.getStringExtra("popfile")).into(popfile_iv)

        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
    }

    override fun initDataBinding() {

    }

    override fun initAfterBinding() {

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            scaleGestureDetector.onTouchEvent(it)
        }
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {

            scaleFactor *= scaleGestureDetector.scaleFactor

            scaleFactor = Math.max(0.1F, Math.min(scaleFactor, 10.0F))

            popfile_iv.run {
                scaleX = scaleFactor
                scaleY = scaleFactor
            }
            return true
        }
    }
}