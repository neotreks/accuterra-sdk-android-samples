package com.neotreks.accuterra.mobile.sdk.sampleapp.onlinetrip.components

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.neotreks.accuterra.mobile.sdk.sampleapp.common.ListItemAdapter
import com.neotreks.accuterra.mobile.sdk.sampleapp.extensions.uuidToLong
import com.neotreks.accuterra.mobile.sdk.trip.model.TripMedia

/**
 * Adapter of [TripMedia]
 */
class OnlineTripMediaAdapter(
    context: Context,
    items: List<TripMedia>,
    lifecycleScope: LifecycleCoroutineScope,
) : ListItemAdapter<TripMedia>(context, items, OnlineTripMediaViewBinder(context, lifecycleScope)) {

    override fun getItemId(item: TripMedia): Long {
        return item.uuid.uuidToLong()
    }

}