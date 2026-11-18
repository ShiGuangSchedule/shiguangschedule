# 拾光课程表 (ShiGuangSchedule) 电脑版完善与优化计划 (PLAN)

## 1. 背景与现状分析

通过对仓库代码 (`shared`、`desktopApp`) 以及本机已安装版本（约 **142.42 MB**，其中 `runtime` 占 **74.01 MB**、`app` 依赖占 **67.90 MB**）的全面审计，当前电脑版（Desktop / JVM）仍处于实验性阶段，存在以下核心短板：

1. **桌面端核心功能缺失或空实现**：
   - **教务系统网页导入 (`WebView.jvm.kt`)**：仅显示“桌面端网页功能暂未实现”占位文本，未提供任何桌面端可用的导入桥接方案。
   - **背景图片裁剪 (`ImageCropper.jvm.kt` & `ImageCropper.kt`)**：桌面端禁用了裁剪预览窗口 (`isCropWindowEnabled` 仅对移动端启用)，且 `cropImageBitmapNative` 直接忽略裁剪坐标返回原图。
   - **系统日历同步 (`CalendarAccountManager.jvm.kt`)**：直接硬编码返回 `false`，无法将课表同步至桌面系统日历（如 Windows 日历 / Outlook / Apple Calendar）。
   - **文件导出后打开/分享 (`ShareManager.jvm.kt` & `ShareManager.kt`)**：`platformShareFile` 为空实现，导出 `.json` / `.ics` 后无法一键定位或打开所在目录。
   - **版本更新直链解析 (`UpdateTool.jvm.kt`)**：`parseTargetUrl` 直接返回 `null`，无法根据桌面系统 (Windows/macOS/Linux) 与架构 (x64/arm64) 匹配对应的安装包下载直链。
   - **文件拖拽无视觉反馈 (`main.kt`)**：虽然 `DesktopFileDropState` 记录了 `isDraggingOver` 状态，但主窗口未渲染拖拽悬停覆盖层提示。

2. **电脑版触控与键鼠/触控板多模态交互体验不足**：
   - **周课表网格触控热区偏小 (`ScheduleGridComponents.kt`)**：课程块长按进入编辑态后的上下拉伸手柄 (`CourseEditHandles`) 仅 `32.dp`（视觉圆点 `12.dp`），在 Windows 触控屏/二合一平板上极易误触或漏触。
   - **周次切换方式单一 (`WeeklyScheduleScreen.kt`)**：仅依赖 `HorizontalPager` 水平滑动切换周次，缺少顶部显式的“上一周 / 下一周”触控按钮、“回到本周”快捷胶囊按钮，以及电脑端方向键 (`←`/`→`)、快捷键 (`Home`/`T`/`Esc`) 与鼠标滚轮/触控板横向滚动支持。
   - **滚轮选择器交互受限 (`NumberPicker.kt`)**：`NativeNumberPicker` 仅支持拖拽滚动，点击中心上下的相邻候选项时无响应，在触控屏和鼠标点击场景下不够直观。
   - **多课程重叠详情弹窗 (`CourseDetailBottomSheet.kt`)**：同格多门课程切换缺少桌面/触控友好的左右翻页按钮，底部分页指示器触控热区较小。

3. **构建工具链僵化与安装包体积过大**：
   - **工具链锁定 (`desktopApp/build.gradle.kts`)**：强制锁定 `jvmToolchain(21)` 且未配置自动解析仓库，在 JDK 25 等更高版本环境下无法直接编译；同时未对无 Android SDK 环境做模块按需加载容错。
   - **代码压缩未启用 & 冗余资源打包**：Release 构建中 `proguard.isEnabled` 被设为 `false`；`modules("jdk.localedata")` 引入了近 30MB 的冗余多语言区域数据；`sqlite-bundled-jvm` 等依赖在单平台分发包中混入了其他操作系统与架构的原生动态库；缺少 `jlink` 深度裁剪参数 (`--strip-debug`, `--no-man-pages`, `--no-header-files`, `--compress=zip-6`) 与精简打包任务。

---

## 2. 计划目标 (Goals & Milestones)

本计划遵循**“每完成一个修改或功能即通过测试验证并进行 Git 版本管理”**的原则，分阶段推进以下目标：

### 阶段一：构建工具链优化与减小安装包体积 / 性能提升 (`build` / `perf`)
- [x] **M1.1 构建系统与多环境兼容优化**：
  - 将 `desktopApp` 的 `jvmToolchain(21)` 改为兼容任意 JDK 21+ 编译器的 `JvmTarget.JVM_21` 字节码目标。
  - 在 `settings.gradle.kts` 与 `shared/build.gradle.kts` 中支持在缺失 Android SDK 的纯桌面开发环境下自动跳过 Android Target，提升桌面版编译速度与独立构建能力。
- [ ] **M1.2 减小安装包体积与运行时性能优化**：
  - 移除体积庞大的 `jdk.localedata` 模块，精简 JRE 仅保留必要模块 (`java.instrument`, `java.management`, `java.naming`, `java.prefs`, `java.sql`, `jdk.httpserver`, `jdk.unsupported`)。
  - 配置 ProGuard 压缩优化规则 (`desktopApp/proguard-rules.pro`) 并支持 Release 字节码瘦身。
  - 新增桌面端依赖瘦身与精简打包任务 (`stripForeignNativeLibs` / `packageCompactDesktop`)，自动剔除 JAR 包中非当前系统/架构的原生二进制文件（如非当前平台的 SQLite / Skiko 原生库），并注入紧凑对象头与字符串去重 JVM 参数 (`-XX:+UseCompactObjectHeaders`, `-XX:+UseStringDeduplication`, `-Xshare:auto`)。

### 阶段二：优化电脑版触控体验与多模态交互 (`feat` / `ux`)
- [ ] **M2.1 课表网格触控与拖拽交互优化 (`ScheduleGrid.kt` & `ScheduleGridComponents.kt`)**：
  - 扩大 `CourseEditHandles` 触控热区至 `48.dp` 并升级为高辨识度胶囊/圆点手柄，降低 Windows 触控屏拉伸课程时长时的操作难度。
  - 优化拖拽边缘自动滚动逻辑：采用基于边缘距离的平滑比例滚动速度，提升触控拖拽课程到屏幕上下边缘时的顺滑度。
- [ ] **M2.2 周课表顶栏触控导航、回到本周与键盘快捷键 (`WeeklyScheduleScreen.kt`)**：
  - 在周课表顶部栏增加触控友好的“上一周 (`<`)”、“下一周 (`>`)”切换按钮，以及非本周时的“回到本周”快捷按钮。
  - 增加桌面端键盘快捷键支持：`←` / `→` 切换周次，`Home` / `T` 回到本周，`Esc` 退出长按编辑或跨周悬浮模式。
- [ ] **M2.3 滚轮选择器与课程详情弹窗触控优化 (`NumberPicker.kt` & `CourseDetailBottomSheet.kt`)**：
  - 为 `NativeNumberPicker` 增加上下候选项点击直达（Tap-to-Scroll）与鼠标滚轮步进能力。
  - 为 `CourseDetailBottomSheet` 多课程卡片增加左右切换按钮并扩大底部分页指示器触控热区。

### 阶段三：完善课程表电脑版核心功能 (`feat`)
- [ ] **M3.1 桌面端原生图片裁剪与缩放支持 (`ImageCropper.jvm.kt` & `ImageCropper.kt`)**：
  - 使用 Skia `Bitmap.extractSubset` 实现真实的桌面端壁纸物理区域裁剪与边界保护。
  - 在桌面端启用可视化裁剪窗口 (`isCropWindowEnabled = true`)，同时支持触控双指缩放、鼠标滚轮缩放及拖拽平移。
- [ ] **M3.2 桌面端文件定位分享、系统日历同步与更新直链匹配 (`ShareManager.jvm.kt`, `CalendarAccountManager.jvm.kt`, `UpdateTool.jvm.kt`, `main.kt`)**：
  - 实现 `ShareManager.jvm.kt` 在系统资源管理器中定位/打开导出的文件，并在桌面端导出完成后提供快捷打开入口。
  - 实现 `CalendarAccountManager.jvm.kt` 生成标准 `.ics` 日历文件并调用系统默认日历应用一键导入。
  - 实现 `UpdateTool.jvm.kt` 根据当前操作系统与 CPU 架构自动匹配 Release 安装包下载直链。
  - 在 `main.kt` 中为 `.json` 课表文件拖拽入窗提供可视化半透明引导浮层。
- [ ] **M3.3 桌面端教务系统本地浏览器桥接导入 (`WebView.jvm.kt`)**：
  - 替换 `WebView.jvm.kt` 的空白占位，实现零体积膨胀的“桌面端教务导入助手”：内置本地回环 HTTP 桥接服务 (`127.0.0.1`) 对接 `WebBridgeHandler`，支持一键打开教务网页、自动生成并复制带 Bridge 垫片的导入脚本到剪贴板，以及手动粘贴 JSON 数据直接导入。

### 阶段四：单元测试与集成验证 (`test`)
- [ ] **M4.1 核心逻辑与桌面端新增功能自动化测试**：
  - 编写并运行针对桌面端更新包匹配、图片裁剪边界计算、周次格式化、课程合并与桌面桥接协议解析的单元测试，确保所有改动经过真实测试验证。
