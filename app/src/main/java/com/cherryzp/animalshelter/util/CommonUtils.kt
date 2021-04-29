package com.cherryzp.animalshelter.util

import android.content.Context
import com.cherryzp.animalshelter.AppApplication

class CommonUtils {

    companion object {

        fun getContext(): Context {
            return AppApplication.context
        }

    }
}