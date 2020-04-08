package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.neotreks.accuterra.mobile.sdk.*
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraMapView
import com.neotreks.accuterra.mobile.sdk.map.TrackingOption
import com.neotreks.accuterra.mobile.sdk.map.query.TrailsQueryBuilder
import com.neotreks.accuterra.mobile.sdk.model.Result
import com.neotreks.accuterra.mobile.sdk.trail.model.MapBounds
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var accuterraMapView: AccuTerraMapView
    private lateinit var mapboxMap: MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, "pk._")

        setContentView(R.layout.activity_main)

        lifecycleScope.launchWhenCreated {
            if (initSdk().isSuccess) {
                setupMap(savedInstanceState)
            }
        }
    }

    private suspend fun initSdk(): Result<Boolean> {
        val sdkConfig = SdkConfig(
            clientToken = "*********************************",
            wsUrl = "*********************************"
        )

        val optionalListener = object : SdkInitListener {
            override fun onProgressChanged(progress: Int) {
                // indicate the progress of the SDK initialization
            }

            override fun onStateChanged(state: SdkInitState, detail: SdkInitStateDetail?) {
                // indicate the SDK initialization state has changed
            }
        }
        return SdkManager.initSdk(this, sdkConfig, optionalListener)
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        accuterraMapView = accuterra_map_view // retrieved by the id set in activity_main.xml

        val listener = object : AccuTerraMapView.IAccuTerraMapViewListener {
            override fun onInitialized(mapboxMap: MapboxMap) {
                this@MainActivity.onMapViewInitialized(mapboxMap)
            }

            override fun onSignificantMapBoundsChange() {
            }

            override fun onStyleChanged(mapboxMap: MapboxMap) {
            }

            override fun onTrackingModeChanged(mode: TrackingOption) {
            }
        }

        accuterraMapView.onCreate(savedInstanceState)
        accuterraMapView.addListener(listener)
        accuterraMapView.initialize(com.mapbox.mapboxsdk.maps.Style.OUTDOORS)
    }

    private fun onMapViewInitialized(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap

        lifecycleScope.launchWhenCreated {
            moveMap()
            addTrails()
            addMapListeners()
        }
    }

    private fun moveMap() {
        val destinationMapBounds = MapBounds(37.99906, -109.04265, 41.00097, -102.04607)
        accuterraMapView.zoomToBounds(destinationMapBounds)
    }

    private suspend fun addTrails() {
        if (SdkManager.isTrailDbInitialized(this)) {
            accuterraMapView.trailLayersManager.addStandardLayers()
        }
    }

    private fun addMapListeners() {
        accuterraMapView.getMapboxMap().addOnMapClickListener { latLng ->
            val searchResult = TrailsQueryBuilder(accuterraMapView.trailLayersManager)
                .setCenter(latLng) // the latitude/longitude clicked on map by user
                .setTolerance(5.0f) // 5 pixel tolerance on click
                .includeAllTrailLayers()
                .create()
                .execute()

            when (searchResult.trailIds.count()) {
                1 -> {
                    accuterraMapView.trailLayersManager.highlightTrail(searchResult.trailIds.single())
                }
                else -> { /* TODO: do something else when multiple trails are clicked */ }
            }

            true // return true to indicate event is handled here
        }
    }
}
