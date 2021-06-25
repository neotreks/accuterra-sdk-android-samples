package com.neotreks.accuterra.mobile.sdk.sampleapp.traillayers

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.model.OrderByBuilder
import com.neotreks.accuterra.mobile.sdk.model.OrderByProperty
import com.neotreks.accuterra.mobile.sdk.model.QueryLimitBuilder
import com.neotreks.accuterra.mobile.sdk.model.SortOrder
import com.neotreks.accuterra.mobile.sdk.sampleapp.MapActivity
import com.neotreks.accuterra.mobile.sdk.sampleapp.databinding.ActivityTrailHeadsBinding
import com.neotreks.accuterra.mobile.sdk.sampleapp.traillayers.layers.TrailLayerType
import com.neotreks.accuterra.mobile.sdk.sampleapp.traillayers.layers.TrailLayersManager
import com.neotreks.accuterra.mobile.sdk.sampleapp.traillayers.queries.TrailsQuery
import com.neotreks.accuterra.mobile.sdk.trail.model.*
import com.neotreks.accuterra.mobile.sdk.util.DelayedLastCallExecutor

class TrailHeadsActivity : MapActivity() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "TrailHeadsActivity"
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: ActivityTrailHeadsBinding
    private lateinit var trailLayersManager: TrailLayersManager
    private val trailsReloadExecutor = DelayedLastCallExecutor(1500)
    private val myLocation: MapLocation = MapLocation(39.4, -104.84) // Castle Rock

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrailHeadsBinding.inflate(layoutInflater)
        accuterraMapView = binding.accuterraMapView
        setContentView(binding.root)
    }

    override fun handleMapViewChanged() {
        if (!trailLayersManager.isLoaded) {
            // Return when trail layers manager is not loaded yet, this can happen
            // on initial zoom that we do in addLayers method
            return
        }
        lifecycleScope.launchWhenResumed {
            trailsReloadExecutor.execute {
                // Change the filter when user pans the map to rather
                var trails = ServiceFactory.getTrailService(accuterraMapView.context)
                    .findTrails(
                        TrailMapBoundsSearchCriteria(
                            mapBounds = getVisibleMapBounds(),
                            limit = QueryLimitBuilder.build(100)
                        )
                    )

                trailLayersManager.setVisibleTrails(trails.toSet())
            }
        }
    }

    override suspend fun addLayers() {
        trailLayersManager = TrailLayersManager(accuterraMapView, lifecycleScope)

        // Find first 50 closest trails from my location
        var trails = ServiceFactory.getTrailService(accuterraMapView.context)
            .findTrails(TrailMapSearchCriteria(
                mapCenter = myLocation,
                distanceRadius = DistanceSearchCriteriaBuilder.build(Int.MAX_VALUE, Comparison.LESS),
                orderBy = OrderByBuilder.build(OrderByProperty.DISTANCE, SortOrder.ASCENDING),
                limit = QueryLimitBuilder.build(50)))

        // Zoom to bounding box of these trails
        zoomToBoundsExtent(trails.map { it.locationInfo.mapBounds }.toSet(), object: MapboxMap.CancelableCallback {
            override fun onCancel() {
            }

            override fun onFinish() {
                lifecycleScope.launchWhenResumed {
                    trailLayersManager.addLayers(trails.toSet(), setOf(), true)
                }
            }
        })
    }

    override fun handleMapViewClick(latLng: LatLng) {
        lifecycleScope.launchWhenResumed {
            val trailsQuery = TrailsQuery(mapboxMap, TrailLayerType.values().toSet(), latLng, 5.0f)
            val result = trailsQuery.execute()
            trailLayersManager.selectTrail(result.firstOrNull())
        }
    }
}
