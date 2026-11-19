package com.xingheyuzhuan.shiguangschedule.data.model

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * JVM / 桌面平台专属应用设置
 */
data class JvmAppSettingsModel(
    /** 语言标签，空字符串表示跟随系统 */
    val languageTag: String = "",
    /** 浏览器插件桥接配置 */
    val extensionBridge: ExtensionBridge = ExtensionBridge()
) {
    /**
     * 浏览器插件桥接配置
     */
    data class ExtensionBridge(
        /** 监听端口 */
        val port: Int = DEFAULT_PORT,
        /** 握手密钥，空字符串表示未生成 */
        val apiKey: String = ""
    ) {
        companion object {
            const val DEFAULT_PORT = 59508
            const val MIN_PORT = 1024
            const val MAX_PORT = 65535

            fun isValidPort(port: Int): Boolean = port in MIN_PORT..MAX_PORT
        }
    }

    companion object {
        val KEY_LANGUAGE_TAG = stringPreferencesKey("app_language_tag")
        val KEY_EXTENSION_BRIDGE_PORT = intPreferencesKey("extension_bridge_port")
        val KEY_EXTENSION_BRIDGE_API_KEY = stringPreferencesKey("extension_bridge_api_key")

        /**
         * 从 Preferences 解析模型
         */
        fun fromPreferences(prefs: Preferences): JvmAppSettingsModel {
            val defaultModel = JvmAppSettingsModel()
            val defaultBridge = defaultModel.extensionBridge
            val storedPort = prefs[KEY_EXTENSION_BRIDGE_PORT] ?: defaultBridge.port
            return JvmAppSettingsModel(
                languageTag = prefs[KEY_LANGUAGE_TAG] ?: defaultModel.languageTag,
                extensionBridge = defaultBridge.copy(
                    port = if (ExtensionBridge.isValidPort(storedPort)) storedPort else defaultBridge.port,
                    apiKey = prefs[KEY_EXTENSION_BRIDGE_API_KEY] ?: defaultBridge.apiKey
                )
            )
        }
    }
}
