package com.secondlook.ai

/**
 * Concise, factual headlines and advice aligned with TANGGOLIN_MOBILE_UI_DESIGN_REVIEW.md
 * Keeps actions clear and avoids preachy or sensationalist language.
 */
object SafetyTips {
    /** Short, concrete headline for the warning banner. [label] = the saved note's label. */
    fun headline(kind: String, label: String? = null): String = when (kind) {
        "my_secret" -> if (label.isNullOrBlank()) "Matches a saved private note." else "Matches private note: $label."
        "password" -> "Password detected."
        "otp" -> "OTP verification code detected."
        "api_key" -> "API key detected."
        "ssh_key" -> "SSH private key detected."
        "db_credential" -> "Server credentials detected."
        "credit_card" -> "Payment card details detected."
        "gov_id" -> "Government ID detected."
        "bank_account" -> "Bank account number detected."
        "phone" -> "Phone number detected."
        "email" -> "Email address detected."
        "private_match" -> "Private details detected."
        else -> "Sensitive detail detected."
    }

    /** Direct action consequence: answers what the user can do immediately. */
    fun forKind(kind: String): String = when (kind) {
        "password", "otp", "api_key", "ssh_key", "db_credential", "credit_card", "gov_id", "my_secret", "private_match" ->
            "Mask it before sharing."
        "bank_account", "phone", "email" ->
            "Check this detail before sending."
        else ->
            "Mask it before sharing."
    }
}
