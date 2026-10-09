package com.secondlook.ai

/**
 * Concise advice under each warning: explains the risk and recommends action.
 */
object SafetyTips {
    /** Short headline for the warning banner. [label] = the taught secret's label. */
    fun headline(kind: String, label: String? = null): String = when (kind) {
        "my_secret" -> if (label.isNullOrBlank()) "Wait! This matches a secret you saved." else "Wait! This matches your saved secret: $label."
        "password" -> "Careful! This looks like a password."
        "otp" -> "Wait! This is an OTP or PIN — never share this."
        "api_key" -> "Caution — this looks like an API key."
        "credit_card" -> "Careful! These are payment card details."
        "gov_id" -> "Careful! Government ID number detected."
        "bank_account" -> "Bank account number detected. Intended?"
        "phone" -> "Phone number detected. Intended?"
        "email" -> "Email address detected. Intended?"
        "private_match" -> "This is marked private. Are you sure you want to send it?"
        else -> "Take a second look before sending."
    }

    fun forKind(kind: String): String = when (kind) {
        "password" -> "Never send passwords over chat. If already sent, change it immediately."
        "otp" -> "Legitimate banks and apps will never ask for your one-time PIN. Sharing this may compromise your account."
        "api_key" -> "Anyone in this chat could use this key. If exposed, revoke it and generate a new key."
        "credit_card" -> "Card numbers and security codes should never be shared over unencrypted messages."
        "gov_id" -> "Government IDs can be exploited for identity theft and fraud. Only share via verified channels."
        "bank_account" -> "Double check that you trust the recipient before sending banking details."
        "phone", "email" -> "Make sure you intended to share personal contact details with this recipient."
        "private_match" -> "You marked this as private. Ensure you want the recipient to see this information."
        "my_secret" -> "You saved this secret in your vault. Tap Hide to mask it before sending."
        else -> "Review sensitive details carefully before sending."
    }
}
