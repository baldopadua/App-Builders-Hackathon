package com.secondlook.ai.notes

/** A name or code word the user is protecting. Matches a draft when at least [need] of [words] appear in it. */
class Anchor(val words: Set<String>, val need: Int, val shown: String)

/**
 * Anchors are the names / code words inside a private note: capitalised words that are not common sentence
 * starters ("Sunflower", "Marco") and quoted phrases ('blue heron at midnight').
 * A draft that contains an anchor only needs to be a little similar to the note; a draft with no anchor must be
 * very similar. This is what keeps "buying bread at the bakery" quiet while "Sunflower launch is next week" warns.
 * JS twin: web-demo/public/core.js (anchorsOf / anchorHits). Keep in sync.
 */
object NoteAnchors {
    private val STOP: Set<String> = (
        "our the we this that these those there here it its if when what who how why only also then so but and or because after before " +
            "while since please thanks thank hello hi yes no not do does did dont can could will would should may might must lets let i my your his her their " +
            "they them he she you one all any some each both most more many much few other another such even just now new old good great today tomorrow " +
            "monday tuesday wednesday thursday friday saturday sunday january february march april may june july august september october november december " +
            "ang ng sa si ni kay yung mga ako ikaw siya kami tayo sila ito iyon po opo hindi oo ay na pa din rin lang naman ba ko mo niya namin natin nila " +
            "kapag kung para pero may wala kasi dahil tapos pag sana ayan eto ayun " +
            "project plan note notes deal meeting team client customer boss"
        ).split(' ').toSet()

    private val QUOTED = Regex("""(?:^|[\s(])['"“‘]([^'"“”‘’]{4,60})['"”’](?=$|[\s.,;:!?)])""")
    private val CAPITALISED = Regex("""\p{Lu}\p{L}{2,}""")
    private val WORD = Regex("""\p{L}[\p{L}\d'-]*""")

    private fun stem(w: String) = if (w.length > 4 && w.endsWith("s") && !w.endsWith("ss")) w.dropLast(1) else w
    private fun wordsOf(s: String): List<String> =
        WORD.findAll(s.lowercase()).map { stem(it.value.removeSuffix("'s")) }.toList()   // "Jakim's" -> "jakim"

    /** A note with several anchors (e.g. a person AND a place) needs up to 2 of them to appear in the draft. */
    fun needed(anchors: List<Anchor>): Int = minOf(2, anchors.size)

    fun of(text: String): List<Anchor> {
        val anchors = ArrayList<Anchor>()
        val seen = HashSet<String>()
        for (m in QUOTED.findAll(text)) {
            val phrase = m.groupValues[1]
            val words = wordsOf(phrase).filter { it.length >= 4 && it !in STOP }.toSet()
            if (words.isNotEmpty()) anchors += Anchor(words, minOf(2, words.size), phrase.trim())
        }
        for (m in CAPITALISED.findAll(text)) {
            val w = stem(m.value.lowercase())
            if (w in STOP || m.value.lowercase() in STOP || !seen.add(w)) continue
            anchors += Anchor(setOf(w), 1, m.value)
        }
        return anchors
    }

    /** The anchors (as the user wrote them) that appear in [draft]. */
    fun hits(anchors: List<Anchor>, draft: String): List<String> {
        if (anchors.isEmpty()) return emptyList()
        val words = wordsOf(draft).toSet()
        return anchors.filter { a -> a.words.count { it in words } >= a.need }.map { it.shown }
    }
}
