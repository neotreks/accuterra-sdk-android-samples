package com.neotreks.accuterra.mobile.sdk.sampleapp.layers

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.mapbox.mapboxsdk.maps.Style
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraMapView
import com.neotreks.accuterra.mobile.sdk.map.TrackingOption

/**
 * An abstract layers manager.
 */
abstract class LayersManager constructor(val accuTerraMapView: AccuTerraMapView,
                                         protected val lifecycleScope: LifecycleCoroutineScope
) {

    internal abstract fun onStyleChangeStart()
    internal abstract fun onStyleChanged()
    internal abstract fun onTrackingModeChanged(mode: TrackingOption)

    /* * * * * * * * * * * * */
    /*       PROTECTED       */
    /* * * * * * * * * * * * */

    protected fun getMapboxStyle(): Style? {
        return accuTerraMapView.getMapboxMap().style
    }

    protected fun isStyleLoaded(): Boolean {
        return accuTerraMapView.isStyleLoaded()
    }

    protected fun getContext(): Context {
        return accuTerraMapView.context
    }

    /**
     * Removes given map source from the map.
     * Throws [IllegalStateException] when it is not possible to remove the source for any reason.
     */
    protected fun removeSourceOrFail(style: Style, sourceId: String) {
        if (style.getSource(sourceId) == null) {
            return
        }
        if (!style.removeSource(sourceId)) {
            throw IllegalStateException("Not able to remove map source: $sourceId")
        }
    }

    /**
     * Removes given map layer from the map.
     * Throws [IllegalStateException] when it is not possible to remove the layer for any reason.
     */
    protected fun removeLayerOrFail(style: Style, layerId: String) {
        if (style.getLayer(layerId) == null) {
            return
        }
        if (!style.removeLayer(layerId)) {
            throw IllegalStateException("Not able to remove map layer: $layerId")
        }
    }

}