package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraMapView
import com.neotreks.accuterra.mobile.sdk.map.TrackingOption
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var accuterraMapView: AccuTerraMapView
    private lateinit var mapboxMap: MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, "pk._")

        setContentView(R.layout.activity_main)

        setupMap(savedInstanceState)
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        accuterraMapView = accuterra_map_view // retrieved by the id set in activity_main.xml

        val listener = object : AccuTerraMapView.IAccuTerraMapViewListener {
            override fun onInitialized(mapboxMap: MapboxMap) {
                this@MainActivity.mapboxMap = mapboxMap
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
}
