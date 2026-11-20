package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.model.CategoryLastSchool
import com.xingheyuzhuan.shiguangschedule.data.model.SchoolHistoryModel
import com.xingheyuzhuan.shiguangschedule.ui.components.AlphabetIndexBar
import com.xingheyuzhuan.shiguangschedule.ui.components.AlphabetIndexBarWidth
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.LocalCardShape
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.SettingsDefaults
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.SettingsGroup
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.SettingsGroupCard
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.SettingsGroupLazyColumn
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsRow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import school_index.AdapterCategory
import school_index.School
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.a11y_clear_search
import shiguangschedule.shared.generated.resources.a11y_delete
import shiguangschedule.shared.generated.resources.a11y_school_icon
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.category_bachelor_associate
import shiguangschedule.shared.generated.resources.category_general_tool
import shiguangschedule.shared.generated.resources.category_other
import shiguangschedule.shared.generated.resources.category_postgraduate
import shiguangschedule.shared.generated.resources.close_24px
import shiguangschedule.shared.generated.resources.label_recent_visit
import shiguangschedule.shared.generated.resources.school_24px
import shiguangschedule.shared.generated.resources.search_hint_school
import shiguangschedule.shared.generated.resources.text_no_adapter_for_category
import shiguangschedule.shared.generated.resources.text_no_school_found
import shiguangschedule.shared.generated.resources.title_update_repo_screen
import shiguangschedule.shared.generated.resources.update_24px

/** 列表左右统一内边距。 */
private val ListHorizontalPadding = 16.dp

/** 学校选择主界面：搜索栏、分类页签与学校索引列表。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolSelectionListScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: SchoolSelectionViewModel = koinViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filteredSchools by viewModel.filteredSchools.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val schoolHistory by viewModel.schoolHistory.collectAsState()

    val textFieldState = rememberTextFieldState(initialText = searchQuery)
    val focusManager = LocalFocusManager.current

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text }.collect { text ->
            viewModel.updateSearchQuery(text.toString())
        }
    }

    val placeholderText = stringResource(Res.string.search_hint_school)

    Scaffold(
        modifier = Modifier.semantics { isTraversalGroup = true },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.arrow_back_24px),
                        contentDescription = stringResource(Res.string.a11y_back)
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .semantics { traversalIndex = 0f },
                    shape = SearchBarDefaults.inputFieldShape,
                    color = SearchBarDefaults.colors().containerColor
                ) {
                    SearchBarDefaults.InputField(
                        query = textFieldState.text.toString(),
                        onQueryChange = { newText ->
                            textFieldState.clearText()
                            textFieldState.edit { append(newText) }
                        },
                        onSearch = { focusManager.clearFocus() },
                        expanded = false,
                        onExpandedChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(placeholderText)
                        },
                        trailingIcon = {
                            if (textFieldState.text.isNotEmpty()) {
                                IconButton(onClick = { textFieldState.clearText() }) {
                                    Icon(
                                        imageVector = vectorResource(Res.drawable.close_24px),
                                        contentDescription = stringResource(Res.string.a11y_clear_search)
                                    )
                                }
                            }
                        }
                    )
                }

                IconButton(
                    onClick = { onNavigate(Destination.UpdateRepo) },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.update_24px),
                        contentDescription = stringResource(Res.string.title_update_repo_screen)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CategoryTabs(
                selectedCategory = selectedCategory,
                onCategorySelected = { category ->
                    viewModel.updateSelectedCategory(category)
                },
                displayCategories = viewModel.displayCategories
            )

            SchoolContent(
                isLoading = isLoading,
                filteredSchools = filteredSchools,
                selectedCategory = selectedCategory,
                schoolHistory = schoolHistory,
                searchQuery = searchQuery,
                onClearHistory = { viewModel.clearHistory(it) },
                onSchoolSelected = { school, category ->
                    viewModel.saveLastSchool(school)
                    onNavigate(
                        Destination.AdapterSelection(
                            schoolId = school.id,
                            schoolName = school.name,
                            categoryNumber = category.value,
                            resourceFolder = school.resource_folder
                        )
                    )
                }
            )
        }
    }
}

/** 学校列表面板：加载中 / 空状态 / 列表。 */
@Composable
private fun SchoolContent(
    isLoading: Boolean,
    filteredSchools: List<School>,
    selectedCategory: AdapterCategory,
    schoolHistory: SchoolHistoryModel,
    searchQuery: String,
    onClearHistory: (AdapterCategory) -> Unit,
    onSchoolSelected: (School, AdapterCategory) -> Unit
) {
    val recentRecord = when (selectedCategory) {
        AdapterCategory.BACHELOR_AND_ASSOCIATE -> schoolHistory.bachelor
        AdapterCategory.POSTGRADUATE -> schoolHistory.postgraduate
        AdapterCategory.GENERAL_TOOL -> schoolHistory.general
        else -> null
    }

    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        filteredSchools.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(
                        if (searchQuery.isNotBlank()) {
                            Res.string.text_no_school_found
                        } else {
                            Res.string.text_no_adapter_for_category
                        }
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        else -> {
            SchoolIndexedList(
                schools = filteredSchools,
                recentRecord = recentRecord?.takeIf { !it.isEmpty },
                onClearHistory = { onClearHistory(selectedCategory) },
                onSchoolSelected = { onSchoolSelected(it, selectedCategory) }
            )
        }
    }
}

/** 按首字母分组的学校列表 + 右侧字母索引条。 */
@Composable
private fun SchoolIndexedList(
    schools: List<School>,
    recentRecord: CategoryLastSchool?,
    onClearHistory: () -> Unit,
    onSchoolSelected: (School) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val groups = remember(schools) {
        schools.groupBy { it.initial.uppercase() }
            .entries
            .sortedBy { it.key }
            .map { (initial, members) -> SettingsGroup(title = initial, items = members) }
    }

    val initials = remember(groups) { groups.mapNotNull { it.title } }

    val indexMap = remember(groups, recentRecord) {
        val map = mutableMapOf<String, Int>()
        var index = if (recentRecord != null) 1 else 0
        groups.forEach { group ->
            map[group.title ?: return@forEach] = index
            index += 1 + group.items.size
        }
        map
    }

    LaunchedEffect(schools) { lazyListState.scrollToItem(0) }

    val recentLeading: (@Composable () -> Unit)? = if (recentRecord != null) {
        {
            RecentSchoolCard(
                record = recentRecord,
                onClearHistory = onClearHistory,
                onSchoolSelected = onSchoolSelected
            )
        }
    } else {
        null
    }

    val navigationBarBottom = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsGroupLazyColumn(
            groups = groups,
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(
                start = ListHorizontalPadding,
                end = AlphabetIndexBarWidth,
                bottom = navigationBarBottom
            ),
            leadingContent = recentLeading
        ) { school, shape ->
            SchoolSettingsRow(
                school = school,
                shape = shape,
                onClick = onSchoolSelected
            )
        }

        AlphabetIndexBar(
            modifier = Modifier.align(Alignment.CenterEnd),
            initials = initials,
            onInitialSelected = { initial ->
                indexMap[initial]?.let { targetIndex ->
                    coroutineScope.launch { lazyListState.scrollToItem(targetIndex) }
                }
            }
        )
    }
}

/** 最近访问分组卡片。 */
@Composable
private fun RecentSchoolCard(
    record: CategoryLastSchool,
    onClearHistory: () -> Unit,
    onSchoolSelected: (School) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        SettingsGroupCard(title = stringResource(Res.string.label_recent_visit)) {
            item {
                SchoolSettingsRow(
                    school = record.toSchool(),
                    onClick = onSchoolSelected,
                    trailing = {
                        IconButton(onClick = onClearHistory) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.close_24px),
                                contentDescription = stringResource(Res.string.a11y_delete),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                )
            }
        }
    }
}

/** 分类切换页签。 */
@Composable
fun CategoryTabs(
    selectedCategory: AdapterCategory,
    onCategorySelected: (AdapterCategory) -> Unit,
    displayCategories: List<AdapterCategory>
) {
    @Composable
    fun getDisplayName(category: AdapterCategory): String {
        return when (category) {
            AdapterCategory.BACHELOR_AND_ASSOCIATE -> stringResource(Res.string.category_bachelor_associate)
            AdapterCategory.POSTGRADUATE -> stringResource(Res.string.category_postgraduate)
            AdapterCategory.GENERAL_TOOL -> stringResource(Res.string.category_general_tool)
            else -> stringResource(Res.string.category_other)
        }
    }

    val selectedIndex = displayCategories.indexOf(selectedCategory).coerceAtLeast(0)

    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = Modifier.fillMaxWidth(),
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedIndex),
                width = 24.dp,
            )
        }
    ) {
        displayCategories.forEachIndexed { index, category ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(
                        text = getDisplayName(category),
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

/** 单个学校条目，基于 [SettingsRow] 实现。 */
@Composable
fun SchoolSettingsRow(
    school: School,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = LocalCardShape.current,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (School) -> Unit
) {
    SettingsRow(
        modifier = modifier.fillMaxWidth(),
        title = school.name,
        leading = {
            Icon(
                imageVector = vectorResource(Res.drawable.school_24px),
                contentDescription = stringResource(Res.string.a11y_school_icon),
                modifier = Modifier.size(SettingsDefaults.IconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailing = trailing,
        shape = shape,
        onClick = { onClick(school) }
    )
}
