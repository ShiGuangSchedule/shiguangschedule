const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { JSDOM } = require("jsdom");
const scriptPath = path.resolve(
  __dirname,
  "../../shared/src/commonMain/composeResources/files/adapters/ncepu_baoding.js",
);
const adapter = require(scriptPath);
const fixture = fs.readFileSync(
  path.join(__dirname, "fixtures/timetable.html"),
  "utf8",
);
const schoolUrl = "https://jwxt.ncepu.edu.cn/jsxsd/xskb/xskb_list.do";

function document(html = fixture, url = schoolUrl) {
  return new JSDOM(html, { url }).window.document;
}

function courseBlock(name, weeks, teacher = "测试教师", room = "虚拟教室") {
  return `${name}<br>FAKE201-01<br><font title="老师">${teacher}</font><br><font title="周次(节次)">${weeks}</font><br><font title="教室">${room}</font>`;
}

function grid(block, heading = "第一大节") {
  return `<table><tr><th>${heading}</th><td><div class="kbcontent">${block}</div></td></tr></table>`;
}

test("解析既有脱敏课表中的多课程格、回退节点和未排课备注", () => {
  const draft = adapter.parseDocument(document(), 20);
  assert.deepEqual(draft.errors, []);
  assert.equal(draft.courses.length, 3);
  assert.equal(draft.notes.length, 1);
  const data = draft.courses.find((item) => item.name === "数据结构");
  assert.deepEqual(data, {
    name: "数据结构",
    teacher: "测试教师甲",
    position: "虚拟教室A",
    day: 1,
    startSection: 1,
    endSection: 1,
    weeks: Array.from({ length: 16 }, (_, index) => index + 1),
  });
  assert.deepEqual(
    draft.courses.find((item) => item.name === "大学英语").weeks,
    [1, 3, 5, 7, 9, 11, 13, 15],
  );
  assert.equal(draft.courses.find((item) => item.name === "离散数学").day, 2);
  assert.deepEqual(
    draft.courses.find((item) => item.name === "离散数学").weeks,
    [2, 4, 6],
  );
  assert.ok(!JSON.stringify(draft.courses).includes("课堂派"));
});

test("五大节与周一到周日的格子坐标保持一致", () => {
  const labels = ["第一", "第二", "第三", "第四", "第五"];
  const html =
    "<table>" +
    labels
      .map(
        (label, period) =>
          `<tr><th>${label}大节</th>` +
          Array.from(
            { length: 7 },
            (_, day) =>
              `<td><div class="kbcontent">${courseBlock(`课程${period}-${day}`, "1周")}</div></td>`,
          ).join("") +
          "</tr>",
      )
      .join("") +
    "</table>";
  const draft = adapter.parseDocument(document(html), 20);
  assert.deepEqual(draft.errors, []);
  assert.equal(draft.courses.length, 35);
  for (const [index, item] of draft.courses.entries()) {
    assert.equal(item.day, (index % 7) + 1);
    assert.equal(item.startSection, Math.floor(index / 7) + 1);
    assert.equal(item.startSection, item.endSection);
  }
});

test("忽略空主节点，且不会重复读取隐藏的回退课程", () => {
  const html = `<table><tr><th>第一大节</th><td><div class="kbcontent">&nbsp;</div><div class="kbcontent1">${courseBlock("回退课程", "2周")}</div></td><td><div class="kbcontent">${courseBlock("主课程", "1周")}</div><div class="kbcontent1">${courseBlock("隐藏副本", "1周")}</div></td></tr></table>`;
  const draft = adapter.parseDocument(document(html), 20);
  assert.deepEqual(
    draft.courses.map((item) => item.name),
    ["回退课程", "主课程"],
  );
});

test("兼容 td 自身为课程节点及 tbody 课表片段", () => {
  const doc = document(
    `<table><tbody><tr><th>第五大节</th><td class="kbcontent">${courseBlock("晚课", "3周")}</td></tr></tbody></table>`,
  );
  const result = adapter.parseDocument(doc.querySelector("tbody"), 20);
  assert.equal(result.courses[0].startSection, 5);
});

test("去除完全重复的安排，同时保留不同教师、教室及周次的安排", () => {
  const block = courseBlock("编译原理", "1-6周", "教师甲", "教室A");
  const html = grid(
    [
      block,
      block,
      courseBlock("编译原理", "1-6周", "教师乙", "教室A"),
      courseBlock("编译原理", "1-6周", "教师甲", "教室B"),
      courseBlock("编译原理", "7-12周", "教师甲", "教室A"),
    ].join("<br>----------<br>"),
  );
  const result = adapter.parseDocument(document(html), 20);
  assert.deepEqual(result.errors, []);
  assert.equal(result.courses.length, 4);
});

for (const [input, expected] of [
  ["1-6(周)", [1, 2, 3, 4, 5, 6]],
  ["1～7周（单）", [1, 3, 5, 7]],
  ["2—8(双周)", [2, 4, 6, 8]],
  ["2,4，6、8周", [2, 4, 6, 8]],
  ["1-5单周,6-10双周", [1, 3, 5, 6, 8, 10]],
  ["1,2,3,4周(单)", [1, 3]],
  ["1,2,3,4周(双)", [2, 4]],
  ["4周,2周,2周", [2, 4]],
  ["每周", Array.from({ length: 20 }, (_, i) => i + 1)],
])
  test(`解析周次 ${input}`, () =>
    assert.deepEqual(adapter.parseWeeks(input, 20), expected));

for (const input of [
  "",
  "0周",
  "21周",
  "6-1周",
  "1-999999999周",
  "1,,2周",
  "1-6单双周",
  "第六周",
  "2周(单)",
  "1-6周[1-2节]",
]) {
  test(`拒绝异常周次 ${input || "空值"}`, () =>
    assert.throws(() => adapter.parseWeeks(input, 20)));
}

test("缺少周次或课程名称产生错误，不静默生成错误课程", () => {
  assert.equal(
    adapter.parseDocument(document(grid(courseBlock("无周次", ""))), 20).errors
      .length,
    1,
  );
  assert.equal(
    adapter.parseDocument(document(grid(courseBlock("", "1周"))), 20).errors
      .length,
    1,
  );
});

test("出现无法识别的大节时阻止部分课程导入", () => {
  const draft = adapter.parseDocument(
    document(
      grid(courseBlock("正常课程", "1周")) +
        grid(courseBlock("未知时段", "1周"), "第六大节"),
    ),
    20,
  );
  assert.equal(draft.courses.length, 1);
  assert.match(draft.errors[0], /无法识别课程所在大节/);
});

test("只接受学校域名，拒绝相似域名、非网页协议和 JSON 异常页", () => {
  assert.equal(adapter.isSchoolUrl("https://webvpn.ncepu.edu.cn/path"), true);
  for (const url of [
    "https://evilncepu.edu.cn/",
    "https://ncepu.edu.cn.evil.example/",
    "file:///ncepu.edu.cn",
    "not a url",
  ])
    assert.equal(adapter.isSchoolUrl(url), false);
  assert.throws(() =>
    adapter.findTimetableDocument(document(fixture, "https://example.com")),
  );
  assert.throws(() =>
    adapter.findTimetableDocument(document('{"flag1":3,"msgContent":"错误"}')),
  );
});

test("查找三层同源 frame，并按课表结构选择文档", () => {
  const top = document("<iframe></iframe>", "https://webvpn.ncepu.edu.cn/");
  let child = top;
  for (let level = 0; level < 3; level++) {
    const next = document(
      level === 2 ? fixture : "<iframe></iframe>",
      "https://webvpn.ncepu.edu.cn/teaching",
    );
    Object.defineProperty(child.querySelector("iframe"), "contentDocument", {
      value: next,
    });
    child = next;
  }
  assert.equal(adapter.findTimetableDocument(top), child);
});

test("无法访问跨域框架时提供指引，不抛出底层安全异常", () => {
  const doc = document("<iframe></iframe>");
  Object.defineProperty(doc.querySelector("iframe"), "contentDocument", {
    get() {
      throw new Error("SecurityError");
    },
  });
  assert.throws(() => adapter.findTimetableDocument(doc), /跨域框架/);
});

test("验证总周数和真实的首周周一，不猜开学日期", () => {
  for (const value of ["1", "20", "60"])
    assert.equal(adapter.validateTotalWeeks(value), false);
  for (const value of ["0", "61", "1.5", "", "abc"])
    assert.equal(typeof adapter.validateTotalWeeks(value), "string");
  for (const value of ["", "2026-10-26"])
    assert.equal(adapter.validateMonday(value), false);
  for (const value of ["2026-10-27", "2026-02-30", "2026-1-01", "2026-13-01"])
    assert.equal(typeof adapter.validateMonday(value), "string");
});

function runtime({
  html = fixture,
  prompts = ["20", "2026-10-26"],
  confirm = true,
  failAt,
  returnFalse = false,
} = {}) {
  const calls = [];
  const root = {
    document: document(html),
    shiguangBridge: {
      showToast(message) {
        calls.push(["toast", message]);
      },
      notifyTaskCompletion() {
        calls.push(["complete"]);
      },
    },
    shiguangBridgePromise: {
      async showPrompt() {
        return prompts.shift();
      },
      async showAlert(title, message) {
        calls.push(["alert", title, message]);
        return confirm;
      },
    },
  };
  for (const method of [
    "savePresetTimeSlots",
    "saveCourseConfig",
    "saveImportedCourses",
  ]) {
    root.shiguangBridgePromise[method] = async (json) => {
      calls.push([method, JSON.parse(json)]);
      if (failAt === method) {
        if (returnFalse) return false;
        throw new Error("模拟写入失败");
      }
      return true;
    };
  }
  return { root, calls };
}

test("成功导入五大节作息、用户填写的学期配置和课程后才通知完成", async () => {
  const { root, calls } = runtime();
  await adapter.run(root);
  assert.deepEqual(
    calls.filter(([kind]) => kind.startsWith("save")).map(([kind]) => kind),
    ["savePresetTimeSlots", "saveCourseConfig", "saveImportedCourses"],
  );
  assert.deepEqual(
    calls.find(([kind]) => kind === "savePresetTimeSlots")[1],
    adapter.TIME_SLOTS,
  );
  const config = calls.find(([kind]) => kind === "saveCourseConfig")[1];
  assert.equal(config.semesterStartDate, "2026-10-26");
  assert.equal(config.semesterTotalWeeks, 20);
  assert.equal(
    calls.find(([kind]) => kind === "saveImportedCourses")[1].length,
    3,
  );
  assert.equal(calls.at(-1)[0], "complete");
  assert.match(calls[0][2], /替换/);
  assert.match(calls[0][2], /创新实践/);
});

for (const options of [
  { prompts: [null] },
  { prompts: ["20", null] },
  { confirm: false },
]) {
  test(`取消导入无写入：${JSON.stringify(options)}`, async () => {
    const { root, calls } = runtime(options);
    await adapter.run(root);
    assert.equal(
      calls.some(([kind]) => kind.startsWith("save") || kind === "complete"),
      false,
    );
  });
}

test("留空开学日期时明确写入 null，不用当天日期代替", async () => {
  const { root, calls } = runtime({ prompts: ["20", ""] });
  await adapter.run(root);
  assert.equal(
    calls.find(([kind]) => kind === "saveCourseConfig")[1].semesterStartDate,
    null,
  );
});

test("一门课程周次无效时中止整个导入，保护已有课表", async () => {
  const { root, calls } = runtime({
    html: fixture.replace("1-16(周)", "1-30周"),
  });
  await adapter.run(root);
  assert.equal(
    calls.some(([kind]) => kind.startsWith("save") || kind === "complete"),
    false,
  );
  assert.match(calls.at(-1)[2], /尚未写入/);
});

for (const failAt of [
  "savePresetTimeSlots",
  "saveCourseConfig",
  "saveImportedCourses",
]) {
  for (const returnFalse of [false, true])
    test(`保存失败停止后续步骤：${failAt}, false=${returnFalse}`, async () => {
      const { root, calls } = runtime({ failAt, returnFalse });
      await adapter.run(root);
      assert.equal(
        calls.some(([kind]) => kind === "complete"),
        false,
      );
      assert.match(calls.at(-1)[2], /部分设置可能已写入/);
      const writes = calls.filter(([kind]) => kind.startsWith("save"));
      assert.equal(writes.at(-1)[0], failAt);
    });
}

test("浏览器脚本注入使用同一解析器，阻止重复运行并在退出后解除锁定", async () => {
  const dom = new JSDOM(fixture, {
    url: schoolUrl,
    runScripts: "outside-only",
  });
  const { root, calls } = runtime();
  let release;
  root.shiguangBridgePromise.showPrompt = () =>
    new Promise((resolve) => {
      release = resolve;
    });
  dom.window.shiguangBridge = root.shiguangBridge;
  dom.window.shiguangBridgePromise = root.shiguangBridgePromise;
  const source = fs.readFileSync(scriptPath, "utf8");
  dom.window.eval(source);
  assert.equal(dom.window.__ncepuBaodingImportRunning, true);
  dom.window.eval(source);
  assert.equal(calls.filter(([kind]) => kind === "toast").length, 1);
  release(null);
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(dom.window.__ncepuBaodingImportRunning, false);
  assert.equal(dom.window.ncepuValidateWeeks("20"), false);
});
