# 内置教务适配（不依赖适配仓库）

本文说明本仓库新增的「App 内置教务适配」机制，以及首个内置适配 **西北农林科技大学（NWAFU）
ehall 新版教务 →「我的课表」** 的实现方式。适配脚本本身严格按官方
《适配脚本开发指南 v2》与适配仓库的目录/字段规范编写，另附一份可直接提 PR 的适配包。

> 背景：拾光课程表的教务导入依赖 **shiguang_warehouse 适配仓库**（`index/school_index.pb`
> + `resources/<学校>/adapters.yaml` + `*.js`）。仓库同步会把本地索引整份覆盖，因此「内置」
> 必须满足两点：① 首次安装即可用；② 适配仓库同步后也不会被冲掉。

---

## 1. 这次内置了什么

| 项目 | 值 |
| :--- | :--- |
| 学校 | 西北农林科技大学（`NWAFU`，拼音首字母 `X`） |
| 适配器 | `NWAFU_WDKBBY_01` / ehall 新版教务（我的课表） |
| 导入入口（`import_url`） | `https://newehall.nwafu.edu.cn/`（**新版门户**，登录后自行进入「我的课表」再点导入） |
| 取数策略 | 接口优先 + 页面解析兜底 |
| 导入内容 | `courses`（名称/教师/教室/星期/起止节次/周次）+ `config.semesterTotalWeeks` |
| 暂不导入 | `timeSlots` / `comboSchedule`（作息时间）、调课/停课/补课 |

接口通道（与教务页面自身 `BH_UTILS.doSyncAjax` 等价的表单 POST）：

* `{ROOT}/sys/wdkbby/modules/jshkcb/dqxnxq.do` → 当前学年学期（`DM` / `MC`）
* `{ROOT}/sys/wdkbby/modules/xskcb/cxxszhxqkb.do`，参数 `XNXQDM=<学期代码>`
  （不传 `SKZC` 即返回整个学期，等同页面「学期课表」视图）
  * 行字段：`KCM` 课程名、`SKJS` 教师、`JASMC`/`JXLDM_DISPLAY`/`XXXQMC` 教室、
    `SKXQ` 星期、`KSJC`/`JSJC` 起止节次、`SKZC` 周次位串、`ZCMC` 周次文本

页面兜底：接口不可用时解析已渲染的 `.wut_tablex` 表格
（`td[data-role="item"]` + 内部 `.mtt_arrange_item`），必要时先点击「学期课表」切换视图。

### 关于域名迁移（2026 起）

学校已把教务/门户从 `ehall.nwafu.edu.cn` 迁到 **`newehall.nwafu.edu.cn`**（新门户为
`ywtb-portal`），旧域名的部分路径会 404，所以：

* `import_url` 只填新门户首页，不写死课表页地址，学生登录后自己进「我的课表」；
* 脚本**不写死域名**：接口根路径取当前页面的 `_JW_INIT_CONFIG.path`（取不到则回退 `/jwapp`），
  请求同源发出，因此新旧域名下都能用；实测 `https://newehall.nwafu.edu.cn/jwapp/sys/wdkbby/...`
  未登录会跳 `authserver.nwafu.edu.cn` 统一认证，说明接口路径仍然有效。

---

## 2. 与官方规范的对照

脚本按官方 wiki 的要求实现，逐条对照如下（官方依据：`如何适配教务v2`、`课程合并与去重函数`、
`一些数据的格式信息`、shiguang_warehouse README）。

| 官方要求 | 本脚本的实现 |
| :--- | :--- |
| 桥接对象为 `window.shiguangBridgePromise` / `window.shiguangBridge`（v2 命名） | 全部调用都带 `window.` 前缀，未使用已废弃的 `AndroidBridge*` |
| 课程字段：`name/teacher/position/day/startSection/endSection/weeks`，`color`/`remark` 提交无效 | 只提交上述字段，不提交 `color`/`remark`；无值的教师/教室传空串 |
| 配置字段：`config` 只需提交想改的字段 | 只提交 `semesterTotalWeeks` |
| 用顶层函数 + `runImportFlow()` 编排，用户取消/失败立即终止 | 顶层函数：`promptUserToStart` / `collectCourses` / `saveCourseConfigData` / `saveCourseList` / `runImportFlow`，末尾 `runImportFlow();` |
| `notifyTaskCompletion()` 只在**流程完全成功**后调用 | 仅在配置与课程都保存成功后调用 |
| 先确认再取数（`promptUserToStart`） | 第一步就是 `showAlert('导入确认', '…请确保已登录…')` |
| 保存顺序：`config` → `timeSlots` → `courses` | `saveCourseConfig` 先于 `saveImportedCourses`（不含 timeSlots，因为不导入作息） |
| 合并去重可直接使用官方 `mergeAndDistinctCourses` | 原样采用官方参考实现（连续节次合并 + 同节次周次合并 + 完全去重） |
| 教务页面显示异常时改用接口而不是抓 DOM | 接口是主通道，DOM 仅在接口失败时兜底，并在导入前弹窗告知 |
| `import_url` 必须是登录入口 | 填新门户首页 `https://newehall.nwafu.edu.cn/`（登录后自行进入「我的课表」），与仓库内其它适配（如 `JSEI`）一致 |

官方目录/字段规范的落实：`adapters.yaml` 七个字段全部提供、`category` 用
`BACHELOR_AND_ASSOCIATE`、`asset_js_path` 为脚本相对路径 —— 见
`tools/warehouse-submission/NWAFU/adapters.yaml`（含 `index/root_index.yaml` 的登记片段）。

---

## 3. 文件清单与职责

| 文件 | 职责 |
| :--- | :--- |
| `shared/src/commonMain/composeResources/files/builtin_NWAFU_wdkbby.js` | 适配脚本本体（内置资源，导入时由 App 写入运行目录后执行） |
| `shared/src/commonMain/composeResources/files/builtin_schools.pb` | 内置学校索引**片段**（仅 `SchoolIndex.schools` 字段，由脚本生成） |
| `tools/gen_builtin_schools.py` | 生成 `builtin_schools.pb`（学校/适配器元信息的唯一来源） |
| `tools/verify_builtin_schools.py` | 解码校验片段，并验证「本地索引 + 片段」拼接后的合并结果 |
| `tools/adapter-tests/run-test.mjs` | 适配脚本离线回归测试（jsdom） |
| `tools/warehouse-submission/` | 按官方仓库结构准备的 PR 提交包（`adapters.yaml` + `root_index` 片段 + 脚本副本 + 同步脚本） |
| `tools/gen_contributors.py` | 生成贡献者数据（CI 同名脚本的 Python 复刻；本地构建缺该资源时用） |
| `docs/submit-adapter.md` | 提交到官方适配仓库的完整流程 |
| `shared/.../tool/ResourceInitializerManager.kt` | 启动时把内置脚本按内容补齐到 `<filesDir>/repo/schools/resources/NWAFU/wdkbby.js` |
| `shared/.../data/repository/SchoolRepository.kt` | 读取学校索引时叠加内置片段，并按 `id` 去重 |
| `shared/.../proto/school_index.proto` | 索引结构定义（未修改） |

### 为什么脚本不放进 `offline_schools.zip`

`shared/assets/offline_repo` 打包出的 zip 只在**首次启动、且 `<filesDir>/repo/index` 不存在**时
解压一次。App 升级后新增/修改的内置脚本无法补进已有安装，因此内置脚本改为独立 compose 资源，
每次启动按内容补齐（幂等）。

### 索引片段为什么要「字节拼接」

`builtin_schools.pb` 只包含 `SchoolIndex.schools`（字段 3）的编码。`SchoolRepository.loadIndex()`
读取本地索引字节后直接**追加**该片段再交给 Wire 解码：

```kotlin
val mergedSource = Buffer()
localBytes?.let { mergedSource.write(it) }
builtinBytes?.let { mergedSource.write(it) }
SchoolIndex.ADAPTER.decode(mergedSource)
```

按 Protobuf 规范，repeated 字段重复出现即视为追加，因此：

* 学校列表 = 本地（适配仓库）索引里的学校 + 内置学校；
* 本地索引的 `protocol_version` / `version_id`（字段 1/2）保持不变，不影响 `GitUpdater` 的版本比较；
* 适配仓库同步覆盖索引后，内置学校依旧存在；
* 若适配仓库里已有同 `id` 学校，读取时 `distinctBy { it.id }` 保留**仓库那份**（内置仅作兜底）。

---

## 4. 新增一个内置学校（步骤）

1. 写适配脚本，放到
   `shared/src/commonMain/composeResources/files/builtin_<学校代码>_<标识>.js`
   （按官方 v2 规范：顶层函数 + `runImportFlow()` + 末尾启动）。
2. 在 `ResourceInitializerManager.builtinResourceMap` 增加一条映射：
   `资源路径` → `schools/resources/<resourceFolder>/<asset_js_path>`。
3. 在 `tools/gen_builtin_schools.py` 的 `SCHOOLS` 里补充学校/适配器条目，然后运行：
   `python tools/gen_builtin_schools.py`（覆盖生成 `builtin_schools.pb`）。
4. 校验与回归：
   `python tools/verify_builtin_schools.py`
   `node tools/adapter-tests/run-test.mjs unit|api|dom`（见 `tools/adapter-tests/README.md`）。
5. 如需上缴官方适配仓库：`python tools/warehouse-submission/sync.py` 后在 `tools/warehouse-submission/`
   目录按 README 提交 PR。

> `resource_folder`、`asset_js_path`、`tools/gen_builtin_schools.py` 中的元信息、以及
> `ResourceInitializerManager.builtinResourceMap` 的目标路径必须完全一致，否则导入时会提示
> 「未找到脚本文件」。

---

## 5. 已知边界与后续可做

* **调课/停课/补课**：教务 `xsdkkc.do`（行内 `ISTK` 标记）未处理，导入的是基础课表；
  页面上「请注意右侧调课信息」的调课不会体现。
* **作息时间**：未调用 `savePresetTimeSlots` / `saveComboSchedule`，节次时间沿用 App 默认；
  教务的 `cxjcs.do` 可拿到上午/下午/晚上的节次时间，后续可补（补了就能同时给出 `timeSlots`）。
* **页面兜底模式**：只能导入页面已渲染的课程；学期总周数取「已渲染课程的最大周次」，
  可能与教务实际周数略有偏差（接口通道用 `SKZC` 位串长度，是准确的）。
* **页面兜底需停留在「我的课表」**：`import_url` 是登录门户，登录后需进入「我的课表」页，
  接口通道则不限页面。

---

## 6. 验证记录（本次改动）

* `node tools/adapter-tests/run-test.mjs unit` → 18 项全部通过
  （周次文本/位串、单元格信息解析、官方合并函数的周次合并/连续节次合并/去重）
* `node tools/adapter-tests/run-test.mjs api` → 全部通过
  （单双周、周次位串、教师去重、脏数据过滤、`XNXQDM` 请求参数、`credentials: include`、
  保存顺序 `config → courses`、学期周数=22）
* `node tools/adapter-tests/run-test.mjs dom "class/网页/我的课表.html"` → 全部通过
  （真实存档页面解析出 34 门课程；`4周` + `6-18周` 合并为 `4,6..18`；单周班/双周班合并为
  `4,6..18`；教室字段不再混入 `(单)/(双)` 与周次）
* `python tools/verify_builtin_schools.py` → 拼接后 `version_id` 保持本地值，
  学校列表同时包含 `GLOBAL_TOOLS` 与 `NWAFU`
* `python tools/warehouse-submission/sync.py --check` → 提交包脚本与内置脚本一致
* 域名探测（2026/10）：`https://newehall.nwafu.edu.cn/` → 200（跳新门户 `/ywtb-portal/...`）；
  `/jwapp/sys/wdkbby/modules/{jshkcb/dqxnxq,xskcb/cxxszhxqkb}.do` → 未登录跳
  `authserver.nwafu.edu.cn` 统一认证（说明接口路径在新域名下仍有效，不是 404）

> 编译状态：`SchoolRepository` / `ResourceInitializerManager` 的改动已由一次本地
> `:androidApp:assembleDebug` 成功构建验证（APK 内可见 `builtin_NWAFU_wdkbby.js` 与
> `builtin_schools.pb`）；`ContributionRepository` 的贡献者兜底是在那次构建之后改的，
> 请再构建一次确认。
