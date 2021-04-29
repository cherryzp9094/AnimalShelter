package com.cherryzp.animalshelter.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.model.response.Shelter
import kotlinx.android.synthetic.main.recycler_main_list_item.view.*

class MainRecyclerAdatper: RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private var shelterList = ArrayList<Shelter>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return VH(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? VH)?.onBind(shelterList[position])
    }

    override fun getItemCount(): Int = shelterList.size

    class VH(parent: ViewGroup): RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.recycler_main_list_item, parent, false)
    ) {
        fun onBind(item: Shelter) {
            itemView.run {
                careNm.text = item.careNm
                careRegNo.text = item.careRegNo.toString()
            }
        }
    }

    fun setShelterList(shelterList: ArrayList<Shelter>) {
        shelterList.clear()
        this.shelterList = shelterList
    }
}