package com.cherryzp.animalshelter.ui.photo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import com.cherryzp.animalshelter.ui.theme.AnimalShelterTheme
import kotlin.test.Test

/**
 * 사진 뷰어 핀치 줌(브리핑 C9⑧, 기기만): 핀치로 확대하면 넘기기가 막히고, 두 번 탭으로 1배가 되면 다시 넘어간다.
 * 사진 주소는 네트워크를 타지 않는 값(불러오기 실패 → 빈 사진)이라 확대 틀만 본다.
 */
@OptIn(ExperimentalTestApi::class)
class PhotoViewerPinchTest {
    @Test
    fun pinchZoomBlocksPagingUntilBackToOneX() = runComposeUiTest {
        setContent {
            AnimalShelterTheme {
                PhotoViewerScreen(
                    state = PhotoViewerUiState(photos = listOf("", " "), title = "테스트"),
                    initialIndex = 0,
                    onClose = {},
                    onRetry = {}
                )
            }
        }
        onNode(hasText("1 / 2")).assertIsDisplayed()
        val photo = onAllNodesWithContentDescription("테스트", substring = true).onFirst()

        photo.performTouchInput {
            pinch(
                start0 = center - Offset(20f, 0f),
                end0 = center - Offset(300f, 0f),
                start1 = center + Offset(20f, 0f),
                end1 = center + Offset(300f, 0f)
            )
        }
        waitForIdle()
        photo.performTouchInput { swipeLeft() }
        waitForIdle()
        onNode(hasText("1 / 2")).assertIsDisplayed() // 확대 중에는 그대로

        photo.performTouchInput { doubleClick() }
        mainClock.advanceTimeBy(500)
        waitForIdle()
        photo.performTouchInput { swipeLeft() }
        waitForIdle()
        onNode(hasText("2 / 2")).assertIsDisplayed()
    }
}
