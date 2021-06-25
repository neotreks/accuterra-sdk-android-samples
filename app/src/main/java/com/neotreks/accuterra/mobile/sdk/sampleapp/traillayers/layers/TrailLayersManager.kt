package com.neotreks.accuterra.mobile.sdk.sampleapp.traillayers.layers

import androidx.annotation.UiThread
import androidx.lifecycle.LifecycleCoroutineScope
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraMapView
import com.neotreks.accuterra.mobile.sdk.map.TrackingOption
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailBasicInfo
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailDriveWaypoint

/**
 * An manager of the well known trails related layers.
 */
class TrailLayersManager internal constructor(accuTerraMapView: AccuTerraMapView,
                                              lifecycleScope: LifecycleCoroutineScope
)
    : LayersManager(accuTerraMapView, lifecycleScope) {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val trailPathsManager: TrailPathsManager = TrailPathsManager(accuTerraMapView, lifecycleScope)
    private val trailHeadsManager: TrailHeadsManager = TrailHeadsManager(accuTerraMapView, lifecycleScope)
    private val trailWaypointsManager: TrailWaypointsManager = TrailWaypointsManager(accuTerraMapView, lifecycleScope)

    private var trails: Set<TrailBasicInfo> = mutableSetOf()
    private var selectedTrailId: Long? = null
    private var waypoints: Set<TrailDriveWaypoint> = mutableSetOf()
    private var selectedWaypointId: Long? = null

    var isLoaded = false
        private set

    private fun getSelectedTrail(): TrailBasicInfo? {
        return trails.firstOrNull { trail -> selectedTrailId?.let { trail.id == it } ?: false }
    }

    private fun getSelectedWaypoint(): TrailDriveWaypoint? {
        return waypoints.firstOrNull { waypoint -> selectedWaypointId?.let { waypoint.id == it } ?: false }
    }

    @UiThread
    suspend fun setVisibleTrails(trails: Set<TrailBasicInfo>) {
        this.trails = trails
        if (isStyleLoaded()) {
            trailPathsManager.setSelectedTrail(getSelectedTrail())
            trailHeadsManager.setVisibleTrails(trails)
        }
    }

    /**
     * Highlights a trail identified by its *trailId* on the map.
     */
    @UiThread
    suspend fun selectTrail(trailId: Long?) {
        selectedTrailId = trailId
        if (isStyleLoaded()) {
            trailPathsManager.setSelectedTrail(getSelectedTrail())
            trailHeadsManager.setSelectedTrailId(trailId)
        }
    }

    @UiThread
    suspend fun setVisibleWaypoints(waypoints: Set<TrailDriveWaypoint>) {
        this.waypoints = waypoints
        if (isStyleLoaded()) {
            trailWaypointsManager.setVisibleWaypoints(waypoints)
        }
    }

    /**
     * Highlight waypoint on the map
     */
    fun selectWaypoint(waypointId: Long?) {
        this.selectedWaypointId = waypointId
        if (isStyleLoaded()) {
            trailWaypointsManager.setSelectedWaypointId(selectedWaypointId)
        }
    }

    /**
     * Ads clustered trail heads and selected trail path to the map as layers
     */
    @UiThread
    suspend fun addLayers(trails: Set<TrailBasicInfo>, waypoints: Set<TrailDriveWaypoint>, showTrailHeads: Boolean) {
        isLoaded = true
        this.trails = trails
        this.waypoints = waypoints
        if (showTrailHeads) {
            addTrailHeads()
        }
        addTrailPaths()
        addTrailWaypoints()
    }

    private suspend fun addTrailHeads() {
        if (!isStyleLoaded()) {
            throw IllegalStateException("The map style is not available yet. Call initialize() and wait for the onInitialized() event.")
        }

        trailHeadsManager.addToMap()
        trailHeadsManager.setVisibleTrails(trails)
        trailHeadsManager.setSelectedTrailId(selectedTrailId)
    }

    private suspend fun addTrailPaths() {
        if (!isStyleLoaded()) {
            throw IllegalStateException("The map style is not available yet. Call initialize() and wait for the onInitialized() event.")
        }

        trailPathsManager.addToMap()
        trailPathsManager.setSelectedTrail(getSelectedTrail())
    }

    private suspend fun addTrailWaypoints() {
        if (!isStyleLoaded()) {
            throw IllegalStateException("The map style is not available yet. Call initialize() and wait for the onInitialized() event.")
        }

        trailWaypointsManager.addToMap()
        trailWaypointsManager.setVisibleWaypoints(waypoints)
        trailWaypointsManager.setSelectedWaypointId(selectedWaypointId)
    }

    /**
     * Reloads map layers. Used when layer data has been changed e.g. after trail DB update.
     */
    suspend fun reloadLayers() {
        // Remove existing layers
        trailPathsManager.resetManager()
        trailHeadsManager.resetManager()
        trailWaypointsManager.resetManager()

        // Load standard layers - it is expected that layer were removed!
        addLayers(trails, waypoints, trailHeadsManager.isLoaded)
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onStyleChangeStart() {
    }

    override fun onStyleChanged() {
        trailPathsManager.onStyleChanged()
        trailHeadsManager.onStyleChanged()
        trailWaypointsManager.onStyleChanged()
    }

    override fun onTrackingModeChanged(mode: TrackingOption) {
        trailPathsManager.onTrackingModeChanged(mode)
        trailHeadsManager.onTrackingModeChanged(mode)
        trailWaypointsManager.onTrackingModeChanged(mode)
    }
}