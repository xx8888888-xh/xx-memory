package com.xxmemory.app.ui.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import com.xxmemory.app.BuildConfig
import com.xxmemory.app.ui.theme.EinkFilterChip
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SpeakerNotes
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xxmemory.app.domain.SchedulerUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showProfileDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var focusedSlotsInput by remember(uiState.focusedTimeSlots) { mutableStateOf(uiState.focusedTimeSlots) }
    var focusedSlotsError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Profile card
        ProfileCard(
            userName = uiState.userName,
            userEmail = uiState.userEmail,
            onClick = { showProfileDialog = true }
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Review strategy section
        SectionTitle("复习策略")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "每日卡片限制",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = uiState.dailyCardLimitEnabled,
                        onCheckedChange = { viewModel.toggleDailyCardLimitEnabled(it) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (uiState.dailyCardLimitEnabled) "${uiState.dailyCardLimit} 张" else "无限制",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.dailyCardLimitEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = uiState.dailyCardLimit.toFloat(),
                    onValueChange = { viewModel.setDailyCardLimit(it.toInt()) },
                    valueRange = 5f..100f,
                    steps = 18,
                    enabled = uiState.dailyCardLimitEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        disabledThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        disabledActiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                )
            }
            Divider(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            SwitchItem(
                icon = Icons.Filled.Shuffle,
                title = "随机顺序复习",
                checked = uiState.shuffleCards,
                onCheckedChange = { viewModel.toggleShuffle(it) }
            )
            Divider(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            SwitchItem(
                icon = Icons.Filled.Visibility,
                title = "先显示详细说明",
                checked = uiState.showDetailFirst,
                onCheckedChange = { viewModel.toggleShowDetailFirst(it) }
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Study mode section
        SectionTitle("复习模式")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "当前模式: ${if (uiState.studyMode == "focused") "集中模式" else "自由模式"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (uiState.studyMode == "focused") {
                        "算法安排的复习日会迁就到你指定的集中时间段。"
                    } else {
                        "完全由算法自主安排每日复习。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EinkFilterChip(
                        selected = uiState.studyMode == "free",
                        onClick = { viewModel.setStudyMode("free") },
                        label = "自由模式",
                        modifier = Modifier.weight(1f)
                    )
                    EinkFilterChip(
                        selected = uiState.studyMode == "focused",
                        onClick = { viewModel.setStudyMode("focused") },
                        label = "集中模式",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (uiState.studyMode == "focused") {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = focusedSlotsInput,
                        onValueChange = {
                            focusedSlotsInput = it
                            focusedSlotsError = null
                        },
                        label = { Text("集中复习时间 (HH:mm, 逗号分隔)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = focusedSlotsError != null,
                        supportingText = {
                            focusedSlotsError?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                    val parsed = SchedulerUtils.parseFocusedSlots(focusedSlotsInput)
                    val inputSlots = focusedSlotsInput.split(",", "；", ";", " ")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    val isValid = inputSlots.isEmpty() || parsed.size == inputSlots.size
                    if (!isValid) {
                        Text(
                            text = "格式示例：08:00,12:00,20:00",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            if (viewModel.setFocusedTimeSlots(focusedSlotsInput)) {
                                focusedSlotsError = null
                                Toast.makeText(context, "已保存复习时间", Toast.LENGTH_SHORT).show()
                            } else {
                                focusedSlotsError = "存在非法时间格式，请检查"
                            }
                        },
                        enabled = isValid
                    ) {
                        Text("保存时间")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Memory algorithm section
        SectionTitle("记忆算法")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "当前算法: ${uiState.algorithmType}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (uiState.algorithmType) {
                        "SM-2" -> "经典间隔重复算法，使用难度因子(EF)动态调整复习间隔"
                        "艾宾浩斯固定" -> "基于艾宾浩斯遗忘曲线的固定复习间隔: 1天→2天→4天→7天→15天→30天"
                        "FSRS" -> "Free Spaced Repetition Scheduler，机器学习驱动的自适应调度算法"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SM-2", "艾宾浩斯固定", "FSRS").forEach { algo ->
                        EinkFilterChip(
                            selected = uiState.algorithmType == algo,
                            onClick = { viewModel.setAlgorithmType(algo) },
                            label = algo,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "复习模式: ${reviewModeLabel(uiState.reviewMode)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reviewModeDescription(uiState.reviewMode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "flashcard" to "闪卡",
                        "baicizhan" to "百词斩",
                        "bbdc" to "不背"
                    ).forEach { (mode, label) ->
                        EinkFilterChip(
                            selected = uiState.reviewMode == mode,
                            onClick = { viewModel.setReviewMode(mode) },
                            label = label,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                SwitchItem(
                    icon = Icons.Filled.School,
                    title = "百词斩深度模式",
                    subtitle = "详情页展示词根词缀、词组搭配、押韵等完整内容",
                    checked = uiState.baicizhanDeepMode,
                    onCheckedChange = { viewModel.toggleBaicizhanDeepMode(it) }
                )
                if (uiState.reviewMode == "bbdc") {
                    Divider(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    SwitchItem(
                        icon = Icons.Filled.AutoStories,
                        title = "不背单词沉浸刷词",
                        subtitle = "全屏极简界面，自动朗读，专注刷词",
                        checked = uiState.bbdcImmersiveMode,
                        onCheckedChange = { viewModel.toggleBbdcImmersiveMode(it) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // TTS section
        SectionTitle("朗读")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard {
            SwitchItem(
                icon = Icons.Filled.VolumeUp,
                title = "进入卡片自动朗读",
                subtitle = "翻到问题面时自动朗读问题文本",
                checked = uiState.ttsAutoPlayQuestion,
                onCheckedChange = { viewModel.toggleTtsAutoPlayQuestion(it) }
            )
            Divider(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            SwitchItem(
                icon = Icons.Filled.VolumeUp,
                title = "揭晓答案自动朗读",
                subtitle = "显示答案后自动朗读答案文本",
                checked = uiState.ttsAutoPlayAnswer,
                onCheckedChange = { viewModel.toggleTtsAutoPlayAnswer(it) }
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Appearance section
        SectionTitle("外观")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard {
            SwitchItem(
                icon = Icons.Filled.BrightnessMedium,
                title = "墨水屏专用模式",
                subtitle = "移除所有动画，使用黑白极简配色，适合电子墨水屏设备",
                checked = uiState.einkMode,
                onCheckedChange = { viewModel.toggleEinkMode(it) }
            )
            Divider(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            SwitchItem(
                icon = Icons.Filled.SpeakerNotes,
                title = "自动朗读",
                checked = uiState.autoPlayAudio,
                onCheckedChange = { viewModel.toggleAutoPlay(it) }
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Notification section
        SectionTitle("通知")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard {
            SwitchItem(
                icon = Icons.Filled.Notifications,
                title = "复习提醒",
                subtitle = "有卡片到期时在指定时间点提醒",
                checked = uiState.dailyReminder,
                onCheckedChange = {
                    val success = viewModel.toggleDailyReminder(it)
                    if (!success) {
                        Toast.makeText(context, "需要通知和精确闹钟权限才能开启提醒", Toast.LENGTH_LONG).show()
                    }
                }
            )
            if (uiState.dailyReminder) {
                Divider(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "提醒时间点",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${uiState.reminderTimeSlots.size} 个",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.reminderTimeSlots.forEach { slot ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = slot,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { viewModel.removeReminderSlot(slot) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showTimePickerDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "添加提醒时间点",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // About section
        SectionTitle("关于")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard {
            SettingsItem(
                icon = Icons.Filled.Info,
                title = "版本",
                subtitle = BuildConfig.VERSION_NAME
            )
            Divider(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            SettingsItem(
                icon = Icons.Filled.AutoAwesome,
                title = "xx memory",
                subtitle = "通用智能记忆助手"
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showProfileDialog) {
        ProfileEditDialog(
            currentName = uiState.userName,
            currentEmail = uiState.userEmail,
            onDismiss = { showProfileDialog = false },
            onConfirm = { name, email ->
                viewModel.setUserName(name)
                viewModel.setUserEmail(email)
                showProfileDialog = false
            }
        )
    }

    if (showTimePickerDialog) {
        TimePickerDialog(
            currentHour = 9,
            currentMinute = 0,
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { hour, minute ->
                viewModel.addReminderSlot(hour, minute)
                showTimePickerDialog = false
            }
        )
    }

    uiState.permissionRationale?.let { rationale ->
        val (title, text, onConfirm) = when (rationale) {
            "notification" -> Triple(
                "需要通知权限",
                "每日复习提醒需要通知权限才能正常工作。请前往应用通知设置开启权限。",
                {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                    viewModel.dismissPermissionRationale()
                }
            )
            "exact_alarm" -> Triple(
                "需要精确闹钟权限",
                "每日复习提醒需要精确闹钟权限才能准时触发。请前往系统设置开启权限。",
                { viewModel.openExactAlarmSettings() }
            )
            else -> Triple(
                "需要权限",
                "开启此功能需要相关权限。",
                { viewModel.dismissPermissionRationale() }
            )
        }
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermissionRationale() },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text("去开启") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPermissionRationale() }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ProfileCard(
    userName: String,
    userEmail: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.surface,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun ProfileEditDialog(
    currentName: String,
    currentEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑个人信息", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("昵称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("邮箱") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), email.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun reviewModeLabel(mode: String): String = when (mode) {
    "flashcard" -> "经典闪卡"
    "baicizhan" -> "百词斩"
    "bbdc" -> "不背单词"
    else -> mode
}

private fun reviewModeDescription(mode: String): String = when (mode) {
    "flashcard" -> "点击翻面后自评掌握程度，适合通用记忆卡片"
    "baicizhan" -> "看单词选释义，支持提示、拼写、斩熟词与图文/深度记忆"
    "bbdc" -> "认识/不认识二选一，再验证释义，强调真实语境例句"
    else -> ""
}

@Composable
private fun TimePickerDialog(
    currentHour: Int,
    currentMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var hour by remember { mutableStateOf(currentHour.toString()) }
    var minute by remember { mutableStateOf(currentMinute.toString().padStart(2, '0')) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置提醒时间", fontWeight = FontWeight.Bold) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = hour,
                    onValueChange = { value ->
                        hour = value.filter { it.isDigit() }.take(2)
                    },
                    label = { Text("时") },
                    modifier = Modifier.width(80.dp),
                    singleLine = true
                )
                Text(":", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = minute,
                    onValueChange = { value ->
                        minute = value.filter { it.isDigit() }.take(2)
                    },
                    label = { Text("分") },
                    modifier = Modifier.width(80.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: currentHour
                    val m = minute.toIntOrNull()?.coerceIn(0, 59) ?: currentMinute
                    onConfirm(h, m)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        content()
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String = "",
    onClick: () -> Unit = {},
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
