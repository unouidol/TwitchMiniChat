package com.fs.twitchminichat

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * One profile's alt-dex list as `/upload_alt_dex` receives it (`docs/design-alt-dex-alert.md`,
 * *a*).
 *
 * @property missingForms the forms the last accepted "Update Alt Dex" listed, by name as the page
 * shows it, without 🔒 or 🎊. Nothing else of the reading is sent: not the obtained forms, not the
 * sprite numbers, not the normalized keys. The server matches the names exactly, so a key computed
 * here could only drift from what it compares.
 * @property readAtMs wall-clock time of that reading, sent as `read_at`.
 */
internal data class PcgAltDexAlertUpload(
    val profileId: String,
    val profileLabel: String,
    val readAtMs: Long,
    val missingForms: List<String>
) {

    companion object {

        /** The upload of [snapshot], store 3's last accepted reading for [profileId]. */
        fun of(
            profileId: String,
            profileLabel: String,
            snapshot: PcgAltDexSnapshot
        ): PcgAltDexAlertUpload {
            return PcgAltDexAlertUpload(
                profileId = profileId,
                profileLabel = profileLabel,
                readAtMs = snapshot.readAtMs,
                missingForms = snapshot.missing.map(PcgAltDexForm::name)
            )
        }
    }
}

/** What the server did with one alt-dex upload. */
internal sealed interface PcgAltDexAlertUploadResult {

    /** 2xx: the server stored the list, [count] names. */
    data class Stored(val count: Int) : PcgAltDexAlertUploadResult

    /**
     * 409 `alt_dex_alerts_off`: the server has the category off for this profile on every device
     * record, so it stored nothing. Never retried automatically.
     */
    data object AlertsOff : PcgAltDexAlertUploadResult

    /** No answer, or any other status. */
    data object Failed : PcgAltDexAlertUploadResult

    /**
     * Not sent: the profile is blank, or it has no usable backend session. Never sent with the
     * legacy key instead, which `/upload_alt_dex` refuses anyway.
     */
    data object NotSent : PcgAltDexAlertUploadResult
}

/** Sends one alt-dex upload, blocking. An interface so [PcgAltDexAlertSync]'s tests answer it. */
internal fun interface PcgAltDexAlertSender {

    /** Sends [upload] once. Callers are off the main thread. */
    fun send(upload: PcgAltDexAlertUpload): PcgAltDexAlertUploadResult
}

/**
 * The `/upload_alt_dex` client: Bearer session only, one request per call, no retry and no queue.
 *
 * It goes through [BackendJsonTransport], as every request of [FcmRegistrationUploader] does, so
 * Logcat shows `upload_alt_dex responseCode=<n>` under `FCM_REGISTER`. Its own lines hold counts
 * and results, never a form name, a label or a profile identifier.
 */
internal class PcgAltDexAlertUploader(
    private val urlString: String,
    private val sessions: BackendSessionReader,
    private val transport: BackendJsonTransport,
    private val log: OwedWorkLog
) : PcgAltDexAlertSender {

    override fun send(upload: PcgAltDexAlertUpload): PcgAltDexAlertUploadResult {
        val profileId = AccountProfileIdResolver.normalize(upload.profileId)
        if (profileId.isBlank()) {
            log.warn("$LOG_LABEL skipped: blank profileId")
            return PcgAltDexAlertUploadResult.NotSent
        }

        val authorizationHeader =
            when (val decision = BackendAuthHeaderProvider(sessions).resolve(profileId)) {
                is BackendSessionAuthDecision.Bearer -> decision.authorizationHeader

                BackendSessionAuthDecision.Missing -> {
                    log.warn("$LOG_LABEL skipped: backend session missing")
                    return PcgAltDexAlertUploadResult.NotSent
                }

                BackendSessionAuthDecision.Unavailable -> {
                    // An untrusted local session is never downgraded to the legacy key.
                    log.warn("$LOG_LABEL skipped: backend session unavailable")
                    return PcgAltDexAlertUploadResult.NotSent
                }
            }

        val result = classify(
            transport.post(
                urlString = urlString,
                payload = payload(upload, profileId),
                logLabel = LOG_LABEL,
                authorizationHeader = authorizationHeader
            )
        )

        val resultName = when (result) {
            is PcgAltDexAlertUploadResult.Stored -> "stored count=${result.count}"
            PcgAltDexAlertUploadResult.AlertsOff -> ERROR_ALERTS_OFF
            PcgAltDexAlertUploadResult.Failed -> "failed"
            PcgAltDexAlertUploadResult.NotSent -> "not_sent"
        }
        log.debug("$LOG_LABEL result=$resultName sent=${upload.missingForms.size}")
        return result
    }

    companion object {

        /** The request's name in Logcat, after the transport's `responseCode` line too. */
        const val LOG_LABEL = "upload_alt_dex"

        /** The `error` of the server's 409 when the category is off for the profile there. */
        const val ERROR_ALERTS_OFF = "alt_dex_alerts_off"

        private val READ_AT_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

        /** The request body: four fields, the names exactly as [upload] holds them. */
        fun payload(upload: PcgAltDexAlertUpload, normalizedProfileId: String): JSONObject {
            val names = JSONArray()
            upload.missingForms.forEach { name -> names.put(name) }

            return JSONObject()
                .put("profile_id", normalizedProfileId)
                .put("profile_label", upload.profileLabel)
                .put("read_at", formatReadAt(upload.readAtMs))
                .put("missing_forms", names)
        }

        /** [readAtMs] as UTC ISO-8601 to the second, ending in `Z`, which the server requires. */
        fun formatReadAt(readAtMs: Long): String {
            return READ_AT_FORMAT.format(Instant.ofEpochMilli(readAtMs.coerceAtLeast(0L)))
        }

        /**
         * The server's answer as a result. A 409 is [PcgAltDexAlertUploadResult.AlertsOff] only
         * with the `alt_dex_alerts_off` error: any other 409 is an ordinary failure.
         */
        fun classify(result: PostJsonResult?): PcgAltDexAlertUploadResult {
            if (result == null) return PcgAltDexAlertUploadResult.Failed

            val body = runCatching { JSONObject(result.responseBody) }.getOrNull()
            return when {
                result.responseCode in 200..299 ->
                    PcgAltDexAlertUploadResult.Stored(count = body?.optInt("count", 0) ?: 0)

                result.responseCode == 409 &&
                    body?.optString("error") == ERROR_ALERTS_OFF ->
                    PcgAltDexAlertUploadResult.AlertsOff

                else -> PcgAltDexAlertUploadResult.Failed
            }
        }
    }
}

/** Whether an accepted "Update Alt Dex" is followed by an upload. */
internal enum class PcgAltDexAfterReadingDecision {

    /** Missing Alt Dex form alerts are off for the profile on this phone: nothing is sent. */
    CATEGORY_OFF,

    /**
     * The category is on here, but the backend has not confirmed it: nothing is sent, and the
     * user is sent to the bell menu, whose OK sends the selection and then the upload.
     */
    NOT_CONFIRMED,

    /** On and confirmed: send the reading. */
    UPLOAD
}

/** What one alt-dex upload step ended with. */
internal enum class PcgAltDexAlertSyncOutcome {

    /** The server stored the list. */
    STORED,

    /** The turn-on found no reading on this phone, so there was nothing to send (decision 3). */
    NO_READING,

    /** 409 `alt_dex_alerts_off`. */
    ALERTS_OFF,

    /** Not stored, for any other reason. */
    FAILED;

    /** Whether the bell menu's `ALT_DEX_UPLOAD` step counts as done. */
    val completesTurnOn: Boolean
        get() = this == STORED || this == NO_READING
}

/**
 * Decides and runs the alt-dex uploads, and keeps the acknowledged alt-dex category in step with
 * what the server answered.
 *
 * The confirmed baseline (see [PcgProfileAlertSelection.confirmedBy]) is the category on here
 * *and* acknowledged as on. The acknowledgement's alt-dex flag is the one part of an
 * acknowledged selection that a delivery answer alone does not raise (see
 * [PcgProfileAlertAcknowledgementStore.record]): the alert also needs the list on the server.
 * So this class is what moves it:
 *
 * - **raised** after the bell menu's turn-on, when the upload is stored or there is no reading
 *   to send;
 * - **lowered** after a 409 `alt_dex_alerts_off`, from either moment.
 *
 * Both writes happen only when an acknowledgement already exists for the profile. Lowering it
 * is what makes the next OK in the bell menu send the selection, then the upload, again; without
 * an acknowledgement the baseline is already unconfirmed, so nothing needs writing. Neither
 * write is a retry: nothing here sends a second request, queues one, or touches the owed-work
 * store.
 *
 * Owner's decision of 2026-11-25. Both writes are made under lock L ([OwedWorkCoordinator]),
 * with the file obtained inside it, as the acknowledgement write of
 * `setProfileSpawnAlertModeBlocking` is: an erase deletes the file under L, and a commit
 * through an instance taken before it would write the whole old map back (R3 on #117).
 *
 * @param acknowledgements the file of [PcgProfileAlertAcknowledgementStore], obtained on each
 *   use rather than once, for the reason above.
 */
internal class PcgAltDexAlertSync(
    private val coordinator: OwedWorkCoordinator,
    private val acknowledgements: () -> SharedPreferences,
    private val categoryEnabled: (profileId: String) -> Boolean,
    private val loadReading: (profileId: String) -> PcgAltDexSnapshot?,
    private val sender: PcgAltDexAlertSender
) {

    /** Whether the accepted reading of [profileId] is to be uploaded. Sends nothing. */
    fun decideAfterReading(profileId: String): PcgAltDexAfterReadingDecision {
        if (!categoryEnabled(profileId)) return PcgAltDexAfterReadingDecision.CATEGORY_OFF

        val acknowledged = PcgProfileAlertAcknowledgementStore.read(acknowledgements(), profileId)
        return if (acknowledged?.altDexFormAlertsEnabled == true) {
            PcgAltDexAfterReadingDecision.UPLOAD
        } else {
            PcgAltDexAfterReadingDecision.NOT_CONFIRMED
        }
    }

    /**
     * Uploads an accepted reading that [decideAfterReading] cleared. A 409 lowers the
     * acknowledged category; a stored list changes nothing, since it was already confirmed.
     */
    fun uploadAfterReading(upload: PcgAltDexAlertUpload): PcgAltDexAlertSyncOutcome {
        val outcome = outcomeOf(sender.send(upload))
        if (outcome == PcgAltDexAlertSyncOutcome.ALERTS_OFF) {
            setConfirmed(upload.profileId, confirmed = false)
        }
        return outcome
    }

    /**
     * The bell menu's `ALT_DEX_UPLOAD` step, run only after its delivery step succeeded: uploads
     * the stored reading of [profileId], if there is one, and confirms the category when the list
     * is stored or there is nothing to send.
     */
    fun uploadAfterTurnOn(profileId: String, profileLabel: String): PcgAltDexAlertSyncOutcome {
        val reading = loadReading(profileId)
        val outcome = if (reading == null) {
            PcgAltDexAlertSyncOutcome.NO_READING
        } else {
            outcomeOf(sender.send(PcgAltDexAlertUpload.of(profileId, profileLabel, reading)))
        }

        when (outcome) {
            PcgAltDexAlertSyncOutcome.STORED,
            PcgAltDexAlertSyncOutcome.NO_READING -> setConfirmed(profileId, confirmed = true)

            PcgAltDexAlertSyncOutcome.ALERTS_OFF -> setConfirmed(profileId, confirmed = false)

            // A failure leaves the category unconfirmed, as the delivery step recorded it.
            PcgAltDexAlertSyncOutcome.FAILED -> Unit
        }
        return outcome
    }

    /** Moves the acknowledged alt-dex category, under L. */
    private fun setConfirmed(profileId: String, confirmed: Boolean) {
        coordinator.locked {
            PcgProfileAlertAcknowledgementStore.setAltDexFormAlertsConfirmed(
                acknowledgements(),
                profileId,
                confirmed
            )
        }
    }

    private fun outcomeOf(result: PcgAltDexAlertUploadResult): PcgAltDexAlertSyncOutcome {
        return when (result) {
            is PcgAltDexAlertUploadResult.Stored -> PcgAltDexAlertSyncOutcome.STORED
            PcgAltDexAlertUploadResult.AlertsOff -> PcgAltDexAlertSyncOutcome.ALERTS_OFF
            PcgAltDexAlertUploadResult.Failed,
            PcgAltDexAlertUploadResult.NotSent -> PcgAltDexAlertSyncOutcome.FAILED
        }
    }

    companion object {

        private const val TAG = "FCM_REGISTER"

        /** The application's instance: the real stores, the real client. */
        fun forContext(context: Context): PcgAltDexAlertSync {
            val appContext = context.applicationContext
            return PcgAltDexAlertSync(
                coordinator = OwedWorkCoordinator.PROCESS,
                acknowledgements = { PcgProfileAlertAcknowledgementStore.preferences(appContext) },
                categoryEnabled = { profileId ->
                    PcgAltDexFormAlertStore.isEnabled(appContext, profileId)
                },
                loadReading = { profileId -> PcgAltDexSnapshotStore.load(appContext, profileId) },
                sender = PcgAltDexAlertUploader(
                    urlString = appContext.getString(R.string.alt_dex_upload_url),
                    sessions = BackendSessionStore(appContext),
                    transport = HttpBackendJsonTransport,
                    log = AndroidOwedWorkLog(TAG)
                )
            )
        }

        /**
         * The message an upload owes the user, or null when it owes none of its own.
         *
         * A 409 and an unconfirmed category share one string: in both, the way out is the bell
         * menu's OK, which sends the selection and then the upload. Neither is ever shown as the
         * generic failure.
         */
        @StringRes
        fun messageFor(outcome: PcgAltDexAlertSyncOutcome): Int? {
            return when (outcome) {
                PcgAltDexAlertSyncOutcome.STORED -> R.string.pcg_alt_dex_updated_and_sent
                PcgAltDexAlertSyncOutcome.ALERTS_OFF -> notConfirmedMessage
                PcgAltDexAlertSyncOutcome.FAILED -> R.string.pcg_alt_dex_alert_upload_failed
                PcgAltDexAlertSyncOutcome.NO_READING -> null
            }
        }

        /**
         * The message the bell menu's `ALT_DEX_UPLOAD` step owes the user after the selection's
         * own toast, or null when it owes none. [outcome] is null when the plan had no such step.
         *
         * - **No reading on this phone:** the hint under the checkbox, once more, as a toast
         *   (decision 3). The category is saved; the first reading uploads.
         * - **A 409:** its own string, never only the generic one.
         * - **Stored, or failed:** none. The selection's toast already says saved, or that only
         *   part of it was, and a failure leaves the category unconfirmed, so the next OK sends
         *   it again. [R.string.pcg_alt_dex_alert_upload_failed] would be wrong here: it sends
         *   the user to Update Alt Dex, which sends nothing while the category is unconfirmed.
         */
        @StringRes
        fun messageAfterTurnOn(outcome: PcgAltDexAlertSyncOutcome?): Int? {
            return when (outcome) {
                PcgAltDexAlertSyncOutcome.NO_READING -> R.string.pcg_alt_dex_form_alert_no_reading
                PcgAltDexAlertSyncOutcome.ALERTS_OFF -> notConfirmedMessage
                PcgAltDexAlertSyncOutcome.STORED,
                PcgAltDexAlertSyncOutcome.FAILED,
                null -> null
            }
        }

        /**
         * The message of [PcgAltDexAfterReadingDecision.NOT_CONFIRMED], and of a 409: the
         * category is on here and not confirmed by the server.
         */
        @get:StringRes
        val notConfirmedMessage: Int
            get() = R.string.pcg_alt_dex_alert_upload_alerts_off
    }
}
