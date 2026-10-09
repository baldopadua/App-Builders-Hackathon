package com.secondlook.ai.bridge

// Flutter <-> native bridge (docs/contract.md). Everything below is commented out because it needs the Flutter
// embedding, which only exists inside the Flutter host app. Paste it into the host app (it compiles there).
//
// Register from MainActivity:
//   override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
//       super.configureFlutterEngine(flutterEngine)
//       AiMethodChannel(DetectionEngine.get(this)).register(flutterEngine.dartExecutor.binaryMessenger)
//   }
//
// import com.secondlook.DetectionEngine
// import io.flutter.plugin.common.BinaryMessenger
// import io.flutter.plugin.common.MethodCall
// import io.flutter.plugin.common.MethodChannel
// import java.util.concurrent.Executors
//
// class AiMethodChannel(private val engine: DetectionEngine) : MethodChannel.MethodCallHandler {
//     private val io = Executors.newSingleThreadExecutor()          // never block the main thread (model load, embeddings)
//     private val main = android.os.Handler(android.os.Looper.getMainLooper())
//
//     fun register(messenger: BinaryMessenger) = MethodChannel(messenger, "secondlook/ai").setMethodCallHandler(this)
//
//     override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
//         io.execute {
//             try {
//                 val out: Any? = when (call.method) {
//                     "check" -> engine.detect(call.argument<String>("text") ?: "").firstOrNull()?.let {
//                         mapOf("type" to it.type, "message" to it.message, "severity" to it.severity, "matchedText" to it.matchedText,
//                             "start" to it.start, "end" to it.end, "tip" to it.tip, "canHide" to it.canHide, "headline" to it.headline)
//                     }
//                     "hideAll" -> engine.hideAll(call.argument<String>("text") ?: "")   // practice screen "Itago" button
//                     // "Teach me": one box for both. kind = "secret" (MPIN / password / key: fingerprint only) or "fact" (meaning + names)
//                     "saveNote" -> engine.secrets.save(call.argument("text")!!, call.argument("label") ?: "")
//                         .let { mapOf("id" to it.id, "label" to it.label, "kind" to it.kind) }
//                     "listNotes" -> engine.secrets.list().map { mapOf("id" to it.id, "label" to it.label, "kind" to it.kind) }
//                     "deleteNote" -> { engine.secrets.delete(call.argument("id")!!); null }
//                     "deleteAll" -> { engine.secrets.deleteAll(); null }
//                     "status" -> mapOf("ready" to engine.isReady, "error" to engine.loadError)
//                     else -> { main.post { result.notImplemented() }; return@execute }
//                 }
//                 main.post { result.success(out) }
//             } catch (e: Exception) {
//                 main.post { result.error(e.javaClass.simpleName, e.message, null) }
//             }
//         }
//     }
// }
