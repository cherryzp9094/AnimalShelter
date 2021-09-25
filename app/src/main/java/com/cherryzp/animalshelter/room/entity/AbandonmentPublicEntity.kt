package com.cherryzp.animalshelter.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "abandonment_public")
data class AbandonmentPublicEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    var age: String?,
    var careAddr: String?,
    var careNm: String?,
    var careTel: String?,
    var chargeNm: String?,
    var colorCd: String?,
    var desertionNo: String?,
    var filename: String?,
    var happenDt: String?,
    var happenPlace: String?,
    var kindCd: String?,
    var neuterYn: String?,
    var noticeEdt: String?,
    var noticeNo: String?,
    var noticeSdt: String?,
    var officetel: String?,
    var orgNm: String?,
    var popfile: String?,
    var processState: String?,
    var sexCd: String?,
    var specialMark: String?,
    var weight: String?
)