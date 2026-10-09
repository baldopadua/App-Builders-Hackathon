package com.secondlook.ai.model

import java.text.Normalizer

/**
 * BERT WordPiece tokenizer for the uncased models (lowercase + strip accents), built from vocab.txt.
 * ONNX Runtime does not ship a tokenizer, so this is a port of BERT's BasicTokenizer + WordPiece.
 * Keeps character offsets so token spans map back to the original text.
 */
class WordPieceTokenizer(vocab: List<String>, private val maxCharsPerWord: Int = 100) {

    class Encoding(
        val inputIds: LongArray,
        val attentionMask: LongArray,
        val tokenTypeIds: LongArray,
        val starts: IntArray,   // char offset (inclusive) of each token in the original text; 0 for [CLS]/[SEP]
        val ends: IntArray,     // char offset (exclusive)
    )

    private val index = HashMap<String, Int>(vocab.size * 2).also { m -> vocab.forEachIndexed { i, t -> m[t] = i } }
    private val clsId = id("[CLS]")
    private val sepId = id("[SEP]")
    private val unkId = id("[UNK]")

    private fun id(token: String): Int = index[token] ?: throw IllegalArgumentException("vocab.txt has no $token")

    /** [maxLength] counts [CLS] and [SEP]. Longer text is truncated (fine for chat drafts). */
    fun encode(text: String, maxLength: Int = 128): Encoding {
        val ids = ArrayList<Int>()
        val starts = ArrayList<Int>()
        val ends = ArrayList<Int>()
        val limit = maxLength - 2
        for (word in splitWords(text)) {
            if (ids.size >= limit) break
            wordPieces(word, ids, starts, ends)
        }
        val n = minOf(ids.size, limit)
        val total = n + 2
        val inputIds = LongArray(total)
        val starts2 = IntArray(total)
        val ends2 = IntArray(total)
        inputIds[0] = clsId.toLong()
        for (i in 0 until n) {
            inputIds[i + 1] = ids[i].toLong()
            starts2[i + 1] = starts[i]
            ends2[i + 1] = ends[i]
        }
        inputIds[total - 1] = sepId.toLong()
        return Encoding(inputIds, LongArray(total) { 1L }, LongArray(total), starts2, ends2)
    }

    // ---- WordPiece -------------------------------------------------------------------------

    private fun wordPieces(w: Word, ids: MutableList<Int>, starts: MutableList<Int>, ends: MutableList<Int>) {
        val s = w.chars.toString()
        if (s.length > maxCharsPerWord) {
            ids += unkId; starts += w.origStart.first(); ends += w.origEnd.last()
            return
        }
        val pieceIds = ArrayList<Int>()
        val pieceStarts = ArrayList<Int>()
        val pieceEnds = ArrayList<Int>()
        var start = 0
        while (start < s.length) {
            var end = s.length
            var found = -1
            while (start < end) {
                val sub = if (start > 0) "##" + s.substring(start, end) else s.substring(start, end)
                val tid = index[sub]
                if (tid != null) { found = tid; break }
                end--
            }
            if (found < 0) {   // unknown word -> single [UNK] for the whole word
                ids += unkId; starts += w.origStart.first(); ends += w.origEnd.last()
                return
            }
            pieceIds += found
            pieceStarts += w.origStart[start]
            pieceEnds += w.origEnd[end - 1]
            start = end
        }
        ids += pieceIds; starts += pieceStarts; ends += pieceEnds
    }

    // ---- Basic tokenizer: clean, lowercase, strip accents, split on whitespace/punctuation/CJK --

    private class Word {
        val chars = StringBuilder()
        val origStart = ArrayList<Int>()   // one entry per char in [chars]
        val origEnd = ArrayList<Int>()
    }

    private fun splitWords(text: String): List<Word> {
        val words = ArrayList<Word>()
        var cur: Word? = null
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val s = i
            i += Character.charCount(cp)
            val e = i
            if (isWhitespace(cp)) { cur = null; continue }
            if (cp == 0 || cp == 0xFFFD || isControl(cp)) continue
            if (isPunctuation(cp) || isCjk(cp)) {
                cur = null
                words += Word().also { append(it, cp, s, e) }
                continue
            }
            val w = cur ?: Word().also { cur = it; words += it }
            append(w, cp, s, e)
        }
        return words
    }

    private fun append(w: Word, cp: Int, s: Int, e: Int) {
        val lowered = String(Character.toChars(Character.toLowerCase(cp)))
        for (c in Normalizer.normalize(lowered, Normalizer.Form.NFD)) {
            if (Character.getType(c).toByte() == Character.NON_SPACING_MARK) continue
            w.chars.append(c); w.origStart += s; w.origEnd += e
        }
    }

    private fun isWhitespace(cp: Int): Boolean =
        cp == 9 || cp == 10 || cp == 13 || cp == 32 || Character.getType(cp).toByte() == Character.SPACE_SEPARATOR

    private fun isControl(cp: Int): Boolean = when (Character.getType(cp).toByte()) {
        Character.CONTROL, Character.FORMAT, Character.PRIVATE_USE, Character.SURROGATE, Character.UNASSIGNED -> true
        else -> false
    }

    private fun isPunctuation(cp: Int): Boolean {
        if (cp in 33..47 || cp in 58..64 || cp in 91..96 || cp in 123..126) return true
        return when (Character.getType(cp).toByte()) {
            Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.START_PUNCTUATION,
            Character.END_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION,
            Character.OTHER_PUNCTUATION -> true
            else -> false
        }
    }

    private fun isCjk(cp: Int): Boolean =
        cp in 0x4E00..0x9FFF || cp in 0x3400..0x4DBF || cp in 0x20000..0x2A6DF || cp in 0x2A700..0x2B73F ||
            cp in 0x2B740..0x2B81F || cp in 0x2B820..0x2CEAF || cp in 0xF900..0xFAFF || cp in 0x2F800..0x2FA1F
}
