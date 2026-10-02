package com.xpj.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * 接收天契助手 App 抓包后广播过来的商品数据（action = com.app.helper.FILE_ACTION），
 * 自动回传到自建任务平台（POST /r/<下发id>/report），无需手动粘贴。
 */
class DataReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DataReceiver"
        const val ACTION_FILE = "com.app.helper.FILE_ACTION"

        // 简单去重：短时间内相同内容只回传一次
        private var lastHash: String = ""
        private var lastReportAt: Long = 0L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FILE) return
        val uriStr = intent.getStringExtra("extra_data") ?: ""
        val path = intent.getStringExtra("extra_data_path") ?: ""

        val pendingResult = goAsync()
        thread {
            try {
                val content = readFileContent(context, uriStr, path)
                if (!content.isNullOrBlank()) {
                    val body = extractBody(content)
                    if (shouldReport(body)) {
                        reportToPlatform(context, body)
                    } else {
                        Log.d(TAG, "duplicate report skipped")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "auto report failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun readFileContent(context: Context, uriStr: String, path: String): String? {
        if (uriStr.startsWith("content://")) {
            try {
                context.contentResolver.openInputStream(Uri.parse(uriStr))?.use {
                    return it.bufferedReader().readText()
                }
            } catch (_: Exception) {
            }
        }
        if (path.isNotEmpty()) {
            try {
                val f = File(path)
                if (f.exists()) return f.readText()
            } catch (_: Exception) {
            }
        }
        return null
    }

    // 抓到的内容优先取 response_body 字段，否则整体原文（与旧 App extractEncryptedData 一致）
    private fun extractBody(fileContent: String): String {
        return try {
            JSONObject(fileContent).optString("response_body", "").ifEmpty { fileContent }
        } catch (_: Exception) {
            fileContent
        }
    }

    private fun shouldReport(body: String): Boolean {
        val now = System.currentTimeMillis()
        val h = body.hashCode().toString()
        val dup = h == lastHash && now - lastReportAt < 5000L
        lastHash = h
        lastReportAt = now
        return !dup
    }

    private fun reportToPlatform(context: Context, body: String) {
        val dispatchId = Prefs.getDispatch(context).trim()
        if (dispatchId.isEmpty()) {
            Log.w(TAG, "dispatch_id empty, skip report")
            return
        }
        val url = URL(ApiConfig.TASK_PLATFORM_DOMAIN.trimEnd('/') + "/r/" + dispatchId + "/report")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val payload = JSONObject().put("body", body).put("version_code", "1").toString()
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            Log.d(TAG, "auto report -> HTTP $code")
        } finally {
            conn.disconnect()
        }
    }
}
