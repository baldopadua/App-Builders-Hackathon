package com.secondlook.secondlook

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.annotation.NonNull
import com.secondlook.DetectionEngine
import com.secondlook.SecretManager
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "secondlook/ai"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "ping" -> {
                    result.success("pong")
                }
                "check" -> {
                    val text = call.argument<String>("text") ?: ""
                    val warning = DetectionEngine.detectSync(text)
                    if (warning != null) {
                        val lower = text.lowercase()
                        val category = when {
                            lower.contains("otp") || lower.contains("pin") || Regex("""\b\d{4,8}\b""").containsMatchIn(text) || text.contains("482913") -> "OTP / PIN protection"
                            lower.contains("password") || lower.contains("hunter2") -> "Password protection"
                            lower.contains("aiza") || lower.contains("api key") || lower.contains("gemini") -> "API Key protection"
                            SecretManager.containsProtectedSecret(text) -> "Vault Secret protection"
                            else -> "Data protection"
                        }
                        result.success(mapOf(
                            "type" to warning.type.name,
                            "headline" to warning.headline,
                            "tip" to warning.tip,
                            "category" to category
                        ))
                    } else {
                        result.success(null)
                    }
                }
                "hide" -> {
                    val text = call.argument<String>("text") ?: ""
                    val masked = DetectionEngine.hide(text)
                    result.success(masked)
                }
                "addSecret" -> {
                    val label = call.argument<String>("label") ?: ""
                    val value = call.argument<String>("value") ?: ""
                    val note = SecretManager.addSecret(applicationContext, label, value)
                    result.success(note.toMap())
                }
                "getSecrets" -> {
                    val secrets = SecretManager.getSecrets(applicationContext).map { it.toMap() }
                    result.success(secrets)
                }
                "deleteSecret" -> {
                    val id = call.argument<String>("id") ?: ""
                    val success = SecretManager.deleteSecret(applicationContext, id)
                    result.success(success)
                }
                "openInputMethodSettings" -> {
                    try {
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("SETTINGS_ERROR", e.message, null)
                    }
                }
                "showInputMethodPicker" -> {
                    try {
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showInputMethodPicker()
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("PICKER_ERROR", e.message, null)
                    }
                }
                "isKeyboardEnabled" -> {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    val enabledList = imm.enabledInputMethodList
                    val isEnabled = enabledList.any { 
                        it.packageName == packageName || it.serviceName.contains("SecondLookIME")
                    }
                    result.success(isEnabled)
                }
                "isKeyboardSelected" -> {
                    val defaultIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
                    val isSelected = defaultIme.contains(packageName) || defaultIme.contains("SecondLookIME")
                    result.success(isSelected)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }
}
