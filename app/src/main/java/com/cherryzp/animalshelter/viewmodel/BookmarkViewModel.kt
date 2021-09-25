package com.cherryzp.animalshelter.viewmodel

import androidx.lifecycle.viewModelScope
import com.cherryzp.animalshelter.base.BaseViewModel
import com.cherryzp.animalshelter.model.response.AbandonmentPublic
import com.cherryzp.animalshelter.repository.ShelterRepository
import com.cherryzp.animalshelter.room.entity.AbandonmentPublicEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BookmarkViewModel(private val repository: ShelterRepository): BaseViewModel() {

    fun insert(abandonmentPublic: AbandonmentPublic) {
        val abandonmentPublicEntity = abandonmentPublic.let {
            AbandonmentPublicEntity(
                age = it.age,
                careAddr = it.careAddr,
                careNm = it.careNm,
                careTel = it.careTel,
                chargeNm = it.chargeNm,
                colorCd = it.colorCd,
                desertionNo = it.desertionNo,
                filename = it.filename,
                happenDt = it.happenDt,
                happenPlace = it.happenPlace,
                kindCd = it.kindCd,
                neuterYn = it.neuterYn,
                noticeEdt = it.noticeEdt,
                noticeNo = it.noticeNo,
                noticeSdt = it.noticeSdt,
                officetel = it.officetel,
                orgNm = it.orgNm,
                popfile = it.popfile,
                processState = it.processState,
                sexCd = it.sexCd,
                specialMark = it.specialMark,
                weight = it.weight
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(abandonmentPublicEntity)
        }
    }

    fun delete() {
        viewModelScope.launch(Dispatchers.IO) {

        }
    }

    fun getAll() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getAll()
        }
    }

}