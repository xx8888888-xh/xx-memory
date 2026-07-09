package com.xxmemory.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xxmemory.app.data.entity.Card
import com.xxmemory.app.ui.theme.Background
import com.xxmemory.app.ui.theme.Primary
import com.xxmemory.app.ui.theme.PrimaryLight
import com.xxmemory.app.ui.theme.Success
import com.xxmemory.app.ui.theme.Surface
import com.xxmemory.app.ui.theme.TextPrimary
import com.xxmemory.app.ui.theme.TextSecondary
import com.xxmemory.app.ui.theme.TextTertiary
import com.xxmemory.app.ui.theme.Warning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToReview: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        // Greeting header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            GreetingHeader()
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
                totalCards = uiState.totalCards,
                todayReviewed = uiState.todayReviewed,
                dueCount = uiState.dueCount
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Week calendar strip
        item {
            Text(
                text = "本周学习",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            WeekCalendarStrip(weekStats = uiState.weekStats)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Subject filter chips
        if (uiState.subjects.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedSubject == null,
                            onClick = { viewModel.selectSubject(null) },
                            label = { Text("全部") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = Surface
                            )
                        )
                    }
                    items(uiState.subjects) { subject ->
                        FilterChip(
                            selected = uiState.selectedSubject == subject,
                            onClick = { viewModel.selectSubject(subject) },
                            label = { Text(subject) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = Surface
                            )
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
                color = TextPrimary,
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
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(filteredCards) { card ->
                DueCardItem(card = card, onCardClick = onNavigateToReview)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
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
            text = "$greeting 👋",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = today,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
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
                .background(PrimaryLight.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = totalCards.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "总卡片",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Success.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = todayReviewed.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Success,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "今日复习",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Warning.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = dueCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Warning,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "待复习",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
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
            containerColor = Primary
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
private fun ProgressSection(totalCards: Int, todayReviewed: Int, dueCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
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
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(64.dp),
                    color = Primary,
                    trackColor = PrimaryLight.copy(alpha = 0.3f),
                    strokeWidth = 6.dp
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "今日进度",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "已复习 $todayReviewed / $totalCards 张",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun WeekCalendarStrip(weekStats: List<DayStat>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekStats.forEach { stat ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (stat.isToday) Primary.copy(alpha = 0.1f)
                        else androidx.compose.ui.graphics.Color.Transparent
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stat.dayOfWeek,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (stat.isToday) Primary else TextSecondary
                )
                Text(
                    text = stat.dayNum.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (stat.isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (stat.isToday) Primary else TextPrimary
                )
                Text(
                    text = "${stat.count}张",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun DueCardItem(card: Card, onCardClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
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
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                if (card.subject.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = card.subject,
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when(card.cardType) {
                        Card.TYPE_QA -> "问答"
                        Card.TYPE_FILL_BLANK -> "填空"
                        Card.TYPE_CODE -> "代码"
                        Card.TYPE_IMAGE -> "图片"
                        Card.TYPE_AUDIO -> "音频"
                        else -> card.cardType
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}