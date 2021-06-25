package com.neotreks.accuterra.mobile.sdk.sampleapp.layers


/**
 * An enumeration of well known trail related layers.
 */
enum class TrailLayerType(
    internal val id: String,
    internal val sourceType: TrailSourceType
) {
    /**
     * A highlighted trail path layer.
     * Visible on all zoom levels.
     */
    SELECTED_TRAIL_PATH("selected-trail-path", TrailSourceType.SELECTED_TRAIL_PATH),

    /**
     * A trail heads layer.
     * Clustered on lower zoom levels.
     */
    TRAIL_HEADS( "trail-heads", TrailSourceType.TRAIL_HEADS),

    /**
     * Selected trail head layer.
     * Never clustered and above clusters
     */
    SELECTED_TRAIL_HEAD("selected-trail-head", TrailSourceType.SELECTED_TRAIL_HEAD),

    /**
     * Cluster overlay for trail heads
     */
    TRAIL_HEADS_CLUSTERS("trail-heads-clusters", TrailSourceType.TRAIL_HEADS),

    /**
     * Cluster labels
     */
    TRAIL_HEADS_CLUSTERS_LABELS("trail-heads-clusters-labels", TrailSourceType.TRAIL_HEADS),

    /**
     * Trail waypoints
     */
    TRAIL_WAYPOINTS("trail-waypoints", TrailSourceType.TRAIL_WAYPOINTS),

    /**
     * Selected Trail Waypoint
     */
    SELECTED_TRAIL_WAYPOINT("selected-trail-waypoint", TrailSourceType.TRAIL_WAYPOINTS);
}