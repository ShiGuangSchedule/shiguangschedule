// 文件: wdkbby.js
// ---------------------------------------------------------------------------
// 学校：西北农林科技大学（NWAFU）
// 系统：ehall 新版教务（金智 jwapp）→「我的课表」
// 入口：https://newehall.nwafu.edu.cn/ （新版门户，登录后自行进入「我的课表」再执行导入）
//       · 旧域名 ehall.nwafu.edu.cn 已迁移，故入口不再固定课表页地址
//       · 脚本不写死域名：接口根路径取当前页面的 _JW_INIT_CONFIG.path，请求走同源
//
// 规范：拾光课程表《适配脚本开发指南 v2》
//   · 数据：courses / timeSlots / config / comboSchedule（本脚本只提交 courses + config）
//   · 桥接：window.shiguangBridgePromise（异步）/ window.shiguangBridge（同步）
//   · 流程：runImportFlow() 编排 → 成功后才 notifyTaskCompletion()
//
// 取数策略：接口优先 + 页面兜底
//   1) 学年学期：POST {ROOT}/sys/wdkbby/modules/jshkcb/dqxnxq.do      → DM / MC
//   2) 课程数据：POST {ROOT}/sys/wdkbby/modules/xskcb/cxxszhxqkb.do   → XNXQDM=<学期代码>
//      （不传 SKZC 即返回整个学期的排课，等同页面「学期课表」视图）
//      行字段：KCM 课程名 / SKJS 教师 / JASMC 教室 / JXLDM_DISPLAY 教学楼 / XXXQMC 校外地点
//             SKXQ 星期(1-7) / KSJC,JSJC 起止节次 / SKZC 周次位串 / ZCMC 周次文本
//   3) 接口不可用时解析页面已渲染的课表（.wut_tablex 的 td[data-role="item"]），
//      必要时先点击「学期课表」切到整学期视图。
//
// 已知边界：调课/停课/补课（xsdkkc.do、ISTK）暂不处理；不导入作息时间（节次上下课时间）。
// ---------------------------------------------------------------------------

// ==================== 0. 常量 ====================

const NWAFU_APP_CODE = 'wdkbby';
const NWAFU_PORTAL_HINT = 'newehall.nwafu.edu.cn 新门户 →「我的课表」';
const NWAFU_URL_TERM = '/modules/jshkcb/dqxnxq.do';
const NWAFU_URL_COURSE = '/modules/xskcb/cxxszhxqkb.do';
const NWAFU_ACTION_TERM = 'dqxnxq';
const NWAFU_ACTION_COURSE = 'cxxszhxqkb';

const NWAFU_DEFAULT_TOTAL_WEEKS = 20;
const NWAFU_MAX_TOTAL_WEEKS = 30;
const NWAFU_SWITCH_WAIT_MS = 6000;
const NWAFU_SWITCH_POLL_MS = 300;

// ==================== 1. 通用工具 ====================

function nwafuToast(message) {
    try {
        window.shiguangBridge.showToast(String(message));
    } catch (e) {
        console.warn('[NWAFU] showToast 不可用:', e && e.message);
    }
}

function nwafuLog(message) {
    try {
        console.log('[NWAFU 课表适配] ' + message);
    } catch (e) { /* ignore */ }
}

function nwafuText(value) {
    if (value === null || value === undefined) return '';
    const text = String(value).replace(/\u00a0/g, ' ').trim();
    if (text === 'null' || text === 'undefined') return '';
    return text;
}

function nwafuNormalizeSpace(value) {
    return String(value === null || value === undefined ? '' : value)
        .replace(/\u00a0/g, ' ')
        .replace(/\s+/g, ' ')
        .trim();
}

function nwafuUniqueSorted(numbers) {
    return Array.from(new Set(numbers.filter((n) => typeof n === 'number' && !isNaN(n) && n > 0)))
        .sort((a, b) => a - b);
}

/** 教师字段去重："刘瑶,刘瑶" -> "刘瑶" */
function nwafuCleanTeacher(value) {
    const raw = nwafuText(value);
    if (!raw) return '';
    const names = raw.split(/[,，、;；]/)
        .map((item) => nwafuNormalizeSpace(item))
        .filter((item) => item !== '');
    return Array.from(new Set(names)).join(',') || raw;
}

function nwafuStripEdges(value) {
    return nwafuNormalizeSpace(value)
        .replace(/^[\s,，、第]+/, '')
        .replace(/[\s,，、第]+$/, '');
}

// ==================== 2. 请求工具 ====================

function nwafuRootPath() {
    const config = window._JW_INIT_CONFIG || {};
    let root = config.path || config.ROOT_PATH || config.contextPath || '/jwapp';
    root = String(root);
    if (root.charAt(0) !== '/') root = '/' + root;
    return root.replace(/\/+$/, '');
}

function nwafuEncodeForm(data) {
    const pairs = [];
    Object.keys(data || {}).forEach((key) => {
        const value = data[key];
        if (value === null || value === undefined) return;
        pairs.push(encodeURIComponent(key) + '=' + encodeURIComponent(String(value)));
    });
    return pairs.join('&');
}

/**
 * 与教务页面自身（BH_UTILS.doSyncAjax）等价的表单 POST：
 * 同源请求、携带 Cookie（credentials: include），服务端按 x-www-form-urlencoded 解析。
 */
async function nwafuApi(modulePath, data) {
    const url = nwafuRootPath() + '/sys/' + NWAFU_APP_CODE + modulePath;
    const response = await fetch(url, {
        method: 'POST',
        credentials: 'include',
        headers: {
            'accept': 'application/json, text/javascript, */*; q=0.01',
            'content-type': 'application/x-www-form-urlencoded; charset=UTF-8',
            'x-requested-with': 'XMLHttpRequest'
        },
        body: nwafuEncodeForm(data || {})
    });

    if (!response || !response.ok) {
        throw new Error('HTTP ' + (response ? response.status : '?'));
    }

    const text = await response.text();
    let json;
    try {
        json = JSON.parse(text);
    } catch (e) {
        throw new Error('返回内容不是 JSON（很可能登录已失效）');
    }
    if (json && json.loginURL) throw new Error('登录状态已失效，请重新登录');
    return json;
}

/** 取出 {datas:{<action>:{rows:[...]}}} 中的 rows；null 表示明确的失败/无权限 */
function nwafuRowsOf(response, action) {
    if (!response || !response.datas) return null;
    const node = response.datas[action];
    if (!node || !node.rows || typeof node.rows.length !== 'number') return null;
    if (node.rows.length === 0 && node.extParams && String(node.extParams.code) !== '1') return null;
    return node.rows;
}

function nwafuMessageOf(response) {
    if (!response || !response.datas) return '';
    const keys = Object.keys(response.datas);
    for (let i = 0; i < keys.length; i++) {
        const node = response.datas[keys[i]];
        if (node && node.extParams && node.extParams.msg) return String(node.extParams.msg);
    }
    return '';
}

// ==================== 3. 学年学期 ====================

async function nwafuFetchTermFromApi() {
    const response = await nwafuApi(NWAFU_URL_TERM, {});
    const rows = nwafuRowsOf(response, NWAFU_ACTION_TERM);
    if (!rows || !rows.length) return null;
    const dm = nwafuText(rows[0].DM);
    if (!dm) return null;
    return { dm: dm, mc: nwafuText(rows[0].MC) };
}

function nwafuFetchTermFromDom() {
    const element = document.getElementById('dqxnxq2');
    if (!element) return null;
    const dm = nwafuText(element.getAttribute('value'));
    if (!dm) return null;
    return { dm: dm, mc: nwafuNormalizeSpace(element.textContent) };
}

async function nwafuResolveTerm() {
    try {
        const term = await nwafuFetchTermFromApi();
        if (term) return term;
        nwafuLog('学年学期接口未返回数据，改用页面信息');
    } catch (e) {
        nwafuLog('学年学期接口失败：' + e.message);
    }
    return nwafuFetchTermFromDom();
}

// ==================== 4. 周次与单元格解析 ====================

/** 周次位串，例如 "0000111111111111110000"（第 5-18 周）→ [5..18] */
function parseWeekBitString(value) {
    const text = nwafuText(value);
    if (!text) return null;
    const compact = text.replace(/\s+/g, '');
    if (!/^[01]+$/.test(compact)) return null;
    if (compact.length < 1 || compact.length > 60) return null;
    const weeks = [];
    for (let i = 0; i < compact.length; i++) {
        if (compact.charAt(i) === '1') weeks.push(i + 1);
    }
    return weeks;
}

// 一组「周次表达式」：6-18周 / 4周 / 7-17周(单) / 4-6周(双),8-13周 / 5-16周,18周
const NWAFU_WEEK_GROUP = '\\d+\\s*(?:[-~至]\\s*\\d+)?\\s*(?:[（(]\\s*[单双]\\s*[)）])?\\s*周\\s*(?:[（(]\\s*[单双]\\s*[)）])?';
const NWAFU_WEEK_EXPRESSION = '(?:' + NWAFU_WEEK_GROUP + '\\s*[,，、]?\\s*)+';

/** 解析一段已确认是周次的内容（可含逗号分隔的多组、单双周标记） */
function parseWeekSegments(expression) {
    const text = nwafuText(expression);
    if (!text) return [];

    const weeks = [];
    text.split(/[,，、]/).forEach((segment) => {
        if (!segment) return;
        let parity = 0; // 0=全部 1=单周 2=双周
        if (segment.indexOf('单') !== -1) parity = 1;
        if (segment.indexOf('双') !== -1) parity = 2;

        const rangePattern = /(\d+)\s*(?:[-~至]\s*(\d+))?/g;
        let match;
        while ((match = rangePattern.exec(segment)) !== null) {
            const start = parseInt(match[1], 10);
            let end = match[2] ? parseInt(match[2], 10) : start;
            if (isNaN(start) || start <= 0) continue;
            if (isNaN(end) || end < start) end = start;
            if (end - start > 60) end = start; // 防御：异常区间不展开
            for (let week = start; week <= end; week++) {
                if (parity === 1 && week % 2 === 0) continue;
                if (parity === 2 && week % 2 === 1) continue;
                weeks.push(week);
            }
        }
    });
    return nwafuUniqueSorted(weeks);
}

/** 只截取「周次表达式」，避免把后面的「第3节-第4节」当成周次 */
function nwafuMatchWeekExpression(text) {
    const match = new RegExp(NWAFU_WEEK_EXPRESSION).exec(text);
    if (!match || !match[0]) return null;

    let start = match.index;
    // 向前吸收「1,3,5周」这类只在最后标一次「周」的写法
    const back = /(?:\d+\s*(?:[-~至]\s*\d+)?\s*[,，、]\s*)+$/.exec(text.substring(0, start));
    if (back) start -= back[0].length;

    return { index: start, text: text.substring(start, match.index + match[0].length) };
}

/** 周次文本： "6-18周" / "4周" / "1-16周(单)" / "1,3,5周" */
function parseWeeksDescription(value) {
    const text = nwafuText(value);
    if (!text) return [];

    const matched = nwafuMatchWeekExpression(text);
    if (matched) {
        const weeks = parseWeekSegments(matched.text);
        if (weeks.length) return weeks;
    }
    // 兜底：形如 "1,3,5周"（只在末尾标一次「周」）
    const head = text.split('周')[0] || '';
    return parseWeekSegments(head.replace(/^[^\d]*/, ''));
}

/** 从单元格文本取起止节次："第1节-第2节" / "第3节" */
function nwafuSectionsFromText(text) {
    const range = /第?\s*(\d+)\s*节\s*[-~至]\s*第?\s*(\d+)\s*节/.exec(text);
    if (range) {
        const start = parseInt(range[1], 10);
        const end = parseInt(range[2], 10);
        if (start > 0) return { start: start, end: end >= start ? end : start };
    }
    const single = /第?\s*(\d+)\s*节/.exec(text);
    if (single) {
        const unit = parseInt(single[1], 10);
        if (unit > 0) return { start: unit, end: unit };
    }
    return null;
}

/** 从 "张莹莹 5-16周,18周 第3节-第4节 N8609 说明" 中拆出教师 / 周次 / 教室 */
function parseArrangementInfo(text) {
    const result = { teacher: '', position: '', weeks: [] };
    const content = nwafuNormalizeSpace(text).replace(/说明\s*$/, '').trim();
    if (!content) return result;

    result.weeks = parseWeeksDescription(content);

    const weekMatch = nwafuMatchWeekExpression(content);
    let head;
    let tail;
    if (weekMatch) {
        head = content.substring(0, weekMatch.index);
        tail = content.substring(weekMatch.index + weekMatch.text.length);
    } else {
        const sectionOnly = /第?\s*\d+\s*节/.exec(content);
        head = sectionOnly ? content.substring(0, sectionOnly.index) : content;
        tail = sectionOnly ? content.substring(sectionOnly.index) : '';
    }

    result.teacher = nwafuCleanTeacher(nwafuStripEdges(head));
    result.position = nwafuStripEdges(
        tail.replace(/第?\s*\d+\s*节(\s*[-~至]\s*第?\s*\d+\s*节)?/g, ' ')
    );
    return result;
}

// ==================== 5. 接口数据 → 课程 ====================

function nwafuBuildPosition(row) {
    const room = nwafuText(row.JASMC) || nwafuText(row.JAS);
    if (room) return room;
    const building = nwafuText(row.JXLDM_DISPLAY) || nwafuText(row.JXLMC);
    if (building) return building;
    return nwafuText(row.XXXQMC);
}

function parseCourseRow(row) {
    if (!row) return null;

    const day = parseInt(nwafuText(row.SKXQ), 10);
    if (!(day >= 1 && day <= 7)) return null;

    const baseName = nwafuText(row.KCM);
    if (!baseName) return null;
    const alias = nwafuText(row.TYXMDM_DISPLAY);
    const name = alias ? (baseName + '(' + alias + ')') : baseName;

    const startSection = parseInt(nwafuText(row.KSJC), 10);
    let endSection = parseInt(nwafuText(row.JSJC), 10);
    if (!(startSection > 0)) return null;
    if (isNaN(endSection) || endSection < startSection) endSection = startSection;

    let weeks = parseWeekBitString(row.SKZC);
    if (!weeks || !weeks.length) weeks = parseWeeksDescription(row.ZCMC);

    return {
        name: name,
        teacher: nwafuCleanTeacher(row.SKJS),
        position: nwafuBuildPosition(row),
        day: day,
        startSection: startSection,
        endSection: endSection,
        weeks: weeks
    };
}

async function fetchCoursesFromApi(term) {
    const response = await nwafuApi(NWAFU_URL_COURSE, { XNXQDM: term.dm });
    const rows = nwafuRowsOf(response, NWAFU_ACTION_COURSE);
    if (!rows) {
        const message = nwafuMessageOf(response);
        throw new Error('课表接口未返回数据' + (message ? ('：' + message) : ''));
    }
    if (!rows.length) throw new Error('课表接口返回 0 条记录（该学期可能没有排课）');

    const courses = [];
    let maxWeekBits = 0;
    rows.forEach((row) => {
        const bits = nwafuText(row.SKZC).replace(/\s+/g, '');
        if (/^[01]+$/.test(bits) && bits.length > maxWeekBits) maxWeekBits = bits.length;
        const course = parseCourseRow(row);
        if (course) courses.push(course);
    });

    if (!courses.length) throw new Error('课表接口返回 ' + rows.length + ' 条记录，但均无法解析出课程');
    nwafuLog('接口返回 ' + rows.length + ' 条排课记录，解析出 ' + courses.length + ' 条');
    return { courses: courses, maxWeekBits: maxWeekBits };
}

// ==================== 6. 页面兜底解析 ====================

function nwafuCellText(element) {
    return nwafuNormalizeSpace(element.textContent);
}

function nwafuIsVisible(element) {
    if (!element) return false;
    if (element.style && element.style.display === 'none') return false;
    if (element.getClientRects && element.getClientRects().length > 0) return true;
    return !!(element.offsetWidth || element.offsetHeight);
}

function nwafuCountArrangementItems() {
    return document.querySelectorAll('.mtt_arrange_item').length;
}

/**
 * 页面默认可能停在「周课表」（一次只显示一周）。
 * 若「学期课表」入口可见，点击它切到整学期视图，等页面重绘后返回。
 */
function switchToSemesterMode() {
    return new Promise((resolve) => {
        const entry = document.querySelector("div[data-action='周课表学年学期']");
        if (!nwafuIsVisible(entry)) {
            resolve(false);
            return;
        }

        const before = nwafuCountArrangementItems();
        try {
            entry.click();
        } catch (e) {
            nwafuLog('切换学期课表失败：' + e.message);
            resolve(false);
            return;
        }
        nwafuLog('已切换到「学期课表」视图，等待页面重绘');

        let waited = 0;
        const timer = setInterval(() => {
            waited += NWAFU_SWITCH_POLL_MS;
            const after = nwafuCountArrangementItems();
            const switched = !nwafuIsVisible(entry);
            if ((switched && after > 0) || (after > before && after > 0) || waited >= NWAFU_SWITCH_WAIT_MS) {
                clearInterval(timer);
                resolve(true);
            }
        }, NWAFU_SWITCH_POLL_MS);
    });
}

function parseArrangementItem(item, day, fallbackStart, fallbackEnd) {
    const children = item.children || [];
    let nameText = '';
    let infoText = '';

    for (let i = 0; i < children.length; i++) {
        const child = children[i];
        const className = child.className ? String(child.className) : '';
        if (className.indexOf('mtt_item_tzkcicon') !== -1) continue;

        const text = nwafuCellText(child);
        if (!text) continue;

        if (!infoText && /周/.test(text) && /\d/.test(text)) {
            infoText = text;
            continue;
        }
        if (!nameText || (nameText.indexOf('《') === -1 && text.indexOf('《') !== -1)) {
            if (text.indexOf('《') !== -1 || !nameText) nameText = text;
        }
    }

    const name = nwafuNormalizeSpace(nameText)
        .replace(/[《》]/g, '')
        .replace(/\[\s*[\w-]+\s*\]\s*$/, '')   // 去掉课序号 [07]、[78S01]
        .replace(/^(调|停|补|辅)+/, '')
        .trim();
    if (!name) return null;

    const info = parseArrangementInfo(infoText);
    let sections = nwafuSectionsFromText(infoText);
    if (!sections && fallbackStart > 0) {
        sections = { start: fallbackStart, end: fallbackEnd >= fallbackStart ? fallbackEnd : fallbackStart };
    }
    if (!sections) return null;

    return {
        name: name,
        teacher: info.teacher,
        position: info.position,
        day: day,
        startSection: sections.start,
        endSection: sections.end,
        weeks: info.weeks
    };
}

function parseCoursesFromDom() {
    let cells = document.querySelectorAll('.wut_container td[data-role="item"], #kcb_container td[data-role="item"]');
    if (!cells.length) cells = document.querySelectorAll('td[data-role="item"]');

    const courses = [];
    for (let i = 0; i < cells.length; i++) {
        const cell = cells[i];
        const day = parseInt(cell.getAttribute('data-week'), 10);
        if (!(day >= 1 && day <= 7)) continue;

        let start = parseInt(cell.getAttribute('data-begin-unit'), 10);
        let end = parseInt(cell.getAttribute('data-end-unit'), 10);
        if (isNaN(start)) start = 0;
        if (isNaN(end)) end = start;

        const items = cell.querySelectorAll('.mtt_arrange_item');
        for (let j = 0; j < items.length; j++) {
            const course = parseArrangementItem(items[j], day, start, end);
            if (course) courses.push(course);
        }
    }
    return courses;
}

// ==================== 7. 合并去重（官方参考实现） ====================

/**
 * 节次与周次合并去重函数
 * 来源：拾光课程表 wiki《课程合并与去重函数》（官方参考实现，按需可直接使用）
 * @param {Array<Object>} courses 原始解析课程数组
 * @returns {Array<Object>} 合并去重后的课程数组
 */
function mergeAndDistinctCourses(courses) {
    if (!Array.isArray(courses) || courses.length <= 1) return courses || [];

    // 1. 深拷贝并规范周次数据，过滤无效项
    const list = courses.map((c) => Object.assign({}, c, {
        name: c.name || '',
        teacher: c.teacher || '',
        position: c.position || '',
        weeks: Array.isArray(c.weeks) ? c.weeks.slice().sort((a, b) => a - b) : []
    }));

    // 阶段 1：合并连续节次与完全重复记录（前提：名称、教师、地点、星期、周次一致）
    list.sort((a, b) => {
        return a.name.localeCompare(b.name) ||
            a.teacher.localeCompare(b.teacher) ||
            a.position.localeCompare(b.position) ||
            (a.day || 0) - (b.day || 0) ||
            a.weeks.join(',').localeCompare(b.weeks.join(',')) ||
            (a.startSection || 0) - (b.startSection || 0);
    });

    const step1Merged = [];
    let current = list[0];

    for (let i = 1; i < list.length; i++) {
        const next = list[i];

        const isSameCourseAndWeeks =
            current.name === next.name &&
            current.teacher === next.teacher &&
            current.position === next.position &&
            current.day === next.day &&
            current.weeks.join(',') === next.weeks.join(',');

        const isContinuous = current.endSection + 1 === next.startSection;
        const isDuplicate = current.startSection === next.startSection && current.endSection === next.endSection;

        if (isSameCourseAndWeeks && isContinuous) {
            // 节次连续：延长结束节次（1-2 节 + 3-4 节 -> 1-4 节）
            current.endSection = next.endSection;
        } else if (isSameCourseAndWeeks && isDuplicate) {
            continue; // 完全重复：跳过
        } else {
            step1Merged.push(current);
            current = next;
        }
    }
    step1Merged.push(current);

    // 阶段 2：合并同节次的周次（前提：名称、教师、地点、星期、开始/结束节次一致）
    step1Merged.sort((a, b) => {
        return a.name.localeCompare(b.name) ||
            a.teacher.localeCompare(b.teacher) ||
            a.position.localeCompare(b.position) ||
            (a.day || 0) - (b.day || 0) ||
            (a.startSection || 0) - (b.startSection || 0) ||
            (a.endSection || 0) - (b.endSection || 0);
    });

    const step2Merged = [];
    let cur = step1Merged[0];

    for (let i = 1; i < step1Merged.length; i++) {
        const nxt = step1Merged[i];

        const isSameCourseAndSection =
            cur.name === nxt.name &&
            cur.teacher === nxt.teacher &&
            cur.position === nxt.position &&
            cur.day === nxt.day &&
            cur.startSection === nxt.startSection &&
            cur.endSection === nxt.endSection;

        if (isSameCourseAndSection) {
            // 周次合并去重（1-8 周 + 9-16 周 -> 1-16 周）
            cur.weeks = Array.from(new Set(cur.weeks.concat(nxt.weeks))).sort((a, b) => a - b);
        } else {
            step2Merged.push(cur);
            cur = nxt;
        }
    }
    step2Merged.push(cur);

    return step2Merged;
}

// ==================== 8. 保存 ====================

async function saveCourseConfigData(config) {
    try {
        return await window.shiguangBridgePromise.saveCourseConfig(JSON.stringify(config));
    } catch (error) {
        nwafuToast('保存课表配置失败: ' + error.message);
        return false;
    }
}

async function saveCourseList(courses) {
    try {
        return await window.shiguangBridgePromise.saveImportedCourses(JSON.stringify(courses));
    } catch (error) {
        nwafuToast('保存课程失败: ' + error.message);
        return false;
    }
}

// ==================== 9. 主导入流程 ====================

/** 前置确认：与官方示例一致，先确认已登录再取数据 */
async function promptUserToStart() {
    const confirmed = await window.shiguangBridgePromise.showAlert(
        '导入确认',
        '将从西北农林科技大学「我的课表」导入课程。\n'
        + '请先登录 ' + NWAFU_PORTAL_HINT + '，再点击开始导入。',
        '开始导入'
    );
    if (!confirmed) {
        nwafuToast('已取消导入');
        return false;
    }
    nwafuToast('正在读取课表数据…');
    return true;
}

/** 接口优先，失败则退回页面解析 */
async function collectCourses() {
    const term = await nwafuResolveTerm();
    let maxWeekBits = 0;

    if (term && term.dm) {
        try {
            const result = await fetchCoursesFromApi(term);
            return { courses: result.courses, maxWeekBits: result.maxWeekBits, term: term, source: '接口' };
        } catch (e) {
            nwafuLog('接口取课表失败：' + e.message + '，改用页面解析');
            nwafuToast('接口取数失败，改为解析当前页面…');
        }
    } else {
        nwafuLog('未能确定学年学期，改用页面解析');
    }

    await switchToSemesterMode();
    const domCourses = parseCoursesFromDom();
    if (!domCourses.length) {
        throw new Error('未能从教务系统取到任何课程数据。\n'
            + '请先登录 ' + NWAFU_PORTAL_HINT + '，打开课表页面后再次点击执行导入。');
    }
    return { courses: domCourses, maxWeekBits: 0, term: term, source: '页面' };
}

function computeTotalWeeks(courses, maxWeekBits) {
    let maxWeek = 0;
    courses.forEach((course) => {
        (course.weeks || []).forEach((week) => {
            if (week > maxWeek) maxWeek = week;
        });
    });
    let total = Math.max(maxWeek, maxWeekBits || 0);
    if (!total) total = NWAFU_DEFAULT_TOTAL_WEEKS;
    return Math.min(Math.max(total, 1), NWAFU_MAX_TOTAL_WEEKS);
}

/** 编排整个导入流程：成功才发送结束信号 */
async function runImportFlow() {
    try {
        if (!(await promptUserToStart())) return;

        const collected = await collectCourses();
        const courses = mergeAndDistinctCourses(collected.courses);
        const totalWeeks = computeTotalWeeks(courses, collected.maxWeekBits);

        // 极少数行既无周次位串也无周次文本：按整学期处理，避免课程永远不显示
        const allWeeks = [];
        for (let week = 1; week <= totalWeeks; week++) allWeeks.push(week);
        let missingWeeks = 0;
        courses.forEach((course) => {
            if (!course.weeks || !course.weeks.length) {
                course.weeks = allWeeks.slice();
                missingWeeks++;
            }
        });
        if (missingWeeks) nwafuLog(missingWeeks + ' 门课程未解析到周次，已按整学期处理');

        if (collected.source === '页面') {
            const acknowledged = await window.shiguangBridgePromise.showAlert(
                '页面解析模式',
                '本次未能通过接口取数，改为解析当前页面，只能导入页面已渲染的课程。\n'
                + '课程条数：' + courses.length + '\n学期周数：' + totalWeeks,
                '继续导入'
            );
            if (!acknowledged) {
                nwafuToast('已取消导入');
                return;
            }
        }

        // 先写配置（学期总周数），再写课程（与官方示例顺序一致）
        const configSaved = await saveCourseConfigData({ semesterTotalWeeks: totalWeeks });
        if (!configSaved) {
            nwafuToast('课表配置保存失败，导入终止');
            return;
        }

        const coursesSaved = await saveCourseList(courses);
        if (!coursesSaved) {
            nwafuToast('课程保存失败，导入终止');
            return;
        }

        const termLabel = collected.term && collected.term.mc ? collected.term.mc : '';
        nwafuToast('导入成功：' + courses.length + ' 门课程' + (termLabel ? '（' + termLabel + '）' : ''));
        window.shiguangBridge.notifyTaskCompletion();
    } catch (error) {
        console.error('[NWAFU 课表适配] 导入失败:', error);
        try {
            await window.shiguangBridgePromise.showAlert(
                '导入失败',
                '导入失败：' + (error && error.message ? error.message : String(error))
                + '\n\n请先登录 ' + NWAFU_PORTAL_HINT + '，打开课表页面后重试。',
                '知道了'
            );
        } catch (e) {
            nwafuToast('导入失败：' + (error && error.message ? error.message : String(error)));
        }
    }
}

// 启动导入流程
runImportFlow();
