package com.secondlook.ai.model

/** Tiny helpers for the Hugging Face config.json that ships next to each model. No JSON library needed. */
object ModelConfig {
    /** Reads the "id2label" block, e.g. {"0": "O", "27": "B-PASSWORD", ...}. */
    fun parseId2Label(configJson: String): Map<Int, String> {
        val key = configJson.indexOf("\"id2label\"")
        require(key >= 0) { "config.json has no id2label" }
        val open = configJson.indexOf('{', key)
        val close = configJson.indexOf('}', open)
        val block = configJson.substring(open, close)
        return Regex("\"(\\d+)\"\\s*:\\s*\"([^\"]+)\"").findAll(block)
            .associate { it.groupValues[1].toInt() to it.groupValues[2] }
    }
}
