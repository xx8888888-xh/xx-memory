package com.xxmemory.app.ui.review

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SpeakerNotes
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xxmemory.app.data.entity.Card as CardEntity

@Composable
internal fun BbdcLearningCard(
    card: CardEntity,
    step: ReviewStep,
    isEinkMode: Boolean,
    onExampleClear: () -> Unit,
    onExampleWrong: () -> Unit,
    onSelfAssessment: (SelfAssessment) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEinkMode) 0.dp else 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TypeLabel(cardType = card.cardType, isEinkMode = isEinkMode)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = card.question,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (card.phonetic.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = card.phonetic,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                when (step) {
                    ReviewStep.EXAMPLE_REVIEW -> ExampleReviewContent(
                        card = card,
                        isEinkMode = isEinkMode,
                        onClear = onExampleClear,
                        onWrong = onExampleWrong
                    )

                    ReviewStep.INDEPENDENT_RECALL -> IndependentRecallContent(
                        card = card,
                        isEinkMode = isEinkMode,
                        onSelfAssessment = onSelfAssessment
                    )

                    ReviewStep.SELF_ASSESSMENT -> SelfAssessmentContent(
                        card = card,
                        isEinkMode = isEinkMode,
                        onSelfAssessment = onSelfAssessment
                    )

                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun ExampleReviewContent(
    card: CardEntity,
    isEinkMode: Boolean,
    onClear: () -> Unit,
    onWrong: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isEinkMode) 0.5f else 0.6f))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.SpeakerNotes,
                        contentDescription = null,
                        tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "真实语境例句",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = card.example.ifBlank { "无例句" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onWrong,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.error
                )
            ) {
                Text("记错了", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Button(
                onClick = onClear,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                )
            ) {
                Text("清晰", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun IndependentRecallContent(
    card: CardEntity,
    isEinkMode: Boolean,
    onSelfAssessment: (SelfAssessment) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "请独立回忆释义",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        SelfAssessmentButtons(
            isEinkMode = isEinkMode,
            onSelfAssessment = onSelfAssessment
        )
    }
}

@Composable
private fun SelfAssessmentContent(
    card: CardEntity,
    isEinkMode: Boolean,
    onSelfAssessment: (SelfAssessment) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SelfAssessmentButtons(
            isEinkMode = isEinkMode,
            onSelfAssessment = onSelfAssessment
        )
    }
}

@Composable
private fun SelfAssessmentButtons(
    isEinkMode: Boolean,
    onSelfAssessment: (SelfAssessment) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SelfAssessmentButton(
            text = "记对了",
            isEinkMode = isEinkMode,
            isPrimary = true,
            onClick = { onSelfAssessment(SelfAssessment.CORRECT) }
        )
        SelfAssessmentButton(
            text = "有点模糊",
            isEinkMode = isEinkMode,
            isPrimary = false,
            onClick = { onSelfAssessment(SelfAssessment.FUZZY) }
        )
        SelfAssessmentButton(
            text = "记不清",
            isEinkMode = isEinkMode,
            isPrimary = false,
            onClick = { onSelfAssessment(SelfAssessment.FORGOT) }
        )
        SelfAssessmentButton(
            text = "记错了",
            isEinkMode = isEinkMode,
            isPrimary = false,
            onClick = { onSelfAssessment(SelfAssessment.WRONG) }
        )
    }
}

@Composable
private fun SelfAssessmentButton(
    text: String,
    isEinkMode: Boolean,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    if (isEinkMode) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.DarkGray
            )
        ) {
            Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    } else {
        if (isPrimary) {
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        } else {
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}
