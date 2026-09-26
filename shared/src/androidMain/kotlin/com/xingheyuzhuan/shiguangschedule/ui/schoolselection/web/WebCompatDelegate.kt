package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * WebView 代理配置与客户端包装类
 */
class WebCompatDelegate(private val webView: WebView) {

    private val defaultUserAgent: String = webView.settings.userAgentString
    private val requestInterceptor = WebViewRequestInterceptor()

    @SuppressLint("SetJavaScriptEnabled")
    fun enhanceSettings(isDesktopMode: Boolean): WebCompatDelegate {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            if (isDesktopMode) {
                userAgentString = DESKTOP_USER_AGENT
                layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                useWideViewPort = false
                loadWithOverviewMode = false
            } else {
                userAgentString = defaultUserAgent
                layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        return this
    }

    fun wrapWebViewClient(original: WebViewClient, isDesktopModeProvider: () -> Boolean): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return original.shouldOverrideUrlLoading(view, request)
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                if (request != null) {
                    val currentDesktopMode = isDesktopModeProvider()
                    val interceptedResponse = requestInterceptor.intercept(request, currentDesktopMode)
                    if (interceptedResponse != null) {
                        return interceptedResponse
                    }
                }
                return original.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                original.onPageStarted(view, url, favicon)
                view?.let { wv ->
                    // 早期注入 JS_INTERCEPT_POST 拦截网络请求
                    wv.evaluateJavascript(JS_INTERCEPT_POST, null)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                original.onPageFinished(view, url)

                view?.injectAllJavaScript()
            }

            override fun onReceivedSslError(v: WebView?, h: SslErrorHandler?, e: SslError?) {
                original.onReceivedSslError(v, h, e)
            }

            override fun onReceivedError(v: WebView, q: WebResourceRequest, e: WebResourceError) =
                original.onReceivedError(v, q, e)
        }
    }

    fun wrapWebChromeClient(original: WebChromeClient, onProgress: (Int) -> Unit): WebChromeClient {
        return object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgress(newProgress)
                original.onProgressChanged(view, newProgress)
            }
            override fun onReceivedTitle(v: WebView?, t: String?) = original.onReceivedTitle(v, t)
        }
    }
}

/** 统一注入 Bridge 初始化与 POST 拦截 JS */
internal fun WebView.injectAllJavaScript() {
    evaluateJavascript(JS_BRIDGE_INIT, null)
    evaluateJavascript(JS_INTERCEPT_POST, null)
}