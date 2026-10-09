package com.secondlook.ai

/** What kind of risk was found. Keep to a few headline categories for the demo. */
enum class RiskCategory {
    PASSWORD,
    API_KEY,        // API keys, access tokens, JWTs, private keys, connection strings with passwords
    CARD,
    PRIVATE_NOTE,   // match against something the user taught ("Teach me what's private"): a fact or a secret value
    OTHER_PII,      // email, phone, government ID, anything else the PII model flags
}

/** HIGH = red banner ("this is a secret"). MEDIUM = soft yellow banner ("Intended?"): phone number, email, bank account. */
enum class Severity { HIGH, MEDIUM }

/**
 * The single hand-over format between the AI core and the keyboard/Flutter UI. See docs/contract.md.
 * [kind] is the fine-grained type the UI keys its strings on:
 * password | otp | api_key | credit_card | gov_id | email | phone | bank_account | pii | private_match | my_secret
 * (my_secret = a value the user taught, e.g. their MPIN; private_match = a private fact matched by meaning / names)
 */
data class RiskResult(
    val category: RiskCategory,
    val start: Int,                 // inclusive char offset in the checked text
    val end: Int,                   // exclusive char offset
    val reason: String,             // short, user-facing, cautious wording (English)
    val confidence: Float,          // 0..1
    val noteLabel: String? = null,  // only for PRIVATE_NOTE, e.g. "Project Sunflower" or "GCash MPIN"
    val kind: String = category.name.lowercase(),
    val severity: Severity = Severity.HIGH,
)
