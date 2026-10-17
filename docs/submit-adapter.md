# 把这份适配提交到官方仓库（按官方流程）

官方依据：`shiguang_warehouse` README（仓库结构 / 字段规范 / 开发流程）+ wiki
《如何适配教务 v2》。本仓库已把该交的东西放在
[`tools/warehouse-submission/`](tools/warehouse-submission/)，复制即可。

---

## 0. 一句话流程

```
Fork 适配仓库 → 在 index/root_index.yaml 登记学校 → 建 resources/NWAFU/（adapters.yaml + wdkbby.js）
→ 自测 → 向 shiguang_warehouse 的 pending 分支提 PR
```

> 目标分支是 **`pending`**，不是 `main`。`main` 开了分支保护，
> 只接受来自本仓库 `pending` 分支的合并（见 `.github/workflows/check-pr-source.yml`）。

---

## 1. 需要提交的文件（3 处）

### ① `index/root_index.yaml`：登记学校

在 `schools:` 列表里按 `id` 排序插入：

```yaml
  - id: "NWAFU"
    name: "西北农林科技大学"
    initial: "X"
    resource_folder: "NWAFU"
```

（现成片段：`tools/warehouse-submission/NWAFU/root_index.snippet.yaml`）

### ② `resources/NWAFU/adapters.yaml`：适配器配置（7 个字段全必填）

```yaml
adapters:
  - adapter_id: "NWAFU_WDKBBY_01"
    adapter_name: "ehall 新版教务（我的课表）"
    category: "BACHELOR_AND_ASSOCIATE"
    asset_js_path: "wdkbby.js"
    import_url: "https://newehall.nwafu.edu.cn/"
    maintainer: "内置适配"
    description: "西北农林科技大学新版门户（newehall）ehall 教务「我的课表」：登录新门户后进入「我的课表」，再点击执行导入。接口优先、页面解析兜底，导入课程与学期总周数（暂不含作息时间与调课）。"
```

要点：`import_url` **必须是登录入口**（这里是新门户首页，旧 `ehall.nwafu.edu.cn` 的路径已 404）；
`category` 只能是 `BACHELOR_AND_ASSOCIATE` / `POSTGRADUATE` / `GENERAL_TOOL`；字段不要增删。

### ③ `resources/NWAFU/wdkbby.js`：适配脚本

```bash
# 先确保副本与 App 内置脚本一致（单一来源在 composeResources）
python tools/warehouse-submission/sync.py
# 校验：内容一致则退出码 0
python tools/warehouse-submission/sync.py --check
```

---

## 2. 自测（两个阶段）

官方建议：Alpha 用浏览器插件，Beta 用「自定义仓库」在 App 里实测。

### Alpha：浏览器插件（可选，但在真机上排查最方便）

用 [shiguang_Tester](https://github.com/XingHeYuZhuan/shiguang_Tester) 在真实教务页面里跑
`wdkbby.js`，确认最终提交的 JSON（`courses` + `config`）符合《一些数据的格式信息》的结构。

### Beta：App 内实测

先在「更多」页点应用图标 **5 次** 打开开发者功能，然后「我的 → 更多 → 更新仓库」。

* **只改了 js，没改 yaml**：索引不用重编。把仓库地址填成自己的 fork（自定义仓库/私有仓库）→
  更新仓库 → 直接测。
* **连 yaml 一起改了（新增学校）**：改 yaml 不会自动生效，索引必须重新编译。
  在 fork 里手动跑 `Actions → 生成 Protobuf 数据索引 → Run workflow`
  （`build-index.yml` 只有 `workflow_dispatch` 触发），它会把 `school_index.pb` 推到
  `index-pb-release` 孤立分支；然后在 App 里用自定义仓库指向该 fork → 更新 → 学校列表出现
  `NWAFU` → 登录新门户进入「我的课表」→ 执行导入。
  本地也可以编：`pip install pyyaml protobuf && python scripts/build_data.py`。

> 别把 `resources/GLOBAL_TOOLS/test.js` 这类测试文件带上 PR。

---

## 3. 提 PR

1. 推送到 fork → 在 GitHub 上向 `ShiGuangSchedule/shiguang_warehouse` 的 **`pending`** 分支提 PR。
2. PR 描述里写清楚（方便维护者审核）：
   * 学校 / 教务系统版本：西北农林科技大学，ehall 新版教务（金智 jwapp，`wdkbby`）
   * `adapter_id`：`NWAFU_WDKBBY_01`；入口：新门户 `https://newehall.nwafu.edu.cn/`
   * 数据来源：接口（`dqxnxq.do` / `cxxszhxqkb.do`）+ 页面兜底
   * 测试情况：真机 App 导入成功、条数/周次正常（可附截图）
   * 已知限制：未处理调课/停课/补课；未导入作息时间（`timeSlots`）
3. 不要把改过的 `index/school_index.pb` 提交进 PR —— 索引由维护者合并后统一编译。

---

## 4. 合并之后

* 维护者跑索引编译 → `index-pb-release` 分支更新 → 用户「更新仓库」后，学校列表里就有官方条目。
* 此时本仓库的内置条目会**按 `id` 去重并让位给仓库那份**（`SchoolRepository.getSchools()` 的
  `distinctBy { it.id }`，本地索引在前），内置条目只作为无网络/未更新仓库时的兜底。
* 如果想删掉内置条目（完全改由官方仓库维护），删掉
  `shared/src/commonMain/composeResources/files/builtin_schools.pb` 里对应记录
  （改 `tools/gen_builtin_schools.py` 的 `SCHOOLS` 后重新生成）与 `ResourceInitializerManager`
  中的脚本映射即可。
