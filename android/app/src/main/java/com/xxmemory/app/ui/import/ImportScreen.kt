package com.xxmemory.app.ui.import

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xxmemory.app.data.entity.Card
import com.xxmemory.app.ui.theme.Background
import com.xxmemory.app.ui.theme.Error
import com.xxmemory.app.ui.theme.Info
import com.xxmemory.app.ui.theme.Outline
import com.xxmemory.app.ui.theme.Primary
import com.xxmemory.app.ui.theme.PrimaryLight
import com.xxmemory.app.ui.theme.Success
import com.xxmemory.app.ui.theme.Surface
import com.xxmemory.app.ui.theme.TextPrimary
import com.xxmemory.app.ui.theme.TextSecondary
import com.xxmemory.app.ui.theme.Warning

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: ImportViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showManualDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.importMessage) {
        uiState.importMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "导入卡片",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))

            // File picker section
            FileImportSection(viewModel = viewModel)
            Spacer(modifier = Modifier.height(16.dp))

            // URL input section
            UrlImportSection(
                urlInput = uiState.urlInput,
                onUrlChange = { viewModel.updateUrlInput(it) },
                onImport = { viewModel.importFromJson(it) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Manual add button
            OutlinedButton(
                onClick = { showManualDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("手动添加卡片")
            }
            Spacer(modifier = Modifier.height(16.dp))

            // AI Skill card section
            AiSkillSection()
            Spacer(modifier = Modifier.height(16.dp))

            // Supported format chips
            FormatChipsSection(
                selectedFormat = uiState.selectedFormat,
                onFormatSelected = { viewModel.selectFormat(it) }
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Recent imports
            Text(
                text = "最近导入",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Primary
                )
            }

            if (uiState.recentImports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无导入记录",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                uiState.recentImports.forEach { card ->
                    RecentImportItem(card = card, viewModel = viewModel)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showManualDialog) {
        ManualCardDialog(
            onDismiss = { showManualDialog = false },
            onConfirm = { q, a, s, d, t ->
                viewModel.importManualCard(q, a, s, d, t)
                showManualDialog = false
            }
        )
    }
}

@Composable
private fun FileImportSection(viewModel: ImportViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importFromFile(context, it)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { launcher.launch("*/*") },
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
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryLight.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FileUpload,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "从文件导入",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "支持 JSON, CSV, TXT, Markdown 格式",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun UrlImportSection(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    onImport: (String) -> Unit
) {
    var showUrlField by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showUrlField = !showUrlField },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Info.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = null,
                        tint = Info,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "从 URL / JSON 导入",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "粘贴 JSON 数据或链接",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            if (showUrlField) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("粘贴 JSON 数据...") },
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onImport(urlInput) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("导入")
                }
            }
        }
    }
}

@Composable
private fun AiSkillSection() {
    var showAiInfo by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showAiInfo = true },
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
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Success.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI 智能导入",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "拍照识别、网页提取、语音转文字",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }

    if (showAiInfo) {
        AlertDialog(
            onDismissRequest = { showAiInfo = false },
            title = { Text("AI 智能导入", fontWeight = FontWeight.Bold) },
            text = {
                Text("AI 智能导入功能需要将 AI Skill 文件上传至 AI 助手（如 TRAE）使用。\n\n支持的导入方式：\n• 拍照识别文字内容\n• 网页内容提取\n• 语音转文字\n\nAI 将自动解析内容并生成记忆卡片。")
            },
            confirmButton = {
                TextButton(onClick = { showAiInfo = false }) {
                    Text("知道了")
                }
            }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FormatChipsSection(
    selectedFormat: String,
    onFormatSelected: (String) -> Unit
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFormat == "JSON",
                onClick = { onFormatSelected(if (selectedFormat == "JSON") "" else "JSON") },
                label = { Text("JSON") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Outline.copy(alpha = 0.3f)
                )
            )
            FilterChip(
                selected = selectedFormat == "CSV",
                onClick = { onFormatSelected(if (selectedFormat == "CSV") "" else "CSV") },
                label = { Text("CSV") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Outline.copy(alpha = 0.3f)
                )
            )
            FilterChip(
                selected = selectedFormat == "TXT",
                onClick = { onFormatSelected(if (selectedFormat == "TXT") "" else "TXT") },
                label = { Text("TXT") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Outline.copy(alpha = 0.3f)
                )
            )
            FilterChip(
                selected = selectedFormat == "Markdown",
                onClick = { onFormatSelected(if (selectedFormat == "Markdown") "" else "Markdown") },
                label = { Text("Markdown") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Outline.copy(alpha = 0.3f)
                )
            )
        }
        if (selectedFormat.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "已选择格式: $selectedFormat",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun RecentImportItem(card: Card, viewModel: ImportViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.question,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (card.subject.isNotEmpty()) {
                    Text(
                        text = card.subject,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            IconButton(
                onClick = { viewModel.deleteCard(card.id) }
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = Error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ManualCardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(Card.TYPE_QA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("手动添加卡片", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("问题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("答案") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("科目") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("详细说明 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "卡片类型",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == Card.TYPE_QA,
                        onClick = { selectedType = Card.TYPE_QA },
                        label = { Text("问答卡") }
                    )
                    FilterChip(
                        selected = selectedType == Card.TYPE_FILL_BLANK,
                        onClick = { selectedType = Card.TYPE_FILL_BLANK },
                        label = { Text("填空卡") }
                    )
                    FilterChip(
                        selected = selectedType == Card.TYPE_CODE,
                        onClick = { selectedType = Card.TYPE_CODE },
                        label = { Text("代码片段") }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == Card.TYPE_IMAGE,
                        onClick = { selectedType = Card.TYPE_IMAGE },
                        label = { Text("图片卡") }
                    )
                    FilterChip(
                        selected = selectedType == Card.TYPE_AUDIO,
                        onClick = { selectedType = Card.TYPE_AUDIO },
                        label = { Text("音频卡") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(question, answer, subject, detail, selectedType) },
                enabled = question.isNotBlank() && answer.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}