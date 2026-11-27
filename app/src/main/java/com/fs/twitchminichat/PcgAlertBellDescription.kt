package com.fs.twitchminichat

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

/**
 * What the chat's bell button shows and says for one profile's alert selection: its icon,
 * tint and alpha, and its accessibility description, which names the active categories by
 * the same string resources the "PCG notifications" menu uses.
 *
 * One source for all of it, so that the bell reads and looks the same at start-up as after
 * the menu's OK. Until this existed the start-up path wrote its own hard-coded English text,
 * "Alerts ON for this account", over whatever category was active (#150), and took the icon
 * and tint from the legacy push setting, so the bell looked on with every category off.
 */
object PcgAlertBellDescription {

    /** Joins the category names into one description. */
    const val SEPARATOR = "; "

    /** Tint of the bell while a category is on. */
    @ColorInt
    const val TINT_ON: Int = 0xFF66FF99.toInt()

    /** Tint of the bell while no category is on. */
    @ColorInt
    const val TINT_OFF: Int = 0xFFFF6666.toInt()

    /** Alpha of the bell while a category is on. */
    const val ALPHA_ON = 1.0f

    /** Alpha of the bell while no category is on: dimmed, so the off state is visible. */
    const val ALPHA_OFF = 0.45f

    /** The bell's icon, tint and alpha. */
    data class Appearance(
        @param:DrawableRes val iconRes: Int,
        @param:ColorInt val tint: Int,
        val alpha: Float
    )

    /**
     * The bell as [selection] has it: on when any category is on, which is when the selection
     * needs Firebase delivery and when [labelResIds] names a category; off otherwise.
     */
    fun appearance(selection: PcgProfileAlertSelection): Appearance {
        return if (selection.requiresFirebaseDelivery) {
            Appearance(R.drawable.ic_bell_on, TINT_ON, ALPHA_ON)
        } else {
            Appearance(R.drawable.ic_bell_off, TINT_OFF, ALPHA_OFF)
        }
    }

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
