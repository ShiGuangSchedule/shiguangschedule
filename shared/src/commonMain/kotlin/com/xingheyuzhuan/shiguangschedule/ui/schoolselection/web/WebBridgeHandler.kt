package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseConversionRepository
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * JS Bridge 消息处理器，负责路由通信请求并与 Native 业务及 UI 进行交互。
 */
class WebBridgeHandler(
    private val coroutineScope: CoroutineScope,
    private val uiEventChannel: SendChannel<WebUiEvent>,
    private val courseConversionRepository: CourseConversionRepository,
    private val onTaskCompleted: () -> Unit,
    private val evaluateJs: (script: String, callback: ((String?) -> Unit)?) -> Unit
) {
    private val json = CourseImportExport.json
    private var importTableId: String? = null

    /**
     * 状态标记：标记当前任务下是否已经成功执行了“作息方案（组合作息）”导入。
     * 用于防止作息方案导入后，后续又调用基础时间导入覆盖掉“方案”状态。
     */
    private var isComboScheduleImported = false

    /**
     * 内存缓存：记录当前导入任务中最新保存的基础时间列表。
     * 作用：
     * 1. 作为强依赖拦截依据（saveComboSchedule 必须在其存在时才能执行）。
     * 2. 作为作息方案补齐与对齐的基准骨架。
     */
    private var latestPresetTimeSlots: List<CourseImportExport.TimeSlotJsonModel>? = null

    /**
     * 设置当前导入的目标课表 ID。
     */
    fun setImportTableId(tableId: String?) {
        this.importTableId = tableId
        this.isComboScheduleImported = false // 切换或重置课表时重置状态
        this.latestPresetTimeSlots = null    // 重置基准时间缓存
    }

    /**
     * 接收并解析来自 JS 端的消息 JSON 字符串。
     */
    fun onMessageReceived(jsonString: String) {
        try {
            val message = bridgeJson.decodeFromString<JsBridgeMessage>(jsonString)
            val callbackId = message.callbackId

            when (message.action) {
                "showToast" -> parsePayload<ShowToastPayload>(message.payload)?.let {
                    showToast(it.message)
                }

                "showAlert" -> parsePayload<ShowAlertPayload>(message.payload)?.let {
                    showAlert(it.titleText, it.contentText, it.confirmText, callbackId)
                }

                "showPrompt" -> parsePayload<ShowPromptPayload>(message.payload)?.let {
                    showPrompt(it.titleText, it.tipText, it.defaultText, it.validatorJsFunction, callbackId)
                }

                "showSingleSelection" -> parsePayload<ShowSingleSelectionPayload>(message.payload)?.let {
                    showSingleSelection(it.titleText, it.itemsJsonString, it.defaultSelectedIndex, callbackId)
                }

                "saveImportedCourses" -> parsePayload<SaveCoursesPayload>(message.payload)?.let {
                    saveImportedCourses(it.coursesJsonString, callbackId)
                }

                "saveCourseConfig" -> parsePayload<SaveConfigPayload>(message.payload)?.let {
                    saveCourseConfig(it.configJsonString, callbackId)
                }

                "savePresetTimeSlots" -> parsePayload<SaveTimeSlotsPayload>(message.payload)?.let {
                    savePresetTimeSlots(it.timeSlotsJsonString, callbackId)
                }

                "saveComboSchedule" -> parsePayload<SaveComboSchedulePayload>(message.payload)?.let {
                    saveComboSchedule(it.comboScheduleJsonString, callbackId)
                }

                "notifyTaskCompletion" -> notifyTaskCompletion()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 显示短提示 Toast。
     */
    fun showToast(message: String) {
        ToastManager.show(message)
    }

    /**
     * 显示 Alert 确认弹窗。
     */
    fun showAlert(
        titleText: String,
        contentText: String,
        confirmText: String? = null,
        callbackId: String? = null
    ) {
        val resolvedConfirmText = confirmText ?: "确定"
        val data = AlertDialogData(titleText, contentText, resolvedConfirmText)

        val promiseCallback: (Boolean) -> Unit = { confirmed ->
            if (callbackId != null) {
                resolveJsPromise(callbackId, if (confirmed) "true" else "false")
            }
        }

        val sendResult = uiEventChannel.trySend(WebUiEvent.ShowAlert(data, promiseCallback))
        if (sendResult.isFailure && callbackId != null) {
            rejectJsPromise(callbackId, "无法显示弹窗：事件队列已满")
        }
    }

    /**
     * 显示 Prompt 输入弹窗并支持 JS 校验。
     */
    fun showPrompt(
        titleText: String,
        tipText: String,
        defaultText: String = "",
        validatorJsFunction: String? = null,
        callbackId: String? = null
    ) {
        val data = PromptDialogData(
            title = titleText,
            tip = tipText,
            defaultText = defaultText,
            validatorJsFunction = validatorJsFunction
        )

        val errorFlow = MutableSharedFlow<String?>(extraBufferCapacity = 1)

        val onCancel: () -> Unit = {
            if (callbackId != null) {
                resolveJsPromise(callbackId, "null")
            }
        }

        val onRequestValidation: (String, () -> Unit) -> Unit = { input, onSuccess ->
            val encodedInput = bridgeJson.encodeToString(input)

            if (data.validatorJsFunction.isNullOrEmpty()) {
                if (callbackId != null) {
                    resolveJsPromise(callbackId, encodedInput)
                }
                onSuccess()
            } else {
                val jsScript = "${data.validatorJsFunction}($encodedInput)"
                evaluateJs(jsScript) { result ->
                    val validationResult = result?.trim('\"')
                    if (validationResult.isNullOrEmpty() || validationResult.equals("false", ignoreCase = true)) {
                        if (callbackId != null) {
                            resolveJsPromise(callbackId, encodedInput)
                        }
                        onSuccess()
                    } else {
                        coroutineScope.launch {
                            errorFlow.emit(validationResult)
                        }
                    }
                }
            }
        }

        val sendResult = uiEventChannel.trySend(
            WebUiEvent.ShowPrompt(data, onRequestValidation, errorFlow.asSharedFlow(), onCancel)
        )
        if (sendResult.isFailure && callbackId != null) {
            rejectJsPromise(callbackId, "无法显示输入框：事件队列已满")
        }
    }

    /**
     * 显示单选列表弹窗。
     */
    fun showSingleSelection(
        titleText: String,
        itemsJsonString: String,
        defaultSelectedIndex: Int = -1,
        callbackId: String? = null
    ) {
        try {
            val items = json.decodeFromString<List<String>>(itemsJsonString)
            val data = SingleSelectionDialogData(titleText, items, defaultSelectedIndex)

            val promiseCallback: (Int?) -> Unit = { selectedIndex ->
                if (callbackId != null) {
                    resolveJsPromise(callbackId, selectedIndex?.toString() ?: "null")
                }
            }

            val sendResult = uiEventChannel.trySend(WebUiEvent.ShowSingleSelection(data, promiseCallback))
            if (sendResult.isFailure && callbackId != null) {
                rejectJsPromise(callbackId, "无法显示列表：事件队列已满")
            }
        } catch (e: Exception) {
            ToastManager.show("单选列表数据错误，无法显示。")
            if (callbackId != null) {
                rejectJsPromise(callbackId, "选项列表 JSON 无效: ${e.message}")
            }
        }
    }

    /**
     * 解析并保存导入的课程数据。
     * 注意：强制清除课程的 color 和 remark 字段（置为 null）。
     */
    fun saveImportedCourses(coursesJsonString: String, callbackId: String? = null) {
        coroutineScope.launch(Dispatchers.Default) {
            val tableId = importTableId
            if (tableId == null) {
                coroutineScope.launch(Dispatchers.Main) {
                    ToastManager.show("导入失败：未选择课表。")
                    if (callbackId != null) rejectJsPromise(callbackId, "课表选择已取消。")
                }
                return@launch
            }

            val result = runCatching {
                val rawCoursesList = json.decodeFromString<List<CourseImportExport.ImportCourseJsonModel>>(coursesJsonString)
                val sanitizedCoursesList = rawCoursesList.map { course ->
                    course.copy(
                        color = null,
                        remark = null
                    )
                }
                courseConversionRepository.importCourses(tableId, sanitizedCoursesList)
            }

            coroutineScope.launch(Dispatchers.Main) {
                result.onSuccess {
                    ToastManager.show("课程导入成功！课表已更新。")
                    if (callbackId != null) resolveJsPromise(callbackId, "true")
                }.onFailure { e ->
                    ToastManager.show("课程导入失败: ${e.message}")
                    if (callbackId != null) rejectJsPromise(callbackId, "课程导入失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 解析并保存课表配置信息。
     */
    fun saveCourseConfig(configJsonString: String, callbackId: String? = null) {
        coroutineScope.launch(Dispatchers.Default) {
            val tableId = importTableId
            if (tableId == null) {
                coroutineScope.launch(Dispatchers.Main) {
                    ToastManager.show("配置导入失败：未选择目标课表。")
                    if (callbackId != null) rejectJsPromise(callbackId, "课表选择已取消或未设置。")
                }
                return@launch
            }

            val result = runCatching {
                val importedConfig = json.decodeFromString<CourseImportExport.CourseConfigJsonModel>(configJsonString)
                courseConversionRepository.importCourseConfig(tableId, importedConfig)
            }

            coroutineScope.launch(Dispatchers.Main) {
                result.onSuccess {
                    ToastManager.show("课表配置导入成功！")
                    if (callbackId != null) resolveJsPromise(callbackId, "true")
                }.onFailure { e ->
                    ToastManager.show("课表配置导入失败: ${e.message}")
                    if (callbackId != null) rejectJsPromise(callbackId, "课表配置导入失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 解析并保存预设时间段信息（基础时间导入）。
     * 注意：强制清除时间段的 alias 字段（置为 null）。
     */
    fun savePresetTimeSlots(timeSlotsJsonString: String, callbackId: String? = null) {
        // 关键时序拦截：如果作息方案已经导入成功，禁止再导入基础时间，防止覆盖方案的绑定状态
        if (isComboScheduleImported) {
            val errorMsg = "顺序拦截：已导入作息方案，禁止再调用基础时间导入以防覆盖方案状态。"
            ToastManager.show("作息导入失败：适配脚本调用顺序非法。")
            if (callbackId != null) {
                rejectJsPromise(callbackId, errorMsg)
            }
            return
        }

        coroutineScope.launch(Dispatchers.Default) {
            val tableId = importTableId
            if (tableId == null) {
                coroutineScope.launch(Dispatchers.Main) {
                    ToastManager.show("导入失败：未选择课表。")
                    if (callbackId != null) rejectJsPromise(callbackId, "课表选择已取消。")
                }
                return@launch
            }

            val result = runCatching {
                val rawTimeSlotsJson = json.decodeFromString<List<CourseImportExport.TimeSlotJsonModel>>(timeSlotsJsonString)
                // 强制过滤清空 alias
                val sanitizedTimeSlotsJson = rawTimeSlotsJson.map { slot ->
                    slot.copy(alias = null)
                }
                courseConversionRepository.importTimeSlots(tableId, sanitizedTimeSlotsJson)
                sanitizedTimeSlotsJson
            }

            coroutineScope.launch(Dispatchers.Main) {
                result.onSuccess { timeSlots ->
                    // 缓存当前任务的基准时间段（已按 number 升序排序）
                    latestPresetTimeSlots = timeSlots.sortedBy { it.number }
                    ToastManager.show("预设时间段导入成功！")
                    if (callbackId != null) resolveJsPromise(callbackId, "true")
                }.onFailure { e ->
                    ToastManager.show("预设时间段导入失败: ${e.message}")
                    if (callbackId != null) rejectJsPromise(callbackId, "预设时间段导入失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 解析并保存作息方案（组合作息导入）。
     * 语法糖支持：要求必须依赖 savePresetTimeSlots 的缓存结果。自动填充未提供的节次，屏蔽超额节次。
     */
    fun saveComboSchedule(comboScheduleJsonString: String, callbackId: String? = null) {
        // 强校验：检查之前是否成功调用了 savePresetTimeSlots
        val baseSlots = latestPresetTimeSlots
        if (baseSlots.isNullOrEmpty()) {
            val errorMsg = "顺序拦截：saveComboSchedule 必须在 savePresetTimeSlots 执行成功后调用！"
            ToastManager.show("作息方案导入失败：未找到前置的基础时间配置。")
            if (callbackId != null) {
                rejectJsPromise(callbackId, errorMsg)
            }
            return
        }

        coroutineScope.launch(Dispatchers.Default) {
            val tableId = importTableId
            if (tableId == null) {
                coroutineScope.launch(Dispatchers.Main) {
                    ToastManager.show("作息方案导入失败：未选择课表。")
                    if (callbackId != null) rejectJsPromise(callbackId, "课表选择已取消。")
                }
                return@launch
            }

            val result = runCatching {
                val rawComboSchedule = json.decodeFromString<CourseImportExport.ComboScheduleJsonModel>(comboScheduleJsonString)

                // 遍历每个公共作息模板，用 savePresetTimeSlots 拿到的基础时间骨架对其进行自动合并与对齐
                val alignedPublicSchedules = rawComboSchedule.publicSchedules.map { template ->
                    val alignedSlots = mergeAndAlignTimeSlots(
                        baseTimeSlots = baseSlots,
                        draftTimeSlots = template.timeSlots
                    )
                    template.copy(timeSlots = alignedSlots)
                }

                val finalComboSchedule = rawComboSchedule.copy(publicSchedules = alignedPublicSchedules)
                courseConversionRepository.importComboSchedule(tableId, finalComboSchedule)
            }

            coroutineScope.launch(Dispatchers.Main) {
                result.onSuccess {
                    // 标记作息方案成功导入，将锁定后续 savePresetTimeSlots 的调用
                    isComboScheduleImported = true
                    ToastManager.show("作息方案导入成功！")
                    if (callbackId != null) resolveJsPromise(callbackId, "true")
                }.onFailure { e ->
                    ToastManager.show("作息方案导入失败: ${e.message}")
                    if (callbackId != null) rejectJsPromise(callbackId, "作息方案导入失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 通知 Web 任务执行完成，清理上下文状态。
     */
    fun notifyTaskCompletion() {
        importTableId = null
        isComboScheduleImported = false
        latestPresetTimeSlots = null // 任务结束清理内存
        onTaskCompleted()
    }

    /**
     * 语法糖差量合并 Helper 函数：
     * 以 baseTimeSlots 的节次集合为骨架，开发者有传入的节次则覆盖，未传入的节次则用基础数据补齐，超过基础数量的自动抛弃。
     * 注意：合并后的 alias 统一置为空（null）。
     */
    private fun mergeAndAlignTimeSlots(
        baseTimeSlots: List<CourseImportExport.TimeSlotJsonModel>,
        draftTimeSlots: List<CourseImportExport.TimeSlotJsonModel>
    ): List<CourseImportExport.TimeSlotJsonModel> {
        val draftSlotMap = draftTimeSlots.associateBy { it.number }

        return baseTimeSlots.map { baseSlot ->
            val overrideSlot = draftSlotMap[baseSlot.number]
            if (overrideSlot != null) {
                CourseImportExport.TimeSlotJsonModel(
                    number = baseSlot.number,
                    startTime = overrideSlot.startTime,
                    endTime = overrideSlot.endTime,
                    alias = null
                )
            } else {
                baseSlot.copy(alias = null)
            }
        }
    }

    private inline fun <reified T> parsePayload(payloadJson: String?): T? {
        if (payloadJson == null) return null
        return try {
            bridgeJson.decodeFromString<T>(payloadJson)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun resolveJsPromise(callbackId: String, resultRawJs: String) {
        val script = buildJsCallbackScript(callbackId, isSuccess = true, resultRawJs = resultRawJs)
        coroutineScope.launch(Dispatchers.Main) {
            evaluateJs(script, null)
        }
    }

    private fun rejectJsPromise(callbackId: String, errorText: String) {
        val safeErrorJson = bridgeJson.encodeToString(errorText)
        val script = buildJsCallbackScript(callbackId, isSuccess = false, resultRawJs = safeErrorJson)
        coroutineScope.launch(Dispatchers.Main) {
            evaluateJs(script, null)
        }
    }
}