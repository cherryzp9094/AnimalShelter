package com.cherryzp.animalshelter.model.response

import android.os.Parcel
import android.os.Parcelable

class AbandonmentPublic(){
    var age: String? = null
    var careAddr: String? = null
    var careNm: String? = null
    var careTel: String? = null
    var chargeNm: String? = null
    var colorCd: String? = null
    var desertionNo: String? = null
    var filename: String? = null
    var happenDt: String? = null
    var happenPlace: String? = null
    var kindCd: String? = null
    var neuterYn: String? = null
    var noticeEdt: String? = null
    var noticeNo: String? = null
    var noticeSdt: String? = null
    var officetel: String? = null
    var orgNm: String? = null
    var popfile: String? = null
    var processState: String? = null
    var sexCd: String? = null
    var specialMark: String? = null
    var weight: String? = null

    override fun toString(): String {
        return "AbandonmentPublic(age=$age, careAddr=$careAddr, careNm=$careNm, careTel=$careTel, chargeNm=$chargeNm, colorCd=$colorCd, desertionNo=$desertionNo, filename=$filename, happenDt=$happenDt, happenPlace=$happenPlace, kindCd=$kindCd, neuterYn=$neuterYn, noticeEdt=$noticeEdt, noticeNo=$noticeNo, noticeSdt=$noticeSdt, officetel=$officetel, orgNm=$orgNm, popfile=$popfile, processState=$processState, sexCd=$sexCd, specialMark=$specialMark, weight=$weight)"
    }

}