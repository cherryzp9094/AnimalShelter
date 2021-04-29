package com.cherryzp.animalshelter.ui.splash

import android.content.Intent
import android.os.Handler
import androidx.lifecycle.Observer
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.base.BaseActivity
import com.cherryzp.animalshelter.databinding.ActivitySplashBinding
import com.cherryzp.animalshelter.ui.main.MainActivity
import com.cherryzp.animalshelter.util.CommonUtils
import kotlinx.android.synthetic.main.activity_splash.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class SplashActivity : BaseActivity<ActivitySplashBinding, SplashViewModel>() {
    override val layoutResourceId: Int
        get() = R.layout.activity_splash
    override val viewModel: SplashViewModel by viewModel()

    val handler = Handler()

    override fun initStartView() {
        viewModel.sido()
    }

    override fun initDataBinding() {

    }

    override fun initAfterBinding() {
        viewModel.loadData.observe(this, Observer {
            if (it) {
                handler.postDelayed(Runnable {
                    intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                }, 2000)
            }
        })
    }
}