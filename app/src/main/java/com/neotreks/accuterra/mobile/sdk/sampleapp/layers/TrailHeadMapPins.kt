package com.neotreks.accuterra.mobile.sdk.sampleapp.layers

import android.content.Context
import android.graphics.drawable.Drawable
import com.neotreks.accuterra.mobile.sdk.trail.model.TechnicalRating

enum class TrailHeadMapPins(private val activeDrawableName: String, private val inactiveDrawableName: String) {
    CLUSTER("ic_mappin_trail_cluster_active", "ic_mappin_trail_cluster_inactive"),
    EASY("ic_mappin_trail_easy_active", "ic_mappin_trail_easy_inactive"),
    MODERATE("ic_mappin_trail_moderate_active", "ic_mappin_trail_moderate_inactive"),
    DIFFICULT("ic_mappin_trail_difficult_active", "ic_mappin_trail_difficult_inactive"),
    SEVERE("ic_mappin_trail_severe_active", "ic_mappin_trail_severe_inactive"),
    EXTREME("ic_mappin_trail_extreme_active", "ic_mappin_trail_extreme_inactive");

    fun getDrawable(context: Context, active: Boolean): Drawable? {
        val name = if (active) activeDrawableName else inactiveDrawableName;
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        return context.getDrawable(id)
    }

    fun getName(active: Boolean): String {
        return if (active) activeDrawableName else inactiveDrawableName
    }

    companion object {
        fun fromAccuTerraDifficulty(technicalRating: TechnicalRating?): TrailHeadMapPins {

            return when (technicalRating?.level ?: 5) {
                1 -> EASY
                2 -> MODERATE
                3 -> DIFFICULT
                4 -> SEVERE
                5 -> EXTREME
                else -> {
                    EXTREME
                }
            }
        }
    }
}