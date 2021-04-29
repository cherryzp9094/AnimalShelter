package com.cherryzp.animalshelter.util

import android.util.Log
import com.cherryzp.animalshelter.model.response.AbandonmentPublic
import com.cherryzp.animalshelter.model.response.Shelter
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

class ParseUtils {

    companion object {
        const val TAG = "ParseUtils"

        val xmlPullParserFactory = XmlPullParserFactory.newInstance()
        val parser = xmlPullParserFactory.newPullParser()

        fun parseShelter(xmlData: String): ArrayList<Shelter> {
            parser.setInput(StringReader(xmlData))

            val shelterList = ArrayList<Shelter>()

            var event = parser.eventType

            var careNm: String? = null
            var careRegNo: String? = null

            while (event != XmlPullParser.END_DOCUMENT) {
                when(event) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name.equals("careNm")) {
                            careNm = parser.nextText()
                        } else if (parser.name.equals("careRegNo")) {
                            careRegNo = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("item")) {
                            val shelter = Shelter(careNm!!, careRegNo!!.toLong())
                            shelterList.add(shelter)

                            careNm = null
                            careRegNo = null
                        }
                    }
                }
                event = parser.next()
            }
            Log.d(TAG, "파싱완료")

            return shelterList
        }

        //유기동물 정보 파싱
        fun parseAbandonment(xmlData: String): Pair<ArrayList<AbandonmentPublic>, String> {
            parser.setInput(StringReader(xmlData))

            var totalCount = "0"

            val abandonmentPublicList = ArrayList<AbandonmentPublic>()

            var event = parser.eventType
            var abandonmentPublic = AbandonmentPublic()

            while (event != XmlPullParser.END_DOCUMENT) {
                when(event) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name.equals("age")) {
                            abandonmentPublic.age = parser.nextText()

                        } else if (parser.name.equals("careAddr")) {
                            abandonmentPublic.careAddr = parser.nextText()

                        } else if (parser.name.equals("careNm")) {
                            abandonmentPublic.careNm = parser.nextText()

                        } else if (parser.name.equals("careTel")) {
                            abandonmentPublic.careTel = parser.nextText()

                        } else if (parser.name.equals("chargeNm")) {
                            abandonmentPublic.chargeNm = parser.nextText()

                        } else if (parser.name.equals("colorCd")) {
                            abandonmentPublic.colorCd = parser.nextText()

                        } else if (parser.name.equals("desertionNo")) {
                            abandonmentPublic.desertionNo = parser.nextText()

                        } else if (parser.name.equals("filename")) {
                            abandonmentPublic.filename = parser.nextText()

                        } else if (parser.name.equals("happenDt")) {
                            abandonmentPublic.happenDt = parser.nextText()

                        } else if (parser.name.equals("happenPlace")) {
                            abandonmentPublic.happenPlace = parser.nextText()

                        } else if (parser.name.equals("kindCd")) {
                            abandonmentPublic.kindCd = parser.nextText()

                        } else if (parser.name.equals("neuterYn")) {
                            abandonmentPublic.neuterYn = parser.nextText()

                        } else if (parser.name.equals("noticeEdt")) {
                            abandonmentPublic.noticeEdt = parser.nextText()

                        } else if (parser.name.equals("noticeNo")) {
                            abandonmentPublic.noticeNo = parser.nextText()

                        } else if (parser.name.equals("noticeSdt")) {
                            abandonmentPublic.noticeSdt = parser.nextText()

                        } else if (parser.name.equals("officetel")) {
                            abandonmentPublic.officetel = parser.nextText()

                        } else if (parser.name.equals("orgNm")) {
                            abandonmentPublic.orgNm = parser.nextText()

                        } else if (parser.name.equals("popfile")) {
                            abandonmentPublic.popfile = parser.nextText()

                        } else if (parser.name.equals("processState")) {
                            abandonmentPublic.processState = parser.nextText()

                        } else if (parser.name.equals("sexCd")) {
                            abandonmentPublic.sexCd = parser.nextText()

                        } else if (parser.name.equals("specialMark")) {
                            abandonmentPublic.specialMark = parser.nextText()

                        } else if (parser.name.equals("weight")) {
                            abandonmentPublic.weight = parser.nextText()

                        } else if (parser.name.equals("totalCount")) {
                            totalCount = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("item")) {
                            abandonmentPublicList.add(abandonmentPublic)

                            abandonmentPublic = AbandonmentPublic()
                        }
                    }
                }
                event = parser.next()
            }
            Log.d(TAG, "파싱완료")

            return Pair(abandonmentPublicList, totalCount)
        }

    }

}