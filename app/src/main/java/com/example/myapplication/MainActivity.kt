package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.text.format.Formatter
import android.view.View
import android.widget.Toast
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
import com.neotreks.accuterra.mobile.sdk.map.cache.*
import com.neotreks.accuterra.mobile.sdk.map.query.TrailPoisQueryBuilder
import com.neotreks.accuterra.mobile.sdk.map.query.TrailsQueryBuilder
import com.neotreks.accuterra.mobile.sdk.model.Result
import com.neotreks.accuterra.mobile.sdk.trail.model.MapBounds
import com.neotreks.accuterra.mobile.sdk.trail.model.MapPoint
import com.neotreks.accuterra.mobile.sdk.trail.model.Trail
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var accuterraMapView: AccuTerraMapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var networkStateReceiver: NetworkStateReceiver

    private var offlineMapService: OfflineMapService? = null
    private var offlineMapServiceConnectionListener = object : OfflineMapServiceConnectionListener {
        override fun onConnected(service: OfflineMapService) {
            offlineMapService = service
            checkOfflineOverlayMaps()
        }

        override fun onDisconnected() {
            offlineMapService = null
        }
    }

    private var offlineCacheProgressListener = object : CacheProgressListener {
        override fun onComplete(mapType: OfflineMapType, trailId: Long) {
            Toast.makeText(
                this@MainActivity,
                "SDK initialization completed successfully",
                Toast.LENGTH_SHORT
            ).show()
            download_progress_bar.visibility = View.GONE
        }

        override fun onError(
            error: HashMap<OfflineMapStyle, String>,
            mapType: OfflineMapType,
            trailId: Long
        ) {
            download_progress_bar.visibility = View.GONE
        }

        override fun onProgressChanged(
            progress: Double,
            mapType: OfflineMapType,
            trailId: Long
        ) {
            download_progress_bar.progress = progress.toInt()
        }
    }

    private var offlineMapServiceConnection: ServiceConnection =
        OfflineMapService.createServiceConnection(
            offlineMapServiceConnectionListener,
            offlineCacheProgressListener
        )

    private var mapStyleIndex = 0
    private val mapStyles = arrayOf(
        AccuTerraStyle.VECTOR,
        com.mapbox.mapboxsdk.maps.Style.SATELLITE_STREETS,
        com.mapbox.mapboxsdk.maps.Style.OUTDOORS,
        com.mapbox.mapboxsdk.maps.Style.SATELLITE
    )

    private val offlineMapStyles = arrayOf(
        AccuTerraStyle.VECTOR,
        com.mapbox.mapboxsdk.maps.Style.SATELLITE_STREETS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, "pk._")
        networkStateReceiver = NetworkStateReceiver(this)

        setContentView(R.layout.activity_main)

        lifecycleScope.launchWhenCreated {
            if (initSdk(this@MainActivity).isSuccess) {
                setupMap(savedInstanceState)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (SdkManager.isTrailDbInitialized(this)) {
            startOfflineMapService()
        }
    }

    private fun startOfflineMapService() {
        Intent(this, OfflineMapService::class.java).also { intent ->
            bindService(intent, offlineMapServiceConnection, Context.BIND_AUTO_CREATE)
        }

        val networkListener = object : NetworkStateReceiver.NetworkStateReceiverListener {
            override fun onNetworkAvailable() {
                longToast(this@MainActivity, "network connected")
            }

            override fun onNetworkUnavailable() {
                longToast(this@MainActivity, "network disconnected")

                if (!offlineMapStyles.contains(mapStyles[mapStyleIndex])) {
                    // the current map style has not been cached locally, swap to a cached one
                    toggleMapStyle()
                }
            }
        }

        networkStateReceiver.addListener(networkListener)
    }

    override fun onStop() {
        super.onStop()
        unbindService(offlineMapServiceConnection)
    }

    private suspend fun initSdk(activity: Activity): Result<Boolean> {
        val sdkConfig = SdkConfig(
            clientToken = "*********************************",
            wsUrl = "*********************************"
        )

        val optionalListener = object : SdkInitListener {
            override fun onProgressChanged(progress: Int) {
                download_progress_bar.progress = progress
            }

            override fun onStateChanged(state: SdkInitState, detail: SdkInitStateDetail?) {
                when (state) {
                    SdkInitState.IN_PROGRESS -> {
                        download_progress_bar.visibility = View.VISIBLE
                    }
                    SdkInitState.WAITING,
                    SdkInitState.WAITING_FOR_NETWORK,
                    SdkInitState.PAUSED -> alertSdkInitStateChanged(activity, state)
                    SdkInitState.COMPLETED -> {
                        longToast(activity, "SDK initialization completed successfully")
                        download_progress_bar.visibility = View.GONE
                    }
                    SdkInitState.CANCELED,
                    SdkInitState.FAILED -> {
                        alertSdkInitCeased(activity, state)
                        download_progress_bar.visibility = View.GONE
                    }
                    SdkInitState.UNKNOWN -> throw IllegalStateException()
                }
            }
        }

        return SdkManager.initSdk(activity, sdkConfig, optionalListener)
    }

    private fun alertSdkInitStateChanged(activity: Activity, state: SdkInitState) {
        runOnUiThread {
            AlertDialog.Builder(activity)
                .setTitle("SDK Initialization")
                .setMessage("SDK initialization ${state.name.toLowerCase(Locale.getDefault())}")
                .setPositiveButton("Ok") { _, _ -> }
                .show()
        }
    }

    private fun longToast(activity: Activity, message: String) {
        runOnUiThread {
            Toast.makeText(
                activity,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun alertSdkInitCeased(activity: Activity, state: SdkInitState) {
        runOnUiThread {
            AlertDialog.Builder(activity)
                .setTitle("SDK Initialization")
                .setMessage("SDK initialization ${state.name.toLowerCase(Locale.getDefault())}")
                .setPositiveButton("Retry") { _, _ ->
                    lifecycleScope.launchWhenCreated {
                        initSdk(activity)
                    }
                }
                .setNegativeButton("Quit") { _, _ ->
                    finish()
                }
                .show()
        }
    }

    private fun setupButtons() {
        button_map_style_toggle.setOnClickListener {
            toggleMapStyle()
        }
    }

    private fun setMapStyle(mapStyle: String, context: Context) {
        accuterraMapView.setStyle(mapStyle, MyCustomStyleProvider(context))
    }

    private fun toggleMapStyle() {
        if (!accuterraMapView.isStyleLoaded()) return

        mapStyleIndex += 1
        if (mapStyleIndex < 0 || mapStyleIndex >= mapStyles.size) {
            mapStyleIndex = 0
        }

        val nextStyle = mapStyles[mapStyleIndex]

        when {
            networkStateReceiver.isConnected() -> {
                setMapStyle(nextStyle, this)
            }
            offlineMapStyles.contains(nextStyle) -> {
                setMapStyle(nextStyle, this)
            }
            else -> toggleMapStyle()
        }
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
        accuterraMapView.initialize(mapStyles[mapStyleIndex], MyCustomStyleProvider(this))
    }

    private fun onMapViewInitialized(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap

        lifecycleScope.launchWhenCreated {
            moveMap()
            addTrails()
            addMapListeners()
            setupButtons()
        }
    }

    private fun checkOfflineOverlayMaps() {
        val offlineCacheManager = offlineMapService?.offlineMapManager ?: return
        lifecycleScope.launchWhenCreated {
            when (offlineCacheManager.getOfflineMapStatus(OfflineMapType.OVERLAY)) {
                OfflineMapStatus.NOT_CACHED, OfflineMapStatus.FAILED -> {
                    val estimateBytes = offlineCacheManager.estimateOverlayCacheSize()
                    val estimateText =
                        Formatter.formatShortFileSize(this@MainActivity, estimateBytes)

                    runOnUiThread {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Download")
                            .setMessage("Would you like to download the overlay map cache? ($estimateText)")
                            .setPositiveButton("YES") { _, _ ->
                                lifecycleScope.launchWhenCreated {
                                    runOnUiThread {
                                        download_progress_bar.progress = 0
                                        download_progress_bar.visibility = View.VISIBLE
                                    }
                                    offlineCacheManager.downloadOfflineMap(OfflineMapType.OVERLAY)
                                }
                            }
                            .setNegativeButton("No") { _, _ -> }
                            .show()
                    }
                }
                else -> {
                    // Already in progress or complete
                }
            }
        }
    }

    private fun downloadOfflineMapForTrail(trailId: Long) {
        val offlineMapManager = offlineMapService?.offlineMapManager ?: return

        lifecycleScope.launchWhenCreated {
            when (offlineMapManager.getOfflineMapStatus(OfflineMapType.TRAIL, trailId)) {
                OfflineMapStatus.FAILED, OfflineMapStatus.NOT_CACHED -> {
                    lifecycleScope.launchWhenCreated {
                        offlineMapManager.downloadOfflineMap(OfflineMapType.TRAIL, trailId)
                    }
                }
                else -> {
                }
            }
        }

    }

    private fun moveMap() {
        val destinationMapBounds = MapBounds(37.99906, -109.04265, 41.00097, -102.04607)
        accuterraMapView.zoomToBounds(destinationMapBounds)
    }

    private suspend fun addTrails() {
        accuterraMapView.trailLayersManager.addStandardLayers()
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
            .setNeutralButton("Ok") { _, _ -> }
            .show()
    }
}
