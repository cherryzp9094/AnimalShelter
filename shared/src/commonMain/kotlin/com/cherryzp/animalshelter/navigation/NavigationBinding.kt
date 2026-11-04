package com.cherryzp.animalshelter.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * 플랫폼 탐색 연결(브리핑 W4 — expect를 늘리지 않고 인터페이스로 주입한다). 웹은 `:webApp`이 브라우저 기록
 * (`history`·`popstate`)과, Android는 `MainActivity`가 App Links 진입 스택(L3)을 준다.
 */
interface NavigationBinding {
    /**
     * 처음 back stack. `rememberNavBackStack`은 저장된 상태가 있으면 그것을 쓰므로(회전·프로세스 복원) 그때는
     * 쓰이지 않는다 — 링크를 다시 적용하지 않는다(L3).
     */
    val initialStack: List<NavKey>

    /** back stack과 플랫폼을 잇는다. `AppNavDisplay`가 back stack을 만든 뒤 한 번 부른다. */
    @Composable
    fun Bind(backStack: NavBackStack<NavKey>)

    companion object {
        /** 목록에서 시작하고 아무것도 잇지 않는다(테스트·기본). */
        val None: NavigationBinding = StartAt(listOf(AnimalListKey))
    }

    /** [initialStack]만 주고 잇는 것은 없는 연결(Android). */
    class StartAt(override val initialStack: List<NavKey>) : NavigationBinding {
        @Composable
        override fun Bind(backStack: NavBackStack<NavKey>) = Unit
    }
}
