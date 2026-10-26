// NCEPU Baoding undergraduate timetable. No requests, credentials or HTML persistence.
(function (root, factory) {
  const adapter = factory();
  if (typeof module === "object" && module.exports) {
    module.exports = adapter;
    return;
  }
  if (root.__ncepuBaodingImportRunning) {
    root.shiguangBridge.showToast("华电保定课表正在读取，请勿重复点击。");
    return;
  }
  root.__ncepuBaodingImportRunning = true;
  root.ncepuValidateWeeks = adapter.validateTotalWeeks;
  root.ncepuValidateMonday = adapter.validateMonday;
  adapter.run(root).finally(function () {
    root.__ncepuBaodingImportRunning = false;
  });
})(typeof window !== "undefined" ? window : globalThis, function () {
  "use strict";

  const PERIODS = ["第一大节", "第二大节", "第三大节", "第四大节", "第五大节"];
  // Match ClassTable's TimetableRepository.defaultSections. Users can edit after import.
  const TIME_SLOTS = [
    { number: 1, startTime: "08:00", endTime: "09:40" },
    { number: 2, startTime: "10:00", endTime: "11:40" },
    { number: 3, startTime: "14:30", endTime: "16:10" },
    { number: 4, startTime: "16:30", endTime: "18:10" },
    { number: 5, startTime: "19:30", endTime: "21:10" },
  ];

  function clean(value) {
    return String(value || "")
      .replace(/\u00a0/g, " ")
      .trim();
  }

  function isSchoolUrl(value) {
    try {
      const url = new URL(value);
      return (
        /^https?:$/.test(url.protocol) &&
        (url.hostname === "ncepu.edu.cn" ||
          url.hostname.endsWith(".ncepu.edu.cn"))
      );
    } catch (_) {
      return false;
    }
  }

  function findTimetableDocument(topDocument) {
    const documents = [];
    function visit(doc, depth) {
      if (!doc || documents.includes(doc)) return;
      documents.push(doc);
      if (depth >= 3) return;
      for (const frame of doc.querySelectorAll("iframe,frame")) {
        try {
          visit(frame.contentDocument, depth + 1);
        } catch (_) {
          // Cross-origin frames must be opened directly by the user.
        }
      }
    }
    function score(doc) {
      try {
        if (!isSchoolUrl(doc.location.href)) return -1;
        const nodes = doc.querySelectorAll(".kbcontent,.kbcontent1").length;
        const hasPeriod = Array.from(doc.querySelectorAll("tr th")).some((th) =>
          PERIODS.some((label) => clean(th.textContent).includes(label)),
        );
        return nodes && hasPeriod
          ? nodes * 1000 + doc.querySelectorAll("tr").length
          : -1;
      } catch (_) {
        return -1;
      }
    }
    visit(topDocument, 0);
    const candidates = documents.map((doc) => ({ doc, score: score(doc) }));
    candidates.sort((a, b) => b.score - a.score);
    if (!candidates.length || candidates[0].score < 0) {
      throw new Error(
        "没有找到华电保定完整课表。请自行登录数字华电／WebVPN，进入教务系统 → 学生课表查询，选择整个学期后再次执行导入。若课表位于无法读取的跨域框架中，请直接打开该课表页面。",
      );
    }
    return candidates[0].doc;
  }

  function validateTotalWeeks(value) {
    return /^\d+$/.test(clean(value)) &&
      Number(value) >= 1 &&
      Number(value) <= 60
      ? false
      : "请输入 1–60 之间的学期总周数";
  }

  function validateMonday(value) {
    value = clean(value);
    if (!value) return false;
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value))
      return "请使用 YYYY-MM-DD 格式，或留空稍后设置";
    const date = new Date(value + "T00:00:00Z");
    if (
      !Number.isFinite(date.getTime()) ||
      date.toISOString().slice(0, 10) !== value ||
      date.getUTCDay() !== 1
    )
      return "请输入真实的学期首周周一日期";
    return false;
  }

  function parseWeeks(input, totalWeeks) {
    if (validateTotalWeeks(String(totalWeeks)))
      throw new Error("学期总周数无效");
    let normalized = clean(input)
      .toLowerCase()
      .replace(/（/g, "(")
      .replace(/）/g, ")")
      .replace(/[～~—–]/g, "-")
      .replace(/[，、]/g, ",")
      .replace(/星期|周/g, "")
      .replace(/\s+/g, "");
    if (!normalized) throw new Error("缺少上课周次");
    // A single trailing parenthesized parity marker applies to the whole list.
    const parityMarkers = normalized.match(/单|双|odd|even/g) || [];
    const suffix = /\((单|双|odd|even)\)$/.exec(normalized);
    const globalParity = parityMarkers.length === 1 && suffix ? suffix[1] : "";
    if (globalParity) normalized = normalized.slice(0, suffix.index);
    const result = new Set();
    for (let part of normalized.split(",")) {
      if (!part) throw new Error("周次包含空项");
      const odd = /单|odd/.test(part + globalParity);
      const even = /双|even/.test(part + globalParity);
      if (odd && even) throw new Error("同一周次片段同时标记了单周和双周");
      part = part.replace(/单|双|odd|even/g, "").replace(/\(\)/g, "");
      if (part === "每" || part === "all" || !part) part = "1-" + totalWeeks;
      const match = /^(\d+)(?:-(\d+))?$/.exec(part);
      if (!match) throw new Error("无法识别周次：" + input);
      const start = Number(match[1]);
      const end = Number(match[2] || match[1]);
      if (start < 1 || end > totalWeeks || start > end) {
        throw new Error(
          "周次范围须在 1–" + totalWeeks + " 内且不能倒序：" + input,
        );
      }
      for (let week = start; week <= end; week++) {
        if ((!odd || week % 2 === 1) && (!even || week % 2 === 0))
          result.add(week);
      }
    }
    if (!result.size) throw new Error("没有可用的上课周次：" + input);
    return Array.from(result).sort((a, b) => a - b);
  }

  function parseBlock(element) {
    const segments = [[]];
    let buffer = "";
    let activeTitle = "";
    function flush() {
      const value = clean(buffer);
      if (/^-{5,}$/.test(value)) {
        if (segments[segments.length - 1].length) segments.push([]);
      } else if (value) {
        segments[segments.length - 1].push({ value, title: activeTitle });
      }
      buffer = "";
      activeTitle = "";
    }
    function visit(node, inheritedTitle) {
      if (node.nodeType === 3) {
        // Formatting whitespace between tags must not swallow the next field's title.
        if (clean(node.textContent)) {
          if (clean(buffer) && activeTitle !== (inheritedTitle || "")) flush();
          activeTitle = inheritedTitle || "";
          buffer += node.textContent;
        }
      } else if (node.nodeType === 1) {
        if (node.tagName.toLowerCase() === "br") {
          flush();
          return;
        }
        const title = node.getAttribute("title") || inheritedTitle;
        for (const child of node.childNodes) visit(child, title);
      }
    }
    visit(element, "");
    flush();
    return segments
      .filter((lines) => lines.length)
      .map((lines) => {
        const field = (title) => {
          const line = lines.find((item) => item.title === title);
          return line ? line.value : "";
        };
        const plain = lines
          .filter(
            (line) =>
              !["老师", "周次(节次)", "教室", "分组名称"].includes(
                line.title,
              ) && !/课堂派|分组名称/.test(line.value),
          )
          .map((line) => line.value);
        const code = plain.find((line) => /^[A-Za-z0-9]+-\d+$/.test(line));
        return {
          name: plain.find((line) => line !== code) || "",
          teacher: field("老师"),
          weeks: field("周次(节次)"),
          position: field("教室"),
        };
      });
  }

  function parseDocument(doc, totalWeeks) {
    const courses = [];
    const errors = [];
    const notes = [];
    const seen = new Set();
    for (const row of doc.querySelectorAll("tr")) {
      const heading = row.querySelector("th");
      const label = heading ? clean(heading.textContent) : "";
      const period = PERIODS.findIndex((item) => label.includes(item)) + 1;
      const cells = Array.from(row.children).filter(
        (cell) => cell.tagName.toLowerCase() === "td",
      );
      if (label.includes("备注")) {
        const note = clean(cells.map((cell) => cell.textContent).join("\n"));
        if (note) notes.push(note);
        continue;
      }
      if (!period) {
        if (
          Array.from(row.querySelectorAll(".kbcontent,.kbcontent1")).some(
            (node) => clean(node.textContent),
          )
        ) {
          errors.push("无法识别课程所在大节：" + (label || "缺少大节表头"));
        }
        continue;
      }
      for (const [index, cell] of cells.slice(0, 7).entries()) {
        const includingSelf = (name) => [
          ...(cell.classList.contains(name) ? [cell] : []),
          ...cell.querySelectorAll("." + name),
        ];
        const primary = includingSelf("kbcontent");
        const blocks = primary.some((node) => clean(node.textContent))
          ? primary
          : includingSelf("kbcontent1");
        for (const block of blocks) {
          for (const parsed of parseBlock(block)) {
            if (!parsed.name) {
              if (parsed.weeks || parsed.teacher || parsed.position)
                errors.push("课程名称缺失，请检查完整课表");
              continue;
            }
            try {
              const course = {
                name: parsed.name,
                teacher: parsed.teacher,
                position: parsed.position,
                day: index + 1,
                startSection: period,
                endSection: period,
                weeks: parseWeeks(parsed.weeks, totalWeeks),
              };
              const key = JSON.stringify(course);
              if (!seen.has(key)) {
                seen.add(key);
                courses.push(course);
              }
            } catch (error) {
              errors.push(parsed.name + "：" + error.message);
            }
          }
        }
      }
    }
    if (!courses.length && !errors.length)
      errors.push("没有识别到课程，请确认课表已加载且选择了完整学期");
    return { courses, errors, notes };
  }

  async function run(root) {
    const bridge = root.shiguangBridgePromise;
    const native = root.shiguangBridge;
    let wroteData = false;
    try {
      const doc = findTimetableDocument(root.document);
      const weeksInput = await bridge.showPrompt(
        "学期总周数",
        "请输入当前所选学期的总周数。不会自动推测开学日期。",
        "20",
        "ncepuValidateWeeks",
      );
      if (weeksInput === null) return;
      if (validateTotalWeeks(weeksInput)) throw new Error("学期总周数无效");
      const totalWeeks = Number(weeksInput);
      const draft = parseDocument(doc, totalWeeks);
      if (draft.errors.length) throw new Error(draft.errors.join("\n"));
      const monday = await bridge.showPrompt(
        "学期首周周一",
        "填写 YYYY-MM-DD；留空则清空开学日期，稍后在课表设置中填写。",
        "",
        "ncepuValidateMonday",
      );
      if (monday === null) return;
      if (validateMonday(monday)) throw new Error("学期首周周一日期无效");
      const summary =
        "识别到 " +
        draft.courses.length +
        " 条课程安排。\n" +
        "将替换所选课表的课程、学期设置和作息。五大节默认时间：08:00–09:40、10:00–11:40、14:30–16:10、16:30–18:10、19:30–21:10（可在导入后调整）。\n" +
        "总周数：" +
        totalWeeks +
        "；首周周一：" +
        (clean(monday) || "稍后设置") +
        "。\n" +
        (draft.notes.length
          ? "以下未排课备注不生成有时间的课程，请另行记录：\n" +
            draft.notes.join("\n")
          : "") +
        "\n请确认当前显示的是完整学期课表；建议导入到新建课表。";
      if (
        !(await bridge.showAlert("确认导入华电保定课表", summary, "确认导入"))
      )
        return;
      async function save(method, payload) {
        // Native writes are separate operations: any failure may leave a partial import.
        wroteData = true;
        if ((await bridge[method](JSON.stringify(payload))) !== true) {
          throw new Error("保存未完成：" + method);
        }
      }
      await save("savePresetTimeSlots", TIME_SLOTS);
      await save("saveCourseConfig", {
        semesterStartDate: clean(monday) || null,
        semesterTotalWeeks: totalWeeks,
        defaultClassDuration: 100,
        defaultBreakDuration: 20,
        firstDayOfWeek: 1,
      });
      await save("saveImportedCourses", draft.courses);
      native.showToast("华电保定课表导入成功，请核对课程、作息和开学日期。");
      native.notifyTaskCompletion();
    } catch (error) {
      const message = error.message || "请返回完整学期课表后重试";
      const suffix = wroteData
        ? "\n部分设置可能已写入，请核对所选课表并重新执行导入。"
        : "\n尚未写入课程或设置。";
      await bridge.showAlert(
        "华电保定课表导入未完成",
        message + suffix,
        "知道了",
      );
    }
  }

  return {
    TIME_SLOTS,
    isSchoolUrl,
    findTimetableDocument,
    validateTotalWeeks,
    validateMonday,
    parseWeeks,
    parseDocument,
    run,
  };
});
