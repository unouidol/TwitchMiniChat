package com.fs.twitchminichat

import android.content.Context
import android.util.Log
import com.fs.twitchminichat.pcg.GeckoSessionManager

/**
 * Coordinates destructive Privacy & Safety data deletion flows.
 *
 * Keeping this logic outside SafetyPrivacyActivity prevents the Activity from
 * becoming a large procedural controller and makes it clear which operations are
 * device-only versus profile/server-wide.
 */
object DeviceDataDeletionController {

    private const val TAG_DEVICE = "DEVICE_DELETE"
    private const val TAG_TOTAL = "TOTAL_DELETE"

    data class Result(
        val ok: Boolean,
        val message: String,
        val requestId: String?,
        val serverRawResponse: String,
        val localResult: LocalDataCleaner.Result?
    )

    /**
     * Removes only this Android device from the server, then wipes local app data.
     *
     * This does not delete server-side Dex lists, OAuth rows, or profile tombstones.
     */
    fun removeThisDeviceData(
        context: Context,
        onComplete: (Result) -> Unit
    ) {
        val appContext = context.applicationContext
        val profileIds = knownProfileIds(appContext)

        Log.d(TAG_DEVICE, "start knownProfileCount=${profileIds.size}")

        FcmRegistrationUploader.deleteDeviceData(
            context = appContext,
            knownProfileIds = profileIds
        ) { serverResult ->
            Log.d(
                TAG_DEVICE,
                "server ok=${serverResult.ok} " +
                        "removedDevice=${serverResult.removedDevice} " +
                        "removedDeviceProfileCount=${serverResult.removedDeviceProfiles.size} " +
                        "requestId=${serverResult.requestId}"
            )

            if (!serverResult.ok) {
                onComplete(
                    Result(
                        ok = false,
                        message = serverResult.message,
                        requestId = serverResult.requestId,
                        serverRawResponse = serverResult.rawResponse,
                        localResult = null
                    )
                )
                return@deleteDeviceData
            }

            wipeLocalAfterServerDelete(
                context = appContext,
                tag = TAG_DEVICE,
                requestId = serverResult.requestId,
                serverRawResponse = serverResult.rawResponse,
                onComplete = onComplete
            )
        }
    }

    /**
     * Removes this Android device and asks the server to delete profile data saved
     * from this device, then wipes local app data.
     *
     * This is the strong privacy action. It can affect other devices because OAuth
     * and uploaded PCG Dex data are profile-scoped server data.
     */
    fun removeThisDeviceAndServerProfileData(
        context: Context,
        onComplete: (Result) -> Unit
    ) {
        val appContext = context.applicationContext
        val profileIds = knownProfileIds(appContext)

        Log.d(TAG_TOTAL, "start knownProfileCount=${profileIds.size}")

        FcmRegistrationUploader.deleteServerData(
            context = appContext,
            knownProfileIds = profileIds
        ) { serverResult ->
            Log.d(
                TAG_TOTAL,
                "server ok=${serverResult.ok} " +
                        "removedDevice=${serverResult.removedDevice} " +
                        "deletedDexCount=${serverResult.deletedDexProfiles.size} " +
                        "oauthDeletedRows=${serverResult.oauthDeletedRows} " +
                        "oauthDeletedTableCount=${serverResult.oauthDeletedTables.size} " +
                        "requestId=${serverResult.requestId}"
            )

            if (!serverResult.ok) {
                onComplete(
                    Result(
                        ok = false,
                        message = serverResult.message,
                        requestId = serverResult.requestId,
                        serverRawResponse = serverResult.rawResponse,
                        localResult = null
                    )
                )
                return@deleteServerData
            }

            wipeLocalAfterServerDelete(
                context = appContext,
                tag = TAG_TOTAL,
                requestId = serverResult.requestId,
                serverRawResponse = serverResult.rawResponse,
                onComplete = onComplete
            )
        }
    }

    /**
     * Wipes Gecko/PCG web data and then Android local app data.
     *
     * The order matters: server deletion happens before this method. Local data may
     * contain device/profile ids required for the server request, so it must not be
     * cleared first.
     */
    private fun wipeLocalAfterServerDelete(
        context: Context,
        tag: String,
        requestId: String?,
        serverRawResponse: String,
        onComplete: (Result) -> Unit
    ) {
        GeckoSessionManager.clearAllWebData(context) { geckoOk: Boolean, geckoMessage: String ->
            Log.d(tag, "gecko ok=$geckoOk message=$geckoMessage")

            if (!geckoOk) {
                onComplete(
                    Result(
                        ok = false,
                        message = geckoMessage,
                        requestId = requestId,
                        serverRawResponse = serverRawResponse,
                        localResult = null
                    )
                )
                return@clearAllWebData
            }

            val localResult = LocalDataCleaner.clearAllLocalData(context)

            /*
             * clearAllLocalData already deletes SharedPreferences, but keeping this
             * explicit call makes the intent obvious if TermsPrefs ever moves to a
             * different backing store.
             */
            TermsPrefs.clearAcceptance(context)

            Log.d(
                tag,
                "local deletedSharedPrefs=${localResult.deletedSharedPrefs} " +
                        "skippedSharedPrefs=${localResult.skippedSharedPrefs} " +
                        "failedSharedPrefs=${localResult.failedSharedPrefs} " +
                        "clearedCacheDirs=${localResult.clearedCacheDirs} " +
                        "failedCacheDirs=${localResult.failedCacheDirs}"
            )

            onComplete(
                Result(
                    ok = true,
                    message = "ok",
                    requestId = requestId,
                    serverRawResponse = serverRawResponse,
                    localResult = localResult
                )
            )
        }
    }

    /**
     * Collects the profile ids known by this install before local data is wiped.
     */
    private fun knownProfileIds(context: Context): List<String> {
        return AccountRepository(context)
            .loadAccounts()
            .map { account ->
                account.profileId
                    .trim()
                    .lowercase()
                    .ifBlank { ProfileIdUtil.fromUsername(account.username) }
            }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
    }
}