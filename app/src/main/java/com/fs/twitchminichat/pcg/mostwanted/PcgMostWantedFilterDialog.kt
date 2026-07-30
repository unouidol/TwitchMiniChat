package com.fs.twitchminichat.pcg.mostwanted

import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import androidx.annotation.ArrayRes
import androidx.appcompat.app.AlertDialog
import com.fs.twitchminichat.R
import com.fs.twitchminichat.pcg.catalog.PcgPokemonTier
import com.fs.twitchminichat.pcg.catalog.PcgPokemonType
import com.fs.twitchminichat.pcg.catalog.PcgSpawnAvailability
import com.fs.twitchminichat.pcg.catalog.PcgVariantKind

/**
 * Presents catalog constraints that do not occupy permanent list space.
 *
 * Evolution stages remain in the visible multi-select controls below search.
 */
class PcgMostWantedFilterDialog(
    private val context: Context,
    private val initialState: PcgMostWantedFilterState,
    private val onApply: (PcgMostWantedFilterState) -> Unit
) {

    /** Inflates, initializes and displays the filter dialog. */
    fun show() {
        val view = LayoutInflater.from(context).inflate(
            R.layout.dialog_pcg_most_wanted_filters,
            null,
            false
        )

        val spinnerTier = view.findViewById<Spinner>(R.id.spinnerMostWantedTier)
        val spinnerType = view.findViewById<Spinner>(R.id.spinnerMostWantedType)
        val spinnerGeneration =
            view.findViewById<Spinner>(R.id.spinnerMostWantedGeneration)
        val spinnerVariant =
            view.findViewById<Spinner>(R.id.spinnerMostWantedVariant)
        val spinnerAvailability =
            view.findViewById<Spinner>(R.id.spinnerMostWantedAvailability)
        val checkStarter =
            view.findViewById<CheckBox>(R.id.checkMostWantedStarter)
        val checkLegendary =
            view.findViewById<CheckBox>(R.id.checkMostWantedLegendary)
        val checkMythical =
            view.findViewById<CheckBox>(R.id.checkMostWantedMythical)
        val checkSelected =
            view.findViewById<CheckBox>(R.id.checkMostWantedSelected)
        val buttonReset =
            view.findViewById<Button>(R.id.btnResetMostWantedFilters)

        setupSpinner(
            spinnerTier,
            R.array.pcg_most_wanted_tier_filter_labels,
            initialState.tiers.singleOrNull()?.ordinal?.plus(1) ?: 0
        )
        setupSpinner(
            spinnerType,
            R.array.pcg_most_wanted_type_filter_labels,
            initialState.types.singleOrNull()?.ordinal?.plus(1) ?: 0
        )
        setupSpinner(
            spinnerGeneration,
            R.array.pcg_most_wanted_generation_filter_labels,
            initialState.generations.singleOrNull() ?: 0
        )
        setupSpinner(
            spinnerVariant,
            R.array.pcg_most_wanted_variant_filter_labels,
            initialState.variantKinds
                .singleOrNull()
                ?.ordinal
                ?.plus(1)
                ?: 0
        )
        setupSpinner(
            spinnerAvailability,
            R.array.pcg_most_wanted_availability_filter_labels,
            initialState.spawnAvailability.ordinal
        )

        checkStarter.isChecked =
            PcgMostWantedCategory.STARTER in initialState.categories
        checkLegendary.isChecked =
            PcgMostWantedCategory.LEGENDARY in initialState.categories
        checkMythical.isChecked =
            PcgMostWantedCategory.MYTHICAL in initialState.categories
        checkSelected.isChecked = initialState.selectedOnly

        buttonReset.setOnClickListener {
            spinnerTier.setSelection(0)
            spinnerType.setSelection(0)
            spinnerGeneration.setSelection(0)
            spinnerVariant.setSelection(0)
            spinnerAvailability.setSelection(0)
            checkStarter.isChecked = false
            checkLegendary.isChecked = false
            checkMythical.isChecked = false
            checkSelected.isChecked = false
        }

        AlertDialog.Builder(context)
            .setTitle(R.string.pcg_most_wanted_filters_title)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.pcg_most_wanted_apply_filters) { _, _ ->
                onApply(
                    PcgMostWantedFilterState(
                        tiers = selectedEnum(
                            spinnerTier,
                            PcgPokemonTier.entries
                        )?.let(::setOf).orEmpty(),
                        types = selectedEnum(
                            spinnerType,
                            PcgPokemonType.entries
                        )?.let(::setOf).orEmpty(),
                        generations = spinnerGeneration
                            .selectedItemPosition
                            .takeIf { position -> position > 0 }
                            ?.let(::setOf)
                            .orEmpty(),
                        evolutionStages = initialState.evolutionStages,
                        variantKinds = selectedEnum(
                            spinnerVariant,
                            PcgVariantKind.entries
                        )?.let(::setOf).orEmpty(),
                        spawnAvailability =
                            PcgSpawnAvailability.entries[
                                spinnerAvailability.selectedItemPosition
                            ],
                        categories = buildSet {
                            if (checkStarter.isChecked) {
                                add(PcgMostWantedCategory.STARTER)
                            }
                            if (checkLegendary.isChecked) {
                                add(PcgMostWantedCategory.LEGENDARY)
                            }
                            if (checkMythical.isChecked) {
                                add(PcgMostWantedCategory.MYTHICAL)
                            }
                        },
                        selectedOnly = checkSelected.isChecked
                    )
                )
            }
            .show()
    }

    /** Populates one spinner from localized string-array resources. */
    private fun setupSpinner(
        spinner: Spinner,
        @ArrayRes labelsResource: Int,
        selectedPosition: Int
    ) {
        spinner.adapter = ArrayAdapter.createFromResource(
            context,
            labelsResource,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }
        spinner.setSelection(selectedPosition)
    }

    /** Maps an All-prefixed spinner position to a nullable enum value. */
    private fun <T : Enum<T>> selectedEnum(
        spinner: Spinner,
        values: List<T>
    ): T? {
        val position = spinner.selectedItemPosition
        return if (position == 0) {
            null
        } else {
            values[position - 1]
        }
    }
}