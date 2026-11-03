package com.cherryzp.animalshelter.ui.shelter

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cherryzp.animalshelter.platform.ExternalActions
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.action_back
import com.cherryzp.animalshelter.resources.action_call
import com.cherryzp.animalshelter.resources.action_open_map
import com.cherryzp.animalshelter.resources.action_retry
import com.cherryzp.animalshelter.resources.action_shelter_notices
import com.cherryzp.animalshelter.resources.disclaimer
import com.cherryzp.animalshelter.resources.error_network
import com.cherryzp.animalshelter.resources.label_data_date
import com.cherryzp.animalshelter.resources.label_designated_on
import com.cherryzp.animalshelter.resources.label_jibun_address
import com.cherryzp.animalshelter.resources.label_road_address
import com.cherryzp.animalshelter.resources.label_shelter_tel
import com.cherryzp.animalshelter.resources.label_target_animals
import com.cherryzp.animalshelter.resources.section_facilities
import com.cherryzp.animalshelter.resources.shelter_limited_info
import com.cherryzp.animalshelter.resources.shelter_stale_notice
import com.cherryzp.animalshelter.resources.title_shelter
import com.cherryzp.animalshelter.ui.common.CenteredLoading
import com.cherryzp.animalshelter.ui.common.MessageSnackbarEffect
import com.cherryzp.animalshelter.ui.common.PART_SEPARATOR
import com.cherryzp.animalshelter.ui.common.resolve
import com.cherryzp.animalshelter.ui.common.uiText
import com.cherryzp.animalshelter.ui.detail.DetailRowView
import com.cherryzp.animalshelter.ui.detail.LabeledValue
import com.cherryzp.animalshelter.ui.detail.MapTarget
import com.cherryzp.animalshelter.ui.detail.NoticeBanner
import com.cherryzp.animalshelter.ui.preview.PreviewData
import com.cherryzp.animalshelter.ui.theme.AnimalShelterTheme
import com.cherryzp.animalshelter.ui.theme.TABULAR_NUMBERS
import org.jetbrains.compose.resources.stringResource

/** 본문 최대 폭(세 번째 칸 360dp에서도, 한 칸 넓은 창에서도 읽기 좋게). */
private val ContentMaxWidth = 720.dp

/** 숫자 값(전화·시간·날짜·시설 수)은 고정폭 숫자(P7). */
@Composable
private fun numberStyle() =
    MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR_NUMBERS)

/** 보호소 정보(ViewModel을 받아 상태를 모은다, P2). 전화·지도·복사는 여기서 부르고 결과만 알린다(B3). */
@Composable
fun ShelterInfoScreen(
    viewModel: ShelterInfoViewModel,
    onBack: () -> Unit,
    onShowNotices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ShelterInfoScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::onRetry,
        onShowNotices = {
            viewModel.onShowNotices()
            onShowNotices()
        },
        onDial = { viewModel.onExternalActionResult(ExternalActions.dial(it)) },
        onOpenMap = {
            viewModel.onExternalActionResult(ExternalActions.openMap(it.lat, it.lng, it.label))
        },
        onCopyAddress = { viewModel.onCopyResult(ExternalActions.copyText(it)) },
        onMessageShown = viewModel::onMessageShown,
        modifier = modifier
    )
}

/**
 * 보호소 정보(상태만, 03 2.6·브리핑 B12): 머리 / 연락·위치 / 시간 / 구조 대상 / 시설(접힘) / 이 보호소의 공고 /
 * 기준일 / 면책. 정보가 없으면(줄인 화면) 키의 이름·전화·주소만.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelterInfoScreen(
    state: ShelterInfoUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onShowNotices: () -> Unit,
    onDial: (String) -> Unit,
    onOpenMap: (MapTarget) -> Unit,
    onCopyAddress: (String) -> Unit,
    onMessageShown: (id: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    MessageSnackbarEffect(
        message = state.message,
        snackbarHostState = snackbarHostState,
        onShown = onMessageShown,
        onAction = { _, id -> onMessageShown(id) }
    )
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.title ?: stringResource(Res.string.title_shelter),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (state.isLoading) {
            CenteredLoading(Modifier.padding(innerPadding))
            return@Scaffold
        }
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            ShelterBody(
                state = state,
                onRetry = onRetry,
                onShowNotices = onShowNotices,
                onDial = onDial,
                onOpenMap = onOpenMap,
                onCopyAddress = onCopyAddress,
                modifier = Modifier
                    .widthIn(max = ContentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun ShelterBody(
    state: ShelterInfoUiState,
    onRetry: () -> Unit,
    onShowNotices: () -> Unit,
    onDial: (String) -> Unit,
    onOpenMap: (MapTarget) -> Unit,
    onCopyAddress: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val info = state.info
    val tel = info?.tel ?: state.fallback.tel
    val roadAddress = info?.roadAddress ?: state.fallback.address
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        if (state.isLimited) NoticeBanner(stringResource(Res.string.shelter_limited_info))
        state.error?.let { error ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NoticeBanner(error.resolve())
                if (state.canRetry) {
                    OutlinedButton(onClick = onRetry) {
                        Text(stringResource(Res.string.action_retry))
                    }
                }
            }
        }
        if (info != null && info.header.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                info.header.forEach { DetailRowView(it) }
            }
        }
        ContactSection(
            tel = tel,
            roadAddress = roadAddress,
            jibunAddress = info?.jibunAddress,
            map = info?.map,
            onDial = onDial,
            onOpenMap = onOpenMap,
            onCopyAddress = onCopyAddress
        )
        if (info != null && (state.today.isNotEmpty() || info.hours.isNotEmpty())) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.today.isNotEmpty()) {
                    Text(
                        text = state.today.map { it.resolve() }.joinToString(PART_SEPARATOR),
                        style = MaterialTheme.typography.titleMedium
                            .copy(fontFeatureSettings = TABULAR_NUMBERS),
                        modifier = Modifier.semantics { heading() }
                    )
                }
                info.hours.forEach { DetailRowView(it) }
            }
        }
        info?.targetAnimals?.let {
            LabeledValue(label = stringResource(Res.string.label_target_animals)) {
                Text(text = it, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (info != null && info.facilities.isNotEmpty()) FacilitiesSection(info.facilities)
        Button(onClick = onShowNotices, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.action_shelter_notices))
        }
        if (info != null) DatesSection(info, state.isDataOld)
        Text(
            text = stringResource(Res.string.disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 연락·위치: 전화(탭 → 다이얼), 도로명 주소(길게 눌러 복사), 지번(있을 때), 버튼 전화하기·지도에서 보기. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactSection(
    tel: String?,
    roadAddress: String?,
    jibunAddress: String?,
    map: MapTarget?,
    onDial: (String) -> Unit,
    onOpenMap: (MapTarget) -> Unit,
    onCopyAddress: (String) -> Unit,
) {
    if (tel == null && roadAddress == null && jibunAddress == null && map == null) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tel?.let {
            val callLabel = stringResource(Res.string.action_call)
            LabeledValue(
                label = stringResource(Res.string.label_shelter_tel),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(onClickLabel = callLabel) { onDial(it) }
            ) {
                Text(
                    text = it,
                    style = numberStyle(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        roadAddress?.let {
            LabeledValue(
                label = stringResource(Res.string.label_road_address),
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = {}, onLongClick = { onCopyAddress(it) })
            ) {
                Text(text = it, style = MaterialTheme.typography.bodyLarge)
            }
        }
        jibunAddress?.let {
            LabeledValue(
                label = stringResource(Res.string.label_jibun_address),
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = {}, onLongClick = { onCopyAddress(it) })
            ) {
                Text(text = it, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (tel != null || map != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tel?.let {
                    FilledTonalButton(onClick = { onDial(it) }) {
                        Text(stringResource(Res.string.action_call))
                    }
                }
                map?.let {
                    OutlinedButton(onClick = { onOpenMap(it) }) {
                        Text(stringResource(Res.string.action_open_map))
                    }
                }
            }
        }
    }
}

/** 시설(접힘, 값 있을 때만). 펼침 상태는 entry 안에서 저장된다. */
@Composable
private fun FacilitiesSection(lines: List<List<com.cherryzp.animalshelter.ui.common.UiText>>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button) { expanded = !expanded }
        ) {
            Text(
                text = stringResource(Res.string.section_facilities),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null
            )
        }
        if (expanded) {
            lines.forEach { parts ->
                Text(
                    text = parts.map { it.resolve() }.joinToString(PART_SEPARATOR),
                    style = numberStyle()
                )
            }
        }
    }
}

/** 기준일: 지정일·데이터 기준일, 2년 넘게 지났으면 안내. */
@Composable
private fun DatesSection(info: ShelterInfoUi, isDataOld: Boolean) {
    if (info.designatedOn == null && info.dataDate == null) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        info.designatedOn?.let {
            LabeledValue(label = stringResource(Res.string.label_designated_on)) {
                Text(it, style = numberStyle())
            }
        }
        info.dataDate?.let { date ->
            LabeledValue(label = stringResource(Res.string.label_data_date)) {
                Text(date, style = numberStyle())
            }
            if (isDataOld) {
                Text(
                    text = stringResource(Res.string.shelter_stale_notice, date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PreviewShelter(state: ShelterInfoUiState) {
    AnimalShelterTheme {
        ShelterInfoScreen(
            state = state,
            onBack = {},
            onRetry = {},
            onShowNotices = {},
            onDial = {},
            onOpenMap = {},
            onCopyAddress = {},
            onMessageShown = {}
        )
    }
}

@Preview
@Composable
private fun ShelterFullPreview() {
    PreviewShelter(PreviewData.shelterState())
}

@Preview
@Composable
private fun ShelterLimitedPreview() {
    PreviewShelter(
        ShelterInfoUiState(
            title = "제2동물보호센터",
            fallback = PreviewData.shelterFallback,
            isLimited = true
        )
    )
}

@Preview
@Composable
private fun ShelterErrorPreview() {
    PreviewShelter(
        ShelterInfoUiState(
            title = "제2동물보호센터",
            fallback = PreviewData.shelterFallback,
            error = uiText(Res.string.error_network),
            canRetry = true
        )
    )
}
