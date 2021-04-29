package com.cherryzp.animalshelter

import android.app.Activity
import android.app.Application
import android.content.Context
import com.cherryzp.animalshelter.common.CommonSingleton
import com.cherryzp.animalshelter.component.LoadingProgressDialog
import com.cherryzp.animalshelter.di.shelterModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AppApplication : Application() {

    private var loadingProgressDialog: LoadingProgressDialog? = null

    companion object {
        lateinit var context: Context
        lateinit var appApplication: AppApplication
    }

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AppApplication)
            androidLogger(Level.DEBUG)
            modules(shelterModule)
        }
        context = applicationContext
        appApplication = this@AppApplication
    }

    fun progressOn(activity: Activity) {
        if (activity.isFinishing) {
            return
        }

        if (CommonSingleton.PROGRESS_COUNT <= 0) {
            if (loadingProgressDialog == null || !loadingProgressDialog!!.isShowing) {
                loadingProgressDialog = LoadingProgressDialog(activity)
                loadingProgressDialog!!.show()
            }
            CommonSingleton.PROGRESS_COUNT = 0
        }
        CommonSingleton.PROGRESS_COUNT++
    }

    fun progressOff() {

        CommonSingleton.PROGRESS_COUNT--

        if (CommonSingleton.PROGRESS_COUNT <= 0) {
            if (loadingProgressDialog != null && loadingProgressDialog!!.isShowing) {
                loadingProgressDialog!!.dismiss()
            }
            CommonSingleton.PROGRESS_COUNT = 0
        }

    }

}