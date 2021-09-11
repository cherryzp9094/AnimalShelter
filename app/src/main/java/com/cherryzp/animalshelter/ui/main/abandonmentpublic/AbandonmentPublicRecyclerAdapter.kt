package com.cherryzp.animalshelter.ui.main.abandonmentpublic

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.databinding.RecyclerAbandonmentPublicListItemBinding
import com.cherryzp.animalshelter.model.response.AbandonmentPublic
import com.cherryzp.animalshelter.ui.main.MainActivity
import com.cherryzp.animalshelter.util.BindingAdapter
import com.cherryzp.animalshelter.util.CommonUtils
import kotlinx.android.synthetic.main.fragment_abandonment_public.view.*
import kotlinx.android.synthetic.main.recycler_abandonment_public_list_item.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.logging.Handler

class AbandonmentPublicRecyclerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private val TAG = "AbandonmentPublicRecyclerAdapter"

    private var abandonmentPublicList = ArrayList<AbandonmentPublic>()

    lateinit var activity: MainActivity

    private var isSelectItem = false;

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return VH(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.recycler_abandonment_public_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? VH)?.onBind(abandonmentPublicList[position])
    }

    override fun getItemCount(): Int = abandonmentPublicList.size

    inner class VH(private val binding: RecyclerAbandonmentPublicListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(abandonmentPublicItem: AbandonmentPublic) {
            with(binding) {
                abandonmentPublic = abandonmentPublicItem
                executePendingBindings()
            }

            itemView.run {
                popfile_iv.run {
                    setBackgroundResource(R.drawable.background_round_image)
                    clipToOutline = true
                }

                item_view.setOnClickListener {

                    if (isSelectItem) return@setOnClickListener

                    isSelectItem = true

                    val intent = Intent(
                        CommonUtils.getContext(),
                        AbandonmentPublicDetailActivity::class.java
                    )
                    intent.putExtra("itemPosition", layoutPosition)

                    val options = ActivityOptions.makeSceneTransitionAnimation(
                        activity,
                        popfile_iv,
                        "profile_image"
                    )

                    ContextCompat.startActivity(context, intent, options.toBundle())

                    //연속 클릭 방지
                    GlobalScope.launch {
                        delay(1000)
                        isSelectItem = false
                    }
                }
            }
        }

    }

    fun setAbandonmentPublicList(abandonmentPublicList: ArrayList<AbandonmentPublic>) {
        this.abandonmentPublicList = abandonmentPublicList
    }
}