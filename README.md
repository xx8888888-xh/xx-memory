# xx memory（通用智能记忆助手）

> 一款基于艾宾浩斯遗忘曲线与间隔重复算法的安卓智能记忆应用，将学习资料一键转化为科学排布的复习卡片。

---

## ✨ 功能特性

### 📄 多格式文档导入
- 支持 **Markdown**、**PDF**、**Word (.docx)**、**Excel (.xlsx)** 等常见文档格式
- 自动解析文档内容，提取关键知识点
- 一键生成问答式记忆卡片，告别手动逐条录入

### 🧠 科学记忆算法
- 基于 **艾宾浩斯遗忘曲线** 与 **间隔重复（Spaced Repetition）** 算法
- 自动规划每日复习任务，在最合适的时机提醒你复习
- 支持「忘记 / 困难 / 良好 / 简单」四级评估，动态调整复习间隔

### 🤖 AI Skill 支持
- 提供可上传至 AI 助手（如 TRAE）的 **Skill 文件**
- AI 自动完成文档解析 → 卡片生成 → 复习计划编排
- 支持音频、图片等多媒体卡片内容

### 📊 学习进度可视化
- 环形进度图展示当日复习完成度
- 周视图日历，直观呈现每日复习密度
- 按卡片组分类筛选，聚焦特定学习内容

### 🌐 社区卡片共享
- 优质卡片资源可在用户间共享流动
- 降低知识整理门槛，助力高效学习

### 📱 精致交互体验
- 卡片翻转动画，模拟真实闪卡体验
- 语音朗读支持，多感官辅助记忆
- 暖色调主题，柔和护眼

---

## 🛠 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | **Kotlin** |
| 架构 | **MVVM** + Repository Pattern |
| UI 框架 | **Jetpack Compose** |
| 本地数据库 | **Room** |
| 依赖注入 | **Hilt** |
| 异步处理 | **Kotlin Coroutines + Flow** |
| 导航 | **Navigation Compose** |
| 文档解析 | Apache POI (Word/Excel)、PDFBox / iText (PDF) |
| 音频播放 | MediaPlayer / ExoPlayer |
| 构建工具 | **Gradle (Kotlin DSL)** |
| 最低支持版本 | Android 8.0 (API 26) |

---

## 📁 项目结构

```
xx-memory/
├── app/                          # 主应用模块
│   ├── src/main/
│   │   ├── java/com/xxmemory/
│   │   │   ├── MainActivity.kt           # 主 Activity
│   │   │   ├── XXMemoryApp.kt            # Application 类
│   │   │   ├── data/                     # 数据层
│   │   │   │   ├── local/                # Room 数据库、DAO
│   │   │   │   ├── model/                # 数据模型
│   │   │   │   ├── parser/               # 文档解析器
│   │   │   │   └── repository/           # 数据仓库
│   │   │   ├── domain/                   # 业务逻辑层
│   │   │   │   ├── model/                # 领域模型
│   │   │   │   └── usecase/              # 用例
│   │   │   ├── ui/                       # 界面层
│   │   │   │   ├── home/                 # 首页
│   │   │   │   ├── import/               # 文档导入
│   │   │   │   ├── review/               # 复习卡片
│   │   │   │   ├── community/            # 社区
│   │   │   │   ├── profile/              # 个人中心
│   │   │   │   └── theme/                # 主题/样式
│   │   │   ├── algo/                     # 记忆算法
│   │   │   │   ├── SpacedRepetition.kt   # 间隔重复核心算法
│   │   │   │   └── EbbinghausCurve.kt    # 艾宾浩斯遗忘曲线
│   │   │   ├── skill/                    # AI Skill 支持
│   │   │   └── di/                       # Hilt 依赖注入模块
│   │   └── res/                          # 资源文件
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── skill/                        # AI Skill 文件
│   └── xx-memory-skill.json
├── docs/                         # 设计文档
│   ├── 安卓软件设计文档：通用智能记忆助手.docx
│   └── xx-memory-prototype（原型）.html
├── build.gradle.kts              # 项目级构建脚本
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## 🚀 构建与运行

### 环境要求

- **Android Studio** Hedgehog (2023.1.1) 或更高版本
- **JDK** 17 或更高版本
- **Android SDK** API 34+
- **Gradle** 8.x（项目自带 Gradle Wrapper）

### 克隆项目

```bash
git clone https://github.com/xx8888888-xh/xx-memory.git
cd xx-memory
```

### 构建调试版本

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

### 在 Android Studio 中运行

1. 使用 Android Studio 打开项目根目录
2. 等待 Gradle 同步完成
3. 连接 Android 设备或启动模拟器
4. 点击 **Run** 按钮或按 `Shift + F10`

生成的 APK 位于 `app/build/outputs/apk/debug/` 目录下。

---

## 📸 界面预览

> 以下为原型设计截图，实际效果以发布版本为准。

| 首页 | 卡片复习 | 文档导入 |
|:---:|:---:|:---:|
| ![首页](docs/screenshots/home.png) | ![复习](docs/screenshots/review.png) | ![导入](docs/screenshots/import.png) |

| 社区 | 个人中心 |
|:---:|:---:|
| ![社区](docs/screenshots/community.png) | ![个人](docs/screenshots/profile.png) |

---

## 🎯 目标用户

- **在校学生**（大学生、考研/考公/考编群体）：每天需要记忆大量知识点
- **语言学习者**（英语、日语等）：需要大量词汇、语法点的反复记忆
- **职场人士**（医生、律师、程序员等）：学习时间碎片化，需要高效利用零散时间
- **专业考试备考者**（法考、医考、CPA 等）：考试资料体量大、知识点密集

---

## 📋 使用场景

- 将课堂笔记（Markdown）导入 App，自动生成问答卡片，等公交时随手复习
- 将 PDF 电子教材导入，App 自动提取段落和关键词生成词汇卡片
- 将多年积累的 Word/Excel 考点整理导入，App 按遗忘曲线规划每日复习任务

---

## 📄 开源协议

本项目采用 **MIT License** 开源协议。详见 [LICENSE](LICENSE) 文件。

---

## 👥 贡献

欢迎提交 Issue 和 Pull Request！

---

<p align="center">
  <b>让记忆更科学，让学习更高效</b> 🧠
</p>