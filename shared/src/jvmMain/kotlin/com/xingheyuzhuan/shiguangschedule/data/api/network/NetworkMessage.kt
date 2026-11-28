package com.xingheyuzhuan.shiguangschedule.data.api.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 桌面端与浏览器插件通信的统一数据信封
 */
@Serializable
data class ExtensionBridgeMessage(
    /** 消息唯一标识 */
    val id: String = "",

    /** 业务动作类型 */
    val action: String,

    /** 双向鉴权密钥 */
    val apiKey: String? = null,

    /** 业务数据载体 */
    val payload: JsonObject? = null,

    /** 状态码（默认 200） */
    val code: Int = 200,

    /** 附加提示或错误信息 */
    val message: String? = null
)