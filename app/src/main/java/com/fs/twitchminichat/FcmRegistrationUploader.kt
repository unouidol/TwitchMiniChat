package com.fs.twitchminichat

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging

object FcmRegistrationUploader {

    private const val TAG = "FCM_REGISTER"

    /**
     * This device could not register for push notifications.
     *
     * Registration runs once at start-up with no retry, so a failure here means no
     * spawn alert arrives until the application is started again.
     */
    private const val MARKER_REGISTER_TOKEN_FAILED = "fcm_register_token_failed"

    /** A manual Pokedex upload did not reach the backend. */
    private const val MARKER_DEX_UPLOAD_FAILED = "pcg_dex_upload_failed"

    /** Cached copy of the current Firebase Cloud Messaging token. */
    private const val KEY_LATEST_FCM_TOKEN = "latest_fcm_token"

    /**
     * How long one token deletion may block before it counts as failed.
     *
     * A deletion that never returns would hold the erase on screen, and the owed-work
     * queue exists exactly so a failure here costs nothing.
     */
    private const val FIREBASE_TOKEN_DELETION_TIMEOUT_SECONDS = 20L

    /** UI-facing deletion outcome that deliberately excludes raw backend metadata. */
    data class DeleteServerDataResult(
        val ok: Boolean,
        val message: String
    )

    /** UI-facing report outcome that deliberately excludes the reported content. */
    data class ReportMessageResult(
        val ok: Boolean
    )

    /**
     * Runs one profile's registration pass: whatever the planner says has to be sent.
     *
     * For an active profile that is the token followed by its alert selection, as
     * before. For a profile with no category active it is the disabled selection alone,
     * which is what stops a server that still holds an active mode from sending - see
     * [PcgProfileRegistrationSyncPlanner].
     */
    fun uploadToken(context: Context, token: String, profileId: String) {
        val appContext = context.applicationContext
        val trimmedToken = token.trim()
        if (trimmedToken.isEmpty()) {
            Log.w(TAG, "Empty token, skip upload")
            return
        }

        thread(start = true, name = "fcm-register-upload") {
            val selection = PcgProfileAlertSelectionStore.read(
                appContext,
                profileId
            )
            val plan = PcgProfileRegistrationSyncPlanner.buildPlan(
                selection = selection,
                acknowledged = PcgProfileAlertAcknowledgementStore.read(
                    appContext,
                    profileId
                )
            )

            if (plan.isEmpty()) {
                Log.d(
                    TAG,
                    "registration pass skipped: backend already holds this selection"
                )
                return@thread
            }

            /*
             * Whether this pass registers a token at all. The alert step used to refuse
             * to run unless a registration had just succeeded, which for the
             * disabled-selection plan - the one with no registration in it - would have
             * made the step unreachable: a planner producing a step nobody runs is worse
             * than the defect it was written for.
             */
            val tokenRegistrationPlanned =
                PcgProfileRegistrationSyncStep.REGISTER_TOKEN in plan

            var registrationSucceeded = false

            for (step in plan) {
                when (step) {
                    PcgProfileRegistrationSyncStep.REGISTER_TOKEN -> {
                        registrationSucceeded = uploadTokenBlocking(
                            appContext,
                            trimmedToken,
                            profileId
                        )
                        if (!registrationSucceeded) {
                            break
                        }
                    }

                    PcgProfileRegistrationSyncStep.RESTORE_ALERT_SELECTION -> {
                        if (tokenRegistrationPlanned && !registrationSucceeded) break

                        val sent = setProfileSpawnAlertModeBlocking(
                            context = appContext,
                            profileId = profileId,
                            selection = selection,
                            token = trimmedToken
                        )
                        Log.d(
                            TAG,
                            "Alert selection sent ok=$sent " +
                                "deliveryRequired=${selection.requiresFirebaseDelivery} " +
                                "afterRegistration=$tokenRegistrationPlanned"
                        )
                    }
                }
            }
        }
    }


    /**
     * Sends the selected Pokémon Community Game spawn settings to the backend.
     *
     * This is the new 4-state preference used by the bell menu:
     *
     * 0 = Dex + Tier A
     * 1 = Dex only
     * 2 = All spawns
     * 3 = No spawns
     *
     * Event spawns are independent from the four ordinary modes. The legacy
     * "enabled" field remains true whenever ordinary, event or Most Wanted
     * alerts need delivery.
     */
    fun setProfileSpawnAlertMode(
        context: Context,
        profileId: String,
        selection: PcgProfileAlertSelection,
        onComplete: (Boolean) -> Unit
    ) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("fcm_registration", Context.MODE_PRIVATE)
        val normalizedProfileId = normalizeProfileId(profileId)

        fun finish(ok: Boolean) {
            Handler(Looper.getMainLooper()).post {
                onComplete(ok)
            }
        }

        if (normalizedProfileId.isBlank()) {
            Log.w(TAG, "Cannot set spawn alert mode: blank profileId")
            finish(false)
            return
        }

        fun sendRequest(token: String) {
            thread(start = true, name = "spawn-alert-mode") {
                val ok = setProfileSpawnAlertModeBlocking(
                    context = appContext,
                    profileId = normalizedProfileId,
                    selection = selection,
                    token = token
                )
                finish(ok)
            }
        }

        val cachedToken = prefs.getString(KEY_LATEST_FCM_TOKEN, null).orEmpty()

        /*
         * When every category is disabled, the request can be sent without
         * forcing a fresh token fetch. Any enabled category requires a usable
         * token because the backend must be able to deliver its notification.
         */
        if (!selection.requiresFirebaseDelivery) {
            sendRequest(cachedToken)
            return
        }

        if (cachedToken.isNotBlank()) {
            sendRequest(cachedToken)
            return
        }

        Log.d(TAG, "No cached Firebase Cloud Messaging token; fetching one for spawn mode")

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(
                    TAG,
                    "Unable to fetch a Firebase Cloud Messaging token for spawn mode " +
                        "errorType=${DiagnosticError.typeOf(task.exception)}"
                )
                finish(false)
                return@addOnCompleteListener
            }

            val freshToken = task.result?.trim().orEmpty()
            if (freshToken.isBlank()) {
                Log.w(TAG, "Fresh Firebase Cloud Messaging token is blank for spawn mode")
                finish(false)
                return@addOnCompleteListener
            }

            prefs.edit {
                putString(KEY_LATEST_FCM_TOKEN, freshToken)
            }

            Log.d(TAG, "Fetched a Firebase Cloud Messaging token for spawn mode")
            sendRequest(freshToken)
        }
    }

    /** Sends one complete alert selection without starting another worker. */
    private fun setProfileSpawnAlertModeBlocking(
        context: Context,
        profileId: String,
        selection: PcgProfileAlertSelection,
        token: String
    ): Boolean {
        val normalizedProfileId = normalizeProfileId(profileId)
        if (normalizedProfileId.isBlank()) {
            Log.w(TAG, "set_spawn_alert_mode skipped: blank profileId")
            return false
        }

        val settings = selection.spawnSettings
        val authDecision = BackendAuthHeaderProvider(
            sessionReader = BackendSessionStore(context)
        ).resolve(normalizedProfileId)

        val payload = JSONObject().apply {
            put("device_id", DeviceCredentialStore.getOrCreateDeviceId(context))
            put("device_name", buildDeviceName(context))
            put("fcm_token", token)
            put("profile_id", normalizedProfileId)
            put("spawn_alert_mode", settings.regularMode.id)
            put("event_spawn_enabled", settings.eventSpawnsEnabled)

            /*
             * Compatibility bridge for the old registration model. The server
             * keeps Firebase delivery while any independent category is active.
             */
            put("enabled", selection.requiresFirebaseDelivery)
        }

        val authorizationHeader = when (authDecision) {
            BackendSessionAuthDecision.Missing -> {
                Log.w(TAG, "set_spawn_alert_mode skipped: backend session missing")
                return false
            }

            is BackendSessionAuthDecision.Bearer -> {
                Log.d(TAG, "set_spawn_alert_mode authMode=backend_session")
                authDecision.authorizationHeader
            }

            BackendSessionAuthDecision.Unavailable -> {
                /*
                 * An unreadable or invalid local session must not be downgraded
                 * to profile-only legacy authentication.
                 */
                Log.w(TAG, "set_spawn_alert_mode skipped: backend session unavailable")
                return false
            }
        }

        val result = postJson(
            urlString = context.getString(R.string.fcm_set_spawn_alert_mode_url),
            payload = payload,
            logLabel = "set_spawn_alert_mode",
            authorizationHeader = authorizationHeader
        )

        val acknowledged = result?.responseCode in 200..299

        if (acknowledged) {
            /*
             * Every successful push funnels through here, which is why the
             * acknowledgement is recorded here and nowhere else. It is the difference
             * between "the backend already knows this" and "the backend was never
             * told", and until it was written down neither could be distinguished, so
             * a failed request could be neither retried nor skipped.
             */
            PcgProfileAlertAcknowledgementStore.record(
                context = context,
                profileId = normalizedProfileId,
                selection = selection
            )
        }

        return acknowledged
    }

    /**
     * Tells the backend that one profile's alerts are off, blocking.
     *
     * Callers must already be off the main thread. Used by the owed-work queue to finish
     * a disable an account removal could not complete, and deliberately narrow: the
     * disabled selection needs no Firebase Cloud Messaging delivery, so it can be sent
     * with whatever token is cached - including none - while any active selection could
     * not.
     */
    fun sendProfileAlertDisableBlocking(
        context: Context,
        profileId: String
    ): Boolean {
        val appContext = context.applicationContext
        val cachedToken = appContext
            .getSharedPreferences(
                DeviceCredentialStore.PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .getString(KEY_LATEST_FCM_TOKEN, null)
            .orEmpty()

        return setProfileSpawnAlertModeBlocking(
            context = appContext,
            profileId = profileId,
            selection = PcgProfileAlertSelection.DISABLED,
            token = cachedToken
        )
    }


    fun uploadDexList(
        context: Context,
        profileId: String,
        profileLabel: String,
        wantedPokemon: List<String>
    ) {
        val appContext = context.applicationContext
        val normalized = wantedPokemon
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        thread(start = true, name = "dex-upload") {
            uploadDexListBlocking(appContext, profileId, profileLabel, normalized)
        }
    }

    /**
     * Deletes this device's server-side app data with dual authentication.
     *
     * [candidateProfileIds] is used only to select a local backend session.
     * The backend derives the authoritative scope from the device record.
     */
    fun deleteServerData(
        context: Context,
        candidateProfileIds: Collection<String>,
        onComplete: (DeleteServerDataResult) -> Unit
    ) {
        deleteWithDeviceCredentials(
            context = context,
            candidateProfileIds = candidateProfileIds,
            threadName = "delete-server-data",
            logLabel = "delete_server_data",
            urlRes = R.string.delete_server_data_url,
            successMessageRes = R.string.server_delete_ok,
            onComplete = onComplete
        )
    }

    /**
     * Removes only this device's registration from the server.
     *
     * Profile-scoped server data such as Pokedex lists, application OAuth records and
     * profile tombstones is deliberately left in place, so other devices signed in to
     * the same profiles keep working. Authentication and payload are identical to
     * [deleteServerData]: the backend derives the authoritative scope from its own
     * registered-device record and ignores profile identifiers sent by the client.
     */
    fun deleteDeviceData(
        context: Context,
        candidateProfileIds: Collection<String>,
        onComplete: (DeleteServerDataResult) -> Unit
    ) {
        deleteWithDeviceCredentials(
            context = context,
            candidateProfileIds = candidateProfileIds,
            threadName = "delete-device-data",
            logLabel = "delete_device_data",
            urlRes = R.string.delete_device_data_url,
            successMessageRes = R.string.device_delete_ok,
            onComplete = onComplete
        )
    }

    /**
     * Performs one destructive deletion request off the main thread.
     *
     * Both deletion endpoints share this path so their authentication, payload and
     * failure handling cannot drift apart as either one changes.
     */
    private fun deleteWithDeviceCredentials(
        context: Context,
        candidateProfileIds: Collection<String>,
        threadName: String,
        logLabel: String,
        @StringRes urlRes: Int,
        @StringRes successMessageRes: Int,
        onComplete: (DeleteServerDataResult) -> Unit
    ) {
        val appContext = context.applicationContext

        thread(start = true, name = threadName) {
            val ready = when (
                val credentials = resolveDeletionCredentials(
                    appContext = appContext,
                    candidateProfileIds = candidateProfileIds,
                    logLabel = logLabel
                )
            ) {
                is DeletionCredentials.Ready -> credentials

                is DeletionCredentials.Blocked -> {
                    postDeleteServerDataResult(
                        onComplete,
                        DeleteServerDataResult(
                            ok = false,
                            message = appContext.getString(credentials.messageRes)
                        )
                    )
                    return@thread
                }
            }

            val result = postJson(
                urlString = appContext.getString(urlRes),
                payload = JSONObject().apply {
                    put("device_id", ready.deviceId)
                    put("device_secret", ready.deviceSecret)
                },
                logLabel = logLabel,
                authorizationHeader = ready.authorizationHeader
            )

            val outcome = DeletionResponseParser.parse(
                responseCode = result?.responseCode,
                rawBody = result?.responseBody
            )
            val ok = outcome is DeletionOutcome.Success

            val message = when (outcome) {
                DeletionOutcome.Success -> appContext.getString(successMessageRes)

                is DeletionOutcome.Failure -> outcome.serverMessage
                    ?: appContext.getString(R.string.server_deletion_failed)
            }

            val scope = DeletionResponseParser.describeScope(result?.responseBody)
            Log.d(
                TAG,
                "$logLabel completed ok=$ok" + if (scope.isEmpty()) "" else " $scope"
            )

            postDeleteServerDataResult(
                onComplete,
                DeleteServerDataResult(
                    ok = ok,
                    message = message
                )
            )
        }
    }

    /** Everything one deletion request needs, or the reason it cannot be sent. */
    private sealed interface DeletionCredentials {

        /** Both authentication factors are available. */
        data class Ready(
            val authorizationHeader: String,
            val deviceId: String,
            val deviceSecret: String
        ) : DeletionCredentials

        /** The request must not be attempted; [messageRes] explains why. */
        data class Blocked(@param:StringRes val messageRes: Int) : DeletionCredentials
    }

    /**
     * Resolves the backend session and the device credential required to delete.
     *
     * Missing or unreadable state never downgrades to a weaker authentication mode:
     * the request is refused instead.
     */
    private fun resolveDeletionCredentials(
        appContext: Context,
        candidateProfileIds: Collection<String>,
        logLabel: String
    ): DeletionCredentials {
        val authorizationHeader = when (
            val authDecision = ServerDeletionAuthProvider(
                sessionReader = BackendSessionStore(appContext)
            ).resolve(candidateProfileIds)
        ) {
            is ServerDeletionAuthDecision.Bearer -> {
                Log.d(TAG, "$logLabel authMode=backend_session")
                authDecision.authorizationHeader
            }

            ServerDeletionAuthDecision.SessionMissing -> {
                Log.w(TAG, "$logLabel skipped: backend session missing")
                return DeletionCredentials.Blocked(
                    R.string.server_deletion_session_required
                )
            }

            ServerDeletionAuthDecision.SessionUnavailable -> {
                Log.w(TAG, "$logLabel skipped: backend session unavailable")
                return DeletionCredentials.Blocked(
                    R.string.server_deletion_auth_unavailable
                )
            }
        }

        val deviceSecret = runCatching {
            DeviceCredentialStore.getExistingDeviceSecret(appContext)
        }.getOrElse { error ->
            Log.e(
                TAG,
                "$logLabel skipped: invalid device credential " +
                    "errorType=${DiagnosticError.typeOf(error)}"
            )
            null
        }

        if (deviceSecret.isNullOrBlank()) {
            Log.w(TAG, "$logLabel skipped: device credential missing")
            return DeletionCredentials.Blocked(
                R.string.server_deletion_device_credential_missing
            )
        }

        return DeletionCredentials.Ready(
            authorizationHeader = authorizationHeader,
            deviceId = DeviceCredentialStore.getOrCreateDeviceId(appContext),
            deviceSecret = deviceSecret
        )
    }

    /**
     * Deletes this device's Firebase Cloud Messaging token, off the main thread.
     *
     * This is what makes an erase safe when the backend cannot be reached. Without a
     * token the phone can receive no notification at all, and the backend drops the
     * registration the next time it tries to send to it - its Firebase call answers
     * `UnregisteredError` and `fcm_sender.remove_registered_device_by_token` prunes the
     * record. That is not the same as the record being deleted when the user taps: if
     * no spawn ever matches those profiles again, the row simply sits there with a dead
     * token and no way to reach this phone.
     */
    fun deleteFirebaseToken(
        context: Context,
        onComplete: (Boolean) -> Unit
    ) {
        val appContext = context.applicationContext

        thread(start = true, name = "delete-firebase-token") {
            val ok = deleteFirebaseTokenBlocking(appContext)
            Handler(Looper.getMainLooper()).post {
                onComplete(ok)
            }
        }
    }

    /**
     * Deletes the Firebase Cloud Messaging token and the cached copy of it.
     *
     * Blocking: callers must already be off the main thread. Returns whether the token
     * is now gone.
     */
    fun deleteFirebaseTokenBlocking(context: Context): Boolean {
        val appContext = context.applicationContext

        val deleted = runCatching {
            Tasks.await(
                FirebaseMessaging.getInstance().deleteToken(),
                FIREBASE_TOKEN_DELETION_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            )
            true
        }.getOrElse { error ->
            Log.w(
                TAG,
                "delete_firebase_token failed " +
                    "errorType=${DiagnosticError.typeOf(error)}"
            )
            false
        }

        if (deleted) {
            /*
             * The cached copy is what every other path reuses instead of asking
             * Firebase again, so leaving it behind would hand a deleted token to the
             * next registration.
             */
            appContext
                .getSharedPreferences(
                    DeviceCredentialStore.PREFERENCES_NAME,
                    Context.MODE_PRIVATE
                )
                .edit(commit = true) {
                    remove(KEY_LATEST_FCM_TOKEN)
                }

            Log.d(TAG, "delete_firebase_token completed ok=true")
        }

        return deleted
    }

    /** Delivers one deletion result on the Android main thread. */
    private fun postDeleteServerDataResult(
        callback: (DeleteServerDataResult) -> Unit,
        result: DeleteServerDataResult
    ) {
        Handler(Looper.getMainLooper()).post {
            callback(result)
        }
    }

    private fun normalizeProfileId(value: String?): String {
        return value?.trim()?.lowercase().orEmpty()
    }

    /**
     * Registers one Firebase Cloud Messaging token using the profile's preselected
     * backend authentication mode.
     *
     * Missing or unreadable backend sessions never authorize a request.
     */
    private fun uploadTokenBlocking(
        context: Context,
        token: String,
        profileId: String
    ): Boolean {
        val normalizedProfileId = normalizeProfileId(profileId)
        if (normalizedProfileId.isBlank()) {
            Log.w(TAG, "Blank profileId, skip register_fcm")
            return false
        }

        val authDecision = BackendAuthHeaderProvider(
            sessionReader = BackendSessionStore(context)
        ).resolve(normalizedProfileId)

        val payload = JSONObject().apply {
            put("device_id", DeviceCredentialStore.getOrCreateDeviceId(context))
            put("device_name", buildDeviceName(context))
            put("fcm_token", token)
            put("profile_id", normalizedProfileId)
        }

        val authorizationHeader = when (authDecision) {
            BackendSessionAuthDecision.Missing -> {
                Log.w(TAG, "register_fcm skipped: backend session missing")
                return false
            }

            is BackendSessionAuthDecision.Bearer -> {
                val deviceSecret = runCatching {
                    DeviceCredentialStore.getOrCreateDeviceSecret(context)
                }.getOrElse { error ->
                    Log.e(
                        TAG,
                        "register_fcm skipped: device credential unavailable " +
                            "errorType=${DiagnosticError.typeOf(error)}"
                    )
                    CrashReporting.recordFailure(
                        "$MARKER_REGISTER_TOKEN_FAILED reason=device_credential_unavailable",
                        error
                    )
                    return false
                }

                /*
                 * Device credential enrollment is allowed only when this profile
                 * is authenticated by its backend Bearer session.
                 */
                payload.put("device_secret", deviceSecret)

                Log.d(TAG, "register_fcm authMode=backend_session")
                authDecision.authorizationHeader
            }

            BackendSessionAuthDecision.Unavailable -> {
                /*
                 * Do not downgrade to the legacy key when local session state exists
                 * but cannot be trusted.
                 */
                Log.w(TAG, "register_fcm skipped: backend session unavailable")
                CrashReporting.recordFailure(
                    "$MARKER_REGISTER_TOKEN_FAILED reason=session_unavailable"
                )
                return false
            }
        }

        val result = postJson(
            urlString = context.getString(R.string.fcm_register_url),
            payload = payload,
            logLabel = "register_fcm",
            authorizationHeader = authorizationHeader
        )

        return result?.responseCode in 200..299
    }

    /**
     * Uploads one profile's missing Pokédex entries using a preselected authentication mode.
     *
     * Missing or unreadable backend sessions never authorize a request.
     */
    private fun uploadDexListBlocking(
        context: Context,
        profileId: String,
        profileLabel: String,
        wantedPokemon: List<String>
    ) {
        val normalizedProfileId = normalizeProfileId(profileId)
        if (normalizedProfileId.isBlank()) {
            Log.w(TAG, "Blank profileId, skip upload_dex_list")
            showToast(context, "Error updating dex list for $profileLabel")
            return
        }

        val authDecision = BackendAuthHeaderProvider(
            sessionReader = BackendSessionStore(context)
        ).resolve(normalizedProfileId)

        val wantedArray = JSONArray()
        for (name in wantedPokemon) {
            wantedArray.put(name)
        }

        val payload = JSONObject().apply {
            put("profile_id", normalizedProfileId)
            put("profile_label", profileLabel)
            put("wanted_pokemon", wantedArray)
        }

        val authorizationHeader = when (authDecision) {
            BackendSessionAuthDecision.Missing -> {
                Log.w(TAG, "upload_dex_list skipped: backend session missing")
                showToast(context, "Error updating dex list for $profileLabel")
                return
            }

            is BackendSessionAuthDecision.Bearer -> {
                Log.d(TAG, "upload_dex_list authMode=backend_session")
                authDecision.authorizationHeader
            }

            BackendSessionAuthDecision.Unavailable -> {
                /*
                 * Do not downgrade to the legacy key when local session state exists
                 * but cannot be trusted.
                 */
                Log.w(TAG, "upload_dex_list skipped: backend session unavailable")
                CrashReporting.recordFailure(
                    "$MARKER_DEX_UPLOAD_FAILED reason=session_unavailable"
                )
                showToast(context, "Error updating dex list for $profileLabel")
                return
            }
        }

        val result = postJson(
            urlString = context.getString(R.string.dex_upload_url),
            payload = payload,
            logLabel = "upload_dex_list",
            authorizationHeader = authorizationHeader
        )

        if (result == null) {
            /*
             * Also fires when the device is simply offline, which is why the reason is
             * kept separate: a backend that has become unreachable for everyone and a
             * user on a train look the same from here, and only the first is a bug.
             */
            CrashReporting.recordFailure("$MARKER_DEX_UPLOAD_FAILED reason=no_response")
            showToast(context, "Error updating dex list for $profileLabel")
            return
        }

        if (result.responseCode in 200..299) {
            val body = runCatching {
                JSONObject(result.responseBody)
            }.getOrNull()

            val count = body?.optInt("count", wantedPokemon.size) ?: wantedPokemon.size
            val uploadResult = body?.optString("result").orEmpty()

            val message = when (uploadResult) {
                "created" -> "Dex list created for $profileLabel ($count Pokémon)"
                "updated" -> "Dex list updated for $profileLabel ($count Pokémon)"
                else -> "Dex list synced for $profileLabel ($count Pokémon)"
            }

            showToast(context, message)
        } else {
            CrashReporting.recordFailure(
                "$MARKER_DEX_UPLOAD_FAILED reason=http_status status=${result.responseCode}"
            )
            showToast(context, "Error updating dex list for $profileLabel")
        }
    }

    private data class PostJsonResult(
        val responseCode: Int,
        val responseBody: String
    )

    /**
     * Sends one JavaScript Object Notation (JSON) request with a mandatory,
     * prevalidated Authorization header.
     */
    private fun postJson(
        urlString: String,
        payload: JSONObject,
        logLabel: String,
        authorizationHeader: String
    ): PostJsonResult? {
        var conn: HttpURLConnection? = null

        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")

                setRequestProperty("Authorization", authorizationHeader)
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val responseText = readStream(
                if (responseCode in 200..299) {
                    conn.inputStream
                } else {
                    conn.errorStream
                }
            )

            Log.d(TAG, "$logLabel responseCode=$responseCode")
            PostJsonResult(responseCode, responseText)
        } catch (t: Throwable) {
            Log.e(
                TAG,
                "$logLabel failed errorType=${DiagnosticError.typeOf(t)}"
            )
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun buildDeviceName(context: Context): String {
        val installId =
            DeviceCredentialStore.getOrCreateDeviceId(context)

        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()

        val humanPart = listOf(manufacturer, model)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "android-device" }

        return "$humanPart [$installId]"
    }

    private fun readStream(stream: InputStream?): String {
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }

    private fun showToast(context: Context, text: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT).show()
        }
    }
    /**
     * Submits one user-triggered message report using the selected authentication mode.
     *
     * Missing or unreadable backend sessions never authorize a request.
     */
    fun reportMessage(
        context: Context,
        reporterProfileId: String,
        channel: String,
        messageUser: String,
        messageText: String,
        messageId: String?,
        messageTimestampSec: Double?,
        reason: String = "user_report",
        onComplete: (ReportMessageResult) -> Unit
    ) {
        val appContext = context.applicationContext
        val normalizedReporterProfileId = normalizeProfileId(reporterProfileId)
        val normalizedChannel = channel.trim().removePrefix("#").lowercase()

        /**
         * Delivers the result on the main thread.
         */
        fun finish(result: ReportMessageResult) {
            Handler(Looper.getMainLooper()).post {
                onComplete(result)
            }
        }

        if (normalizedReporterProfileId.isBlank()) {
            Log.w(TAG, "report_message skipped: blank reporter profile")
            finish(
                ReportMessageResult(
                    ok = false
                )
            )
            return
        }

        thread(start = true, name = "report-message") {
            val authDecision = BackendAuthHeaderProvider(
                sessionReader = BackendSessionStore(appContext)
            ).resolve(normalizedReporterProfileId)

            val payload = JSONObject().apply {
                put("reporter_profile_id", normalizedReporterProfileId)
                put("channel", normalizedChannel)
                put("message_user", messageUser.trim())
                put("message_text", messageText)
                put("message_id", messageId ?: JSONObject.NULL)
                put("message_timestamp", messageTimestampSec ?: JSONObject.NULL)
                put("reason", reason)
            }

            val authorizationHeader = when (authDecision) {
                BackendSessionAuthDecision.Missing -> {
                    Log.w(TAG, "report_message skipped: backend session missing")
                    finish(
                        ReportMessageResult(
                            ok = false
                        )
                    )
                    return@thread
                }

                is BackendSessionAuthDecision.Bearer -> {
                    Log.d(TAG, "report_message authMode=backend_session")
                    authDecision.authorizationHeader
                }

                BackendSessionAuthDecision.Unavailable -> {
                    /*
                     * Do not downgrade to the legacy key when local session state exists
                     * but cannot be trusted.
                     */
                    Log.w(TAG, "report_message skipped: backend session unavailable")
                    finish(
                        ReportMessageResult(
                            ok = false
                        )
                    )
                    return@thread
                }
            }

            val result = postJson(
                urlString = appContext.getString(R.string.report_message_url),
                payload = payload,
                logLabel = "report_message",
                authorizationHeader = authorizationHeader
            )

            val rawBody = result?.responseBody.orEmpty()
            val body = runCatching {
                if (rawBody.isNotBlank()) JSONObject(rawBody) else null
            }.getOrNull()

            val ok =
                result?.responseCode in 200..299 &&
                        body?.optBoolean("ok", false) == true

            /*
             * Do not log the reported text, author, message identifier,
             * backend response body, or authentication credentials.
             */
            Log.d(
                TAG,
                "report_message completed ok=$ok"
            )

            finish(
                ReportMessageResult(
                    ok = ok
                )
            )
        }
    }
}
