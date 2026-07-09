package com.xxmemory.app.ui.settings

import android.widget.Toast
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SpeakerNotes
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import com.xxmemory.app.ui.theme.Background
import com.xxmemory.app.ui.theme.Primary
import com.xxmemory.app.ui.theme.PrimaryLight
import com.xxmemory.app.ui.theme.Surface
import com.xxmemory.app.ui.theme.TextPrimary
import com.xxmemory.app.ui.theme.TextSecondary
import com.xxmemory.app.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showProfileDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
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

        // Account section
        SectionTitle("账户")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard {
            SettingsItem(
                icon = Icons.Filled.Person,
                title = "个人信息",
                subtitle = uiState.userName,
                onClick = { showProfileDialog = true }
            )
            Divider(color = PrimaryLight.copy(alpha = 0.2f))
            SettingsItem(
                icon = Icons.Filled.Cloud,
                title = "数据同步",
                subtitle = if (uiState.syncEnabled) "已开启" else "已关闭",
                trailing = {
                    Switch(
                        checked = uiState.syncEnabled,
                        onCheckedChange = { viewModel.toggleSync(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Primary,
                            checkedTrackColor = PrimaryLight
                        )
                    )
                }
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Review strategy section
        SectionTitle("复习策略")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "每日卡片限制",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.dailyCardLimit} 张",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                var sliderValue by remember { mutableFloatStateOf(uiState.dailyCardLimit.toFloat()) }
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { viewModel.setDailyCardLimit(sliderValue.toInt()) },
                    valueRange = 5f..100f,
                    steps = 18,
                    colors = SliderDefaults.colors(
                        thumbColor = Primary,
                        activeTrackColor = Primary
                    )
                )
            }
            Divider(color = PrimaryLight.copy(alpha = 0.2f))
            SwitchItem(
                icon = Icons.Filled.Shuffle,
                title = "随机顺序复习",
                checked = uiState.shuffleCards,
                onCheckedChange = { viewModel.toggleShuffle(it) }
            )
            Divider(color = PrimaryLight.copy(alpha = 0.2f))
            SwitchItem(
                icon = Icons.Filled.Visibility,
                title = "先显示详细说明",
                checked = uiState.showDetailFirst,
                onCheckedChange = { viewModel.toggleShowDetailFirst(it) }
            )
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
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SM-2", "艾宾浩斯固定", "FSRS").forEach { algo ->
                        FilterChip(
                            selected = uiState.algorithmType == algo,
                            onClick = { viewModel.setAlgorithmType(algo) },
                            label = { Text(algo, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = Surface
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
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
            Divider(color = PrimaryLight.copy(alpha = 0.2f))
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
                title = "每日复习提醒",
                subtitle = "每天 ${uiState.reminderHour.toString().padStart(2, '0')}:${uiState.reminderMinute.toString().padStart(2, '0')} 提醒复习",
                checked = uiState.dailyReminder,
                onCheckedChange = {
                    val success = viewModel.toggleDailyReminder(it)
                    if (!success) {
                        Toast.makeText(context, "需要通知和精确闹钟权限才能开启提醒", Toast.LENGTH_LONG).show()
                    }
                }
            )
            Divider(color = PrimaryLight.copy(alpha = 0.2f))
            SettingsItem(
                icon = Icons.Filled.AccessTime,
                title = "提醒时间",
                subtitle = "${uiState.reminderHour.toString().padStart(2, '0')}:${uiState.reminderMinute.toString().padStart(2, '0')}",
                onClick = { showTimePickerDialog = true }
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        // About section
        SectionTitle("关于")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard {
            SettingsItem(
                icon = Icons.Filled.Info,
                title = "版本",
                subtitle = "1.0.0"
            )
            Divider(color = PrimaryLight.copy(alpha = 0.2f))
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
            currentHour = uiState.reminderHour,
            currentMinute = uiState.reminderMinute,
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { hour, minute ->
                viewModel.setReminderTime(hour, minute)
                showTimePickerDialog = false
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
                    .background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Surface,
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
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = TextTertiary
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
        color = TextSecondary
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
            tint = Primary,
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
                    color = TextSecondary
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = TextTertiary,
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Primary,
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
                    color = TextSecondary
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Primary,
                checkedTrackColor = PrimaryLight
            )
        )
    }
}
