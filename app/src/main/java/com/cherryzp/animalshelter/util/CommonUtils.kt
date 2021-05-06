package com.cherryzp.animalshelter.util

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.cherryzp.animalshelter.AppApplication

class CommonUtils {

    companion object {

        fun getContext(): Context {
            return AppApplication.context
        }

        fun hideKeyboard(act: Activity) {
            val imm: InputMethodManager = act.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            var view: View? = act.currentFocus
            if (view == null) {
                view = View(act)
            }
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }
    }
}