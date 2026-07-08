package com.xxmemory.app.ui.review

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SpeakerNotes
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xxmemory.app.ui.theme.Background
import com.xxmemory.app.ui.theme.Error
import com.xxmemory.app.ui.theme.Primary
import com.xxmemory.app.ui.theme.PrimaryLight
import com.xxmemory.app.ui.theme.Success
import com.xxmemory.app.ui.theme.Surface
import com.xxmemory.app.ui.theme.TextPrimary
import com.xxmemory.app.ui.theme.TextSecondary
import com.xxmemory.app.ui.theme.Warning

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    if (uiState.isComplete && uiState.cards.isEmpty()) {
        EmptyReviewState(onReload = { viewModel.loadDueCards() })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp)
    ) {
        // Progress bar
        LinearProgressIndicator(
            progress = uiState.progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Primary,
            trackColor = PrimaryLight.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${uiState.currentNumber} / ${uiState.totalCount}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isComplete) {
            ReviewCompleteState(
                completedCount = uiState.completedCount,
                onReload = { viewModel.loadDueCards() }
            )
        } else {
            val card = uiState.currentCard ?: return

            // Subject pill
            if (card.subject.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Primary.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = card.subject,
                        color = Primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Flippable card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { viewModel.flipCard() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!uiState.isFlipped) {
                            Text(
                                text = "点击翻转",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = card.question,
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = card.question,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(PrimaryLight.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = card.answer,
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            if (card.detail.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = card.detail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speaker button
            if (uiState.isFlipped) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryLight.copy(alpha = 0.2f))
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SpeakerNotes,
                            contentDescription = "朗读",
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Assessment buttons
            if (uiState.isFlipped) {
                Text(
                    text = "你对这张卡片的掌握程度如何？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssessmentButton(
                        text = "忘记",
                        color = Error,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.assessCard(0) }
                    )
                    AssessmentButton(
                        text = "困难",
                        color = Warning,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.assessCard(1) }
                    )
                    AssessmentButton(
                        text = "良好",
                        color = Success,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.assessCard(2) }
                    )
                    AssessmentButton(
                        text = "简单",
                        color = Primary,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.assessCard(3) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AssessmentButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = color
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyReviewState(onReload: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Success,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "没有待复习的卡片",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "去导入页面添加新的卡片吧",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onReload,
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("刷新")
        }
    }
}

@Composable
private fun ReviewCompleteState(completedCount: Int, onReload: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = Warning,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "太棒了！",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "已完成 $completedCount 张卡片的复习",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onReload,
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("查看是否有新的卡片")
        }
    }
}