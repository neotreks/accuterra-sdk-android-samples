package com.neotreks.accuterra.mobile.sdk.sampleapp.layers

import androidx.annotation.UiThread
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraMapView
import com.neotreks.accuterra.mobile.sdk.map.TrackingOption
import com.neotreks.accuterra.mobile.sdk.sampleapp.R
import com.neotreks.accuterra.mobile.sdk.trail.model.TechnicalRating
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailBasicInfo
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailDriveWaypoint
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailMarker
import com.neotreks.accuterra.mobile.sdk.trail.service.TrailLoadFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * An initialization of a trails waypoints layer from a given source.
 */
class TrailWaypointsManager constructor(accuTerraMapView: AccuTerraMapView,
                                        lifecycleScope: LifecycleCoroutineScope
)
    : LayersManager(accuTerraMapView, lifecycleScope) {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    var isLoaded = false
        private set
    private var waypoints: Set<TrailDriveWaypoint> = mutableSetOf()
    private var selectedWaypointId: Long? = null

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "TrailHeadsManager"
    }

    /**
     * Creates empty sources and layers for trail waypoints
     * This method is synchronous as sources are filled in later in setVisibleWaypoints
     */
    @UiThread
    fun addToMap() {
        val style = getMapboxStyle() ?: return
        isLoaded = true

        // Register map pins in style

        registerMapPins(style)

        // Add Sources

        val trailWaypointsSource = addTrailWaypointsSource(style)

        // Add Layers

        val trailWaypointsLayer = addTrailWaypointsLayer(style, trailWaypointsSource)
        addSelectedTrailWaypointLayer(style, trailWaypointsSource, above = trailWaypointsLayer)
        setSelectedWaypointId(selectedWaypointId)
    }

    /**
     * Highligts waypoint marker on the map.
     */
    fun setSelectedWaypointId(waypointId: Long?) {
        selectedWaypointId = waypointId
        val style = getMapboxStyle() ?: return
        if (!isLoaded) {
            return
        }

        val selectedTrailWaypointLayer = style.getLayer(TrailLayerType.SELECTED_TRAIL_WAYPOINT.id) as? SymbolLayer
        val trailWaypointsLayer = style.getLayer(TrailLayerType.TRAIL_WAYPOINTS.id) as? SymbolLayer

        // When we filled the source, we added attributes to each marker. We care about the id attribute.

        val propertyKey = TrailLayerType.TRAIL_WAYPOINTS.sourceType.idPropertyKey

        // Filter layers using predicates. Check Mapbox documentation for details about predicates

        selectedTrailWaypointLayer?.setFilter(
            Expression.eq(Expression.literal(propertyKey), Expression.literal(selectedWaypointId ?: -1))
        )

        trailWaypointsLayer?.setFilter(
            Expression.all(
                Expression.neq(Expression.literal(propertyKey), Expression.literal(selectedWaypointId ?: -1)),
            )
        )
    }

    @UiThread
    suspend fun setVisibleWaypoints(waypoints: Set<TrailDriveWaypoint>) {
        withContext(Dispatchers.Main) {
            this@TrailWaypointsManager.waypoints = waypoints
            val style = getMapboxStyle() ?: return@withContext
            if (!isStyleLoaded() || !isLoaded) {
                return@withContext
            }

            val waypointsSource = style.getSourceAs<GeoJsonSource>(TrailSourceType.TRAIL_WAYPOINTS.id)
                ?: throw IllegalStateException("The source ${TrailSourceType.TRAIL_WAYPOINTS.id} has not been added yet.")

            withContext(Dispatchers.Default) {
                val features = waypoints.map { waypoint ->
                    parseWaypoint(waypoint)
                }
                val featureCollection = FeatureCollection.fromFeatures(features)

                // Put the features into the map
                withContext(Dispatchers.Main) {
                    waypointsSource.setGeoJson(featureCollection)
                }
            }
        }
    }

    /**
     * Resets the manager as it would be not initialized yet.
     */
    suspend fun resetManager() {
        withContext(Dispatchers.Main) {
            // Clean selection
            isLoaded = false
            waypoints = mutableSetOf()
            selectedWaypointId = null
            removeLayersAndSources()
        }
    }

    private fun parseWaypoint(waypoint: TrailDriveWaypoint): Feature {
        val pin = TrailWaypointMapPins.fromTrailDriveWaypoint(waypoint)
        return Feature.fromGeometry(
            Point.fromLngLat(waypoint.point.location.longitude, waypoint.point.location.latitude),
            JsonObject().apply {
                add("pointId", JsonPrimitive(waypoint.id))
                add("activeMapPinName", JsonPrimitive(pin.getName(true)))
                add("inactiveMapPinName", JsonPrimitive(pin.getName(false)))
                add(TrailSourceType.TRAIL_WAYPOINTS.idPropertyKey, JsonPrimitive(waypoint.id))
            }
        )
            ?: throw IllegalStateException("Cannot parse trail drive waypoint Feature from: $waypoint")
    }

    private fun registerMapPins(style: Style) {

        TrailWaypointMapPins.values().forEach { pin ->
            pin.getDrawable(getContext(), true)?.let {
                style.addImage(pin.getName(true), it)
            }
            pin.getDrawable(getContext(), false)?.let {
                style.addImage(pin.getName(false), it)
            }
        }

    }

    private fun addTrailWaypointsSource(style: Style): GeoJsonSource {
        val source = GeoJsonSource(
            TrailSourceType.TRAIL_WAYPOINTS.id,
            FeatureCollection.fromFeatures(emptyArray()),
            GeoJsonOptions()
        )

        style.addSource(source)
        return source
    }

    private fun addTrailWaypointsLayer(style: Style, source: GeoJsonSource): SymbolLayer {
        val trailWaypointsLayer = SymbolLayer(TrailLayerType.TRAIL_WAYPOINTS.id, source.id)
            .withProperties(
                PropertyFactory.iconSize(0.25f),
                PropertyFactory.iconOpacity(1.0f),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                PropertyFactory.iconIgnorePlacement(true), // VERY IMPORTANT!!
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconImage(Expression.image(Expression.get("inactiveMapPinName")))
            )

        style.addLayer(trailWaypointsLayer)
        return trailWaypointsLayer
    }

    private fun addSelectedTrailWaypointLayer(style: Style, source: GeoJsonSource, above: SymbolLayer): SymbolLayer {
        val selectedTrailWaypointLayer = SymbolLayer(TrailLayerType.SELECTED_TRAIL_WAYPOINT.id, source.id)
            .withProperties(
                PropertyFactory.iconSize(0.25f),
                PropertyFactory.iconOpacity(1.0f),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                PropertyFactory.iconIgnorePlacement(true), // VERY IMPORTANT!!
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconImage(Expression.image(Expression.get("activeMapPinName")))
            )

        style.addLayerAbove(selectedTrailWaypointLayer, above.id)
        return selectedTrailWaypointLayer
    }

    private fun removeLayersAndSources() {
        val style = getMapboxStyle() ?: return
        removeLayerOrFail(style, TrailLayerType.TRAIL_WAYPOINTS.id)
        removeLayerOrFail(style, TrailLayerType.SELECTED_TRAIL_WAYPOINT.id)
        removeSourceOrFail(style, TrailSourceType.TRAIL_WAYPOINTS.id)
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onStyleChangeStart() {
        // Nothing to do here
    }

    override fun onStyleChanged() {
        if (isLoaded) {
            this.addToMap()
            lifecycleScope.launch {
                setVisibleWaypoints(waypoints)
                setSelectedWaypointId(selectedWaypointId)
            }
        }
    }

    override fun onTrackingModeChanged(mode: TrackingOption) {
        // nothing to do, but style could be changed here if needed
    }
}