package com.neotreks.accuterra.mobile.sdk.sampleapp.traillayers.layers

import androidx.annotation.UiThread
import androidx.lifecycle.LifecycleCoroutineScope
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraMapView
import com.neotreks.accuterra.mobile.sdk.map.TrackingOption
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailBasicInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * An initialization of a trail paths and selected trail paths layers from a given source.
 */
class TrailPathsManager constructor(accuTerraMapView: AccuTerraMapView,
                                    lifecycleScope: LifecycleCoroutineScope
)
    : LayersManager(accuTerraMapView, lifecycleScope) {

    var isLoaded = false
        private set
    private var selectedTrail: TrailBasicInfo? = null

    companion object {
        private const val TAG = "TrailPathsManager"
    }

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    @UiThread
    fun addToMap() {
        val style = getMapboxStyle() ?: return
        isLoaded = true

        val selectedTrailPathSource = addSelectedTrailPathSource(style)
        addSelectedTrailPathLayer(style, selectedTrailPathSource)
    }

    @UiThread
    suspend fun setSelectedTrail(trail: TrailBasicInfo?) {
        withContext(Dispatchers.Main) {
            this@TrailPathsManager.selectedTrail = trail

            val style = getMapboxStyle() ?: return@withContext
            if (!isStyleLoaded() || !isLoaded) {
                return@withContext
            }

            val source = style.getSourceAs<GeoJsonSource>(TrailSourceType.SELECTED_TRAIL_PATH.id)
                ?: throw IllegalStateException("The source ${TrailSourceType.SELECTED_TRAIL_PATH.id} has not been added yet.")

            if (trail == null) {
                source.setGeoJson(FeatureCollection.fromFeatures(arrayOf()))
                return@withContext
            }

            withContext(Dispatchers.Default) {

                var features = mutableListOf<Feature>()
                val trailService = ServiceFactory.getTrailService(getContext())

                // Drives contain navigation information, we assume each trail has only one drive, but there can be more drives in future, for example
                // It might be possible to drive the trail from trail head to trail end or in opposite direction
                // In each case the geometry might be different, for example when there is one-way segment on the trail

                trail?.id?.let { trailId ->
                    val drives = trailService.getTrailDrives(trailId)
                    drives.firstOrNull()?.let { drive ->
                        features.add(parseTrailDrive(drive.trailDrivePath))
                    }
                }

                // Update geometries for the layer source, must be done on main thread
                withContext(Dispatchers.Main) {
                    source.setGeoJson(FeatureCollection.fromFeatures(features))
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
            selectedTrail = null
            removeLayersAndSources()
        }
    }

    private fun addSelectedTrailPathSource(style: Style): GeoJsonSource {
        val source = GeoJsonSource(TrailSourceType.SELECTED_TRAIL_PATH.id, FeatureCollection.fromFeatures(emptyArray()))
        style.addSource(source)
        return source
    }

    private fun addSelectedTrailPathLayer(style: Style, source: GeoJsonSource) {
        val selectedTrailLayer = LineLayer(TrailLayerType.SELECTED_TRAIL_PATH.id, source.id)
            .withProperties(
                PropertyFactory.lineColor("rgb(1,92,212)"),
                PropertyFactory.lineWidth(6f),
                PropertyFactory.lineOpacity(1f)
            )

        // We will insert the selected trail path layer below selected trail head layer, if present

        style.getLayer(TrailLayerType.SELECTED_TRAIL_HEAD.id)?.let {
            style.addLayerBelow(selectedTrailLayer, it.id)
        } ?: run {
            style.addLayer(selectedTrailLayer)
        }
    }

    /**
     * Converts AccuTerra Trail drive path to Feature
     * It is also possible to fill attributes here and use them to style the layer.
     */
    private fun parseTrailDrive(trailDrivePath: LineString): Feature {
        return Feature.fromGeometry(trailDrivePath)
            ?: throw IllegalStateException("Cannot parse trail Feature from GeoJson: ${trailDrivePath.toJson()}")
    }

    private fun removeLayersAndSources() {
        getMapboxStyle()?.let { style ->
            removeLayerOrFail(style, TrailLayerType.SELECTED_TRAIL_PATH.id)
            removeSourceOrFail(style, TrailSourceType.SELECTED_TRAIL_PATH.id)
        }
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onStyleChangeStart() {
        // Nothing to do here
    }

    override fun onStyleChanged() {
        if (isLoaded) {
            addToMap()
            lifecycleScope.launch {
                setSelectedTrail(this@TrailPathsManager.selectedTrail)
            }
        }
    }

    override fun onTrackingModeChanged(mode: TrackingOption) {
        // nothing to do, but style could be changed here if needed
    }
}