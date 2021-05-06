package com.cherryzp.animalshelter.ui.main.search

interface SearchItemSelectListener {

    fun itemSelectedListener(itemKind: SearchRecyclerAdapter.SearchItemKind, selectItem: Any, isCancel: Boolean)

}