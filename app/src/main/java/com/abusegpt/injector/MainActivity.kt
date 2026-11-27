package com.abusegpt.injector

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var keyInput: EditText
    private lateinit var injectBtn: Button
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        keyInput = findViewById(R.id.keyInput)
        injectBtn = findViewById(R.id.injectBtn)
        statusText = findViewById(R.id.statusText)

        injectBtn.setOnClickListener {
            val userKey = keyInput.text.toString().trim()
            if (userKey.isEmpty()) {
                Toast.makeText(this, "Введите ключ!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            statusText.text = "Проверка ключа..."
            injectBtn.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                // ВСТАВЬ СЮДА ПУБЛИЧНЫЙ URL ТВОЕГО ПРОЕКТА DISCLOUD
                if (validateKey(userKey, "https://твой-проект.discloud.app")) {
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
                        statusText.text = "Неверный или просроченный ключ"
                        injectBtn.isEnabled = true
                        Toast.makeText(this@MainActivity, "Неверный ключ", Toast.LENGTH_SHORT).show() 
                    }
                }
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
