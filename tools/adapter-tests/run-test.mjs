/*
 * 西北农林科技大学「我的课表」内置适配脚本 · 离线回归测试
 *
 * 依赖：node >= 18 与 jsdom（在 tools/adapter-tests 下执行 `npm i jsdom` 即可）
 *
 * 用法：
 *   node tools/adapter-tests/run-test.mjs unit
 *       —— 纯函数（周次文本/位串、单元格信息、官方合并去重函数）
 *
 *   node tools/adapter-tests/run-test.mjs api
 *       —— 模拟教务接口跑通全流程（单双周、周次位串、脏数据过滤、请求参数、配置顺序）
 *
 *   node tools/adapter-tests/run-test.mjs dom <存档页面.html>
 *       —— 接口全部失败时用浏览器另存的「我的课表」页面跑通页面兜底通道
 *
 * 退出码 0 表示断言全部通过。
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { JSDOM } from 'jsdom';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const REPO = path.resolve(HERE, '..', '..');
const ADAPTER = path.join(REPO, 'shared', 'src', 'commonMain', 'composeResources', 'files', 'builtin_NWAFU_wdkbby.js');

const mode = process.argv[2] || 'unit';
const savedPagePath = process.argv[3] || process.env.SG_SAVED_PAGE || '';
const adapterSource = fs.readFileSync(ADAPTER, 'utf8');

const PAGE_URL = 'https://ehall.nwafu.edu.cn/jwapp/sys/wdkbby/*default/index.do#/xskcb';

function hr(title) {
  console.log('\n' + '='.repeat(70) + '\n' + title + '\n' + '='.repeat(70));
}

/**
 * 造一个 fetch 桩：
 *   handler(url, params, options) 返回 JSON 对象 → 成功响应；返回 null → HTTP 500。
 */
function makeFetch(handler) {
  const calls = [];
  const fetchStub = (url, options = {}) => {
    const params = Object.fromEntries(new URLSearchParams(options.body || ''));
    calls.push({ url, method: options.method, params, credentials: options.credentials });
    return Promise.resolve().then(() => {
      const payload = handler(url, params, options);
      if (payload === null) {
        return { ok: false, status: 500, text: async () => '' };
      }
      return { ok: true, status: 200, text: async () => JSON.stringify(payload) };
    });
  };
  fetchStub.calls = calls;
  return fetchStub;
}

/** 装载页面 + 桥接桩 + fetch 桩，然后执行适配脚本 */
function boot(html, fetchStub) {
  const dom = new JSDOM(html, { url: PAGE_URL, runScripts: 'outside-only' });
  const captured = { courses: null, config: null, alerts: [], toasts: [], completed: false, saveOrder: [] };

  let settle;
  const finished = new Promise((resolve) => { settle = resolve; });
  const done = () => settle();

  dom.window.shiguangBridge = {
    showToast: (message) => captured.toasts.push(message),
    notifyTaskCompletion: () => { captured.completed = true; done(); },
  };
  dom.window.shiguangBridgePromise = {
    showAlert: (title, content, confirmText) => {
      captured.alerts.push({ title, content, confirmText });
      // 只有失败提示会终止流程，其余弹窗一律确认继续
      if (title === '导入失败') done();
      return Promise.resolve(title !== '导入失败');
    },
    saveImportedCourses: (json) => {
      captured.courses = JSON.parse(json);
      captured.saveOrder.push('courses');
      return Promise.resolve(true);
    },
    saveCourseConfig: (json) => {
      captured.config = JSON.parse(json);
      captured.saveOrder.push('config');
      return Promise.resolve(true);
    },
  };
  dom.window.fetch = fetchStub;

  dom.window.eval(adapterSource);
  return { dom, captured, finished };
}

function dumpCourses(courses) {
  console.log('课程条数    :', courses.length);
  for (const c of courses) {
    console.log(`  周${c.day} 第${c.startSection}-${c.endSection}节 | ${c.name} | ${c.teacher} | ${c.position} | 周次 ${c.weeks.join(',')}`);
  }
}

async function runUnitMode() {
  hr('模式：纯函数单元检查');
  const dom = new JSDOM('<html><body></body></html>', { runScripts: 'outside-only' });
  // 空页面下脚本必然报「取不到课程」，这里只关心纯函数，静音脚本自身的日志
  dom.window.console = { log: () => { }, warn: () => { }, error: () => { }, info: () => { }, debug: () => { } };
  dom.window.shiguangBridgePromise = {
    showAlert: () => Promise.resolve(true),
    saveImportedCourses: () => Promise.resolve(true),
    saveCourseConfig: () => Promise.resolve(true),
  };
  dom.window.shiguangBridge = { showToast: () => { }, notifyTaskCompletion: () => { } };
  dom.window.fetch = makeFetch(() => null);
  dom.window.eval(adapterSource);

  // 官方规范要求脚本以顶层函数编写，因此纯函数都挂在 window 上
  const weeks = dom.window.parseWeeksDescription;
  const bits = dom.window.parseWeekBitString;
  const info = dom.window.parseArrangementInfo;
  const merge = dom.window.mergeAndDistinctCourses;

  let failed = 0;
  const check = (ok, label, actual, expected) => {
    if (!ok) failed++;
    console.log(`${ok ? 'PASS' : 'FAIL'}  ${label}${ok ? '' : `\n      actual=${JSON.stringify(actual)}\n      expect=${JSON.stringify(expected)}`}`);
  };

  const weekCases = [
    ['6-18周', [6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18]],
    ['4周', [4]],
    ['1-8周(单)', [1, 3, 5, 7]],
    ['1-8周（双）', [2, 4, 6, 8]],
    ['第3-5周', [3, 4, 5]],
    ['1,3,5周', [1, 3, 5]],
    ['6-18周 第1节-第2节', [6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18]],
    ['5-16周,18周', [5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 18]],
    ['4-6周(双),8-13周', [4, 6, 8, 9, 10, 11, 12, 13]],
    ['3周,6-18周 第3节-第4节 田径场西侧', [3, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18]],
    ['7-17周(单) 第3节-第4节 N8202', [7, 9, 11, 13, 15, 17]],
    ['', []],
  ];
  for (const [input, expected] of weekCases) {
    const actual = weeks(input);
    check(JSON.stringify(actual) === JSON.stringify(expected), `parseWeeksDescription(${JSON.stringify(input)})`, actual, expected);
  }

  const bitWeeks = bits('0000111111111111110000');
  const expectBits = [5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18];
  check(JSON.stringify(bitWeeks) === JSON.stringify(expectBits), 'parseWeekBitString(位串)', bitWeeks, expectBits);
  check(bits('6-18周') === null, 'parseWeekBitString(非位串) -> null', bits('6-18周'), null);

  const parsedInfo = info('张莹莹 5-16周,18周 第3节-第4节 N8609 说明');
  const expectInfo = { teacher: '张莹莹', position: 'N8609', weeks: [5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 18] };
  check(JSON.stringify(parsedInfo) === JSON.stringify(expectInfo), 'parseArrangementInfo', parsedInfo, expectInfo);

  // 官方合并函数：同节次周次合并
  const mergedWeeks = merge([
    { name: 'A', teacher: 'T', position: 'P', day: 1, startSection: 1, endSection: 2, weeks: [4] },
    { name: 'A', teacher: 'T', position: 'P', day: 1, startSection: 1, endSection: 2, weeks: [6, 7] },
    { name: 'A', teacher: 'T', position: 'P', day: 2, startSection: 1, endSection: 2, weeks: [1] },
  ]);
  check(mergedWeeks.length === 2 && JSON.stringify(mergedWeeks[0].weeks) === JSON.stringify([4, 6, 7]),
    'mergeAndDistinctCourses(单双周/拆分行 -> 周次合并)', mergedWeeks, '两条 + 周次 [4,6,7]');

  // 官方合并函数：连续节次合并（1-2 节 + 3-4 节 -> 1-4 节）
  const mergedSections = merge([
    { name: '大学物理', teacher: '李老师', position: '101', day: 1, startSection: 1, endSection: 2, weeks: [1, 2, 3] },
    { name: '大学物理', teacher: '李老师', position: '101', day: 1, startSection: 3, endSection: 4, weeks: [1, 2, 3] },
  ]);
  check(mergedSections.length === 1 && mergedSections[0].startSection === 1 && mergedSections[0].endSection === 4,
    'mergeAndDistinctCourses(连续节次 1-2 + 3-4 -> 1-4)', mergedSections, '一条 1-4 节');

  // 官方合并函数：完全重复记录去重
  const deduped = merge([
    { name: 'B', teacher: '', position: '', day: 3, startSection: 5, endSection: 6, weeks: [1, 2] },
    { name: 'B', teacher: '', position: '', day: 3, startSection: 5, endSection: 6, weeks: [1, 2] },
  ]);
  check(deduped.length === 1, 'mergeAndDistinctCourses(完全重复 -> 去重)', deduped, '一条');

  console.log(failed === 0 ? '\n全部通过' : `\n失败 ${failed} 项`);
  return failed;
}

async function runApiMode() {
  hr('模式：接口通道（合成教务响应）');
  const html = fs.existsSync(savedPagePath) ? fs.readFileSync(savedPagePath, 'utf8') : '<html><body></body></html>';

  const TERM = { code: 0, datas: { dqxnxq: { rows: [{ DM: '2026-2027-1', MC: '2026-2027学年 秋' }], extParams: { code: '1' } } } };
  const weeks5to18 = '0'.repeat(4) + '1'.repeat(14) + '0'.repeat(4); // 22 周位串
  const oddOnly = Array.from({ length: 22 }, (_, i) => ((i + 1) % 2 === 1 ? '1' : '0')).join('');
  const single4 = '0001000000000000000000';
  const ROWS = [
    { KCM: '【本】高等数学甲I（上）', SKJS: '张莹莹', JASMC: 'N3209', SKXQ: '1', KSJC: '1', JSJC: '2', SKZC: single4, ZCMC: '4周' },
    { KCM: '【本】高等数学甲I（上）', SKJS: '张莹莹', JASMC: 'N3209', SKXQ: '1', KSJC: '1', JSJC: '2', SKZC: weeks5to18, ZCMC: '5-18周' },
    { KCM: '【本】大学英语（Ⅰ）', SKJS: '刘瑶,刘瑶', JASMC: 'N8T03', SKXQ: '5', KSJC: '3', JSJC: '4', SKZC: oddOnly, ZCMC: '1-22周(单)' },
    { KCM: '体育', SKJS: '王五', XXXQMC: '北校区体育场', SKXQ: '2', KSJC: '5', JSJC: '6', SKZC: weeks5to18, ZCMC: '5-18周' },
    { KCM: '坏数据（无星期）', SKJS: 'x', SKXQ: '', KSJC: '1', JSJC: '2', SKZC: weeks5to18 },
    { KCM: '坏数据（无节次）', SKJS: 'x', SKXQ: '3', KSJC: '', JSJC: '', SKZC: weeks5to18 },
  ];
  const COURSE = { code: 0, datas: { cxxszhxqkb: { rows: ROWS, extParams: { code: '1' } } } };

  const fetchStub = makeFetch((url) => {
    if (url.includes('dqxnxq.do')) return TERM;
    if (url.includes('cxxszhxqkb.do')) return COURSE;
    return null;
  });

  const { captured, finished } = boot(html, fetchStub);
  await Promise.race([finished, new Promise((resolve) => setTimeout(resolve, 15000))]);

  const courses = captured.courses || [];
  console.log('请求        :', JSON.stringify(fetchStub.calls.map((c) => ({ url: c.url, method: c.method, params: c.params, credentials: c.credentials }))));
  console.log('弹窗        :', JSON.stringify(captured.alerts.map((a) => a.title)));
  console.log('保存顺序    :', JSON.stringify(captured.saveOrder));
  console.log('课表配置    :', JSON.stringify(captured.config));
  dumpCourses(courses);

  const problems = [];
  if (!captured.completed) problems.push('流程未以 notifyTaskCompletion 结束');
  if (JSON.stringify(captured.alerts.map((a) => a.title)) !== JSON.stringify(['导入确认'])) {
    problems.push('接口模式不应出现「页面解析模式」提示，实际: ' + JSON.stringify(captured.alerts.map((a) => a.title)));
  }
  if (JSON.stringify(captured.saveOrder) !== JSON.stringify(['config', 'courses'])) {
    problems.push('保存顺序应为 config -> courses（官方示例顺序），实际: ' + JSON.stringify(captured.saveOrder));
  }
  if (courses.length !== 3) problems.push('课程条数应为 3（两条高等数学合并 + 过滤两条坏数据），实际 ' + courses.length);

  const hmath = courses.find((c) => c.name.includes('高等数学'));
  if (!hmath) problems.push('缺少高等数学');
  else if (JSON.stringify(hmath.weeks) !== JSON.stringify([4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18])) {
    problems.push('高等数学周次合并异常: ' + JSON.stringify(hmath.weeks));
  }

  const english = courses.find((c) => c.name.includes('大学英语'));
  if (!english) problems.push('缺少大学英语');
  else {
    if (english.teacher !== '刘瑶') problems.push('教师去重失败: ' + english.teacher);
    if (english.position !== 'N8T03') problems.push('教室异常: ' + english.position);
    if (english.weeks.some((w) => w % 2 === 0)) problems.push('单周过滤失败: ' + JSON.stringify(english.weeks));
  }

  const pe = courses.find((c) => c.name === '体育');
  if (!pe || pe.position !== '北校区体育场') problems.push('XXXQMC 兜底教室失败: ' + JSON.stringify(pe));
  if (!captured.config || captured.config.semesterTotalWeeks !== 22) {
    problems.push('学期周数应取周次位串长度 22，实际 ' + JSON.stringify(captured.config));
  }

  const courseRequest = fetchStub.calls.find((c) => c.url.includes('cxxszhxqkb.do'));
  if (!courseRequest || courseRequest.method !== 'POST' || courseRequest.params.XNXQDM !== '2026-2027-1' || 'SKZC' in courseRequest.params) {
    problems.push('课表接口请求参数异常: ' + JSON.stringify(courseRequest));
  }
  if (!courseRequest || courseRequest.credentials !== 'include') problems.push('请求应携带 Cookie（credentials: include）');

  hr(problems.length ? '接口模式断言失败:\n- ' + problems.join('\n- ') : '接口模式断言全部通过');
  return problems.length;
}

async function runDomMode() {
  hr('模式：页面兜底（接口全部失败，解析存档页面）');
  const fetchStub = makeFetch(() => null); // 所有接口都失败
  const { captured, finished } = boot(fs.readFileSync(savedPagePath, 'utf8'), fetchStub);
  await Promise.race([finished, new Promise((resolve) => setTimeout(resolve, 15000))]);

  const courses = captured.courses || [];
  console.log('弹窗        :', JSON.stringify(captured.alerts.map((a) => a.title)));
  console.log('保存顺序    :', JSON.stringify(captured.saveOrder));
  console.log('课表配置    :', JSON.stringify(captured.config));
  dumpCourses(courses);

  const problems = [];
  if (!captured.completed) problems.push('流程未以 notifyTaskCompletion 结束');
  if (courses.length === 0) problems.push('未解析到任何课程');
  if (!captured.alerts.some((a) => a.title === '页面解析模式')) problems.push('页面兜底时应提示「页面解析模式」');

  const hmath = courses.find((c) => c.name.includes('高等数学') && c.day === 1);
  if (!hmath) problems.push('未解析到周一的高等数学');
  else {
    const expected = [4, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18];
    if (JSON.stringify(hmath.weeks) !== JSON.stringify(expected)) problems.push('高等数学周次合并异常: ' + JSON.stringify(hmath.weeks));
    if (hmath.teacher !== '张莹莹') problems.push('高等数学教师异常: ' + hmath.teacher);
    if (hmath.position !== 'N3209') problems.push('高等数学教室异常: ' + hmath.position);
    if (hmath.startSection !== 1 || hmath.endSection !== 2) problems.push('高等数学节次异常');
  }

  const badRoom = courses.find((c) => /\d+\s*周|单|双/.test(c.position));
  if (badRoom) problems.push('教室字段混入周次/单双周: ' + badRoom.position);

  const seenKeys = new Set();
  for (const c of courses) {
    const key = [c.name, c.teacher, c.position, c.day, c.startSection, c.endSection].join('|');
    if (seenKeys.has(key)) problems.push('存在未合并的重复排课: ' + key);
    seenKeys.add(key);
  }

  const english = courses.find((c) => c.name.includes('大学英语') && c.day === 3);
  if (!english) problems.push('未解析到周三的大学英语');
  else {
    const expected = [4, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18];
    if (JSON.stringify(english.weeks) !== JSON.stringify(expected)) {
      problems.push('单双周班级合并结果异常: ' + JSON.stringify(english.weeks));
    }
    if (english.teacher !== '刘瑶') problems.push('大学英语教师去重异常: ' + english.teacher);
    if (english.position !== 'N8202') problems.push('大学英语教室异常: ' + english.position);
  }

  hr(problems.length ? 'DOM 模式断言失败:\n- ' + problems.join('\n- ') : 'DOM 模式断言全部通过');
  return problems.length;
}

let failed = 0;
if (mode === 'unit') {
  failed = await runUnitMode();
} else if (mode === 'api') {
  failed = await runApiMode();
} else if (mode === 'dom') {
  if (!savedPagePath || !fs.existsSync(savedPagePath)) {
    console.error('dom 模式需要传入浏览器另存的「我的课表」页面路径，例如：');
    console.error('  node tools/adapter-tests/run-test.mjs dom "E:\\\\path\\\\我的课表.html"');
    process.exit(2);
  }
  failed = await runDomMode();
} else {
  console.error('未知模式：' + mode + '（可用：unit / api / dom）');
  process.exit(2);
}
process.exitCode = failed ? 1 : 0;
