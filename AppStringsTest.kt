package com.example.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStringsTest {

    @Test
    fun allThreeLanguages_instantiateAndFormatCleanly() {
        val en = stringsFor(AppLanguage.ENGLISH)
        val hi = stringsFor(AppLanguage.HINDI)
        val gu = stringsFor(AppLanguage.GUJARATI)

        assertEquals("Krishna Travels", en.appTitle)
        assertEquals("कृष्णा ट्रैवल्स", hi.appTitle)
        assertEquals("કૃષ્ણા ટ્રાવેલ્સ", gu.appTitle)

        assertEquals("Owner", en.roleLabel("owner"))
        assertEquals("मैनेजर", hi.roleLabel("manager"))
        assertEquals("માલિક", gu.roleLabel("owner"))

        assertEquals("Scheduled", en.tripStatusLabel("scheduled"))
        assertEquals("पूर्ण", hi.tripStatusLabel("completed"))
        assertEquals("ચાલુ છે", gu.tripStatusLabel("in_progress"))

        assertEquals("Diesel", en.expenseCategoryLabel("Diesel"))
        assertEquals("डीजल", hi.expenseCategoryLabel("Diesel"))
        assertEquals("ડીઝલ", gu.expenseCategoryLabel("Diesel"))

        assertEquals("12/30 Cabins", en.cabinsRatio(12, 30))
        assertTrue(hi.welcomeUser("राज").contains("राज"))
        assertFalse(gu.msgOwnerOnlyError.isBlank())
    }
}
