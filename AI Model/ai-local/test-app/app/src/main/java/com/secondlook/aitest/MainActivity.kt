package com.secondlook.aitest

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.secondlook.ai.Redactor
import com.secondlook.ai.RiskCategory
import com.secondlook.ai.RiskResult
import com.secondlook.ai.SafetyTips
import com.secondlook.ai.SecondLookChecker
import com.secondlook.ai.Severity
import com.secondlook.ai.android.AssetModels
import com.secondlook.ai.android.EncryptedNotesStore
import com.secondlook.ai.model.PiiDetector
import com.secondlook.ai.notes.KIND_SECRET
import com.secondlook.ai.notes.NoteLimitException
import com.secondlook.ai.notes.NoteMatcher
import com.secondlook.ai.notes.TaughtSecretMatcher
import com.secondlook.ai.rules.RuleDetector
import java.util.concurrent.Executors

/**
 * Test harness for the ai-local core. Type in the box and the checker runs after a short pause, on a background
 * thread, exactly as the keyboard will call it. Everything is local; this app declares no permissions.
 * All sample text is synthetic.
 */
class MainActivity : Activity() {

    private val worker = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())
    @Volatile private var version = 0
    @Volatile private var checker: SecondLookChecker? = null
    @Volatile private var pii: PiiDetector? = null
    @Volatile private var matcher: NoteMatcher? = null
    @Volatile private var store: EncryptedNotesStore? = null

    private lateinit var status: TextView
    private lateinit var input: EditText
    private lateinit var verdict: TextView
    private lateinit var preview: TextView
    private lateinit var details: TextView
    private lateinit var debug: TextView
    private lateinit var noteText: EditText
    private lateinit var noteLabel: EditText
    private lateinit var notesBox: LinearLayout
    private lateinit var thresholdLabel: TextView
    private lateinit var tipView: TextView
    private lateinit var hideBtn: Button
    private var shownText: String? = null       // what the verdict on screen is about, for "Itago"
    private var shownResult: RiskResult? = null

    private val checkRunnable = Runnable { runCheck() }

    private val samples = listOf(   // all synthetic. Everyday Filipinos first; developer secrets last (secondary)
        "GCash MPIN" to "GCash MPIN ko ay 4821",
        "Passcode" to "Passcode ko 0517, ikaw na bahala",
        "No clue words (teach 4821 first)" to "eto na yung 4821, bilisan mo",
        "Lola: OTP" to "Anak, eto yung code na pinadala sa akin: 482913",
        "\"Premyo\" scam" to "Ma'am eto po yung OTP: 902114, para ma-claim ko na yung premyo",
        "GCash ref no. (quiet)" to "Bayad na po via GCash. Ref No. 1023 4567 89016, salamat!",
        "ATM PIN" to "PIN ng ATM ko 1234, pakikuha mo na lang yung pera",
        "Kid: game password" to "Password ng ML account ko: Lodi1234",
        "FB password" to "pass ng FB ko: Kitty123",
        "TIN" to "TIN ko: 123-456-789-000",
        "Card + CVV" to "card: 5555555555554444 cvv 123",
        "GCash number" to "Pa-GCash na lang sa 09171234567, salamat!",
        "Bank account" to "BDO account no. 0012345678, Juan Dela Cruz",
        "Harmless" to "Kumain ka na ba? Ingat pauwi!",
        "Jakim (save note first)" to "Let's raid Jakim in Taguig",
        "No names (save note first)" to "Prices will go up by 20% next month",
        "Dev: Gemini key" to "GEMINI_API_KEY=AIzaSyA1b2C3d4E5f6G7h8I9j0KlMnOpQrStUvW",
        "Dev: SSH key" to "Eto yung SSH key ng server: -----BEGIN OPENSSH PRIVATE KEY-----",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        loadModels()
    }

    override fun onDestroy() {
        ui.removeCallbacks(checkRunnable)
        worker.shutdown()
        super.onDestroy()
    }

    // ---- models ---------------------------------------------------------------------------------------

    private fun loadModels() {
        worker.execute {
            try {
                val t0 = SystemClock.elapsedRealtime()
                val p = AssetModels.piiDetector(this)
                val t1 = SystemClock.elapsedRealtime()
                val e = AssetModels.embeddingModel(this)
                val t2 = SystemClock.elapsedRealtime()
                val s = EncryptedNotesStore(this, e)
                val m = NoteMatcher(e, s, threshold = 0.20f + thresholdBar.progress / 100f)
                pii = p; store = s; matcher = m
                checker = SecondLookChecker(RuleDetector(), p, m, TaughtSecretMatcher(s))
                ui.post {
                    status.text = "Models loaded from the APK: PII ${t1 - t0} ms, embeddings ${t2 - t1} ms.\n" + permissionLine()
                    status.setTextColor(Color.parseColor("#1B5E20"))
                    refreshNotes()
                    scheduleCheck()
                }
            } catch (t: Throwable) {
                ui.post {
                    status.text = "Model load FAILED: ${t.javaClass.simpleName}: ${t.message}"
                    status.setTextColor(Color.parseColor("#B71C1C"))
                }
            }
        }
    }

    private fun permissionLine(): String {
        @Suppress("DEPRECATION")
        val perms = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions
        val internet = perms?.contains("android.permission.INTERNET") == true
        return if (internet) "INTERNET permission: DECLARED (should not be!)" else "INTERNET permission: not declared"
    }

    // ---- checking -------------------------------------------------------------------------------------

    private fun scheduleCheck() {
        ui.removeCallbacks(checkRunnable)
        ui.postDelayed(checkRunnable, 250)
    }

    private fun runCheck() {
        val c = checker ?: return
        val text = input.text.toString()
        val v = ++version
        worker.execute {
            val t0 = SystemClock.elapsedRealtime()
            val report = c.checkDetailed(text)
            val total = SystemClock.elapsedRealtime() - t0
            val ents = pii?.entities(text).orEmpty()
            val scores = matcher?.scores(text).orEmpty()
            if (v == version) ui.post { render(text, report, ents, scores, total) }
        }
    }

    private fun render(
        text: String, r: SecondLookChecker.Report, ents: List<PiiDetector.Entity>,
        scores: List<NoteMatcher.Score>, totalMs: Long,
    ) {
        val result = r.result
        shownText = text
        shownResult = result
        tipView.text = if (result != null) "💡 " + SafetyTips.forKind(result.kind) else ""
        hideBtn.visibility = if (result != null && result.kind != "private_match") View.VISIBLE else View.GONE
        if (text.isBlank()) {
            verdict.text = "Type a message above, or tap a sample."
            verdict.setTextColor(Color.GRAY)
            preview.text = ""
        } else if (result == null) {
            verdict.text = "✓ No risk found"
            verdict.setTextColor(Color.parseColor("#1B5E20"))
            preview.text = text
        } else {
            val name = when (result.category) {
                RiskCategory.PASSWORD -> "PASSWORD / CODE"
                RiskCategory.API_KEY -> "API KEY / TOKEN"
                RiskCategory.CARD -> "CARD"
                RiskCategory.PRIVATE_NOTE -> "PRIVATE NOTE"
                RiskCategory.OTHER_PII -> "PERSONAL INFO"
            }
            val soft = result.severity == Severity.MEDIUM
            verdict.text = "${if (soft) "🟡" else "⚠"} ${SafetyTips.headline(result.kind, result.noteLabel)}\n${result.reason}  ·  $name ${"%.2f".format(result.confidence)}"
            verdict.setTextColor(Color.parseColor(if (soft) "#9A5B00" else "#B71C1C"))
            val s = SpannableString(text)
            val a = result.start.coerceIn(0, text.length)
            val b = result.end.coerceIn(a, text.length)
            s.setSpan(BackgroundColorSpan(Color.parseColor("#FFCDD2")), a, b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            preview.text = s
        }

        details.text = (if (r.stage != null) "Decided by: ${r.stage}\n" else "") +
            r.stages.joinToString("\n") { st ->
                "• ${st.name}: ${st.result?.let { "HIT ${it.category} ${"%.2f".format(it.confidence)}" } ?: "no hit"} (${st.millis} ms)"
            } + "\nAll stages: $totalMs ms"

        val th = matcher?.threshold ?: 0f
        debug.text = "PII model entities (all, before filtering):\n" +
            (if (ents.isEmpty()) "  none" else ents.joinToString("\n") { "  ${it.type} \"${it.text}\" ${"%.2f".format(it.score)}" }) +
            "\n\nPrivate-note similarity (threshold ${"%.2f".format(th)}):\n" +
            (if (scores.isEmpty()) "  no notes saved, or text too short" else scores.joinToString("\n") {
                "  ${it.noteLabel}: ${"%.3f".format(it.score)}" + if (it.anchors.isNotEmpty()) "  [private word in draft: ${it.anchors.joinToString()}]" else ""
            })
    }

    // ---- notes ----------------------------------------------------------------------------------------

    private fun refreshNotes() {
        val s = store ?: return
        worker.execute {
            val list = s.list()
            ui.post {
                notesBox.removeAllViews()
                if (list.isEmpty()) notesBox.addView(text("Nothing taught yet.", 13f, color = Color.GRAY))
                for (n in list) {
                    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                    val shown = if (n.kind == KIND_SECRET) "🔑 ${n.label}  (secret value, fingerprint only)" else "🔒 ${n.label}  (private fact)"
                    row.addView(text(shown, 15f, true), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
                    row.addView(button("Delete") {
                        worker.execute { s.delete(n.id); ui.post { refreshNotes(); scheduleCheck() } }
                    })
                    notesBox.addView(row)
                }
            }
        }
    }

    private fun saveNote() {
        val s = store ?: return toast("Models are still loading")
        val t = noteText.text.toString().trim()
        if (t.length < 4) return toast("Type a private fact, or one secret value such as your MPIN")
        val label = noteLabel.text.toString().trim()
        worker.execute {
            try {
                val saved = s.save(t, label)
                val how = if (saved.kind == KIND_SECRET) "secret value: only a fingerprint is kept" else "private fact, encrypted on this phone"
                ui.post { noteText.setText(""); noteLabel.setText(""); refreshNotes(); scheduleCheck(); toast("Saved ($how)") }
            } catch (e: NoteLimitException) {
                ui.post { toast(e.message ?: "Limit reached") }
            } catch (e: Throwable) {
                ui.post { toast("Could not save: ${e.message}") }
            }
        }
    }

    // ---- UI -------------------------------------------------------------------------------------------

    private lateinit var thresholdBar: SeekBar

    private fun buildUi() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(24))
        }
        col.addView(text("SecondLook AI test", 22f, true))
        col.addView(text("Local checker only. Synthetic text, please.", 13f, color = Color.GRAY))

        status = text("Loading models…", 13f, color = Color.DKGRAY)
        col.addView(status)

        input = EditText(this).apply {
            hint = "Type a message…"
            minLines = 3
            gravity = android.view.Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) = scheduleCheck()
            })
        }
        col.addView(input, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(8) })

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((name, t) in samples) row.addView(button(name) { input.setText(t); input.setSelection(t.length) })
        col.addView(HorizontalScrollView(this).apply { addView(row); isHorizontalScrollBarEnabled = false })

        val result = card()
        verdict = text("Type a message above, or tap a sample.", 16f, true, Color.GRAY)
        preview = text("", 14f)
        details = text("", 12f, color = Color.DKGRAY)
        tipView = text("", 13f, color = Color.DKGRAY)
        hideBtn = button("🙈 Itago (hide it)") {
            val t = shownText
            val r = shownResult
            if (t != null && r != null && input.text.toString() == t) {   // only hide what is on screen
                Redactor.hide(t, r)?.let { masked -> input.setText(masked); input.setSelection(masked.length) }
            }
        }.apply { visibility = View.GONE }
        result.addView(verdict); result.addView(tipView); result.addView(hideBtn); result.addView(preview); result.addView(details)
        col.addView(result)

        // Private notes ("Teach me what's private")
        val notes = card()
        notes.addView(text("Teach me what's private", 16f, true))
        notes.addView(text("Up to 5. One secret value (your MPIN, passcode, password, API key) is kept only as a fingerprint and caught anywhere, even with no clue words. A sentence is a private fact, encrypted and compared by meaning + names. Delete anytime.", 12f, color = Color.DKGRAY))
        noteText = EditText(this).apply { hint = "e.g. 4821   or   Si Jakim ay nakatira sa Taguig"; minLines = 2 }
        noteLabel = EditText(this).apply { hint = "Short label shown in warnings, e.g. GCash MPIN"; setSingleLine() }
        notes.addView(noteText); notes.addView(noteLabel)
        val btns = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        btns.addView(button("Save") { saveNote() })
        btns.addView(button("Fill MPIN example (4821)") {
            noteText.setText("4821")
            noteLabel.setText("GCash MPIN")
        })
        btns.addView(button("Fill Sunflower example") {
            noteText.setText("Our client is buying the bakery next door. The project is called Sunflower.")
            noteLabel.setText("Project Sunflower")
        })
        notes.addView(btns)
        val btns2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        btns2.addView(button("Fill Jakim example") {
            noteText.setText("Si Jakim ay nakatira sa Taguig")
            noteLabel.setText("Jakim location")
        })
        btns2.addView(button("Fill price example (no names)") {
            noteText.setText("We are raising our prices by 20 percent next month. Do not tell customers yet.")
            noteLabel.setText("Price increase")
        })
        notes.addView(HorizontalScrollView(this).apply { addView(btns2); isHorizontalScrollBarEnabled = false })
        notesBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        notes.addView(notesBox)
        notes.addView(button("Delete ALL notes") {
            val s = store ?: return@button
            worker.execute { s.deleteAll(); ui.post { refreshNotes(); scheduleCheck(); toast("All notes deleted") } }
        })
        thresholdLabel = text("", 13f)
        thresholdBar = SeekBar(this).apply {
            max = 70; progress = 55   // 0.20 .. 0.90, default 0.75
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    val th = 0.20f + p / 100f
                    thresholdLabel.text = "Similarity needed when no private word appears: ${"%.2f".format(th)}  (lower = warns more)"
                    matcher?.threshold = th
                    if (fromUser) scheduleCheck()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        thresholdLabel.text = "Similarity needed when no private word appears: 0.75  (lower = warns more)"
        notes.addView(thresholdLabel); notes.addView(thresholdBar)
        col.addView(notes)

        val dbg = card()
        dbg.addView(text("Debug", 14f, true))
        debug = text("", 12f, color = Color.DKGRAY)
        dbg.addView(debug)
        col.addView(dbg)

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            isFillViewport = true
            addView(col)
        })
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun text(t: String, size: Float, bold: Boolean = false, color: Int = Color.parseColor("#212121")) =
        TextView(this).apply {
            text = t; textSize = size; setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun button(t: String, onClick: () -> Unit) = Button(this).apply {
        text = t; isAllCaps = false; setOnClickListener { onClick() }
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = GradientDrawable().apply {
            setColor(Color.WHITE); cornerRadius = dp(10).toFloat(); setStroke(dp(1), Color.parseColor("#DDDDDD"))
        }
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(12) }
    }
}
