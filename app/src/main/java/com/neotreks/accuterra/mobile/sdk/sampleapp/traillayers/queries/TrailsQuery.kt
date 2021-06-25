package com.neotreks.accuterra.mobile.sdk.sampleapp.traillayers.queries

import android.graphics.RectF
import androidx.annotation.UiThread
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.neotreks.accuterra.mobile.sdk.sampleapp.traillayers.layers.TrailLayerType

/**
 * Represents a Trail Map query.
 * Searches for visible trails in a given list of layers found in a rectangular area represented
 * by its center and a radius.
 */
class TrailsQuery constructor(
    private val mapboxMap: MapboxMap,
    val layers: Set<TrailLayerType>,
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

        val foundTrailIds = mutableSetOf<Long>()
        layers.forEach { layer ->
            if (style.getLayer(layer.id) != null) {
                queryRenderedTrailFeatures(layer, searchArea, foundTrailIds)
            }
        }

        return foundTrailIds
    }

    private fun queryRenderedTrailFeatures(layer: TrailLayerType,
                                           searchArea: RectF,
                                           foundTrailIds: MutableSet<Long>) {

        val features = mapboxMap.queryRenderedFeatures(searchArea, layer.id)

        features
            .forEach { feature ->
                if (layer.sourceType.featureGeometryTypes.contains(feature.geometry()?.type())) {
                    val properties = feature.properties()
                        ?: throw IllegalStateException("No properties on a feature in $layer layer.")

                    if (properties.has(layer.sourceType.idPropertyKey)) {
                        foundTrailIds.add(properties.get(layer.sourceType.idPropertyKey).asLong)
                    }
                }
            }
    }
}