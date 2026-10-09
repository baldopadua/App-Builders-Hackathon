package com.secondlook.secondlook

import androidx.annotation.NonNull
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
                else -> {
                    result.notImplemented()
                }
            }
        }
    }
}
