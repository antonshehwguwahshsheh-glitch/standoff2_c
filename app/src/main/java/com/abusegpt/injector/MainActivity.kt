package com.abusegpt.injector

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val userKey = "ZANI-XXXXXX" // Сюда пользователь вводит ключ
        
        CoroutineScope(Dispatchers.IO).launch {
            // ВСТАВЬ СЮДА ПУБЛИЧНЫЙ URL ТВОЕГО ПРОЕКТА DISCLOUD
            if (validateKey(userKey, "https://твой-проект.discloud.app")) {
                if (ensureRootAndSelinux()) {
                    injectCheat()
                    withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Чит запущен", Toast.LENGTH_SHORT).show() }
                } else {
                    withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Нет root прав", Toast.LENGTH_SHORT).show() }
                }
            } else {
                withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Неверный ключ", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun validateKey(key: String, serverUrl: String): Boolean {
        return try {
            val url = URL("$serverUrl/api/validate")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            val writer = OutputStreamWriter(conn.outputStream)
            writer.write("{\"key\":\"$key\"}")
            writer.flush()
            val response = conn.inputStream.bufferedReader().readText()
            response.contains("\"status\":\"valid\"")
        } catch (e: Exception) { false }
    }

    private fun ensureRootAndSelinux(): Boolean {
        return try {
            val checkRoot = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = checkRoot.inputStream.bufferedReader().readText()
            if (!output.contains("uid=0")) return false
            Runtime.getRuntime().exec(arrayOf("su", "-c", "setenforce 0")).waitFor()
            true
        } catch (e: Exception) { false }
    }

    private fun injectCheat() {
        val gamePackage = "com.axlebolt.standoff2"
        val process = Runtime.getRuntime().exec("pidof $gamePackage")
        val pid = process.inputStream.bufferedReader().readText().trim()
        if (pid.isNotEmpty()) {
            Runtime.getRuntime().exec(arrayOf("su", "-c", "chmod 777 /data/local/tmp/libcheat.so"))
            injectNative(pid.toInt(), "/data/local/tmp/libcheat.so")
        }
    }

    private external fun injectNative(pid: Int, libPath: String)

    companion object {
        init { System.loadLibrary("injector") }
    }
}
