package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.neotreks.accuterra.mobile.sdk.*
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraMapView
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraStyle
import com.neotreks.accuterra.mobile.sdk.map.TrackingOption
import com.neotreks.accuterra.mobile.sdk.map.query.TrailPoisQueryBuilder
import com.neotreks.accuterra.mobile.sdk.map.query.TrailsQueryBuilder
import com.neotreks.accuterra.mobile.sdk.model.Result
import com.neotreks.accuterra.mobile.sdk.trail.model.MapBounds
import com.neotreks.accuterra.mobile.sdk.trail.model.MapPoint
import com.neotreks.accuterra.mobile.sdk.trail.model.Trail
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
        accuterraMapView.initialize(AccuTerraStyle.VECTOR)
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
//        if (SdkManager.isTrailDbInitialized(this)) {
        accuterraMapView.trailLayersManager.addStandardLayers()
//        }
    }

    private fun addMapListeners() {
        mapboxMap.addOnMapClickListener { latLng ->
            handleMapClick(latLng)
            true
        }
    }

    private fun handleMapClick(latLng: LatLng) {
        var clickHandled = handlePOIClicked(latLng)

        if (!clickHandled) {
            clickHandled = handleTrailClicked(latLng)
        }
    }

    /**@param latLng The gps coordinate [LatLng] of the map click
     * @return whether the click is successfully handled by this function. True if POIs are detected at the location of the click, false if no POIs are detected.
     * */
    private fun handlePOIClicked(latLng: LatLng): Boolean {
        val poiSearchResult = TrailPoisQueryBuilder(accuterraMapView.trailLayersManager)
            .setCenter(latLng)
            .setTolerance(5.0f)
            .includeAllTrailLayers()
            .create()
            .execute()

        return when (poiSearchResult.trailPois.count()) {
            0 -> false
            else -> {
                val poiResult = poiSearchResult.trailPois.first()
                val poiId = poiResult.poiIds.first()

                lifecycleScope.launchWhenCreated {
                    val trail =
                        ServiceFactory
                            .getTrailService(this@MainActivity)
                            .getTrailById(poiResult.trailId)

                    val poi =
                        trail?.navigationInfo?.mapPoints?.first { mapPoint -> mapPoint.id == poiId }

                    showPoiDialog(trail!!, poi!!)
                }

                true
            }
        }
    }

    /**@param latLng The gps coordinate [LatLng] of the map click
     * @return whether the click is successfully handled by this function. True if Trails are detected at the location of the click, false if no Trails are detected.
     * */
    private fun handleTrailClicked(latLng: LatLng): Boolean {
        val searchResult = TrailsQueryBuilder(accuterraMapView.trailLayersManager)
            .setCenter(latLng) // the latitude/longitude clicked on map by user
            .setTolerance(5.0f) // 5 pixel tolerance on click
            .includeAllTrailLayers()
            .create()
            .execute()

        return when (searchResult.trailIds.count()) {
            1 -> {
                val trailId = searchResult.trailIds.single()
                accuterraMapView.trailLayersManager.highlightTrail(trailId)

                lifecycleScope.launchWhenCreated {
                    val trail =
                        ServiceFactory.getTrailService(this@MainActivity).getTrailById(trailId)
                            ?: throw IllegalArgumentException("trailId $trailId not found in data set")

                    displayTrailPOIs(trail)
                }

                true
            }
            else -> {
                /* TODO: do something else when multiple trails are clicked */
                false
            }
        }
    }

    private fun displayTrailPOIs(trail: Trail) {
        accuterraMapView.trailLayersManager.showTrailPOIs(trail)
    }

    private fun showPoiDialog(trail: Trail, poi: MapPoint) {
        val alertTitle = trail.info.name + ": " + when {
            !poi.name.isNullOrBlank() -> {
                poi.name
            }
            poi.navigationOrder != null -> {
                "Waypoint ${poi.navigationOrder}"
            }
            else -> "POI ${poi.id}"
        }

        AlertDialog.Builder(this@MainActivity)
            .setTitle(alertTitle)
            .setMessage(poi.description)
            .setNeutralButton("Ok") { _, _ ->  }
            .show()
    }
}
