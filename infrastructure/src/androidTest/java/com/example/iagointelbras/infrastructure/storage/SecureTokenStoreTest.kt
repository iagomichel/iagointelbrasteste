package com.example.iagointelbras.infrastructure.storage

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class SecureTokenStoreTest {
    private lateinit var preferences: android.content.SharedPreferences
    private lateinit var store: SecureTokenStore

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        preferences = context.getSharedPreferences("secure_session", Context.MODE_PRIVATE)
        store = SecureTokenStore(context)
        store.clear()
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun storesTokenAsCiphertextAndReadsItBack() {
        val token = "secure-session-test-token"

        store.write(token)

        assertEquals(token, store.read())
        val ciphertext = preferences.getString("access_token", null)
        assertNotNull(ciphertext)
        assertFalse(ciphertext.orEmpty().contains(token))
    }

    @Test
    fun clearRemovesStoredToken() {
        store.write("secure-session-test-token")

        store.clear()

        assertEquals(null, store.read())
        assertEquals(null, preferences.getString("access_token", null))
        assertFalse(KeyStore.getInstance("AndroidKeyStore").run {
            load(null)
            containsAlias("casa_inteligente_session_key")
        })
    }
}



