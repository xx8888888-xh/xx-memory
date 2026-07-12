package com.xxmemory.app.ui.import

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xxmemory.app.data.entity.Card
import com.xxmemory.app.ui.theme.EinkAlertDialog
import com.xxmemory.app.ui.theme.EinkFilterChip
import com.xxmemory.app.ui.theme.rememberEinkMode

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: ImportViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showManualDialog by remember { mutableStateOf(false) }
    val isEinkMode = rememberEinkMode()

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
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "导入卡片",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.isImporting) {
                if (isEinkMode) {
                    Text(
                        text = "导入中...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // File picker section
            FileImportSection(viewModel = viewModel, isEinkMode = isEinkMode)
            Spacer(modifier = Modifier.height(16.dp))

            // Collapsible secondary import menu
            MoreImportOptionsCard(
                uiState = uiState,
                viewModel = viewModel,
                isEinkMode = isEinkMode,
                onShowManualDialog = { showManualDialog = true }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Supported format chips
            FormatChipsSection(
                selectedFormat = uiState.selectedFormat,
                onFormatSelected = { viewModel.selectFormat(it) },
                isEinkMode = isEinkMode
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Recent imports
            Text(
                text = "最近导入",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                if (isEinkMode) {
                    Text(
                        text = "加载中...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                uiState.recentImports.forEach { card ->
                    RecentImportItem(card = card, viewModel = viewModel, isEinkMode = isEinkMode)
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
            isEinkMode = isEinkMode,
            onConfirm = { q, a, s, d, tags, type, img, aud, phonetic, example, collocations, etymology, hint, rhyme, derivatives, distractors, mnemonics ->
                viewModel.importManualCard(
                    question = q,
                    answer = a,
                    subject = s,
                    detail = d,
                    cardType = type,
                    tags = tags,
                    imageUrl = img,
                    audioUrl = aud,
                    phonetic = phonetic,
                    example = example,
                    collocations = collocations,
                    etymology = etymology,
                    hint = hint,
                    rhyme = rhyme,
                    derivatives = derivatives,
                    distractors = distractors,
                    mnemonics = mnemonics
                )
                showManualDialog = false
            }
        )
    }
}

@Composable
private fun FileImportSection(viewModel: ImportViewModel, isEinkMode: Boolean) {
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { launcher.launch("*/*") },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    .background(
                        if (isEinkMode) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FileUpload,
                    contentDescription = null,
                    tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "从文件导入",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "支持 JSON, CSV, TXT, Markdown 格式",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MoreImportOptionsCard(
    uiState: ImportUiState,
    viewModel: ImportViewModel,
    isEinkMode: Boolean,
    onShowManualDialog: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showJsonInput by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isEinkMode) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = null,
                            tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "更多导入方式",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "JSON、手动添加、AI 智能导入",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // JSON import
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showJsonInput = !showJsonInput }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = null,
                        tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "从 JSON 导入",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (showJsonInput) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (showJsonInput) {
                    OutlinedTextField(
                        value = uiState.urlInput,
                        onValueChange = { viewModel.updateUrlInput(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("粘贴 JSON 数据...") },
                        minLines = 3,
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.importFromJson(uiState.urlInput) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("导入")
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Manual add
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onShowManualDialog() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "手动添加卡片",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // AI import
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showAiDialog = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.SmartToy,
                        contentDescription = null,
                        tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "AI 智能导入",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showAiDialog) {
        AiImportInfoDialog(
            onDismiss = { showAiDialog = false },
            isEinkMode = isEinkMode
        )
    }
}

@Composable
private fun AiImportInfoDialog(
    onDismiss: () -> Unit,
    isEinkMode: Boolean
) {
    EinkAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 智能导入", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "AI 智能导入需要配合项目中的 AI Skill 文件使用。\n\n使用方式：\n1. 将 skill/xx-memory-skill.json 文件导入支持的 AI 助手\n2. 明确告知 AI 卡片类型：单词类型请使用 cardType: vocabulary，古诗文类型请使用 cardType: poetry\n3. 通过对话让 AI 生成符合项目格式的 JSON/CSV/Markdown/TXT 卡片数据\n4. 将生成的内容粘贴到「从 JSON 导入」或保存为文件导入\n\nAI 助手将帮助你批量生成记忆卡片。"
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FormatChipsSection(
    selectedFormat: String,
    onFormatSelected: (String) -> Unit,
    isEinkMode: Boolean
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EinkFilterChip(
                selected = selectedFormat == "JSON",
                onClick = { onFormatSelected(if (selectedFormat == "JSON") "" else "JSON") },
                label = "JSON"
            )
            EinkFilterChip(
                selected = selectedFormat == "CSV",
                onClick = { onFormatSelected(if (selectedFormat == "CSV") "" else "CSV") },
                label = "CSV"
            )
            EinkFilterChip(
                selected = selectedFormat == "TXT",
                onClick = { onFormatSelected(if (selectedFormat == "TXT") "" else "TXT") },
                label = "TXT"
            )
            EinkFilterChip(
                selected = selectedFormat == "Markdown",
                onClick = { onFormatSelected(if (selectedFormat == "Markdown") "" else "Markdown") },
                label = "Markdown"
            )
        }
        if (selectedFormat.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "已选择格式: $selectedFormat",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecentImportItem(card: Card, viewModel: ImportViewModel, isEinkMode: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.question,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (card.subject.isNotEmpty()) {
                    Text(
                        text = card.subject,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = { viewModel.deleteCard(card.id) }
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
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
    isEinkMode: Boolean,
    onConfirm: (String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var audioUrl by remember { mutableStateOf("") }
    var phonetic by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }
    var collocations by remember { mutableStateOf("") }
    var etymology by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var rhyme by remember { mutableStateOf("") }
    var derivatives by remember { mutableStateOf("") }
    var distractors by remember { mutableStateOf("") }
    var mnemonics by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(Card.TYPE_QA) }

    EinkAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("手动添加卡片", fontWeight = FontWeight.Bold)
        },
        text = {
            val scrollState = rememberScrollState()
            Column(modifier = Modifier.verticalScroll(scrollState)) {
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
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("标签 (用英文逗号分隔)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phonetic,
                    onValueChange = { phonetic = it },
                    label = { Text("音标 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it },
                    label = { Text("例句 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = collocations,
                    onValueChange = { collocations = it },
                    label = { Text("词组搭配 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = etymology,
                    onValueChange = { etymology = it },
                    label = { Text("词根词缀 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("提示 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = rhyme,
                    onValueChange = { rhyme = it },
                    label = { Text("押韵词 / 联想词 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = derivatives,
                    onValueChange = { derivatives = it },
                    label = { Text("派生词 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = distractors,
                    onValueChange = { distractors = it },
                    label = { Text("四选一干扰项 (可选，单词卡用英文逗号分隔)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = mnemonics,
                    onValueChange = { mnemonics = it },
                    label = { Text("助记 (可选，谐音/词根/联想/图片提示)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "卡片类型",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EinkFilterChip(
                        selected = selectedType == Card.TYPE_QA,
                        onClick = { selectedType = Card.TYPE_QA },
                        label = "问答卡"
                    )
                    EinkFilterChip(
                        selected = selectedType == Card.TYPE_CODE,
                        onClick = { selectedType = Card.TYPE_CODE },
                        label = "代码片段"
                    )
                    EinkFilterChip(
                        selected = selectedType == Card.TYPE_IMAGE,
                        onClick = { selectedType = Card.TYPE_IMAGE },
                        label = "图片卡"
                    )
                    EinkFilterChip(
                        selected = selectedType == Card.TYPE_AUDIO,
                        onClick = { selectedType = Card.TYPE_AUDIO },
                        label = "音频卡"
                    )
                    EinkFilterChip(
                        selected = selectedType == Card.TYPE_DICTATION,
                        onClick = { selectedType = Card.TYPE_DICTATION },
                        label = "默写卡"
                    )
                    EinkFilterChip(
                        selected = selectedType == Card.TYPE_VOCABULARY,
                        onClick = { selectedType = Card.TYPE_VOCABULARY },
                        label = "单词卡"
                    )
                    EinkFilterChip(
                        selected = selectedType == Card.TYPE_POETRY,
                        onClick = { selectedType = Card.TYPE_POETRY },
                        label = "古诗文"
                    )
                }
                if (selectedType == Card.TYPE_IMAGE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("图片 URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                if (selectedType == Card.TYPE_AUDIO || selectedType == Card.TYPE_DICTATION || selectedType == Card.TYPE_POETRY) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = audioUrl,
                        onValueChange = { audioUrl = it },
                        label = { Text("音频 URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(question, answer, subject, detail, tags, selectedType, imageUrl, audioUrl, phonetic, example, collocations, etymology, hint, rhyme, derivatives, distractors, mnemonics) },
                enabled = question.isNotBlank() && answer.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                )
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
