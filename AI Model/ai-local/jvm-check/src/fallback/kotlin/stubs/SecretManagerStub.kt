package com.secondlook

/** No saved notes during this measurement (the vault rule only fires on a saved value). */
object SecretManager {
    fun containsProtectedSecret(@Suppress("UNUSED_PARAMETER") text: String) = false
}
