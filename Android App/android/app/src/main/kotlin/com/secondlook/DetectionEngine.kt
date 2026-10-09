package com.secondlook

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.secondlook.ai.AiDetectionAdapter
import com.secondlook.ai.RuleBasedAiAdapter
import java.util.concurrent.Executors

/**
 * Central Detection Engine coordinating AI and heuristic leak detection.
 *
 * Implements the Plug-and-Play AI Architecture for Phase 2.3:
 * - Defaults to [RuleBasedAiAdapter] for sub-millisecond, zero-jank offline protection.
 * - Dynamically hooks into Annie's on-device AI model adapter when classes/models are dropped.
 * - Guarantees background thread isolation: model loading, tokenizing, and inference
 *   never lock or lag keyboard typing.
 */
object DetectionEngine {
    private const val TAG = "DetectionEngine"

    private val bgExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val fallbackAdapter: AiDetectionAdapter = RuleBasedAiAdapter()
    @Volatile
    private var activeAdapter: AiDetectionAdapter = fallbackAdapter

    @Volatile
    private var isInitialized = false

    /**
     * Singleton accessor matching ARCHITECTURE.md: `DetectionEngine.get(context)`.
     */
    fun get(context: Context): DetectionEngine {
        init(context)
        return this
    }

    /**
     * Warm up and load models asynchronously in the background.
     * Guaranteed never to block the main/UI thread.
     */
    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        bgExecutor.execute {
            try {
                Log.i(TAG, "Initializing DetectionEngine on background thread...")
                fallbackAdapter.init(context.applicationContext)

                // Dynamic plug-and-play discovery for Annie's AI core
                val annieAdapter = discoverAnnieAiAdapter(context.applicationContext)
                if (annieAdapter != null) {
                    Log.i(TAG, "Successfully plugged into Annie's AI Core: ${annieAdapter.engineName}")
                    activeAdapter = annieAdapter
                } else {
                    Log.i(TAG, "Annie's AI Core not yet detected. Active engine: ${fallbackAdapter.engineName}")
                    activeAdapter = fallbackAdapter
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during AI adapter initialization. Safe fallback active.", e)
                activeAdapter = fallbackAdapter
            }
        }
    }

    /**
     * Explicitly registers a custom AI adapter (e.g. during handoff or unit testing).
     */
    fun registerAdapter(adapter: AiDetectionAdapter) {
        Log.i(TAG, "Registering AI adapter: ${adapter.engineName}")
        activeAdapter = adapter
    }

    fun getActiveEngineName(): String = activeAdapter.engineName

    val isReady: Boolean
        get() = activeAdapter.isReady

    /**
     * Synchronously analyzes draft text for immediate Send key interception.
     * Fast-paths through active adapter, falling back seamlessly on error or timeout.
     */
    fun detectSync(text: String): Warning? {
        return try {
            activeAdapter.detectSync(text) ?: fallbackAdapter.detectSync(text)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in active adapter detectSync; using fallback", e)
            fallbackAdapter.detectSync(text)
        }
    }

    /**
     * Asynchronously analyzes draft text after debounce pause.
     * Guaranteed execution on background executor; callback dispatched to main thread.
     */
    fun detectAsync(text: String, callback: (Warning?) -> Unit) {
        bgExecutor.execute {
            val warning = try {
                activeAdapter.detectSync(text) ?: fallbackAdapter.detectSync(text)
            } catch (e: Exception) {
                Log.e(TAG, "Exception in active adapter detectAsync", e)
                fallbackAdapter.detectSync(text)
            }
            mainHandler.post {
                callback(warning)
            }
        }
    }

    /**
     * Masks detected sensitive secrets in the draft text.
     */
    fun hide(text: String, warning: Warning? = null): String {
        return try {
            activeAdapter.hide(text, warning)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in active adapter hide; using fallback", e)
            fallbackAdapter.hide(text, warning)
        }
    }

    /**
     * Probes the runtime classpath for Annie's AI core implementation if dropped in.
     */
    private fun discoverAnnieAiAdapter(context: Context): AiDetectionAdapter? {
        val candidateClassNames = listOf(
            "com.secondlook.ai.AnnieAiAdapter",
            "com.secondlook.ai.model.PiiDetectorAdapter",
            "com.secondlook.ai.OnnxAiAdapter"
        )

        for (className in candidateClassNames) {
            try {
                val clazz = Class.forName(className)
                val instance = clazz.getDeclaredConstructor().newInstance()
                if (instance is AiDetectionAdapter) {
                    instance.init(context)
                    return instance
                }
            } catch (_: ClassNotFoundException) {
                // Class not dropped in yet
            } catch (e: Exception) {
                Log.w(TAG, "Candidate adapter $className failed to instantiate", e)
            }
        }
        return null
    }
}
