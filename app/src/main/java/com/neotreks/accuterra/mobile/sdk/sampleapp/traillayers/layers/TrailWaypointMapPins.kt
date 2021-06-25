package com.neotreks.accuterra.mobile.sdk.sampleapp.traillayers.layers

import android.content.Context
import android.graphics.drawable.Drawable
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailDriveWaypoint

enum class TrailWaypointMapPins(private val activeDrawableName: String, private val inactiveDrawableName: String) {
    TRAIL_HEAD("ic_mappin_waypoint_trailhead_active", "ic_mappin_waypoint_trailhead_inactive"),
    TRAIL_END("ic_mappin_waypoint_trailend_active", "ic_mappin_waypoint_trailend_inactive"),
    TURN_LIST("ic_mappin_waypoint_turnlist_active", "ic_mappin_waypoint_turnlist_inactive"),
    INFO("ic_mappin_waypoint_info_active", "ic_mappin_waypoint_info_inactive"),
    OBSTACLE("ic_mappin_waypoint_obstacle_active", "ic_mappin_waypoint_obstacle_inactive");

    fun getDrawable(context: Context, active: Boolean): Drawable? {
        val name = if (active) activeDrawableName else inactiveDrawableName;
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        return context.getDrawable(id)
    }

    fun getName(active: Boolean): String {
        return if (active) activeDrawableName else inactiveDrawableName
    }

    companion object {

        private const val OBSTACLE_TAG_CODE = "obs"

        fun fromTrailDriveWaypoint(waypoint: TrailDriveWaypoint): TrailWaypointMapPins {
            return when {
                waypoint.point.isTrailHead() -> {
                    TRAIL_HEAD
                }
                waypoint.point.isTrailEnd() -> {
                    TRAIL_END
                }
                waypoint.point.tags.any { it.code == OBSTACLE_TAG_CODE } -> {
                    OBSTACLE
                }
                else -> {
                    TURN_LIST
                }
            }
        }
    }
}