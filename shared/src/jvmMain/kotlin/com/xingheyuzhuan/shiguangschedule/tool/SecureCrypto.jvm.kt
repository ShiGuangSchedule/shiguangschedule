package com.xingheyuzhuan.shiguangschedule.tool

import com.xingheyuzhuan.shiguangschedule.data.di.AppStorage
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Single
actual class SecureCrypto : KoinComponent {

    private val transformation = "AES/GCM/NoPadding"
    private val keyStoreType = "PKCS12"
    private val alias = "ShiguangApiCryptoKeyAlias"
    private val keyStorePassword = "ShiguangScheduleStorePassword".toCharArray()

    private val appStorage: AppStorage by inject()

    private val keyStoreFile: File by lazy {
        File(appStorage.filesDir.toFile(), "keystore.p12").also { file ->
            file.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) {
                    error("无法创建 keystore 目录: ${parent.absolutePath}")
                }
            }
        }
    }

    @Volatile
    private var cachedKey: SecretKey? = null

    @Synchronized
    private fun getSecretKey(): SecretKey {
        cachedKey?.let { return it }

        val keyStore = KeyStore.getInstance(keyStoreType)
        if (keyStoreFile.exists()) {
            keyStoreFile.inputStream().use { keyStore.load(it, keyStorePassword) }
        } else {
            keyStore.load(null, keyStorePassword)
        }

        val existing = (keyStore.getEntry(alias, KeyStore.PasswordProtection(keyStorePassword))
                as? KeyStore.SecretKeyEntry)?.secretKey
        val key = existing ?: run {
            val newKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
            keyStore.setEntry(
                alias,
                KeyStore.SecretKeyEntry(newKey),
                KeyStore.PasswordProtection(keyStorePassword)
            )
            saveKeyStore(keyStore)
            newKey
        }
        cachedKey = key
        return key
    }

    private fun saveKeyStore(keyStore: KeyStore) {
        val tmp = File(keyStoreFile.parentFile, "${keyStoreFile.name}.tmp")
        tmp.outputStream().use { keyStore.store(it, keyStorePassword) }
        if (!tmp.renameTo(keyStoreFile)) {
            tmp.copyTo(keyStoreFile, overwrite = true)
            tmp.delete()
        }
    }

    actual fun encrypt(data: String): CryptoResult? {
        if (data.isEmpty()) return null
        return try {
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            CryptoResult(
                encryptedData = Base64.getEncoder().encodeToString(encryptedBytes),
                iv = Base64.getEncoder().encodeToString(cipher.iv)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun decrypt(encryptedData: String, ivString: String): String? {
        if (encryptedData.isEmpty() || ivString.isEmpty()) return null
        return try {
            val cipher = Cipher.getInstance(transformation)
            val ivBytes = Base64.getDecoder().decode(ivString)
            val gcmSpec = GCMParameterSpec(128, ivBytes)

            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmSpec)
            val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData))
            String(decryptedBytes, Charsets.UTF_8)
                .replace("\u0000", "")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}