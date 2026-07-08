package com.xxmemory.app.ui.settings

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SpeakerNotes
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Profile card
        ProfileCard(userName = uiState.userName, userEmail = uiState.userEmail)
        Spacer(modifier = Modifier.height(20.dp))

        // Account section
        SectionTitle("账户")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard {
            SettingsItem(
                icon = Icons.Filled.Person,
                title = "个人信息",
                subtitle = uiState.userName,
                onClick = { }
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
                        color = TextPrimary
                    )
                    Text(
                        text = "${uiState.dailyCardLimit} 张",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = uiState.dailyCardLimit.toFloat(),
                    onValueChange = { viewModel.setDailyCardLimit(it.toInt()) },
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

        // Appearance section
        SectionTitle("外观")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard {
            SwitchItem(
                icon = Icons.Filled.DarkMode,
                title = "深色模式",
                checked = uiState.darkMode,
                onCheckedChange = { viewModel.toggleDarkMode(it) }
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
                subtitle = "每天定时提醒复习",
                checked = uiState.dailyReminder,
                onCheckedChange = { viewModel.toggleDailyReminder(it) }
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
                subtitle = "1.0.0",
                onClick = { }
            )
            Divider(color = PrimaryLight.copy(alpha = 0.2f))
            SettingsItem(
                icon = Icons.Filled.AutoAwesome,
                title = "xx memory",
                subtitle = "通用智能记忆助手",
                onClick = { }
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ProfileCard(userName: String, userEmail: String) {
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
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1),
                    style = MaterialTheme.typography.headlineMedium,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
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
        colors = CardDefaults.cardColors(containerColor = Surface)
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
                color = TextPrimary
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
                color = TextPrimary
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