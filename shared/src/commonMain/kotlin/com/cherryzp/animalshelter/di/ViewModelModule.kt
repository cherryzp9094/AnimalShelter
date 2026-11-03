package com.cherryzp.animalshelter.di

import com.cherryzp.animalshelter.navigation.AnimalDetailKey
import com.cherryzp.animalshelter.ui.detail.AnimalDetailViewModel
import com.cherryzp.animalshelter.ui.filter.FilterViewModel
import com.cherryzp.animalshelter.ui.list.AnimalListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * 화면 ViewModel(브리핑 P1·P3). Nav3 entry 안에서 `koinViewModel()`로 꺼낸다(entry별 범위).
 * 키가 필요한 것은 `koinViewModel { parametersOf(key) }`.
 */
val viewModelModule: Module = module {
    viewModelOf(::AnimalListViewModel)
    viewModelOf(::FilterViewModel)
    viewModel { (key: AnimalDetailKey) ->
        AnimalDetailViewModel(key, get(), get(), get(), get(), get(), get(), get())
    }
}
