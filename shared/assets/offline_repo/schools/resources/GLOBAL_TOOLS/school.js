// 文件: school.js

// 1. 显示一个公告信息弹窗
async function demoAlert() {
    try {
        console.log("即将显示公告弹窗...");
        const confirmed = await window.shiguangBridgePromise.showAlert(
            "重要通知",
            "这是一个弹窗示例。",
            "好的"
        );
        if (confirmed) {
            console.log("用户点击了确认按钮。Alert Promise Resolved: " + confirmed);
            window.shiguangBridge.showToast("Alert：用户点击了确认！");
            return true; // 成功时返回 true
        } else {
            console.log("用户点击了取消按钮或关闭了弹窗。Alert Promise Resolved: " + confirmed);
            window.shiguangBridge.showToast("Alert：用户取消了！");
            return false; // 用户取消时返回 false
        }
    } catch (error) {
        console.error("显示公告弹窗时发生错误:", error);
        window.shiguangBridge.showToast("Alert：显示弹窗出错！" + error.message);
        return false; // 出现错误时也返回 false
    }
}

// 2. 显示带输入框的弹窗，并进行简单验证
function validateName(name) {
    if (name === null || name.trim().length === 0) {
        return "输入不能为空！";
    }
    if (name.length < 2) {
        return "姓名至少需要2个字符！";
    }
    return false; // 返回 false 表示验证通过
}

async function demoPrompt() {
    try {
        console.log("即将显示输入框弹窗...");
        const name = await window.shiguangBridgePromise.showPrompt(
            "输入你的姓名",
            "请输入至少2个字符",
            "测试用户",
            "validateName" // 传入全局函数名
        );
        if (name !== null) {
            console.log("用户输入的姓名是: " + name);
            window.shiguangBridge.showToast("欢迎你，" + name + "！");
            return true; // 成功时返回 true
        } else {
            console.log("用户取消了输入。");
            window.shiguangBridge.showToast("Prompt：用户取消了输入！");
            return false; // 用户取消时返回 false
        }
    } catch (error) {
        console.error("显示输入框弹窗时发生错误:", error);
        window.shiguangBridge.showToast("Prompt：显示输入框出错！" + error.message);
        return false; // 出现错误时也返回 false
    }
}

// 3. 显示一个单选列表弹窗
async function demoSingleSelection() {
    const fruits = ["苹果", "香蕉", "橙子", "葡萄", "西瓜", "芒果"];
    try {
        console.log("即将显示单选列表弹窗...");
        const selectedIndex = await window.shiguangBridgePromise.showSingleSelection(
            "选择你喜欢的水果",
            JSON.stringify(fruits), // 必须是 JSON 字符串
            2 // 默认选中索引为 2（橙子）
        );
        if (selectedIndex !== null && selectedIndex >= 0 && selectedIndex < fruits.length) {
            console.log("用户选择了: " + fruits[selectedIndex] + " (索引: " + selectedIndex + ")");
            window.shiguangBridge.showToast("你选择了 " + fruits[selectedIndex]);
            return true; // 成功时返回 true
        } else {
            console.log("用户取消了选择。");
            window.shiguangBridge.showToast("Single Selection：用户取消了选择！");
            return false; // 用户取消时返回 false
        }
    } catch (error) {
        console.error("显示单选列表弹窗时发生错误:", error);
        window.shiguangBridge.showToast("Single Selection：显示列表出错！" + error.message);
        return false; // 出现错误时也返回 false
    }
}

// 4. 导入课程数据
async function demoSaveCourses() {
    console.log("正在准备测试课程数据...");

    // 每个课程对象须符合 1.1 课程数据结构
    // color / remark 为内部字段，无需提交
    const testCourses = [
        {
            "name": "高等数学",
            "teacher": "张教授",
            "position": "教101",
            "day": 1,
            "startSection": 1,
            "endSection": 2,
            "weeks": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20],
            "credit": 4.0
        },
        {
            "name": "测试自定义课程1",
            "teacher": "测试老师1",
            "position": "测试教室1",
            "day": 1,
            "isCustomTime": true,
            "customStartTime": "08:00",
            "customEndTime": "09:00",
            "weeks": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20],
            "credit": 1.0
        },
        {
            "name": "测试自定义课程2",
            "teacher": "测试老师2",
            "position": "测试教室2",
            "day": 3,
            "isCustomTime": true,
            "customStartTime": "06:00",
            "customEndTime": "12:00",
            "weeks": [3, 5, 7, 9, 11, 13, 15],
            "credit": 0.5
        },
        {
            "name": "大学英语",
            "teacher": "李老师",
            "position": "文史楼203",
            "day": 1,
            "startSection": 2,
            "endSection": 4,
            "weeks": [1, 3, 5, 7, 9, 11, 13, 15, 17, 19],
            "credit": 3.0
        },
        {
            "name": "数据结构",
            "teacher": "王副教授",
            "position": "信息楼B301",
            "day": 7,
            "startSection": 2,
            "endSection": 2,
            "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20],
            "credit": 4.0
        },
        {
            "name": "数据结构",
            "teacher": "王副教授",
            "position": "信息楼B301",
            "day": 7,
            "startSection": 3,
            "endSection": 3,
            "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20],
            "credit": 4.0
        },
        {
            "name": "数据结构",
            "teacher": "王副教授",
            "position": "信息楼B301",
            "day": 7,
            "startSection": 4,
            "endSection": 4,
            "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20],
            "credit": 4.0
        },
        {
            "name": "数据结构",
            "teacher": "王副教授",
            "position": "信息楼B301",
            "day": 7,
            "startSection": 5,
            "endSection": 5,
            "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20],
            "credit": 4.0
        },
        {
            "name": "数据结构",
            "teacher": "王副教授",
            "position": "信息楼B301",
            "day": 7,
            "startSection": 6,
            "endSection": 6,
            "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20],
            "credit": 4.0
        },
        {
            "name": "计算机组成原理",
            "teacher": "赵教授",
            "position": "实验楼401",
            "day": 4,
            "startSection": 4,
            "endSection": 4,
            "weeks": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20],
            "credit": 3.5
        },
        {
            "name": "操作系统",
            "teacher": "钱副教授",
            "position": "信息楼C205",
            "day": 5,
            "startSection": 5,
            "endSection": 5,
            "weeks": [1, 3, 5, 7, 9, 11, 13, 15, 17, 19],
            "credit": 3.0
        },
        {
            "name": "计算机网络",
            "teacher": "孙教授",
            "position": "信息楼D103",
            "day": 6,
            "startSection": 6,
            "endSection": 6,
            "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20],
            "credit": 3.0
        },
        {
            "name": "软件工程",
            "teacher": "周副教授",
            "position": "创新楼301",
            "day": 7,
            "startSection": 7,
            "endSection": 7,
            "weeks": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20],
            "credit": 2.5
        },
        {
            "name": "数据库原理",
            "teacher": "吴教授",
            "position": "信息楼E201",
            "day": 1,
            "startSection": 8,
            "endSection": 8,
            "weeks": [1, 3, 5, 7, 9, 11, 13, 15, 17, 19],
            "credit": 3.0
        },
        {
            "name": "人工智能",
            "teacher": "郑副教授",
            "position": "智能楼101",
            "day": 2,
            "startSection": 9,
            "endSection": 9,
            "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20],
            "credit": 2.0
        },
        {
            "name": "机器学习",
            "teacher": "冯教授",
            "position": "智能楼203",
            "day": 3,
            "startSection": 10,
            "endSection": 10,
            "weeks": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20],
            "credit": 3.0
        },
        {
            "name": "编译原理",
            "teacher": "陈副教授",
            "position": "信息楼F105",
            "day": 4,
            "startSection": 11,
            "endSection": 11,
            "weeks": [1, 3, 5, 7, 9, 11, 13, 15, 17, 19],
            "credit": 3.0
        },
        {
            "name": "计算机图形学",
            "teacher": "褚教授",
            "position": "图形楼301",
            "day": 5,
            "startSection": 12,
            "endSection": 12,
            "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20],
            "credit": 2.0
        },
        {
            "name": "网络安全",
            "teacher": "卫副教授",
            "position": "安全楼201",
            "day": 6,
            "startSection": 13,
            "endSection": 13,
            "weeks": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20],
            "credit": 2.5
        },
        {
            "name": "分布式系统",
            "teacher": "蒋教授",
            "position": "云楼101",
            "day": 7,
            "startSection": 14,
            "endSection": 14,
            "weeks": [1, 3, 5, 7, 9, 11, 13, 15, 17, 19],
            "credit": 2.5
        },
        {
            "name": "大数据技术",
            "teacher": "沈副教授",
            "position": "数据楼301",
            "day": 1,
            "startSection": 15,
            "endSection": 15,
            "weeks": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20],
            "credit": 2.0
        },
        {
            "name": "物联网技术",
            "teacher": "韩教授",
            "position": "物联楼201",
            "day": 2,
            "startSection": 16,
            "endSection": 16,
            "weeks": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20],
            "credit": 2.0
        }
    ];

    try {
        console.log("正在尝试导入课程...");
        const result = await window.shiguangBridgePromise.saveImportedCourses(JSON.stringify(testCourses));
        if (result === true) {
            console.log("课程导入成功！");
            window.shiguangBridge.showToast("测试课程导入成功！");
        } else {
            console.log("课程导入未成功，结果：" + result);
            window.shiguangBridge.showToast("测试课程导入失败，请查看日志。");
        }
    } catch (error) {
        console.error("导入课程时发生错误:", error);
        window.shiguangBridge.showToast("导入课程失败: " + error.message);
    }
}

// 5. 导入预设时间段（基础时间）
// 注意：number 必须从 1 开始连续递增，且时间段之间不允许重叠
async function importPresetTimeSlots() {
    console.log("正在准备预设时间段数据...");

    // 每个时间段对象须符合 1.2 预设时间段数据结构
    // alias 为内部字段，无需提交
    const presetTimeSlots = [
        { "number": 1,  "startTime": "07:00", "endTime": "07:40" },
        { "number": 2,  "startTime": "07:45", "endTime": "08:25" },
        { "number": 3,  "startTime": "08:30", "endTime": "09:10" },
        { "number": 4,  "startTime": "09:15", "endTime": "09:55" },
        { "number": 5,  "startTime": "10:15", "endTime": "10:55" },
        { "number": 6,  "startTime": "11:00", "endTime": "11:40" },
        { "number": 7,  "startTime": "11:45", "endTime": "12:25" },
        { "number": 8,  "startTime": "12:30", "endTime": "13:10" },
        { "number": 9,  "startTime": "13:30", "endTime": "14:10" },
        { "number": 10, "startTime": "14:15", "endTime": "14:55" },
        { "number": 11, "startTime": "15:00", "endTime": "15:40" },
        { "number": 12, "startTime": "15:45", "endTime": "16:25" },
        { "number": 13, "startTime": "16:45", "endTime": "17:25" },
        { "number": 14, "startTime": "17:30", "endTime": "18:10" },
        { "number": 15, "startTime": "18:15", "endTime": "18:55" },
        { "number": 16, "startTime": "19:00", "endTime": "19:40" }
    ];

    try {
        console.log("正在尝试导入预设时间段...");
        const result = await window.shiguangBridgePromise.savePresetTimeSlots(JSON.stringify(presetTimeSlots));
        if (result === true) {
            console.log("预设时间段导入成功！");
            window.shiguangBridge.showToast("测试时间段导入成功！");
            return true;
        } else {
            console.log("预设时间段导入未成功，结果：" + result);
            window.shiguangBridge.showToast("测试时间段导入失败，请查看日志。");
            return false;
        }
    } catch (error) {
        console.error("导入时间段时发生错误:", error);
        window.shiguangBridge.showToast("导入时间段失败: " + error.message);
        return false;
    }
}

// 6. 导入作息方案（组合作息）
// 前置条件：必须先成功调用过 importPresetTimeSlots。
// 差量合并：只提交需要覆盖的节次，未提交的节次由基础时间段补齐。
async function demoSaveComboSchedule() {
    console.log("正在准备作息方案数据...");

    /**
     * 辅助函数：获取相对于基准日期偏移 `offsetDays` 天后的 YYYY-MM-DD 格式日期
     */
    function getOffsetDateString(baseDate, offsetDays) {
        const targetDate = new Date(baseDate);
        targetDate.setDate(targetDate.getDate() + offsetDays);

        const year = targetDate.getFullYear();
        const month = String(targetDate.getMonth() + 1).padStart(2, '0');
        const day = String(targetDate.getDate()).padStart(2, '0');

        return `${year}-${month}-${day}`;
    }

    const today = new Date();

    // 方案一生效时间：今日 ~ 6天后（共 7 天）
    const p1StartDate = getOffsetDateString(today, 0);
    const p1EndDate = getOffsetDateString(today, 6);

    // 方案二生效时间：14天后 ~ 20天后（共 7 天）
    const p2StartDate = getOffsetDateString(today, 14);
    const p2EndDate = getOffsetDateString(today, 20);

    // 每个公共作息模板须符合 1.4 组合作息方案数据结构
    // 借助差量合并：只提交需要覆盖的节次，其余节次由基础时间段自动补齐
    const comboScheduleData = {
        "name": "测试组合作息方案",
        "publicSchedules": [
            {
                "name": "上午缩短作息方案",
                "startDate": p1StartDate,
                "endDate": p1EndDate,
                // 仅覆盖上午 1~8 节（每节课缩短 5 分钟），9~16 节沿用基础时间段
                "timeSlots": [
                    { "number": 1, "startTime": "07:00", "endTime": "07:35" },
                    { "number": 2, "startTime": "07:45", "endTime": "08:20" },
                    { "number": 3, "startTime": "08:30", "endTime": "09:05" },
                    { "number": 4, "startTime": "09:15", "endTime": "09:50" },
                    { "number": 5, "startTime": "10:15", "endTime": "10:50" },
                    { "number": 6, "startTime": "11:00", "endTime": "11:35" },
                    { "number": 7, "startTime": "11:45", "endTime": "12:20" },
                    { "number": 8, "startTime": "12:30", "endTime": "13:05" }
                ]
            },
            {
                "name": "下午缩短作息方案",
                "startDate": p2StartDate,
                "endDate": p2EndDate,
                // 仅覆盖下午 9~16 节（每节课缩短 5 分钟），1~8 节沿用基础时间段
                "timeSlots": [
                    { "number": 9,  "startTime": "13:30", "endTime": "14:05" },
                    { "number": 10, "startTime": "14:15", "endTime": "14:50" },
                    { "number": 11, "startTime": "15:00", "endTime": "15:35" },
                    { "number": 12, "startTime": "15:45", "endTime": "16:20" },
                    { "number": 13, "startTime": "16:45", "endTime": "17:20" },
                    { "number": 14, "startTime": "17:30", "endTime": "18:05" },
                    { "number": 15, "startTime": "18:15", "endTime": "18:50" },
                    { "number": 16, "startTime": "19:00", "endTime": "19:35" }
                ]
            }
        ]
    };

    try {
        console.log("正在尝试导入作息方案...");
        const result = await window.shiguangBridgePromise.saveComboSchedule(JSON.stringify(comboScheduleData));
        if (result === true) {
            console.log("作息方案导入成功！");
            window.shiguangBridge.showToast("测试作息方案导入成功！");
        } else {
            console.log("作息方案导入未成功，结果：" + result);
            window.shiguangBridge.showToast("测试作息方案导入失败，请查看日志。");
        }
    } catch (error) {
        console.error("导入作息方案时发生错误:", error);
        window.shiguangBridge.showToast("导入作息方案失败: " + error.message);
    }
}

// 7. 导入课表配置
async function demoSaveConfig() {
    console.log("正在准备配置数据...");

    // 只传入要修改的字段，未提供的字段将使用默认值
    const courseConfigData = {
        "semesterStartDate": null,
        "semesterTotalWeeks": 18,
        "defaultClassDuration": 50,
        "defaultBreakDuration": 5,
        "firstDayOfWeek": 7
    };

    try {
        console.log("正在尝试导入课表配置...");
        const result = await window.shiguangBridgePromise.saveCourseConfig(JSON.stringify(courseConfigData));

        if (result === true) {
            console.log("课表配置导入成功！");
            window.shiguangBridge.showToast("测试配置导入成功");
        } else {
            console.log("课表配置导入未成功，结果：" + result);
            window.shiguangBridge.showToast("测试配置导入失败，请查看日志。");
        }
    } catch (error) {
        console.error("导入配置时发生错误:", error);
        window.shiguangBridge.showToast("导入配置失败: " + error.message);
    }
}


window.shiguangBridge.showToast("这是一个来自 JS 的 Toast 消息，会很快消失！");

/**
 * 编排这些异步操作，并在用户取消时停止后续执行。
 */
async function runAllDemosSequentially() {
    window.shiguangBridge.showToast("所有演示将按顺序开始...");

    // 1. 运行第一个演示：Alert
    const alertResult = await demoAlert();
    if (!alertResult) {
        console.log("用户取消了 Alert 演示，停止后续执行。");
        return; // 用户取消，立即退出函数
    }

    // 2. 运行第二个演示：Prompt
    const promptResult = await demoPrompt();
    if (!promptResult) {
        console.log("用户取消了 Prompt 演示，停止后续执行。");
        return; // 用户取消，立即退出函数
    }

    // 3. 运行第三个演示：SingleSelection
    const selectionResult = await demoSingleSelection();
    if (!selectionResult) {
        console.log("用户取消了 Single Selection 演示，停止后续执行。");
        return; // 用户取消，立即退出函数
    }

    console.log("所有弹窗演示已完成。");
    window.shiguangBridge.showToast("所有弹窗演示已完成！");

    // 以下是数据导入流程
    await demoSaveCourses();

    // 组合作息强制依赖基础时间段：必须先成功导入基础时间段，再导入组合作息
    const timeSlotSaved = await importPresetTimeSlots();
    if (timeSlotSaved) {
        await demoSaveComboSchedule();
    } else {
        window.shiguangBridge.showToast("时间段导入失败，跳过组合作息。");
    }

    await demoSaveConfig();

    // 发送最终的生命周期完成信号
    window.shiguangBridge.notifyTaskCompletion();
}

// 启动所有演示
runAllDemosSequentially();