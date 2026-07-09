# XX Memory 项目交接文档

## 一、项目基本信息
- **项目名称**: xx memory（通用智能记忆助手）
- **项目路径**: `c:\Users\xx\Desktop\xx memory`
- **GitHub 仓库**: https://github.com/xx8888888-xh/xx-memory
- **分支策略**: main（主分支）、review/对抗式审查修复（审查分支，无价值后会被 main 覆盖）

## 二、用户硬性要求与规则（必须遵守）
1. **GitHub 交付规则**: 任务完成后代码验证无误，必须自动 commit 并 push 到 GitHub，再结束任务。
2. **review 分支处理规则**: 检查 review 分支 bug 确认全部修复后，必须用 main 分支覆盖 review 分支（`git push origin main:review/对抗式审查修复 --force`）。
3. **技术自主权**: 用户不干预技术路线，AI 自行选择并验证最终效果。
4. **代码风格**: 尽量最小化修改，优先在原有实现上工作。
5. **不中途停止**: 任务未完成前不能停止，直到完全完成并通过测试。
6. **并行子 Agent**: 尽量使用多个并行子 Agent 提高效率。
7. **真机测试**: 所有功能必须通过 Android 模拟器真机测试验证。

## 三、开发环境配置
- **Android Studio**: `D:\software\android studio`
- **Android SDK**: `C:\Android\Sdk`
- **JDK**: `D:\software\android studio\jbr` (OpenJDK 21.0.10)
- **Gradle**: 使用缓存 `C:\Users\xx\.gradle\wrapper\dists\gradle-8.14.4-bin\7h43i68gcl63g7h891b1077u3\`
- **环境变量**（PowerShell 中设置）:
  ```powershell
  $env:JAVA_HOME = "D:\software\android studio\jbr"
  $env:ANDROID_HOME = "C:\Android\Sdk"
  $env:ANDROID_SDK_ROOT = "C:\Android\Sdk"
  ```
- **模拟器**: AVD 名称 `Clock3_Test_Device` (API 34, x86_64)
  - 模拟器路径: `C:\Android\Sdk\emulator\emulator.exe`
  - 系统镜像: `C:\Android\Sdk\system-images\android-34\default\x86_64`
- **ADB 路径**: `C:\Android\Sdk\platform-tools\adb.exe`

## 四、项目技术架构
- **语言**: Kotlin
- **UI 框架**: Jetpack Compose (Material 3)
- **数据库**: Room (SQLite)，数据库名 `xx_memory_database`，版本 2
- **架构模式**: MVVM（ViewModel + Repository + DAO）
- **依赖注入**: 无，使用单例模式（XxMemoryApplication.instance）
- **后台任务**: AlarmManager + BroadcastReceiver (AlarmReceiver, BootReceiver)
- **状态管理**: Kotlin Flow / StateFlow
- **协程**: GlobalScope.launch(Dispatchers.IO) 用于后台任务

### 核心模块
| 模块 | 路径 |
|------|------|
| 数据层 | `android/app/src/main/java/com/xxmemory/app/data/` |
| 实体类 | `.../data/entity/` (Card, ReviewLog, WeeklyProgress) |
| DAO | `.../data/dao/` (CardDao, ReviewLogDao) |
| 数据库 | `.../data/AppDatabase.kt` |
| Repository | `.../data/repository/CardRepository.kt` |
| 业务逻辑 | `.../domain/` (Scheduler, NotificationScheduler, 算法) |
| 解析器 | `.../domain/parser/` (DocumentParser, MarkdownParser, CsvParser, TxtParser) |
| UI 层 | `.../ui/` (home, import, review, settings, statistics, navigation, theme) |
| 通知 | `.../notification/` (AlarmReceiver, NotificationHelper, BootReceiver) |

### 记忆曲线算法
- **SM-2**: 经典间隔重复，quality 映射 0-3 → 0-5
- **艾宾浩斯固定**: 1/2/4/7/15/30/60/180 天
- **FSRS v4**: 完整三参数模型 (S/D/R)，从 easeFactor 恢复 difficulty
- 算法统一接口: `MemoryAlgorithm`，内部定义 `ScheduleResult`

## 五、整机测试方法（必须执行）

### 1. 构建 APK
```powershell
cd "c:\Users\xx\Desktop\xx memory\android"
$env:JAVA_HOME = "D:\software\android studio\jbr"
$env:ANDROID_HOME = "C:\Android\Sdk"
$env:ANDROID_SDK_ROOT = "C:\Android\Sdk"
.\gradlew.bat assembleDebug
```
- APK 输出路径: `android/app/build/outputs/apk/debug/app-debug.apk`

### 2. 安装到模拟器
```powershell
C:\Android\Sdk\platform-tools\adb.exe install -r "c:\Users\xx\Desktop\xx memory\android\app\build\outputs\apk\debug\app-debug.apk"
```

### 3. 启动应用
```powershell
C:\Android\Sdk\platform-tools\adb.exe shell am start -n com.xxmemory.app/.MainActivity
```

### 4. 获取 UI 层次结构（验证页面状态）
```powershell
C:\Android\Sdk\platform-tools\adb.exe shell uiautomator dump /sdcard/window_dump.xml
C:\Android\Sdk\platform-tools\adb.exe pull /sdcard/window_dump.xml "c:\Users\xx\Desktop\xx memory\ui_dump.xml"
```

### 5. 模拟点击（坐标基于 1080x2400 屏幕）
```powershell
# 底部导航坐标（约）
主页: 100 2150
导入: 320 2150
复习: 540 2150
统计: 750 2150
设置: 950 2150

# 通用点击
C:\Android\Sdk\platform-tools\adb.exe shell input tap <x> <y>

# 滑动
C:\Android\Sdk\platform-tools\adb.exe shell input swipe <x1> <y1> <x2> <y2> <duration_ms>
```

### 6. 测试检查清单（每次修改后必须验证）
- [ ] 主页显示正常（总卡片、今日复习、待复习、进度、本周学习、学科筛选）
- [ ] 导入页面正常（文件导入、URL 导入、手动添加、AI 导入、格式 Chips）
- [ ] 手动添加卡片对话框（问题/答案/科目/详细说明/卡片类型选择）
- [ ] 复习流程（翻转卡片 → 评分 → 算法更新 → 完成）
- [ ] 算法切换（SM-2 / 艾宾浩斯固定 / FSRS）
- [ ] 统计页面（连续学习、总复习、总卡片、本周趋势、科目掌握度）
- [ ] 设置页面（每日卡片限制 Slider、随机顺序、先显示详细说明、算法选择、墨水屏模式、自动朗读、每日提醒）
- [ ] 墨水屏模式开关及持久化（开启后界面变为黑白极简）
- [ ] 标签/收藏功能
- [ ] 多类型卡片（问答/填空/代码/图片/音频）

## 六、Git 工作流程
1. **提交前**: 确保所有修改已验证，构建成功，测试通过
2. **提交命令**:
   ```bash
   git add -A
   git commit -m "描述"
   git push origin main
   ```
3. **review 分支处理**:
   - 审查 review 分支与 main 的差异
   - 确认所有 bug 已修复后，用 main 覆盖 review:
     ```bash
     git push origin main:review/对抗式审查修复 --force
     ```
4. **禁止操作**: 不要运行 `git push --force` 到 main/master（除非用户明确要求）

## 七、关键配置文件
- `android/app/build.gradle.kts`: 依赖管理、isMinifyEnabled=true
- `android/app/src/main/AndroidManifest.xml`: allowBackup=false、权限声明
- `android/app/proguard-rules.pro`: Room Entity 类保持规则
- `.gitignore`: 已追加 APK/AAB/签名密钥/截图等排除规则

## 八、已知限制与注意事项
1. **CardDao 使用 ABORT**: 不再是 REPLACE，重复插入会失败，不会级联删除复习日志
2. **NotificationScheduler 使用精确闹钟**: setExactAndAllowWhileIdle，需要 BOOT_COMPLETED 重新注册
3. **AlarmReceiver 使用 goAsync()**: onReceive 后协程有 10 秒 WakeLock 保护
4. **ImportViewModel 多格式导入**: 支持 md/markdown/csv/txt，使用 org.commonmark 和 opencsv
5. **Build.VERSION_CODES.TIRAMISU (API 33+)**: 通知权限检查必需
6. **Gradle 8.14.4**: 不要修改为其他版本，本地仅缓存了此版本

## 九、长久记忆文件位置
- 用户画像: `c:\Users\xx\.trae-cn\memory\user_profile.md`
- 项目记忆: `c:\Users\xx\.trae-cn\memory\projects\-c-Users-xx-Desktop-xx-memory\project_memory.md`
- 近期话题: `c:\Users\xx\.trae-cn\memory\projects\-c-Users-xx-Desktop-xx-memory\20260709\topics.md`
