package com.cherryzp.animalshelter.util

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.cherryzp.animalshelter.model.response.AbandonmentPublic
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object BindingAdapter{

    @BindingAdapter("insert_img")
    @JvmStatic
    fun insertImg(view: ImageView, uri: String) {
        Glide.with(CommonUtils.getContext()).load(uri).into(view)
    }

    @BindingAdapter("insert_process_state")
    @JvmStatic
    fun insertProcessState(view: TextView, abandonmentPublic: AbandonmentPublic?){
        if (abandonmentPublic?.noticeEdt != null && abandonmentPublic.noticeEdt!!.toIntOrNull() != null) {
            val currentDateTime = Calendar.getInstance().time
            var dateFormat = SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(currentDateTime)

            val noticeEdtInt = abandonmentPublic.noticeEdt?.toInt()
            val currentInt = dateFormat.toInt()

            if (noticeEdtInt != null) {
                if (noticeEdtInt - currentInt > 0) {
                    view.text = "공고중"
                } else {
                    view.text = abandonmentPublic.processState
                }
            }
        }
    }
}