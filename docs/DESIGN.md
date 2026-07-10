# xx memory 增强设计文档

## 目标
实现不背单词式复习流程、自由/集中复习模式、音频/默写/填空卡片、日历视图、二级导入菜单，并保证数据持久化与墨水屏合规。

## 1. 数据层

### Card 实体扩展
新增字段（Migration 4→5）：
- `learning_stage` INTEGER DEFAULT 0    // 0=新学, 1=一次通过选项, 2=例句阶段通过, 3=完全独立通过（已学会）
- `learning_started_at` INTEGER DEFAULT 0  // 首次学习开始时间，用于学习阶段冷却
- `dictation_progress` TEXT DEFAULT ""  // 默写/填空进度（可选）

Card companion 增加 `TYPE_DICTATION = "dictation"`。

### DAO 扩展
- `getCardsForCalendar(start, end)`：按 next_review_date 区间查询，用于日历。
- `getDueCountForDay(timestamp)`：某一天的待复习数量。

### SettingsManager 扩展
- `studyMode: String` // "free" 自由模式 / "focused" 集中模式
- `focusedTimeSlots: String` // 集中模式下用户指定时间点，逗号分隔 HH:mm，如 "08:00,12:00,20:00"
- `ttsAutoPlayQuestion: Boolean` // 进入卡片自动朗读问题
- `ttsAutoPlayAnswer: Boolean`   // 揭晓答案后自动朗读答案/音频

## 2. 复习调度（自由/集中模式）

所有算法返回的 nextReviewDate 为当天 0 点（已有 getNextDayTimestamp）。
- 自由模式：直接使用算法结果。
- 集中模式：取算法结果当天 0 点，查找 focusedTimeSlots 中最近的时间点；若算法结果跨天，则取目标日期最近的指定时间。通过 `SchedulerUtils.adjustToFocusedSlot(nextDayTimestamp, slots)` 实现。

提醒系统（AlarmReceiver）在设定的 focusedTimeSlots 或默认 20:00 触发，查询当天有到期卡片时显示通知。

## 3. 不背单词复习流程

### 新卡片（learning_stage < 3）
1. Stage 0：四选一（question + 4 个 answer 选项）。
   - 选对 → stage=1，记录一次成功，quality=3，进入下一张。
   - 选错 → 显示答案，用户选择“有点模糊/记不清/记错了”，按选择映射 quality（2/1/0），stage 保持 0，安排复习。
2. Stage 1：展示例句（example，不显示答案），用户自评“清晰/记错了”。
   - 清晰 → stage=2，quality=3。
   - 记错了 → quality=0，stage 保持 1。
3. Stage 2：不展示例句，直接问答案，用户自评“记对了/有点模糊/记不清”。
   - 记对了 → stage=3（已学会），quality=3。
   - 模糊/记不清 → quality=2/1，stage 退回 1。
   - 记错了 → quality=0，stage 保持 2。

### 复习卡片（learning_stage == 3）
直接展示 question，用户自评“记对了/有点模糊/记不清”。
- 记对了 → quality=3
- 有点模糊 → quality=2
- 记不清 → quality=1
- 查看答案后可重新选择（更改自评），再提交。

quality 映射统一：
- 0 = 忘记/记错了
- 1 = 困难/记不清
- 2 = 良好/有点模糊
- 3 = 简单/记对了

## 4. 音频与默写

- 新增 `AudioPlayer` 工具类（MediaPlayer + TTS 包装）。
- dictation 卡片进入时自动播放 audioUrl；如无 audioUrl 则使用 TTS 朗读 answer。
- 默写界面显示 question，播放音频，下方输入框让用户输入完整文本；提交后对比 answer。
- fill_blank 卡片用下划线替换 answer，用户输入；支持同时播放音频（如果有 audioUrl）。

## 5. 日历视图

- 首页“未来一周复习安排”点击后弹出 FullMonthCalendarDialog。
- 一周视图：从明天起 7 天，每天显示到期数量；有到期用深色背景。
- 一月视图：当前月，每天显示到期数量，点击日期显示该日具体卡片列表与时间（基于 focusedTimeSlots 或默认时间）。

## 6. 导入二级菜单

导入页保留顶部“从文件导入”。
其他导入（JSON 粘贴、手动添加、AI Skill）折叠到“更多导入方式”二级菜单卡片中，点击展开显示三个子选项。

## 7. 墨水屏合规

- 所有动画/ripple 已禁用（Theme.kt NoIndication）。
- 新增/修改图标统一使用 Color.DarkGray；按钮填充使用 surfaceVariant/gray，仅边框可用 Black。
- 避免 CircularProgressIndicator、AnimatedContent 等；用静态 Box/Text 替代。

## 8. 测试计划

- 单元测试：验证自由/集中模式时间调整、learning stage 状态机、三种算法科学性。
- 模拟器测试：
  1. 导入 JSON/CSV/TXT/Markdown 卡片
  2. 切换复习模式，测试不背单词完整流程
  3. 测试 dictation/fill_blank 卡片与音频
  4. 测试日历一周/一月视图
  5. 测试设置页自由/集中模式切换与提醒时间
  6. 切换墨水屏模式验证无动画、灰色图标
  7. 杀掉应用重进，验证数据保留
