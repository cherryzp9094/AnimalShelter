package com.cherryzp.animalshelter.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.cherryzp.animalshelter.AppApplication
import com.cherryzp.animalshelter.BR
import com.cherryzp.animalshelter.R

abstract class BaseActivity<T : ViewDataBinding, V : BaseViewModel> : AppCompatActivity() {

    lateinit var dataBinding: T

    /**
     * setContentView로 호출할 Layout의 리소스 Id.
     * ex) R.layout.activity_sbs_main
     */
    abstract val layoutResourceId: Int

    /**
     * viewModel 로 쓰일 변수.
     */
    abstract val viewModel: V

    /**
     * 레이아웃을 띄운 직후 호출.
     * 뷰나 액티비티의 속성 등을 초기화.
     * ex) 리사이클러뷰, 툴바, 드로어뷰..
     */
    abstract fun initStartView()

    /**
     * 두번째로 호출.
     * 데이터 바인딩 및 rxjava 설정.
     * ex) rxjava observe, databinding observe..
     */
    abstract fun initDataBinding()

    /**
     * 바인딩 이후에 할 일을 여기에 구현.
     * 그 외에 설정할 것이 있으면 이곳에서 설정.
     * 클릭 리스너도 이곳에서 설정.
     */
    abstract fun initAfterBinding()

    private var isSetBackButtonValid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startAnim()

        dataBinding = DataBindingUtil.setContentView(this, layoutResourceId)

        dataBinding.apply {
            lifecycleOwner = this@BaseActivity
            setVariable(BR.viewModel, viewModel)
        }

        /**
         * 레이아웃을 띄운 직후 호출.
         * 뷰나 액티비티의 속성 등을 초기화.
         * ex) 리사이클러뷰, 툴바, 드로어뷰..
         */
        initStartView()

        /**
         * 두번째로 호출.
         * 데이터 바인딩 및 rxjava 설정.
         * ex) rxjava observe, databinding observe..
         */
        initDataBinding()

        /**
         * 바인딩 이후에 할 일을 여기에 구현.
         * 그 외에 설정할 것이 있으면 이곳에서 설정.
         * 클릭 리스너도 이곳에서 설정.
         */
        initAfterBinding()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        backAnim()
    }

    fun startAnim() {
        overridePendingTransition(R.anim.horizon_enter, R.anim.none)
    }

    fun backAnim() {
        overridePendingTransition(R.anim.none, R.anim.horizon_exit)
    }

    fun progressOn() {
        AppApplication.appApplication.progressOn(this)
    }

    fun progressOff() {
        AppApplication.appApplication.progressOff()
    }
}