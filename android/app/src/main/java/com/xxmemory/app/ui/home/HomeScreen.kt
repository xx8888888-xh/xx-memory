package com.xxmemory.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xxmemory.app.XxMemoryApplication
import com.xxmemory.app.data.entity.Card
import com.xxmemory.app.data.repository.CardRepository
import com.xxmemory.app.domain.SchedulerUtils
import com.xxmemory.app.ui.statistics.StatisticsViewModel
import com.xxmemory.app.ui.statistics.StatisticsUiState
import com.xxmemory.app.ui.statistics.WeeklyDayStat
import com.xxmemory.app.ui.statistics.SubjectMastery
import com.xxmemory.app.ui.theme.EinkFilterChip
import com.xxmemory.app.ui.theme.rememberEinkMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToReview: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    statsViewModel: StatisticsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val statsState by statsViewModel.uiState.collectAsState()
    val isEinkMode = rememberEinkMode()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showCalendarDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadData()
                statsViewModel.loadStatistics()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isEinkMode) {
                    Text(
                        text = "加载中...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                } else {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp)
            ) {
                // Top bar with greeting and settings icon
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GreetingHeader()
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "设置",
                                tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    CardCountBadge(totalCards = uiState.totalCards, dueCount = uiState.dueCount, todayReviewed = uiState.todayReviewed)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // CTA button
                item {
                    StartReviewButton(
                        dueCount = uiState.dueCount,
                        onClick = onNavigateToReview
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Progress ring area
                item {
                    ProgressSection(
                        todayReviewed = uiState.todayReviewed,
                        dueCount = uiState.dueCount,
                        isEinkMode = isEinkMode
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Next 7 days review schedule
                item {
                    NextWeekReviewSection(
                        nextSevenDays = uiState.nextSevenDays,
                        isEinkMode = isEinkMode,
                        onClick = {
                            showCalendarDialog = true
                            viewModel.loadCalendarMonth()
                            viewModel.selectCalendarDay(CardRepository.getStartOfDay(System.currentTimeMillis()))
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Subject filter chips
                if (uiState.subjects.isNotEmpty()) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                EinkFilterChip(
                                    selected = uiState.selectedSubject == null,
                                    onClick = { viewModel.selectSubject(null) },
                                    label = "全部"
                                )
                            }
                            items(uiState.subjects) { subject ->
                                EinkFilterChip(
                                    selected = uiState.selectedSubject == subject,
                                    onClick = { viewModel.selectSubject(subject) },
                                    label = subject
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Due cards list
                item {
                    Text(
                        text = "待复习卡片",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val filteredCards = viewModel.getFilteredCards()
                if (filteredCards.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无待复习卡片",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(filteredCards) { card ->
                        DueCardItem(card = card, isEinkMode = isEinkMode, onCardClick = onNavigateToReview)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Statistics section embedded in home page
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "学习统计",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Streak ring
                item {
                    StreakRing(streakDays = statsState.streakDays, isEinkMode = isEinkMode)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Stats cards row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "今日复习",
                            value = "${statsState.todayReviewed}",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "总复习数",
                            value = "${statsState.totalReviewed}",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "总卡片数",
                            value = "${statsState.totalCards}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Weekly trend chart
                item {
                    WeeklyTrendChart(weeklyStats = statsState.weeklyStats, isEinkMode = isEinkMode)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Subject mastery
                if (statsState.subjectMastery.isNotEmpty()) {
                    item {
                        Text(
                            text = "科目掌握度",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    items(statsState.subjectMastery) { subject ->
                        SubjectMasteryBar(subject = subject, isEinkMode = isEinkMode)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (showCalendarDialog) {
            CalendarDialogOverlay(
                monthDays = uiState.calendarMonthDays,
                selectedDay = uiState.selectedCalendarDay,
                selectedDayCards = uiState.selectedDayCards,
                onDayClick = { viewModel.selectCalendarDay(it) },
                onDismiss = {
                    showCalendarDialog = false
                    viewModel.selectCalendarDay(null)
                },
                isEinkMode = isEinkMode
            )
        }
    }
}

@Composable
private fun GreetingHeader() {
    val dateFormat = SimpleDateFormat("MM月dd日 EEEE", Locale.CHINESE)
    val today = dateFormat.format(Date())

    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 6 -> "夜深了"
        hour < 12 -> "早上好"
        hour < 14 -> "中午好"
        hour < 18 -> "下午好"
        else -> "晚上好"
    }

    Column {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = today,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CardCountBadge(totalCards: Int, dueCount: Int, todayReviewed: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = totalCards.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "总卡片",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = todayReviewed.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "今日复习",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = dueCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "待复习",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StartReviewButton(dueCount: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        enabled = dueCount > 0
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (dueCount > 0) "开始今日复习 ($dueCount 张卡片)" else "今日复习已完成",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun ProgressSection(todayReviewed: Int, dueCount: Int, isEinkMode: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEinkMode) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                val progress = if (todayReviewed + dueCount > 0) {
                    todayReviewed.toFloat() / (todayReviewed + dueCount)
                } else 0f
                if (isEinkMode) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .border(6.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        strokeWidth = 6.dp
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "今日进度",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "已复习 $todayReviewed / ${todayReviewed + dueCount} 张",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NextWeekReviewSection(
    nextSevenDays: List<DayDueStat>,
    isEinkMode: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEinkMode) 0.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "未来一周复习安排",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                nextSevenDays.forEach { stat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (stat.isToday) {
                                    if (isEinkMode) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                } else Color.Transparent
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stat.dayOfWeek,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (stat.isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stat.dayNum.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (stat.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${stat.dueCount}张",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDialogOverlay(
    monthDays: List<CalendarDayStat>,
    selectedDay: Long?,
    selectedDayCards: List<Card>,
    onDayClick: (Long) -> Unit,
    onDismiss: () -> Unit,
    isEinkMode: Boolean
) {
    val settings = XxMemoryApplication.instance.settingsManager
    val focusedSlots = SchedulerUtils.parseFocusedSlots(settings.focusedTimeSlots)
    val timeLabel = if (settings.studyMode == "focused" && focusedSlots.isNotEmpty()) {
        "复习时间：${focusedSlots.joinToString(", ")}"
    } else {
        "复习时间：按算法自由安排"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "复习日历",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "关闭",
                            tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                val dayHeaders = listOf("日", "一", "二", "三", "四", "五", "六")
                Row(modifier = Modifier.fillMaxWidth()) {
                    dayHeaders.forEach { header ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = header,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            val rows = monthDays.chunked(7)
            items(rows) { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { day ->
                        CalendarDayCell(
                            day = day,
                            selected = day.timestamp == selectedDay,
                            onClick = { onDayClick(day.timestamp) },
                            isEinkMode = isEinkMode,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                val dateText = selectedDay?.let {
                    SimpleDateFormat("MM月dd日", Locale.CHINESE).format(Date(it))
                } ?: "请选择日期"
                Text(
                    text = "$dateText 到期卡片（${selectedDayCards.size}张）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (selectedDayCards.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "当天没有到期卡片",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(selectedDayCards) { card ->
                    CalendarCardItem(card = card, isEinkMode = isEinkMode)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDayStat,
    selected: Boolean,
    onClick: () -> Unit,
    isEinkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        selected -> if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
        day.isToday -> if (isEinkMode) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        !day.isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isEinkMode) 0.3f else 0.2f)
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        selected -> Color.White
        !day.isCurrentMonth -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = when {
        day.isToday && !selected -> if (isEinkMode) Color.Black else MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .background(bgColor, RoundedCornerShape(4.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.dayNum.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = if (day.isToday || selected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = "${day.dueCount}张",
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color.White else if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun CalendarCardItem(card: Card, isEinkMode: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEinkMode) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.question,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                if (card.subject.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = card.subject,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = cardTypeLabel(card.cardType),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun cardTypeLabel(cardType: String): String = when (cardType) {
    Card.TYPE_QA -> "问答"
    Card.TYPE_FILL_BLANK -> "填空"
    Card.TYPE_CODE -> "代码"
    Card.TYPE_IMAGE -> "图片"
    Card.TYPE_AUDIO -> "音频"
    Card.TYPE_DICTATION -> "默写"
    else -> cardType
}

@Composable
private fun DueCardItem(card: Card, isEinkMode: Boolean, onCardClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCardClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEinkMode) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.question,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                if (card.subject.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = card.subject,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                    )
                }
                if (card.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "标签: ${card.tags}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = cardTypeLabel(card.cardType),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- Statistics components (embedded in home) ---

@Composable
private fun StreakRing(streakDays: Int, isEinkMode: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEinkMode) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isEinkMode) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(4.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$streakDays",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    val progress = ((streakDays % 7).coerceAtLeast(0) / 7f).let { if (it == 0f && streakDays > 0) 1f else it }
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.size(80.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        strokeWidth = 8.dp
                    )
                    Text(
                        text = "$streakDays",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "连续学习",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$streakDays 天",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyTrendChart(weeklyStats: List<WeeklyDayStat>, isEinkMode: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEinkMode) 0.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "本周学习趋势",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            val maxCount = weeklyStats.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyStats.forEach { stat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${stat.count}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(((stat.count.toFloat() / maxCount) * 80).dp.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stat.dayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectMasteryBar(subject: SubjectMastery, isEinkMode: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEinkMode) 0.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = subject.subject,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${subject.masteredCards}/${subject.totalCards}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val progress = subject.masteredCards.toFloat() / subject.totalCards.coerceAtLeast(1)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .background(if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
