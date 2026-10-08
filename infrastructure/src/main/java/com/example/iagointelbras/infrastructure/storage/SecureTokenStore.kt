package com.example.iagointelbras.infrastructure.storage

import android.annotation.SuppressLint
import android.content.SharedPreferences
import com.example.iagointelbras.domain.repository.TokenStore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureTokenStore @Inject constructor(@ApplicationContext context: Context) : TokenStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun read(): String? {
        val encrypted = preferences.getString(TOKEN_KEY, null) ?: return null
        return try {
            encrypted.split(SEPARATOR).takeIf { it.size == 2 }?.let { (iv, data) -> decrypt(iv, data) }
        } catch (_: Exception) {
            runCatching(::clear)
            null
        }
    }

    override fun write(token: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val iv = cipher.iv.encode()
        val data = cipher.doFinal(token.toByteArray(Charsets.UTF_8)).encode()
        check(preferences.editAndCommit { putString(TOKEN_KEY, "$iv$SEPARATOR$data") })
    }

    override fun clear() {
        check(preferences.editAndCommit { remove(TOKEN_KEY) })
        runCatching {
            KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }.deleteEntry(KEY_ALIAS)
        }
    }

    private fun decrypt(iv: String, data: String): String = Cipher.getInstance(TRANSFORMATION).run {
        init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_LENGTH, iv.decode()))
        doFinal(data.decode()).toString(Charsets.UTF_8)
    }

    private fun key(): SecretKey = (KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        .getKey(KEY_ALIAS, null) as? SecretKey) ?: generateKey()

    private fun generateKey(): SecretKey = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
        init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        generateKey()
    }

    private fun ByteArray.encode() = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.decode() = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val PREFERENCES = "secure_session"
        const val TOKEN_KEY = "access_token"
        const val KEY_ALIAS = "casa_inteligente_session_key"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH = 128
        const val SEPARATOR = "."
    }
}

@SuppressLint("UseKtx")
private inline fun SharedPreferences.editAndCommit(action: SharedPreferences.Editor.() -> Unit): Boolean =
    edit().apply(action).commit()
