package com.abusegpt.injector

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var injectBtn: Button
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        injectBtn = findViewById(R.id.injectBtn)
        statusText = findViewById(R.id.statusText)

        // Получаем уникальный HWID устройства
        val hwid = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        
        injectBtn.setOnClickListener {
            statusText.text = "Проверка HWID..."
            injectBtn.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                // ВСТАВЬ СЮДА ПУБЛИЧНЫЙ URL ТВОЕГО ПРОЕКТА DISCLOUD (например, https://твой-сайт.discloud.app)
                val serverUrl = "https://твой-проект.discloud.app" 
                
                // Отправляем HWID на сервер для привязки/проверки (используем ID 1, так как ключей больше нет)
                val result = checkHwid(serverUrl, "1", hwid)
                
                if (result == "valid" || result == "registered") {
                    if (ensureRootAndSelinux()) {
                        withContext(Dispatchers.Main) { statusText.text = "Внедрение..." }
                        injectCheat()
                        withContext(Dispatchers.Main) { 
                            statusText.text = "Чит запущен! Открой Standoff 2."
                            Toast.makeText(this@MainActivity, "Успешно", Toast.LENGTH_SHORT).show() 
                        }
                    } else {
                        withContext(Dispatchers.Main) { 
                            statusText.text = "Ошибка: Нет Root прав"
                            injectBtn.isEnabled = true 
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) { 
                        statusText.text = "Ошибка: $result. Купи сброс в боте."
                        injectBtn.isEnabled = true
                    }
                }
            }
        }
    }

    private fun checkHwid(serverUrl: String, userId: String, hwid: String): String {
        return try {
            val url = URL("$serverUrl/api/register")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-bot-token", "BOT_SECRET_123")
            conn.doOutput = true
            val writer = OutputStreamWriter(conn.outputStream)
            writer.write("{\"userId\":\"$userId\",\"hwid\":\"$hwid\"}")
            writer.flush()
            val response = conn.inputStream.bufferedReader().readText()
            if (response.contains("\"status\":\"valid\"")) "valid"
            else if (response.contains("\"status\":\"registered\"")) "registered"
            else "HWID не совпадает"
        } catch (e: Exception) { "Ошибка сети" }
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
