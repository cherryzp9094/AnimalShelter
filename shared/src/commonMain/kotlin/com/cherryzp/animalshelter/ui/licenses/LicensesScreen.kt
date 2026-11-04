package com.cherryzp.animalshelter.ui.licenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.a11y_loading
import com.cherryzp.animalshelter.resources.about_licenses
import com.cherryzp.animalshelter.resources.action_back
import com.cherryzp.animalshelter.resources.license_android_only
import com.cherryzp.animalshelter.resources.license_hide_text
import com.cherryzp.animalshelter.resources.license_show_text
import com.cherryzp.animalshelter.resources.license_web_only
import com.cherryzp.animalshelter.resources.licenses_intro
import com.cherryzp.animalshelter.ui.common.PART_SEPARATOR
import com.cherryzp.animalshelter.ui.common.TestTags
import com.cherryzp.animalshelter.ui.common.progressName
import com.cherryzp.animalshelter.ui.common.resolve
import com.cherryzp.animalshelter.ui.theme.AnimalShelterTheme
import kotlin.coroutines.cancellation.CancellationException
import org.jetbrains.compose.resources.stringResource

/** 본문 최대 폭(설정과 같다). */
private val ContentMaxWidth = 720.dp

/**
 * 한 `Text`에 담는 전문의 줄 수 상한. 웹 고지(약 4,300줄)를 한 덩어리로 두면 배치·의미 노드가 커서, 빈 줄에서
 * 끊어 여러 `Text`로 나눈다.
 */
private const val TEXT_CHUNK_LINES = 60

/**
 * 오픈소스 라이선스(5a O1, 상태만 — 정적 화면이라 ViewModel이 없다, P2의 예외). 설정 > 정보에서 연다. 맨 위 안내 뒤
 * [OpenSourceNotices]의 묶음마다 제목·줄(이름 · 저작권자/플랫폼/라이선스 · 주소)과 [전문 보기]. 전문은
 * `Res.readBytes`로 읽어 그 묶음 아래에 펼친다(펼침은 묶음마다 `rememberSaveable` — 재생성·프로세스 종료 뒤에도).
 * 주소·전문은 고르고 복사할 수 있고, 누르는 링크는 없다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.about_licenses),
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .testTag(TestTags.LICENSES_SCROLL)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.licenses_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                OpenSourceNotices.forEachIndexed { index, group ->
                    if (index > 0) HorizontalDivider()
                    NoticeGroupSection(group)
                }
            }
        }
    }
}

@Composable
private fun NoticeGroupSection(group: NoticeGroup) {
    var expanded by rememberSaveable(group.key) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = group.title.resolve(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() }
        )
        group.entries.forEach { NoticeRow(it) }
        group.licenseUrl?.let { url ->
            SelectionContainer(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(text = url, style = MaterialTheme.typography.bodySmall)
            }
        }
        val file = group.textFile ?: return@Column
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.padding(horizontal = 4.dp).heightIn(min = 48.dp)
                .testTag(TestTags.licenseToggle(group.key))
        ) {
            Text(
                stringResource(
                    if (expanded) Res.string.license_hide_text else Res.string.license_show_text
                )
            )
        }
        if (expanded) LicenseText(file)
    }
}

@Composable
private fun NoticeRow(entry: NoticeEntry) {
    val platform = when (entry.platform) {
        NoticePlatform.ANDROID -> stringResource(Res.string.license_android_only)
        NoticePlatform.WEB -> stringResource(Res.string.license_web_only)
        NoticePlatform.BOTH -> null
    }
    val detail = listOfNotNull(entry.holder, platform, entry.license).joinToString(PART_SEPARATOR)
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = entry.name, style = MaterialTheme.typography.bodyLarge)
        if (detail.isNotEmpty()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SelectionContainer {
            Text(text = entry.url, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** 전문을 읽는 동안·읽은 뒤·실패. */
private sealed interface LicenseTextState {
    data object Loading : LicenseTextState

    data class Loaded(val chunks: List<String>) : LicenseTextState

    data object Failed : LicenseTextState
}

/** 전문(펼친 자리): 읽는 동안 진행 표시(Y2), 실패하면 자리를 비운다(주소는 위에 남는다). */
@Composable
private fun LicenseText(file: String) {
    val state by produceState<LicenseTextState>(LicenseTextState.Loading, file) {
        value = try {
            LicenseTextState.Loaded(chunkLines(Res.readBytes(file).decodeToString()))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            LicenseTextState.Failed
        }
    }
    when (val current = state) {
        LicenseTextState.Loading -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            CircularProgressIndicator(
                Modifier.size(24.dp).progressName(stringResource(Res.string.a11y_loading)),
                strokeWidth = 2.dp
            )
        }

        is LicenseTextState.Loaded -> SelectionContainer(Modifier.padding(horizontal = 16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                current.chunks.forEach { chunk ->
                    Text(text = chunk, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        LicenseTextState.Failed -> Unit
    }
}

/** 전문을 [TEXT_CHUNK_LINES]줄 안팎에서, 빈 줄을 만나면 끊는다(빈 줄은 덩어리 사이의 간격이 된다). */
internal fun chunkLines(text: String): List<String> {
    val chunks = mutableListOf<String>()
    val current = mutableListOf<String>()
    fun flush() {
        if (current.any { it.isNotBlank() }) chunks += current.joinToString("\n").trim('\n')
        current.clear()
    }
    text.replace("\r\n", "\n").lines().forEach { line ->
        if (line.isBlank() && current.size >= TEXT_CHUNK_LINES) flush() else current += line
    }
    flush()
    return chunks
}

@Preview
@Composable
private fun LicensesScreenPreview() {
    AnimalShelterTheme {
        LicensesScreen(onBack = {})
    }
}
