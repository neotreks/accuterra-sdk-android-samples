package com.neotreks.accuterra.mobile.sdk.sampleapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraMapView
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraStyle
import com.neotreks.accuterra.mobile.sdk.map.TrackingOption
import com.neotreks.accuterra.mobile.sdk.trail.extension.extend
import com.neotreks.accuterra.mobile.sdk.trail.extension.toLatLngBounds
import com.neotreks.accuterra.mobile.sdk.trail.model.MapBounds
import java.lang.ref.WeakReference

abstract class MapActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "MapActivity"
    }

    lateinit var accuterraMapView: AccuTerraMapView
    lateinit var mapboxMap: MapboxMap

    private lateinit var accuTerraMapViewListener: AccuTerraMapViewListener
    private lateinit var mapViewLoadingFailListener: MapLoadingFailListener

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accuTerraMapViewListener = AccuTerraMapViewListener(this)
        mapViewLoadingFailListener = MapLoadingFailListener(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        lifecycleScope.launchWhenCreated {
            setupMap(savedInstanceState)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onStart() {
        super.onStart()
        accuterraMapView.onStart()
    }

    override fun onStop() {
        // Call standard stop functions
        super.onStop()
        try {
            accuterraMapView.onStop()
        } catch (e: Throwable) {
            Log.e(TAG, "Error while stopping $TAG", e)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        accuterraMapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        accuterraMapView.removeListener(accuTerraMapViewListener)
        accuterraMapView.removeOnDidFailLoadingMapListener(mapViewLoadingFailListener)
        accuterraMapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        accuterraMapView.onSaveInstanceState(outState)
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        accuterraMapView.onCreate(savedInstanceState)
        accuterraMapView.addListener(accuTerraMapViewListener)
        accuterraMapView.addOnDidFailLoadingMapListener(mapViewLoadingFailListener)

        accuterraMapView.initialize(lifecycleScope, AccuTerraStyle.VECTOR)
    }

    private fun onAccuTerraMapViewReady() {

        // Add layers and register map click handler
        lifecycleScope.launchWhenResumed {
            addLayers()
            registerMapClickHandler()
        }

    }

    abstract fun handleMapViewChanged()

    @UiThread
    fun getVisibleMapBounds(): MapBounds {
        val mapBounds = mapboxMap.projection.visibleRegion.latLngBounds

        val latSouth = mapBounds.latSouth
        val latNorth = mapBounds.latNorth
        val lonWest = mapBounds.lonWest
        val lonEast = mapBounds.lonEast

        return MapBounds(latSouth, lonWest, latNorth, lonEast)
    }

    abstract suspend fun addLayers()

    private fun registerMapClickHandler() {
        mapboxMap.addOnMapClickListener { latLng ->
            handleMapViewClick(latLng)
            return@addOnMapClickListener true
        }
    }

    abstract fun handleMapViewClick(latLng: LatLng)

    /**
     * Zooms to bounds collection without animation, 10 pixel padding
     */
    fun zoomToBoundsExtent(boundsCollection: Set<MapBounds>, callback: MapboxMap.CancelableCallback) {
        if (boundsCollection.isNotEmpty()) {
            var combinedBounds: MapBounds? = null
            boundsCollection.forEach { item ->
                combinedBounds?.let { bounds ->
                    combinedBounds = bounds.extend(item)
                } ?: run {
                    combinedBounds = item
                }
            }

            combinedBounds?.let { bounds ->
                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds.toLatLngBounds(), 10)
                mapboxMap.moveCamera(cameraUpdate, callback)
            }
        }
    }

    private class AccuTerraMapViewListener(activity: MapActivity)
        : AccuTerraMapView.IAccuTerraMapViewListener {

        private val weakActivity= WeakReference(activity)

        override fun onInitialized(mapboxMap: MapboxMap) {
            val activity = weakActivity.get()
                ?: return

            activity.mapboxMap = mapboxMap
            activity.onAccuTerraMapViewReady()
        }

        override fun onSignificantMapBoundsChange() {
            weakActivity.get()?.handleMapViewChanged()
        }

        override fun onStyleChanged(mapboxMap: MapboxMap) {
        }

        override fun onTrackingModeChanged(mode: TrackingOption) {
        }
    }

    private class MapLoadingFailListener(activity: MapActivity) :
        MapView.OnDidFailLoadingMapListener {

        private val weakActivity= WeakReference(activity)

        override fun onDidFailLoadingMap(errorMessage: String?) {
            weakActivity.get()?.let { context ->
                Toast.makeText(context, errorMessage ?: "Unknown Error While Loading Map", Toast.LENGTH_LONG).show()
            }
        }
    }
}
