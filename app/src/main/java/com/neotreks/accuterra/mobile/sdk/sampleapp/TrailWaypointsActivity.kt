package com.neotreks.accuterra.mobile.sdk.sampleapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraMapView
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraStyle
import com.neotreks.accuterra.mobile.sdk.map.TrackingOption
import com.neotreks.accuterra.mobile.sdk.model.OrderByBuilder
import com.neotreks.accuterra.mobile.sdk.model.OrderByProperty
import com.neotreks.accuterra.mobile.sdk.model.QueryLimitBuilder
import com.neotreks.accuterra.mobile.sdk.model.SortOrder
import com.neotreks.accuterra.mobile.sdk.sampleapp.databinding.ActivityTrailHeadsBinding
import com.neotreks.accuterra.mobile.sdk.sampleapp.databinding.ActivityTrailWaypointsBinding
import com.neotreks.accuterra.mobile.sdk.sampleapp.layers.TrailLayerType
import com.neotreks.accuterra.mobile.sdk.sampleapp.layers.TrailLayersManager
import com.neotreks.accuterra.mobile.sdk.sampleapp.queries.TrailsQuery
import com.neotreks.accuterra.mobile.sdk.sampleapp.queries.WaypointsQuery
import com.neotreks.accuterra.mobile.sdk.trail.model.*
import com.neotreks.accuterra.mobile.sdk.util.DelayedLastCallExecutor
import java.lang.ref.WeakReference

class TrailWaypointsActivity : MapActivity() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "TrailWaypointsActivity"
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: ActivityTrailWaypointsBinding
    private lateinit var trailLayersManager: TrailLayersManager
    private val myLocation: MapLocation = MapLocation(39.4, -104.84) // Castle Rock

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrailWaypointsBinding.inflate(layoutInflater)
        accuterraMapView = binding.accuterraMapView
        setContentView(binding.root)
    }

    override fun handleMapViewChanged() {
        // we don't do any changes when user pans map while displaying trail path and waypoints
    }

    override suspend fun addLayers() {
        trailLayersManager = TrailLayersManager(accuterraMapView, lifecycleScope)

        // Find most close trail from my location
        var trails = ServiceFactory.getTrailService(accuterraMapView.context)
            .findTrails(
                TrailMapSearchCriteria(
                mapCenter = myLocation,
                distanceRadius = DistanceSearchCriteriaBuilder.build(Int.MAX_VALUE, Comparison.LESS),
                orderBy = OrderByBuilder.build(OrderByProperty.DISTANCE, SortOrder.ASCENDING),
                limit = QueryLimitBuilder.build(1))
            )

        var waypoints = mutableSetOf<TrailDriveWaypoint>()
        trails.firstOrNull()?.let {
            // Drives contain navigation information, for now we assume each trail has exactly one drive,
            // but there can be more drives in future, for example it might be possible to drive the trail
            // from trail head to trail end or in opposite direction
            // In each case the geometry might be different, for example when there is one-way segment on the
            // trail. Also waypoint instructions might be different for each drive
            ServiceFactory.getTrailService(this).getTrailDrives(it.id)?.let { trailDrives ->
                trailDrives.firstOrNull()?.let { trailDrive ->
                    waypoints.addAll(trailDrive.waypoints)
                }
            }
        }

        // Zoom to bounding box of the trail
        zoomToBoundsExtent(trails.map { it.locationInfo.mapBounds }.toSet(), object: MapboxMap.CancelableCallback {
            override fun onCancel() {
            }

            override fun onFinish() {
                lifecycleScope.launchWhenResumed {
                    trailLayersManager.addLayers(trails.toSet(), waypoints, false)
                    trailLayersManager.selectTrail(trails.firstOrNull()?.id)
                }
            }
        })
    }

    override fun handleMapViewClick(latLng: LatLng) {
        lifecycleScope.launchWhenResumed {
            val waypointsQuery = WaypointsQuery(mapboxMap, latLng, 5.0f)
            val result = waypointsQuery.execute()
            trailLayersManager.selectWaypoint(result.firstOrNull())
        }
    }
}
