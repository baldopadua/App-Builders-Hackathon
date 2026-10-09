package com.secondlook.ai

/** One detection stage. Return null when nothing is found. Must be fast and never log [text]. */
interface Detector {
    fun detect(text: String): RiskResult?
}
