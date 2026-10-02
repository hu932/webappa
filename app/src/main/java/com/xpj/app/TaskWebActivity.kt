package com.xpj.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.xpj.app.databinding.ActivityTaskWebBinding

class TaskWebActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskWebBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskWebBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dispatchId = intent.getStringExtra("dispatch_id")?.trim().orEmpty().ifBlank { "unknown" }
        val url = ApiConfig.TASK_PLATFORM_DOMAIN.trimEnd('/') + "/r/" + Uri.encode(dispatchId)

        // 优先跳虾皮 App 的 WebView 打开，失败回退本 App WebView
        if (openInShopee(url)) {
            finish()
            return
        }

        setupWebView()
        binding.tvUrl.text = url
        binding.webView.loadUrl(url)
        binding.btnBack.setOnClickListener { goBack() }
        binding.btnRefresh.setOnClickListener { binding.webView.reload() }
    }

    private fun openInShopee(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage(ApiConfig.TARGET_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val ws = binding.webView.settings
        ws.javaScriptEnabled = true
        ws.domStorageEnabled = true
        ws.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith(ApiConfig.TASK_PLATFORM_DOMAIN.trimEnd('/'))) return false
                openExternal(url)
                return true
            }
        }
        binding.webView.webChromeClient = WebChromeClient()
    }

    private fun openExternal(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage(ApiConfig.TARGET_PACKAGE)
            })
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Exception) {
                // 无可用浏览器，忽略
            }
        }
    }

    private fun goBack() {
        if (binding.webView.canGoBack()) binding.webView.goBack() else finish()
    }

    override fun onBackPressed() {
        goBack()
    }
}
