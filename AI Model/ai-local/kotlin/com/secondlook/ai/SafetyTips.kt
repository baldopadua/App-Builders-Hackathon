package com.secondlook.ai

/**
 * One short, Filipino-friendly line of advice under each warning: what the risk is and what to do.
 * Defaults only; the app's strings.xml (Jakim) may override by kind.
 * JS twin: web-demo/public/core.js (SAFETY_TIPS). Keep in sync.
 */
object SafetyTips {
    /** Short Filipino-first headline for the banner: what a lola or a kid reads first. [label] = the taught secret's label. */
    fun headline(kind: String, label: String? = null): String = when (kind) {
        "my_secret" -> if (label.isNullOrBlank()) "Teka! Ito yung sikretong tinuro mo." else "Teka! Ito yung tinuro mong sikreto: $label."
        "password" -> "Ingat! Mukhang password ito."
        "otp" -> "Teka! OTP o PIN ito — huwag ibigay kahit kanino."
        "api_key" -> "Teka lang — mukhang API key ito."
        "credit_card" -> "Ingat! Detalye ng card ito."
        "gov_id" -> "Ingat! Government ID number ito."
        "bank_account" -> "Account number ito. Sinadya mo ba?"
        "phone" -> "Phone number ito. Sinadya mo ba?"
        "email" -> "Email address ito. Sinadya mo ba?"
        "private_match" -> "Pribado mo ito. Sigurado ka bang ise-send?"
        else -> "Teka lang, silipin muna."
    }

    fun forKind(kind: String): String = when (kind) {
        "password" -> "Huwag i-send ang password sa chat. Kung na-send na, palitan agad."
        "otp" -> "Walang totoong bangko, GCash o Maya na hihingi ng OTP o PIN mo. Kapag may humingi, scam iyan."
        "api_key" -> "Kahit sino sa chat ay puwedeng gumamit ng key na ito. Kung na-send na, i-revoke at gumawa ng bago."
        "credit_card" -> "Hindi kailangan ang card number o CVV para makatanggap ng bayad."
        "gov_id" -> "Ginagamit ang TIN, SSS at PhilSys sa identity theft at loan scams. I-send lang sa opisyal na channel."
        "bank_account" -> "Okay lang kung pambayad ito. Siguraduhing tama ang taong padadalhan mo."
        "phone", "email" -> "Okay lang kung sinadya. Siguraduhing kilala mo ang kausap mo."
        "private_match" -> "Minarkahan mo itong pribado. Siguraduhing okay lang na malaman ito ng kausap mo."
        "my_secret" -> "Ikaw mismo ang nagturo na sikreto ito. Pindutin ang Itago bago i-send."
        else -> "Silipin muna bago i-send."
    }
}
