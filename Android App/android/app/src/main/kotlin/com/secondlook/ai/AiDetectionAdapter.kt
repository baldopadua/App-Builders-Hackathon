package com.secondlook.ai

import android.content.Context
import com.secondlook.Warning

/**
 * Plug-and-Play AI Engine Adapter Interface.
 *
 * Provides a standardized contract between the Keyboard IME and AI Detection engine.
 * Enables seamless switching between regex heuristics, ONNX Runtime models, and
 * vector similarity matchers without touching the keyboard IME logic.
 */
interface AiDetectionAdapter {
    /**
     * Human-readable identifier for the active engine (e.g. "RuleBasedAiAdapter", "OnDeviceAiAdapter").
     */
    val engineName: String

    /**
     * Whether models, tokenizers, or vocabularies are fully loaded and ready for inference.
     */
    val isReady: Boolean

    /**
     * Initializes the adapter and starts loading models asynchronously on a background thread.
     * MUST NOT block the UI thread.
     */
    fun init(context: Context)

    /**
     * Synchronously evaluates draft text. Used for immediate Send key interception.
     * MUST return quickly (< 50ms) to ensure zero UI jank.
     */
    fun detectSync(text: String): Warning?

    /**
     * Asynchronously evaluates draft text after a typing debounce pause.
     * Inference executes on a background thread; the callback is posted to the main thread.
     */
    fun detectAsync(text: String, callback: (Warning?) -> Unit)

    /**
     * Masks detected sensitive secrets in the draft text (the "Itago" action).
     */
    fun hide(text: String, warning: Warning? = null): String
}
