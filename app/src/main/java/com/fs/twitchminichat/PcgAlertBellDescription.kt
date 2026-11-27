package com.fs.twitchminichat

/**
 * What the chat's bell button says to accessibility services: the alert categories a
 * profile has on, by the same string resources the "PCG notifications" menu uses.
 *
 * One source for the description, so that it reads the same at start-up as after the
 * menu's OK. Until this existed the start-up path wrote its own hard-coded English text,
 * "Alerts ON for this account", over whatever category was active.
 */
object PcgAlertBellDescription {

    /** Joins the category names into one description. */
    const val SEPARATOR = "; "

    /**
     * The string resources naming each category [selection] has on, in the menu's order:
     * the standard spawn mode, event spawns, Most Wanted, Missing Alt Dex forms.
     *
     * With nothing on, the title of [PcgSpawnAlertMode.NONE] alone, so the description is
     * never empty.
     */
    fun labelResIds(selection: PcgProfileAlertSelection): List<Int> {
        val settings = selection.spawnSettings

        val active = buildList {
            if (settings.regularMode != PcgSpawnAlertMode.NONE) {
                add(settings.regularMode.titleRes)
            }
            if (settings.eventSpawnsEnabled) {
                add(R.string.pcg_event_spawn_alert_label)
            }
            if (selection.mostWantedEnabled) {
                add(R.string.pcg_most_wanted_alert_label)
            }
            if (selection.altDexFormAlertsEnabled) {
                add(R.string.pcg_alt_dex_form_alerts_name)
            }
        }

        return active.ifEmpty { listOf(PcgSpawnAlertMode.NONE.titleRes) }
    }
}
