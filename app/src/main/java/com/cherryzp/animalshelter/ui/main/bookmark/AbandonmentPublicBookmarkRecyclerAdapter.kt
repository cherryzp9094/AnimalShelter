package com.cherryzp.animalshelter.ui.main.bookmark

import android.app.ActivityOptions
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.cherryzp.animalshelter.R
import com.cherryzp.animalshelter.databinding.RecyclerAbandonmentPublicBookmarkListItemBinding
import com.cherryzp.animalshelter.room.entity.AbandonmentPublicEntity
import com.cherryzp.animalshelter.util.CommonUtils
import kotlinx.android.synthetic.main.recycler_abandonment_public_list_item.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AbandonmentPublicBookmarkRecyclerAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TAG = "AbandonmentPublicBookmarkRecyclerAdapter"

    private var abandonmentPublicList = ArrayList<AbandonmentPublicEntity>()

    lateinit var activity: AbandonmentPublicBookmarkActivity

    private var isSelectItem = false;

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return VH(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.recycler_abandonment_public_bookmark_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? VH)?.onBind(abandonmentPublicList[position])
    }

    override fun getItemCount(): Int = abandonmentPublicList.size

    fun setAbandonmentPublicList(abandonmentPublicList: ArrayList<AbandonmentPublicEntity>) {
        this.abandonmentPublicList = abandonmentPublicList
    }

    inner class VH(private val binding: RecyclerAbandonmentPublicBookmarkListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(abandonmentPublicItem: AbandonmentPublicEntity) {
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
                        AbandonmentPublicBookmarkDetailActivity::class.java
                    )

                    insertIntentAbandonmentItem(abandonmentPublicItem, intent)

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

        //AbandonmentPublic 데이터 인텐트에 넣기
        private fun insertIntentAbandonmentItem(abandonmentPublicItem: AbandonmentPublicEntity, intent: Intent): Intent {
            intent.putExtra("age", abandonmentPublicItem.age)
            intent.putExtra("careAddr", abandonmentPublicItem.careAddr)
            intent.putExtra("careNm", abandonmentPublicItem.careNm)
            intent.putExtra("careTel", abandonmentPublicItem.careTel)
            intent.putExtra("chargeNm", abandonmentPublicItem.chargeNm)
            intent.putExtra("colorCd", abandonmentPublicItem.colorCd)
            intent.putExtra("desertionNo", abandonmentPublicItem.desertionNo)
            intent.putExtra("filename", abandonmentPublicItem.filename)
            intent.putExtra("happenDt", abandonmentPublicItem.happenDt)
            intent.putExtra("happenPlace", abandonmentPublicItem.happenPlace)
            intent.putExtra("kindCd", abandonmentPublicItem.kindCd)
            intent.putExtra("neuterYn", abandonmentPublicItem.neuterYn)
            intent.putExtra("noticeEdt", abandonmentPublicItem.noticeEdt)
            intent.putExtra("noticeNo", abandonmentPublicItem.noticeNo)
            intent.putExtra("noticeSdt", abandonmentPublicItem.noticeSdt)
            intent.putExtra("officetel", abandonmentPublicItem.officetel)
            intent.putExtra("orgNm", abandonmentPublicItem.orgNm)
            intent.putExtra("popfile", abandonmentPublicItem.popfile)
            intent.putExtra("processState", abandonmentPublicItem.processState)
            intent.putExtra("sexCd", abandonmentPublicItem.sexCd)
            intent.putExtra("specialMark", abandonmentPublicItem.specialMark)
            intent.putExtra("weight", abandonmentPublicItem.weight)

            return intent
        }

    }
}