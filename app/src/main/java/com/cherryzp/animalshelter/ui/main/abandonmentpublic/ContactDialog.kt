package com.cherryzp.animalshelter.ui.main.abandonmentpublic

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import androidx.databinding.DataBindingUtil
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.databinding.DialogContactBinding

class ContactDialog(private val context: Context, private val shelterNumber: String?, private val guardianNumber: String?) {

    lateinit var dialog: Dialog

    public fun showContactDialog() {
        dialog = Dialog(context)

        val databindng = DataBindingUtil.inflate<DialogContactBinding>(LayoutInflater.from(context), R.layout.dialog_contact, null, false)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.setContentView(databindng.root)

        databindng.callGuardianBtn.setOnClickListener(onClickListener)
        databindng.callShelterBtn.setOnClickListener(onClickListener)

        val activity = context as Activity
        if (!activity.isFinishing) dialog.show()
    }

    private val onClickListener = View.OnClickListener{
        when (it.id) {
            R.id.call_shelter_btn -> {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$shelterNumber"))
                context.startActivity(intent)
            }
            R.id.call_guardian_btn -> {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$guardianNumber"))
                context.startActivity(intent)
            }
        }
        dialog.dismiss()
    }

}