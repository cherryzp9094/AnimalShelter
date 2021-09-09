package com.cherryzp.animalshelter.di

import com.cherryzp.animalshelter.AppApplication
import com.cherryzp.animalshelter.model.DataModel
import com.cherryzp.animalshelter.model.DataModelImpl
import com.cherryzp.animalshelter.repository.ShelterRepository
import com.cherryzp.animalshelter.service.ShelterService
import com.cherryzp.animalshelter.ui.main.MainRecyclerAdatper
import com.cherryzp.animalshelter.ui.main.MainViewModel
import com.cherryzp.animalshelter.ui.main.abandonmentpublic.AbandonmentPublicRecyclerAdapter
import com.cherryzp.animalshelter.ui.main.search.SearchRecyclerAdapter
import com.cherryzp.animalshelter.ui.splash.SplashViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

val retrofitModule = module {
    single {
        Retrofit.Builder()
            .baseUrl("http://openapi.animal.go.kr/openapi/service/rest/abandonmentPublicSrvc/")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(ShelterService::class.java)
    }
}

val adapterModule = module {
    factory {
        MainRecyclerAdatper()
    }
    factory {
        AbandonmentPublicRecyclerAdapter()
    }
    factory {
        SearchRecyclerAdapter()
    }
}

val modelModule = module {
    factory<DataModel> {
        DataModelImpl(get())
    }
    single {
        ShelterRepository(androidContext() as AppApplication)
    }
}

val viewModelModule = module {
    factory {
        SplashViewModel(get())
    }
    single {
        MainViewModel(get(), get())
    }
}

val shelterModule = listOf(retrofitModule, adapterModule, modelModule, viewModelModule)