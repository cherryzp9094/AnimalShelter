package com.cherryzp.animalshelter.util

import android.content.Context
import com.cherryzp.animalshelter.AppApplication
import com.cherryzp.animalshelter.component.LoadingProgressDialog

class CommonUtils {

    companion object {

        fun getContext(): Context {
            return AppApplication.context
        }

    }
}