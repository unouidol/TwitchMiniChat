package com.fs.twitchminichat

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import java.security.SecureRandom
import java.util.UUID

/**
 * Stores the stable device identifier and the device-scoped backend credential.
 *
 * The raw credential remains inside the app sandbox. The backend receives it
 * during authenticated registration but stores only its SHA-256 hash.
 */
object DeviceCredentialStore {

    /** Synchronizes credential creation across concurrent profile registrations. */
    private val credentialLock = Any()

    /** Generates cryptographically secure random credential bytes. */
    private val secureRandom = SecureRandom()

    /**
     * Returns the existing installation identifier or creates one atomically.
     */
    fun getOrCreateDeviceId(context: Context): String {
        synchronized(credentialLock) {
            val prefs = preferences(context)
            val existing = prefs
                .getString(KEY_DEVICE_ID, null)
                ?.trim()
                .orEmpty()

            if (existing.isNotBlank()) {
                return existing
            }

            val generated = UUID.randomUUID().toString()

            /*
             * Commit synchronously because the identifier can immediately be
             * used by a backend request.
             */
            prefs.edit(commit = true) {
                putString(KEY_DEVICE_ID, generated)
            }

            return generated
        }
    }

    /**
     * Returns the currently enrolled or pending device secret, if available.
     */
    fun getExistingDeviceSecret(context: Context): String? {
        synchronized(credentialLock) {
            val existing = preferences(context)
                .getString(KEY_DEVICE_SECRET, null)
                ?.trim()
                .orEmpty()

            if (existing.isBlank()) {
                return null
            }

            check(DEVICE_SECRET_PATTERN.matches(existing)) {
                "Stored device credential has an invalid format"
            }

            return existing
        }
    }

    /**
     * Returns the device secret or creates a 256-bit URL-safe secret.
     *
     * The same value is retained after temporary network failures so a later
     * registration cannot accidentally attempt to replace the backend hash.
     */
    fun getOrCreateDeviceSecret(context: Context): String {
        synchronized(credentialLock) {
            getExistingDeviceSecret(context)?.let {
                return it
            }

            val randomBytes = ByteArray(DEVICE_SECRET_BYTES)
            secureRandom.nextBytes(randomBytes)

            val generated = Base64.encodeToString(
                randomBytes,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )

            check(DEVICE_SECRET_PATTERN.matches(generated)) {
                "Generated device credential has an invalid format"
            }

            /*
             * Persist before starting the enrollment request. Losing the local
             * value after backend enrollment would make the credential unusable.
             */
            preferences(context).edit(commit = true) {
                putString(KEY_DEVICE_SECRET, generated)
            }

            return generated
        }
    }

    /**
     * Opens the private preferences shared with Firebase registration state.
     */
    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    private const val PREFERENCES_NAME = "fcm_registration"
    private const val KEY_DEVICE_ID = "install_id"
    private const val KEY_DEVICE_SECRET = "device_secret_v1"
    private const val DEVICE_SECRET_BYTES = 32

    private val DEVICE_SECRET_PATTERN =
        Regex("^[A-Za-z0-9_-]{43,128}$")
}