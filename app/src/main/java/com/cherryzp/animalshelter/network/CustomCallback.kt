package com.cherryzp.animalshelter.network

import android.util.Log
import com.cherryzp.animalshelter.util.CommonUtils
import retrofit2.Call
import retrofit2.Response

abstract class CustomCallback<T> : retrofit2.Callback<T> {
    abstract fun onSuccess(call: Call<T>, response: Response<T>)
    abstract fun onError(call: Call<T>, response: Response<T>)
    abstract fun onFail(call: Call<T>, t: Throwable)

    override fun onResponse(call: Call<T>, response: Response<T>) {
        when (response.code()) {
            200 -> {
                Log.d("success", response.toString() + "")
                onSuccess(call, response)
            }
            400, 401, 402, 403, 404, 500 -> {
                Log.d("error", response.toString() + "")
                onError(call, response)
            }
            else -> {
                Log.e("error", response.toString() + "")
                onError(call, response)
            }
        }

    }

    override fun onFailure(call: Call<T>, t: Throwable) {
        t.printStackTrace()
        onFail(call, t)

    }
}