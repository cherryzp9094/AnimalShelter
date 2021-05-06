package com.cherryzp.animalshelter.ui.main.search

import android.content.Context
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SearchItemDecoration(private val context: Context, private val left: Int, private val top: Int, private val right: Int, private val bottom: Int): RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.left = dpToPx(left)
        outRect.right = dpToPx(right)
        outRect.top = dpToPx(top)
        outRect.bottom = dpToPx(bottom)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics).toInt()
    }

}