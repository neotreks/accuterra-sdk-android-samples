package com.neotreks.accuterra.mobile.sdk.sampleapp.traillayers.layers

/**
 * An enumeration of the sources for the trails layers.
 *
 * @param id The layerId used in mapbox.
 * @param idPropertyKey Name of the *Long* feature property that carries the Trail ID information.
 * @param featureGeometryTypes The type of the feature geometry.
 */
enum class TrailSourceType(
    val id: String,
    val idPropertyKey: String,
    val featureGeometryTypes: Array<String>
) {
    SELECTED_TRAIL_PATH("elected-trail-path", "id", arrayOf("LineString", "MultiLineString")),
    TRAIL_HEADS("trail-heads", "trailId", arrayOf("Point")),
    SELECTED_TRAIL_HEAD("selected-trail-head", "trailId", arrayOf("Point")),
    TRAIL_WAYPOINTS("trail-waypoints", "id", arrayOf("Point"));
}