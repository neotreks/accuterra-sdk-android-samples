package com.example.myapplication

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyValue
import com.neotreks.accuterra.mobile.sdk.map.PoiDrawable
import com.neotreks.accuterra.mobile.sdk.map.loader.IPoiDrawable
import com.neotreks.accuterra.mobile.sdk.map.style.AccuterraStyleProvider
import com.neotreks.accuterra.mobile.sdk.map.style.TrailLayerStyleType
import com.neotreks.accuterra.mobile.sdk.map.style.TrailMarkerStyleType
import com.neotreks.accuterra.mobile.sdk.map.style.TrailPoiStyleType

class MyCustomStyleProvider(mapStyle: String, private val context: Context) :
    AccuterraStyleProvider(mapStyle, context) {

    override fun getTrailProperties(type: TrailLayerStyleType): Array<PropertyValue<out Any>> {
        return when (type) {
            TrailLayerStyleType.TRAIL_PATH -> {
                arrayOf(PropertyFactory.lineColor("rgb(39,68,102)"))
            }
            TrailLayerStyleType.SELECTED_TRAIL_PATH -> {
                arrayOf(
                    PropertyFactory.lineColor("rgb(0,255,0)"),
                    PropertyFactory.lineWidth(3f)
                )
            }
            TrailLayerStyleType.TRAIL_HEAD -> {
                arrayOf()
            }
        }
    }

    override fun getPoiDrawable(type: TrailPoiStyleType): IPoiDrawable {
        return when (type) {
            TrailPoiStyleType.TRAIL_HEAD -> PoiDrawable(
                type.name,
                context.getDrawable(R.drawable.ic_heart_teal_24dp)!!
            )
            TrailPoiStyleType.TRAIL_POI -> PoiDrawable(
                type.name,
                context.getDrawable(R.drawable.ic_poi_purple_24dp)!!
            )
            TrailPoiStyleType.SELECTED_TRAIL_POI -> PoiDrawable(
                type.name,
                context.getDrawable(R.drawable.ic_location_pin_blue_24dp)!!
            )
        }
    }

    override fun getTrailMarkerProperties(type: TrailMarkerStyleType): Array<PropertyValue<out Any>> {

        return when(type) {
            TrailMarkerStyleType.CLUSTER -> {
                arrayOf(
                    PropertyFactory.circleColor(
                        ContextCompat.getColor(
                            context,
                            android.R.color.holo_orange_light
                        )
                    ),
                    PropertyFactory.circleRadius(15f),
                    PropertyFactory.circleOpacity(0.50f)
                )
            }
            TrailMarkerStyleType.CLUSTER_LABEL -> {
                arrayOf(
                    PropertyFactory.textSize(11f),
                    PropertyFactory.textColor(Color.DKGRAY),
                    PropertyFactory.textIgnorePlacement(true),
                    PropertyFactory.textAllowOverlap(true),
                    PropertyFactory.textFont(arrayOf("Roboto Regular"))
                )
            }
        }
    }

}