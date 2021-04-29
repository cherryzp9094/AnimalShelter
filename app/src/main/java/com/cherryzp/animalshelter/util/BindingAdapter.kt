package com.cherryzp.animalshelter.util

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

object BindingAdapter{

    @BindingAdapter("insert_img")
    @JvmStatic
    fun insertImg(view: ImageView, uri: String) {
        Glide.with(CommonUtils.getContext()).load(uri).into(view)
    }
}