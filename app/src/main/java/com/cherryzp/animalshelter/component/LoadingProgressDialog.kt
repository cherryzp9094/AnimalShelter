package com.cherryzp.animalshelter.component

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.util.CommonUtils

class LoadingProgressDialog(context: Context) : Dialog(context){

    private val TAG = "LoadingProgressDialog"

    init {
        setCancelable(false)
        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setContentView(R.layout.dialog_loading_progress)
    }

}