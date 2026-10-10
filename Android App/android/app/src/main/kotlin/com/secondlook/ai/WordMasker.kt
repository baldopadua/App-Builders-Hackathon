package com.secondlook.ai

import com.secondlook.Warning

/**
 * Utility for expanding detected secret spans to word boundaries so that
 * partial masking (e.g. leaving suffixes like "-*sss" visible) never occurs.
 */
object WordMasker {

    /**
     * Given the original [text] and a secret range [[start], [end]),
     * expands the range outward to cover the entire word/token until whitespace
     * (or label delimiters / clause separators).
     */
    fun expandSecretBoundaries(text: String, start: Int, end: Int): Pair<Int, Int> {
        if (text.isEmpty()) return Pair(0, 0)
        val clampedStart = start.coerceIn(0, text.length)
        val clampedEnd = end.coerceIn(clampedStart, text.length)
        if (clampedStart >= clampedEnd && clampedStart == text.length) return Pair(clampedStart, clampedEnd)

        var s = clampedStart
        var e = clampedEnd

        // Check if the secret is explicitly wrapped in matching quotes (e.g. "secret" or 'secret')
        val isWrappedInDoubleQuotes = s > 0 && e < text.length && text[s - 1] == '"' && text[e] == '"'
        val isWrappedInSingleQuotes = s > 0 && e < text.length && text[s - 1] == '\'' && text[e] == '\''

        // 1. Expand leftwards until whitespace or label separator
        while (s > 0) {
            val prev = text[s - 1]
            if (prev.isWhitespace()) break
            if (isWrappedInDoubleQuotes && prev == '"') break
            if (isWrappedInSingleQuotes && prev == '\'') break
            // Stop at label separator like ':' or '=' if preceded by a keyword character
            if ((prev == ':' || prev == '=') && s > 1 && text[s - 2].isLetterOrDigit()) {
                break
            }
            s--
        }

        // 2. Expand rightwards until whitespace or clause separator
        while (e < text.length) {
            val next = text[e]
            if (next.isWhitespace()) break
            if (isWrappedInDoubleQuotes && next == '"') break
            if (isWrappedInSingleQuotes && next == '\'') break
            // Stop at clause punctuation followed by whitespace or end: e.g. "secret, " or "secret."
            if ((next == ',' || next == ';') && (e + 1 == text.length || text[e + 1].isWhitespace())) {
                break
            }
            if (next == '.' && (e + 1 == text.length || text[e + 1].isWhitespace())) {
                break
            }
            e++
        }

        return Pair(s, e)
    }

    /**
     * Ensures that if any bullets ('•') appear in [maskedText], the masking is expanded
     * in [originalText] to cover the entire contiguous word containing the masked portion.
     */
    fun ensureFullWordMask(originalText: String, maskedText: String, warning: Warning? = null): String {
        if (originalText.isEmpty()) return maskedText

        // If warning has a range, we can use it directly
        val rawStart = if (warning != null && warning.start >= 0 && warning.end > warning.start) {
            warning.start
        } else {
            maskedText.indexOf('•')
        }

        val rawEnd = if (warning != null && warning.start >= 0 && warning.end > warning.start) {
            warning.end
        } else {
            val last = maskedText.lastIndexOf('•')
            if (last >= 0) last + 1 else -1
        }

        val s: Int
        val e: Int

        if (rawStart in 0 until originalText.length && rawEnd in (rawStart + 1)..originalText.length) {
            val (expS, expE) = expandSecretBoundaries(originalText, rawStart, rawEnd)
            s = expS
            e = expE
        } else {
            val firstBullet = maskedText.indexOf('•')
            if (firstBullet < 0) return maskedText
            val lastBullet = maskedText.lastIndexOf('•') + 1
            val (expS, expE) = expandSecretBoundaries(originalText, firstBullet, lastBullet)
            s = expS
            e = expE
        }

        if (s < e && s >= 0 && e <= originalText.length) {
            val secret = originalText.substring(s, e)
            val bullets = "•".repeat(secret.length)
            return originalText.substring(0, s) + bullets + originalText.substring(e)
        }

        return maskedText
    }
}
