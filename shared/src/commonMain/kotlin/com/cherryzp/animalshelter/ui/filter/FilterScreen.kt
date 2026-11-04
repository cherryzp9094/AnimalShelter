package com.cherryzp.animalshelter.ui.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cherryzp.animalshelter.data.repository.CacheState
import com.cherryzp.animalshelter.data.repository.CodeTable
import com.cherryzp.animalshelter.model.AnimalKind
import com.cherryzp.animalshelter.model.Kind
import com.cherryzp.animalshelter.model.Neutered
import com.cherryzp.animalshelter.model.NoticeState
import com.cherryzp.animalshelter.model.SearchFilter
import com.cherryzp.animalshelter.model.Sex
import com.cherryzp.animalshelter.model.ShelterCode
import com.cherryzp.animalshelter.model.Sido
import com.cherryzp.animalshelter.model.Sigungu
import com.cherryzp.animalshelter.resources.Res
import com.cherryzp.animalshelter.resources.action_back
import com.cherryzp.animalshelter.resources.choice_all
import com.cherryzp.animalshelter.resources.choice_no
import com.cherryzp.animalshelter.resources.choice_unknown
import com.cherryzp.animalshelter.resources.choice_yes
import com.cherryzp.animalshelter.resources.filter_apply
import com.cherryzp.animalshelter.resources.filter_breed
import com.cherryzp.animalshelter.resources.filter_breed_hint_select_kind
import com.cherryzp.animalshelter.resources.filter_breed_search
import com.cherryzp.animalshelter.resources.filter_date
import com.cherryzp.animalshelter.resources.filter_kind
import com.cherryzp.animalshelter.resources.filter_neuter
import com.cherryzp.animalshelter.resources.filter_region
import com.cherryzp.animalshelter.resources.filter_region_shelter
import com.cherryzp.animalshelter.resources.filter_region_sido
import com.cherryzp.animalshelter.resources.filter_region_sigungu
import com.cherryzp.animalshelter.resources.filter_reset
import com.cherryzp.animalshelter.resources.filter_sex
import com.cherryzp.animalshelter.resources.filter_state
import com.cherryzp.animalshelter.resources.filter_state_closing_soon_hint
import com.cherryzp.animalshelter.resources.filter_state_hint
import com.cherryzp.animalshelter.resources.kind_all
import com.cherryzp.animalshelter.resources.kind_cat
import com.cherryzp.animalshelter.resources.kind_dog
import com.cherryzp.animalshelter.resources.kind_other
import com.cherryzp.animalshelter.resources.sex_female
import com.cherryzp.animalshelter.resources.sex_male
import com.cherryzp.animalshelter.resources.state_all
import com.cherryzp.animalshelter.resources.state_notice
import com.cherryzp.animalshelter.resources.state_protect
import com.cherryzp.animalshelter.resources.title_filter
import com.cherryzp.animalshelter.ui.common.BottomBar
import com.cherryzp.animalshelter.ui.common.BottomBarButtonHeight
import com.cherryzp.animalshelter.ui.common.QuickDate
import com.cherryzp.animalshelter.ui.common.TestTags
import com.cherryzp.animalshelter.ui.theme.AnimalShelterTheme
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** 폼 본문 최대 폭(넓은 창에서 가운데). */
private val FormMaxWidth = 640.dp

/**
 * 필터 화면(ViewModel을 받아 상태를 모은다). "검색하기"는 검색을 시작하고 바로 닫는다(브리핑 A17).
 * 전체 화면 push — 두 칸에서도 칸 메타데이터 없이(P11).
 */
@Composable
fun FilterScreen(viewModel: FilterViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FilterScreen(
        state = state,
        onBack = onBack,
        onKindSelected = viewModel::onKindSelected,
        onBreedQueryChanged = viewModel::onBreedQueryChanged,
        onBreedSelected = viewModel::onBreedSelected,
        onQuickDate = viewModel::onQuickDate,
        onDateFromPicked = viewModel::onDateFromPicked,
        onDateToPicked = viewModel::onDateToPicked,
        onSidoSelected = viewModel::onSidoSelected,
        onSigunguSelected = viewModel::onSigunguSelected,
        onShelterSelected = viewModel::onShelterSelected,
        onStateSelected = viewModel::onStateSelected,
        onSexSelected = viewModel::onSexSelected,
        onNeuteredSelected = viewModel::onNeuteredSelected,
        onRetryCodes = viewModel::onRetryCodes,
        onReset = viewModel::onReset,
        onApply = {
            viewModel.onApply()
            onBack()
        },
        modifier = modifier
    )
}

/** 필터 화면(상태만 — `@Preview`·UI 테스트). 03 2.2 순서. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    state: FilterUiState,
    onBack: () -> Unit,
    onKindSelected: (AnimalKind?) -> Unit,
    onBreedQueryChanged: (String) -> Unit,
    onBreedSelected: (Kind?) -> Unit,
    onQuickDate: (QuickDate?) -> Unit,
    onDateFromPicked: (LocalDate) -> Unit,
    onDateToPicked: (LocalDate) -> Unit,
    onSidoSelected: (Sido?) -> Unit,
    onSigunguSelected: (Sigungu?) -> Unit,
    onShelterSelected: (ShelterCode?) -> Unit,
    onStateSelected: (NoticeState) -> Unit,
    onSexSelected: (Sex?) -> Unit,
    onNeuteredSelected: (Neutered?) -> Unit,
    onRetryCodes: (CodeTable) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filter = state.filter
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.title_filter),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                }
            )
        },
        bottomBar = {
            FilterBottomBar(canApply = state.canApply, onReset = onReset, onApply = onApply)
        }
    ) { innerPadding ->
        // 품종 검색 칸이 키보드에 가리지 않게: 바깥 inset을 소비한 뒤 imePadding → verticalScroll(C4, edge-to-edge 스킬)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.widthIn(max = FormMaxWidth).fillMaxWidth().padding(16.dp)
            ) {
                Section(Res.string.filter_kind) {
                    ChoiceRow(
                        tag = "kind",
                        options = listOf(
                            null to Res.string.kind_all,
                            AnimalKind.DOG to Res.string.kind_dog,
                            AnimalKind.CAT to Res.string.kind_cat,
                            AnimalKind.OTHER to Res.string.kind_other
                        ),
                        selected = filter.kind,
                        onSelected = onKindSelected
                    )
                }
                Section(Res.string.filter_breed) {
                    val kind = filter.kind
                    CodeDropdown(
                        label = stringResource(Res.string.filter_breed),
                        state = state.kinds,
                        selectedName = filter.kindName,
                        itemName = { it.name },
                        onSelected = onBreedSelected,
                        onRetry = { kind?.let { onRetryCodes(CodeTable.Kinds(it)) } },
                        disabledHint = stringResource(Res.string.filter_breed_hint_select_kind),
                        query = state.breedQuery,
                        onQueryChange = onBreedQueryChanged,
                        placeholder = stringResource(Res.string.filter_breed_search)
                    )
                }
                Section(Res.string.filter_date) {
                    DateRangeFields(
                        from = filter.dateFrom,
                        to = filter.dateTo,
                        today = state.today,
                        quickDate = state.quickDate,
                        error = state.dateError,
                        onQuickDate = onQuickDate,
                        onFromPicked = onDateFromPicked,
                        onToPicked = onDateToPicked
                    )
                }
                Section(Res.string.filter_region) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CodeDropdown(
                            label = stringResource(Res.string.filter_region_sido),
                            state = state.sido,
                            selectedName = filter.sidoName,
                            itemName = { it.name },
                            onSelected = onSidoSelected,
                            onRetry = { onRetryCodes(CodeTable.Sido) }
                        )
                        CodeDropdown(
                            label = stringResource(Res.string.filter_region_sigungu),
                            state = state.sigungu,
                            selectedName = filter.sigunguName,
                            itemName = { it.name },
                            onSelected = onSigunguSelected,
                            onRetry = {
                                filter.sidoCode?.let { onRetryCodes(CodeTable.Sigungu(it)) }
                            }
                        )
                        CodeDropdown(
                            label = stringResource(Res.string.filter_region_shelter),
                            state = state.shelters,
                            selectedName = filter.shelterName,
                            itemName = { it.name },
                            onSelected = onShelterSelected,
                            onRetry = {
                                val sido = filter.sidoCode
                                val sigungu = filter.sigunguCode
                                if (sido != null &&
                                    sigungu != null
                                ) {
                                    onRetryCodes(CodeTable.Shelters(sido, sigungu))
                                }
                            }
                        )
                    }
                }
                Section(Res.string.filter_state) {
                    ChoiceRow(
                        tag = "state",
                        options = listOf(
                            NoticeState.ALL to Res.string.state_all,
                            NoticeState.NOTICE to Res.string.state_notice,
                            NoticeState.PROTECT to Res.string.state_protect
                        ),
                        selected = filter.state,
                        onSelected = onStateSelected
                    )
                    Hint(stringResource(Res.string.filter_state_hint))
                    if (state.showClosingSoonHint) {
                        Hint(
                            stringResource(Res.string.filter_state_closing_soon_hint)
                        )
                    }
                }
                Section(Res.string.filter_sex) {
                    ChoiceRow(
                        tag = "sex",
                        options = listOf(
                            null to Res.string.choice_all,
                            Sex.MALE to Res.string.sex_male,
                            Sex.FEMALE to Res.string.sex_female,
                            Sex.UNKNOWN to Res.string.choice_unknown
                        ),
                        selected = filter.sex,
                        onSelected = onSexSelected
                    )
                }
                Section(Res.string.filter_neuter) {
                    ChoiceRow(
                        tag = "neuter",
                        options = listOf(
                            null to Res.string.choice_all,
                            Neutered.YES to Res.string.choice_yes,
                            Neutered.NO to Res.string.choice_no,
                            Neutered.UNKNOWN to Res.string.choice_unknown
                        ),
                        selected = filter.neutered,
                        onSelected = onNeuteredSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(title: StringResource, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(title),
            // 섹션 제목은 상세·보호소·설정과 같은 역할(3d 점검 A5)
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )
        content()
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** 하나만 고르는 세그먼트 버튼 줄(M3가 라디오 역할·`selectableGroup`을 준다). */
@Composable
private fun <T> ChoiceRow(
    tag: String,
    options: List<Pair<T, StringResource>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelected(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                modifier = Modifier.testTag(TestTags.choice(tag, value)),
                label = {
                    // 2줄까지(영어 "Unknown"·큰 글자에서 잘리지 않게, C2⑦) — 줄 높이는 같게 늘어난다
                    Text(stringResource(label), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            )
        }
    }
}

/**
 * 하단 고정: 초기화 + 검색하기(날짜 오류가 있으면 비활성). 버튼 높이 48dp(06 6절, 3d 점검 A4).
 * inset은 공용 [BottomBar](R6) — 가로 화면의 컷아웃을 피하고, 키보드가 열리면 바는 키보드 뒤에 있다.
 */
@Composable
private fun FilterBottomBar(canApply: Boolean, onReset: () -> Unit, onApply: () -> Unit) {
    BottomBar {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.weight(1f).heightIn(min = BottomBarButtonHeight)
        ) {
            Text(stringResource(Res.string.filter_reset))
        }
        Button(
            onClick = onApply,
            enabled = canApply,
            modifier = Modifier.weight(1f).heightIn(min = BottomBarButtonHeight)
                .testTag(TestTags.FILTER_APPLY)
        ) {
            Text(stringResource(Res.string.filter_apply))
        }
    }
}

@Preview
@Composable
private fun FilterScreenPreview() {
    AnimalShelterTheme {
        FilterScreen(
            state = FilterUiState(
                filter = SearchFilter(
                    kind = AnimalKind.DOG,
                    sidoCode = "6110000",
                    sidoName = "서울특별시"
                ),
                sido = CacheState(listOf(Sido("6110000", "서울특별시")), fresh = true, error = null),
                sigungu = CacheState(null, fresh = false, error = null),
                kinds = CacheState(listOf(Kind("000054", "골든 리트리버")), fresh = false, error = null),
                today = LocalDate(2026, 11, 4),
                showClosingSoonHint = true
            ),
            onBack = {},
            onKindSelected = {},
            onBreedQueryChanged = {},
            onBreedSelected = {},
            onQuickDate = {},
            onDateFromPicked = {},
            onDateToPicked = {},
            onSidoSelected = {},
            onSigunguSelected = {},
            onShelterSelected = {},
            onStateSelected = {},
            onSexSelected = {},
            onNeuteredSelected = {},
            onRetryCodes = {},
            onReset = {},
            onApply = {}
        )
    }
}
