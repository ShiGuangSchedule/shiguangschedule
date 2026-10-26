# 华北电力大学（保定）本科课表适配

本适配参考工作区 ClassTable 的 `NcepuBaodingAdapter`、`web_dom_extractor.dart`、`WeekRule` 和默认五大节作息，移植为拾光课程表的内置学校入口与 JavaScript 导入脚本。适用于保定校区本科教务系统的“学生课表查询”页面；北京校区及研究生系统尚未验证。

## 使用方式

1. 使用包含本次改动的 Android 构建，在教务导入的本科／专科学校列表搜索“华北电力大学”或 `HBDLDXBD`，选择“华北电力大学（保定）”。
2. 选择唯一入口“华北电力大学（保定）教务系统”，初始网页为用户指定的[统一身份认证登录页](https://ids.ncepu.edu.cn/?timestamp=1793009307516&service=https%3A%2F%2Fjwxt.ncepu.edu.cn%2FLogon.do%3Fmethod%3DlogonByHbdldx)。需先接入校园网或 WebVPN。
3. 自行完成统一身份认证，进入教务系统 → 学生课表查询，选择所需学期，显示**整个学期**的完整课表。适配不会代输密码或绕过登录；不要在单周筛选结果上导入，否则只能读取当时显示的课程。
4. 点击“执行导入”，选择目标课表。建议先新建一个课表，以便保留原有课表。
5. 输入学期总周数、首周周一日期，核对确认框中的课程安排数量、作息和未排课备注后确认。首周周一留空会清空目标课表的开学日期，之后需要到课表设置中填写。
6. 导入会替换目标课表中的课程、学期配置和专属作息；核对星期、周次、教师、教室及第一周日期后使用。

如果只显示简版列表，可通过网页右上角菜单切换桌面模式，再重新查询完整课表。

当前上游 Desktop 的网页视图仍是占位实现，因此本次不宣称支持 Windows 应用内登录导入。脚本测试在 Node DOM 环境运行，不等于手机实测。

## 作息与解析规则

沿用 ClassTable 当前的五大节模型，每条课程安排占一个大节：

| 大节 | 开始 | 结束 |
| --- | --- | --- |
| 第一大节 | 08:00 | 09:40 |
| 第二大节 | 10:00 | 11:40 |
| 第三大节 | 14:30 | 16:10 |
| 第四大节 | 16:30 | 18:10 |
| 第五大节 | 19:30 | 21:10 |

这些时间来自工作区已有配置，不是本次查询到的学校最新作息。若学校实行其他季节作息，请在导入后修改专属作息表；不会根据月份推测作息或开学日期。

- 检查顶层文档及最多三层同源 `iframe/frame`，只接受 `ncepu.edu.cn` 或其子域名上具有课表结构的页面。跨域框架无法直接读取时，提示用户打开实际课表页。
- 根据“第一大节”至“第五大节”的表头确定节次，根据七个数据格的顺序确定周一至周日。
- 优先读取 `.kbcontent`，主节点没有内容时回退到 `.kbcontent1`；兼容 `td` 自身是课程节点的结构。
- 使用 `<br>` 与至少五个连字符的分隔行拆分同格多门课程；提取 `老师`、`周次(节次)`、`教室` 标题字段，过滤课程编号、课堂派和分组名称。
- 支持连续周、离散周、单双周、中英文括号及常见分隔符，例如 `1-16(周)`、`1-15周(单)`、`2,4,6周`。
- 完全相同的安排去重；教师、教室、周次不同的安排保留。颜色由拾光课程表原有导入逻辑分配。
- 未排课备注在确认框显示，不生成虚构时间的课程，也不会持久化为课程备注，需要的内容请另行记录。
- 缺失／非法周次、缺少课程名称、空课表会中止导入，避免用部分解析结果覆盖完整课表。没有识别过的节次标注格式会报错，不能自动猜测。

## 接入与维护

| 文件 | 用途 |
| --- | --- |
| `shared/src/commonMain/composeResources/files/adapters/ncepu_baoding.js` | 实际运行的解析器与桥接导入流程，亦可供外部适配脚本仓库采用 |
| `shared/src/commonMain/kotlin/com/xingheyuzhuan/shiguangschedule/data/repository/BuiltInSchoolAdapters.kt` | 内置学校元数据、统一教务入口和打包脚本读取 |
| `shared/src/commonMain/kotlin/com/xingheyuzhuan/shiguangschedule/data/repository/SchoolRepository.kt` | 将内置学校合并到离线／在线学校索引 |
| `shared/src/commonMain/kotlin/com/xingheyuzhuan/shiguangschedule/ui/schoolselection/list/AdapterSelectionScreen.kt` | 解析内置／远程适配器路径，保留远程扩展适配器的资源目录 |
| `shared/src/commonMain/kotlin/com/xingheyuzhuan/shiguangschedule/ui/schoolselection/web/WebViewScreen.kt` | 优先读取内置脚本，其他适配仍走原有本地脚本加载路径 |
| `shared/src/commonMain/kotlin/com/xingheyuzhuan/shiguangschedule/ui/schoolselection/web/WebDialogHost.kt` | 长说明支持滚动，连续输入框按标题与默认值重置输入和错误状态 |
| `tests/ncepu_baoding/` | 脱敏 HTML 与 JavaScript 回归测试 |
| `shared/src/commonTest/kotlin/com/xingheyuzhuan/shiguangschedule/data/repository/BuiltInSchoolAdaptersTest.kt` | 学校合并、入口路径、在线同 ID 冲突回归测试 |

学校 ID 为 `NCEPU_BAODING`；统一入口保留 `NCEPU_BAODING_PORTAL` 适配器 ID，脚本路径为 `builtin/ncepu_baoding.js`。旧 `_WEBVPN`、`_CAMPUS` 入口已移除，下载的旧索引也不会恢复这两个选项。学校入口不依赖修改二进制 Protobuf 索引；即使索引缺失或在线更新，内置入口仍保留。同一学校下的其他远程适配器会保留，内置同 ID 适配器优先使用随应用发布的版本。

入口说明按用户指定原文显示：“适配华北电力大学（保定）教务系统，需校园网或WebVPN访问。登陆后切换到课表页面，点击导入即可。”初始 URL 保留用户提供的 `timestamp` 和教务系统 `service` 参数。

课程读取在 WebView 内直接执行，不需要 ClassTable 原先的 Base64 HTML 传输层。脚本不发送额外网络请求，不读取账号、密码或 Cookie，不上传或保存原始 HTML。学校登录页面本身仍会通信，WebView 也可能保留登录会话与缓存，这与解析脚本无关。

写入顺序为 `savePresetTimeSlots` → `saveCourseConfig` → `saveImportedCourses`，每一步等待原生成功回调；全部成功后才调用 `notifyTaskCompletion`。原有桥接接口的三个保存操作没有统一事务，因此保存失败可能留下部分设置。脚本会明确提示，并停止后续步骤；请核对目标课表并重新导入。输入框和最终确认取消时不会写入课程或配置。

## 开发验证

需要 Node.js 22 或更新版本、Java 21 工具链和项目要求的 Android SDK（当前为 API 37）。在仓库根目录执行：

```powershell
npm.cmd ci --ignore-scripts
npm.cmd run format:check
npm.cmd test
node --check shared/src/commonMain/composeResources/files/adapters/ncepu_baoding.js
git diff --check
powershell -File tool/build_android.ps1
```

Node 开发依赖仅用于测试和格式化，不进入 Android 应用。脱敏样例复制自 ClassTable 的 `test/fixtures/ncepu_timetable_sanitized.html`，保留虚构课程、教师、教室和备注，不包含学生信息或认证数据。

### Windows 构建环境

`tool/build_android.ps1` 默认运行完整共享模块测试和 Android Debug 构建。它使用 Java 21，并将 Gradle 缓存、Android 用户目录和系统临时文件放入项目的 `.local-build/`。该目录已经加入忽略规则，执行 Gradle `clean` 不会删掉这里的工具链。

脚本优先使用环境中的 Java 21，或者本项目已经准备好的 Java 21 便携版；可通过 `-JavaHome` 指定其他 Java 21 安装。Android SDK 来自 `-AndroidSdk`、环境变量或本机 `local.properties`。首次运行会复制已有的 Gradle 依赖缓存及发行版，排除锁文件；这种缓存复用方式参考 [Gradle 官方说明](https://docs.gradle.org/current/userguide/dependency_caching.html#sec:copying_cache)。

下载使用 `HTTPS_PROXY` 或 `HTTP_PROXY` 中的 HTTP 代理设置。脚本把这些设置传递给 Gradle；Gradle Wrapper 的下载超时调整为 60 秒，并允许重试三次。代理地址来自当前环境，不在仓库中写死或保存认证信息。执行结束后会恢复进程原来的环境变量。

依赖齐全后可运行：

```powershell
powershell -File tool/build_android.ps1 -Offline
```

这会让 Gradle 使用已有依赖缓存。也可通过 `-Tasks` 单独选择构建任务，例如 `-Tasks ':shared:jvmTest'`。生成的安装包在 `androidApp/build/outputs/apk/debug/`，不要提交 APK、`.local-build/` 或本机 `local.properties`。

本次验证结果（2026-10-26）：

- 适配开发阶段的脚本回归测试 43 项通过；JavaScript 格式、语法、Git 差异检查及新增 Kotlin 文件格式检查通过。
- 使用构建脚本运行完整 `:shared:jvmTest` 和 `:androidApp:assembleDebug`，结果为 `BUILD SUCCESSFUL`，共执行 81 个任务。
- 完整 Gradle 测试四项通过，失败与错误均为零，覆盖无在线索引、统一入口的登录 URL、远程资源保留、内置适配器优先级，以及旧索引不能恢复已删除入口的行为。
- 添加 `-Offline` 后再次构建成功，确认 Gradle 所需依赖缓存完整。启动脚本在成功与报错退出两种情况下，均已验证能恢复原有环境变量。
- 生成 `arm64-v8a`、`armeabi-v7a` 和 `x86_64` 三个 Debug APK，均通过 APK 签名验证；包内华电脚本的 SHA-256 与被测试的源码一致。
- 本次统一入口改动已重新构建，并在 ARM64 APK 中核对指定标题、说明及完整初始 URL；旧的三个入口标题已不再出现在应用代码中。

此前的环境故障已解决：对照实验确认 Java 21 可写入项目目录，但无法在原全局 Gradle 缓存生成 `module-info.class`；数据库编译工具也无法写入原系统临时目录。构建脚本将这些写入迁入 `.local-build/`，并显式传递代理设置。此前缺失的 `graphics-shapes-desktop` 依赖已成功下载并按官方 SHA-1 校验，完整 Gradle 测试已经实际运行。

当前 Java 21 便携工具链位于 `.local-build/jdk21/`，原始下载包已按官方 SHA-256 校验；Android API 37 已安装。Gradle Wrapper 版本和依赖版本保持上游配置，尚未发布正式版本或推送 GitHub。用户已反馈此前版本手机验证可以正常工作。

本地生成的安装包：

| 架构 | 文件 |
| --- | --- |
| ARM 64 位 | `androidApp/build/outputs/apk/debug/shiguangschedule-v2.1.0-arm64-v8a-debug.apk` |
| ARM 32 位 | `androidApp/build/outputs/apk/debug/shiguangschedule-v2.1.0-armeabi-v7a-debug.apk` |
| x86 64 位 | `androidApp/build/outputs/apk/debug/shiguangschedule-v2.1.0-x86_64-debug.apk` |

手机验证：用户已反馈此前版本的导入功能正常；本次统一入口和新初始网页仍待手机复核。网络中断重试及小屏／大字体等场景尚无逐项验收记录，可继续补充。面向用户的说明与确认弹窗不展示实现来源。学校页面结构改变时，先新增脱敏复现样例，再调整解析规则。
