package com.xpj.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

/**
 * 文件查看器：用户用本 App 打开抓包得到的 JSON 文件时，
 * 自动读取内容并上传到任务平台（无需任何手动输入）。
 *
 * 流程（与天契一致）：
 *   打开任务链接 → 跳转虾皮 App → 点击获取任务 → 弹出 JSON 文件
 *   → 用本 App 打开该 JSON → 这里自动上传。
 */
class FileViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri: Uri? = intent.data
        if (uri == null) {
            Toast.makeText(this, "没有收到文件", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val content = readContent(uri)
        if (content.isNullOrBlank()) {
            Toast.makeText(this, "无法读取文件内容", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Toast.makeText(this, "正在上传数据...", Toast.LENGTH_SHORT).show()
        upload(content)
        finish()
    }

    private fun readContent(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
        } catch (_: Exception) {
            null
        }
    }

    private fun upload(content: String) {
        val dispatchId = Prefs.getDispatch(this).trim()
        thread {
            try {
                val code = if (dispatchId.isNotEmpty()) {
                    // 平台 webview 上报：/r/<下发id>/report  JSON: {body, version_code}
                    val url = URL(ApiConfig.TASK_PLATFORM_DOMAIN.trimEnd('/') + "/r/" + dispatchId + "/report")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.connectTimeout = 15000
                    conn.readTimeout = 15000
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    val payload = JSONObject().put("body", content).put("version_code", "1").toString()
                    conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    val c = conn.responseCode
                    conn.disconnect()
                    c
                } else {
                    // 无下发id时走天契契约：/xdd/xiapi/product/report  form: body + versionCode
                    val url = URL(ApiConfig.TASK_PLATFORM_DOMAIN.trimEnd('/') + "/xdd/xiapi/product/report")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.connectTimeout = 15000
                    conn.readTimeout = 15000
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    val form = "body=" + URLEncoder.encode(content, "UTF-8") + "&versionCode=1"
                    conn.outputStream.use { it.write(form.toByteArray(Charsets.UTF_8)) }
                    val c = conn.responseCode
                    conn.disconnect()
                    c
                }
                runOnUiThread {
                    Toast.makeText(
                        this,
                        if (code == 200) "数据已自动上传" else "上传失败 HTTP $code",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "上传异常: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
