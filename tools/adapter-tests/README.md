# 适配脚本离线回归测试（jsdom）

针对 `shared/src/commonMain/composeResources/files/builtin_*.js` 内置适配脚本的离线测试，
不需要真实教务账号，也不会发起真实网络请求。

## 准备

```bash
cd tools/adapter-tests
npm i jsdom        # 或 pnpm add jsdom（node_modules 已被 .gitignore 忽略）
```

## 运行

```bash
# 1) 纯函数：周次文本/位串解析、单元格信息解析、拆分行合并
node tools/adapter-tests/run-test.mjs unit

# 2) 接口通道：用合成教务响应跑通全流程
#    （单双周、周次位串、教师去重、脏数据过滤、请求参数、学期总周数）
node tools/adapter-tests/run-test.mjs api

# 3) DOM 兜底：用浏览器另存的「我的课表」页面解析（接口请求全部模拟失败）
node tools/adapter-tests/run-test.mjs dom "D:\path\to\我的课表.html"
# 也可以用环境变量：SG_SAVED_PAGE="D:\path\to\我的课表.html"
```

退出码 `0` 表示断言全部通过。

`dom` 模式需要一份**「学期课表」视图**的存档页面（浏览器里切到「学期课表」后另存为），
否则页面上只有当前一周的课程。

## 测试内容一览

| 模式 | 覆盖点 |
| :--- | :--- |
| `unit` | `parseWeeksDescription`（`6-18周`、`4-6周(双),8-13周`、`1,3,5周`、`7-17周(单)`…）、`parseWeekBitString`、`parseArrangementInfo`、官方 `mergeAndDistinctCourses`（同节次周次合并 / 连续节次 1-2+3-4→1-4 / 完全去重） |
| `api` | 学年学期接口、课表接口请求参数（`POST` + `XNXQDM` + `credentials: include`）、周次位串取周、单双周、教师去重、无星期/无节次脏数据过滤、`XXXQMC` 教室兜底、学期总周数取位串长度、保存顺序 `config → courses` |
| `dom` | 真实页面 `.wut_tablex` 解析、`data-week` 取星期、单元格文本取节次、`4周` + `6-18周` 合并、单周班/双周班合并、教室字段不混入周次、页面兜底时的「页面解析模式」提示 |

> 教务系统改版后（接口名、字段名、DOM 结构变化）先跑这三组测试即可快速定位。

测试通过说明脚本符合官方《适配脚本开发指南 v2》的调用约定：`window.shiguangBridgePromise`
异步接口、`notifyTaskCompletion()` 只在成功时调用、`saveCourseConfig` 先于
`saveImportedCourses`、课程字段只含 `name/teacher/position/day/startSection/endSection/weeks`。
