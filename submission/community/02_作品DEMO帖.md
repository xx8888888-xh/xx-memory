# 【初赛作品】xx memory v1.0.0-alpha —— 可运行 DEMO 提交

> 赛道：**#学习工作-造个新解法**
> 作品版本：v1.0.0-alpha
> 作品形态：Android App（Kotlin + Jetpack Compose）
> 完整源码：https://github.com/xx8888888-xh/xx-memory
> APK 直链：https://github.com/xx8888888-xh/xx-memory/blob/main/releases/xx-memory-v1.0.0-alpha.apk
> 全程使用：TRAE IDE

---

## 一、作品一句话介绍

把任意学习资料一键转化为按艾宾浩斯曲线科学排布的复习卡片，AI 辅助制卡 + 智能排序 + 多模态复习，解决「背了忘、忘了背」的低效记忆痛点。

---

## 二、可运行 DEMO

**APK 大小**：17.6 MB
**最低支持**：Android 8.0 (API 26)
**目标系统**：Android 14 (API 34)
**验证状态**：
- ✅ `gradlew.bat assembleDebug` BUILD SUCCESSFUL
- ✅ MuMu 模拟器（Android 12, API 32）安装运行无崩溃
- ✅ MainActivity 正常启动，前台运行无 FATAL 异常
- ✅ 4 个单元测试套件全部通过（DifficultFirstSorterTest / JsonCardParserTest / MemoryAlgorithmTest / SchedulerUtilsTest）
- ✅ MuMu 全功能自动化点击测试 20 项通过（19 PASS / 1 INFO / 0 FAIL）

**安装方式**：
1. 手机浏览器打开 APK 链接下载安装
2. 或在电脑下载后用 `adb install xx-memory-v1.0.0-alpha.apk` 推送
3. 首次启动自动导入 6 张默认卡片（含古诗词默写卡片），可直接体验复习流程

---

## 三、核心功能清单

| 功能模块 | 完成状态 | 说明 |
|---------|---------|------|
| 多格式导入 | ✅ | Markdown / PDF / Word / Excel / 纯文本 / JSON / URL 全支持 |
| 智能制卡 | ✅ | 自动解析文档内容，提取关键知识点生成问答卡片 |
| 间隔重复算法 | ✅ | 基于艾宾浩斯遗忘曲线 + SM-2，四级评估（忘记/困难/良好/简单） |
| 墨水屏模式 | ✅ | 专为墨水屏设备优化的灰度 UI，无动画、低功耗 |
| 学习统计 | ✅ | 环形进度、周视图日历、卡片组筛选、连续学习天数（Streak） |
| 困难优先排序 | ✅ | 智能识别易遗忘卡片，优先安排复习 |
| 通知提醒 | ✅ | 基于卡片到期时间自动调度本地闹钟提醒 |
| AI Skill 支持 | ✅ | 提供 TRAE Skill 文件，AI 可直接完成「解析 → 制卡 → 编排」 |
| 新用户引导 | ✅ | 3 步卡片式引导（目标选择 / 每日学习量 / 个性化配置） |
| 默写模式 | ✅ | 类似百词斩的古诗词全文默写，强化精细编码 |
| 听写模式 | ✅ | TTS 朗读题目 + 语音输入，多感官辅助 |
| BBDC 沉浸式复习 | ✅ | 选错重试 + 错题降级，符合「不背单词」复习流 |

---

## 四、技术架构

- **语言**：Kotlin
- **架构**：MVVM + Repository Pattern
- **UI**：Jetpack Compose（纯声明式 UI）
- **数据库**：Room（本地存储）
- **异步**：Kotlin Coroutines + Flow
- **导航**：Navigation Compose
- **算法**：艾宾浩斯遗忘曲线 + SM-2 + FSRS 混合调度
- **构建**：Gradle 8.14.4 (Kotlin DSL) + KSP

**核心代码文件**（70+ Kotlin 源文件）：
- `domain/EbbinghausAlgorithm.kt`：遗忘曲线核心算法
- `domain/MemoryAlgorithm.kt`：SM-2 评分实现
- `domain/DifficultFirstSorter.kt`：困难优先智能排序
- `domain/JsonCardParser.kt`：JSON 卡片解析器
- `domain/Scheduler.kt`：复习计划调度器
- `data/AppDatabase.kt`：Room 数据库
- `ui/review/ReviewScreen.kt`：复习主界面

---

## 五、核心功能截图

### 首页与复习

![首页](https://raw.githubusercontent.com/xx8888888-xh/xx-memory/main/screenshots/01_home_screen.png)

![复习](https://raw.githubusercontent.com/xx8888888-xh/xx-memory/main/screenshots/11_review_screen.png)

### 多格式导入

![导入页](https://raw.githubusercontent.com/xx8888888-xh/xx-memory/main/screenshots/02_import_screen.png)

![手动添加](https://raw.githubusercontent.com/xx8888888-xh/xx-memory/main/screenshots/08_manual_filled.png)

### 统计与设置

![统计](https://raw.githubusercontent.com/xx8888888-xh/xx-memory/main/screenshots/13_statistics_screen.png)

![设置](https://raw.githubusercontent.com/xx8888888-xh/xx-memory/main/screenshots/14_settings_screen.png)

![墨水屏模式](https://raw.githubusercontent.com/xx8888888-xh/xx-memory/main/screenshots/15_settings_dark_mode.png)

### 数据持久化

![持久化测试](https://raw.githubusercontent.com/xx8888888-xh/xx-memory/main/screenshots/16_persistence_test.png)

---

## 六、AI 辅助开发记录

本项目全程使用 **TRAE IDE** 进行开发，AI 辅助完成了：
- 项目脚手架搭建（MVVM + Navigation + Room）
- Jetpack Compose UI 组件生成（卡片翻转 / 进度环 / 统计图表）
- 间隔重复算法实现（SM-2 / FSRS 混合）
- 文档解析模块（Apache POI、PDFBox）
- MuMu 模拟器自动化测试脚本
- 单元测试与对抗式代码审查

**开发周期**：约 3 周（从需求分析到完整可运行 Demo）

---

## 七、复赛规划

若晋级复赛，将进一步完善：
- **手写输入**：在默写卡片上手写输入，增强肌肉记忆
- **目标留存率滑块**：自定义期望记忆保持率（90% / 95%），算法自适应
- **错误重测队列**：答错卡片在当日复习结束后二次测试
- **社区卡片市场**：用户分享优质卡片包
- **多端同步**：支持 Web 端复习

---

> **作品已开源，欢迎 Star、Fork、Issue：**
> https://github.com/xx8888888-xh/xx-memory

#TRAE AI 创造力大赛 #vibe coding 大赏
