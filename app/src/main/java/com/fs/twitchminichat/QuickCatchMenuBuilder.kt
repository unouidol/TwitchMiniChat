package com.fs.twitchminichat

import android.content.Context

/**
 * Builds the visual entries shown by QuickCatchPresetMenuAdapter.
 *
 * This builder owns the quick catch menu structure:
 *
 * Smart presets
 *   - temporary runtime rows
 *   - generated from recommendation data
 *   - not saved into the user's preset list
 *
 * User presets
 *   - saved/manual preset rows
 *   - visible only when enabled by the user
 *
 * ChatFragment should only pass current UI state and callbacks. It should not
 * assemble section headers, subtitles, count text, or recommendation rows.
 */
object QuickCatchMenuBuilder {

    fun build(
    context: Context,
    userPresets: List<CatchPreset>,
    countsByBallId: Map<String, Int>,
    profileId: String?,
    recommendationSet: QuickCatchRecommendationSet
    ): List<QuickCatchPresetMenuEntry> {
        /**
         * Friend Ball subtitle is profile/buddy based, so compute it once for this
         * menu build and reuse it for both Smart and User rows.
         */
        val friendBallSubtitle = FriendBallSubtitleFormatter.build(
            context = context,
            profileId = profileId
        ).orEmpty()
        val smartRows = buildSmartRows(
            context = context,
            countsByBallId = countsByBallId,
            recommendations = recommendationSet.visibleSmartRecommendations,
            friendBallSubtitle = friendBallSubtitle
        )

        val userRows = buildUserRows(
            context = context,
            userPresets = userPresets,
            countsByBallId = countsByBallId,
            recommendations = recommendationSet.visibleUserRecommendations,
            friendBallSubtitle = friendBallSubtitle
        )

        return buildList {
            if (smartRows.isNotEmpty()) {
                add(
                    QuickCatchPresetMenuEntry.Header(
                        title = context.getString(R.string.catch_section_smart_presets)
                    )
                )

                smartRows.forEach { row ->
                    add(QuickCatchPresetMenuEntry.PresetRow(row))
                }
            }

            if (userRows.isNotEmpty()) {
                add(
                    QuickCatchPresetMenuEntry.Header(
                        title = context.getString(R.string.catch_section_user_presets)
                    )
                )

                userRows.forEach { row ->
                    add(QuickCatchPresetMenuEntry.PresetRow(row))
                }
            }
        }
    }

    /**
     * Builds Smart Preset rows from runtime recommendation data.
     *
     * These rows are generated from the internal ball catalog through
     * QuickCatchRecommendationProvider. They are not saved user presets.
     */
    private fun buildSmartRows(
        context: Context,
        countsByBallId: Map<String, Int>,
        recommendations: List<CatchBallRecommendation>,
        friendBallSubtitle: String
    ): List<QuickCatchPresetRow> {
        return recommendations
            .distinctBy { recommendation -> recommendation.preset.id }
            .map { recommendation ->
                val preset = recommendation.preset
                val count = resolveDisplayedCountForPreset(
                    preset = preset,
                    countsByBallId = countsByBallId
                )

                val subtitle = when {
                    CatchPresetBallHelper.isFriendBallPreset(preset) ->
                        friendBallSubtitle

                    else ->
                        CatchBallReasonFormatter.format(
                            context = context,
                            reasonKeys = recommendation.reasonKeys
                        )
                }

                QuickCatchPresetRow(
                    preset = preset,
                    label = preset.label,
                    countText = count?.toString() ?: "-",
                    subtitle = subtitle,
                    showBuyButton = CatchPresetBallHelper.canBuyFromPreset(preset),
                    showBuddyButton = CatchPresetBallHelper.isFriendBallPreset(preset)
                )
            }
    }

    /**
     * Builds User Preset rows from the user's enabled preset list.
     *
     * User rows are not created from recommendation data. Recommendation data is
     * only used to add useful subtitles/reasons to rows that already exist.
     */
    private fun buildUserRows(
        context: Context,
        userPresets: List<CatchPreset>,
        countsByBallId: Map<String, Int>,
        recommendations: List<CatchBallRecommendation>,
        friendBallSubtitle: String
    ): List<QuickCatchPresetRow> {
        val recommendationByPresetId = recommendations.associateBy { recommendation ->
            recommendation.preset.id
        }

        return userPresets.map { preset ->
            val count = resolveDisplayedCountForPreset(
                preset = preset,
                countsByBallId = countsByBallId
            )

            val recommendation = recommendationByPresetId[preset.id]

            val subtitle = when {
                CatchPresetBallHelper.isFriendBallPreset(preset) ->
                    friendBallSubtitle

                else ->
                    CatchBallReasonFormatter.format(
                        context = context,
                        reasonKeys = recommendation?.reasonKeys.orEmpty()
                    )
            }

            QuickCatchPresetRow(
                preset = preset,
                label = preset.label,
                countText = count?.toString() ?: "-",
                subtitle = subtitle,
                showBuyButton = CatchPresetBallHelper.canBuyFromPreset(preset),
                showBuddyButton = CatchPresetBallHelper.isFriendBallPreset(preset)
            )
        }
    }

    /**
     * Resolves the inventory count displayed on the right side of each row.
     *
     * This belongs in the menu builder because it is presentation-model logic:
     * it decides what the quick catch row should display.
     */
    private fun resolveDisplayedCountForPreset(
        preset: CatchPreset,
        countsByBallId: Map<String, Int>
    ): Int? {
        val effectiveBallId = CatchPresetBallHelper.effectiveBallId(preset) ?: return null
        return countsByBallId[effectiveBallId]
    }
}