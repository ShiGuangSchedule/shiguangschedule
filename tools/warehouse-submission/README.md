# 适配仓库提交包（西北农林科技大学）

本目录按**官方适配仓库 `shiguang_warehouse` 的目录结构**准备了一份可直接提 PR 的适配包，
用于把这所学校贡献给上游（App 内置版本在 `shared/src/commonMain/composeResources/files/`）。

## 官方目录规范（摘自 shiguang_warehouse README）

```
shiguang_warehouse/
├── index/root_index.yaml          # 整个仓库的根索引：登记学校/工具
├── resources/<resource_folder>/   # 与 root_index 中 resource_folder 同名的目录
│   ├── adapters.yaml              # 该学校的适配器配置（可多个）
│   └── <script>.js                # 适配脚本（与 asset_js_path 对应）
└── proto/school_index.proto
```

`root_index.yaml` 条目字段：`id` / `name` / `initial` / `resource_folder`。
`adapters.yaml` 条目字段（全部必填，不可增删）：`adapter_id` / `adapter_name` /
`category`（`BACHELOR_AND_ASSOCIATE` | `POSTGRADUATE` | `GENERAL_TOOL`）/ `asset_js_path` /
`import_url`（**必须是登录入口**）/ `maintainer` / `description`。

## 本目录内容

| 文件 | 用途 |
| :--- | :--- |
| `NWAFU/adapters.yaml` | 提交到 `resources/NWAFU/adapters.yaml` |
| `NWAFU/root_index.snippet.yaml` | 追加到 `index/root_index.yaml` 的 `schools:` 列表 |
| `NWAFU/wdkbby.js` | 适配脚本副本（由 `sync.py` 从内置脚本生成，勿手改） |
| `sync.py` | 把内置脚本同步成 `NWAFU/wdkbby.js`；`--check` 只校验一致性 |

## 提交步骤

1. `python tools/warehouse-submission/sync.py`（保证脚本副本与内置版本一致）
2. Fork `shiguang_warehouse`，把 `NWAFU/` 目录复制到 `resources/NWAFU/`
3. 把 `NWAFU/root_index.snippet.yaml` 的内容插入 `index/root_index.yaml` 的 `schools:` 下
4. 按官方「开发流程」自测（自定义仓库 / 私有仓库拉取测试），完成后向 `pending` 分支提 PR
5. 上游 CI 会由 yaml 编译出 `school_index.pb`；合并后 App 即可从官方仓库直接使用

> 注意：`adapters.yaml` / `root_index.yaml` 的改动必须经过索引编译（CI）才会生效；
> 只改 yaml 不重编索引时，App 看不到变化。
> 索引编译后 `NWAFU` 会出现在学校列表里，此时 App 里「内置适配」条目会按同 id 去重、
> 以仓库那份为准（详见 `docs/builtin-adapters.md`）。
