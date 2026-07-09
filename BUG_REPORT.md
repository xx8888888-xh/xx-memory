# XX Memory — 对抗式代码审查与修复报告

> 审查基准：commit `ef135d9`（Initial snapshot from main）
> 修复后分支：`review`
> 审查日期：2026-07-09

---

## 一、P0 级 Bug（功能错误，会导致运行时数据损坏）

### 1. FSRS 算法 difficulty 字段存储错误

**文件**：`MemoryAlgorithm.kt` + `ReviewViewModel.kt`

**问题描述**：
在 `ReviewViewModel.assessCard` 中，更新卡片时的代码为：
```kotlin
difficulty = result.nextEaseFactor // Keep difficulty in sync with EF.
```

FSRS 算法的 `difficulty`（D 值）范围是 [1.0, 10.0]，而 `easeFactor`（EF）范围是 [1.3, 3.0]。将 EF 值直接存入 difficulty 字段，会导致：
- difficulty 被压缩到 1.3~3.0 范围（应为 1~10）
- 下次 FSRS 计算时，`easeFactorToDifficulty()` 会将这个错误值 [1.3, 3.0] 再映射转换一次，产生完全错误的 difficulty，进而导致错误的间隔计算

这是**数据损坏型 Bug**——仅在使用 FSRS 算法时触发，SM-2 不受影响（SM-2 不使用 difficulty 字段）。

**修复方法**：
1. 在 `MemoryAlgorithm.ScheduleResult` 中新增 `nextDifficulty: Double = 0.0` 字段（默认 0.0 表示 N/A）
2. `FsrsAlgorithm.calculate` 返回 `nextDifficulty = newDifficulty`
3. `SM2Algorithm` 和 `EbbinghausFixedAlgorithm` 不设置（保持默认 0.0）
4. 在 `ReviewViewModel.assessCard` 中：
```kotlin
difficulty = if (result.nextDifficulty > 0) result.nextDifficulty.toFloat() else card.difficulty
```

---

## 二、P1 级 Bug（逻辑缺陷，可能导致特定条件下错误）

### 2. Statistics 连续天数计算受夏令时影响

**文件**：`StatisticsViewModel.kt`

**问题描述**：
`calculateStreak()` 使用硬编码 `checkDay -= 86400000L` 向前推算日期。在有夏令时的时区，UTC 偏移量变化会导致某天不是精确的 86400000 ms（可能是 82800000 或 90000000 ms），导致日期边界漂移，连续天数计算偏移。

**修复方法**：
改用 `Calendar.add(Calendar.DAY_OF_YEAR, -1)` 代替硬编码毫秒数，让系统 API 正确处理夏令时。

```kotlin
// 修复前
var checkDay = todayStart
for (i in 0..365) {
    // ...
    checkDay -= 86400000L
}

// 修复后
val cal = Calendar.getInstance()
cal.timeInMillis = todayStart
for (i in 0..365) {
    val dayStart = CardRepository.getStartOfDay(cal.timeInMillis)
    // ...
    cal.add(Calendar.DAY_OF_YEAR, -1)  // 安全滚动日期
}
```

### 3. EbbinghausAlgorithm 缺少 "SM-2" 算法类型显式匹配

**文件**：`EbbinghausAlgorithm.kt`

**问题描述**：
`getAlgorithm()` 的 `when` 分支只匹配 `"艾宾浩斯固定"` 和 `"FSRS"`，其他类型（包括默认的 `"SM-2"`）走 `else` 降级到 `SM2Algorithm`。虽然运行时行为正确（降级到 SM2），但会打印 `Log.w` 警告日志，且在算法选择逻辑上不严谨。

**修复方法**：
添加 `"SM-2" -> SM2Algorithm` 显式匹配分支：
```kotlin
return when (type) {
    "SM-2" -> SM2Algorithm
    "艾宾浩斯固定" -> EbbinghausFixedAlgorithm
    "FSRS" -> FsrsAlgorithm
    else -> {
        Log.w("EbbinghausAlgorithm", "未知算法类型: $type, 降级为SM-2")
        SM2Algorithm
    }
}
```

### 4. 导航栏 eink 模式颜色错误

**文件**：`AppNavigation.kt`

**问题描述**：
底部导航栏使用硬编码 `Primary`（橙色）和 `TextSecondary`（棕色）作为选中/未选中颜色。在 eink 模式下，导航栏仍显示彩色图标与文字。

**修复方法**：
改为使用 `MaterialTheme.colorScheme.onSurface` 和 `MaterialTheme.colorScheme.onSurfaceVariant`：
```kotlin
colors = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.onSurface,
    selectedTextColor = MaterialTheme.colorScheme.onSurface,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)
```

---

## 三、P2 级问题（UI 一致性：eink 模式颜色硬编码）

### 5. 5 个 Screen 文件中硬编码 Color.kt 常量

**涉及文件**（共 5 个）：
- `HomeScreen.kt`
- `ReviewScreen.kt`
- `ImportScreen.kt`
- `StatisticsScreen.kt`
- `SettingsScreen.kt`

**问题描述**：
上述文件中大量使用 `com.xxmemory.app.ui.theme.*` 直接定义的色彩常量（`Primary`、`PrimaryLight`、`Success`、`Warning`、`Error`、`Info`、`Background`、`Surface`、`TextPrimary`、`TextSecondary`、`TextTertiary`、`Outline`）。这些常量在普通模式下提供温暖的棕色系配色，但在 eink 模式下无法自动切换为灰阶色。

**修复方法**：
对所有 5 个文件执行系统性替换，将硬编码颜色常量替换为 `MaterialTheme.colorScheme.*` 动态引用：

| 硬编码常量 | 替换为目标 | 普通模式效果 | Eink 模式效果 |
|-----------|-----------|------------|-------------|
| `Primary` | `MaterialTheme.colorScheme.primary` | 橙色 #E8916E | 黑色 |
| `PrimaryLight` | `MaterialTheme.colorScheme.primaryContainer` | 浅橙 #F0A88A | 白色 |
| `Background` | `MaterialTheme.colorScheme.background` | 暖白 #FAF6F2 | 纯白 |
| `Surface` | `MaterialTheme.colorScheme.surface` | 纯白 | 纯白 |
| `TextPrimary` | `MaterialTheme.colorScheme.onSurface` | 深棕 #4A3F37 | 黑色 |
| `TextSecondary` | `MaterialTheme.colorScheme.onSurfaceVariant` | 中棕 #8C7E72 | 深灰 |
| `TextTertiary` | `MaterialTheme.colorScheme.outline` | 浅棕 #B8A99E | 浅灰 |
| `Success` | `MaterialTheme.colorScheme.tertiary` | 浅橙 (fallback) | 灰色 |
| `Warning` | `MaterialTheme.colorScheme.secondary` | 橙色 (fallback) | 深灰 |
| `Error` | `MaterialTheme.colorScheme.error` | 红色 | 黑色 |
| `Info` | `MaterialTheme.colorScheme.tertiary` | 浅橙 (fallback) | 灰色 |
| `Outline` | `MaterialTheme.colorScheme.outline` | 边框色 | 浅灰 |

共涉及约 **180+ 处**颜色引用替换，覆盖背景色、文本色、图标色、按钮色、进度条色、分隔线色、卡片容器色等所有 UI 组件。

---

## 四、P3 级问题（资源与性能）

### 6. ImportViewModel 持续监听所有卡片

**文件**：`ImportViewModel.kt`

**问题**：`loadRecentImports()` 使用 `repository.getAllCards().collect` 持续监听 Flow。Room Flow 在每次数据库变化时重新发射，即使 Import 页面不在前台。不过 `loadRecentImports` 在 ViewModel 初始化时被调用，collect 会随 viewModelScope 取消。

**评估**：非紧急。ViewModel 作用域内监听是标准模式，Room Flow 经过了高效设计（仅在数据变化时通知）。标记为观察项。

---

## 五、已确认正确的功能闭环

完成上述修复后，逐项确认以下功能闭环：

| 功能 | 状态 | 说明 |
|------|------|------|
| 通知权限申请 | ✅ | POST_NOTIFICATIONS 在 Android 13+ 申请，含拒后提示 |
| 精确闹钟权限 | ✅ | SCHEDULE_EXACT_ALARM 通过 `canScheduleExactAlarms()` 检查 |
| 每日提醒调度 | ✅ | MainActivity → NotificationScheduler → AlarmReceiver → Scheduler → 次日重调度 |
| 开机恢复提醒 | ✅ | BootReceiver 监听 BOOT_COMPLETED，重调度每日提醒 |
| TTS 生命周期 | ✅ | DisposableEffect shutdown() 确保释放 |
| MediaPlayer 释放 | ✅ | DisposableEffect release() |
| 多媒体卡片渲染 | ✅ | QA/填空/代码/图片/音频 5 种类型均正确渲染 |
| SM-2 算法 | ✅ | EF 计算、间隔递增、重置逻辑 |
| 艾宾浩斯算法 | ✅ | 固定间隔序列 [1,2,4,7,15,30,60,180] 天 |
| FSRS v4 算法 | ✅ | D/S/R 三参数模型 + 正确 persistence |
| 数据导入 | ✅ | 文件/JSON/手动/4 种解析器 |
| 数据库迁移 | ✅ | MIGRATION_1_2 (v1→v2 添加 review_logs 索引) |
| ProGuard | ✅ | Entity、Gson、Compose、Coil、commonmark、opencsv 保持规则 |
| 设置持久化 | ✅ | SharedPreferences + ViewModel 双向同步 |
| 统计 | ✅ | 连续天数、今日/总复习数、周趋势、科目掌握度 |

---

## 六、提交历史

```
7c86fe5 review: use MaterialTheme.colorScheme in nav bar for eink compatibility
43615ab review: fix FSRS difficulty storage bug, add SM-2 explicit match, DST-safe streak calc
d3bfd12 review: intermediate fixes before final pass
ef135d9 Initial snapshot from main
```
