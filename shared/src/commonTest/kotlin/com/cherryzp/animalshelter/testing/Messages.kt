package com.cherryzp.animalshelter.testing

import com.cherryzp.animalshelter.ui.common.UiMessage

/** 안내 비교는 문구·버튼으로(중간 검토 F3 — `id`는 낼 때마다 달라진다). */
fun UiMessage?.content(): UiMessage? = this?.copy(id = 0)
