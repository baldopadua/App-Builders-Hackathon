package com.secondlook.ai

import com.secondlook.SecretManager
import com.secondlook.SecretNote
import com.secondlook.WarningType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleBasedAiAdapterTest {

    private lateinit var adapter: RuleBasedAiAdapter

    @Before
    fun setUp() {
        adapter = RuleBasedAiAdapter()
        SecretManager.clearSecretsForTesting()
    }

    @Test
    fun testPasswordWithSpecialCharsFullyMasked() {
        val input = "password: mango72-sss"
        val warning = adapter.detectSync(input)
        assertNotNull("Should detect password warning", warning)
        assertEquals(WarningType.HIGH, warning!!.type)
        assertEquals("mango72-sss", warning.matchedText)
        assertEquals(10, warning.start)
        assertEquals(21, warning.end)

        val masked = adapter.hide(input, warning)
        assertEquals("password: •••••••••••", masked)
    }

    @Test
    fun testPasswordHunter2() {
        val input = "password: hunter2"
        val warning = adapter.detectSync(input)
        assertNotNull(warning)
        assertEquals("hunter2", warning!!.matchedText)
        assertEquals(10, warning.start)
        assertEquals(17, warning.end)

        val masked = adapter.hide(input, warning)
        assertEquals("password: •••••••", masked)
    }

    @Test
    fun testPinMasking() {
        val input = "PIN ko 4821"
        val warning = adapter.detectSync(input)
        assertNotNull("Should detect PIN", warning)
        assertEquals("4821", warning!!.matchedText)
        assertEquals(7, warning.start)
        assertEquals(11, warning.end)

        val masked = adapter.hide(input, warning)
        assertEquals("PIN ko ••••", masked)
    }

    @Test
    fun testPinWithPrecedingNumberNotMaskingAmount() {
        val input = "May 2500 ako, PIN ko 4821"
        val warning = adapter.detectSync(input)
        assertNotNull("Should detect PIN", warning)
        assertEquals("4821", warning!!.matchedText)
        assertEquals(21, warning.start)
        assertEquals(25, warning.end)

        val masked = adapter.hide(input, warning)
        assertEquals("May 2500 ako, PIN ko ••••", masked)
    }

    @Test
    fun testOtpSurroundedByText() {
        val input = "OTP ko ay 482913 at ok"
        val warning = adapter.detectSync(input)
        assertNotNull("Should detect OTP", warning)
        assertEquals("482913", warning!!.matchedText)
        assertEquals(10, warning.start)
        assertEquals(16, warning.end)

        val masked = adapter.hide(input, warning)
        assertEquals("OTP ko ay •••••• at ok", masked)
    }

    @Test
    fun testBareOtpCode() {
        val input = "482913"
        val warning = adapter.detectSync(input)
        assertNotNull("Should detect bare OTP", warning)
        assertEquals("482913", warning!!.matchedText)
        assertEquals(0, warning.start)
        assertEquals(6, warning.end)

        val masked = adapter.hide(input, warning)
        assertEquals("••••••", masked)
    }

    @Test
    fun testApiKeyStripeLiveFullyMasked() {
        val input = "sk_live_ABC123XYZ4567890abcdef"
        val warning = adapter.detectSync(input)
        assertNotNull("Should detect API key", warning)
        assertEquals(input, warning!!.matchedText)
        assertEquals(0, warning.start)
        assertEquals(input.length, warning.end)

        val masked = adapter.hide(input, warning)
        assertEquals("•".repeat(input.length), masked)
    }

    @Test
    fun testMultiWordPassword() {
        val input = "pw is 'hunter2 extra'"
        val warning = adapter.detectSync(input)
        assertNotNull("Should detect quoted multi-word password", warning)
        assertEquals("hunter2 extra", warning!!.matchedText)
        assertEquals(7, warning.start)
        assertEquals(20, warning.end)

        val masked = adapter.hide(input, warning)
        assertEquals("pw is '•••••••••••••'", masked)
    }

    @Test
    fun testPasswordWithQuotes() {
        val input = "password: 'mango72-sss'"
        val warning = adapter.detectSync(input)
        assertNotNull("Should detect quoted password", warning)
        assertEquals("mango72-sss", warning!!.matchedText)
        assertEquals(11, warning.start)
        assertEquals(22, warning.end)

        val masked = adapter.hide(input, warning)
        assertEquals("password: '•••••••••••'", masked)
    }

    @Test
    fun testAlreadyMaskedTextDoesNotTriggerWarning() {
        val maskedText = "password: •••••••••••"
        val warning = adapter.detectSync(maskedText)
        assertNull("Already masked text should not trigger new warning", warning)
    }

    @Test
    fun testHideFallbackWithoutWarningParameter() {
        val input = "password: mango72-sss"
        val masked = adapter.hide(input, null)
        assertEquals("password: •••••••••••", masked)
    }

    @Test
    fun testTaughtVaultSecretMasking() {
        SecretManager.addSecretForTesting(
            SecretNote(
                id = "note_1",
                label = "GCash MPIN",
                value = "9876",
                createdAt = System.currentTimeMillis()
            )
        )

        val input = "Ang code ko ay 9876 mamaya"
        val warning = adapter.detectSync(input)
        assertNotNull("Should detect taught vault secret", warning)
        assertEquals("9876", warning!!.matchedText)
        assertEquals(15, warning.start)
        assertEquals(19, warning.end)

        val masked = adapter.hide(input, warning)
        assertEquals("Ang code ko ay •••• mamaya", masked)
    }

    @Test
    fun testComplexPasswordWithoutLabelCensorsEntireWord() {
        val input = "@!$\"-*sss"
        val warning = adapter.detectSync(input)
        assertNotNull("Should detect complex secret string", warning)
        assertEquals(input, warning!!.matchedText)
        assertEquals(0, warning.start)
        assertEquals(input.length, warning.end)

        val masked = adapter.hide(input, warning)
        assertEquals("•••••••••", masked)
    }

    @Test
    fun testWordMaskerExpandsPartialSubSpanToFullWord() {
        // User's exact scenario: detector flagged only the '$' at [2, 3) inside "@!$"-*sss"
        val input = "@!$\"-*sss"
        val partialWarning = com.secondlook.Warning(
            type = WarningType.HIGH,
            headline = "Teka! Password ito",
            tip = "Password detected",
            start = 2,
            end = 3,
            matchedText = "$"
        )
        // If an engine originally returned "@!•"-*sss"
        val fullMasked = WordMasker.ensureFullWordMask(input, "@!•\"-*sss", partialWarning)
        assertEquals("•••••••••", fullMasked)
    }

    @Test
    fun testWordMaskerExpandsPartialPasswordWithSuffix() {
        // User's previous scenario: detector flagged only "mango72" leaving "'-sss" exposed
        val input = "password: mango72-sss"
        val partialWarning = com.secondlook.Warning(
            type = WarningType.HIGH,
            headline = "Teka! Password ito",
            tip = "Password detected",
            start = 10,
            end = 17,
            matchedText = "mango72"
        )
        val fullMasked = WordMasker.ensureFullWordMask(input, "password: ••••••'-sss", partialWarning)
        assertEquals("password: •••••••••••", fullMasked)
    }

    @Test
    fun testDetectionEngineHideExpandsPartialMask() {
        val input = "@!$\"-*sss"
        val partialWarning = com.secondlook.Warning(
            type = WarningType.HIGH,
            headline = "Teka! Password ito",
            tip = "Password detected",
            start = 2,
            end = 3,
            matchedText = "$"
        )
        val result = com.secondlook.DetectionEngine.hide(input, partialWarning)
        assertEquals("•••••••••", result)
    }

    @Test
    fun testStandaloneMaskedDotsDoesNotTriggerWarning() {
        val warning = adapter.detectSync("•••••••••")
        assertNull("Masked bullets should never re-trigger detection", warning)
    }

    @Test
    fun testComplexPasswordInSentenceCensorsOnlyThatWord() {
        val input = "eto ang pass: @!$\"-*sss mamaya"
        val masked = adapter.hide(input, null)
        assertEquals("eto ang pass: ••••••••• mamaya", masked)
    }

    @Test
    fun testPinWithPrecedingAmountDoesNotMaskAmount() {
        val input = "May 2500 ako, PIN ko 4821"
        val masked = adapter.hide(input, null)
        assertEquals("May 2500 ako, PIN ko ••••", masked)
    }
}
