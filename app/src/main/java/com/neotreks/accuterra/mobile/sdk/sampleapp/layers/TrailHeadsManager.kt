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
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailMarker
import com.neotreks.accuterra.mobile.sdk.trail.service.TrailLoadFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * An initialization of a trails heads layer from a given source.
 */
class TrailHeadsManager constructor(accuTerraMapView: AccuTerraMapView,
                                    lifecycleScope: LifecycleCoroutineScope
)
    : LayersManager(accuTerraMapView, lifecycleScope) {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    var isLoaded = false
        private set
    private var trails: Set<TrailBasicInfo> = mutableSetOf()
    private var selectedTrailId: Long? = null

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "TrailHeadsManager"
    }

    /**
     * Creates empty sources and layers for trail heads, clusters and selected trail head
     * This method is synchronous as sources are filled in later in setVisibleTrails
     */
    @UiThread
    fun addToMap() {
        val style = getMapboxStyle() ?: return
        isLoaded = true

        // Register map pins in style

        registerMapPins(style)

        // Add Sources

        val trailHeadsSource = addTrailHeadsSource(style)
        val selectedTrailHeadSource = addSelectedTrailHeadSource(style)

        // Add Layers

        val unclusteredTrailHeadsLayer = addTrailHeadsLayer(style, trailHeadsSource)
        val clustersLayer = addClusterIconsLayer(style, trailHeadsSource, above = unclusteredTrailHeadsLayer)
        val clusterLabelsLayer = addClusterLabelsLayer(style, trailHeadsSource, above = clustersLayer)
        addSelectedTrailHeadLayer(style, selectedTrailHeadSource, above = clusterLabelsLayer)
        setSelectedTrailId(selectedTrailId)
    }

    /**
     * Highlights trail head marker on the map.
     */
    fun setSelectedTrailId(trailId: Long?) {
        selectedTrailId = trailId
        val style = getMapboxStyle() ?: return
        if (!isLoaded) {
            return
        }

        val selectedTrailHeadLayer = style.getLayer(TrailLayerType.SELECTED_TRAIL_HEAD.id) as? SymbolLayer
        val trailHeadsLayer = style.getLayer(TrailLayerType.TRAIL_HEADS.id) as? SymbolLayer

        // When we filled the source, we added attributes to each marker. We care about the trailId attribute.

        val propertyKey = TrailLayerType.TRAIL_HEADS.sourceType.idPropertyKey

        // Filter layers using filter expressions. Check Mapbox documentation for details about expressions

        selectedTrailHeadLayer?.setFilter(
            Expression.eq(Expression.literal(propertyKey), Expression.literal(selectedTrailId ?: -1))
        )

        trailHeadsLayer?.setFilter(
            Expression.all(
                Expression.neq(Expression.literal(propertyKey), Expression.literal(selectedTrailId ?: -1)),
                Expression.not(Expression.has("cluster"))
            )
        )
    }

    @UiThread
    suspend fun setVisibleTrails(trails: Set<TrailBasicInfo>) {
        withContext(Dispatchers.Main) {
            this@TrailHeadsManager.trails = trails
            val style = getMapboxStyle() ?: return@withContext
            if (!isStyleLoaded() || !isLoaded) {
                return@withContext
            }

            val trailheadsSource = style.getSourceAs<GeoJsonSource>(TrailSourceType.TRAIL_HEADS.id)
                ?: throw IllegalStateException("The source ${TrailSourceType.TRAIL_HEADS.id} has not been added yet.")

            val selectedTrailHeadSource = style.getSourceAs<GeoJsonSource>(TrailSourceType.SELECTED_TRAIL_HEAD.id)
                ?: throw IllegalStateException("The source ${TrailSourceType.SELECTED_TRAIL_HEAD.id} has not been added yet.")

            val trailDifficultyRatingMap = mutableMapOf<Long, TechnicalRating>()
            trails.associateByTo(trailDifficultyRatingMap, { it.id }, { it.techRatingHigh })

            withContext(Dispatchers.Default) {
                val trailIds = trails.map { it.id }
                val markers = withContext(Dispatchers.IO) {
                    ServiceFactory.getTrailService(getContext()).getTrailMarkers(TrailLoadFilter(trailIds))
                }

                val features = markers.map { marker ->
                    parseMarker(marker, trailDifficultyRatingMap[marker.trailId])
                }
                val featureCollection = FeatureCollection.fromFeatures(features)

                // Put the features into the map
                withContext(Dispatchers.Main) {
                    // Both sources can share the same collection, because their layers have different styles
                    // and filters
                    trailheadsSource.setGeoJson(featureCollection)
                    selectedTrailHeadSource.setGeoJson(featureCollection)
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
            trails = mutableSetOf()
            selectedTrailId = null
            removeLayersAndSources()
        }
    }

    private fun parseMarker(marker: TrailMarker, technicalRating: TechnicalRating?): Feature {
        val pin = TrailHeadMapPins.fromAccuTerraDifficulty(technicalRating)
        return Feature.fromGeometry(
            Point.fromLngLat(marker.longitude, marker.latitude),
            JsonObject().apply {
                add("pointId", JsonPrimitive(marker.pointId))
                add("activeMapPinName", JsonPrimitive(pin.getName(true)))
                add("inactiveMapPinName", JsonPrimitive(pin.getName(false)))
                add(TrailSourceType.TRAIL_HEADS.idPropertyKey, JsonPrimitive(marker.trailId))
            }
        )
            ?: throw IllegalStateException("Cannot parse trail marker Feature from: $marker")
    }

    private fun registerMapPins(style: Style) {

        TrailHeadMapPins.values().forEach { pin ->
            pin.getDrawable(getContext(), true)?.let {
                style.addImage(pin.getName(true), it)
            }
            pin.getDrawable(getContext(), false)?.let {
                style.addImage(pin.getName(false), it)
            }
        }

    }

    private fun addTrailHeadsSource(style: Style): GeoJsonSource {
        val source = GeoJsonSource(
            TrailSourceType.TRAIL_HEADS.id,
            FeatureCollection.fromFeatures(emptyArray()),
            GeoJsonOptions()
                .withCluster(true)
                .withClusterMaxZoom(10)
                .withClusterRadius(50)
        )

        style.addSource(source)
        return source
    }

    private fun addSelectedTrailHeadSource(style: Style): GeoJsonSource {
        val source = GeoJsonSource(
            TrailSourceType.SELECTED_TRAIL_HEAD.id,
            FeatureCollection.fromFeatures(emptyArray())
        )

        style.addSource(source)
        return source
    }

    private fun addTrailHeadsLayer(style: Style, source: GeoJsonSource): SymbolLayer {
        val trailHeadsLayer = SymbolLayer(TrailLayerType.TRAIL_HEADS.id, source.id)
            .withProperties(
                PropertyFactory.iconSize(0.25f),
                PropertyFactory.iconOpacity(1.0f),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                PropertyFactory.iconIgnorePlacement(true), // VERY IMPORTANT!!
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconImage(Expression.image(Expression.get("inactiveMapPinName")))
            )
            // Set predicate, so trail heads are visible only when not clustered
            .withFilter(Expression.not(Expression.has("cluster")))

        style.addLayer(trailHeadsLayer)
        return trailHeadsLayer
    }

    private fun addClusterIconsLayer(style: Style, source: GeoJsonSource, above: SymbolLayer): SymbolLayer {
        val clustersLayer = SymbolLayer(TrailLayerType.TRAIL_HEADS_CLUSTERS.id, source.id)
            .withProperties(
                PropertyFactory.iconSize(0.25f),
                PropertyFactory.iconOpacity(1.0f),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                PropertyFactory.iconIgnorePlacement(true), // VERY IMPORTANT!!
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconImage(TrailHeadMapPins.CLUSTER.getName(false))
            )
            // Set predicate, so trail clusters are visible only when cluster attribute is true
            .withFilter(Expression.has("cluster"))

        style.addLayerAbove(clustersLayer, above.id)
        return clustersLayer
    }

    private fun addClusterLabelsLayer(style: Style, source: GeoJsonSource, above: SymbolLayer): SymbolLayer {
        val clusterLabelsLayer = SymbolLayer(TrailLayerType.TRAIL_HEADS_CLUSTERS_LABELS.id, source.id)
            .withProperties(
                PropertyFactory.textField(Expression.toString(Expression.get("point_count"))),
                PropertyFactory.textSize(12f),
                PropertyFactory.textColor(getContext().getColor(R.color.trailheadsClusterTextColor)),
                PropertyFactory.textIgnorePlacement(true),
                PropertyFactory.textAllowOverlap(true),
                // Note: The font must be supported by the map style
                PropertyFactory.textFont(arrayOf("Roboto Regular")),
                // The MapPin alligned to the bottom. We want to render text in the middle of the pin circle, so we need
                // to set y offset to move the text up
                PropertyFactory.textOffset(arrayOf(0f, -2.1f)),
            )
            // Set predicate, so trail cluster labels are visible only when cluster attribute is true
            .withFilter(Expression.has("cluster"))

        style.addLayerAbove(clusterLabelsLayer, above.id)
        return clusterLabelsLayer
    }

    private fun addSelectedTrailHeadLayer(style: Style, source: GeoJsonSource, above: SymbolLayer): SymbolLayer {
        val selectedTrailHeadsLayer = SymbolLayer(TrailLayerType.SELECTED_TRAIL_HEAD.id, source.id)
            .withProperties(
                PropertyFactory.iconSize(0.25f),
                PropertyFactory.iconOpacity(1.0f),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                PropertyFactory.iconIgnorePlacement(true), // VERY IMPORTANT!!
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconImage(Expression.image(Expression.get("activeMapPinName")))
            )

        style.addLayerAbove(selectedTrailHeadsLayer, above.id)
        return selectedTrailHeadsLayer
    }

    private fun removeLayersAndSources() {
        val style = getMapboxStyle() ?: return
        removeLayerOrFail(style, TrailLayerType.TRAIL_HEADS_CLUSTERS_LABELS.id)
        removeLayerOrFail(style, TrailLayerType.TRAIL_HEADS_CLUSTERS.id)
        removeLayerOrFail(style, TrailLayerType.TRAIL_HEADS.id)
        removeLayerOrFail(style, TrailLayerType.SELECTED_TRAIL_HEAD.id)
        removeSourceOrFail(style, TrailSourceType.TRAIL_HEADS.id)
        removeSourceOrFail(style, TrailSourceType.SELECTED_TRAIL_HEAD.id)
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
                setVisibleTrails(trails)
                setSelectedTrailId(selectedTrailId)
            }
        }
    }

    override fun onTrackingModeChanged(mode: TrackingOption) {
        // nothing to do, but style could be changed here if needed
    }
}