package com.xingheyuzhuan.shiguangschedule.ui.settings.course

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.data.model.DualColor
import com.xingheyuzhuan.shiguangschedule.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_apply_to_all_schemes
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.action_double_week
import shiguangschedule.shared.generated.resources.action_select_all
import shiguangschedule.shared.generated.resources.action_single_week
import shiguangschedule.shared.generated.resources.check_24px
import shiguangschedule.shared.generated.resources.label_course_weeks
import shiguangschedule.shared.generated.resources.label_none
import shiguangschedule.shared.generated.resources.text_weeks_selected
import shiguangschedule.shared.generated.resources.title_select_color
import shiguangschedule.shared.generated.resources.title_select_weeks


@Composable
fun WeekSection(
    selectedWeeks: Set<Int>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = stringResource(Res.string.label_course_weeks)
    val noneSelected = stringResource(Res.string.label_none)

    Surface(
        onClick = onClick,
        modifier = modifier,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            if (selectedWeeks.isEmpty()) {
                Text(text = noneSelected, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            } else {
                Text(
                    text = stringResource(Res.string.text_weeks_selected, selectedWeeks.sorted().joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun TimeSection(
    dayName: String,
    timeDesc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = dayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = timeDesc, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ColorIndicatorSection(
    colorIndex: Int,
    colorMaps: List<DualColor>,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val displayColor = colorMaps.getOrNull(colorIndex)?.let {
        if (isDark) it.dark else it.light
    } ?: MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(16.dp)
            .background(displayColor)
            .clickable(onClick = onClick)
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekSelectorBottomSheet(
    totalWeeks: Int,
    selectedWeeks: Set<Int>,
    onDismissRequest: () -> Unit,
    onConfirm: (Set<Int>) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    var tempSelectedWeeks by remember { mutableStateOf(selectedWeeks) }
    val titleSelectWeeks = stringResource(Res.string.title_select_weeks)
    val actionSelectAll = stringResource(Res.string.action_select_all)
    val actionSingleWeek = stringResource(Res.string.action_single_week)
    val actionDoubleWeek = stringResource(Res.string.action_double_week)
    val actionCancel = stringResource(Res.string.action_cancel)
    val actionConfirm = stringResource(Res.string.action_confirm)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = modalBottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titleSelectWeeks,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 48.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(totalWeeks) { index ->
                    val weekNumber = index + 1
                    val isSelected = tempSelectedWeeks.contains(weekNumber)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                tempSelectedWeeks = if (isSelected) {
                                    tempSelectedWeeks - weekNumber
                                } else {
                                    tempSelectedWeeks + weekNumber
                                }
                            }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = weekNumber.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tempSelectedWeeks.size == totalWeeks && totalWeeks > 0,
                    onClick = {
                        tempSelectedWeeks = if (tempSelectedWeeks.size == totalWeeks) emptySet()
                        else (1..totalWeeks).toSet()
                    },
                    label = { Text(actionSelectAll) }
                )
                FilterChip(
                    selected = tempSelectedWeeks.isNotEmpty() && tempSelectedWeeks.all { it % 2 != 0 },
                    onClick = {
                        tempSelectedWeeks = (1..totalWeeks).filter { it % 2 != 0 }.toSet()
                    },
                    label = { Text(actionSingleWeek) }
                )
                FilterChip(
                    selected = tempSelectedWeeks.isNotEmpty() && tempSelectedWeeks.all { it % 2 == 0 },
                    onClick = {
                        tempSelectedWeeks = (1..totalWeeks).filter { it % 2 == 0 }.toSet()
                    },
                    label = { Text(actionDoubleWeek) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onDismissRequest, modifier = Modifier.weight(1f)) {
                    Text(actionCancel)
                }
                Button(
                    onClick = {
                        onConfirm(tempSelectedWeeks)
                        coroutineScope.launch { modalBottomSheetState.hide() }.invokeOnCompletion {
                            if (!modalBottomSheetState.isVisible) onDismissRequest()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(actionConfirm)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerBottomSheet(
    colorMaps: List<DualColor>,
    selectedIndex: Int,
    showApplyToAllOption: Boolean = false,
    onDismissRequest: () -> Unit,
    onConfirm: (colorIndex: Int, applyToAll: Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    var tempSelectedIndex by remember { mutableIntStateOf(selectedIndex) }
    var applyToAll by remember { mutableStateOf(false) }

    val isDark = LocalIsDarkTheme.current
    val actionCancel = stringResource(Res.string.action_cancel)
    val actionConfirm = stringResource(Res.string.action_confirm)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = modalBottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.title_select_color),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(colorMaps) { index, dualColor ->
                    val color = if (isDark) dualColor.dark else dualColor.light
                    val isSelected = tempSelectedIndex == index

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .clickable { tempSelectedIndex = index }
                            .then(
                                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.check_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (showApplyToAllOption) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { applyToAll = !applyToAll }
                        .padding(vertical = 8.dp)
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = applyToAll,
                        onCheckedChange = { applyToAll = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.action_apply_to_all_schemes),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismissRequest, modifier = Modifier.weight(1f)) {
                    Text(actionCancel)
                }
                Button(
                    onClick = {
                        onConfirm(tempSelectedIndex, applyToAll)
                        coroutineScope.launch { modalBottomSheetState.hide() }.invokeOnCompletion {
                            if (!modalBottomSheetState.isVisible) onDismissRequest()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(actionConfirm)
                }
            }
        }
    }
}