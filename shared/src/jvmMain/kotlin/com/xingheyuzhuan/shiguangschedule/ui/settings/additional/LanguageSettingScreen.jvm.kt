package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import com.xingheyuzhuan.shiguangschedule.data.repository.JvmAppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale
import java.util.ResourceBundle

/**
 * JVM / Desktop 平台的语言配置管理器 actual 实现
 */
actual object PlatformLocaleManager : KoinComponent {

    private val appSettingsRepository: JvmAppSettingsRepository by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _languageVersion = MutableStateFlow(0L)
    val languageVersion: StateFlow<Long> = _languageVersion.asStateFlow()

    private var cachedLanguageTag: String = ""

    init {
        // 在初始化时安全同步读取首选项，确保 UI 渲染前本地化配置已生效
        runBlocking {
            val settings = appSettingsRepository.getSettings().first()
            cachedLanguageTag = settings.languageTag
            applyLocale(cachedLanguageTag)
        }
    }

    fun initialize() {}

    /**
     * 应用指定的语言标签到 JVM 运行时
     */
    private fun applyLocale(tag: String) {
        val targetLocale = if (tag.isEmpty()) {
            getTrueSystemLocale()
        } else {
            Locale.forLanguageTag(tag)
        }
        Locale.setDefault(targetLocale)
        ResourceBundle.clearCache()
    }

    /**
     * 获取系统底层的真实 Locale
     */
    private fun getTrueSystemLocale(): Locale {
        val language = System.getProperty("user.language") ?: "en"
        val script = System.getProperty("user.script") ?: ""
        val country = System.getProperty("user.country") ?: ""
        val variant = System.getProperty("user.variant") ?: ""

        return Locale.Builder().apply {
            if (language.isNotEmpty()) setLanguage(language)
            if (script.isNotEmpty()) setScript(script)
            if (country.isNotEmpty()) setRegion(country)
            if (variant.isNotEmpty()) setVariant(variant)
        }.build()
    }

    actual fun setLanguageTag(tag: String) {
        cachedLanguageTag = tag
        applyLocale(tag)
        _languageVersion.value++

        // 异步将修改持久化到 DataStore
        scope.launch {
            appSettingsRepository.updateSettings { current ->
                current.copy(languageTag = tag)
            }
        }
    }

    actual fun getCurrentLanguageTag(): String {
        return cachedLanguageTag
    }
}