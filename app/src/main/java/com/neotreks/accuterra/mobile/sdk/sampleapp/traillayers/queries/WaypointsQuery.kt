package com.neotreks.accuterra.mobile.sdk.sampleapp.traillayers.queries

import android.graphics.RectF
import androidx.annotation.UiThread
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.neotreks.accuterra.mobile.sdk.sampleapp.traillayers.layers.TrailLayerType

/**
 * Represents a Trail Waypoints Map query.
 * Searches for visible waypoints in a given list of layers found in a rectangular area represented
 * by its center and a radius.
 */
class WaypointsQuery constructor(
    private val mapboxMap: MapboxMap,
    val latLng: LatLng,
    val distanceTolerance: Float) {

    /**
     * Executes the query on map.
     */
    @UiThread
    fun execute(): Set<Long> {
        val style = mapboxMap.style
            ?: throw IllegalStateException("The map style has not been initialized yet.")

        val centerPoint = mapboxMap.projection.toScreenLocation(latLng)

        val searchArea = RectF(
            centerPoint.x - distanceTolerance,
            centerPoint.y - distanceTolerance,
            centerPoint.x + distanceTolerance,
            centerPoint.y + distanceTolerance)

        val foundWaypointIds = mutableSetOf<Long>()
        setOf(TrailLayerType.TRAIL_WAYPOINTS, TrailLayerType.SELECTED_TRAIL_WAYPOINT).forEach { layer ->
            if (style.getLayer(layer.id) != null) {
                queryRenderedWaypointFeatures(layer, searchArea, foundWaypointIds)
            }
        }

        return foundWaypointIds
    }

    private fun queryRenderedWaypointFeatures(layer: TrailLayerType,
                                           searchArea: RectF,
                                              foundWaypointIds: MutableSet<Long>) {

        val features = mapboxMap.queryRenderedFeatures(searchArea, layer.id)

        features
            .forEach { feature ->
                if (layer.sourceType.featureGeometryTypes.contains(feature.geometry()?.type())) {
                    val properties = feature.properties()
                        ?: throw IllegalStateException("No properties on a feature in $layer layer.")

                    if (properties.has(layer.sourceType.idPropertyKey)) {
                        foundWaypointIds.add(properties.get(layer.sourceType.idPropertyKey).asLong)
                    }
                }
            }
    }
}