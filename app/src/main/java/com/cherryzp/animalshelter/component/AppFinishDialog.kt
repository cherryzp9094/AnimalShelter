package com.cherryzp.animalshelter.component

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.databinding.DialogAppFinishBinding
import com.cherryzp.animalshelter.databinding.DialogContactBinding

class AppFinishDialog (private val context: Context) {

    lateinit var dialog: Dialog

    public fun showContactDialog() {
        dialog = Dialog(context)

        val databindng = DataBindingUtil.inflate<DialogAppFinishBinding>(
            LayoutInflater.from(context),
            R.layout.dialog_app_finish,
            null,
            false
        )

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.setContentView(databindng.root)

        databindng.okBtn.setOnClickListener(onClickListener)
        databindng.cancelBtn.setOnClickListener(onClickListener)

        val activity = context as Activity
        if (!activity.isFinishing) dialog.show()
    }

    private val onClickListener = View.OnClickListener {
        when (it.id) {
            R.id.ok_btn -> {
                ActivityCompat.finishAffinity(context as Activity)
            }
            R.id.cancel_btn -> {
                dialog.dismiss()
            }
        }
    }
}