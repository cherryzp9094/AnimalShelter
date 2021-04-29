package com.cherryzp.animalshelter.network

import android.util.Log
import com.cherryzp.animalshelter.AppApplication
import retrofit2.Call
import retrofit2.Response

abstract class CustomCallback<T> : retrofit2.Callback<T> {

    companion object {
        const val TAG = "CustomCallback"

        fun logLine(str: String) {
            if (str.length > 3000) {    // 텍스트가 3000자 이상이 넘어가면 줄
                Log.d(TAG, "응답 -> " +str.substring(0, 3000));
                logLine(str.substring(3000))
            } else {
                Log.d(TAG, "응답 -> $str");
            }
        }
    }

    abstract fun onSuccess(call: Call<T>, response: Response<T>)
    abstract fun onError(call: Call<T>, response: Response<T>)
    abstract fun onFail(call: Call<T>, t: Throwable)

    override fun onResponse(call: Call<T>, response: Response<T>) {
        when (response.code()) {
            200 -> {
                Log.d("success", response.toString() + "")
                logLine(response.body().toString())
                onSuccess(call, response)
                AppApplication.appApplication.progressOff()
            }
            400, 401, 402, 403, 404, 500 -> {
                Log.d("error", response.toString() + "")
                onError(call, response)
                AppApplication.appApplication.progressOff()
            }
            else -> {
                Log.e("error", response.toString() + "")
                Log.e("error", response.errorBody().toString())
                onError(call, response)
                AppApplication.appApplication.progressOff()
            }
        }

    }

    override fun onFailure(call: Call<T>, t: Throwable) {
        t.printStackTrace()
        onFail(call, t)
    }
}