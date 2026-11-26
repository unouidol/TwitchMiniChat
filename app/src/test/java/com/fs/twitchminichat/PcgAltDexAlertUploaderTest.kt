package com.fs.twitchminichat

import com.fs.twitchminichat.pcg.PcgAltDexKnownForms
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Characterizes the `/upload_alt_dex` client (`docs/design-alt-dex-alert.md`, *a* and *j*). */
class PcgAltDexAlertUploaderTest {

    private class RecordingTransport(
        private val answer: PostJsonResult?
    ) : BackendJsonTransport {

        val payloads = mutableListOf<JSONObject>()
        val headers = mutableListOf<String>()

        override fun post(
            urlString: String,
            payload: JSONObject,
            logLabel: String,
            authorizationHeader: String
        ): PostJsonResult? {
            assertEquals("upload_alt_dex", logLabel)
            payloads += payload
            headers += authorizationHeader
            return answer
        }
    }

    private val log = RecordingOwedWorkLog()

    private fun uploader(
        transport: BackendJsonTransport,
        lookup: BackendSessionLookup = BackendSessionLookup.Present("session-a")
    ) = PcgAltDexAlertUploader(
        urlString = "https://backend.invalid/upload_alt_dex",
        sessions = { lookup },
        transport = transport,
        log = log
    )

    private val upload = PcgAltDexAlertUpload(
        profileId = " Profile-A ",
        profileLabel = "label-a",
        readAtMs = 1_795_421_560_123L,
        missingForms = listOf("Alo Vulpix", "Gal Farfetch’d", "Venusaur (F)")
    )

    @Test
    fun payloadHoldsTheFourFieldsAndTheNamesExactly() {
        val transport = RecordingTransport(PostJsonResult(200, """{"ok":true,"count":3}"""))

        uploader(transport).send(upload)

        val payload = transport.payloads.single()
        assertEquals(
            setOf("profile_id", "profile_label", "read_at", "missing_forms"),
            payload.keys().asSequence().toSet()
        )
        assertEquals("profile-a", payload.getString("profile_id"))
        assertEquals("label-a", payload.getString("profile_label"))
        val names = payload.getJSONArray("missing_forms")
        assertEquals(upload.missingForms, List(names.length()) { names.getString(it) })
    }

    @Test
    fun readAtIsUtcToTheSecondEndingInZ() {
        assertEquals("2026-11-23T08:12:40Z", PcgAltDexAlertUploader.formatReadAt(1_795_421_560_123L))
        assertEquals("1970-01-01T00:00:00Z", PcgAltDexAlertUploader.formatReadAt(-5L))
    }

    /**
     * Names come from store 3 as the reading stored them: 🔒 and 🎊 already removed, and no
     * sprite number, key or obtained form reaches the payload.
     */
    @Test
    fun anUploadOfAReadingSendsOnlyTheMissingNamesWithoutLockOrConfetti() {
        val listed = listOfNotNull(
            PcgAltDexKnownForms.formOf("🔒Mega Venusaur", 10033),
            PcgAltDexKnownForms.formOf("🎊PCG Absol 🎊", 100101)
        )
        val snapshot = PcgAltDexSnapshot(
            obtained = listOfNotNull(PcgAltDexKnownForms.formOf("Unown (B)", 10002)),
            missing = listed,
            readAtMs = 0L
        )
        val transport = RecordingTransport(PostJsonResult(200, "{}"))

        uploader(transport).send(PcgAltDexAlertUpload.of("profile-a", "label-a", snapshot))

        val sent = transport.payloads.single().toString()
        val names = transport.payloads.single().getJSONArray("missing_forms")
        assertEquals(listOf("Mega Venusaur", "PCG Absol"), List(names.length()) { names.getString(it) })
        assertTrue(sent, "🔒" !in sent && "🎊" !in sent)
        assertTrue(sent, "10033" !in sent && "100101" !in sent && "Unown" !in sent)
    }

    @Test
    fun aBearerSessionIsTheOnlyAuthorization() {
        val transport = RecordingTransport(PostJsonResult(200, "{}"))

        uploader(transport).send(upload)

        assertEquals(listOf("Bearer session-a"), transport.headers)
    }

    @Test
    fun noSessionOrAnUnusableOneSendsNothing() {
        listOf(
            BackendSessionLookup.Missing,
            BackendSessionLookup.Unavailable,
            BackendSessionLookup.Present("  ")
        ).forEach { lookup ->
            val transport = RecordingTransport(PostJsonResult(200, "{}"))

            val result = uploader(transport, lookup).send(upload)

            assertEquals(lookup.toString(), PcgAltDexAlertUploadResult.NotSent, result)
            assertTrue(lookup.toString(), transport.payloads.isEmpty())
        }
    }

    @Test
    fun aBlankProfileSendsNothing() {
        val transport = RecordingTransport(PostJsonResult(200, "{}"))

        val result = uploader(transport).send(upload.copy(profileId = "  "))

        assertEquals(PcgAltDexAlertUploadResult.NotSent, result)
        assertTrue(transport.payloads.isEmpty())
    }

    @Test
    fun answersAreClassified() {
        assertEquals(
            PcgAltDexAlertUploadResult.Stored(count = 3),
            PcgAltDexAlertUploader.classify(PostJsonResult(200, """{"ok":true,"count":3}"""))
        )
        assertEquals(
            PcgAltDexAlertUploadResult.AlertsOff,
            PcgAltDexAlertUploader.classify(
                PostJsonResult(409, """{"ok":false,"error":"alt_dex_alerts_off"}""")
            )
        )
        listOf(
            null,
            PostJsonResult(409, """{"ok":false,"error":"other"}"""),
            PostJsonResult(409, "not json"),
            PostJsonResult(401, """{"ok":false,"error":"alt_dex_alerts_off"}"""),
            PostJsonResult(500, "")
        ).forEach { answer ->
            assertEquals(
                answer.toString(),
                PcgAltDexAlertUploadResult.Failed,
                PcgAltDexAlertUploader.classify(answer)
            )
        }
    }

    /** A 409 is sent once and answered once: the client neither retries nor loops. */
    @Test
    fun oneCallSendsOneRequestEvenWhenRefused() {
        val transport = RecordingTransport(
            PostJsonResult(409, """{"ok":false,"error":"alt_dex_alerts_off"}""")
        )

        val result = uploader(transport).send(upload)

        assertEquals(PcgAltDexAlertUploadResult.AlertsOff, result)
        assertEquals(1, transport.payloads.size)
    }

    /** Its own log lines hold counts and results, never a name, a label or a profile. */
    @Test
    fun logLinesHoldNoNameLabelOrProfile() {
        uploader(RecordingTransport(PostJsonResult(200, """{"count":3}"""))).send(upload)
        uploader(RecordingTransport(null), BackendSessionLookup.Missing).send(upload)

        val lines = log.lines.joinToString("\n")
        assertTrue(lines, lines.isNotEmpty())
        (upload.missingForms + listOf("label-a", "profile-a")).forEach { secret ->
            assertTrue(secret, secret !in lines.lowercase() && secret !in lines)
        }
    }
}
