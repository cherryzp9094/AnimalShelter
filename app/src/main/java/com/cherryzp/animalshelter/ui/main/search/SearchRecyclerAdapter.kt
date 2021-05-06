package com.cherryzp.animalshelter.ui.main.search

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.model.response.Shelter
import com.cherryzp.animalshelter.model.response.Sido
import com.cherryzp.animalshelter.model.response.Sigungu
import com.cherryzp.animalshelter.ui.main.MainActivity
import com.cherryzp.animalshelter.util.CommonUtils
import kotlinx.android.synthetic.main.recycler_search_item.view.*

class SearchRecyclerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    enum class SearchItemKind {
        SIDO, SIGUNGU, SHELTER
    }

    lateinit var activity: MainActivity
    lateinit var itemKind: SearchItemKind
    lateinit var searchItemSelectListener: SearchItemSelectListener

    private var searchList = ArrayList<Any>()

    var activatedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return VH(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? SearchRecyclerAdapter.VH)?.onBind(searchList[position])
    }

    override fun getItemCount(): Int = searchList.size

    fun resetActivatedPosition() {
        activatedPosition = -1
    }

    inner class VH(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.recycler_search_item, parent, false)
    ) {
        @SuppressLint("ClickableViewAccessibility")
        fun onBind(item: Any) {
            itemView.run {
                when (itemKind) {
                    SearchItemKind.SIDO -> {
                        val item: Sido = item as Sido

                        search_item_tv.text = item.orgdownNm
                    }

                    SearchItemKind.SIGUNGU -> {
                        val item: Sigungu = item as Sigungu

                        search_item_tv.text = item.orgdownNm
                    }

                    SearchItemKind.SHELTER -> {
                        val item: Shelter = item as Shelter

                        search_item_tv.text = item.careNm
                    }
                }

                item_layout.tag = layoutPosition
                item_layout.isActivated = layoutPosition == activatedPosition
                item_layout.setOnClickListener(onClickListener)
            //                item_layout.setOnTouchListener(onTouchLiListener)
            }
        }
    }

    fun setSearchList(searchList: ArrayList<Any>) {
        this.searchList = searchList
    }

    val onClickListener = View.OnClickListener {
        CommonUtils.hideKeyboard(activity)

        val position: Int = it.tag as Int

        if (activatedPosition == position) {
            this.resetActivatedPosition()
            notifyDataSetChanged()
            searchItemSelectListener.itemSelectedListener(itemKind, searchList[position], true)
        } else {
            activatedPosition = position
            notifyDataSetChanged()
            searchItemSelectListener.itemSelectedListener(itemKind, searchList[position], false)
        }

    }

}